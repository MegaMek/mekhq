/*
 * MekLifeSupport.java
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

import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekLifeSupport extends Part {
	private static final long serialVersionUID = -1989526319692474127L;

	public MekLifeSupport() {
		this(0, null);
	}
	
	public MekLifeSupport(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Mech Life Support System";
    }
	
	public MekLifeSupport clone() {
		MekLifeSupport clone = new MekLifeSupport(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
		return clone;
	}
	
	@Override
	public double getTonnage() {
		//TODO: what should this tonnage be?
		return 0;
	}
	
	@Override
	public long getStickerPrice() {
		return 50000;
	}

    @Override
    public boolean isSamePartType(Part part) {

        return part instanceof MekLifeSupport;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		writeToXmlEnd(pw1, indent);
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
	public int getTechLevel() {
		return TechConstants.T_ALLOWED_ALL;
	}
	
	@Override
	public void fix() {
		super.fix();
		if(null != unit) {
			unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT);
		}
	}

	@Override
	public MissingPart getMissingPart() {
		return new MissingMekLifeSupport(getUnitTonnage(), campaign);
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
			unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT);
			Part spare = campaign.checkForExistingSparePart(this);
			if(!salvage) {
				campaign.removePart(this);
			} else if(null != spare) {
				spare.incrementQuantity();
				campaign.removePart(this);
			}
			unit.removePart(this);
			Part missing = getMissingPart();
			unit.addPart(missing);
			campaign.addPart(missing, 0);
		}
		setUnit(null);
		updateConditionFromEntity(false);
	}

	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		if(null != unit) {
			int priorHits = hits;
			Entity entity = unit.getEntity();
			for (int i = 0; i < entity.locations(); i++) {
				if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, i) > 0) {
					if (!unit.isSystemMissing(Mech.SYSTEM_LIFE_SUPPORT, i)) {					
						hits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, i);	
						break;
					} else {
						remove(false);
						return;
					}
				}
			}
			if(checkForDestruction 
					&& hits > priorHits && hits >= 2
					&& Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
				remove(false);
				return;
			}
		}
	}
	
	@Override 
	public int getBaseTime() {
		if(isSalvaging()) {
			return 180;
		}
		if(hits > 1) {
			return 120;
		}
		return 60;
	}
	
	@Override
	public int getDifficulty() {
		if(isSalvaging()) {
			return -1;
		}
		if(hits > 1) {
			return 1;
		}
		return 0;
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}
	
	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			if(hits == 0) {
				unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT);
			} else {
				unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, hits);
			}
		}
	}
	
	@Override
    public String checkFixable() {
		if(isSalvaging()) {
			return null;
		}
        for(int i = 0; i < unit.getEntity().locations(); i++) {
            if(unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, i) > 0) {
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
	public boolean isMountedOnDestroyedLocation() {
		if(null == unit) {
			return false;
		}
		for(int i = 0; i < unit.getEntity().locations(); i++) {
			 if(unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, i) > 0
					 && unit.isLocationDestroyed(i)) {
				 return true;
			 }
		 }
		return false;
	}
	
	@Override
	public boolean isPartForEquipmentNum(int index, int loc) {
		return Mech.SYSTEM_LIFE_SUPPORT == index;
	}
	
	@Override
	public boolean isRightTechType(String skillType) {
		return skillType.equals(SkillType.S_TECH_MECH);
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
				if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, i) > 0) {
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
		 if(((Mech)unit.getEntity()).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
			 if(unit.getEntity().getLocationFromAbbr(loc) == Mech.LOC_LT
					 || unit.getEntity().getLocationFromAbbr(loc) == Mech.LOC_RT) {
				 return true;
			 }
		 } else if (unit.getEntity().getLocationFromAbbr(loc) == Mech.LOC_HEAD) {
             return true;
         }
		 return false;	
    }
	
}
