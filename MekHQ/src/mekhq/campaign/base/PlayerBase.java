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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.Version;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
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

    // Populated during XML load; drained by CampaignXmlParser to reconnect persons after load.
    private transient List<UUID> pendingPersonIds = new ArrayList<>();

    /** Returns and clears the person UUIDs read from XML, for use during post-load reconnection. */
    public List<UUID> drainPendingPersonIds() {
        List<UUID> ids = new ArrayList<>(pendingPersonIds);
        pendingPersonIds.clear();
        return ids;
    }

    // Populated during XML load; drained by CampaignXmlParser to reconnect parts/units after load.
    private transient List<Part> pendingBaseWarehouseParts = new ArrayList<>();
    private transient List<Unit> pendingBaseHangarUnits = new ArrayList<>();

    /** Returns and clears the parts loaded from {@code <baseWarehouse>} XML. */
    public List<Part> drainPendingBaseWarehouseParts() {
        List<Part> result = new ArrayList<>(pendingBaseWarehouseParts);
        pendingBaseWarehouseParts.clear();
        return result;
    }

    /** Returns and clears the units loaded from {@code <baseHangar>} XML. */
    public List<Unit> drainPendingBaseHangarUnits() {
        List<Unit> result = new ArrayList<>(pendingBaseHangarUnits);
        pendingBaseHangarUnits.clear();
        return result;
    }

    /**
     * Returns the {@link FixedLocation} that anchors this base, navigating up the location tree
     * from this base's node.
     */
    public @Nullable FixedLocation getFixedLocation() {
        ILocation parent = getParent();
        return parent instanceof FixedLocation fl ? fl : null;
    }

    @Override
    public void writeToXML(PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "playerBase");
        writeBaseFieldsToXML(pw, indent);
        if (getParentLocation() != null && getParentLocation().getCurrentSystem() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "systemId",
                  getParentLocation().getCurrentSystem().getId());
        }
        // Persons who have arrived live under basePersonnel.
        for (LocationNode child : getBasePersonnel().getLocationNode().getChildren()) {
            if (child.getLocatable() instanceof Person p) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personId", p.getId().toString());
            }
        }
        // Travel nodes (CurrentLocation) sit directly under the base so they can carry
        // units and parts alongside people in the future.
        for (LocationNode child : getLocationNode().getChildren()) {
            if (child.getLocatable() instanceof CurrentLocation currentLoc) {
                currentLoc.writeToXML(pw, indent);
            }
        }
        // Spare parts stored at this base's warehouse.
        getBaseWarehouse().writeToXML(pw, indent, "baseWarehouse");
        // Units stationed at this base's hangar.
        getBaseHangar().writeToXML(pw, indent, "baseHangar");
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "playerBase");
    }

    public static @Nullable PlayerBase generateInstanceFromXML(Node wn, Campaign campaign,
          Version version) {
        try {
            String systemId = null;
            PlayerBase base = new PlayerBase();

            NodeList nl = wn.getChildNodes();
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                if (wn2.getNodeName().equalsIgnoreCase("systemId")) {
                    systemId = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("personId")) {
                    base.pendingPersonIds.add(UUID.fromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("location")) {
                    // Travel nodes sit directly under the base (not under basePersonnel).
                    CurrentLocation travelLoc = CurrentLocation.generateInstanceFromXML(wn2, campaign);
                    if (travelLoc != null) {
                        travelLoc.setParent(base);
                        campaign.addLocation(travelLoc);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("baseWarehouse")) {
                    NodeList partNodes = wn2.getChildNodes();
                    for (int i = 0; i < partNodes.getLength(); i++) {
                        Node partNode = partNodes.item(i);
                        if (partNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        Part part = Part.generateInstanceFromXML(partNode, version);
                        if (part != null) {
                            part.setCampaign(campaign);
                            base.getBaseWarehouse().addPart(part);
                            base.pendingBaseWarehouseParts.add(part);
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("baseHangar")) {
                    NodeList unitNodes = wn2.getChildNodes();
                    for (int i = 0; i < unitNodes.getLength(); i++) {
                        Node unitNode = unitNodes.item(i);
                        if (unitNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        Unit unit = Unit.generateInstanceFromXML(unitNode, version, campaign);
                        if (unit != null) {
                            base.getBaseHangar().addUnit(unit);
                            base.pendingBaseHangarUnits.add(unit);
                        }
                    }
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
            return base;

        } catch (Exception ex) {
            logger.error("", ex);
            return null;
        }
    }
}
