/*
 * MissingDropshipDockingCollar.java
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

import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingDropshipDockingCollar extends MissingPart {

	/**
	 *
	 */
	private static final long serialVersionUID = -717866644605314883L;

	private int collarType;

	public MissingDropshipDockingCollar() {
    	this(0, null, Dropship.COLLAR_STANDARD);
    }

    public MissingDropshipDockingCollar(int tonnage, Campaign c, int collarType) {
        super(tonnage, c);
        this.collarType = collarType;
        this.name = "Dropship Docking Collar";
        if (collarType == Dropship.COLLAR_NO_BOOM) {
            name += " (No Boom)";
        } else if (collarType == Dropship.COLLAR_PROTOTYPE) {
            name += " (Prototype)";
        }
    }

    @Override
	public int getBaseTime() {
		return 2880;
	}

	@Override
	public int getDifficulty() {
		return -2;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Dropship) {
            ((Dropship)unit.getEntity()).setDamageDockCollar(true);
            ((Dropship)unit.getEntity()).setDamageKFBoom(true);
		}

	}

	@Override
	public Part getNewPart() {
		return new DropshipDockingCollar(getUnitTonnage(), campaign, collarType);
	}

	@Override
	public String checkFixable() {
		return null;
	}


	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "collarType", collarType);
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("collarType")) {
                collarType = Integer.parseInt(wn2.getTextContent());
            }
        }
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		return (part instanceof DropshipDockingCollar)
		        && (refit || (((DropshipDockingCollar) part).getCollarType() == collarType));
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
        if (collarType != Dropship.COLLAR_NO_BOOM) {
            return DropshipDockingCollar.TA_BOOM;
        } else {
            return DropshipDockingCollar.TA_NO_BOOM;
        }
	}

}
