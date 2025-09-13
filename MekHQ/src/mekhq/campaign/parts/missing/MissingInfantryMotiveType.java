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
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Infantry;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.InfantryMotiveType;
import mekhq.campaign.parts.Part;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingInfantryMotiveType extends MissingPart {
    private EntityMovementMode mode;

    public MissingInfantryMotiveType() {
        this(0, null, null);
    }

    public MissingInfantryMotiveType(int tonnage, Campaign c, EntityMovementMode m) {
        super(tonnage, c);
        this.mode = m;
        if (null != mode) {
            assignName();
        }
    }

    @Override
    public int getBaseTime() {
        return 0;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    private void assignName() {
        switch (mode) {
            case INF_UMU:
                name = "Scuba Gear";
                break;
            case INF_MOTORIZED:
                name = "Motorized Vehicle";
                break;
            case INF_JUMP:
                name = "Jump Pack";
                break;
            case HOVER:
                name = "Hover Infantry Vehicle";
                break;
            case WHEELED:
                name = "Wheeled Infantry Vehicle";
                break;
            case TRACKED:
                name = "Tracked Infantry Vehicle";
                break;
            default:
                name = "Unknown Motive Type";
                break;
        }
    }

    @Override
    public void updateConditionFromPart() {
        //Do nothing
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public Part getNewPart() {
        return new InfantryMotiveType(0, campaign, mode);
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof InfantryMotiveType && mode.equals(((InfantryMotiveType) part).getMovementMode());
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "moveMode", mode.name());
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("moveMode")) {
                mode = EntityMovementMode.parseFromString(wn2.getTextContent().trim());
                assignName();
            }
        }
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
        return Infantry.getMotiveTechAdvancement(mode);
    }
}
