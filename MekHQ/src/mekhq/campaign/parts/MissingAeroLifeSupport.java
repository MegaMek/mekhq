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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;

import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingAeroLifeSupport extends MissingPart {
    private static final long serialVersionUID = 2806921577150714477L;

    private boolean fighter;
    private Money cost;

    public MissingAeroLifeSupport() {
        this(0, Money.zero(), false, null);
    }

     public MissingAeroLifeSupport(int tonnage, Money cost, boolean f, Campaign c) {
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
         int time = 0;
         if (campaign.getCampaignOptions().useAeroSystemHits()) {
             //Test of proposed errata for repair times
             if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                 time = 1200;
             } else {
                 time = 180;
             }
             return time;
         }

         //Published errata for replacement times of small aero vs large craft
         if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
             time = 6720;
         } else {
             time = 180;
         }
         return time;
     }

    @Override
    public int getDifficulty() {
        //Published errata for replacement times of small aero vs large craft
        if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
            return 0;
        } else {
            return -1;
        }
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
                && (cost.equals(part.getStickerPrice()));
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
                + cost.toXmlString()
                +"</cost>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("fighter")) {
                fighter = wn2.getTextContent().trim().equalsIgnoreCase("true");
            } else if (wn2.getNodeName().equalsIgnoreCase("cost")) {
                cost = Money.fromXmlString(wn2.getTextContent().trim());
            }
        }
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero)unit.getEntity()).setLifeSupport(false);
        }

    }

    @Override
    public String getLocationName() {
        if (null != unit) {
            return unit.getEntity().getLocationName(unit.getEntity().getBodyLocation());
        }
        return null;
    }

    @Override
    public int getLocation() {
        if (null != unit) {
            return unit.getEntity().getBodyLocation();
        }
        return Entity.LOC_NONE;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return AeroLifeSupport.TECH_ADVANCEMENT;
    }

    @Override
    public PartRepairType getMassRepairOptionType() {
        return PartRepairType.ELECTRONICS;
    }
}
