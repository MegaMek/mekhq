/*
 * MissingInfantryMotiveType.java
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

import megamek.common.EntityMovementMode;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingInfantryMotiveType extends MissingPart {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2454012279066776500L;
	private EntityMovementMode mode;

	public MissingInfantryMotiveType() {
    	this(0, null, null);
    }
	
	public MissingInfantryMotiveType(int tonnage, Campaign c, EntityMovementMode m) {
		super(tonnage, c);
		this.mode = m;
		if(null != mode) {
			assignName();
		}
	}
	
	private void assignName() {
		switch (mode) {
        case INF_UMU:
            name = "Scuba Gear";
            break;
        case INF_MOTORIZED:
        	name = "Motorized Vehicle";
            break;
        case INF_JUMP:
        	name = "Jump Pack";
            break;
        case HOVER:
        	name = "Hover Infantry Vehicle";
            break;
        case WHEELED:
        	name = "Wheeled Infantry Vehicle";
            break;
        case TRACKED:
        	name = "Tracked Infantry Vehicle";
            break;
        default:
        	name = "Unknown Motive Type";
		}
	}
	
	@Override
	public void updateConditionFromPart() {
		//Do nothing		
	}

	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public Part getNewPart() {
		return new InfantryMotiveType(0, campaign, mode);
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		return part instanceof InfantryMotiveType && mode.equals(((InfantryMotiveType)part).getMovementMode());
	}

	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public int getTechRating() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getAvailability(int era) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<moveMode>"
				+mode
				+"</moveMode>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);		
			if (wn2.getNodeName().equalsIgnoreCase("moveMode")) {
				mode = EntityMovementMode.getMode(wn2.getTextContent());
				assignName();
			}
		}
	}

	
}