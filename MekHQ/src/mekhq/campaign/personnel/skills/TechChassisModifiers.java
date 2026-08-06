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
package mekhq.campaign.personnel.skills;

import static megamek.common.units.EntityWeightClass.WEIGHT_ASSAULT;
import static megamek.common.units.EntityWeightClass.WEIGHT_HEAVY;
import static megamek.common.units.EntityWeightClass.WEIGHT_LIGHT;
import static megamek.common.units.EntityWeightClass.WEIGHT_MEDIUM;
import static mekhq.campaign.personnel.PersonnelOptions.*;

import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.common.units.Entity;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;

/**
 * Computes the tech-flavoured weight-class SPA modifiers (Affinity, Antipathy, and Unit Specialist) for a tech working
 * on a specific unit. These mirror the combat SPAs but map piloting to Maintenance and gunnery to Repair, so they are
 * applied directly at the maintenance and repair target-roll sites rather than in {@link Skill#getSPAModifiers}.
 *
 * <p>Returned values are target-roll modifiers: negative improves the roll (a bonus), positive worsens it (a penalty).
 * Affinity and a matching Specialist yield {@code -1}; Antipathy and a non-matching Specialist yield {@code +1}.
 * Affinity/Antipathy affect Maintenance only; the Specialist affects both Maintenance and Repair.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class TechChassisModifiers {
    private enum Family {MEK, VEHICULAR, FLIGHT}

    private TechChassisModifiers() {}

    /**
     * Returns the tech's Maintenance target-roll modifier for the given unit: Affinity/Antipathy for the unit's family
     * and weight class, plus the Unit Specialist bonus/penalty. Negative improves the roll.
     *
     * @param tech   the tech performing maintenance (may be {@code null})
     * @param entity the unit being maintained (may be {@code null})
     *
     * @return the target-roll modifier (0 if none applies)
     */
    public static int getMaintenanceModifier(final @Nullable Person tech, final @Nullable Entity entity) {
        Family family = familyOf(entity);
        if ((tech == null) || (family == null)) {
            return 0;
        }
        int weightClass = entity.getWeightClass();
        PersonnelOptions options = tech.getOptions();

        int modifier = specialistModifier(options, family, weightClass);

        String affinity = affinityName(family, weightClass);
        if ((affinity != null) && options.booleanOption(affinity)) {
            modifier -= 1;
        }
        String antipathy = antipathyName(family, weightClass);
        if ((antipathy != null) && options.booleanOption(antipathy)) {
            modifier += 1;
        }
        return modifier;
    }

    /**
     * Returns the tech's Repair target-roll modifier for the given unit: only the Unit Specialist bonus/penalty applies
     * to repair (Affinity/Antipathy are the maintenance-only piloting analog). Negative improves the roll.
     *
     * @param tech   the tech performing the repair (may be {@code null})
     * @param entity the unit being repaired (may be {@code null})
     *
     * @return the target-roll modifier (0 if none applies)
     */
    public static int getRepairModifier(final @Nullable Person tech, final @Nullable Entity entity) {
        Family family = familyOf(entity);
        if ((tech == null) || (family == null)) {
            return 0;
        }
        return specialistModifier(tech.getOptions(), family, entity.getWeightClass());
    }

    private static int specialistModifier(final PersonnelOptions options, final Family family, final int weightClass) {
        IOption specialist = options.getOption(TECH_UNIT_SPECIALIST);
        if ((specialist == null) || !specialist.booleanValue()) {
            return 0;
        }
        String currentKey = specialtyKey(family, weightClass);
        if (currentKey == null) {
            return 1;
        }
        return specialist.stringValue().equalsIgnoreCase(currentKey) ? -1 : 1;
    }

    private static @Nullable Family familyOf(final @Nullable Entity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.isMek()) {
            return Family.MEK;
        }
        if (entity.isFighter()) {
            return Family.FLIGHT;
        }
        if (entity.isVehicle()) {
            return Family.VEHICULAR;
        }
        return null;
    }

    private static @Nullable String specialtyKey(final Family family, final int weightClass) {
        return switch (family) {
            case MEK -> switch (weightClass) {
                case WEIGHT_LIGHT -> SPECIALIST_CHOICE_MEK_LIGHT;
                case WEIGHT_MEDIUM -> SPECIALIST_CHOICE_MEK_MEDIUM;
                case WEIGHT_HEAVY -> SPECIALIST_CHOICE_MEK_HEAVY;
                case WEIGHT_ASSAULT -> SPECIALIST_CHOICE_MEK_ASSAULT;
                default -> null;
            };
            case VEHICULAR -> switch (weightClass) {
                case WEIGHT_LIGHT -> SPECIALIST_CHOICE_VEHICULAR_LIGHT;
                case WEIGHT_MEDIUM -> SPECIALIST_CHOICE_VEHICULAR_MEDIUM;
                case WEIGHT_HEAVY -> SPECIALIST_CHOICE_VEHICULAR_HEAVY;
                case WEIGHT_ASSAULT -> SPECIALIST_CHOICE_VEHICULAR_ASSAULT;
                default -> null;
            };
            case FLIGHT -> switch (weightClass) {
                case WEIGHT_LIGHT -> SPECIALIST_CHOICE_FLIGHT_LIGHT;
                case WEIGHT_MEDIUM -> SPECIALIST_CHOICE_FLIGHT_MEDIUM;
                case WEIGHT_HEAVY -> SPECIALIST_CHOICE_FLIGHT_HEAVY;
                default -> null;
            };
        };
    }

    private static @Nullable String affinityName(final Family family, final int weightClass) {
        return switch (family) {
            case MEK -> switch (weightClass) {
                case WEIGHT_LIGHT -> TECH_MEK_AFFINITY_LIGHT;
                case WEIGHT_MEDIUM -> TECH_MEK_AFFINITY_MEDIUM;
                case WEIGHT_HEAVY -> TECH_MEK_AFFINITY_HEAVY;
                case WEIGHT_ASSAULT -> TECH_MEK_AFFINITY_ASSAULT;
                default -> null;
            };
            case VEHICULAR -> switch (weightClass) {
                case WEIGHT_LIGHT -> TECH_VEHICULAR_AFFINITY_LIGHT;
                case WEIGHT_MEDIUM -> TECH_VEHICULAR_AFFINITY_MEDIUM;
                case WEIGHT_HEAVY -> TECH_VEHICULAR_AFFINITY_HEAVY;
                case WEIGHT_ASSAULT -> TECH_VEHICULAR_AFFINITY_ASSAULT;
                default -> null;
            };
            case FLIGHT -> switch (weightClass) {
                case WEIGHT_LIGHT -> TECH_FLIGHT_AFFINITY_LIGHT;
                case WEIGHT_MEDIUM -> TECH_FLIGHT_AFFINITY_MEDIUM;
                case WEIGHT_HEAVY -> TECH_FLIGHT_AFFINITY_HEAVY;
                default -> null;
            };
        };
    }

    private static @Nullable String antipathyName(final Family family, final int weightClass) {
        return switch (family) {
            case MEK -> switch (weightClass) {
                case WEIGHT_LIGHT -> TECH_MEK_ANTIPATHY_LIGHT;
                case WEIGHT_MEDIUM -> TECH_MEK_ANTIPATHY_MEDIUM;
                case WEIGHT_HEAVY -> TECH_MEK_ANTIPATHY_HEAVY;
                case WEIGHT_ASSAULT -> TECH_MEK_ANTIPATHY_ASSAULT;
                default -> null;
            };
            case VEHICULAR -> switch (weightClass) {
                case WEIGHT_LIGHT -> TECH_VEHICULAR_ANTIPATHY_LIGHT;
                case WEIGHT_MEDIUM -> TECH_VEHICULAR_ANTIPATHY_MEDIUM;
                case WEIGHT_HEAVY -> TECH_VEHICULAR_ANTIPATHY_HEAVY;
                case WEIGHT_ASSAULT -> TECH_VEHICULAR_ANTIPATHY_ASSAULT;
                default -> null;
            };
            case FLIGHT -> switch (weightClass) {
                case WEIGHT_LIGHT -> TECH_FLIGHT_ANTIPATHY_LIGHT;
                case WEIGHT_MEDIUM -> TECH_FLIGHT_ANTIPATHY_MEDIUM;
                case WEIGHT_HEAVY -> TECH_FLIGHT_ANTIPATHY_HEAVY;
                default -> null;
            };
        };
    }
}
