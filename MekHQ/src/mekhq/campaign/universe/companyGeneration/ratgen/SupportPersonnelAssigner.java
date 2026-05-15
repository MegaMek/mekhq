/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe.companyGeneration.ratgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import megamek.common.enums.SkillLevel;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.campaign.universe.enums.TechAssignmentSortFactor;

/**
 * Stage 7e companion to {@link SupportPersonnelGenerator}: walks the campaign's units, picks an
 * appropriate Tech from the freshly-generated support pool for each, and assigns via
 * {@link Unit#setTech(Person)}.
 *
 * <p>Two ordered lists are built:</p>
 *
 * <ul>
 *   <li><b>Units</b> are sorted using a chain of 1-3 user-picked sort slots (Pilot Rank /
 *       Unit Weight Class / Pilot Skill) from {@link CompanyGenerationOptions}. Each slot has its
 *       own ascending / descending direction. Slots set to {@link TechAssignmentSortFactor#NONE}
 *       are skipped.</li>
 *   <li><b>Techs</b> in each role pool are sorted by quality descending — primarily by
 *       experience level, with rank as tiebreaker.</li>
 * </ul>
 *
 * <p>The two lists are then walked in parallel: highest-priority units claim the best available
 * tech for their role-appropriate pool, subject to a per-tech daily-minutes cap of
 * {@link Person#PRIMARY_ROLE_SUPPORT_TIME} (480 min/day). A tech that can't fit a specific unit is
 * not dropped from the pool — they may still fit a lighter unit later in the priority list.</p>
 *
 * <p>Routing table (which tech role maintains which unit type):</p>
 *
 * <table>
 *   <caption>Tech role per unit type</caption>
 *   <tr><th>Unit type</th><th>Tech pool</th></tr>
 *   <tr><td>Mek, LAM, ProtoMek</td><td>MEK_TECH</td></tr>
 *   <tr><td>Tank, VTOL, Naval, Gun Emplacement</td><td>MECHANIC</td></tr>
 *   <tr><td>ConvFighter, AeroSpaceFighter, SmallCraft</td><td>AERO_TEK</td></tr>
 *   <tr><td>BattleArmor</td><td>BA_TECH</td></tr>
 *   <tr><td>DropShip, JumpShip, WarShip, SpaceStation</td><td>(VESSEL_CREW handles)</td></tr>
 *   <tr><td>Infantry</td><td>(no maintenance)</td></tr>
 * </table>
 *
 * <p>Gated on {@link CompanyGenerationOptions#isAssignTechsToUnits()}.</p>
 */
public final class SupportPersonnelAssigner {

    private static final MMLogger LOGGER = MMLogger.create(SupportPersonnelAssigner.class);

    private SupportPersonnelAssigner() {
        // utility class
    }

    /**
     * Assigns each maintainable unit a tech from the appropriate role pool, in user-configured
     * priority order, subject to per-tech daily capacity.
     *
     * @param campaign         the campaign to read units from and assign techs into
     * @param options          the setup options (sort grid, assignment toggle)
     * @param generationResult the output of {@link SupportPersonnelGenerator#generate} — the techs
     *                         in {@link SupportPersonnelGenerator.Result#generatedPersons()} are
     *                         the pool this method draws from
     * @return the number of unit-to-tech assignments made
     */
    public static int assign(Campaign campaign, CompanyGenerationOptions options,
          SupportPersonnelGenerator.Result generationResult) {
        if (campaign == null || options == null || generationResult == null) {
            return 0;
        }
        if (!options.isAssignTechsToUnits()) {
            LOGGER.info("[CompanyGen][Pipeline][Assign] disabled by isAssignTechsToUnits");
            return 0;
        }

        Map<PersonnelRole, List<Person>> pools = buildSortedPools(campaign, generationResult.generatedPersons());
        if (pools.isEmpty()) {
            LOGGER.info("[CompanyGen][Pipeline][Assign] no support techs available to assign");
            return 0;
        }

        Comparator<Unit> unitComparator = buildUnitComparator(campaign, options);
        List<Unit> units = new ArrayList<>(campaign.getActiveUnits());
        units.sort(unitComparator);

        int assigned = 0;
        int skipped = 0;
        for (Unit unit : units) {
            if (unit.getTech() != null) {
                continue;
            }
            if (unit.isMothballed()) {
                continue;
            }
            if (unit.getMaintenanceTime() <= 0) {
                continue; // infantry / vessels / self-maintaining
            }
            PersonnelRole role = matchTechRole(unit);
            if (role == null) {
                skipped++;
                continue;
            }
            List<Person> pool = pools.get(role);
            if (pool == null || pool.isEmpty()) {
                skipped++;
                continue;
            }
            if (tryAssignFromPool(unit, pool)) {
                assigned++;
            } else {
                skipped++;
            }
        }

        LOGGER.info("[CompanyGen][Pipeline][Assign] assigned techs to {} units; {} units skipped " +
                          "(no role match / pool exhausted / full)", assigned, skipped);
        return assigned;
    }

    /**
     * Greedy: walk the pool in quality order; the first tech with enough daily capacity to absorb
     * this unit gets the assignment. Techs that can't fit this unit are NOT removed — they may
     * still absorb a lighter unit later in the priority order.
     */
    private static boolean tryAssignFromPool(Unit unit, List<Person> pool) {
        int unitTime = unit.getMaintenanceTime();
        for (Person tech : pool) {
            if (tech.getMaintenanceTimeUsing() + unitTime <= Person.PRIMARY_ROLE_SUPPORT_TIME) {
                unit.setTech(tech);
                return true;
            }
        }
        return false;
    }

    private static Map<PersonnelRole, List<Person>> buildSortedPools(Campaign campaign,
          Collection<Person> generatedPersons) {
        Map<PersonnelRole, List<Person>> pools = new EnumMap<>(PersonnelRole.class);
        if (generatedPersons == null) {
            return pools;
        }
        for (Person person : generatedPersons) {
            if (person == null) {
                continue;
            }
            PersonnelRole role = person.getPrimaryRole();
            if (role == PersonnelRole.MEK_TECH
                  || role == PersonnelRole.MECHANIC
                  || role == PersonnelRole.AERO_TEK
                  || role == PersonnelRole.BA_TECH) {
                pools.computeIfAbsent(role, k -> new ArrayList<>()).add(person);
            }
        }
        // Sort each pool by quality descending (skill level dominant; rank breaks ties). The
        // assignment loop above relies on the first tech being the best available.
        Comparator<Person> ascending = Comparator
              .comparingInt((Person p) -> techSkillExp(p, campaign))
              .thenComparingInt(Person::getRankNumeric);
        Comparator<Person> descending = ascending.reversed();
        for (List<Person> pool : pools.values()) {
            pool.sort(descending);
        }
        return pools;
    }

    private static Comparator<Unit> buildUnitComparator(Campaign campaign,
          CompanyGenerationOptions options) {
        Comparator<Unit> chain = (a, b) -> 0;
        chain = appendSlot(chain, campaign, options.getTechAssignmentPrimarySort(),
              options.isTechAssignmentPrimaryDescending());
        chain = appendSlot(chain, campaign, options.getTechAssignmentSecondarySort(),
              options.isTechAssignmentSecondaryDescending());
        chain = appendSlot(chain, campaign, options.getTechAssignmentTertiarySort(),
              options.isTechAssignmentTertiaryDescending());
        return chain;
    }

    private static Comparator<Unit> appendSlot(Comparator<Unit> base, Campaign campaign,
          TechAssignmentSortFactor factor, boolean descending) {
        if (factor == null || factor == TechAssignmentSortFactor.NONE) {
            return base;
        }
        Comparator<Unit> slot = switch (factor) {
            case PILOT_RANK -> Comparator.comparingInt(SupportPersonnelAssigner::pilotRankOf);
            case UNIT_WEIGHT -> Comparator.comparingInt(SupportPersonnelAssigner::weightClassOf);
            case PILOT_SKILL -> Comparator.comparingInt(u -> pilotSkillOf(u, campaign));
            default -> null;
        };
        if (slot == null) {
            return base;
        }
        if (descending) {
            slot = slot.reversed();
        }
        return base.thenComparing(slot);
    }

    private static int pilotRankOf(Unit unit) {
        Person commander = unit.getCommander();
        return (commander != null) ? commander.getRankNumeric() : 0;
    }

    private static int weightClassOf(Unit unit) {
        Entity entity = unit.getEntity();
        return (entity != null) ? entity.getWeightClass() : 0;
    }

    private static int pilotSkillOf(Unit unit, Campaign campaign) {
        Person commander = unit.getCommander();
        if (commander == null) {
            return 0;
        }
        SkillLevel level = commander.getSkillLevel(campaign, false);
        return (level != null) ? level.getExperienceLevel() : 0;
    }

    private static int techSkillExp(Person tech, Campaign campaign) {
        SkillLevel level = tech.getSkillLevel(campaign, false);
        return (level != null) ? level.getExperienceLevel() : 0;
    }

    /**
     * Maps a Unit to the {@link PersonnelRole} of the tech who should maintain it. Returns
     * {@code null} for units that aren't covered by a SUPPORT tech role (vessels handled by
     * organic VESSEL_CREW, infantry have zero maintenance time).
     */
    private static PersonnelRole matchTechRole(Unit unit) {
        Entity entity = unit.getEntity();
        if (entity == null) {
            return null;
        }
        if (entity.isMek() || entity.isProtoMek()) {
            return PersonnelRole.MEK_TECH;
        }
        if (entity.isVehicle()) {
            return PersonnelRole.MECHANIC;
        }
        if (entity.isAerospaceFighter()
              || entity.isConventionalFighter()
              || entity.isSmallCraft()) {
            return PersonnelRole.AERO_TEK;
        }
        if (entity.isBattleArmor()) {
            return PersonnelRole.BA_TECH;
        }
        return null;
    }
}
