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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.ForceStub;
import mekhq.campaign.unit.Unit;

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
	private ArrayList<UUID> unitIds;
	private int id = -1;
	private int missionId;
	private ForceStub stub;
	
	//allow multiple loot objects for meeting different mission objectives
	private ArrayList<Loot> loots;
	
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
		this.unitIds = new ArrayList<UUID>();
		this.loots = new ArrayList<Loot>();
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
		for(UUID uid : unitIds) {
			force.addUnit(uid);
		}
		return force;
	}
	
	public void addForces(int fid) {
		subForceIds.add(fid);
	}
	
	public void addUnit(UUID uid) {
		unitIds.add(uid);
	}
	
	public void removeUnit(UUID uid) {
		int idx = -1;
		for(int i = 0; i < unitIds.size(); i++) {
			if(uid.equals(unitIds.get(i))) {
				idx = i;
				break;
			}
		}
		if(idx > -1) {
			unitIds.remove(idx);
		}
	}
	
	public void removeForce(int fid) {
		//int idx = -1;
		ArrayList<Integer> toRemove = new ArrayList<Integer>();
		for(int i = 0; i < subForceIds.size(); i++) {
			if(fid == subForceIds.get(i)) {
				//idx = i;
				//break;
				toRemove.add(subForceIds.get(i));
			}
		}
		//if(idx > -1) {
		//	subForceIds.remove(idx);
		//}
		subForceIds.removeAll(toRemove);
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
		for(UUID uid : unitIds) {
			Unit u = campaign.getUnit(uid);
			if(null != u) {
				u.undeploy();
			}
		}
		subForceIds = new ArrayList<Integer>();
		unitIds = new ArrayList<UUID>();
	}
	
	public void generateStub(Campaign c) {
		stub = new ForceStub(getForces(c), c);
	}
	
	public ForceStub getForceStub() {
		return stub;
	}
	
	public boolean isAssigned(Unit unit, Campaign campaign) {
		for(UUID uid : getForces(campaign).getAllUnits()) {
			if(uid.equals(unit.getId())) {
				return true;
			}
		}
		return false;
	}
	
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		writeToXmlEnd(pw1, indent);
	}
	
	protected void writeToXmlBegin(PrintWriter pw1, int indent) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
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
		if(loots.size() > 0 && status == S_CURRENT) {
		    pw1.println(MekHqXmlUtil.indentStr(indent+1)+"<loots>");
		    for(Loot l : loots) {
		        l.writeToXml(pw1, indent+2);     
		    }
		    pw1.println(MekHqXmlUtil.indentStr(indent+1)+"</loots>");
		}
		if(null != date) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<date>"
					+df.format(date)
					+"</date>");
		}
	}
	
	protected void writeToXmlEnd(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</scenario>");		
	}
	
	protected void loadFieldsFromXmlNode(Node wn) throws ParseException {
		//do nothing
	}
	
	public static Scenario generateInstanceFromXML(Node wn, Campaign c, Version version) {
		Scenario retVal = null;
		NamedNodeMap attrs = wn.getAttributes();
		Node classNameNode = attrs.getNamedItem("type");
		String className = classNameNode.getTextContent();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		
		try {
			// Instantiate the correct child class, and call its parsing function.
			retVal = (Scenario) Class.forName(className).newInstance();
			retVal.loadFieldsFromXmlNode(wn);
			
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
				} else if (wn2.getNodeName().equalsIgnoreCase("date")) {
					retVal.date = df.parse(wn2.getTextContent().trim());
				} else if (wn2.getNodeName().equalsIgnoreCase("loots")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y=0; y<nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        
                        if (!wn3.getNodeName().equalsIgnoreCase("loot")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.logMessage("Unknown node type not loaded in techUnitIds nodes: "+wn3.getNodeName());
                            continue;
                        }               
                        Loot loot = Loot.generateInstanceFromXML(wn3, c, version);
                        retVal.loots.add(loot);
                    }
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
	
	public ArrayList<Loot> getLoot() {
	    return loots;
	}
	
	public void addLoot(Loot l) {
	    loots.add(l);
	}
	
	public void resetLoot() {
		loots = new ArrayList<Loot>();
	}
	
}