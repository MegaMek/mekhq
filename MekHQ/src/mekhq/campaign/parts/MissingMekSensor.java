/*
 * MissingMekSensor.java
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

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import mekhq.campaign.Campaign;

import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingMekSensor extends MissingPart {
	private static final long serialVersionUID = 931907976883324097L;

	public MissingMekSensor() {
		this(0, null);
	}
	
	public MissingMekSensor(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Mech Sensors";
    }
	
	@Override 
	public int getBaseTime() {
		return 260;
	}
	
	@Override
	public int getDifficulty() {
		return 0;
	}
	
	@Override
	public double getTonnage() {
		//TODO: what should this tonnage be?
		return 0;
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		// Do nothing - no fields to load.
	}

	@Override
	public int getAvailability(int era) {
		return EquipmentType.RATING_C;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_C;
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		return part instanceof MekSensor && getUnitTonnage() == part.getUnitTonnage();
	}
	
	@Override
    public String checkFixable() {
        for(int i = 0; i < unit.getEntity().locations(); i++) {
        	if(unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, i) > 0) {
            	if(unit.isLocationBreached(i)) {
            		return unit.getEntity().getLocationName(i) + " is breached.";
            	}
            	if(unit.isLocationDestroyed(i)) {
            		return unit.getEntity().getLocationName(i) + " is destroyed.";
            	}
            	
            }
        }
        return null;
    }

	@Override
	public Part getNewPart() {
		return new MekSensor(getUnitTonnage(), campaign);
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS);
		}
	}

	@Override
	public String getLocationName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLocation() {
		if(null != unit) {
			Entity entity = unit.getEntity();
			for (int i = 0; i < entity.locations(); i++) {
				if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, i) > 0) {
					return i;
				}
			}
		}
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
	
	@Override
    public boolean isInLocation(String loc) {
		 if(null == unit || null == unit.getEntity() || !(unit.getEntity() instanceof Mech)) {
			 return false;
		 }
		 if (unit.getEntity().getLocationFromAbbr(loc) == Mech.LOC_HEAD) {
             return true;
         }
		 if(((Mech)unit.getEntity()).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
     		if(unit.getEntity().getLocationFromAbbr(loc) == Mech.LOC_CT) {
     			return true;
     		}
		 }
		 return false;	
    }

}
