/*
 * MekCockpit.java
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
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekCockpit extends Part {
	private static final long serialVersionUID = -1989526319692474127L;

	private int type;
	
	public MekCockpit() {
		this(0, Mech.COCKPIT_STANDARD, null);
	}
	
	public MekCockpit(int tonnage, int t, Campaign c) {
        super(tonnage, c);
        this.type = t;
        this.name = Mech.getCockpitDisplayString(type);
    }
	
	public MekCockpit clone() {
		MekCockpit clone = new MekCockpit(getUnitTonnage(), type, campaign);
        clone.copyBaseData(this);
		return clone;
	}
	
	@Override
	public double getTonnage() {
		switch (type) {
        case Mech.COCKPIT_SMALL:
            return 2;
        case Mech.COCKPIT_TORSO_MOUNTED:
            return 4;
        default:
            return 3;
		}
	}
	
	@Override
	public long getStickerPrice() {
		switch (type) {
        case Mech.COCKPIT_COMMAND_CONSOLE:
            return 500000;
        case Mech.COCKPIT_SMALL:
            return 175000;
        case Mech.COCKPIT_TORSO_MOUNTED:
            return 750000;
        case Mech.COCKPIT_STANDARD:
            return 200000;
        case Mech.COCKPIT_INDUSTRIAL:
            return 100000;
        default:
            return 200000;
		}
	}

	@Override
	public int getTechLevel() {
		switch (type) {
        case Mech.COCKPIT_COMMAND_CONSOLE:
            return TechConstants.T_IS_ADVANCED;
        case Mech.COCKPIT_TORSO_MOUNTED:
        case Mech.COCKPIT_SUPERHEAVY:
		case Mech.COCKPIT_SUPERHEAVY_TRIPOD:
		case Mech.COCKPIT_TRIPOD:
		case Mech.COCKPIT_INTERFACE:
			//TODO: some of these depend on clan/IS
            return TechConstants.T_IS_EXPERIMENTAL;
        default:
            return TechConstants.T_ALLOWED_ALL;
		}
	}
	
    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof MekCockpit 
        		&& ((MekCockpit)part).getType() == type;
    }
    
    public int getType() {
    	return type;
    }
    
	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+type
				+"</type>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("type")) {
				type = Integer.parseInt(wn2.getTextContent());
			}
		}
	}

	@Override
	public int getAvailability(int era) {
		//TODO: change once we add DA era 
		switch (type) {
        case Mech.COCKPIT_COMMAND_CONSOLE:
        	if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_C;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_F;
			} else {
				return EquipmentType.RATING_E;
			}
        case Mech.COCKPIT_SMALL:
        	if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_X;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_X;
			} else {
				return EquipmentType.RATING_E;
			}
        case Mech.COCKPIT_TORSO_MOUNTED:
        	if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_X;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_X;
			} else {
				return EquipmentType.RATING_F;
			}
        case Mech.COCKPIT_INDUSTRIAL:
        	if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_B;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_C;
			} else {
				return EquipmentType.RATING_C;
			}
        default:
            return EquipmentType.RATING_C;
		}
	}

	@Override
	public int getTechRating() {
		switch(type) {
		case Mech.COCKPIT_SMALL:
		case Mech.COCKPIT_INTERFACE:
		case Mech.COCKPIT_SUPERHEAVY:
		case Mech.COCKPIT_SUPERHEAVY_TRIPOD:
		case Mech.COCKPIT_TRIPOD:
            return EquipmentType.RATING_E;
		case Mech.COCKPIT_INDUSTRIAL:
		case Mech.COCKPIT_PRIMITIVE_INDUSTRIAL:
            return EquipmentType.RATING_C;		
		default:
			return EquipmentType.RATING_D;
		}
	}

	@Override
	public void fix() {
		super.fix();
		if(null != unit) {
			unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT);
		}
	}

	@Override
	public MissingPart getMissingPart() {
		return new MissingMekCockpit(getUnitTonnage(), type, campaign);
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
			unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT);
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
		int priorHits = hits;
		if(null != unit) {
			Entity entity = unit.getEntity();
			for (int i = 0; i < entity.locations(); i++) {
				if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT, i) > 0) {
					//check for missing equipment as well
					if (!unit.isSystemMissing(Mech.SYSTEM_COCKPIT, i)) {					
						hits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT, i);	
						break;
					} else {
						remove(false);
						return;
					}
				}
			}
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
			return 300;
		}
		//TODO: These are made up values until the errata establish them
		return 200;
	}
	
	@Override
	public int getDifficulty() {
		if(isSalvaging()) {
			return 0;
		}
		//TODO: These are made up values until the errata establish them
		return 3;
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}
	
	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			if(hits == 0) {
				unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT);
			} else {
				unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT, hits);
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
        for(int i = 0; i < unit.getEntity().locations(); i++) {
            if(unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT, i) > 0) {
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
			 if(unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT, i) > 0
					 && unit.isLocationDestroyed(i)) {
				 return true;
			 }
		 }
		return false;
	}
	
	 @Override
	 public boolean isPartForEquipmentNum(int index, int loc) {
		 return Mech.SYSTEM_COCKPIT == index;
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
		if(type == Mech.COCKPIT_TORSO_MOUNTED) {
			return Mech.LOC_CT;
		} else {
			return Mech.LOC_HEAD;
		}
	}

    @Override
	public int getIntroDate() {
    	//TODO: where are aerospace cockpits
    	//TODO: differentiate clan for some designs
		switch(type) {
		case Mech.COCKPIT_STANDARD:
			return 2468;
		case Mech.COCKPIT_SMALL:
			return 3060;
		case Mech.COCKPIT_COMMAND_CONSOLE:
			return 2625;
		case Mech.COCKPIT_TORSO_MOUNTED:
		case Mech.COCKPIT_DUAL:
			return 3053;
		case Mech.COCKPIT_INDUSTRIAL:
			return 2469;
		case Mech.COCKPIT_PRIMITIVE:
			return 2430;
		case Mech.COCKPIT_PRIMITIVE_INDUSTRIAL:
			return 2300;
		case Mech.COCKPIT_SUPERHEAVY:
			return 3060;
		case Mech.COCKPIT_SUPERHEAVY_TRIPOD:
			return 3130;
		case Mech.COCKPIT_TRIPOD:
			return 2590;
		case Mech.COCKPIT_INTERFACE:
			return 3074;
		default:
			return EquipmentType.DATE_NONE;
		}
	}

	@Override
	public int getExtinctDate() {
		switch(type) {
		case Mech.COCKPIT_PRIMITIVE:
			return 2520;
		case Mech.COCKPIT_PRIMITIVE_INDUSTRIAL:
			return 2520;
		case Mech.COCKPIT_COMMAND_CONSOLE:
			return 2850;
		default:
			return EquipmentType.DATE_NONE;
		}
	}

	@Override
	public int getReIntroDate() {
		switch(type) {
		case Mech.COCKPIT_COMMAND_CONSOLE:
			return 3030;
		default:
			return EquipmentType.DATE_NONE;
		}
	}
    
}
