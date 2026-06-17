/*
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
package mekhq.campaign.mission;

import java.io.PrintWriter;
import java.text.ParseException;

import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Missions are primarily holder objects for a set of scenarios.
 * <p>
 * The really cool stuff will happen when we subclass this into Contract
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Mission extends AbstractMission {
    private static final MMLogger LOGGER = MMLogger.create(Mission.class);

    // region Constructors
    public Mission() {
        this(null);
    }

    public Mission(final @Nullable String name) {
        setName(name);
        setSystemId("Unknown System");
    }
    // endregion Constructors

    @Override
    public int getLengthInMonths() {
        // Missions don't have durations, so we treat it as always being 1 month long. This only really matters for
        // faction standing.
        return 1;
    }

    @Override
    public void writeToXML(Campaign campaign, final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(campaign, pw, indent);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected int writeToXMLBegin(Campaign campaign, final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "mission", "id", getId(), "type", getClass());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "name", getName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", getType());
        if (getSystemId() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "systemId", getSystemId());
        } else {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "planetName", getLegacyPlanetName());
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "status", getStatus().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "desc", getDescription());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "id", getId());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "scenarios");
        for (Scenario s : getScenarios()) {
            s.writeToXML(pw, indent);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "scenarios");
        return indent;
    }

    @Override
    protected void writeToXMLEnd(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "mission");
    }

    @Override
    public void loadFieldsFromXmlNode(Campaign campaign, Version version, Node wn) throws ParseException {
        // do nothing
    }

    public static Mission generateInstanceFromXML(Node node, Campaign campaign, Version version) {
        Mission retVal = null;
        NamedNodeMap attrs = node.getAttributes();
        Node classNameNode = attrs.getNamedItem("type");
        String className = classNameNode.getTextContent();

        try {
            // Instantiate the correct child class, and call its parsing
            // function.
            retVal = (Mission) Class.forName(className).getDeclaredConstructor().newInstance();
            retVal.loadFieldsFromXmlNode(campaign, version, node);

            // Okay, now load mission-specific fields!
            NodeList nl = node.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("name")) {

                    retVal.setName(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("planetId") ||
                                 wn2.getNodeName().equalsIgnoreCase("systemId")) {
                    retVal.setSystemId(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("planetName")) {
                    PlanetarySystem system = campaign.getSystemByName(wn2.getTextContent());

                    if (system != null) {
                        retVal.setSystemId(campaign.getSystemByName(wn2.getTextContent()).getId());
                    } else {
                        retVal.setLegacyPlanetName(wn2.getTextContent());
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("status")) {
                    retVal.setStatus(MissionStatus.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("id")) {
                    retVal.setId(Integer.parseInt(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                    retVal.setDescription(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("type")) {
                    retVal.setType(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarios")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("scenario")) {
                            // Error condition of sorts!
                            // what should we do here?
                            LOGGER.error("Unknown node type not loaded in Scenario nodes: {}", wn3.getNodeName());

                            continue;
                        }
                        Scenario s = Scenario.generateInstanceFromXML(wn3, campaign, version);

                        if (null != s) {
                            retVal.addScenario(s);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("", ex);
        }

        return retVal;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return getStatus().isCompleted() ? getName() + " (Complete)" : getName();
    }
}
