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
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekGyro extends Part {
	private static final long serialVersionUID = 3420475726506139139L;
	protected int type;
    protected int walkMP;

    public MekGyro() {
    	this(false, 0, 0, 0);
    }
    
    public MekGyro(boolean salvage, int tonnage, int type, int walkMP) {
        super(salvage, tonnage);
        this.type = type;
        this.name = Mech.getGyroTypeString(type);
        this.walkMP = walkMP;
        computeCost();
    }
    
    public int getType() {
        return type;
    }

    public int getWalkMP() {
        return walkMP;
    }
    
   

    public static int getGyroBaseTonnage(int walkMP, int unitTonnage) {
    	return (int) Math.ceil(walkMP * unitTonnage / 100f);
    }
    
    private int getGyroBaseTonnage() {
    	return MekGyro.getGyroBaseTonnage(getWalkMP(), getUnitTonnage());
    }
    
    public static double getGyroTonnage(double gyroBaseTonnage, int gyroType) {
        if (gyroType == Mech.GYRO_XL) {
            return gyroBaseTonnage * 0.5;
        } else if (gyroType == Mech.GYRO_COMPACT) {
        	return gyroBaseTonnage * 1.5;
        } else if (gyroType == Mech.GYRO_HEAVY_DUTY) {
        	return gyroBaseTonnage * 2;
        }
    	
        return gyroBaseTonnage;
    }
    
    public static double getGyroTonnage(int walkMP, int unitTonnage, int gyroType) {
    	return MekGyro.getGyroTonnage(MekGyro.getGyroBaseTonnage(walkMP, unitTonnage), gyroType);
    }
    
    @Override
    public double getTonnage() {
    	return MekGyro.getGyroTonnage(getGyroBaseTonnage(), getType());
    }
    
    protected void computeCost() {
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
        
        this.cost = (long) Math.round(c);
    }
 
    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof MekGyro
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus())
                && getType() == ((MekGyro) part).getType()
                && getUnitTonnage() == ((MekGyro) part).getUnitTonnage();
    }

    @Override
    public int getPartType() {
        return PART_TYPE_MEK_GYRO;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+type
				+"</type>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<walkMP>"
				+walkMP
				+"</walkMP>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("type")) {
				type = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("walkMP")) {
				walkMP = Integer.parseInt(wn2.getTextContent());
			} 
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
	public void fix() {
		hits = 0;
		updateConditionFromEntity();
		if(null != unit) {
			unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
		}

	}

	@Override
	public Part getMissingPart() {
		return new MissingMekGyro(isSalvage(), getUnitTonnage(), getType(), getWalkMP());
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
			unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
			if(!salvage) {
				unit.campaign.removePart(this);
			}
			unit.removePart(this);
			Part missing = getMissingPart();
			unit.campaign.addPart(missing);
			unit.addPart(missing);
		}	
		setUnit(null);
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit) {
			hits = unit.getEntity().getHitCriticals(CriticalSlot.TYPE_SYSTEM,Mech.SYSTEM_GYRO, Mech.LOC_CT);
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
		return null;
	}
}
