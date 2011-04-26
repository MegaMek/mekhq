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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

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
	
	public static final int G_MALE = 0;
	public static final int G_FEMALE = 1;
	
	public static final int T_MECHWARRIOR = 0;
	public static final int T_VEE_CREW = 1;
	public static final int T_AERO_PILOT = 2;
	public static final int T_PROTO_PILOT = 3;
	public static final int T_BA = 4;
	public static final int T_MECH_TECH = 5;
    public static final int T_MECHANIC = 6;
    public static final int T_AERO_TECH = 7;
    public static final int T_BA_TECH = 8;
    public static final int T_DOCTOR = 9;
    public static final int T_NUM = 10;
    
    public static final int S_ACTIVE = 0;
    public static final int S_RETIRED = 1;
    public static final int S_KIA = 2;
    public static final int S_MIA = 3;
    public static final int S_NUM = 4;
	
	protected int id;
    private int type;
    //any existing work item for this person
    protected PersonnelWorkItem task;
    protected int taskId;
    //days of rest
    protected int daysRest;
    protected boolean deployed;
    protected String biography;
    protected String portraitCategory;
    protected String portraitFile;
    protected int gender;
    protected GregorianCalendar birthday;

    private int rank;
    private int status;
    protected int xp;
    
    //default constructor
    public Person() {
        daysRest = 0;
        portraitCategory = Pilot.ROOT_PORTRAIT;
        portraitFile = Pilot.PORTRAIT_NONE;
        xp = 0;
        gender = G_MALE;
        birthday = new GregorianCalendar(3042, Calendar.JANUARY, 1);
        rank = 0;
        status = S_ACTIVE;
    }
    
    public static String getGenderName(int gender) {
    	switch(gender) {
    	case G_MALE:
    		return "Male";
    	case G_FEMALE:
    		return "Female";
    	default:
    		return "?";
    	}
    }
    
    public static String getStatusName(int status) {
    	switch(status) {
    	case S_ACTIVE:
    		return "Active";
    	case S_RETIRED:
    		return "Retired";
    	case S_KIA:
    		return "Killed in Action";
    	case S_MIA:
    		return "Missing in Action";
    	default:
    		return "?";
    	}
    }
    
    public String getGenderName() {
    	return getGenderName(gender);
    }
    
    public String getStatusName() {
    	return getStatusName(status);
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
    
    public int getType() {
        return type;
    }
    
    public void setType(int t) {
    	this.type = t;
    }
    
    public int getStatus() {
    	return status;
    }
    
    public void setStatus(int s) {
    	this.status = s;
    }

    public static String getTypeDesc(int type) {
        switch(type) {
            case(T_MECHWARRIOR):
                return "Mechwarrior";
            case(T_VEE_CREW):
                return "Vehicle crew";
            case(T_AERO_PILOT):
                return "Aero Pilot";
            case(T_PROTO_PILOT):
                return "Proto Pilot";
            case(T_BA):
                return "Battle armor Pilot";
            case(T_MECH_TECH):
                return "Mech Tech";
            case(T_MECHANIC):
                    return "Mechanic";
            case(T_AERO_TECH):
            	return "Aero Tech";
            case(T_BA_TECH):
                return "Battle Armor Tech";
            case(T_DOCTOR):
                return "Doctor";
            default:
                return "??";
        }
    }
    
    public String getTypeDesc() {
        return getTypeDesc(type);
    }
    
    public void setGender(int g) {
    	this.gender = g;
    }
    
    public int getGender() {
    	return gender;
    }
    
    public void setBirthday(GregorianCalendar date) {
    	this.birthday = date;
    }
    
    public GregorianCalendar getBirthday() {
    	return birthday;
    }
    
    public int getAge(GregorianCalendar today) {
    	// Get age based on year
    	int age = today.get(Calendar.YEAR) - birthday.get(Calendar.YEAR);

    	// Add the tentative age to the date of birth to get this year's birthday
    	GregorianCalendar tmpDate = (GregorianCalendar) birthday.clone();
    	tmpDate.add(Calendar.YEAR, age);

    	// If this year's birthday has not happened yet, subtract one from age
    	if (today.before(tmpDate)) {
    	    age--;
    	}
    	return age;
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

    public boolean isActive() {
    	return getStatus() == S_ACTIVE;
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
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<gender>"
				+gender
				+"</gender>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<rank>"
				+rank
				+"</rank>");
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<birthday>"
				+df.format(birthday.getTime())
				+"</birthday>");

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
					retVal.setPortraitCategory(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("portraitFile")) {
					retVal.setPortraitFileName(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("taskId")) {
					retVal.taskId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("xp")) {
					retVal.xp = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("gender")) {
					retVal.gender = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("rank")) {
					retVal.rank = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("birthday")) {
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
					retVal.birthday = (GregorianCalendar) GregorianCalendar.getInstance();
					retVal.birthday.setTime(df.parse(wn2.getTextContent().trim()));
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
	
	public int getMonthlySalary() {
		int retVal = 0;
		
		switch (getType()) {
	    	case T_MECHWARRIOR:
				retVal = 1500;
				break;
			case T_VEE_CREW:
				retVal = 900;
				break;
			case T_AERO_PILOT:
				retVal = 1500;
				break;
			case T_PROTO_PILOT:
				//TODO: Confirm ProtoMech pilots should be paid as BA pilots?
				retVal = 960;
				break;
			case T_BA:
				retVal = 960;
				break;
			case T_MECH_TECH:
				retVal = 800;
				break;
			case T_MECHANIC:
				retVal = 640;
				break;
			case T_AERO_TECH:
				retVal = 800;
				break;
			case T_BA_TECH:
				retVal = 800;
				break;
			case T_DOCTOR:
				retVal = 1500;
				break;
			case T_NUM:
				// Not a real pilot type. If someone has this, they don't get paid!
				break;
		}

		//TODO: Add conventional aircraft pilots.
		//TODO: Add regular infantry.
		//TODO: Add specialist/Anti-Mech infantry.
		//TODO: Add vessel crewmen (DropShip).
		//TODO: Add vessel crewmen (JumpShip).
		//TODO: Add vessel crewmen (WarShip).
		
		//TODO: Properly pay vehicle crews for actual size.
		//TODO: Properly pay large ship crews for actual size.
		
		//TODO: Add quality mod to salary calc..
		//TODO: Add era mod to salary calc..
		
		return retVal;
	}
	
	public int getRank() {
		return rank;
	}
	
	public void setRank(int r) {
		this.rank = r;
	}
	
	public abstract String getName();
	
	public abstract String getCallsign();

	public abstract String getSkillSummary();
	
	public abstract void improveSkill(int type);

	protected abstract void loadFieldsFromXmlNode(Node wn);
}
