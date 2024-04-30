/*
 * IUnitGenerator.java
 *
 * Copyright (c) 2016 - Carl Spain. All rights reserved.
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import megamek.client.ratgenerator.MissionRole;
import megamek.common.EntityMovementMode;
import megamek.common.MechSummary;
import megamek.common.UnitType;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;

/**
 * Common interface to interact with various methods for generating units.
 *
 * @author Neoancient
 */
public interface IUnitGenerator {
    /**
     * For convenience in generating traditional ground + vtol units.
     */
    EnumSet<EntityMovementMode> MIXED_TANK_VTOL = EnumSet.of(EntityMovementMode.TRACKED,
            EntityMovementMode.WHEELED, EntityMovementMode.HOVER, EntityMovementMode.WIGE,
            EntityMovementMode.VTOL);

    /**
     * For convenience in generating infantry units.
     */
    EnumSet<EntityMovementMode> ALL_INFANTRY_MODES = EnumSet.of(EntityMovementMode.INF_JUMP,
            EntityMovementMode.INF_LEG, EntityMovementMode.INF_MOTORIZED, EntityMovementMode.INF_UMU,
            EntityMovementMode.TRACKED, EntityMovementMode.WHEELED, EntityMovementMode.HOVER);

    EnumSet<EntityMovementMode> ALL_BATTLE_ARMOR_MODES = EnumSet.of(EntityMovementMode.INF_JUMP,
            EntityMovementMode.INF_LEG, EntityMovementMode.INF_UMU, EntityMovementMode.VTOL);

    /**
     * For convenience in generating infantry units, the minimum tonnage of a foot infantry platoon.
     */
    double FOOT_PLATOON_INFANTRY_WEIGHT = 3.0;

    /**
     * For convenience in generating battle armor, minimum tonnage of a battle armor squad.
     */
    double BATTLE_ARMOR_MIN_WEIGHT = 4.0;

    /**
     * For convenience in generating battle armor/infantry, when the tonnage does not matter
     * (a dedicated DropShip bay, battle armor riding on a 'Mech, etc)
     */
    double NO_WEIGHT_LIMIT = -1.0;

    /**
     * Convenience function to let us know whether a unit type supports weight class selection.
     * @param unitType The unit type to check.
     * @return Whether or not the unit type supports weight class selection.
     */
    static boolean unitTypeSupportsWeightClass(final int unitType) {
        return (unitType == UnitType.AEROSPACEFIGHTER) || (unitType == UnitType.MEK) || (unitType == UnitType.TANK);
    }

    /**
     * @param unitType UnitType constant
     * @return true if the generator supports the unit type
     */
    boolean isSupportedUnitType(final int unitType);

    /**
     * Generate a single unit.
     *
     * @param faction Faction shortname
     * @param unitType UnitType constant
     * @param weightClass EntityWeightClass constant, or -1 for unspecified
     * @param year The year of the campaign date
     * @param quality Index of equipment rating, with zero being the lowest quality.
     * @return A unit that matches the criteria, or null if none can be generated
     */
    default @Nullable MechSummary generate(final String faction, final int unitType, final int weightClass,
                                           final int year, final int quality) {
        return generate(faction, unitType, weightClass, year, quality, null);
    }

    /**
     * Generate a single unit.
     *
     * @param faction Faction shortname
     * @param unitType UnitType constant
     * @param weightClass EntityWeightClass constant, or -1 for unspecified
     * @param year The year of the campaign date
     * @param quality Index of equipment rating, with zero being the lowest quality.
     * @param filter All generated units return true when the filter function is applied.
     * @return A unit that matches the criteria
     */
    default @Nullable MechSummary generate(final String faction, final int unitType, final int weightClass,
                                           final int year, final int quality, @Nullable Predicate<MechSummary> filter) {
        return generate(faction, unitType, weightClass, year, quality, new ArrayList<>(), filter);
    }

    /**
     * Generate a unit using additional parameters specific to the generation method.
     *
     * @param faction Faction shortname
     * @param unitType UnitType constant
     * @param weightClass EntityWeightClass constant, or -1 for unspecified
     * @param year The year of the campaign date
     * @param quality Index of equipment rating, with zero being the lowest quality.
     * @param movementModes A collection of various movement modes
     * @param filter All generated units return true when the filter function is applied.
     * @return A unit that matches the criteria
     */
    default @Nullable MechSummary generate(final String faction, final int unitType, final int weightClass,
                                           final int year, final int quality,
                                           final Collection<EntityMovementMode> movementModes,
                                           @Nullable Predicate<MechSummary> filter) {
        return generate(faction, unitType, weightClass, year, quality, movementModes, new ArrayList<>(), filter);
    }

    /**
     * Generate a unit using additional parameters specific to the generation method.
     *
     * @param faction Faction shortname
     * @param unitType UnitType constant
     * @param weightClass EntityWeightClass constant, or -1 for unspecified
     * @param year The year of the campaign date
     * @param quality Index of equipment rating, with zero being the lowest quality.
     * @param movementModes A collection of various movement modes
     * @param missionRoles A collection of various mission roles
     * @return A unit that matches the criteria
     */
    default @Nullable MechSummary generate(final String faction, final int unitType, final int weightClass,
                                           final int year, final int quality,
                                           final Collection<EntityMovementMode> movementModes,
                                           final Collection<MissionRole> missionRoles) {
        return generate(faction, unitType, weightClass, year, quality, movementModes, missionRoles, null);
    }

    /**
     * Generate a unit using additional parameters specific to the generation method.
     *
     * @param faction Faction shortname
     * @param unitType UnitType constant
     * @param weightClass EntityWeightClass constant, or -1 for unspecified
     * @param year The year of the campaign date
     * @param quality Index of equipment rating, with zero being the lowest quality.
     * @param movementModes A collection of various movement modes
     * @param missionRoles A collection of various mission roles
     * @param filter All generated units return true when the filter function is applied.
     * @return A unit that matches the criteria
     */
    @Nullable MechSummary generate(final String faction, final int unitType, final int weightClass, final int year,
                                   final int quality, final Collection<EntityMovementMode> movementModes,
                                   final Collection<MissionRole> missionRoles, @Nullable Predicate<MechSummary> filter);

    /**
     * Generates a list of units.
     *
     * @param count The number of units to generate
     * @param faction Faction shortname
     * @param unitType UnitType constant
     * @param weightClass EntityWeightClass constant, or -1 for unspecified
     * @param year The year of the campaign date
     * @param quality Index of equipment rating, with zero being the lowest quality.
     * @return A list of units matching the criteria.
     */
    default List<MechSummary> generate(final int count, final String faction, final int unitType, final int weightClass,
                                       final int year, final int quality) {
        return generate(count, faction, unitType, weightClass, year, quality, null);
    }

    /**
     * Generates a list of units with an additional test function.
     *
     * @param count The number of units to generate
     * @param faction Faction shortname
     * @param unitType UnitType constant
     * @param weightClass EntityWeightClass constant, or -1 for unspecified
     * @param year The year of the campaign date
     * @param quality Index of equipment rating, with zero being the lowest quality.
     * @param filter All generated units return true when the filter function is applied.
     * @return A list of units matching the criteria.
     */
    default List<MechSummary> generate(final int count, final String faction, final int unitType, final int weightClass,
                                       final int year, final int quality, @Nullable Predicate<MechSummary> filter) {
        return generate(count, faction, unitType, weightClass, year, quality, new ArrayList<>(), filter);
    }

    /**
     * Generates a list of units using additional parameters specific to the generation method.
     *
     * @param count The number of units to generate
     * @param faction Faction shortname
     * @param unitType UnitType constant
     * @param weightClass EntityWeightClass constant, or -1 for unspecified
     * @param year The year of the campaign date
     * @param quality Index of equipment rating, with zero being the lowest quality.
     * @param movementModes A collection of various movement modes
     * @param filter All generated units return true when the filter function is applied.
     * @return A list of units matching the criteria.
     */
    default List<MechSummary> generate(final int count, final String faction, final int unitType, final int weightClass,
                                       final int year, final int quality,
                                       final Collection<EntityMovementMode> movementModes,
                                       @Nullable Predicate<MechSummary> filter) {
        return generate(count, faction, unitType, weightClass, year, quality, movementModes, new ArrayList<>(), filter);
    }

    /**
     * Generates a list of units using additional parameters specific to the generation method.
     *
     * @param count The number of units to generate
     * @param faction Faction shortname
     * @param unitType UnitType constant
     * @param weightClass EntityWeightClass constant, or -1 for unspecified
     * @param year The year of the campaign date
     * @param quality Index of equipment rating, with zero being the lowest quality.
     * @param movementModes A collection of various movement modes
     * @param missionRoles A collection of various mission roles
     * @return A list of units matching the criteria.
     */
    default @Nullable List<MechSummary> generate(final int count, final String faction, final int unitType,
                                                 final int weightClass, final int year, final int quality,
                                                 final Collection<EntityMovementMode> movementModes,
                                                 final Collection<MissionRole> missionRoles) {
        return generate(count, faction, unitType, weightClass, year, quality, movementModes, missionRoles, null);
    }

    /**
     * Generates a list of units using additional parameters specific to the generation method.
     *
     * @param count The number of units to generate
     * @param faction Faction shortname
     * @param unitType UnitType constant
     * @param weightClass EntityWeightClass constant, or -1 for unspecified
     * @param year The year of the campaign date
     * @param quality Index of equipment rating, with zero being the lowest quality.
     * @param movementModes A collection of various movement modes
     * @param missionRoles A collection of various mission roles
     * @param filter All generated units return true when the filter function is applied.
     * @return A list of units matching the criteria.
     */
    @Nullable List<MechSummary> generate(final int count, final String faction, final int unitType,
                                         final int weightClass, final int year, final int quality,
                                         final Collection<EntityMovementMode> movementModes,
                                         final Collection<MissionRole> missionRoles,
                                         @Nullable Predicate<MechSummary> filter);

    /**
     * Generates a single unit to be used in an OpFor using the given set of parameters.
     * Note that some of the properties of the parameters may be ignored for generation mechanisms that aren't the RAT
     * Generator
     * @param parameters data structure containing unit generation parameters
     * @return The generated unit, or null if none are generated.
     */
    @Nullable MechSummary generate(final UnitGeneratorParameters parameters);

    /**
     * Generates the given count of units to be used in an OpFor using the given set of parameters.
     * Note that some of the properties of the parameters may be ignored for generation mechanisms that aren't the RAT
     * Generator
     * @param count How many to generate
     * @param parameters data structure containing unit generation parameters
     * @return List of generated units. Empty if none are generated.
     */
    List<MechSummary> generate(final int count, final UnitGeneratorParameters parameters);

    /**
     * Generates a list of turrets given a skill level, quality and year
     * @param num How many turrets to generate
     * @param skill The skill level of the turret operator
     * @param quality The quality level of the turret
     * @param currentYear The current year
     * @return List of turrets
     */
    List<MechSummary> generateTurrets(final int num, final SkillLevel skill, final int quality, final int currentYear);
}
