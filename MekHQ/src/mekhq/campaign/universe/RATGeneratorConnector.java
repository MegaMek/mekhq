/*
 * RATGeneratorConnector.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.UnitTable;
import megamek.common.MechSummary;
import megamek.common.UnitType;
import mekhq.MekHQ;

/**
 * Provides access to RATGenerator through IUnitGenerator interface.
 * 
 * @author Neoancient
 *
 */

public class RATGeneratorConnector implements IUnitGenerator {
	
	/* Initialize RATGenerator and load the data for the current game year */
	public RATGeneratorConnector(int year) {
		while (!RATGenerator.getInstance().isInitialized()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		RATGenerator.getInstance().loadYear(year);
	}
	
	private UnitTable findTable(String faction, int unitType, int weightClass, int year,
			int quality) {
		FactionRecord fRec = RATGenerator.getInstance().getFaction(faction);
		if (fRec == null) {
			Faction f = Faction.getFaction(faction);
			if (f != null) {
				if (f.isPeriphery()) {
					fRec = RATGenerator.getInstance().getFaction("Periphery");
				} else if (f.isClan()) {
					fRec = RATGenerator.getInstance().getFaction("CLAN");					
				} else {
					fRec = RATGenerator.getInstance().getFaction("IS");
				}
			}
			if (fRec == null) {
				MekHQ.logError("Could not locate faction record for " + faction);
				return null;
			}
		}
		String rating = null;
		if (fRec.getRatingLevels().size() != 1) {
			List<String> ratings = fRec.getRatingLevelSystem();
			rating = ratings.get(Math.min(quality, ratings.size() - 1));
		}
		ArrayList<Integer> wcs = new ArrayList<Integer>();
		if (weightClass >= 0) {
			wcs.add(weightClass);
		}
		
		return UnitTable.findTable(fRec, unitType, year, rating, wcs,
					ModelRecord.NETWORK_NONE, new ArrayList<>(), new ArrayList<>(), 2, fRec);
	}

	/* (non-Javadoc)
	 * @see mekhq.campaign.universe.IUnitGenerator#isSupportedUnitType(int)
	 */
	@Override
	public boolean isSupportedUnitType(int unitType) {
		return unitType != UnitType.GUN_EMPLACEMENT
				&& unitType != UnitType.SPACE_STATION;
	}

	/* (non-Javadoc)
	 * @see mekhq.campaign.universe.IUnitGenerator#generate(java.lang.String, int, int, int, int)
	 */
	@Override
	public MechSummary generate(String faction, int unitType, int weightClass,
			int year, int quality) {
		UnitTable ut = findTable(faction, unitType, weightClass, year, quality);
		return (ut == null)? null : ut.generateUnit();
	}

	/* (non-Javadoc)
	 * @see mekhq.campaign.universe.IUnitGenerator#generate(java.lang.String, int, int, int, int, java.util.function.Predicate)
	 */
	@Override
	public MechSummary generate(String faction, int unitType, int weightClass,
			int year, int quality, Predicate<MechSummary> filter) {
		UnitTable ut = findTable(faction, unitType, weightClass, year, quality);
		return (ut == null)? null : ut.generateUnit(ms -> filter.test(ms));
	}

	/* (non-Javadoc)
	 * @see mekhq.campaign.universe.IUnitGenerator#generate(int, java.lang.String, int, int, int, int)
	 */
	@Override
	public List<MechSummary> generate(int count, String faction, int unitType,
			int weightClass, int year, int quality) {
		UnitTable ut = findTable(faction, unitType, weightClass, year, quality);
		return ut == null? new ArrayList<MechSummary>() : ut.generateUnits(count);
	}

	/* (non-Javadoc)
	 * @see mekhq.campaign.universe.IUnitGenerator#generate(int, java.lang.String, int, int, int, int, java.util.function.Predicate)
	 */
	@Override
	public List<MechSummary> generate(int count, String faction, int unitType,
			int weightClass, int year, int quality,
			Predicate<MechSummary> filter) {
		UnitTable ut = findTable(faction, unitType, weightClass, year, quality);
		return ut == null? new ArrayList<MechSummary>() : ut.generateUnits(count, ms -> filter.test(ms));
	}
}
