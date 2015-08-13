/*
 * TurretLock.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

package mekhq.campaign.parts;

import java.io.PrintWriter;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Tank;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;

import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class TurretLock extends Part {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TurretLock(Campaign c) {
		super(0, c);
		this.name = "Turret Lock";
	}
	
	@Override 
	public int getBaseTime() {
		return 90;
	}
	
	@Override
	public int getDifficulty() {
		return -1;
	}
	
	public TurretLock clone() {
		TurretLock clone = new TurretLock(campaign);
        clone.copyBaseData(this);
        return clone;
	}
	
	@Override
	public int getAvailability(int era) {
		return 0;
	}

	@Override
	public long getStickerPrice() {
		return 0;
	}
	
    @Override
	public int getTechLevel() {
		return TechConstants.T_ALLOWED_ALL;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_C;
	}

	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public boolean isSamePartType(Part part) {
		return part instanceof TurretLock;
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String checkFixable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void fix() {
		super.fix();
		if(null != unit && unit.getEntity() instanceof Tank) {
			((Tank)unit.getEntity()).unlockTurret();
		}
	}

	@Override
	public MissingPart getMissingPart() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(boolean salvage) {
		//nothing to do here
	}

	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		//nothing to do here because we are just going to check directly in needsFixing()
		//since this "part" can never be removed
	}

	@Override
	public void updateConditionFromPart() {
		//nothing to do here
	}

	@Override
	public boolean needsFixing() {
		if(null != unit && unit.getEntity() instanceof Tank) {
			return ((Tank)unit.getEntity()).isTurretLocked(Tank.LOC_TURRET);
		}
		return false;
	}
	
	@Override
	public boolean isSalvaging() {
		return false;
	}
	
	@Override
	public String checkScrappable() {
		return "Turret Lock is not scrappable";
	}
	
	@Override
	public boolean canNeverScrap() {
		return true;
	}

	@Override
	public String getLocationName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLocation() {
		return Entity.LOC_NONE;
	}
	
	@Override
	public int getIntroDate() {
		return EquipmentType.DATE_NONE;
	}

	@Override
	public int getExtinctDate() {
		return EquipmentType.DATE_NONE;
	}

	@Override
	public int getReIntroDate() {
		return EquipmentType.DATE_NONE;
	}
	
			
	
}