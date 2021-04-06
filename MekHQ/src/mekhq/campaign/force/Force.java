/*
 * Force.java
 *
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.force;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Vector;

import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;
import mekhq.gui.enums.LayeredForceIcon;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.unit.Unit;

/**
 * This is a hierarchical object to define forces for TO&E. Each Force
 * object can have a parent force object and a vector of child force objects.
 * Each force can also have a vector of PilotPerson objects. The idea
 * is that any time TOE is refreshed in MekHQView, the force object can be traversed
 * to generate a set of TreeNodes that can be applied to the JTree showing the force
 * TO&E.
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Force implements Serializable {
    private static final long serialVersionUID = -3018542172119419401L;

    // pathway to force icon
    public static final String ROOT_LAYERED = "Layered";
    public static final int FORCE_NONE = -1;
    private String iconCategory = ROOT_LAYERED;
    private String iconFileName = AbstractIcon.DEFAULT_ICON_FILENAME;
    private LinkedHashMap<String, Vector<String>> iconMap = new LinkedHashMap<>();

    private String name;
    private Camouflage camouflage;
    private String desc;
    private boolean combatForce;
    private Force parentForce;
    private Vector<Force> subForces;
    private Vector<UUID> units;
    private Vector<Integer> oldUnits;
    private int scenarioId;

    protected UUID techId;

    //an ID so that forces can be tracked in Campaign hash
    private int id;

    public Force(String name) {
        setName(name);
        setCamouflage(new Camouflage());
        setDescription("");
        this.combatForce = true;
        this.parentForce = null;
        this.subForces = new Vector<>();
        this.units = new Vector<>();
        this.oldUnits = new Vector<>();
        this.scenarioId = -1;

        // Initialize the Force Icon
        Vector<String> frame = new Vector<>();
        frame.add("Frame.png");
        iconMap.put(LayeredForceIcon.FRAME.getLayerPath(), frame);
    }

    public Force(String n, int id, Force parent) {
        this(n);
        this.parentForce = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public Camouflage getCamouflage() {
        return camouflage;
    }

    public Camouflage getCamouflageOrElse(Camouflage camouflage) {
        return getCamouflage().hasDefaultCategory() ? camouflage : getCamouflage();
    }

    public void setCamouflage(Camouflage camouflage) {
        this.camouflage = camouflage;
    }

    public String getDescription() {
        return desc;
    }

    public void setDescription(String d) {
        this.desc = d;
    }

    public boolean isCombatForce() {
        return combatForce;
    }

    public void setCombatForce(boolean combatForce, boolean setForSubForces) {
        this.combatForce = combatForce;
        if (setForSubForces) {
            for (Force force : subForces) {
                force.setCombatForce(combatForce, true);
            }
        }
    }

    public int getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(int i) {
        this.scenarioId = i;
        for (Force sub : getSubForces()) {
            sub.setScenarioId(i);
        }
    }

    public void setTechID(UUID tech) {
        techId = tech;
    }

    public UUID getTechID() {
        return techId;
    }

    public boolean isDeployed() {
        //forces are deployed if their parent force is
        if ((null != parentForce) && parentForce.isDeployed()) {
            return true;
        }
        return scenarioId != -1;
    }

    public Force getParentForce() {
        return parentForce;
    }

    public void setParentForce(Force parent) {
        this.parentForce = parent;
    }

    public Vector<Force> getSubForces() {
        return subForces;
    }

    public boolean isAncestorOf(Force otherForce) {
        Force pForce = otherForce.getParentForce();
        while (pForce != null) {
            if (pForce.getId() == getId()) {
                return true;
            }
            pForce = pForce.getParentForce();
        }
        return false;
    }

    /**
     * @return the full hierarchical name of the force, including all parents
     */
    public String getFullName() {
        String toReturn = getName();
        if (null != parentForce) {
            toReturn += ", " + parentForce.getFullName();
        }
        return toReturn;
    }

    /**
     * Add a subforce to the subforce vector. In general, this
     * should not be called directly to add forces to the campaign
     * because they will not be assigned an id. Use {@link Campaign#addForce(Force, Force)}
     * instead
     * The boolean assignParent here is set to false when assigning forces from the
     * TOE to a scenario, because we don't want to switch this forces real parent
     * @param sub
     */
    public void addSubForce(Force sub, boolean assignParent) {
        if (assignParent) {
            sub.setParentForce(this);
        }
        subForces.add(sub);
    }

    public Vector<UUID> getUnits() {
        return units;
    }

    /**
     * @param combatForcesOnly to only include combat forces or to also include non-combat forces
     * @return all the unit ids in this force and all of its subforces
     */
    public Vector<UUID> getAllUnits(boolean combatForcesOnly) {
        Vector<UUID> allUnits;
        if (combatForcesOnly && !isCombatForce()) {
            allUnits = new Vector<>();
        } else {
            allUnits = new Vector<>(units);
        }

        for (Force f : subForces) {
            allUnits.addAll(f.getAllUnits(combatForcesOnly));
        }
        return allUnits;
    }

    /**
     * Add a unit id to the units vector. In general, this
     * should not be called directly to add unid because they will
     * not be assigned a force id. Use {@link Campaign#addUnitToForce(mekhq.campaign.unit.Unit, int)}
     * instead
     * @param uid
     */
    public void addUnit(UUID uid) {
        units.add(uid);
    }

    /**
     * This should not be directly called except by {@link Campaign#removeUnitFromForce(mekhq.campaign.unit.Unit)}
     * instead
     * @param id
     */
    public void removeUnit(UUID id) {
        int idx = 0;
        boolean found = false;
        for (UUID uid : getUnits()) {
            if (uid.equals(id)) {
                found = true;
                break;
            }
            idx++;
        }
        if (found) {
            units.remove(idx);
        }
    }

    public boolean removeUnitFromAllForces(UUID id) {
        int idx = 0;
        boolean found = false;
        for (UUID uid : getUnits()) {
            if (uid.equals(id)) {
                found = true;
                break;
            }
            idx++;
        }
        if (found) {
            units.remove(idx);
        } else {
            for (Force sub : getSubForces()) {
                found = sub.removeUnitFromAllForces(id);
                if (found) {
                    break;
                }
            }
        }
        return found;
    }

    public void clearScenarioIds(Campaign c) {
        clearScenarioIds(c, true);
    }

    public void clearScenarioIds(Campaign c, boolean killSub) {
        if (killSub) {
            for (UUID uid : getUnits()) {
                Unit u = c.getUnit(uid);
                if (null != u) {
                    u.undeploy();
                }
            }
            // We only need to clear the subForces if we're killing everything.
            for (Force sub : getSubForces()) {
                Scenario s = c.getScenario(sub.getScenarioId());
                if (s != null) {
                    s.removeForce(sub.getId());
                }
                sub.clearScenarioIds(c);
            }
        } else {
            // If we're not killing the units from the scenario, then we need to assign them with the
            // scenario ID and add them to the scenario.
            for (UUID uid : getUnits()) {
                c.getUnit(uid).setScenarioId(getScenarioId());
                c.getScenario(getScenarioId()).addUnit(uid);
            }
        }
        setScenarioId(-1);
    }

    @Override
    public String toString() {
        return name;
    }

    public int getId() {
        return id;
    }

    public void setId(int i) {
        this.id = i;
    }

    public void removeSubForce(int id) {
        int idx = 0;
        boolean found = false;
        for (Force sforce : getSubForces()) {
            if (sforce.getId() == id) {
                found = true;
                break;
            }
            idx++;
        }
        if (found) {
            subForces.remove(idx);
        }
    }

    public String getIconCategory() {
        return iconCategory;
    }

    public void setIconCategory(String s) {
        this.iconCategory = s;
    }

    public String getIconFileName() {
        return iconFileName;
    }

    public void setIconFileName(String s) {
        this.iconFileName = s;
    }

    public LinkedHashMap<String, Vector<String>> getIconMap() {
        return iconMap;
    }

    public void setIconMap(LinkedHashMap<String, Vector<String>> iconMap) {
        this.iconMap = iconMap;
    }

    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent++) + "<force id=\"" + id + "\" type=\"" + this.getClass().getName() + "\">");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "name", name);
        if (!getCamouflage().hasDefaultCategory()) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "camouflageCategory", getCamouflage().getCategory());
        }
        if (!getCamouflage().hasDefaultFilename()) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "camouflageFilename", getCamouflage().getFilename());
        }
        // TODO : Java 11 : swap to isBlank
        if (!getDescription().trim().isEmpty()) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "desc", desc);
        }
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "combatForce", combatForce);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "iconCategory", iconCategory);

        if (iconCategory.equals(Force.ROOT_LAYERED)) {
            MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "iconHashMap");
            for (Map.Entry<String, Vector<String>> entry : iconMap.entrySet()) {
                if ((entry.getValue() != null) && !entry.getValue().isEmpty()) {
                    pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<iconentry key=\"" + MekHqXmlUtil.escape(entry.getKey()) + "\">");
                    for (String value : entry.getValue()) {
                        pw1.println(MekHqXmlUtil.indentStr(indent + 2) +"<value name=\"" + MekHqXmlUtil.escape(value) + "\"/>");
                    }
                    MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent + 1, "iconentry");
                }
            }
            MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, "iconHashMap");
        }

        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "iconFileName", iconFileName);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "scenarioId", scenarioId);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "techId", techId);

        if (units.size() > 0) {
            MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "units");
            for (UUID uid : units) {
                pw1.println(MekHqXmlUtil.indentStr(indent + 1) +"<unit id=\"" + uid + "\"/>");
            }
            MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, "units");
        }

        if (subForces.size() > 0) {
            MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "subforces");
            for (Force sub : subForces) {
                sub.writeToXml(pw1, indent + 1);
            }
            MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, "subforces");
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "force");
    }

    public static Force generateInstanceFromXML(Node wn, Campaign c, Version version) {
        Force retVal = null;
        NamedNodeMap attrs = wn.getAttributes();
        Node idNameNode = attrs.getNamedItem("id");
        String idString = idNameNode.getTextContent();

        try {
            retVal = new Force("");
            NodeList nl = wn.getChildNodes();
            retVal.id = Integer.parseInt(idString);

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.setName(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("camouflageCategory")) {
                    retVal.getCamouflage().setCategory(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("camouflageFilename")) {
                    retVal.getCamouflage().setFilename(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                    retVal.setDescription(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("combatForce")) {
                    retVal.setCombatForce(Boolean.parseBoolean(wn2.getTextContent().trim()), false);
                } else if (wn2.getNodeName().equalsIgnoreCase("iconCategory")) {
                    retVal.iconCategory = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("iconHashMap")) {
                    processIconMapNodes(retVal, wn2, version);
                } else if (wn2.getNodeName().equalsIgnoreCase("iconFileName")) {
                    retVal.iconFileName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioId")) {
                    retVal.scenarioId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("techId")) {
                    retVal.techId = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("units")) {
                    processUnitNodes(retVal, wn2, version);
                } else if (wn2.getNodeName().equalsIgnoreCase("subforces")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("force")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.getLogger().error("Unknown node type not loaded in Forces nodes: " + wn3.getNodeName());
                            continue;
                        }

                        retVal.addSubForce(generateInstanceFromXML(wn3, c, version), true);
                    }
                }
            }
            c.importForce(retVal);
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.getLogger().error(ex);
        }

        return retVal;
    }

    private static void processUnitNodes(Force retVal, Node wn, Version version) {
        NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            NamedNodeMap attrs = wn2.getAttributes();
            Node classNameNode = attrs.getNamedItem("id");
            String idString = classNameNode.getTextContent();
            retVal.addUnit(UUID.fromString(idString));
        }
    }

    private static void processIconMapNodes(Force retVal, Node wn, Version version) {
        NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            NamedNodeMap attrs = wn2.getAttributes();
            Node keyNode = attrs.getNamedItem("key");
            String key = keyNode.getTextContent();
            Vector<String> values = null;
            if (wn2.hasChildNodes()) {
                values = processIconMapSubNodes(wn2, version);
            }
            retVal.iconMap.put(key, values);
        }
    }

    private static Vector<String> processIconMapSubNodes(Node wn, Version version) {
        Vector<String> values = new Vector<>();
        NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            NamedNodeMap attrs = wn2.getAttributes();
            Node keyNode = attrs.getNamedItem("name");
            String key = keyNode.getTextContent();
            if ((null != key) && !key.isEmpty()) {
                values.add(key);
            }
        }
        return values;
    }

    public Vector<Object> getAllChildren(Campaign campaign) {
        Vector<Object> children = new Vector<>(subForces);
        //add any units
        Enumeration<UUID> uids = getUnits().elements();
        //put them into a temporary array so I can sort it by rank
        List<Unit> units = new ArrayList<>();
        List<Unit> unmannedUnits = new ArrayList<>();
        while (uids.hasMoreElements()) {
            Unit u = campaign.getUnit(uids.nextElement());
            if (null != u) {
                if (null == u.getCommander()) {
                    unmannedUnits.add(u);
                } else {
                    units.add(u);
                }
            }
        }
        units.sort((u1, u2) -> ((Comparable<Integer>) u2.getCommander().getRankNumeric())
                .compareTo(u1.getCommander().getRankNumeric()));

        children.addAll(units);
        children.addAll(unmannedUnits);
        return children;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Force) && (((Force) o).getId() == id) && ((Force) o).getFullName().equals(getFullName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getFullName());
    }

    public void fixIdReferences(Map<Integer, UUID> uHash) {
        for (int oid : oldUnits) {
            UUID nid = uHash.get(oid);
            if (null != nid) {
                units.add(nid);
            }
        }
        for (Force sub : subForces) {
            sub.fixIdReferences(uHash);
        }
    }

    public int getTotalBV(Campaign c) {
        int bvTotal = 0;

        for (Force sforce : getSubForces()) {
            bvTotal += sforce.getTotalBV(c);
        }

        for (UUID id : getUnits()) {
            // no idea how this would happen, but some times a unit in a forces unit ID list has an invalid ID?
            if (c.getUnit(id) == null) {
                continue;
            }

            bvTotal += c.getUnit(id).getEntity().calculateBattleValue();
        }

        return bvTotal;
    }
}
