/*
 * MissingGravDeck.java
 * 
 * Copyright (c) 2019 by The MegaMek Team
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
import megamek.common.Jumpship;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

/**
 *
 * @author MKerensky
 */
public class MissingGravDeck extends MissingPart {

    /**
     * 
     */
    private static final long serialVersionUID = -6034090299851704878L;

    private int deckType;
    private int deckNumber;

    public MissingGravDeck() {
        this(0, 0, null, GravDeck.GRAV_DECK_TYPE_STANDARD);
    }

    public MissingGravDeck(int tonnage, int deckNumber, Campaign c, int deckType) {
        super(tonnage, c);
        this.deckNumber = deckNumber;
        this.deckType = deckType;
        this.name = "Grav Deck";
        if (deckType == GravDeck.GRAV_DECK_TYPE_STANDARD) {
            name += " (Standard)";
        } else if (deckType == GravDeck.GRAV_DECK_TYPE_LARGE) {
            name += " (Large)";
        } else if (deckType == GravDeck.GRAV_DECK_TYPE_HUGE) {
            name += " (Huge)";
        }
    }

    public int getDeckNumber() {
        return deckNumber;
    }

    public int getDeckType() {
        return deckType;
    }

    @Override 
    public int getBaseTime() {
        return 4800;
    }

    @Override
    public int getDifficulty() {
        return 3;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Jumpship) {
            ((Jumpship) unit.getEntity()).setGravDeckDamageFlag(deckNumber, hits);
        }
    }

    @Override
    public Part getNewPart() {
        return new GravDeck(0, 0, campaign, deckType);
    }

    @Override
    public String checkFixable() {
        return null;
    }

    @Override
    public double getTonnage() {
        //TO tables p 407
        if (deckType == GravDeck.GRAV_DECK_TYPE_STANDARD) {
            return 50;
        } else if (deckType == GravDeck.GRAV_DECK_TYPE_LARGE) {
            return 100;
        } else {
            return 500;
        }
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "deckType", deckType);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "deckNumber", deckNumber);
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("deckType")) {
                deckType = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("deckNumber")) {
                deckNumber = Integer.parseInt(wn2.getTextContent());
            }
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return (part instanceof GravDeck)
                && (refit || (((GravDeck) part).getDeckType() == deckType));
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
        return GravDeck.TA_GRAV_DECK;
    }
}