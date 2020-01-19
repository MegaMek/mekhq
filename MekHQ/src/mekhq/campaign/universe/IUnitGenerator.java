/*
 * IUnitGenerator.java
 *
 * Copyright (c) 2016 Carl Spain. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.universe;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import megamek.common.EntityMovementMode;
import megamek.common.MechSummary;
import megamek.common.UnitType;

/**
 * Common interface to interact with various methods for generating units.
 *
 * @author Neoancient
 *
 */
public interface IUnitGenerator {

    /*
     * For convenience in generating traditional ground + vtol units.
     */
    final static EnumSet<EntityMovementMode> MIXED_TANK_VTOL = EnumSet.of(EntityMovementMode.TRACKED,
            EntityMovementMode.WHEELED, EntityMovementMode.HOVER, EntityMovementMode.WIGE,
            EntityMovementMode.VTOL);

    /**
     * For convenience in generating infantry units.
     */
    final static EnumSet<EntityMovementMode> ALL_INFANTRY_MODES = EnumSet.of(EntityMovementMode.INF_JUMP,
            EntityMovementMode.INF_LEG, EntityMovementMode.INF_MOTORIZED, EntityMovementMode.INF_UMU,
            EntityMovementMode.TRACKED, EntityMovementMode.WHEELED, EntityMovementMode.HOVER);

    /**
     * For convenience in generating infantry units, the maximum tonnage of a foot infantry platoon.
     */
    final static double FOOT_PLATOON_INFANTRY_WEIGHT = 3.0;

    /**
     * Convenience function to let us know whether a unit type supports weight class selection.
     * @param unitType The unit type to check.
     * @return Whether or not the unit type supports weight class selection.
     */
    static boolean unitTypeSupportsWeightClass(int unitType) {
        return unitType == UnitType.AERO || unitType == UnitType.MEK || unitType == UnitType.TANK;
    }

    /**
     *
     * @param unitType UnitType constant
     * @return true if the generator supports the unit type
     */
    boolean isSupportedUnitType(int unitType);

    /**
     * Generate a single unit.
     *
     * @param faction Faction shortname
     * @param unitType UnitType constant
     * @param weightClass EntityWeightClass constant, or -1 for unspecified
     * @param year The year of the campaign date
     * @param quality Index of equipment rating, with zero being the lowest quality.
     * @return A unit that matches the criteria
     */
    MechSummary generate(String faction, int unitType, int weightClass, int year, int quality);

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
    MechSummary generate(String faction, int unitType, int weightClass, int year, int quality,
            Predicate<MechSummary> filter);

    /**
     *
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
    List<MechSummary> generate(int count, String faction, int unitType, int weightClass,
            int year, int quality);

    /**
     *
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

    List<MechSummary> generate(int count, String faction, int unitType, int weightClass,
            int year, int quality, Predicate<MechSummary> filter);

    /**
     * Generate a unit using additional parameters specific to the generation method.
     *
     * @param faction Faction shortname
     * @param unitType UnitType constant
     * @param weightClass EntityWeightClass constant, or -1 for unspecified
     * @param year The year of the campaign date
     * @param quality Index of equipment rating, with zero being the lowest quality.
     * @param movementModes A collection of various movement modes
     * @return A unit that matches the criteria
     */
    MechSummary generate(String faction, int unitType, int weightClass,
            int year, int quality, Collection<EntityMovementMode> movementModes,
            Predicate<MechSummary> filter);

    /**
     *
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
    List<MechSummary> generate(int count, String faction, int unitType, int weightClass,
            int year, int quality, Collection<EntityMovementMode> movementModes,
            Predicate<MechSummary> filter);

    /**
     * Generates the given count of units, for the given faction, using the given set of parameters.
     * Note that some of the properties of the parameters may be ignored for generation mechanisms that aren't the RAT Generator
     * @param count How many to generate
     * @param parameters data structure containing unit generation parameters
     * @return List of generated units. Empty if none generated.
     */
    List<MechSummary> generate(int count, UnitGeneratorParameters parameters);

    /**
     * Generates a single unit, for the given faction, using the given set of parameters.
     * Note that some of the properties of the parameters may be ignored for generation mechanisms that aren't the RAT Generator
     * @param parameters data structure containing unit generation parameters
     * @return Generated units. Null if none generated.
     */
    MechSummary generate(UnitGeneratorParameters parameters);

    /**
     * Generates a list of turrets given a skill level, quality and year
     * @param num How many turrets to generate
     * @param skill The skill level of the turret operator
     * @param quality The quality level of the turret
     * @param currentYear The current year
     * @return List of turrets
     */
    List<MechSummary> generateTurrets(int num, int skill, int quality, int currentYear);
}
