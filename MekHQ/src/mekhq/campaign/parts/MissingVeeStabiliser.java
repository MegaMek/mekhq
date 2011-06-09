/*
 * MissingVeeStabiliser.java
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

import megamek.common.Entity;
import megamek.common.EquipmentType;
import mekhq.campaign.MekHqXmlUtil;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingVeeStabiliser extends MissingPart {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2806921577150714477L;
	private int loc;
	
	public MissingVeeStabiliser() {
    	this(0, 0);
    }
    
    public MissingVeeStabiliser(int tonnage, int loc) {
    	super(0);
    	this.time = 60;
    	this.difficulty = 0;
    	this.name = "Vehicle Stabiliser";
    	this.loc = loc;
    }
    
	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public Part getNewPart() {
		return new VeeStabiliser(getUnitTonnage(), Entity.LOC_NONE);
	}

	@Override
	public long getPurchasePrice() {
		return 0;
	}

	@Override
	public boolean isAcceptableReplacement(Part part) {
		return part instanceof VeeStabiliser;
	}

	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_C;
	}

	@Override
	public int getAvailability(int era) {
		return EquipmentType.RATING_C;
	}
	
	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<loc>"
				+loc
				+"</loc>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("loc")) {
				loc = Integer.parseInt(wn2.getTextContent());
			}
		}
	}
	
	@Override 
	public void fix() {
		VeeStabiliser replacement = (VeeStabiliser)findReplacement();
		replacement.setLocation(loc);
		if(null != replacement) {
			unit.addPart(replacement);
			remove(false);
			//assign the replacement part to the unit			
			replacement.updateConditionFromPart();
		}
	}
	
	public int getLocation() {
		return loc;
	}
	
}