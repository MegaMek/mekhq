/*
 * MissingMekGyro.java
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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.team.SupportTeam;
import mekhq.campaign.work.MekGyroReplacement;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingMekGyro extends MissingPart {
	private static final long serialVersionUID = 3420475726506139139L;
	protected int type;
    protected int walkMP;

    public MissingMekGyro() {
    	this(false, 0, 0, 0);
    }
    
    public MissingMekGyro(boolean salvage, int tonnage, int type, int walkMP) {
        super(salvage, tonnage);
        this.type = type;
        this.name = Mech.getGyroTypeString(type);
        this.walkMP = walkMP;
        this.time = 200;
        this.difficulty = 0;
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
    	return MekGyro.getGyroBaseTonnage(getWalkMP(), getTonnage());
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
    
    public double getGyroTonnage() {
    	return MekGyro.getGyroTonnage(getGyroBaseTonnage(), getType());
    }
    
    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
    	if(!(task instanceof MekGyroReplacement)) {
    		return false;
    	}
    	// Gyro compatibility is based on type and gyro tonnage, not unit tonnage...
    	// But gyro tonnage isn't story, only unit tonnage and unit MP.
    	// Unit tonnage and unit walk MP are only relevant in calculating Gyro tonnage...
    	// But with type it's enough to calculate it from.
    	double unitGyroTonnage = MekGyro.getGyroTonnage((int) Math.ceil(((MekGyroReplacement)task).getUnit().getEntity().getEngine().getRating() / 100f), ((MekGyroReplacement)task).getUnit().getEntity().getGyroType());
    	
        return (((MekGyroReplacement)task).getUnit().getEntity().getGyroType() == type
                && getGyroTonnage() == unitGyroTonnage);
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
	public boolean isAcceptableReplacement(Part part) {
		if(part instanceof MekGyro) {
			MekGyro gyro = (MekGyro)part;
			return type == gyro.getType() && getGyroTonnage() == gyro.getGyroTonnage();
		}
		return false;
	}

	@Override
	public boolean isSamePartTypeAndStatus(Part part) {
		// TODO Auto-generated method stub
		return false;
	}
}