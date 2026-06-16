/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;

import java.io.PrintWriter;

import megamek.logging.MMLogger;
import mekhq.campaign.location.AcademyCampusLocation;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A fixed, non-moving location at a specific planetary system.
 *
 * <p>Unlike {@link CurrentLocation}, a {@code FixedLocation} never transits between systems —
 * it is always considered to be on-planet. This is used to represent permanent installations
 * such as academy campuses.</p>
 */
public class FixedLocation extends AbstractLocation {
    private static final MMLogger logger = MMLogger.create(FixedLocation.class);

    public FixedLocation(PlanetarySystem system) {
        super(system);
    }

    @Override
    public void processArrivals(Campaign campaign) {
        for (LocationNode child : getLocationNode().getChildren()) {
            if (child.getLocatable() instanceof AcademyCampusLocation campus) {
                campus.processArrivals(campaign);
            }
        }
    }

    @Override
    public void writeToXML(PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "fixedLocation");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "currentSystemId", currentSystem.getId());
        locationNode.writeToXML(pw, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "fixedLocation");
    }

    public static FixedLocation generateInstanceFromXML(Node wn, Campaign campaign) {
        FixedLocation returnValue = null;
        try {
            returnValue = new FixedLocation(null);
            NodeList nodeList = wn.getChildNodes();
            for (int x = 0; x < nodeList.getLength(); x++) {
                Node wn2 = nodeList.item(x);
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                if (wn2.getNodeName().equalsIgnoreCase("currentSystemId")) {
                    PlanetarySystem system = Systems.getInstance().getSystemById(wn2.getTextContent());
                    if (system == null) {
                        logger.error("Couldn't find system: {}", wn2.getTextContent());
                        system = campaign.getSystemByName("Terra");
                        if (system == null) {
                            logger.error("Couldn't find Terra fallback; using first available system");
                            system = campaign.getSystems().getFirst();
                        }
                    }
                    returnValue.currentSystem = system;
                } else if (wn2.getNodeName().equalsIgnoreCase("locationNodeChildren")) {
                    LocationNode.reconnectChildren(wn2, returnValue, campaign);
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }
        return returnValue;
    }
}
