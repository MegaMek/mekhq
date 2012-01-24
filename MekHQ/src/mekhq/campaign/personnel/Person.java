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
import java.util.UUID;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.ConvFighter;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
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
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlSerializable;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Ranks;
import mekhq.campaign.Unit;
import mekhq.campaign.work.IMedicalWork;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.Modes;

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
	public static final int T_AERO_PILOT  = 2;
	public static final int T_GVEE_DRIVER = 3;
	public static final int T_NVEE_DRIVER = 4;
	public static final int T_VTOL_PILOT  = 5;
	public static final int T_VEE_GUNNER  = 6;
	public static final int T_BA          = 7;
	public static final int T_INFANTRY    = 8;
	public static final int T_PROTO_PILOT = 9;
	public static final int T_SPACE_PILOT = 10;
	public static final int T_SPACE_CREW  = 11;
	public static final int T_SPACE_GUNNER= 12;
	public static final int T_NAVIGATOR   = 13;
	public static final int T_MECH_TECH   = 14;
    public static final int T_MECHANIC    = 15;
    public static final int T_AERO_TECH   = 16;
    public static final int T_BA_TECH     = 17;
    public static final int T_ASTECH      = 18;
    public static final int T_DOCTOR      = 19;
    public static final int T_MEDIC       = 20;
    public static final int T_ADMIN_COM   = 21;
    public static final int T_ADMIN_LOG   = 22;
    public static final int T_ADMIN_TRA   = 23;
    public static final int T_ADMIN_HR    = 24;
    public static final int T_NUM         = 25;
    
    public static final int S_ACTIVE = 0;
    public static final int S_RETIRED = 1;
    public static final int S_KIA = 2;
    public static final int S_MIA = 3;
    public static final int S_NUM = 4;
	
    protected UUID id;
    protected int oldId;
    
    private String name;
    private String callsign;
    protected int gender;

    private int primaryRole;
    private int secondaryRole;
    
    protected String biography;
    protected GregorianCalendar birthday;
    
    private Hashtable<String,Skill> skills;
    private PilotOptions options = new PilotOptions();
    private int toughness;
    
    private int rank;
    private int status;
    protected int xp;
    protected int salary;
    private int hits;
    
    //assignments
    private UUID unitId;
    protected UUID doctorId;
    //for reverse compatability v0.1.8 and earlier
    protected int teamId = -1;
    
    //for reverse compatability
    private int oldUnitId;
    private int oldDoctorId;
    
    //days of rest
    protected int daysRest;
    
    //portrait
    protected String portraitCategory;
    protected String portraitFile;
    
    //need to pass in the rank system
    private Ranks ranks;
    
    //stuff to track for support teams
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
        doctorId = null;
        unitId = null;
        oldDoctorId = -1;
        oldUnitId = -1;
        toughness = 0;
        biography = "";
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
    	//you cant be primary tech and a secondary astech
    	//you cant be a primary astech and a secondary tech
    	if((isTechPrimary() && secondaryRole == T_ASTECH)
    			|| (isTechSecondary() && primaryRole == T_ASTECH)) {
    		secondaryRole = T_NONE;
    	}
    	if((primaryRole == T_DOCTOR && secondaryRole == T_MEDIC)
    			|| (secondaryRole == T_DOCTOR && primaryRole == T_MEDIC)) {
    		secondaryRole = T_NONE;
    	}  	
    }
    
    public int getSecondaryRole() {
        return secondaryRole;
    }
    
    public void setSecondaryRole(int t) {
    	this.secondaryRole = t;
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
            case(T_GVEE_DRIVER):
                return "Vehicle Driver";
            case(T_NVEE_DRIVER):
                return "Naval Driver";
            case(T_VTOL_PILOT):
                return "VTOL Pilot";
            case(T_VEE_GUNNER):
                return "Vehicle Gunner";
            case(T_AERO_PILOT):
                return "Aero Pilot";
            case(T_PROTO_PILOT):
                return "Proto Pilot";
            case(T_BA):
                return "Battle Armor Pilot";
            case(T_INFANTRY):
                return "Soldier";
            case(T_SPACE_PILOT):
                return "Vessel Pilot";
            case(T_SPACE_CREW):
                return "Vessel Crewmember";
            case(T_SPACE_GUNNER):
                    return "Vessel Gunner";
            case(T_NAVIGATOR):
                return "Hyperspace Navigator";
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
    
    public String getRoleDesc() {
    	String role = getPrimaryRoleDesc();
    	if(secondaryRole != T_NONE && secondaryRole != -1) {
    		role += "/" + getSecondaryRoleDesc();
    	}
    	return role;
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
        case(T_GVEE_DRIVER):
            return hasSkill(SkillType.S_PILOT_GVEE);
        case(T_NVEE_DRIVER):
            return hasSkill(SkillType.S_PILOT_NVEE);
        case(T_VTOL_PILOT):
            return hasSkill(SkillType.S_PILOT_VTOL);
        case(T_VEE_GUNNER):
            return hasSkill(SkillType.S_GUN_VEE);
        case(T_AERO_PILOT):
            return hasSkill(SkillType.S_GUN_AERO) && hasSkill(SkillType.S_PILOT_AERO);
        case(T_PROTO_PILOT):
            return false;
        case(T_BA):
            return hasSkill(SkillType.S_GUN_BA);
        case(T_INFANTRY):
            return hasSkill(SkillType.S_SMALL_ARMS);
        case(T_SPACE_PILOT):
            return hasSkill(SkillType.S_PILOT_SPACE);
        case(T_SPACE_CREW):
            return hasSkill(SkillType.S_TECH_VESSEL);
        case(T_SPACE_GUNNER):
            return hasSkill(SkillType.S_GUN_SPACE);
        case(T_NAVIGATOR):
            return hasSkill(SkillType.S_NAV);
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

    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getId() {
        return id;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }
    
    public UUID getAssignedTeamId() {
        return doctorId;
    }
    
    public void setDoctorId(UUID t) {
    	this.doctorId = t;
    }
  
    public boolean checkNaturalHealing() {
        if(needsFixing() && doctorId == null) {
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
	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<person id=\""
				+id.toString()
				+"\" type=\""
				+this.getClass().getName()
				+"\">");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<name>"
				+MekHqXmlUtil.escape(name)
				+"</name>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<callsign>"
				+MekHqXmlUtil.escape(callsign)
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
				+MekHqXmlUtil.escape(biography)
				+"</biography>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<daysRest>"
				+daysRest
				+"</daysRest>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<id>"
				+this.id.toString()
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
		if(null != doctorId) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<doctorId>"
					+doctorId.toString()
					+"</doctorId>");
		}
		if(null != unitId) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<unitId>"
					+unitId.toString()
					+"</unitId>");
		}
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
			skill.writeToXml(pw1, indent+1);
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

	public static Person generateInstanceFromXML(Node wn, int version) {
		Person retVal = null;
		
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
			String pilotNickname = null;
			int pilotGunnery = -1;
			int pilotPiloting = -1;
			int pilotCommandBonus = -1;
			int type = 0;
			
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
					if(version < 14) {
						retVal.oldId = Integer.parseInt(wn2.getTextContent());
					} else {
						retVal.id = UUID.fromString(wn2.getTextContent());
					}
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
				} else if (wn2.getNodeName().equalsIgnoreCase("doctorId")) {
					if(version < 14) {
						retVal.oldDoctorId = Integer.parseInt(wn2.getTextContent());
					} else {
						if(!wn2.getTextContent().equals("null")) {
							retVal.doctorId = UUID.fromString(wn2.getTextContent());
						}
					}
				} else if (wn2.getNodeName().equalsIgnoreCase("unitId")) {
					if(version < 14) {
						retVal.oldUnitId = Integer.parseInt(wn2.getTextContent());
					} else {
						if(!wn2.getTextContent().equals("null")) {
							retVal.unitId = UUID.fromString(wn2.getTextContent());
						}
					}
				} else if (wn2.getNodeName().equalsIgnoreCase("status")) {
					retVal.status = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("salary")) {
					retVal.salary = Integer.parseInt(wn2.getTextContent());
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
				} else if (wn2.getNodeName().equalsIgnoreCase("pilotName")) {
					pilotName = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("pilotNickname")) {
					pilotNickname = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("type")) {
					type = Integer.parseInt(wn2.getTextContent());
				} else if(wn2.getNodeName().equalsIgnoreCase("skill")) {
					Skill s = Skill.generateInstanceFromXML(wn2);
					if(null != s && null != s.getType()) {
						retVal.skills.put(s.getType().getName(), s);
					}
				}
			}
			
			if(version < 13) {
				if(retVal.primaryRole > T_INFANTRY) {
					retVal.primaryRole += 4;

				}
				if(retVal.secondaryRole > T_INFANTRY) {
					retVal.secondaryRole += 4;
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
	                    MekHQ.logMessage("Error restoring advantage: " +  adv);
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
	                    MekHQ.logMessage("Error restoring edge: " +  adv);
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
	                    MekHQ.logMessage("Error restoring implants: " +  adv);
	                }
	            }
	        }
			//check to see if we are dealing with a PilotPerson from 0.1.8 or earlier
			if(pilotGunnery != -1) {
				switch(type) {
				case 0:
					retVal.addSkill(SkillType.S_GUN_MECH,7-pilotGunnery,0);
					retVal.addSkill(SkillType.S_PILOT_MECH,8-pilotPiloting,0);
					retVal.primaryRole = T_MECHWARRIOR;
					break;
				case 1:
					retVal.addSkill(SkillType.S_GUN_VEE,7-pilotGunnery,0);
					retVal.addSkill(SkillType.S_PILOT_GVEE,8-pilotPiloting,0);
					retVal.primaryRole = T_GVEE_DRIVER;
					break;
				case 2:
					retVal.addSkill(SkillType.S_GUN_AERO,7-pilotGunnery,0);
					retVal.addSkill(SkillType.S_PILOT_AERO,8-pilotPiloting,0);
					retVal.primaryRole = T_AERO_PILOT;
					break;
				case 4:
					retVal.addSkill(SkillType.S_GUN_BA,7-pilotGunnery,0);
					retVal.addSkill(SkillType.S_ANTI_MECH,8-pilotPiloting,0);
					retVal.primaryRole = T_BA;
					break;

				}
				retVal.addSkill(SkillType.S_TACTICS,pilotCommandBonus,0);
			}
			if(null != pilotName) {
				retVal.setName(pilotName);
			}
			if(null != pilotNickname) {
				retVal.setCallsign(pilotNickname);
			}
		} catch (Exception ex) {
			// Errrr, apparently either the class name was invalid...
			// Or the listed name doesn't exist.
			// Doh!
			MekHQ.logError(ex);
		}
		
		if (retVal.id == null) {
			MekHQ.logMessage("ID not pre-defined; generating person's ID.", 5);
			retVal.id = UUID.randomUUID();
		}
		
		return retVal;
	}
	
	public static int getSalary(int role) {
		int base = 0;
		switch (role) {
    	case T_MECHWARRIOR:
			base = 1500;
			break;
		case T_GVEE_DRIVER:
		case T_NVEE_DRIVER:
		case T_VTOL_PILOT:
		case T_VEE_GUNNER:
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
		case T_SPACE_PILOT:
		case T_SPACE_CREW:
		case T_SPACE_GUNNER:
		case T_NAVIGATOR:
			base = 1000;
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
		return base;
	}
	
	public int getSalary() {
		
		if(salary > -1) {
			return salary;
		}
		
		//if salary is -1, then use the standard amounts
		int primaryBase = getSalary(getPrimaryRole());
		
		switch(getExperienceLevel(false)) {
		case SkillType.EXP_ULTRA_GREEN:
			primaryBase *= 0.5;
			break;
		case SkillType.EXP_GREEN:
			primaryBase *= 0.6;
			break;
		case SkillType.EXP_VETERAN:
			primaryBase *= 1.6;
			break;
		case SkillType.EXP_ELITE:
			primaryBase *= 3.2;
			break;
		default:
		}
		
		if(getPrimaryRole() == T_ASTECH || getPrimaryRole() == T_MEDIC) {
			primaryBase *= 0.5;
		}
		
		int secondaryBase = getSalary(getSecondaryRole())/2 ;

		switch(getExperienceLevel(true)) {
		case SkillType.EXP_ULTRA_GREEN:
			secondaryBase *= 0.5;
			break;
		case SkillType.EXP_GREEN:
			secondaryBase *= 0.6;
			break;
		case SkillType.EXP_VETERAN:
			secondaryBase *= 1.6;
			break;
		case SkillType.EXP_ELITE:
			secondaryBase *= 3.2;
			break;
		default:
		}
		
		if(getSecondaryRole() == T_ASTECH || getSecondaryRole() == T_MEDIC) {
			secondaryBase *= 0.5;
		}
		
		double offMult = 0.6;
		if(ranks.isOfficer(getRank())) {
			offMult = 1.2;
		}
		
		double antiMekMult = 1.0;
		if(hasSkill(SkillType.S_ANTI_MECH)) {
			antiMekMult = 1.5;
		}
		
		return (int)((primaryBase+secondaryBase)  * offMult * antiMekMult);
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
    	return SkillType.getExperienceLevelName(getExperienceLevel(false));
	}

	public String toString() {
		return getDesc();
	}
	
	public int getExperienceLevel(boolean secondary) {
		int role = primaryRole;
		if(secondary) {
			role = secondaryRole;
		}
		switch(role) {
		case T_MECHWARRIOR:
			if(hasSkill(SkillType.S_GUN_MECH) && hasSkill(SkillType.S_PILOT_MECH)) {
				return (int)Math.floor((getSkill(SkillType.S_GUN_MECH).getExperienceLevel() 
						+ getSkill(SkillType.S_PILOT_MECH).getExperienceLevel()) / 2.0);
			} else {
				return -1;
			}
		case T_GVEE_DRIVER:
			if(hasSkill(SkillType.S_PILOT_GVEE)) {
				return getSkill(SkillType.S_PILOT_GVEE).getExperienceLevel();
			} else {
				return -1;
			}
		case T_NVEE_DRIVER:
			if(hasSkill(SkillType.S_PILOT_NVEE)) {
				return getSkill(SkillType.S_PILOT_NVEE).getExperienceLevel();
			} else {
				return -1;
			}
		case T_VTOL_PILOT:
			if(hasSkill(SkillType.S_PILOT_VTOL)) {
				return getSkill(SkillType.S_PILOT_VTOL).getExperienceLevel();
			} else {
				return -1;
			}
		case T_VEE_GUNNER:
			if(hasSkill(SkillType.S_GUN_VEE)) {
				return getSkill(SkillType.S_GUN_VEE).getExperienceLevel();
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
		case T_SPACE_PILOT:
			if(hasSkill(SkillType.S_PILOT_SPACE)) {
				return getSkill(SkillType.S_PILOT_SPACE).getExperienceLevel();
			} else {
				return -1;
			}
		case T_SPACE_CREW:
			if(hasSkill(SkillType.S_TECH_VESSEL)) {
				return getSkill(SkillType.S_TECH_VESSEL).getExperienceLevel();
			} else {
				return -1;
			}
		case T_SPACE_GUNNER:
			if(hasSkill(SkillType.S_GUN_SPACE)) {
				return getSkill(SkillType.S_GUN_SPACE).getExperienceLevel();
			} else {
				return -1;
			}
		case T_NAVIGATOR:
			if(hasSkill(SkillType.S_NAV)) {
				return getSkill(SkillType.S_NAV).getExperienceLevel();
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
	
	public String getPatientDesc(Campaign c) {
        String toReturn = "<html><font size='2'><b>" + getFullTitle() + "</b><br/>";
        Person doctor = c.getPerson(doctorId);
        if(null != doctor) {
        	toReturn += "assigned to " + doctor.getName() + "<br>";
        }
        toReturn += getHits() + " hit(s)";
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
		return Modes.MODE_NORMAL;
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
	
	public void addSkill(String skillName, int xpLvl, boolean random) {
		skills.put(skillName, new Skill(skillName, xpLvl, random));
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
			doctorId = null;
		}
	}

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
    		if(ent.getMovementMode() == EntityMovementMode.NAVAL 
    				|| ent.getMovementMode() == EntityMovementMode.HYDROFOIL
    				|| ent.getMovementMode() == EntityMovementMode.SUBMARINE) {
    			return hasSkill(SkillType.S_PILOT_NVEE);
    		} else {
    			return hasSkill(SkillType.S_PILOT_GVEE);
    		}
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
    
    public UUID getUnitId() {
    	return unitId;
    }
    
    public void setUnitId(UUID i) {
    	unitId = i;
    }
    
    public int getOldSupportTeamId() {
    	return teamId;
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
    	if(isTechPrimary() || getPrimaryRole() == T_DOCTOR) {
	        this.minutesLeft = 480;
	        this.overtimeLeft = 240;
    	}
    	if(isTechSecondary() || getSecondaryRole() == T_DOCTOR) {
        	this.minutesLeft /= 2;
        	this.overtimeLeft /= 2;
        }
    }
    
    public boolean isTech() {
    	//type must be correct and you must be more than ultra-green in the skill
    	boolean isMechTech = hasSkill(SkillType.S_TECH_MECH) && getSkill(SkillType.S_TECH_MECH).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
    	boolean isAeroTech = hasSkill(SkillType.S_TECH_AERO) && getSkill(SkillType.S_TECH_AERO).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
    	boolean isMechanic = hasSkill(SkillType.S_TECH_MECHANIC) && getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
    	boolean isBATech = hasSkill(SkillType.S_TECH_BA) && getSkill(SkillType.S_TECH_BA).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
    	return (isTechPrimary() || isTechSecondary()) && (isMechTech || isAeroTech || isMechanic || isBATech);
    }
    
    public boolean isTechPrimary() {
    	return primaryRole == T_MECH_TECH ||  primaryRole == T_AERO_TECH ||  primaryRole == T_MECHANIC ||  primaryRole == T_BA_TECH ||  primaryRole == T_SPACE_CREW;
    }
    
    public boolean isTechSecondary() {
    	return secondaryRole == T_MECH_TECH ||  secondaryRole == T_AERO_TECH ||  secondaryRole == T_MECHANIC ||  secondaryRole == T_BA_TECH;
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
    	if((unit.getEntity() instanceof SmallCraft || unit.getEntity() instanceof Jumpship)
    			&& hasSkill(SkillType.S_TECH_VESSEL)) {
    		return getSkill(SkillType.S_TECH_VESSEL);
    	}
    	if(unit.getEntity() instanceof Aero 
    			&& !(unit.getEntity() instanceof SmallCraft) 
    			&& !(unit.getEntity() instanceof Jumpship) 
    			&& hasSkill(SkillType.S_TECH_AERO)) {
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
    
    public String getDocDesc(Campaign c) {
        String toReturn = "<html><font size='2'><b>" + getName() + "</b><br/>";
        Skill skill = getSkill(SkillType.S_DOCTOR);
        if(null != skill) {
        	toReturn += SkillType.getExperienceLevelName(skill.getExperienceLevel()) + " " + SkillType.S_DOCTOR;
        }
        if(c.getMedicsPerDoctor() < 4) {
        	toReturn += "</font><font size='2' color='red'>" + ", " + c.getMedicsPerDoctor() + " medics</font><font size='2'><br>";
        } else {
        	toReturn += ", " + c.getMedicsPerDoctor() + " medics<br>";
        }
        toReturn += c.getPatientsFor(this) + " patient(s)";
        toReturn += "</font></html>";
        return toReturn;
    }
    
    public int getBestTechLevel() {
    	int lvl = -1;
    	Skill mechSkill = getSkill(SkillType.S_TECH_MECH);
        Skill mechanicSkill = getSkill(SkillType.S_TECH_MECHANIC);
        Skill baSkill = getSkill(SkillType.S_TECH_BA);
        Skill aeroSkill = getSkill(SkillType.S_TECH_AERO);
        if(null != mechSkill && mechSkill.getLevel() > lvl) {
        	lvl = mechSkill.getLevel();
        }
        if(null != mechanicSkill && mechanicSkill.getLevel() > lvl) {
        	lvl = mechanicSkill.getLevel();
        }
        if(null != baSkill && baSkill.getLevel() > lvl) {
        	lvl = baSkill.getLevel();
        }
        if(null != aeroSkill && aeroSkill.getLevel() > lvl) {
        	lvl = aeroSkill.getLevel();
        }
        return lvl;
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
    	if(unit.getEntity() instanceof SmallCraft || unit.getEntity() instanceof Jumpship) {
    		return hasSkill(SkillType.S_TECH_VESSEL);
    	}
    	if(unit.getEntity() instanceof Aero) {
    		return hasSkill(SkillType.S_TECH_AERO);
    	}
    	return false;
    }
    
    public UUID getDoctorId() {
    	return doctorId;
    }
    
    public boolean isDoctor() {
    	return primaryRole == T_DOCTOR && hasSkill(SkillType.S_DOCTOR);
    }
    
    public int getToughness() {
    	return toughness;
    }
    
    public void setToughness(int t) {
    	toughness = t;
    }
    
    public void resetSkillTypes() {
    	for(String skillName : skills.keySet()) {
    		Skill s = skills.get(skillName);
    		s.updateType();
    	}
    }
    
    public int getOldId() {
    	return oldId;
    }
    
    public void fixIdReferences(Hashtable<Integer, UUID> uHash, Hashtable<Integer, UUID> pHash) {
    	unitId = uHash.get(oldUnitId);
    	doctorId = pHash.get(oldDoctorId);
    }
}
