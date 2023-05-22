/*
 * MissingJumpshipDockingCollar.java
 *
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.annotations.Nullable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.DockingCollar;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.TechAdvancement;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;

/**
 * @author MKerensky (magnusmd@hotmail.com)
 */
public class MissingJumpshipDockingCollar extends MissingPart {
    private int collarType;
    private int collarNumber;

    public MissingJumpshipDockingCollar() {
        this(0, 0, null, Jumpship.COLLAR_STANDARD);
    }

    public MissingJumpshipDockingCollar(int tonnage, int collarNumber, Campaign c, int collarType) {
        super(tonnage, c);
        this.collarNumber = collarNumber;
        this.collarType = collarType;
        this.name = "Jumpship Docking Collar";
        if (collarType == Jumpship.COLLAR_NO_BOOM) {
            name += " (Pre Boom)";
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
        if (null != unit && unit.getEntity() instanceof Jumpship) {
            DockingCollar collar = unit.getEntity().getCollarById(collarNumber);
            if (collar != null) {
                collar.setDamaged(true);
            }
        }
    }

    @Override
    public Part getNewPart() {
        return new JumpshipDockingCollar(0, 0, campaign, collarType);
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public double getTonnage() {
        return 1000;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "collarType", collarType);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "collarNumber", collarNumber);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("collarType")) {
                collarType = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("collarNumber")) {
                collarNumber = Integer.parseInt(wn2.getTextContent());
            }
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return (part instanceof JumpshipDockingCollar)
                && (refit || (((JumpshipDockingCollar) part).getCollarType() == collarType));
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
        if (collarType != Jumpship.COLLAR_NO_BOOM) {
            return JumpshipDockingCollar.TA_BOOM;
        } else {
            return JumpshipDockingCollar.TA_NO_BOOM;
        }
    }
}
