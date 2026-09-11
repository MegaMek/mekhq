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
package mekhq.campaign.universe.commandGeneration.ratgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.enums.TechAssignmentSortFactor;

/**
 * Turns the Setup tab's tech assignment grid into the pieces
 * {@link mekhq.campaign.utilities.AutomatedTechAssignments} needs: the order to offer units a tech in, and the pool of
 * techs to draw from.
 *
 * <p>The player picks up to three sort slots - Pilot Rank, Unit Weight Class, Pilot Skill - each with its own
 * ascending or descending direction, and the slots are applied in order. A slot set to
 * {@link TechAssignmentSortFactor#NONE} is skipped. With every slot set to {@code NONE} the order is empty and the
 * shared assigner falls back to its own battle value ordering.</p>
 *
 * @since 0.51.01
 */
public final class TechAssignmentOrder {

    private TechAssignmentOrder() {
        // utility class
    }

    /**
     * Builds the unit ordering the player asked for on the Setup tab.
     *
     * @param campaign the campaign the units belong to, used to read pilot skill levels
     * @param options  the generation options holding the three sort slots and their directions
     *
     * @return the ordering, or {@code null} when no slot is set, so the caller keeps the shared assigner's default
     *
     * @since 0.51.01
     */
    public static @Nullable Comparator<Unit> unitOrderFor(Campaign campaign, CommandGenerationOptions options) {
        if ((campaign == null) || (options == null)) {
            return null;
        }
        Comparator<Unit> order = null;
        order = appendSlot(order, campaign, options.getTechAssignmentPrimarySort(),
              options.isTechAssignmentPrimaryDescending());
        order = appendSlot(order, campaign, options.getTechAssignmentSecondarySort(),
              options.isTechAssignmentSecondaryDescending());
        order = appendSlot(order, campaign, options.getTechAssignmentTertiarySort(),
              options.isTechAssignmentTertiaryDescending());
        return order;
    }

    /**
     * The techs among {@code generatedPersons}, so generation assigns only the people it just created and never
     * claims a tech the campaign already had.
     *
     * @param generatedPersons everyone generation produced; may be {@code null}
     *
     * @return the tech-role people among them, never {@code null}
     *
     * @since 0.51.01
     */
    public static List<Person> techsAmong(@Nullable Collection<Person> generatedPersons) {
        List<Person> techs = new ArrayList<>();
        if (generatedPersons == null) {
            return techs;
        }
        for (Person person : generatedPersons) {
            if (person == null) {
                continue;
            }
            PersonnelRole role = person.getPrimaryRole();
            if ((role == PersonnelRole.MEK_TECH)
                  || (role == PersonnelRole.MECHANIC)
                  || (role == PersonnelRole.AERO_TEK)
                  || (role == PersonnelRole.BA_TECH)) {
                techs.add(person);
            }
        }
        return techs;
    }

    /**
     * Adds one sort slot to the chain being built, skipping slots the player left unset.
     *
     * @param base       the ordering built from the earlier slots, or {@code null} when this is the first one set
     * @param campaign   the campaign the units belong to
     * @param factor     the factor this slot sorts by; {@code null} and {@link TechAssignmentSortFactor#NONE} are
     *                   skipped
     * @param descending whether this slot sorts highest first
     *
     * @return the ordering with this slot appended, or {@code base} unchanged when the slot is unset
     */
    private static @Nullable Comparator<Unit> appendSlot(@Nullable Comparator<Unit> base, Campaign campaign,
          TechAssignmentSortFactor factor, boolean descending) {
        if ((factor == null) || (factor == TechAssignmentSortFactor.NONE)) {
            return base;
        }
        Comparator<Unit> slot = switch (factor) {
            case PILOT_RANK -> Comparator.comparingInt(TechAssignmentOrder::pilotRankOf);
            case UNIT_WEIGHT -> Comparator.comparingInt(TechAssignmentOrder::weightClassOf);
            case PILOT_SKILL -> Comparator.comparingInt(unit -> pilotSkillOf(unit, campaign));
            default -> null;
        };
        if (slot == null) {
            return base;
        }
        if (descending) {
            slot = slot.reversed();
        }
        return (base == null) ? slot : base.thenComparing(slot);
    }

    /** The rank of the unit's commander, or {@code 0} when it has none. */
    private static int pilotRankOf(Unit unit) {
        Person commander = unit.getCommander();
        return (commander != null) ? commander.getRankNumeric() : 0;
    }

    /** The unit's weight class, or {@code 0} when it has no entity. */
    private static int weightClassOf(Unit unit) {
        Entity entity = unit.getEntity();
        return (entity != null) ? entity.getWeightClass() : 0;
    }

    /** The experience level of the unit's commander, or {@code 0} when it has none. */
    private static int pilotSkillOf(Unit unit, Campaign campaign) {
        Person commander = unit.getCommander();
        if (commander == null) {
            return 0;
        }
        SkillLevel level = commander.getSkillLevel(campaign, false);
        return (level != null) ? level.getExperienceLevel() : 0;
    }
}
