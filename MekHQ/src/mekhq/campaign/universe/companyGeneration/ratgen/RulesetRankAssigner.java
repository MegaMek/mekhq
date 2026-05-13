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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;

/**
 * Tree-aware rank assignment pass for the Force Generator pipeline.
 *
 * <p>Replaces the flat-list rank logic in the legacy {@code AbstractCompanyGenerator}. The legacy
 * pipeline picked "the highest-skilled Person → company commander, then one officer per lance" off
 * a sorted tracker list. The ratgen pipeline produces a real Formation tree with arbitrary nesting,
 * so this pass walks the tree post-order and claims one officer per Formation node — Lance commanders
 * pick their Lieutenant first, then the Company looks for its Captain among the remaining unranked
 * crew, and so on up to Brigade / Army / Galaxy.</p>
 *
 * <p>FormationLevel → officer rank index ({@code Rank.RWO_MAX + N}):</p>
 *
 * <ul>
 *   <li>LANCE / STAR_OR_NOVA / LEVEL_II_OR_CHOIR → O3 (Lieutenant / Star Captain / Adept)</li>
 *   <li>COMPANY / BINARY_OR_TRINARY / LEVEL_III → O4 (Captain / Star Colonel / Demi-Precentor)</li>
 *   <li>BATTALION / CLUSTER / LEVEL_IV → O5 (Major / Galaxy Commander / Precentor)</li>
 *   <li>REGIMENT / GALAXY / LEVEL_V → O7 (Brigadier General / Khan / Precentor Martial)</li>
 *   <li>BRIGADE / TOUMAN / LEVEL_VI → O8 (Major General)</li>
 *   <li>DIVISION → O9, CORPS → O10, ARMY → O11, ARMY_GROUP → O12</li>
 * </ul>
 *
 * <p>Non-officer combat crew get an enlisted Sergeant rank (E12 IS, E4 Clan/CS); support staff get
 * a Corporal-equivalent (E8 IS, E4 Clan/CS) matching the legacy generator's convention. The campaign's
 * rank system (loaded from {@code data/universe/ranks.xml} per faction) maps the integer index to the
 * faction-appropriate display name.</p>
 *
 * <p>Gated on {@link CompanyGenerationOptions#isAutomaticallyAssignRanks()}. Honors
 * {@link CompanyGenerationOptions#isUseSpecifiedFactionToAssignRanks()} for the faction picker.</p>
 */
public final class RulesetRankAssigner {

    private static final MMLogger LOGGER = MMLogger.create(RulesetRankAssigner.class);

    private RulesetRankAssigner() {
        // utility class
    }

    public static void apply(Campaign campaign, CompanyGenerationOptions options) {
        long startNanos = System.nanoTime();
        if (campaign == null || options == null) {
            LOGGER.info("[CompanyGen][RankAssign] apply: campaign or options null, skipping");
            return;
        }
        if (!options.isAutomaticallyAssignRanks()) {
            LOGGER.info("[CompanyGen][RankAssign] apply: disabled by isAutomaticallyAssignRanks");
            return;
        }
        Formation root = campaign.getFormations();
        if (root == null) {
            LOGGER.info("[CompanyGen][RankAssign] apply: campaign has no root Formation, skipping");
            return;
        }

        Faction faction = options.isUseSpecifiedFactionToAssignRanks()
              ? options.getSpecifiedFaction()
              : campaign.getFaction();
        if (faction == null) {
            faction = campaign.getFaction();
        }

        int enlistedRank = enlistedRankForFaction(faction);
        int supportRank = supportRankForFaction(faction);

        LOGGER.info("[CompanyGen][RankAssign] START faction={} enlistedRank={} supportRank={} root='{}' thread={}",
              faction.getShortName(), enlistedRank, supportRank, root.getName(),
              Thread.currentThread().getName());

        // Pass 1: walk the tree post-order so lance / star commanders claim their officer rank
        // before their parent company / binary looks for ITS commander among remaining unranked
        // crew. Each Formation promotes the first not-yet-promoted combat Person in its subtree.
        LOGGER.info("[CompanyGen][RankAssign][Pass1] BEFORE walkPostOrder");
        long pass1Start = System.nanoTime();
        Set<Person> promoted = new LinkedHashSet<>();
        int officersAssigned = walkPostOrder(campaign, root, promoted);
        long pass1Ms = (System.nanoTime() - pass1Start) / 1_000_000;
        LOGGER.info("[CompanyGen][RankAssign][Pass1] AFTER walkPostOrder officers={} elapsed={}ms",
              officersAssigned, pass1Ms);

        // Pass 2: every other combat Person gets the enlisted rank; every support Person gets the
        // Corporal-equivalent. Walk the tree's Units to limit the impact to crew we generated, not
        // pre-existing personnel.
        LOGGER.info("[CompanyGen][RankAssign][Pass2] BEFORE enlisted/support assignment");
        long pass2Start = System.nanoTime();
        int enlistedAssigned = 0;
        int supportAssigned = 0;
        Set<UUID> personIdsAlreadyDone = new HashSet<>();
        for (UUID unitId : root.getAllUnits(false)) {
            Unit unit = campaign.getUnit(unitId);
            if (unit == null) {
                continue;
            }
            for (Person person : unitCrew(unit)) {
                if (person == null) {
                    continue;
                }
                if (!personIdsAlreadyDone.add(person.getId())) {
                    continue;
                }
                if (promoted.contains(person)) {
                    continue;
                }
                if (person.isSupport()) {
                    person.setRank(supportRank);
                    supportAssigned++;
                } else if (person.isCombat()) {
                    person.setRank(enlistedRank);
                    enlistedAssigned++;
                }
            }
        }
        long pass2Ms = (System.nanoTime() - pass2Start) / 1_000_000;
        LOGGER.info("[CompanyGen][RankAssign][Pass2] AFTER enlisted={} support={} elapsed={}ms",
              enlistedAssigned, supportAssigned, pass2Ms);

        long totalMs = (System.nanoTime() - startNanos) / 1_000_000;
        LOGGER.info("[CompanyGen][RankAssign] DONE; officers={} enlisted={} support={} totalMs={}",
              officersAssigned, enlistedAssigned, supportAssigned, totalMs);
    }

    /**
     * Walks the formation tree post-order, promoting one Person per Formation node to that
     * Formation's officer rank. Returns the count of officers promoted.
     */
    private static int walkPostOrder(Campaign campaign, Formation formation, Set<Person> promoted) {
        int count = 0;
        for (Formation sub : formation.getSubFormations()) {
            count += walkPostOrder(campaign, sub, promoted);
        }
        int rankIndex = rankIndexForLevel(formation.getFormationLevel());
        if (rankIndex < 0) {
            LOGGER.info("[CompanyGen][RankAssign][Pass1]   formation '{}' (level={}) -> no rank mapping, skip",
                  formation.getName(), formation.getFormationLevel());
            return count;
        }
        Person commander = pickCommander(campaign, formation, promoted);
        if (commander != null) {
            commander.setRank(rankIndex);
            promoted.add(commander);
            count++;
            LOGGER.info("[CompanyGen][RankAssign][Pass1]   formation '{}' (level={}) -> '{}' promoted to rank index {}",
                  formation.getName(), formation.getFormationLevel(),
                  commander.getFullName(), rankIndex);
        } else {
            LOGGER.info("[CompanyGen][RankAssign][Pass1]   formation '{}' (level={}) -> no unpromoted combat Person available",
                  formation.getName(), formation.getFormationLevel());
        }
        return count;
    }

    /**
     * Finds the first combat Person in the formation's subtree who hasn't already been promoted by
     * a deeper formation. For the MVP this is "first not-yet-promoted"; a future refinement can
     * sort by skill (combat-weighted when isPrioritizeOfficerCombatSkills is on).
     */
    private static Person pickCommander(Campaign campaign, Formation formation, Set<Person> promoted) {
        for (UUID unitId : formation.getAllUnits(false)) {
            Unit unit = campaign.getUnit(unitId);
            if (unit == null) {
                continue;
            }
            for (Person person : unitCrew(unit)) {
                if (person == null) {
                    continue;
                }
                if (promoted.contains(person)) {
                    continue;
                }
                if (person.isCombat()) {
                    return person;
                }
            }
        }
        return null;
    }

    /**
     * Returns every Person attached to a unit — drivers + gunners + vessel crew + navigator. We
     * collect manually instead of using {@code getActiveCrew()} because the latter filters out
     * wounded Tank / Infantry pilots, and at generation time we want to count every assigned slot.
     */
    private static List<Person> unitCrew(Unit unit) {
        List<Person> out = new ArrayList<>();
        if (unit.getDrivers() != null) {
            out.addAll(unit.getDrivers());
        }
        if (unit.getGunners() != null) {
            out.addAll(unit.getGunners());
        }
        if (unit.getVesselCrew() != null) {
            out.addAll(unit.getVesselCrew());
        }
        Person navigator = unit.getNavigator();
        if (navigator != null) {
            out.add(navigator);
        }
        return out;
    }

    /**
     * Maps {@link FormationLevel} to the {@code Person.setRank(int)} index for that formation's
     * commander, covering every echelon the ratgen engine can produce (Lance through Army Group
     * for IS, Point through Touman for Clan, Level I through Level VI for ComStar/WoB).
     */
    private static int rankIndexForLevel(FormationLevel level) {
        if (level == null) {
            return -1;
        }
        return switch (level) {
            // O3 — Lieutenant / Star Captain / Adept
            case LANCE, STAR_OR_NOVA, LEVEL_II_OR_CHOIR -> Rank.RWO_MAX + 3;
            // O4 — Captain / Star Colonel / Demi-Precentor
            case COMPANY, BINARY_OR_TRINARY, LEVEL_III -> Rank.RWO_MAX + 4;
            // O5 — Major / Galaxy Commander / Precentor
            case BATTALION, CLUSTER, LEVEL_IV -> Rank.RWO_MAX + 5;
            // O7 — Brigadier General / Khan / Precentor Martial. Skips O6 to match the legacy
            // generator's force-size → rank table, which jumps from O5 directly to O7 for the
            // higher echelons.
            case REGIMENT, GALAXY, LEVEL_V -> Rank.RWO_MAX + 7;
            // O8 — Major General
            case BRIGADE, TOUMAN, LEVEL_VI -> Rank.RWO_MAX + 8;
            case DIVISION -> Rank.RWO_MAX + 9;
            case CORPS -> Rank.RWO_MAX + 10;
            case ARMY -> Rank.RWO_MAX + 11;
            case ARMY_GROUP -> Rank.RWO_MAX + 12;
            default -> -1;
        };
    }

    /**
     * Returns the enlisted rank index for non-officer combat crew, matching the legacy generator's
     * convention: Sergeant-equivalent for Inner Sphere / Periphery (E12), lower index for Clan and
     * ComStar / WoB rank systems (E4).
     */
    private static int enlistedRankForFaction(Faction faction) {
        return (faction.isComStarOrWoB() || faction.isClan()) ? 4 : 12;
    }

    /**
     * Returns the rank index for support personnel (techs, doctors, admins). Mirrors the legacy
     * generator's {@code generateSupportPerson} switch: Corporal-equivalent for IS / Periphery,
     * lower index for Clan / ComStar / WoB.
     */
    private static int supportRankForFaction(Faction faction) {
        return (faction.isComStarOrWoB() || faction.isClan()) ? 4 : 8;
    }
}
