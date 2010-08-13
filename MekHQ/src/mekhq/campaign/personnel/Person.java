/*
 * Person.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

package mekhq.campaign.personnel;

import java.io.PrintWriter;
import java.io.Serializable;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Pilot;
import mekhq.MekHQApp;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlSerializable;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.team.SupportTeam;
import mekhq.campaign.work.PersonnelWorkItem;

/**
 * This is an abstract class for verious types of personnel
 * The personnel types themselves will be various wrappers for 
 * 1) pilots (including tank crews)
 * 2) large aero crews (because they can double as teams)
 * 3) support teams
 * 4) infantry squads/platoons (including BA)
 * 5) Administrators/other staff?
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class Person implements Serializable, MekHqXmlSerializable {
	private static final long serialVersionUID = -847642980395311152L;
	protected int id;
    //any existing work item for this person
    protected PersonnelWorkItem task;
    protected int taskId;
    //days of rest
    protected int daysRest;
    protected boolean deployed;
    protected String biography;
    protected String portraitCategory;
    protected String portraitFile;

    protected int xp;
    
    //default constructor
    public Person() {
        daysRest = 0;
        portraitCategory = Pilot.ROOT_PORTRAIT;
        portraitFile = Pilot.PORTRAIT_NONE;
        xp = 0;
    }
    
    public abstract void reCalc();
    public abstract String getDesc();
    public abstract String getDescHTML();

    public String getPortraitCategory() {
        return portraitCategory;
    }

    public String getPortraitFileName() {
        return portraitFile;
    }
    
    public void setPortraitCategory(String s) {
        this.portraitCategory = s;
    }

    public void setPortraitFileName(String s) {
        this.portraitFile = s;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
    
    public void setTask(PersonnelWorkItem task) {
        this.task = task;
    }
    
    public PersonnelWorkItem getTask() {
        return task;
    }
    
    public int getTaskId() {
    	return taskId;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }
    
    public SupportTeam getTeamAssigned() {
        if(null == task) {
            return null;
        }
        return task.getTeam();
    }
    
    public String getAssignedDoctorString() {
        if(null == getTeamAssigned()) {
            return "";
        }
        return " (assigned to " + getTeamAssigned().getName() + ")";
    }
    
    public abstract void runDiagnostic(Campaign campaign);
    
    public abstract void heal();
    
    public abstract boolean needsHealing();
    
    public boolean checkNaturalHealing() {
        if(needsHealing() && null == task.getTeam()) {
            daysRest++;
            if(daysRest >= 15) {
                heal();
                daysRest = 0;
                return true;
            }
        }
        return false;
    }
    
    public boolean isDeployed() {
        return deployed;
    }
    
    public void setDeployed(boolean b) {
        this.deployed = b;
        if(null != task && deployed) {
            task.setTeam(null);
        }
    }
    
    /**
     * 
     * @return an html String report for the person
     */
    public abstract String getDossier();
    
    public String getBiography() {
        return biography;
    }
    
    public void setBiography(String s) {
        this.biography = s;
    }

	public abstract void writeToXml(PrintWriter pw1, int indent, int id);
	
	protected void writeToXmlBegin(PrintWriter pw1, int indent, int id) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<person id=\""
				+id
				+"\" type=\""
				+this.getClass().getName()
				+"\">");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<biography>"
				+biography
				+"</biography>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<daysRest>"
				+daysRest
				+"</daysRest>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<deployed>"
				+deployed
				+"</deployed>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<id>"
				+this.id
				+"</id>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<portraitCategory>"
				+portraitCategory
				+"</portraitCategory>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<portraitFile>"
				+portraitFile
				+"</portraitFile>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<xp>"
				+xp
				+"</xp>");

		// The task reference can be loaded through its ID.
		// But...  If the task is null, we just bypass it.
		if (task != null) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<taskId>"
					+task.getId()
					+"</taskId>");
		}
	}
	
	protected void writeToXmlEnd(PrintWriter pw1, int indent, int id) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</person>");
	}

	public static Person generateInstanceFromXML(Node wn) {
		Person retVal = null;
		NamedNodeMap attrs = wn.getAttributes();
		Node classNameNode = attrs.getNamedItem("type");
		String className = classNameNode.getTextContent();

		try {
			// Instantiate the correct child class, and call its parsing function.
			retVal = (Person) Class.forName(className).newInstance();
			retVal.loadFieldsFromXmlNode(wn);
			
			// Okay, now load Part-specific fields!
			NodeList nl = wn.getChildNodes();
			
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				
				if (wn2.getNodeName().equalsIgnoreCase("biography")) {
					retVal.biography = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("daysRest")) {
					retVal.daysRest = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("deployed")) {
					if (wn2.getTextContent().equalsIgnoreCase("true"))
						retVal.deployed = true;
					else
						retVal.deployed = false;
				} else if (wn2.getNodeName().equalsIgnoreCase("id")) {
					retVal.id = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("portraitCategory")) {
					retVal.portraitCategory = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("portraitFile")) {
					retVal.portraitFile = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("taskId")) {
					retVal.taskId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("xp")) {
					retVal.xp = Integer.parseInt(wn2.getTextContent());
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
	
	public abstract int getMonthlySalary();
	protected abstract void loadFieldsFromXmlNode(Node wn);
}
