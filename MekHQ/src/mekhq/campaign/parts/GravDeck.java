/*
 * GravDeck.java
 *
 * Copyright (c) 2019, MegaMek team
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

import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author MKerensky
 */
public class GravDeck extends Part {

    /**
     *
     */
    private static final long serialVersionUID = -3387290388135852860L;

    static final TechAdvancement TA_GRAV_DECK = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES, DATE_ES)
            .setTechRating(RATING_B)
            .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private int deckType;
    private int deckNumber;

    public static final int GRAV_DECK_TYPE_STANDARD = 0;
    public static final int GRAV_DECK_TYPE_LARGE = 1;
    public static final int GRAV_DECK_TYPE_HUGE = 2;

    public GravDeck() {
        this(0, 0, null, GRAV_DECK_TYPE_STANDARD);
    }

    public GravDeck(int tonnage, int deckNumber, Campaign c, int deckType) {
        super(tonnage, c);
        this.deckNumber = deckNumber;
        this.deckType = deckType;
        this.name = "Grav Deck";
        if (deckType == GRAV_DECK_TYPE_STANDARD) {
            name += " (Standard)";
        } else if (deckType == GRAV_DECK_TYPE_LARGE) {
            name += " (Large)";
        } else if (deckType == GRAV_DECK_TYPE_HUGE) {
            name += " (Huge)";
        }
    }

    public int getDeckNumber() {
        return deckNumber;
    }

    public GravDeck clone() {
        GravDeck clone = new GravDeck(0, deckNumber, campaign, deckType);
        clone.copyBaseData(this);
        return clone;
    }

    public int getDeckType() {
        return deckType;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if (null != unit && unit.getEntity() instanceof Jumpship) {
            hits = ((Jumpship) unit.getEntity()).getGravDeckDamageFlag(deckNumber);

            if (checkForDestruction
                    && hits > priorHits
                    && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        if(isSalvaging()) {
            return 4800;
        }
        return 1440;
    }

    @Override
    public int getDifficulty() {
        if(isSalvaging()) {
            return 3;
        }
        return 2;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Jumpship) {
            ((Jumpship) unit.getEntity()).setGravDeckDamageFlag(deckNumber, hits);
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit && unit.getEntity() instanceof Jumpship) {
            ((Jumpship) unit.getEntity()).setGravDeckDamageFlag(deckNumber, 0);
        }
    }

    @Override
    public void remove(boolean salvage) {
        if (unit.getEntity() instanceof Jumpship) {
            ((Jumpship) unit.getEntity()).setGravDeckDamageFlag(deckNumber, 1);

            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if(!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if(null != spare) {
                spare.incrementQuantity();
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.getQuartermaster().addPart(missing, 0);
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingGravDeck(0, deckNumber, campaign, deckType);
    }

    @Override
    public String checkFixable() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        return (hits > 0);
    }

    @Override
    public Money getStickerPrice() {
        if (deckType == GRAV_DECK_TYPE_STANDARD) {
            return Money.of(5000000);
        } else if (deckType == GRAV_DECK_TYPE_LARGE) {
            return Money.of(10000000);
        } else {
            return Money.of(40000000);
        }
    }

    @Override
    public double getTonnage() {
        //TO tables p 407
        if (deckType == GRAV_DECK_TYPE_STANDARD) {
            return 50;
        } else if (deckType == GRAV_DECK_TYPE_LARGE) {
            return 100;
        } else {
            return 500;
        }
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (part instanceof GravDeck)
                && (deckType == ((GravDeck)part).deckType);
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
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_VESSEL);
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
        return TA_GRAV_DECK;
    }
}
