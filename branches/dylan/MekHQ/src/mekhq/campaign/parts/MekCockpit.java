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
            return TechConstants.T_IS_EXPERIMENTAL;
        default:
            return TechConstants.T_IS_TW_NON_BOX;
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
	public int getTechBase() {
		return T_BOTH;
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
		switch (type) {
        case Mech.COCKPIT_SMALL: 	
        case Mech.COCKPIT_TORSO_MOUNTED:
            return EquipmentType.RATING_E;
        case Mech.COCKPIT_INDUSTRIAL:
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
		setSalvaging(false);
		setUnit(null);
		updateConditionFromEntity();
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit) {
			Entity entity = unit.getEntity();
			for (int i = 0; i < entity.locations(); i++) {
				if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT, i) > 0) {
					if (entity.isSystemRepairable(Mech.SYSTEM_COCKPIT, i)) {					
						hits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT, i);	
						break;
					} else {
						remove(false);
						return;
					}
				}
			}
		}
		if(hits == 0) {
			time = 0;
			difficulty = 0;
		} 
		else {
			//TODO: These are made up values until the errata establish them
			time = 200;
			difficulty = 3;
		}
		if(isSalvaging()) {
			this.time = 300;
			this.difficulty = 0;
		}
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
				for(int i = 0; i < hits; i++) {
					unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT, hits);
				}
			}
		}
	}
	
	@Override
    public String checkFixable() {
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
		return Entity.LOC_NONE;
	}
}
