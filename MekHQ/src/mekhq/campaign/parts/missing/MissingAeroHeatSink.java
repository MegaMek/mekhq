/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AeroHeatSink;
import mekhq.campaign.parts.Part;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingAeroHeatSink extends MissingPart {
    private static final MMLogger LOGGER = MMLogger.create(MissingAeroHeatSink.class);

    private int type;

    @Deprecated(since = "0.51.0", forRemoval = true)
    public MissingAeroHeatSink() {
        this(0, Aero.HEAT_SINGLE, false, null);
    }

    public MissingAeroHeatSink(int tonnage, int type, boolean omniPodded, Campaign c) {
        super(tonnage, omniPodded, c);
        this.type = type;
        this.name = nameFor(type);
    }

    private static String nameFor(int type) {
        if (type == AeroHeatSink.CLAN_HEAT_DOUBLE) {
            return "Aero Double Heat Sink (Clan)";
        } else if (type == Aero.HEAT_DOUBLE) {
            return "Aero Double Heat Sink";
        }
        return "Aero Heat Sink";
    }

    @Override
    public int getBaseTime() {
        return isOmniPodded() ? 30 : 90;
    }

    @Override
    public int getDifficulty() {
        return -2;
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public Part getNewPart() {
        return new AeroHeatSink(getUnitTonnage(), type, omniPodded, campaign);
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof AeroHeatSink && type == ((AeroHeatSink) part).getType();
    }

    @Override
    public double getTonnage() {
        return 1;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Aero) {
            if (hits == 0) {
                ((Aero) unit.getEntity()).setHeatSinks(((Aero) unit.getEntity()).getHeatSinks() - 1);
            }
        }
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        // The heat sink type decides which spare replaces this placeholder, so it must survive a save
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", type);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList childNodes = wn.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            Node childNode = childNodes.item(index);
            if (!childNode.getNodeName().equalsIgnoreCase("type")) {
                continue;
            }
            try {
                type = Integer.parseInt(childNode.getTextContent().trim());
                name = nameFor(type);
            } catch (NumberFormatException exception) {
                LOGGER.error("Unreadable heat sink type '{}' on a missing aero heat sink; keeping {}",
                      childNode.getTextContent(), name, exception);
            }
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
        if (type == Aero.HEAT_SINGLE) {
            return AeroHeatSink.TA_SINGLE;
        } else if (type == AeroHeatSink.CLAN_HEAT_DOUBLE) {
            return AeroHeatSink.TA_CLAN_DOUBLE;
        } else {
            return AeroHeatSink.TA_IS_DOUBLE;
        }
    }

    @Override
    public boolean isOmniPoddable() {
        return true;
    }
}
