/*
 * KFDriveCoil.java
 * 
 * Copyright (c) 2019, The MegaMek Team
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

import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;

import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Jumpship;
import megamek.common.LandAirMech;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author MKerensky
 */
public class KFDriveCoil extends Part {

	/**
     * 
     */
    private static final long serialVersionUID = 4515211961051281110L;

    public KFDriveCoil() {
    	this(0, null);
    }
    
    public KFDriveCoil(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "K-F Drive Coil";
    }
        
    public KFDriveCoil clone() {
    	KFDriveCoil clone = new KFDriveCoil(0, campaign);
        clone.copyBaseData(this);
    	return clone;
    }
    
	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		int priorHits = hits;
		if(null != unit) {
		    if (unit.getEntity() instanceof Jumpship) {
    			if(((Jumpship)unit.getEntity()).getKFDriveCoilHit()) {
    				hits = 1;
    			} else {
    				hits = 0;
    			}
		    }
			if(checkForDestruction 
					&& hits > priorHits 
					&& Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
				remove(false);
			}
		}
	}
	
	@Override 
	public int getBaseTime() {
	    int time;
		if(isSalvaging()) {
		    //SO KF Drive times, p184-5
			time = 28800;
		} else {
		    time = 4800;
		}
		return time;
	}
	
	@Override
	public int getDifficulty() {
	    //SO Difficulty Mods
		if(isSalvaging()) {
			return 2;
		}
		return 5;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Jumpship) {
		        ((Jumpship)unit.getEntity()).setKFDriveCoilHit(needsFixing());
		}
	}

	@Override
	public void fix() {
		super.fix();
		if (null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setGearHit(false);
		} else if (null != unit && unit.getEntity() instanceof LandAirMech) {
            unit.repairSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_LANDING_GEAR);
		}
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
		    if (unit.getEntity() instanceof Aero) {
		        ((Aero)unit.getEntity()).setGearHit(true);
		    } else if (unit.getEntity() instanceof LandAirMech) {
		        unit.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_LANDING_GEAR, 3);
		    }
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
	public MissingPart getMissingPart() {
		return new MissingLandingGear(getUnitTonnage(), campaign);
	}

	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}

	@Override
	public Money getStickerPrice() {
		return Money.of(10.0 * getUnitTonnage());
	}

	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public int getTechRating() {
		//go with conventional fighter avionics
		return EquipmentType.RATING_B;
	}

	@Override
	public boolean isSamePartType(Part part) {
		return part instanceof KFDriveCoil;
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		//nothing to load
	}
	
	@Override
	public boolean isRightTechType(String skillType) {
        if (unit != null && unit.getEntity() instanceof LandAirMech) {
            return skillType.equals(SkillType.S_TECH_MECH);
        }
        return (skillType.equals(SkillType.S_TECH_AERO) || skillType.equals(SkillType.S_TECH_VESSEL));
	}

    @Override
    public String getLocationName() {
        if (null != unit) {
            return unit.getEntity().getLocationName(unit.getEntity().getBodyLocation());
        }
        return null;
    }

    @Override
    public int getLocation() {
        if (null != unit) {
            return unit.getEntity().getBodyLocation();
        }
        return Entity.LOC_NONE;
    }
    
	@Override
	public TechAdvancement getTechAdvancement() {
	    return TA_GENERIC;
	}
	
}
