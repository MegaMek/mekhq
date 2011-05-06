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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.MekHQApp;
import mekhq.campaign.MekHqXmlSerializable;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PilotPerson;


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
	
	public static final int S_ACTIVE  = 0;
	public static final int S_SUCCESS = 1;
	public static final int S_FAILED  = 2;
	public static final int S_BREACH  = 3;
	
	private String name;
	private int status;
	private String desc;
	private ArrayList<Scenario> scenarios;
	private int id = -1;
	
	public Mission() {
		this(null);
	}
	
	public Mission(String n) {
		this.name = n;
		this.desc = "";
		this.status = S_ACTIVE;
		scenarios = new ArrayList<Scenario>();
	}
	
	public static String getStatusName(int s) {
		
		switch(s) {
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
	 * Don't use this method directly as it will not
	 * add an id to the added scenario. Use Campaign#AddScenario instead
	 * @param s
	 */
	public void addScenario(Scenario s) {
		scenarios.add(s);
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int i) {
		this.id = i;
	}
	
	public boolean isActive() {
		return status ==S_ACTIVE;
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int inId) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<mission id=\""
				+id
				+"\" type=\""
				+this.getClass().getName()
				+"\">");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<name>"
				+name
				+"</name>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<status>"
				+status
				+"</status>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<desc>"
				+desc
				+"</desc>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<scenarios>");
		for(Scenario s : scenarios) {
			s.writeToXml(pw1, indent+2);
		}
		pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</scenarios>");
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</mission>");
		
	}
	
	public static Mission generateInstanceFromXML(Node wn) {
		Mission retVal = null;
		NamedNodeMap attrs = wn.getAttributes();
		Node classNameNode = attrs.getNamedItem("type");
		String className = classNameNode.getTextContent();

		try {
			// Instantiate the correct child class, and call its parsing function.
			retVal = (Mission) Class.forName(className).newInstance();
			
			// Okay, now load Part-specific fields!
			NodeList nl = wn.getChildNodes();
			
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				
				if (wn2.getNodeName().equalsIgnoreCase("name")) {
					retVal.name = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("status")) {
					retVal.status = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("id")) {
					retVal.id = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
					retVal.setDesc(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("scenarios")) {
					NodeList nl2 = wn2.getChildNodes();
					for (int y=0; y<nl2.getLength(); y++) {
						Node wn3 = nl2.item(y);
						// If it's not an element node, we ignore it.
						if (wn3.getNodeType() != Node.ELEMENT_NODE)
							continue;
						
						if (!wn3.getNodeName().equalsIgnoreCase("scenario")) {
							// Error condition of sorts!
							// Errr, what should we do here?
							MekHQApp.logMessage("Unknown node type not loaded in Scenario nodes: "+wn3.getNodeName());

							continue;
						}
						Scenario s = Scenario.generateInstanceFromXML(wn3);
						retVal.addScenario(s);
					}
				} 
			}
		} catch (Exception ex) {
			// Errrr, apparently either the class name was invalid...
			// Or the listed name doesn't exist.
			// Doh!
			MekHQApp.logError(ex);
		}
		
		return retVal;
	}
	
}