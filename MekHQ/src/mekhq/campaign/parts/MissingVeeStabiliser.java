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

import megamek.common.Tank;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

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
    	this(0, 0, null);
    }
    
    public MissingVeeStabiliser(int tonnage, int loc, Campaign c) {
    	super(0, c);
    	this.name = "Vehicle Stabiliser";
    	this.loc = loc;
    }
    
    @Override 
	public int getBaseTime() {
		return 60;
	}
	
	@Override
	public int getDifficulty() {
		return 0;
	}
    
	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public Part getNewPart() {
		return new VeeStabiliser(getUnitTonnage(), loc, campaign);
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		return part instanceof VeeStabiliser;
	}

	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<loc>"
				+loc
				+"</loc>");
		writeToXmlEnd(pw1, indent);
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
		VeeStabiliser replacement = (VeeStabiliser)findReplacement(false);
		if(null != replacement) {
			VeeStabiliser actualReplacement = replacement.clone();
			unit.addPart(actualReplacement);
			campaign.addPart(actualReplacement, 0);
			replacement.decrementQuantity();
			actualReplacement.setLocation(loc);
			remove(false);
			//assign the replacement part to the unit			
			actualReplacement.updateConditionFromPart();
		}
	}
	
	public int getLocation() {
		return loc;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Tank) {
			((Tank)unit.getEntity()).setStabiliserHit(loc);
		}
	}

	@Override
	public String getLocationName() {
		return unit.getEntity().getLocationName(loc);
	}
	
    @Override
    public TechAdvancement getTechAdvancement() {
        return TankLocation.TECH_ADVANCEMENT;
    }
	
	
}