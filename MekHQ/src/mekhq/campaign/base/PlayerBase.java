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
import mekhq.campaign.location.AcademyCampusLocation;
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
        return parent instanceof FixedLocation fixedLocation ? fixedLocation : null;
    }

    @Override
    public void writeToXML(PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "playerBase");
        writeBaseFieldsToXML(pw, indent);
        if (getCurrentLocation() != null && getCurrentLocation().getCurrentSystem() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "systemId",
                  getCurrentLocation().getCurrentSystem().getId());
        }
        // Persons who have arrived live under basePersonnel.
        for (LocationNode child : getBasePersonnel().getLocationNode().getChildren()) {
            if (child.getLocatable() instanceof Person person) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personId", person.getId().toString());
            }
        }
        // Travel nodes (CurrentLocation) and homeSchool campuses sit directly under the base.
        for (LocationNode child : getLocationNode().getChildren()) {
            if (child.getLocatable() instanceof CurrentLocation currentLocation) {
                currentLocation.writeToXML(pw, indent);
            } else if (child.getLocatable() instanceof AcademyCampusLocation campus) {
                campus.writeToXML(pw, indent);
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
            PlayerBase base = new PlayerBase();
            NodeList nodeList = wn.getChildNodes();

            // Pre-scan for systemId so we can establish the base's FixedLocation parent
            // before loading hangar units. Hangar.addUnit calls unit.setParent(hangar),
            // which requires the root of the hangar's location tree to be an AbstractLocation.
            // Without this pre-scan, canSetParent silently returns false for every unit.
            String systemId = null;
            for (int x = 0; x < nodeList.getLength(); x++) {
                Node wn2 = nodeList.item(x);
                if (wn2.getNodeType() == Node.ELEMENT_NODE
                          && wn2.getNodeName().equalsIgnoreCase("systemId")) {
                    systemId = wn2.getTextContent().trim();
                    break;
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
            if (!base.setParent(fixedLocation)) {
                logger.error("PlayerBase could not be anchored under system '{}' — skipping", systemId);
                return null;
            }

            for (int x = 0; x < nodeList.getLength(); x++) {
                Node wn2 = nodeList.item(x);
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                String nodeName = wn2.getNodeName();
                if (nodeName.equalsIgnoreCase("systemId")) {
                    // Already handled in pre-scan above.
                } else if (nodeName.equalsIgnoreCase("personId")) {
                    base.pendingPersonIds.add(UUID.fromString(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("location")) {
                    // Travel nodes sit directly under the base (not under basePersonnel).
                    CurrentLocation travelLocation = CurrentLocation.generateInstanceFromXML(wn2, campaign);
                    if (travelLocation != null) {
                        travelLocation.setParent(base);
                        campaign.addLocation(travelLocation);
                    }
                } else if (nodeName.equalsIgnoreCase("academyCampus")) {
                    AcademyCampusLocation campus = AcademyCampusLocation.generateInstanceFromXML(wn2);
                    if (campus != null) {
                        LocationNode.LocationManager.setLocation(campus, base);
                    }
                } else if (nodeName.equalsIgnoreCase("baseWarehouse")) {
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
                } else if (nodeName.equalsIgnoreCase("baseHangar")) {
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

            return base;

        } catch (Exception ex) {
            logger.error("", ex);
            return null;
        }
    }
}
