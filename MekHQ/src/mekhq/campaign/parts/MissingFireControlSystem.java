/*
 * MissingFireControlSystem.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;

import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.TechAdvancement;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingFireControlSystem extends MissingPart {
    private Money cost;

    public MissingFireControlSystem() {
        this(0, Money.zero(), null);
    }

    public MissingFireControlSystem(int tonnage, Money cost, Campaign c) {
        super(0, c);
        this.cost = cost;
        this.name = "Fire Control System";
    }

    @Override
    public int getBaseTime() {
        int time = 0;
        if (campaign.getCampaignOptions().useAeroSystemHits()) {
            //Test of proposed errata for repair times
            if (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship) {
                time = 1200;
                if (unit.getEntity().hasNavalC3()) {
                    time *= 2;
                }
            } else {
                time = 600;
            }
        }
        return time;
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
        return new FireControlSystem(getUnitTonnage(), cost, campaign);
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof FireControlSystem && cost.equals(part.getStickerPrice());
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setFCSHits(3);
        }
    }

    @Override
    public void writeToXML(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MHQXMLUtility.indentStr(indent+1)
                +"<cost>"
                + cost.toXmlString()
                +"</cost>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("cost")) {
                cost = Money.fromXmlString(wn2.getTextContent().trim());
            }
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
        return TA_GENERIC;
    }
}
