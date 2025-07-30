/*
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
package mekhq.campaign.mission;

import java.io.PrintWriter;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
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
public class Mission {
    private static final MMLogger logger = MMLogger.create(Mission.class);

    // region Variable Declarations
    private String name;
    protected String systemId;
    private MissionStatus status;
    private String desc;
    private String type;
    private List<Scenario> scenarios;
    private int id = -1;
    private String legacyPlanetName;
    // endregion Variable Declarations

    // region Constructors
    public Mission() {
        this(null);
    }

    public Mission(final @Nullable String name) {
        this.name = name;
        this.systemId = "Unknown System";
        this.desc = "";
        this.type = "";
        this.status = MissionStatus.ACTIVE;
        scenarios = new ArrayList<>();
    }
    // endregion Constructors

    public String getName() {
        return name;
    }

    /**
     * Returns the name of this object as an HTML hyperlink.
     *
     * <p>The hyperlink is formatted with a "MISSION:" protocol prefix followed by the object's ID. This allows UI
     * components that support HTML to render the name as a clickable link, which can be used to navigate to or focus on
     * this specific object when clicked.</p>
     *
     * @return An HTML formatted string containing the object's name as a hyperlink with its ID
     *
     * @author Illiani
     * @since 0.50.05
     */
    public String getHyperlinkedName() {
        return String.format("<a href='MISSION:%s'>%s</a>", getId(), getName());
    }

    public void setName(String n) {
        this.name = n;
    }

    public String getType() {
        return type;
    }

    public void setType(String t) {
        this.type = t;
    }

    public String getSystemId() {
        return getSystem().getId();
    }

    public void setSystemId(String n) {
        this.systemId = n;
    }

    public PlanetarySystem getSystem() {
        return Systems.getInstance().getSystemById(systemId);
    }

    /**
     * Convenience property to return the name of the current planet. Sometimes, the "current planet" doesn't match up
     * with an existing planet in our planet database, in which case we return whatever was stored.
     *
     * @return
     */
    public String getSystemName(LocalDate when) {
        if (getSystem() == null) {
            return legacyPlanetName;
        }

        return getSystem().getName(when);
    }

    public void setLegacyPlanetName(String name) {
        legacyPlanetName = name;
    }

    public String getDescription() {
        return desc;
    }

    public void setDesc(String d) {
        this.desc = d;
    }

    public MissionStatus getStatus() {
        return status;
    }

    public void setStatus(MissionStatus status) {
        this.status = status;
    }

    public boolean isActiveOn(LocalDate date) {
        return isActiveOn(date, false);
    }

    public boolean isActiveOn(LocalDate date, boolean excludeEndDateCheck) {
        return getStatus().isActive();
    }

    // region Scenarios
    public List<Scenario> getScenarios() {
        return scenarios;
    }

    public List<Scenario> getVisibleScenarios() {
        return getScenarios().stream().filter(scenario -> !scenario.isCloaked()).collect(Collectors.toList());
    }

    public List<Scenario> getCurrentScenarios() {
        return getScenarios().stream()
                     .filter(scenario -> scenario.getStatus().isCurrent())
                     .collect(Collectors.toList());
    }

    public List<AtBScenario> getCurrentAtBScenarios() {
        return getScenarios().stream()
                     .filter(scenario -> scenario.getStatus().isCurrent() && (scenario instanceof AtBScenario))
                     .map(scenario -> (AtBScenario) scenario)
                     .collect(Collectors.toList());
    }

    public List<Scenario> getCompletedScenarios() {
        return getScenarios().stream()
                     .filter(scenario -> !scenario.getStatus().isCurrent())
                     .collect(Collectors.toList());
    }

    /**
     * Don't use this method directly as it will not add an id to the added scenario. Use Campaign#AddScenario instead
     *
     * @param scenario the scenario to add this this mission
     */
    public void addScenario(final Scenario scenario) {
        scenario.setMissionId(getId());
        getScenarios().add(scenario);
    }

    public void clearScenarios() {
        scenarios.clear();
    }

    public boolean hasPendingScenarios() {
        // scenarios that are pending, but have not been revealed don't count
        return getScenarios().stream()
                     .anyMatch(scenario -> (scenario.getStatus().isCurrent() && !scenario.isCloaked()));
    }
    // endregion Scenarios

    public int getId() {
        return id;
    }

    public void setId(int i) {
        this.id = i;
    }

    /**
     * Returns the contract length in months.
     *
     * @return the number and corresponding length of the contract in months as an integer
     */
    public int getLength() {
        // Missions don't have durations, so we treat it as always being 1 month long. This only really matters for
        // faction standing.
        return 1;
    }

    // region File I/O

    /**
     * @deprecated use {@link #writeToXML(Campaign, PrintWriter, int) instead}
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public void writeToXML(final PrintWriter pw, int indent) {
        return;
    }

    public void writeToXML(Campaign campaign, final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(campaign, pw, indent);
        writeToXMLEnd(pw, indent);
    }

    /**
     * @deprecated use {@link #writeToXMLBegin(Campaign, PrintWriter, int)} instead;
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    protected int writeToXMLBegin(final PrintWriter pw, int indent) {
        return indent;
    }

    protected int writeToXMLBegin(Campaign campaign, final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "mission", "id", id, "type", getClass());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "name", name);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", type);
        if (systemId != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "systemId", systemId);
        } else {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "planetName", legacyPlanetName);
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "status", status.name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "desc", desc);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "id", id);
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "scenarios");
        for (Scenario s : scenarios) {
            s.writeToXML(pw, indent);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "scenarios");
        return indent;
    }

    protected void writeToXMLEnd(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "mission");
    }

    /**
     * @deprecated use {@link #loadFieldsFromXmlNode(Campaign, Version, Node)}  instead;
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public void loadFieldsFromXmlNode(Node wn) throws ParseException {
        return;
    }

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
            retVal = (Mission) Class.forName(className).newInstance();
            retVal.loadFieldsFromXmlNode(campaign, version, node);

            // Okay, now load mission-specific fields!
            NodeList nl = node.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.name = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("planetId") ||
                                 wn2.getNodeName().equalsIgnoreCase("systemId")) {
                    retVal.systemId = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("planetName")) {
                    PlanetarySystem system = campaign.getSystemByName(wn2.getTextContent());

                    if (system != null) {
                        retVal.systemId = campaign.getSystemByName(wn2.getTextContent()).getId();
                    } else {
                        retVal.legacyPlanetName = wn2.getTextContent();
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("status")) {
                    retVal.setStatus(MissionStatus.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("id")) {
                    retVal.id = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                    retVal.setDesc(wn2.getTextContent());
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
                            // Errr, what should we do here?
                            logger.error("Unknown node type not loaded in Scenario nodes: " + wn3.getNodeName());

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
            logger.error("", ex);
        }

        return retVal;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return getStatus().isCompleted() ? name + " (Complete)" : name;
    }
}
