/*
 * AmmoStorage.java
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

import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import megamek.common.TargetRoll;
import mekhq.campaign.Era;
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This will be a special type of part that will only exist as spares
 * It will determine the amount of ammo of a particular type that 
 * is available
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class AmmoStorage extends EquipmentPart {
	private static final long serialVersionUID = 2892728320891712304L;

	protected long munition;
	protected int shots;
	
    public AmmoStorage() {
    	this(false, 0, null, 0);
    }
    
    public AmmoStorage(boolean salvage, int tonnage, EquipmentType et, int shots) {
        super(salvage, tonnage, et, -1);
        this.shots = shots;
        if(null != type && type instanceof AmmoType) {
        	this.munition = ((AmmoType)type).getMunitionType();
        }

    }
    
    @Override
    public double getTonnage() {
    	return shots / ((AmmoType)type).getShots();
    }

    public int getShots() {
    	return shots;
    }
    
    public void addShots(int s) {
    	shots += s;
    }
    
    public void reduceShots(int s) {
    	shots = Math.max(0, shots - s);
    }
    
	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);		
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<munition>"
				+munition
				+"</munition>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<shots>"
				+shots
				+"</shots>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("munition")) {
				munition = Long.parseLong(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("shots")) {
				shots = Integer.parseInt(wn2.getTextContent());
			}
		}
	}

	@Override
	public int getAvailability(int era) {		
		return type.getAvailability(Era.convertEra(era));
	}

	@Override
	public int getTechRating() {
		return type.getTechRating();
	}

	@Override
	public void fix() {
		//nothing to fix
		return;
	}

	@Override
	public Part getMissingPart() {
		//nothing to do here
		return null;
	}
	
	@Override
	public TargetRoll getAllMods() {
		//nothing to do here
		return null;
	}

	@Override
	public void updateConditionFromEntity() {
		//nothing to do here
		return;
	}
	
	@Override
	public void updateConditionFromPart() {
		//nothing to do here
		return;
	}

	@Override
	public boolean needsFixing() {
		return false;
	}
	
	public String getDesc() {
		String toReturn = "<html><font size='2'";
		String scheduled = "";
		if (getTeamId() != -1) {
			scheduled = " (scheduled) ";
		}
	
		toReturn += ">";
		toReturn += "<b>Reload " + getName() + "</b><br/>";
		toReturn += getDetails() + "<br/>";
		toReturn += "" + getTimeLeft() + " minutes" + scheduled;
		toReturn += "</font></html>";
		return toReturn;
	}
	
    @Override
    public String getDetails() {
    	return ((AmmoType)type).getDesc() + ", " + shots + " shots";
    }
	
	@Override
    public String checkFixable() {
        return null;
    }
}
