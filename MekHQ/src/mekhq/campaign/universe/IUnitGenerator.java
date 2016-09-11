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

import java.util.List;
import java.util.Map;

import megamek.common.MechSummary;

/**
 * Common interface to interact with various methods for generating units.
 * 
 * @author Neoancient
 *
 */
public interface IUnitGenerator {

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
	 * @param weightClass EntityWeightClass constant
	 * @param year The year of the campaign date
	 * @param quality Index of equipment rating, with zero being the lowest quality.
	 * @return A unit that matches the criteria
	 */
	MechSummary generate(String faction, int unitType, int weightClass, int year, int quality);
	
	/**
	 * 
	 * Generates a list of units.
	 * 
	 * @param count The number of units to generate
	 * @param faction Faction shortname
	 * @param unitType UnitType constant
	 * @param weightClass EntityWeightClass constant
	 * @param year The year of the campaign date
	 * @param quality Index of equipment rating, with zero being the lowest quality.
	 * @return A list of units matching the criteria.
	 */
	List<MechSummary> generate(int count, String faction, int unitType, int weightClass,
			int year, int quality);
	
	/**
	 * Generate a unit using additional parameters specific to the generation method.
	 * 
	 * @param faction Faction shortname
	 * @param unitType UnitType constant
	 * @param weightClass EntityWeightClass constant
	 * @param year The year of the campaign date
	 * @param quality Index of equipment rating, with zero being the lowest quality.
	 * @param options A map of additional parameters keyed to the parameter name.
	 * @return A unit that matches the criteria
	 */
	default MechSummary generate(String faction, int unitType, int weightClass,
			int year, int quality, Map<String,Object> options) {
		return generate(faction, unitType, weightClass, year, quality);
	}

	/**
	 * 
	 * Generates a list of units using additional parameters specific to the generation method.
	 * 
	 * @param count The number of units to generate
	 * @param faction Faction shortname
	 * @param unitType UnitType constant
	 * @param weightClass EntityWeightClass constant
	 * @param year The year of the campaign date
	 * @param quality Index of equipment rating, with zero being the lowest quality.
	 * @param options A map of additional parameters keyed to the parameter name.
	 * @return A list of units matching the criteria.
	 */
	default List<MechSummary> generate(int count, String faction, int unitType, int weightClass,
			int year, int quality, Map<String,Object> options) {
		return generate(count, faction, unitType, weightClass, year, quality);
	}
}
