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

package mekhq.campaign;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Vector;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.MekHQApp;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PilotPerson;

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
    private String iconCategory = ROOT_ICON;
    private String iconFileName = ICON_NONE;

	
	private String name;
	private String desc;
	private Force parentForce;
	private Vector<Force> subForces;
	private Vector<Integer> personnel;
	private int scenarioId;

	//an ID so that forces can be tracked in Campaign hash
	private int id;
	
	public Force(String n) {
		this.name = n;
		this.desc = "";
		this.parentForce = null;
		this.subForces = new Vector<Force>();
		this.personnel = new Vector<Integer>();
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
	}
	
	public boolean isDeployed() {
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
	
	public Vector<Integer> getPersonnel() {
		return personnel;
	}
	
	/**
	 * This returns all the personnel ids in this force and all of its subforces
	 * @return
	 */
	public Vector<Integer> getAllPersonnel() {
		Vector<Integer> people = new Vector<Integer>();
		for(int pid : personnel) {
			people.add(pid);
		}
		for(Force f : subForces) {
			people.addAll(f.getAllPersonnel());
		}
		return people;	
	}
	
	/**
	 * Add a person id to the personnel vector. In general, this 
	 * should not be called directly to add personnel because they will
	 * not be assigned a force id. Use {@link Campaign#addPersonToForce(mekhq.campaign.personnel.Person, int)}
	 * instead
	 * @param pid
	 */
	public void addPerson(int pid) {
		personnel.add(pid);
	}
	
	/**
	 * This should not be directly called except by {@link Campaign#RemovePersonFromForce(mekhq.campaign.personnel.Person)}
	 * instead
	 * @param id
	 */
	public void removePerson(int id) {
		int idx = 0;
		boolean found = false;
		for(int pid : getPersonnel()) {
			if(pid == id) {
				found = true;
				break;
			}
			idx++;
		}
		if(found) {
			personnel.remove(idx);
		}
	}
	
	public boolean removePersonFromAllForces(int id) {
		int idx = 0;
		boolean found = false;
		for(int pid : getPersonnel()) {
			if(pid == id) {
				found = true;
				break;
			}
			idx++;
		}
		if(found) {
			personnel.remove(idx);
		} else {
			for(Force sub : getSubForces()) {
				found = sub.removePersonFromAllForces(id);
				if(found) {
					break;
				}
			}
		} 
		return found;
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
				+name
				+"</name>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<desc>"
				+desc
				+"</desc>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<iconCategory>"
				+iconCategory
				+"</iconCategory>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<iconFileName>"
				+iconFileName
				+"</iconFileName>");
		if(personnel.size() > 0) {
			//for now I am just going to print person ids to xml
			//TODO: change personnel to a vector of ids rather than persons
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<personnel>");
			for(int pid : personnel) {
				pw1.println(MekHqXmlUtil.indentStr(indent+2)
						+"<person id=\"" + pid + "\"/>");
			}
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"</personnel>");
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
	
	public static Force generateInstanceFromXML(Node wn, Campaign c) {
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
				} else if (wn2.getNodeName().equalsIgnoreCase("personnel")) {
						processPersonnelNodes(retVal, wn2);
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
							MekHQApp.logMessage("Unknown node type not loaded in Forces nodes: "+wn3.getNodeName());
							continue;
						}
						
						retVal.addSubForce(generateInstanceFromXML(wn3, c), true);
					}
				}
			}	
			c.addForceToHash(retVal);	
		} catch (Exception ex) {
			// Errrr, apparently either the class name was invalid...
			// Or the listed name doesn't exist.
			// Doh!
			MekHQApp.logError(ex);
		}
		
		return retVal;
	}
	
	private static void processPersonnelNodes(Force retVal, Node wn) {
	
		NodeList nl = wn.getChildNodes();
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;
			NamedNodeMap attrs = wn2.getAttributes();
			Node classNameNode = attrs.getNamedItem("id");
			String idString = classNameNode.getTextContent();
			retVal.addPerson(Integer.parseInt(idString));			
		}
	}
	
}