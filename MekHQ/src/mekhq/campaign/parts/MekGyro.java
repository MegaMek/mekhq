/*
 * MekGyro.java
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
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekGyro extends Part {
	private static final long serialVersionUID = 3420475726506139139L;
	protected int type;
	protected double gyroTonnage;

    public MekGyro() {
    	this(0, 0, 0, null);
    }
    
    public MekGyro(int tonnage, int type, int walkMP, Campaign c) {
        super(tonnage, c);
        this.type = type;
        this.name = Mech.getGyroTypeString(type);
        this.gyroTonnage = MekGyro.getGyroTonnage(walkMP, type, getUnitTonnage());
    }
    
    public MekGyro(int tonnage, int type, double gyroTonnage, Campaign c) {
    	super(tonnage, c);
        this.type = type;
        this.name = Mech.getGyroTypeString(type);
        this.gyroTonnage = gyroTonnage;
    }
    
    public MekGyro clone() {
    	MekGyro clone = new MekGyro(getUnitTonnage(), type, gyroTonnage, campaign);
        clone.copyRepairData(this);
    	return clone;
    }
    
    public int getType() {
        return type;
    }

    public static int getGyroBaseTonnage(int walkMP, int unitTonnage) {
    	return (int) Math.ceil(walkMP * unitTonnage / 100f);
    }
    
    public static double getGyroTonnage(int walkMP, int gyroType, int unitTonnage) {
    	int gyroBaseTonnage = MekGyro.getGyroBaseTonnage(walkMP, unitTonnage);
        if (gyroType == Mech.GYRO_XL) {
            return gyroBaseTonnage * 0.5;
        } else if (gyroType == Mech.GYRO_COMPACT) {
        	return gyroBaseTonnage * 1.5;
        } else if (gyroType == Mech.GYRO_HEAVY_DUTY) {
        	return gyroBaseTonnage * 2;
        }
    	
        return gyroBaseTonnage;
    }
    
    @Override
    public double getTonnage() {
    	return gyroTonnage;
    }
    
    @Override
    public long getStickerPrice() {
        double c = 0;
        
        if (getType() == Mech.GYRO_XL) {
            c = 750000 * getTonnage();
        } else if (getType() == Mech.GYRO_COMPACT) {
            c = 400000 * getTonnage();
        } else if (getType() == Mech.GYRO_HEAVY_DUTY) {
            c = 500000 * getTonnage();
        } else {
            c = 300000 * getTonnage();
        }
        
        return (long) Math.round(c);
    }
 
    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof MekGyro
                && getType() == ((MekGyro) part).getType()
                && getTonnage() == ((MekGyro) part).getTonnage();
    }

    @Override
    public int getPartType() {
        return PART_TYPE_MEK_GYRO;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+type
				+"</type>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<gyroTonnage>"
				+gyroTonnage
				+"</gyroTonnage>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		int walkMP = -1;
		int uTonnage = 0;
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
					
			if (wn2.getNodeName().equalsIgnoreCase("type")) {
				type = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("gyroTonnage")) {
				gyroTonnage = Double.parseDouble(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("walkMP")) {
				walkMP = Integer.parseInt(wn2.getTextContent());
			} else if(wn2.getNodeName().equalsIgnoreCase("unitTonnage")) {
				uTonnage = Integer.parseInt(wn2.getTextContent());
			}
		}
		if(gyroTonnage == 0) {
			//need to calculate gyroTonnage for reverse compatability
	        gyroTonnage = MekGyro.getGyroTonnage(walkMP, type, uTonnage);
		}
	}

	@Override
	public int getAvailability(int era) {
		switch(type) {
		case Mech.GYRO_COMPACT:
		case Mech.GYRO_HEAVY_DUTY:
		case Mech.GYRO_XL:
			if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_X;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_X;
			} else {
				return EquipmentType.RATING_E;
			}
		default:
			return EquipmentType.RATING_C;	
		}
	}

	@Override
	public int getTechRating() {
		switch(type) {
		case Mech.GYRO_COMPACT:
		case Mech.GYRO_HEAVY_DUTY:
		case Mech.GYRO_XL:
			return EquipmentType.RATING_E;
		default:
			return EquipmentType.RATING_D;	
		}
	}
	
	@Override
	public int getTechLevel() {
		return TechConstants.T_IS_TW_ALL;
	}
	
	@Override 
	public int getTechBase() {
		switch(type) {
		case Mech.GYRO_COMPACT:
		case Mech.GYRO_HEAVY_DUTY:
		case Mech.GYRO_XL:
			return T_IS;
		default:
			return T_BOTH;	
		}
	}

	@Override
	public void fix() {
		super.fix();
		if(null != unit) {
			unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
		}

	}

	@Override
	public Part getMissingPart() {
		return new MissingMekGyro(getUnitTonnage(), getType(), getTonnage(), campaign);
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
			unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
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
			campaign.addPart(missing);
		}	
		setSalvaging(false);
		setUnit(null);
		updateConditionFromEntity();
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit) {
			if(!unit.getEntity().isSystemRepairable(Mech.SYSTEM_GYRO, Mech.LOC_CT)) {
				remove(false);
				return;
			}
			hits = unit.getEntity().getDamagedCriticals(CriticalSlot.TYPE_SYSTEM,Mech.SYSTEM_GYRO, Mech.LOC_CT);
		}
		if(hits == 0) {
			time = 0;
			difficulty = 0;
		}
		else if(hits == 1) {
			time = 120;
			difficulty = 1;
		} 
		else if(hits == 2) {
			time = 240;
			difficulty = 4;
		}
		else if(hits > 2) {
			remove(false);
		}
		if(isSalvaging()) {
			this.time = 200;
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
				unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
			} else {
				for(int i = 0; i < hits; i++) {
					unit.hitSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
				}
			}
		}
	}
	
	@Override
	public String checkFixable() {
		if(!isSalvaging() && unit.isLocationBreached(Mech.LOC_CT)) {
    		return unit.getEntity().getLocationName(Mech.LOC_CT) + " is breached.";
		}
		return null;
	}
	
	@Override
	public boolean isPartForCriticalSlot(int index, int loc) {
		return Mech.SYSTEM_GYRO == index;
	}
	
	 @Override
	 public boolean isRightTechType(String skillType) {
		 return skillType.equals(SkillType.S_TECH_MECH);
	 }
}
