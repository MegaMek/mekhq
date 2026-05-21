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
package mekhq.campaign.base;

import java.io.PrintWriter;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A player-defined base at a fixed planetary system.
 *
 * <p>{@code PlayerBase} extends {@link AbstractBase} and is anchored under a
 * {@link FixedLocation}. All common fields (id, displayName, displayType, planetId) are
 * managed by the parent class.</p>
 */
public class PlayerBase extends AbstractBase {

    public PlayerBase(ILocation parentLocation) {
        super(parentLocation);
    }

    /** For XML deserialization only. */
    private PlayerBase() {
        super();
    }

    /**
     * Returns the {@link FixedLocation} that anchors this base, navigating up the location tree
     * from this base's node.
     */
    public @Nullable FixedLocation getFixedLocation() {
        ILocation parent = getParentLocation();
        return parent instanceof FixedLocation fl ? fl : null;
    }

    @Override
    public void writeToXML(PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "playerBase");
        writeBaseFieldsToXML(pw, indent);
        if (getLocation() != null && getLocation().getCurrentSystem() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "systemId",
                  getLocation().getCurrentSystem().getId());
        }
        getLocationNode().writeToXML(pw, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "playerBase");
    }

    public static @Nullable PlayerBase generateInstanceFromXML(Node wn, Campaign campaign) {
        try {
            String systemId = null;
            Node childrenNode = null;
            PlayerBase base = new PlayerBase();

            NodeList nl = wn.getChildNodes();
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                if (wn2.getNodeName().equalsIgnoreCase("systemId")) {
                    systemId = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("locationNodeChildren")) {
                    childrenNode = wn2;
                } else {
                    readBaseFieldFromXML(base, wn2);
                }
            }

            if (systemId == null) {
                logger.error("PlayerBase XML missing systemId — skipping");
                return null;
            }

            PlanetarySystem system = Systems.getInstance().getSystemById(systemId);
            if (system == null) {
                logger.error("PlayerBase could not find system '{}' — skipping", systemId);
                return null;
            }

            FixedLocation fixedLocation = new FixedLocation(system);
            base.setParent(fixedLocation);
            if (childrenNode != null) {
                LocationNode.reconnectChildren(childrenNode, base, campaign);
            }
            return base;

        } catch (Exception ex) {
            logger.error("", ex);
            return null;
        }
    }
}
