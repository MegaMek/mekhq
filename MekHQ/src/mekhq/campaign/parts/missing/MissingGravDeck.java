/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.parts.missing;

import java.io.PrintWriter;

import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.GravDeck;
import mekhq.campaign.parts.Part;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author MKerensky
 */
public class MissingGravDeck extends MissingPart {
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
    public @Nullable String checkFixable() {
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
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "deckType", deckType);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "deckNumber", deckNumber);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
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
