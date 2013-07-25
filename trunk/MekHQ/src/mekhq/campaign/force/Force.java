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

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;

import mekhq.MekHQ;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
    public static final String ROOT_ICON = "-- General --";
    public static final String ICON_NONE = "None";
    public static final int FORCE_NONE = -1;
    private String iconCategory = ROOT_ICON;
    private String iconFileName = ICON_NONE;

	
	private String name;
	private String desc;
	private Force parentForce;
	private Vector<Force> subForces;
	private Vector<UUID> units;
	private Vector<Integer> oldUnits;
	private int scenarioId;

	//an ID so that forces can be tracked in Campaign hash
	private int id;
	
	public Force(String n) {
		this.name = n;
		this.desc = "";
		this.parentForce = null;
		this.subForces = new Vector<Force>();
		this.units = new Vector<UUID>();
		this.oldUnits = new Vector<Integer>();
		this.scenarioId = -1;
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
		Vector<UUID> allUnits = new Vector<UUID>();
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
	 * not be assigned a force id. Use {@link Campaign#addUnitToForce(mekhq.campaign.Unit, int)}
	 * instead
	 * @param uid
	 */
	public void addUnit(UUID uid) {
		units.add(uid);
	}
	
	/**
	 * This should not be directly called except by {@link Campaign#RemoveUnitFromForce(mekhq.campaign.Unit)}
	 * instead
	 * @param id
	 */
	public void removeUnit(UUID id) {
		int idx = 0;
		boolean found = false;
		for(UUID uid : getUnits()) {
			if(uid.equals(id)) {
				found = true;
				break;
			}
			idx++;
		}
		if(found) {
			units.remove(idx);
		}
	}
	
	public boolean removeUnitFromAllForces(UUID id) {
		int idx = 0;
		boolean found = false;
		for(UUID uid : getUnits()) {
			if(uid.equals(id)) {
				found = true;
				break;
			}
			idx++;
		}
		if(found) {
			units.remove(idx);
		} else {
			for(Force sub : getSubForces()) {
				found = sub.removeUnitFromAllForces(id);
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
		for(Force sforce : getSubForces()) {
			if(sforce.getId() == id) {
				found = true;
				break;
			}
			idx++;
		}
		if(found) {
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

	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<force id=\""
				+id
				+"\" type=\""
				+this.getClass().getName()
				+"\">");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<name>"
				+MekHqXmlUtil.escape(name)
				+"</name>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<desc>"
				+MekHqXmlUtil.escape(desc)
				+"</desc>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<iconCategory>"
				+iconCategory
				+"</iconCategory>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<iconFileName>"
				+iconFileName
				+"</iconFileName>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<scenarioId>"
				+scenarioId
				+"</scenarioId>");
		if(units.size() > 0) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<units>");
			for(UUID uid : units) {
				pw1.println(MekHqXmlUtil.indentStr(indent+2)
						+"<unit id=\"" + uid + "\"/>");
			}
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"</units>");
		}
		if(subForces.size() > 0) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<subforces>");		
			for(Force sub : subForces) {
				sub.writeToXml(pw1, indent+2);
			}
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"</subforces>");
			}
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</force>");
		
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
			
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				if (wn2.getNodeName().equalsIgnoreCase("name")) {
					retVal.name = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
					retVal.desc = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("iconCategory")) {
					retVal.iconCategory = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("iconFileName")) {
					retVal.iconFileName = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("scenarioId")) {
					retVal.scenarioId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("units")) {
						processUnitNodes(retVal, wn2, version);
				} else if (wn2.getNodeName().equalsIgnoreCase("subforces")) {
					NodeList nl2 = wn2.getChildNodes();
					for (int y=0; y<nl2.getLength(); y++) {
						Node wn3 = nl2.item(y);
						// If it's not an element node, we ignore it.
						if (wn3.getNodeType() != Node.ELEMENT_NODE)
							continue;
						
						if (!wn3.getNodeName().equalsIgnoreCase("force")) {
							// Error condition of sorts!
							// Errr, what should we do here?
							MekHQ.logMessage("Unknown node type not loaded in Forces nodes: "+wn3.getNodeName());
							continue;
						}
						
						retVal.addSubForce(generateInstanceFromXML(wn3, c, version), true);
					}
				}
			}	
			c.addForceToHash(retVal);	
		} catch (Exception ex) {
			// Errrr, apparently either the class name was invalid...
			// Or the listed name doesn't exist.
			// Doh!
			MekHQ.logError(ex);
		}
		
		return retVal;
	}
	
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
		Vector<Object> children = new Vector<Object>();
		children.addAll(subForces);
		//add any units
		Enumeration<UUID> uids = getUnits().elements();
		//put them into a temporary array so I can sort it by rank
		ArrayList<Unit> units = new ArrayList<Unit>();
		ArrayList<Unit> unmannedUnits = new ArrayList<Unit>();
		while(uids.hasMoreElements()) {
			Unit u = campaign.getUnit(uids.nextElement());
			if(null != u) {
				if(null == u.getCommander()) {
					unmannedUnits.add(u);
				} else {
					units.add(u);
				}
			}
		}
		Collections.sort(units, new Comparator<Unit>(){		 
            public int compare(final Unit u1, final Unit u2) {
               return ((Comparable<Integer>)u2.getCommander().getRank()).compareTo(u1.getCommander().getRank());
            }
        });
		children.addAll(units);
		children.addAll(unmannedUnits);
		return children;
	}
	
	@Override
    public boolean equals(Object o) {
    	return o instanceof Force && ((Force)o).getId() == id && ((Force)o).getFullName().equals(getFullName());
    }
	
	public void fixIdReferences(Hashtable<Integer, UUID> uHash) {
		for(int oid : oldUnits) {
			UUID nid = uHash.get(oid);
			if(null != nid) {
				units.add(nid);
			}
		}
		for(Force sub : subForces) {
			sub.fixIdReferences(uHash);
		}
	}
	
}
