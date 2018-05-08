/*
 * Mission.java
 * 
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 * 
 * This file is part of MekHQ.
 * 
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planets;

/**
 * Missions are primarily holder objects for a set of scenarios.
 * 
 * The really cool stuff will happen when we subclass this into Contract
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Mission implements Serializable, MekHqXmlSerializable {

    /**
     * 
     */
    private static final long serialVersionUID = -5692134027829715149L;

    public static final int S_ACTIVE = 0;
    public static final int S_SUCCESS = 1;
    public static final int S_FAILED = 2;
    public static final int S_BREACH = 3;
    public static final int S_NUM = 4;

    private String name;
    protected String planetId;
    private int status;
    private String desc;
    private String type;
    private ArrayList<Scenario> scenarios;
    private int id = -1;
    private String legacyPlanetName;

    public Mission() {
        this(null);
    }

    public Mission(String n) {
        this.name = n;
        this.planetId = "Unknown Planet";
        this.desc = "";
        this.type = "";
        this.status = S_ACTIVE;
        scenarios = new ArrayList<Scenario>();
    }

    public static String getStatusName(int s) {

        switch (s) {
        case S_ACTIVE:
            return "Active";
        case S_SUCCESS:
            return "Success";
        case S_FAILED:
            return "Failed";
        case S_BREACH:
            return "Contract Breach";
        default:
            return "?";
        }
    }

    public String getName() {
        return name;
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

    public String getPlanetId() {
        return getPlanet().getId();
    }

    public void setPlanetId(String n) {
        this.planetId = n;
    }

    public Planet getPlanet() {
        return Planets.getInstance().getPlanetById(planetId);
    }
    
    /**
     * Convenience property to return the name of the current planet. 
     * Sometimes, the "current planet" doesn't match up with an existing planet in our planet database,
     * in which case we return whatever was stored.
     * @return
     */
    public String getPlanetName(DateTime when) {
        if(getPlanet() == null) {
            return legacyPlanetName;
        }
        
        return getPlanet().getName(when);
    }

    public String getDescription() {
        return desc;
    }

    public void setDesc(String d) {
        this.desc = d;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int s) {
        this.status = s;
    }

    public String getStatusName() {
        return getStatusName(getStatus());
    }

    public ArrayList<Scenario> getScenarios() {
        return scenarios;
    }

    /**
     * Don't use this method directly as it will not add an id to the added
     * scenario. Use Campaign#AddScenario instead
     * 
     * @param s
     */
    public void addScenario(Scenario s) {
        s.setMissionId(getId());
        scenarios.add(s);
    }

    public int getId() {
        return id;
    }

    public void setId(int i) {
        this.id = i;
    }

    public boolean isActive() {
        return status == S_ACTIVE;
    }

    public void removeScenario(int id) {
        int idx = 0;
        boolean found = false;
        for (Scenario s : getScenarios()) {
            if (s.getId() == id) {
                found = true;
                break;
            }
            idx++;
        }
        if (found) {
            scenarios.remove(idx);
        }
    }

    public void clearScenarios() {
        scenarios.clear();
    }

    public boolean hasPendingScenarios() {
        for (Scenario s : scenarios) {
            if (s.isCurrent()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        writeToXmlEnd(pw1, indent);
    }

    protected void writeToXmlBegin(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<mission id=\"" + id + "\" type=\"" + this.getClass().getName() + "\">");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<name>" + MekHqXmlUtil.escape(name) + "</name>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<type>" + MekHqXmlUtil.escape(type) + "</type>");
        
        if(planetId != null) {         
            pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<planetId>" + MekHqXmlUtil.escape(planetId) + "</planetId>");
        } else {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<planetId>" + MekHqXmlUtil.escape(legacyPlanetName) + "</planetId>");
        }
        pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<status>" + status + "</status>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<desc>" + MekHqXmlUtil.escape(desc) + "</desc>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<id>" + id + "</id>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<scenarios>");
        for (Scenario s : scenarios) {
            s.writeToXml(pw1, indent + 2);
        }
        pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "</scenarios>");
    }

    protected void writeToXmlEnd(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</mission>");
    }

    public void loadFieldsFromXmlNode(Node wn) throws ParseException {
        // do nothing
    }

    public static Mission generateInstanceFromXML(Node wn, Campaign c, Version version) {
        final String METHOD_NAME = "generateInstanceFromXML(Node,Campaign,Version)"; //$NON-NLS-1$

        Mission retVal = null;
        NamedNodeMap attrs = wn.getAttributes();
        Node classNameNode = attrs.getNamedItem("type");
        String className = classNameNode.getTextContent();

        try {
            // Instantiate the correct child class, and call its parsing
            // function.
            retVal = (Mission) Class.forName(className).newInstance();
            retVal.loadFieldsFromXmlNode(wn);

            // Okay, now load mission-specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.name = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("planetId")) {
                    retVal.planetId = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("planetName")) {
                    Planet planet = c.getPlanet(wn2.getTextContent());
                    
                    if(planet != null) {
                        retVal.planetId = c.getPlanet(wn2.getTextContent()).getId();
                    } else {
                        retVal.legacyPlanetName = wn2.getTextContent();
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("status")) {
                    retVal.status = Integer.parseInt(wn2.getTextContent());
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
                        if (wn3.getNodeType() != Node.ELEMENT_NODE)
                            continue;

                        if (!wn3.getNodeName().equalsIgnoreCase("scenario")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.getLogger().log(Mission.class, METHOD_NAME, LogLevel.ERROR,
                                    "Unknown node type not loaded in Scenario nodes: " + wn3.getNodeName()); //$NON-NLS-1$

                            continue;
                        }
                        Scenario s = Scenario.generateInstanceFromXML(wn3, c, version);

                        if (null != s) {
                            retVal.addScenario(s);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.getLogger().log(Mission.class, METHOD_NAME, ex);
        }

        return retVal;
    }

}
