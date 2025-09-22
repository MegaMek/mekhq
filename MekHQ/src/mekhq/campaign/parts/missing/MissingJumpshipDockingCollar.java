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
import megamek.common.equipment.DockingCollar;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.JumpshipDockingCollar;
import mekhq.campaign.parts.Part;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
