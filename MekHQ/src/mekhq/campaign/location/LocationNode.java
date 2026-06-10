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
import mekhq.campaign.CurrentLocation;
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

    @Nullable
    public AbstractLocation getNearestAbstractLocation() {
        if (locatable instanceof AbstractLocation abstractLocation) {
            return abstractLocation;
        }

        if (parent != null) {
            return parent.getNearestAbstractLocation();
        }

        return null;
    }

    @Nullable
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
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "locationNodeChildren");
        for (LocationNode child : children) {
            ILocation locatable = child.getLocatable();
            if (locatable instanceof AcademyCampusLocation campus) {
                campus.writeToXML(pw, indent);
            }
            // Future: Additional objects that need to be saved in a location-context other than the main force
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "locationNodeChildren");
    }

    /**
     * Reconnects deserialized children of a {@link Campaign} location node.
     *
     * <p>Person, Unit, and Part reconnection will be added here once those types are fully
     * serialized through the location tree.</p>
     */
    public static void reconnectChildren(Node xmlNode, Campaign campaign) {
        reconnectChildren(xmlNode, campaign, campaign);
    }

    /**
     * Reconnects deserialized children of any {@link ILocation} node.
     *
     * @param xmlNode  the {@code <locationNodeChildren>} DOM node
     * @param parent   the {@link ILocation} to attach deserialized children to
     * @param campaign the owning campaign (used for UUID-keyed lookups when needed)
     */
    public static void reconnectChildren(Node xmlNode, ILocation parent, Campaign campaign) {
        NodeList nl = xmlNode.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node wn = nl.item(i);
            try {
                if (wn.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                if (wn.getNodeName().equalsIgnoreCase("academyCampus")) {
                    AcademyCampusLocation campus = AcademyCampusLocation.generateInstanceFromXML(wn);
                    if (campus != null) {
                        LocationManager.setLocation(campus, parent);
                        NodeList campusChildren = wn.getChildNodes();
                        for (int j = 0; j < campusChildren.getLength(); j++) {
                            Node campusChild = campusChildren.item(j);
                            if (campusChild.getNodeType() != Node.ELEMENT_NODE) {
                                continue;
                            }
                            if (campusChild.getNodeName().equalsIgnoreCase("location")) {
                                CurrentLocation travelNode = CurrentLocation.generateInstanceFromXML(campusChild, campaign);
                                if (travelNode != null) {
                                    LocationManager.setLocation(travelNode, campus);
                                    campaign.addLocation(travelNode);
                                }
                            }
                        }
                    }
                } else {
                    // Person, Unit, and Part reconnection will be added here
                    logger.warn("Unrecognized locationNodeChildren element '{}' — skipping", wn.getNodeName());
                }
            } catch (Exception ex) {
                logger.error("", ex);
            }
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
