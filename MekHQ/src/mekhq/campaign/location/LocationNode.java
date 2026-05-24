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

package mekhq.campaign.location;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A simple node for forming a tree of {@link ILocation} implementations.
 */
public class LocationNode {
    private static final MMLogger logger = MMLogger.create(LocationNode.class);

    private final ILocation locatable;

    LocationNode parent = null;
    Set<LocationNode> children = new HashSet<>();

    public LocationNode(ILocation locatable) {
        this.locatable = locatable;
    }

    public ILocation getLocatable() {
        return locatable;
    }

    public AbstractLocation getCurrentLocation() {
        if (locatable instanceof AbstractLocation abstractLocation) {
            return abstractLocation;
        }

        if (parent != null) {
            return parent.getCurrentLocation();
        }

        return null;
    }

    public LocationNode getParent() {
        return parent;
    }

    void setParent(LocationNode parent) {
        this.parent = parent;
    }

    /**
     * This {@code Location}'s child {@code Location}s.
     *
     * @return immutable copy of the node's children in a {@code Set}
     */
    public Set<LocationNode> getChildren() {
        return Set.copyOf(children);
    }

    void setChildren(Set<LocationNode> children) {
        this.children = children;
    }

    boolean addChild(LocationNode childLocationNode) {
        return children.add(childLocationNode);
    }

    boolean removeChild(LocationNode childLocationNode) {
        return children.remove(childLocationNode);
    }

    public void writeToXML(PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent, "locationNodeChildren");
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, indent, "locationNodeChildren");
    }

    public static void reconnectChildren(Node xmlNode, Campaign campaign) {
        NodeList nl = xmlNode.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node wn = nl.item(i);
            if (wn.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            // Unit, Person, and Part child reconnection will be added here once
            // those types implement ILocation and are serialized by writeToXML.
            logger.warn("Unrecognized locationNodeChildren element '{}' — skipping", wn.getNodeName());
        }
    }

    public static class LocationManager {

        public static void setLocation(ILocation childLocation, @Nullable ILocation parentLocation) {
            if (parentLocation != null) {
                setLocation(childLocation.getLocationNode(), parentLocation.getLocationNode());
            } else {
                setLocation(childLocation.getLocationNode(), null);
            }
        }

        public static void setLocation(LocationNode childLocationNode, @Nullable LocationNode parentLocationNode) {
            if (childLocationNode.getParent() != null) {
                childLocationNode.getParent().removeChild(childLocationNode);
            }

            childLocationNode.setParent(parentLocationNode);

            if (parentLocationNode != null) {
                parentLocationNode.addChild(childLocationNode);
            }
        }
    }
}
