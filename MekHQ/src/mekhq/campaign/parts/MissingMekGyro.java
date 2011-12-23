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

import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingMekGyro extends MissingPart {
	private static final long serialVersionUID = 3420475726506139139L;
	protected int type;
    protected double gyroTonnage;

    public MissingMekGyro() {
    	this(0, 0, 0, null);
    }
    
    public MissingMekGyro(int tonnage, int type, double gyroTonnage, Campaign c) {
        super(tonnage, c);
        this.type = type;
        this.name = Mech.getGyroTypeString(type);
        this.gyroTonnage = gyroTonnage;
        this.time = 200;
        this.difficulty = 0;
    }
    
    public int getType() {
        return type;
    }
  
    @Override
    public double getTonnage() {
    	return gyroTonnage;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+type
				+"</type>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<gyroTonnage>"
				+gyroTonnage
				+"</gyroTonnage>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			int walkMP = -1;
			if (wn2.getNodeName().equalsIgnoreCase("type")) {
				type = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("gyroTonnage")) {
				gyroTonnage = Double.parseDouble(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("walkMP")) {
				walkMP = Integer.parseInt(wn2.getTextContent());
			} 
			if(walkMP > -1) {
				//need to calculate gyroTonnage for reverse compatability
		        gyroTonnage = MekGyro.getGyroTonnage(walkMP, type, getUnitTonnage());
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
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		if(part instanceof MekGyro) {
			MekGyro gyro = (MekGyro)part;
			return getType() == gyro.getType() && getTonnage() == gyro.getTonnage();
		}
		return false;
	}
	
	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public Part getNewPart() {
		return new MekGyro(getUnitTonnage(), getType(), getTonnage(), campaign);
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
		}
	}
}