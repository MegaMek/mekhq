/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;

import megamek.common.annotations.Nullable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.TechAdvancement;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingDropshipDockingCollar extends MissingPart {

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
        if (null != unit && unit.getEntity() instanceof Dropship) {
            ((Dropship) unit.getEntity()).setDamageDockCollar(true);
            ((Dropship) unit.getEntity()).setDamageKFBoom(true);
        }

    }

    @Override
    public Part getNewPart() {
        return new DropshipDockingCollar(getUnitTonnage(), campaign, collarType);
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "collarType", collarType);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
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
