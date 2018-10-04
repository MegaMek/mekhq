/*
 * Force.java
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

package mekhq.campaign.force;

import static org.apache.commons.text.StringEscapeUtils.escapeXml10;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.unit.Unit;
import mekhq.util.ForceIconId;
import mekhq.util.dom.DomProcessor;

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

    public static final int FORCE_NONE = -1;

    public Force(String name) {
        this.name = name;
        this.desc = "";
        this.parentForce = null;
        this.subForces = new Vector<>();
        this.units = new Vector<>();
        this.oldUnits = new Vector<>();
        this.scenarioId = -1;
    }

    private String name;
    private String desc;
    private Force parentForce;
    private Vector<Force> subForces;
    private Vector<UUID> units;
    private Vector<Integer> oldUnits;
    private int scenarioId;
    private ForceIconId forceIconId; // nullable
    private UUID techId;

    //an ID so that forces can be tracked in Campaign hash
    private int id;

    public String getName() {
        return name;
    }
    
    public void setName(String n) {
        this.name = n;
    }
    
    public String getDescription() {
        return desc;
    }
    
    public void setDescription(String d) {
        this.desc = d;
    }
    
    public int getScenarioId() {
        return scenarioId;
    }
    
    public void setScenarioId(int i) {
        this.scenarioId = i;
        for(Force sub : getSubForces()) {
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
        if(null != parentForce && parentForce.isDeployed()) {
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
        boolean isAncestor = false;
        Force pForce = otherForce.getParentForce();
        while(!isAncestor && pForce != null) {
            if(pForce.getId() == getId()) {
                return true;
            }
            pForce = pForce.getParentForce();
        }
        return isAncestor;
    }
    
    /**
     * This returns the full hierarchical name of the force, including all parents
     * @return
     */
    public String getFullName() {
        String toReturn = getName();
        if(null != parentForce) {
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
        if(assignParent) {
            sub.setParentForce(this);
        }
        subForces.add(sub);
    }
    
    public Vector<UUID> getUnits() {
        return units;
    }
    
    /**
     * This returns all the unit ids in this force and all of its subforces
     * @return
     */
    public Vector<UUID> getAllUnits() {
        Vector<UUID> allUnits = new Vector<>();
        for(UUID uid : units) {
            allUnits.add(uid);
        }
        for(Force f : subForces) {
            allUnits.addAll(f.getAllUnits());
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
     * @param unitId
     */
    public void removeUnit(UUID unitId) {
        int idx = 0;
        boolean found = false;
        for(UUID uid : getUnits()) {
            if(uid.equals(unitId)) {
                found = true;
                break;
            }
            idx++;
        }
        if(found) {
            units.remove(idx);
        }
    }
    
    public boolean removeUnitFromAllForces(UUID unitId) {
        int idx = 0;
        boolean found = false;
        for(UUID uid : getUnits()) {
            if(uid.equals(unitId)) {
                found = true;
                break;
            }
            idx++;
        }
        if(found) {
            units.remove(idx);
        } else {
            for(Force sub : getSubForces()) {
                found = sub.removeUnitFromAllForces(unitId);
                if(found) {
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
            for(UUID uid : getUnits()) {
                Unit u = c.getUnit(uid);
                if(null != u) {
                    u.undeploy();
                }
            }
            // We only need to clear the subForces if we're killing everything.
            for(Force sub : getSubForces()) {
                Scenario s = c.getScenario(sub.getScenarioId());
                if (s != null) {
                    s.removeForce(sub.getId());
                }
                sub.clearScenarioIds(c);
            }
        } else {
            // If we're not killing the units from the scenario, then we need to assign them with the
            // scenario ID and add them to the scenario.
            for(UUID uid : getUnits()) {
                c.getUnit(uid).setScenarioId(getScenarioId());
                c.getScenario(getScenarioId()).addUnit(uid);
            }
        }
        setScenarioId(-1);
    }

    public int getId() {
        return id;
    }
    
    public void setId(int i) {
        this.id = i;
    }
    
    public void removeSubForce(int forceId) {
        int idx = 0;
        boolean found = false;
        for(Force sforce : getSubForces()) {
            if(sforce.getId() == forceId) {
                found = true;
                break;
            }
            idx++;
        }
        if(found) {
            subForces.remove(idx);
        }
    }

    public Optional<ForceIconId> getForceIconId() {
        return Optional.ofNullable(forceIconId);
    }

    public void setForceIconId(Optional<ForceIconId> forceIconId) {
        this.forceIconId = forceIconId.orElse(null);
    }

    public void setForceIconId(ForceIconId forceIconId) {
        this.forceIconId = forceIconId;
    }

    @SuppressWarnings("nls")
    public void printXML(PrintWriter out, int indent) {
        String indent0 = MekHqXmlUtil.indentStr(indent);
        String indent1 = MekHqXmlUtil.indentStr(indent + 1);
        
        out.println(indent0 + "<force id=\"" + id + "\" type=\"" + this.getClass().getName() + "\">");
        out.println(indent1 + "<name>" + escapeXml10(name) + "</name>");
        out.println(indent1 + "<desc>" + escapeXml10(desc) + "</desc>");

        if (forceIconId != null) {
            forceIconId.printXML(out, indent + 1);
        }

        out.println(indent1 + "<scenarioId>" + scenarioId + "</scenarioId>");
        if (techId != null) {
            out.println(indent1 + "<techId>" + techId.toString() + "</techId>");
        }
        if (!units.isEmpty()) {
            out.println(indent1 + "<units>");
            for (UUID uid : units) {
                out.println(MekHqXmlUtil.indentStr(indent + 2) + "<unit id=\"" + uid + "\"/>");
            }
            out.println(indent1 + "</units>");
        }
        if (!subForces.isEmpty()) {
            out.println(indent1 + "<subforces>");
            for (Force sub : subForces) {
                sub.printXML(out, indent + 2);
            }
            out.println(indent1 + "</subforces>");
        }
        out.println(indent0 + "</force>");

    }
    
    @SuppressWarnings("nls")
    public static Force generateInstanceFromXML(Node wn, Campaign c, Version version) {
        final String METHOD_NAME = "generateInstanceFromXML(Node,Campaign,Version)";
        try {
            Element root = (Element) wn;
            
            DomProcessor p = DomProcessor.at(root);

            Force force = new Force(p.text("name", null));

            force.id         = Integer.parseInt(root.getAttribute("id"));
            force.desc       = p.text("desc", "");
            force.scenarioId = p.text("scenarioId", Integer::parseInt, -1);
            force.techId     = p.text("techId", UUID::fromString, null);
            
            p.uniqueChildElement("units").ifPresent(units -> {
                processUnitNodes(force, units, version);
            });

            p.uniqueChildElement("subforces").ifPresent(subforces -> {

                NodeList nl2 = subforces.getChildNodes();
                for (int y=0; y<nl2.getLength(); y++) {
                    Node wn3 = nl2.item(y);
                    // If it's not an element node, we ignore it.
                    if (wn3.getNodeType() != Node.ELEMENT_NODE)
                        continue;
                    
                    if (!wn3.getNodeName().equalsIgnoreCase("force")) {
                        // Error condition of sorts!
                        // Errr, what should we do here?
                        MekHQ.getLogger().log(Force.class, METHOD_NAME, LogLevel.ERROR,
                                "Unknown node type not loaded in Forces nodes: " + wn3.getNodeName()); //$NON-NLS-1$
                        continue;
                    }
                    
                    force.addSubForce(generateInstanceFromXML(wn3, c, version), true);
                }
            });

            force.forceIconId = ForceIconId.fromXML((Element) wn).orElse(null);

            c.addForceToHash(force);
            
            return force;
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.getLogger().error(Force.class, METHOD_NAME, ex);
            return null;
        }
    }
    
    @SuppressWarnings("nls")
    private static void processUnitNodes(Force retVal, Node wn, Version version) {
    
        NodeList nl = wn.getChildNodes();
        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeType() != Node.ELEMENT_NODE)
                continue;
            NamedNodeMap attrs = wn2.getAttributes();
            Node classNameNode = attrs.getNamedItem("id");
            String idString = classNameNode.getTextContent();
            if(version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
                retVal.oldUnits.add(Integer.parseInt(idString));
            } else {
                retVal.addUnit(UUID.fromString(idString));    
            }
        }
    }
    
    public Vector<Object> getAllChildren(Campaign campaign) {
        Vector<Object> children = new Vector<>();
        children.addAll(subForces);
        //add any units
        Enumeration<UUID> uids = getUnits().elements();
        //put them into a temporary array so I can sort it by rank
        ArrayList<Unit> mannedUnits = new ArrayList<>();
        ArrayList<Unit> unmannedUnits = new ArrayList<>();
        while(uids.hasMoreElements()) {
            Unit u = campaign.getUnit(uids.nextElement());
            if(null != u) {
                if(null == u.getCommander()) {
                    unmannedUnits.add(u);
                } else {
                    mannedUnits.add(u);
                }
            }
        }
        Collections.sort(mannedUnits, Comparator.comparing(u -> u.getCommander().getRankNumeric()));

        children.addAll(mannedUnits);
        children.addAll(unmannedUnits);
        return children;
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

    @Override
    public int hashCode() {
        return 123311 * id + name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Force other = (Force) obj;
        return id == other.id
            && name.equals(other.name)
            && Objects.equals(parentForce, other.parentForce);
    }

    @Override
    public String toString() {
        return name;
    }

    /** @deprecated with no replacement (this is a magic value for "no image", use null or Optional.empty() or something) */
    @Deprecated
    public static final String ROOT_LAYERED = "Layered"; //$NON-NLS-1$

    /** @deprecated with no replacement (this is a magic value for "no image", use null or Optional.empty() or something) */
    @Deprecated
    public static final String ICON_NONE = "None"; //$NON-NLS-1$

}
