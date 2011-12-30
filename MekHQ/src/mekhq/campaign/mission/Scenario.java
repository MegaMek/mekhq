/*
 * Scenario.java
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
import java.util.ArrayList;
import java.util.Date;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.ForceStub;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Scenario implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2193761569359938090L;
	
	public static final int S_CURRENT  = 0;
	public static final int S_VICTORY  = 1;
	public static final int S_MVICTORY = 2;
	public static final int S_DEFEAT   = 3;
	public static final int S_MDEFEAT  = 4;
	public static final int S_DRAW     = 5;
	public static final int S_NUM      = 6;
	
	private String name;
	private String desc;
	private String report;
	private int status;
	private Date date;
	private ArrayList<Integer> subForceIds;
	private ArrayList<Integer> unitIds;
	private int id = -1;
	private int missionId;
	private ForceStub stub;
	
	public Scenario() {
		this(null);
	}
	
	public Scenario(String n) {
		this.name = n;
		this.desc = "";
		this.report = "";
		this.status = S_CURRENT;
		this.date = null;
		this.subForceIds = new ArrayList<Integer>();
		this.unitIds = new ArrayList<Integer>();
	}
	
	public static String getStatusName(int s) {
		switch(s) {
		case S_CURRENT:
			return "Pending";
		case S_VICTORY:
			return "Victory";
		case S_MVICTORY:
			return "Marginal Victory";
		case S_DEFEAT:
			return "Defeat";
		case S_MDEFEAT:
			return "Marginal Defeat";
		case S_DRAW:
			return "Draw";
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
	
	public String getReport() {
		return report;
	}
	
	public void setReport(String r) {
		this.report = r;
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
	
	public void setDate(Date d) {
		this.date = d;
	}
	
	public Date getDate() {
		return date;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int i) {
		this.id = i;
	}
	
	public int getMissionId() {
		return missionId;
	}
	
	public void setMissionId(int i) {
		this.missionId = i;
	}
	
	public Force getForces(Campaign campaign) {
		Force force = new Force("Assigned Forces");
		for(int subid : subForceIds) {
			Force sub = campaign.getForce(subid);
			if(null != sub) {
				force.addSubForce(sub, false);
			}
		}
		for(int uid : unitIds) {
			force.addUnit(uid);
		}
		return force;
	}
	
	public void addForces(int fid) {
		subForceIds.add(fid);
	}
	
	public void addUnit(int uid) {
		unitIds.add(uid);
	}
	
	public void removeUnit(int uid) {
		int idx = -1;
		for(int i = 0; i < unitIds.size(); i++) {
			if(uid == unitIds.get(i)) {
				idx = i;
				break;
			}
		}
		if(idx > -1) {
			unitIds.remove(idx);
		}
	}
	
	public void removeForce(int fid) {
		int idx = -1;
		for(int i = 0; i < subForceIds.size(); i++) {
			if(fid == subForceIds.get(i)) {
				idx = i;
				break;
			}
		}
		if(idx > -1) {
			subForceIds.remove(idx);
		}
	}
	
	public boolean isCurrent() {
		return status == S_CURRENT;
	}
	
	public void clearAllForcesAndPersonnel(Campaign campaign) {
		for(int fid : subForceIds) {
			Force f = campaign.getForce(fid);
			if(null != f) {
				f.clearScenarioIds(campaign);
			}	
		}
		for(int uid : unitIds) {
			Unit u = campaign.getUnit(uid);
			if(null != u) {
				u.undeploy();
			}
		}
		subForceIds = new ArrayList<Integer>();
		unitIds = new ArrayList<Integer>();
	}
	
	public void generateStub(Campaign c) {
		stub = new ForceStub(getForces(c), c);
	}
	
	public ForceStub getForceStub() {
		return stub;
	}
	
	public boolean isAssigned(Unit unit, Campaign campaign) {
		for(int uid : getForces(campaign).getAllUnits()) {
			if(uid == unit.getId()) {
				return true;
			}
		}
		return false;
	}
	
	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<scenario id=\""
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
				+"<report>"
				+MekHqXmlUtil.escape(report)
				+"</report>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<status>"
				+status
				+"</status>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<id>"
				+id
				+"</id>");
		if(null != stub) {
			stub.writeToXml(pw1, indent+1);
		}
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</scenario>");
		
	}
	
	public static Scenario generateInstanceFromXML(Node wn) {
		Scenario retVal = null;
		NamedNodeMap attrs = wn.getAttributes();
		Node classNameNode = attrs.getNamedItem("type");
		String className = classNameNode.getTextContent();

		try {
			// Instantiate the correct child class, and call its parsing function.
			retVal = (Scenario) Class.forName(className).newInstance();
			
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
				} else if (wn2.getNodeName().equalsIgnoreCase("report")) {
					retVal.setReport(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("forceStub")) {
					retVal.stub = ForceStub.generateInstanceFromXML(wn2);
				} 
			}
		} catch (Exception ex) {
			// Errrr, apparently either the class name was invalid...
			// Or the listed name doesn't exist.
			// Doh!
			MekHQ.logError(ex);
		}
		
		return retVal;
	}
	
}