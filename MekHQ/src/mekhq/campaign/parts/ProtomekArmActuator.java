/*
 * ProtomekActuator.java
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
import megamek.common.EquipmentType;
import megamek.common.Protomech;
import megamek.common.TechConstants;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ProtomekArmActuator extends Part {
    private static final long serialVersionUID = 719878556021696393L;
    protected int location;

    public ProtomekArmActuator() {
        this(0, 0, null);
    }
    
    public ProtomekArmActuator clone() {
        ProtomekArmActuator clone = new ProtomekArmActuator(getUnitTonnage(), location, campaign);
        clone.copyBaseData(this);
        return clone;
    }
    
    public ProtomekArmActuator(int tonnage, Campaign c) {
        this(tonnage, -1, c);
    }
    
    public ProtomekArmActuator(int tonnage, int loc, Campaign c) {
        super(tonnage, c);
        this.name = "Protomech Arm Actuator";
        this.location = loc;
    }
    
    public void setLocation(int loc) {
        this.location = loc;
    }
    
   
    @Override
    public double getTonnage() {
        //TODO: how much do actuators weight?
        //apparently nothing
        return 0;
    }
    
    @Override
    public long getStickerPrice() {
        return getUnitTonnage() * 180;
    }

    @Override
    public boolean isSamePartType (Part part) {
        return part instanceof ProtomekArmActuator
                && getUnitTonnage() == ((ProtomekArmActuator)part).getUnitTonnage();
    }
    
    public int getLocation() {
        return location;
    }
    
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<location>"
                +location
                +"</location>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();
        
        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            
            if (wn2.getNodeName().equalsIgnoreCase("location")) {
                location = Integer.parseInt(wn2.getTextContent());
            } 
        }
    }

    @Override
    public int getAvailability(int era) {
        if(era == EquipmentType.ERA_CLAN) {
            return EquipmentType.RATING_D;
        } else {
            return EquipmentType.RATING_X;
        }
    }

    @Override
    public int getTechRating() {
        return EquipmentType.RATING_D;
    }
    
    @Override
    public void fix() {
        super.fix();
        if(null != unit) {
            unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_ARMCRIT, location);
        }
    }
    
    @Override
    public int getTechBase() {
        return T_CLAN;
    }
    
    @Override
    public int getTechLevel() {
        return TechConstants.T_CLAN_TW;
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingProtomekArmActuator(getUnitTonnage(), location, campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit) {
            int h = Math.max(1, hits);
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_ARMCRIT, location, h);
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
        location = -1;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if(null != unit) {      
        	int priorHits = hits;
            hits = unit.getEntity().getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_ARMCRIT, location);
            if(checkForDestruction 
					&& hits > priorHits 
					&& Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
				remove(false);
				return;
			}
        }
    }
    
    @Override 
	public int getBaseTime() {
		if(isSalvaging()) {
			return 120;
		}
        if(hits <= 1) {
            return 100;
        } 
        else if(hits == 2) {
            return 150;
        }
        else {
        	return 200;
        }
	}
	
	@Override
	public int getDifficulty() {
		if(isSalvaging()) {
			return 0;
		}
		if(hits <= 1) {
            return 0;
        } 
        else if(hits == 2) {
            return 1;
        }
        else {
        	return 3;
        }
	}

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }
    
    @Override
    public String getDetails() {
        if(null != unit) {
            return unit.getEntity().getLocationName(location);
        }
        return getUnitTonnage() + " tons";
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit) {
            if(hits > 0) {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_ARMCRIT, location, 1);
            } else {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_ARMCRIT, location);
            }
        }   
    }
    
    @Override
    public String checkFixable() {
    	if(null == unit) {
    		return null;
    	}
        if(isSalvaging()) {
            return null;
        }
        if(unit.isLocationBreached(location)) {
            return unit.getEntity().getLocationName(location) + " is breached.";
        }
        if(isMountedOnDestroyedLocation()) {
            return unit.getEntity().getLocationName(location) + " is destroyed.";
        }
        return null;
    }
    
    @Override
    public boolean isMountedOnDestroyedLocation() {
        return null != unit && unit.isLocationDestroyed(location);
    }
    
    @Override
    public boolean onBadHipOrShoulder() {
        return false;
    }
    
    @Override
    public boolean isPartForEquipmentNum(int index, int loc) {
        return false;//index == type && loc == location;
    }
    
    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MECH);
    }
    
    @Override
    public boolean isOmniPoddable() {
        return false;
    }

	@Override
	public String getLocationName() {
		return unit.getEntity().getLocationName(location);
	}
	
	@Override
	public int getIntroDate() {
		return 3055;
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
	public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.ACTUATOR;
    }
}
