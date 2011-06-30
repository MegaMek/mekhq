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
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.ConvFighter;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.Pilot;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.VTOL;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import mekhq.MekHQApp;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlSerializable;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Ranks;
import mekhq.campaign.Unit;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.team.MedicalTeam;
import mekhq.campaign.team.SupportTeam;
import mekhq.campaign.work.IMedicalWork;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.IWork;
import mekhq.campaign.work.Modes;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Person implements Serializable, MekHqXmlSerializable, IMedicalWork {
	private static final long serialVersionUID = -847642980395311152L;
	
	public static final int G_MALE = 0;
	public static final int G_FEMALE = 1;
	
	public static final int T_NONE        = 0;
	public static final int T_MECHWARRIOR = 1;
	public static final int T_VEE_CREW    = 2;
	public static final int T_AERO_PILOT  = 3;
	public static final int T_PROTO_PILOT = 4;
	public static final int T_BA          = 5;
	public static final int T_INFANTRY    = 6;
	public static final int T_MECH_TECH   = 7;
    public static final int T_MECHANIC    = 8;
    public static final int T_AERO_TECH   = 9;
    public static final int T_BA_TECH     = 10;
    public static final int T_ASTECH      = 11;
    public static final int T_DOCTOR      = 12;
    public static final int T_MEDIC       = 13;
    public static final int T_ADMIN_COM   = 14;
    public static final int T_ADMIN_LOG   = 15;
    public static final int T_ADMIN_TRA   = 16;
    public static final int T_ADMIN_HR    = 17;
    public static final int T_NUM         = 18;
    
    public static final int S_ACTIVE = 0;
    public static final int S_RETIRED = 1;
    public static final int S_KIA = 2;
    public static final int S_MIA = 3;
    public static final int S_NUM = 4;
	
    protected int id;
    
    private String name;
    private String callsign;
    protected int gender;

    private int primaryRole;
    private int secondaryRole;
    
    protected String biography;
    protected GregorianCalendar birthday;
    
    private Hashtable<String,Skill> skills;
    
    private PilotOptions options = new PilotOptions();
    
    private int rank;
    private int status;
    protected int xp;
    protected int salary;
    private int hits;
    
    //assignments
    private int unitId;
    protected int medicalTeamId;
    //for reverse compatability v0.1.8 and earlier
    protected int teamId = -1;
    
    //days of rest
    protected int daysRest;
    
    //portrait
    protected String portraitCategory;
    protected String portraitFile;
    
    //need to pass in the rank system
    private Ranks ranks;
    
    //stuff to track for support teams
    protected int hours;
    protected int minutesLeft;
    protected int overtimeLeft;
    
    //default constructor
    public Person() {
    	this("Biff the Understudy", null);
    }
    
    public Person(String name, Ranks r) {
    	this.name = name;
    	callsign = "";
        daysRest = 0;
        portraitCategory = Pilot.ROOT_PORTRAIT;
        portraitFile = Pilot.PORTRAIT_NONE;
        xp = 0;
        gender = G_MALE;
        birthday = new GregorianCalendar(3042, Calendar.JANUARY, 1);
        rank = 0;
        status = S_ACTIVE;
        hits = 0;
        skills = new Hashtable<String,Skill>();
        salary = -1;
        ranks = r;
        medicalTeamId = -1;
        unitId = -1;
        hours = 8;
        resetMinutesLeft();
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

    public String getName() {
    	return name;
    }
    
    public void setName(String n) {
    	this.name = n;
    	//TODO: fix this
    	//resetPilotName();
    }
    
    public String getCallsign() {
    	return callsign;
    }
    
    public void setCallsign(String n) {
    	this.callsign = n;
    }
    
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
    
    public int getPrimaryRole() {
        return primaryRole;
    }
    
    public void setPrimaryRole(int t) {
    	this.primaryRole = t;
    }
    
    public int getSecondaryRole() {
        return primaryRole;
    }
    
    public void setSecondaryRole(int t) {
    	this.primaryRole = t;
    }
    
    public int getStatus() {
    	return status;
    }
    
    public void setStatus(int s) {
    	this.status = s;
    }

    public static String getRoleDesc(int type) {
        switch(type) {
        	case(T_NONE):
        		return "None";
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
            case(T_INFANTRY):
                return "Infantry";
            case(T_MECH_TECH):
                return "Mech Tech";
            case(T_MECHANIC):
                    return "Mechanic";
            case(T_AERO_TECH):
            	return "Aero Tech";
            case(T_BA_TECH):
                return "Battle Armor Tech";
            case(T_ASTECH):
                return "Astech";
            case(T_DOCTOR):
                return "Doctor";
            case(T_MEDIC):
                return "Medic";
            case(T_ADMIN_COM):
                return "Admin/Command";
            case(T_ADMIN_LOG):
                return "Admin/Logistical";
            case(T_ADMIN_TRA):
                return "Admin/Transport";
            case(T_ADMIN_HR):
                return "Admin/HR";
            default:
                return "??";
        }
    }
    
    public String getPrimaryRoleDesc() {
        return getRoleDesc(primaryRole);
    }
    
    public String getSecondaryRoleDesc() {
        return getRoleDesc(secondaryRole);
    }
    
    public boolean canPerformRole(int role) {
    	switch(role) {
    	case(T_NONE):
    		return true;
        case(T_MECHWARRIOR):
            return hasSkill(SkillType.S_GUN_MECH) && hasSkill(SkillType.S_PILOT_MECH);
        case(T_VEE_CREW):
            return hasSkill(SkillType.S_GUN_VEE) && (hasSkill(SkillType.S_PILOT_GVEE) || hasSkill(SkillType.S_PILOT_VTOL) || hasSkill(SkillType.S_PILOT_NVEE));
        case(T_AERO_PILOT):
            return hasSkill(SkillType.S_GUN_AERO) && hasSkill(SkillType.S_PILOT_AERO);
        case(T_PROTO_PILOT):
            return false;
        case(T_BA):
            return hasSkill(SkillType.S_GUN_BA);
        case(T_INFANTRY):
            return hasSkill(SkillType.S_SMALL_ARMS);
        case(T_MECH_TECH):
            return hasSkill(SkillType.S_TECH_MECH) && getSkill(SkillType.S_TECH_MECH).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
        case(T_MECHANIC):
            return hasSkill(SkillType.S_TECH_MECHANIC) && getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
        case(T_AERO_TECH):
            return hasSkill(SkillType.S_TECH_AERO) && getSkill(SkillType.S_TECH_AERO).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
        case(T_BA_TECH):
            return hasSkill(SkillType.S_TECH_BA) && getSkill(SkillType.S_TECH_BA).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
        case(T_ASTECH):
            return hasSkill(SkillType.S_ASTECH);
        case(T_DOCTOR):
            return hasSkill(SkillType.S_DOCTOR) && getSkill(SkillType.S_DOCTOR).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
        case(T_MEDIC):
            return hasSkill(SkillType.S_MEDTECH);
        case(T_ADMIN_COM):
        case(T_ADMIN_LOG):
        case(T_ADMIN_TRA):
        case(T_ADMIN_HR):
            return hasSkill(SkillType.S_ADMIN);
        default:
            return false;
    }
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

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }
    
    public int getAssignedTeamId() {
        return medicalTeamId;
    }
    
    public void setTeamId(int t) {
    	this.medicalTeamId = t;
    }
  
    public boolean checkNaturalHealing() {
        if(needsFixing() && medicalTeamId == -1) {
            daysRest++;
            if(daysRest >= 15) {
                heal();
                daysRest = 0;
                return true;
            }
        }
        return false;
    }
    
    public boolean isDeployed(Campaign c) {
    	Unit u = c.getUnit(unitId);
    	if(null != u) {
    		return u.getScenarioId() != -1;
    	}
        return false;
    }
  
    public String getBiography() {
        return biography;
    }
    
    public void setBiography(String s) {
        this.biography = s;
    }

    public boolean isActive() {
    	return getStatus() == S_ACTIVE;
    }
 
    @Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<person id=\""
				+id
				+"\" type=\""
				+this.getClass().getName()
				+"\">");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<name>"
				+name
				+"</name>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<callsign>"
				+callsign
				+"</callsign>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<primaryRole>"
				+primaryRole
				+"</primaryRole>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<secondaryRole>"
				+secondaryRole
				+"</secondaryRole>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<biography>"
				+biography
				+"</biography>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<daysRest>"
				+daysRest
				+"</daysRest>");
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
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<medicalTeamId>"
				+medicalTeamId
				+"</medicalTeamId>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<unitId>"
				+unitId
				+"</unitId>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<salary>"
				+salary
				+"</salary>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<status>"
				+status
				+"</status>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<hits>"
				+hits
				+"</hits>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<hours>"
				+hours
				+"</hours>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<minutesLeft>"
				+minutesLeft
				+"</minutesLeft>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<overtimeLeft>"
				+overtimeLeft
				+"</overtimeLeft>");
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<birthday>"
				+df.format(birthday.getTime())
				+"</birthday>");
		for(String skName : skills.keySet()) {
			Skill skill = skills.get(skName);
			skill.writeToXml(pw1, indent+1, id);
		}
		if (countOptions(PilotOptions.LVL3_ADVANTAGES) > 0) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<advantages>"
					+String.valueOf(getOptionList("::", PilotOptions.LVL3_ADVANTAGES))
					+"</advantages>");
		}
		if (countOptions(PilotOptions.EDGE_ADVANTAGES) > 0) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<edge>"
					+String.valueOf(getOptionList("::", PilotOptions.EDGE_ADVANTAGES))
					+"</edge>");
		}
		if (countOptions(PilotOptions.MD_ADVANTAGES) > 0) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<implants>"
					+String.valueOf(getOptionList("::", PilotOptions.MD_ADVANTAGES))
					+"</implants>");
		}
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</person>");
	}

	public static Person generateInstanceFromXML(Node wn) {
		Person retVal = null;
		NamedNodeMap attrs = wn.getAttributes();
		Node classNameNode = attrs.getNamedItem("type");
		String className = classNameNode.getTextContent();

		try {
			// Instantiate the correct child class, and call its parsing function.
			retVal = new Person();
			
			// Okay, now load Part-specific fields!
			NodeList nl = wn.getChildNodes();
			
			String advantages = null;
			String edge = null;
			String implants = null;
			
			//backwards compatability
			String pilotName = null;
			int pilotGunnery = -1;
			int pilotPiloting = -1;
			int pilotCommandBonus = -1;
			int pilotInitBonus = -1;
			
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				
				if (wn2.getNodeName().equalsIgnoreCase("name")) {
					retVal.name = wn2.getTextContent();
				} else if(wn2.getNodeName().equalsIgnoreCase("callsign")) {
					retVal.callsign = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("biography")) {
					retVal.biography = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("primaryRole")) {
					retVal.primaryRole = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("secondaryRole")) {
					retVal.secondaryRole = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("daysRest")) {
					retVal.daysRest = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("id")) {
					retVal.id = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("teamId")) {
					retVal.teamId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("portraitCategory")) {
					retVal.setPortraitCategory(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("portraitFile")) {
					retVal.setPortraitFileName(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("xp")) {
					retVal.xp = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("hits")) {
					retVal.hits = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("gender")) {
					retVal.gender = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("rank")) {
					retVal.rank = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("medicalTeamId")) {
					retVal.medicalTeamId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("unitId")) {
					retVal.unitId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("status")) {
					retVal.status = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("salary")) {
					retVal.salary = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("hours")) {
					retVal.hours = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("minutesLeft")) {
					retVal.minutesLeft = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("overtimeLeft")) {
					retVal.overtimeLeft = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("birthday")) {
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
					retVal.birthday = (GregorianCalendar) GregorianCalendar.getInstance();
					retVal.birthday.setTime(df.parse(wn2.getTextContent().trim()));
				} else if (wn2.getNodeName().equalsIgnoreCase("advantages")) {
					advantages = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("edge")) {
					edge = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("implants")) {
					implants = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("pilotGunnery")) {
					pilotGunnery = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("pilotPiloting")) {
					pilotPiloting = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("pilotHits")) {
					retVal.hits = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("pilotCommandBonus")) {
					pilotCommandBonus = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("pilotInitBonus")) {
					pilotInitBonus = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("pilotName")) {
					pilotName = wn2.getTextContent();
				} else if(wn2.getNodeName().equalsIgnoreCase("skill")) {
					Skill s = Skill.generateInstanceFromXML(wn2);
					if(null != s) {
						retVal.skills.put(s.getType().getName(), s);
					}
				}
			}
			
			if ((null != advantages) && (advantages.trim().length() > 0)) {
	            StringTokenizer st = new StringTokenizer(advantages,"::");
	            while (st.hasMoreTokens()) {
	                String adv = st.nextToken();
	                String advName = Pilot.parseAdvantageName(adv);
	                Object value = Pilot.parseAdvantageValue(adv);

	                try {
	                    retVal.getOptions().getOption(advName).setValue(value);
	                } catch (Exception e) {
	                    MekHQApp.logMessage("Error restoring advantage: " +  adv);
	                }
	            }
	        }
			if ((null != edge) && (edge.trim().length() > 0)) {
	            StringTokenizer st = new StringTokenizer(edge,"::");
	            while (st.hasMoreTokens()) {
	                String adv = st.nextToken();
	                String advName = Pilot.parseAdvantageName(adv);
	                Object value = Pilot.parseAdvantageValue(adv);

	                try {
	                    retVal.getOptions().getOption(advName).setValue(value);
	                } catch (Exception e) {
	                    MekHQApp.logMessage("Error restoring edge: " +  adv);
	                }
	            }
	        }
			if ((null != implants) && (implants.trim().length() > 0)) {
	            StringTokenizer st = new StringTokenizer(implants,"::");
	            while (st.hasMoreTokens()) {
	                String adv = st.nextToken();
	                String advName = Pilot.parseAdvantageName(adv);
	                Object value = Pilot.parseAdvantageValue(adv);

	                try {
	                    retVal.getOptions().getOption(advName).setValue(value);
	                } catch (Exception e) {
	                    MekHQApp.logMessage("Error restoring implants: " +  adv);
	                }
	            }
	        }
			//check to see if we are dealing with a PilotPerson from 0.1.8 or earlier
			if(pilotGunnery != -1) {
				switch(retVal.primaryRole) {
				case T_MECHWARRIOR:
					retVal.addSkill(SkillType.S_GUN_MECH,7-pilotGunnery,0);
					retVal.addSkill(SkillType.S_PILOT_MECH,8-pilotPiloting,0);
					break;
				case T_VEE_CREW:
					retVal.addSkill(SkillType.S_GUN_VEE,7-pilotGunnery,0);
					retVal.addSkill(SkillType.S_PILOT_GVEE,8-pilotPiloting,0);
					break;
				case T_AERO_PILOT:
					retVal.addSkill(SkillType.S_GUN_AERO,7-pilotGunnery,0);
					retVal.addSkill(SkillType.S_PILOT_AERO,8-pilotPiloting,0);
					break;
				case T_BA:
					retVal.addSkill(SkillType.S_GUN_BA,7-pilotGunnery,0);
					retVal.addSkill(SkillType.S_ANTI_MECH,8-pilotPiloting,0);
					break;

				}
				retVal.addSkill(SkillType.S_TACTICS,pilotCommandBonus,0);
			}
			if(null != pilotName) {
				retVal.setName(pilotName);
			}
		} catch (Exception ex) {
			// Errrr, apparently either the class name was invalid...
			// Or the listed name doesn't exist.
			// Doh!
			MekHQApp.logError(ex);
		}
		
		return retVal;
	}
	
	public int getSalary() {
		
		if(salary > -1) {
			return salary;
		}
		
		//if salary is -1, then use the standard amounts
		int base = 0;
		
		switch (getPrimaryRole()) {
	    	case T_MECHWARRIOR:
				base = 1500;
				break;
			case T_VEE_CREW:
				base = 900;
				break;
			case T_AERO_PILOT:
				base = 1500;
				break;
			case T_PROTO_PILOT:
				//TODO: Confirm ProtoMech pilots should be paid as BA pilots?
				base = 960;
				break;
			case T_BA:
				base = 960;
				break;
			case T_INFANTRY:
				base = 750;
				break;
			case T_MECH_TECH:
				base = 800;
				break;
			case T_MECHANIC:
				base = 640;
				break;
			case T_AERO_TECH:
				base = 800;
				break;
			case T_BA_TECH:
				base = 800;
				break;
			case T_ASTECH:
				base = 640;
				break;
			case T_DOCTOR:
				base = 1500;
				break;
			case T_MEDIC:
				base = 640;
				break;
			case T_ADMIN_COM:
			case T_ADMIN_LOG:
			case T_ADMIN_TRA:
			case T_ADMIN_HR:
				base = 320;
				break;
			case T_NUM:
				// Not a real pilot type. If someone has this, they don't get paid!
				base = 0;
				break;
		}

		double expMult = 1.0;
		switch(getExperienceLevel()) {
		case SkillType.EXP_ULTRA_GREEN:
			expMult = 0.5;
			break;
		case SkillType.EXP_GREEN:
			expMult = 0.6;
			break;
		case SkillType.EXP_VETERAN:
			expMult = 1.6;
			break;
		case SkillType.EXP_ELITE:
			expMult =3.2;
			break;
		default:
			expMult = 1.0;
		}
		
		if(getPrimaryRole() == T_ASTECH || getPrimaryRole() == T_MEDIC) {
			expMult = 0.5;
		}
		
		double offMult = 0.6;
		if(ranks.isOfficer(getRank())) {
			offMult = 1.2;
		}
		
		double antiMekMult = 1.0;
		if(hasSkill(SkillType.S_ANTI_MECH)) {
			antiMekMult = 1.5;
		}
		
		return (int)(base * expMult * offMult * antiMekMult);
		//TODO: Add conventional aircraft pilots.
		//TODO: Add vessel crewmen (DropShip).
		//TODO: Add vessel crewmen (JumpShip).
		//TODO: Add vessel crewmen (WarShip).
		
		//TODO: Properly pay large ship crews for actual size.
		
		//TODO: Add era mod to salary calc..
	}
	
	public int getRank() {
		return rank;
	}
	
	public void setRank(int r) {
		this.rank = r;
	}

	public String getSkillSummary() {
    	return SkillType.getExperienceLevelName(getExperienceLevel());
	}

	public String toString() {
		return getDesc();
	}
	
	public int getExperienceLevel() {
		switch(primaryRole) {
		case T_MECHWARRIOR:
			if(hasSkill(SkillType.S_GUN_MECH) && hasSkill(SkillType.S_PILOT_MECH)) {
				return (int)Math.floor((getSkill(SkillType.S_GUN_MECH).getExperienceLevel() 
						+ getSkill(SkillType.S_PILOT_MECH).getExperienceLevel()) / 2.0);
			} else {
				return -1;
			}
		case T_VEE_CREW:
			if(hasSkill(SkillType.S_GUN_VEE) && hasSkill(SkillType.S_PILOT_GVEE)) {
				return (int)Math.floor((getSkill(SkillType.S_GUN_VEE).getExperienceLevel() 
						+ getSkill(SkillType.S_PILOT_GVEE).getExperienceLevel()) / 2.0);
			} else {
				return -1;
			}
		case T_AERO_PILOT:
			if(hasSkill(SkillType.S_GUN_AERO) && hasSkill(SkillType.S_PILOT_AERO)) {
				return (int)Math.floor((getSkill(SkillType.S_GUN_AERO).getExperienceLevel() 
						+ getSkill(SkillType.S_PILOT_AERO).getExperienceLevel()) / 2.0);
			} else {
				return -1;
			}
		case T_BA:
			if(hasSkill(SkillType.S_GUN_BA) && hasSkill(SkillType.S_ANTI_MECH)) {
				return (int)Math.floor((getSkill(SkillType.S_GUN_BA).getExperienceLevel() 
						+ getSkill(SkillType.S_ANTI_MECH).getExperienceLevel()) / 2.0);
			} else {
				return -1;
			}
		case T_INFANTRY:
			if(hasSkill(SkillType.S_SMALL_ARMS)) {
				return getSkill(SkillType.S_SMALL_ARMS).getExperienceLevel();
			} else {
				return -1;
			}
		case T_MECH_TECH:
			if(hasSkill(SkillType.S_TECH_MECH)) {
				return getSkill(SkillType.S_TECH_MECH).getExperienceLevel();
			} else {
				return -1;
			}
		case T_MECHANIC:
			if(hasSkill(SkillType.S_TECH_MECHANIC)) {
				return getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel();
			} else {
				return -1;
			}
		case T_AERO_TECH:
			if(hasSkill(SkillType.S_TECH_AERO)) {
				return getSkill(SkillType.S_TECH_AERO).getExperienceLevel();
			} else {
				return -1;
			}
		case T_BA_TECH:
			if(hasSkill(SkillType.S_TECH_BA)) {
				return getSkill(SkillType.S_TECH_BA).getExperienceLevel();
			} else {
				return -1;
			}
		case T_ASTECH:
			if(hasSkill(SkillType.S_ASTECH)) {
				return getSkill(SkillType.S_ASTECH).getExperienceLevel();
			} else {
				return -1;
			}
		case T_DOCTOR:
			if(hasSkill(SkillType.S_DOCTOR)) {
				return getSkill(SkillType.S_DOCTOR).getExperienceLevel();
			} else {
				return -1;
			}
		case T_MEDIC:
			if(hasSkill(SkillType.S_MEDTECH)) {
				return getSkill(SkillType.S_MEDTECH).getExperienceLevel();
			} else {
				return -1;
			}
		case T_ADMIN_COM:
		case T_ADMIN_LOG:
		case T_ADMIN_TRA:
		case T_ADMIN_HR:
			if(hasSkill(SkillType.S_ADMIN)) {
				return getSkill(SkillType.S_ADMIN).getExperienceLevel();
			} else {
				return -1;
			}
		default:
			return -1;
		}
    }
	
	public String getDesc() {
		//String care = "";
		//String status = "";
		//if(pilot.getHits() > 0) {
		//	status = " (" + pilot.getStatusDesc() + ")";
		//}
		//return care + pilot.getName() + " [" + pilot.getGunnery() + "/" + pilot.getPiloting() + " " + getTypeDesc() + "]" + status;
		return "";
	}
	
	public String getDescHTML() {
        String toReturn = "<html><font size='2'><b>" + getName() + "</b><br/>";
        //toReturn += getTypeDesc() + " (" + pilot.getGunnery() + "/" + pilot.getPiloting() + ")<br/>";
        //toReturn += pilot.getStatusDesc();
        toReturn += "</font></html>";
        return toReturn;
    }
	
	public String getFullTitle() {
		String rank = ranks.getRank(getRank());
		if(rank.equalsIgnoreCase("None")) {
			return getName();
		}
		return rank + " " + getName();
	}
	
	public void setRankSystem(Ranks r) {
		this.ranks = r;
	}
	
	@Override public int getMode() {
		return IWork.MODE_NORMAL;
	}
	

	@Override
	public int getDifficulty() {
		return 0;
	}

	@Override
	public TargetRoll getAllMods() {
		TargetRoll mods = new TargetRoll(getDifficulty(), "difficulty");
		return mods;
	}
	
	@Override
	public String fail(int rating) {
		return " <font color='red'><b>Failed to heal.</b></font>";
	}
	
	@Override
	public String getPatientName() {
		return getName();
	}
	
	public boolean hasSkill(String skillName) {
		return null != skills.get(skillName);
	}
	
	public Skill getSkill(String skillName) {
		return skills.get(skillName);
	}
	
	public void addSkill(String skillName, int lvl, int bonus) {
		skills.put(skillName, new Skill(skillName, lvl, bonus));
	}
	
	public void addSkill(String skillName) {
		skills.put(skillName, new Skill(skillName));
	}
	
	public void removeSkill(String skillName) {
		if(hasSkill(skillName)) {
			skills.remove(skillName);
		}
	}
	
	public void improveSkill(String skillName) {
		if(hasSkill(skillName)) {
			getSkill(skillName).improve();
		} else {
			addSkill(skillName, 0, 0);
		}
	}
	
	public int getCostToImprove(String skillName) {
		if(hasSkill(skillName)) {
			return getSkill(skillName).getCostToImprove();
		} else {
			return -1;
		}
	}
	
	public int getHits() {
		return hits;
	}
	
	public void setHits(int h) {
		this.hits = h;
	}

	@Override
	public void heal() {
		hits = Math.max(hits - 1, 0);
		if(!needsFixing()) {
			medicalTeamId = -1;
		}
	}
/*
	@Override
	public boolean canFix(Person person) {
		return false;//team instanceof MedicalTeam && ((MedicalTeam)team).getPatients() < 25;
	}
*/
	@Override
	public boolean needsFixing() {
		return hits > 0 && status != S_KIA && status == S_ACTIVE;
	}

	@Override
	public String succeed() {
		heal();
		return " <font color='green'><b>Successfully healed one hit.</b></font>";
	}
	
	public PilotOptions getOptions() {
        return options;
    }
	
	/**
     * Returns the options of the given category that this pilot has
     */
    public Enumeration<IOption> getOptions(String grpKey) {
        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (group.getKey().equalsIgnoreCase(grpKey)) {
                return group.getOptions();
            }
        }

        // no pilot advantages -- return an empty Enumeration
        return new Vector<IOption>().elements();
    }

    public int countOptions() {
        int count = 0;

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                IOption option = j.nextElement();

                if (option.booleanValue()) {
                    count++;
                }
            }
        }

        return count;
    }

    public int countOptions(String grpKey) {
        int count = 0;

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(grpKey)) {
                continue;
            }

            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                IOption option = j.nextElement();

                if (option.booleanValue()) {
                    count++;
                }
            }
        }

        return count;
    }
	
    /**
     * Returns a string of all the option "codes" for this pilot, for a given
     * group, using sep as the separator
     */
    public String getOptionList(String sep, String grpKey) {
        StringBuffer adv = new StringBuffer();

        if (null == sep) {
            sep = "";
        }

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();
            if (!group.getKey().equalsIgnoreCase(grpKey)) {
                continue;
            }
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                IOption option = j.nextElement();

                if (option.booleanValue()) {
                    if (adv.length() > 0) {
                        adv.append(sep);
                    }

                    adv.append(option.getName());
                    if ((option.getType() == IOption.STRING) || (option.getType() == IOption.CHOICE) || (option.getType() == IOption.INTEGER)) {
                        adv.append(" ").append(option.stringValue());
                    }
                }
            }
        }

        return adv.toString();
    }
    
	public int getEdge() {
    	return getOptions().intOption("edge");
    }
    
    public void setEdge(int e) {
    	for (Enumeration<IOption> i = getOptions(PilotOptions.EDGE_ADVANTAGES); i.hasMoreElements();) {
        	IOption ability = i.nextElement();
        	if(ability.getName().equals("edge")) {
        		ability.setValue(e);
        	}
        }
    }
    
    /**
     * This will flip the boolean status of the current edge trigger
     * @param name
     */
    public void changeEdgeTrigger(String name) {
    	for (Enumeration<IOption> i = getOptions(PilotOptions.EDGE_ADVANTAGES); i.hasMoreElements();) {
        	IOption ability = i.nextElement();
        	if(ability.getName().equals(name)) {
        		ability.setValue(!ability.booleanValue());
        	}
        }
    }
    
    /**
     * This function returns an html-coded tooltip that says what 
     * edge will be used
     * @return
     */
    public String getEdgeTooltip() {
    	String edgett = "";
    	for (Enumeration<IOption> i = getOptions(PilotOptions.EDGE_ADVANTAGES); i.hasMoreElements();) {
        	IOption ability = i.nextElement();
        	//yuck, it would be nice to have a more fool-proof way of identifying edge triggers
        	if(ability.getName().contains("edge_when") && ability.booleanValue()) {
        		edgett = edgett + ability.getDescription() + "<br>";
        	}
        }
    	if(edgett.equals("")) {
    		return "No triggers set";
    	}
    	return "<html>" + edgett + "</html>";
    }
    
    /**
     * This function returns an html-coded list that says what 
     * abilities are enabled for this pilot
     * @return
     */
    public String getAbilityList(String type) {
    	String abilityString = "";
        for (Enumeration<IOption> i = getOptions(type); i.hasMoreElements();) {
        	IOption ability = i.nextElement();
        	if(ability.booleanValue()) {
        		abilityString = abilityString + ability.getDisplayableNameWithValue() + "<br>";
        	}
        }
        if(abilityString.equals("")) {
        	return null;
        }
        return "<html>" + abilityString + "</html>";
    }
    
    public void acquireAbility(String type, String name, Object value) {
    	for (Enumeration<IOption> i = getOptions(type); i.hasMoreElements();) {
        	IOption ability = i.nextElement();
        	if(ability.getName().equals(name)) {
        		ability.setValue(value);
        	}
    	}
    }
    
    public boolean isSupport() {
    	return primaryRole >= T_MECH_TECH;
    }
    
    public boolean canDrive(Entity ent) {
    	if(ent instanceof Mech) {
    		return hasSkill(SkillType.S_PILOT_MECH);
    	}
    	else if(ent instanceof VTOL) {
    		return hasSkill(SkillType.S_PILOT_VTOL);
    	}
    	else if(ent instanceof Tank) {
    		return hasSkill(SkillType.S_PILOT_GVEE);
    	}
    	else if(ent instanceof ConvFighter) {
    		return hasSkill(SkillType.S_PILOT_JET) || hasSkill(SkillType.S_PILOT_AERO);
    	}
    	else if(ent instanceof SmallCraft || ent instanceof Jumpship) {
    		return hasSkill(SkillType.S_PILOT_SPACE);
    	}
    	else if(ent instanceof Aero) {
    		return hasSkill(SkillType.S_PILOT_AERO);
    	}
    	else if(ent instanceof Infantry) {
    		return true;
    	}
    	return false;
    }
    
    public boolean canGun(Entity ent) {
    	if(ent instanceof Mech) {
    		return hasSkill(SkillType.S_GUN_MECH);
    	}
    	else if(ent instanceof Tank) {
    		return hasSkill(SkillType.S_GUN_VEE);
    	}
    	else if(ent instanceof ConvFighter) {
    		return hasSkill(SkillType.S_GUN_JET) || hasSkill(SkillType.S_GUN_AERO);
    	}
    	else if(ent instanceof SmallCraft || ent instanceof Jumpship) {
    		return hasSkill(SkillType.S_GUN_SPACE);
    	}
    	else if(ent instanceof Aero) {
    		return hasSkill(SkillType.S_GUN_AERO);
    	}
    	else if(ent instanceof BattleArmor) {
    		return hasSkill(SkillType.S_GUN_BA);
    	}
    	else if(ent instanceof Infantry) {
    		return hasSkill(SkillType.S_SMALL_ARMS);
    	}
    	return false;
    }
    
    public int getUnitId() {
    	return unitId;
    }
    
    public void setUnitId(int i) {
    	unitId = i;
    }
    
    public int getOldSupportTeamId() {
    	return teamId;
    }
    
    public int getHours() {
        return hours;
    }
    
    public void setHours(int i) {
        this.hours = i;
    }
    
    public int getMinutesLeft() {
        return minutesLeft;
    }
    
    public void setMinutesLeft(int m) {
        this.minutesLeft = m;
    }
    
    public int getOvertimeLeft() {
        return overtimeLeft;
    }
    
    public void setOvertimeLeft(int m) {
        this.overtimeLeft = m;
    }
    
    public void resetMinutesLeft() {
        this.minutesLeft = 60 * getHours();
        this.overtimeLeft = 60 * 4;
    }
    
    public boolean isTech() {
    	//type must be correct and you must be more than ultra-green in the skill
    	boolean hasType = primaryRole == T_MECH_TECH ||  primaryRole == T_AERO_TECH ||  primaryRole == T_MECHANIC ||  primaryRole == T_BA_TECH;
    	boolean isMechTech = hasSkill(SkillType.S_TECH_MECH) && getSkill(SkillType.S_TECH_MECH).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
    	boolean isAeroTech = hasSkill(SkillType.S_TECH_AERO) && getSkill(SkillType.S_TECH_AERO).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
    	boolean isMechanic = hasSkill(SkillType.S_TECH_MECHANIC) && getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
    	boolean isBATech = hasSkill(SkillType.S_TECH_BA) && getSkill(SkillType.S_TECH_BA).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
    	return hasType && (isMechTech || isAeroTech || isMechanic || isBATech);
    }
   
    public boolean isTaskOvertime(IPartWork partWork) {
        return partWork.getTimeLeft() > getMinutesLeft()
                 && (partWork.getTimeLeft() - getMinutesLeft()) <= getOvertimeLeft();
    }
    
    public Skill getSkillForWorkingOn(Unit unit) {
    	if(unit.getEntity() instanceof Mech && hasSkill(SkillType.S_TECH_MECH)) {
    		return getSkill(SkillType.S_TECH_MECH);
    	}
    	if(unit.getEntity() instanceof BattleArmor && hasSkill(SkillType.S_TECH_BA)) {
    		return getSkill(SkillType.S_TECH_BA);
    	}
    	if(unit.getEntity() instanceof Tank && hasSkill(SkillType.S_TECH_MECHANIC)) {
    		return getSkill(SkillType.S_TECH_MECHANIC);
    	}
    	if(unit.getEntity() instanceof Aero && hasSkill(SkillType.S_TECH_AERO)) {
    		return getSkill(SkillType.S_TECH_AERO);
    	}
    	//if we are still here then we didn't have the right tech skill, so return the highest
    	//of any tech skills that we do have
    	Skill skill = null;
    	if(hasSkill(SkillType.S_TECH_MECH)) {
    		skill = getSkill(SkillType.S_TECH_MECH);
    	}
    	if(hasSkill(SkillType.S_TECH_BA)) {
    		if(null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_BA).getFinalSkillValue()) {
    			skill = getSkill(SkillType.S_TECH_BA);
    		}
    	}
    	if(hasSkill(SkillType.S_TECH_MECHANIC)) {
    		if(null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_MECHANIC).getFinalSkillValue()) {
    			skill = getSkill(SkillType.S_TECH_MECHANIC);
    		}
    	}
    	if(hasSkill(SkillType.S_TECH_AERO)) {
    		if(null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_AERO).getFinalSkillValue()) {
    			skill = getSkill(SkillType.S_TECH_AERO);
    		}
    	}
    	return skill;
    }
    
    public String getTechDesc(boolean overtimeAllowed) {
         String toReturn = "<html><font size='2'><b>" + getName() + "</b><br/>";
         Skill mechSkill = getSkill(SkillType.S_TECH_MECH);
         Skill mechanicSkill = getSkill(SkillType.S_TECH_MECHANIC);
         Skill baSkill = getSkill(SkillType.S_TECH_BA);
         Skill aeroSkill = getSkill(SkillType.S_TECH_AERO);
         boolean first = true;
         if(null != mechSkill) {
        	 if(!first) {
        		 toReturn += "; ";
        	 }
        	 toReturn += SkillType.getExperienceLevelName(mechSkill.getExperienceLevel()) + " " + SkillType.S_TECH_MECH;
        	 first = false;
         }
         if(null != mechanicSkill) {
        	 if(!first) {
        		 toReturn += "; ";
        	 }
        	 toReturn += SkillType.getExperienceLevelName(mechanicSkill.getExperienceLevel()) + " " + SkillType.S_TECH_MECHANIC;
        	 first = false;
         }
         if(null != baSkill) {
        	 if(!first) {
        		 toReturn += "; ";
        	 }
        	 toReturn += SkillType.getExperienceLevelName(baSkill.getExperienceLevel()) + " " + SkillType.S_TECH_BA;
        	 first = false;
         }
         if(null != aeroSkill) {
        	 if(!first) {
        		 toReturn += "; ";
        	 }
        	 toReturn += SkillType.getExperienceLevelName(aeroSkill.getExperienceLevel()) + " " + SkillType.S_TECH_AERO;
        	 first = false;
         }
         toReturn += "<br/>";
         toReturn += getMinutesLeft() + " minutes left";
         if(overtimeAllowed) {
             toReturn += " + (" + getOvertimeLeft() + " overtime)";
         }
         toReturn += "</font></html>";
         return toReturn;
    }
    
    public boolean isRightTechTypeFor(Unit unit) {
    	if(unit.getEntity() instanceof Mech) {
    		return hasSkill(SkillType.S_TECH_MECH);
    	}
    	if(unit.getEntity() instanceof BattleArmor) {
    		return hasSkill(SkillType.S_TECH_BA);
    	}
    	if(unit.getEntity() instanceof Tank) {
    		return hasSkill(SkillType.S_TECH_MECHANIC);
    	}
    	if(unit.getEntity() instanceof Aero) {
    		return hasSkill(SkillType.S_TECH_AERO);
    	}
    	return false;
    }
}
