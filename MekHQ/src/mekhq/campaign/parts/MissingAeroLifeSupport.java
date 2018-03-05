/*
 * MissingAeroSensor.java
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

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingAeroLifeSupport extends MissingPart {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2806921577150714477L;

	private boolean fighter;
	private long cost;
	
	public MissingAeroLifeSupport() {
    	this(0, 0, false, null);
    }
    
	 public MissingAeroLifeSupport(int tonnage, long cost, boolean f, Campaign c) {
		 super(tonnage, c);
		 this.cost = cost;
		 this.name = "Fighter Life Support";
		 this.fighter = f;
		 if(!fighter) {
			 this.name = "Spacecraft Life Support";
		 }
	 }
	 
	 @Override 
	 public int getBaseTime() {
	     //Published errata for replacement times of small aero vs large craft
	     Entity e = unit.getEntity();
	     if (e.hasETypeFlag(Entity.ETYPE_DROPSHIP) || e.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
	         return 6720;
	     }
	     return 180;
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
		return new AeroLifeSupport(getUnitTonnage(), cost, fighter, campaign);
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		return part instanceof AeroLifeSupport && fighter == ((AeroLifeSupport)part).isForFighter()
				&& (cost == part.getStickerPrice());
	}

	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<fighter>"
				+fighter
				+"</fighter>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<cost>"
				+cost
				+"</cost>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);		
			if (wn2.getNodeName().equalsIgnoreCase("fighter")) {
				if(wn2.getTextContent().trim().equalsIgnoreCase("true")) {
					fighter = true;
				} else {
					fighter = false;
				}
			}
			else if (wn2.getNodeName().equalsIgnoreCase("cost")) {
				cost = Long.parseLong(wn2.getTextContent());
			} 
		}
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setLifeSupport(false);
		}
		
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
    
    @Override
    public TechAdvancement getTechAdvancement() {
        return AeroLifeSupport.TECH_ADVANCEMENT;
    }
	
	@Override
	public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.ELECTRONICS;
    }
}