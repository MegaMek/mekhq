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
public class MissingAeroSensor extends MissingPart {

	/**
	 *
	 */
	private static final long serialVersionUID = 2806921577150714477L;

	private boolean dropship;

	public MissingAeroSensor() {
    	this(0, false, null);
    }

    public MissingAeroSensor(int tonnage, boolean drop, Campaign c) {
    	super(tonnage, c);
    	this.name = "Aero Sensors";
    	this.dropship = drop;
    }

    @Override
	public int getBaseTime() {
		return 1200;
	}

	@Override
	public int getDifficulty() {
		return -2;
	}

	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public Part getNewPart() {
		return new AeroSensor(getUnitTonnage(), dropship, campaign);
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		return part instanceof AeroSensor && dropship == ((AeroSensor)part).isForDropShip()
				&& (dropship || getUnitTonnage() == part.getUnitTonnage());
	}

	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<dropship>"
				+dropship
				+"</dropship>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();

		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("dropship")) {
				if(wn2.getTextContent().trim().equalsIgnoreCase("true")) {
					dropship = true;
				} else {
					dropship = false;
				}
			}
		}
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setSensorHits(3);
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
	    return AeroSensor.TECH_ADVANCEMENT;
	}

	@Override
	public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.ELECTRONICS;
    }
}
