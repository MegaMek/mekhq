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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.ConvFighter;
import megamek.common.Crew;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.VTOL;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.LogEntry;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.IMedicalWork;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.Modes;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Person implements Serializable, MekHqXmlSerializable, IMedicalWork {
    private static final long serialVersionUID = -847642980395311152L;

    public static final int G_MALE = 0;
    public static final int G_FEMALE = 1;

    public static final int T_NONE = 0;
    public static final int T_MECHWARRIOR = 1;
    public static final int T_AERO_PILOT = 2;
    public static final int T_GVEE_DRIVER = 3;
    public static final int T_NVEE_DRIVER = 4;
    public static final int T_VTOL_PILOT = 5;
    public static final int T_VEE_GUNNER = 6;
    public static final int T_BA = 7;
    public static final int T_INFANTRY = 8;
    public static final int T_PROTO_PILOT = 9;
    public static final int T_CONV_PILOT = 10;
    public static final int T_SPACE_PILOT = 11;
    public static final int T_SPACE_CREW = 12;
    public static final int T_SPACE_GUNNER = 13;
    public static final int T_NAVIGATOR = 14;
    public static final int T_MECH_TECH = 15;
    public static final int T_MECHANIC = 16;
    public static final int T_AERO_TECH = 17;
    public static final int T_BA_TECH = 18;
    public static final int T_ASTECH = 19;
    public static final int T_DOCTOR = 20;
    public static final int T_MEDIC = 21;
    public static final int T_ADMIN_COM = 22;
    public static final int T_ADMIN_LOG = 23;
    public static final int T_ADMIN_TRA = 24;
    public static final int T_ADMIN_HR = 25;
    public static final int T_NUM = 26;

    public static final int S_ACTIVE = 0;
    public static final int S_RETIRED = 1;
    public static final int S_KIA = 2;
    public static final int S_MIA = 3;
    public static final int S_NUM = 4;

    public static final int PRONOUN_HESHE = 0;
    public static final int PRONOUN_HIMHER = 1;
    public static final int PRONOUN_HISHER = 2;
    public static final int PRONOUN_HISHERS = 3;

    // Prisoners, Bondsmen, and Normal Personnel
    public static final int PRISONER_NOT = 0;
    public static final int PRISONER_YES = 1;
    public static final int PRISONER_BONDSMAN = 2;
    public static final int PRISONER_NUM = 3;

    //phenotypes
    public static final int PHENOTYPE_NONE = 0;
    public static final int PHENOTYPE_MW = 1;
    public static final int PHENOTYPE_BA = 2;
    public static final int PHENOTYPE_AERO = 3;
    public static final int PHENOTYPE_VEE = 4;
    public static final int PHENOTYPE_NUM = 5;


    protected UUID id;
    protected int oldId;

    private String name;
    private String callsign;
    private int gender;

    private int primaryRole;
    private int secondaryRole;

    protected String biography;
    protected GregorianCalendar birthday;
    protected GregorianCalendar deathday;
    protected ArrayList<LogEntry> personnelLog;

    private Hashtable<String, Skill> skills;
    private PilotOptions options = new PilotOptions();
    private int toughness;

    private int status;
    protected int xp;
    protected int acquisitions;
    protected int salary;
    private int hits;
    private int prisonerStatus;

    boolean dependent;
    boolean commander;
    boolean isClanTech;

    //phenotype and background
    private int phenotype;
    private boolean clan;

    //assignments
    private UUID unitId;
    protected UUID doctorId;
    private ArrayList<UUID> techUnitIds;
    //for reverse compatability v0.1.8 and earlier
    protected int teamId = -1;

    //for reverse compatability
    private int oldUnitId;
    private int oldDoctorId;

    //days of rest
    protected int idleMonths;
    protected int daysToWaitForHealing;

    //portrait
    protected String portraitCategory;
    protected String portraitFile;

    //need to pass in the rank system
    private int rankOrder;

    //stuff to track for support teams
    protected int minutesLeft;
    protected int overtimeLeft;
    protected int nTasks;

    /**
     * * Start Advanced Medical ***
     */
    // Do not reorder these for backwards compatibility!
    public static final int BODY_HEAD = 0;
    public static final int BODY_LEFT_LEG = 1;
    public static final int BODY_LEFT_ARM = 2;
    public static final int BODY_CHEST = 3;
    public static final int BODY_ABDOMEN = 4;
    public static final int BODY_RIGHT_ARM = 5;
    public static final int BODY_RIGHT_LEG = 6;
    public static final int BODY_NUM = 7;

    private ArrayList<Injury> injuries = new ArrayList<Injury>();
    private int hit_location[] = new int[BODY_NUM];
    /**
     * * End Advanced Medical ***
     */

    //lets just go ahead and pass in the campaign - to hell with OOP
    private Campaign campaign;

    //default constructor
    public Person(Campaign c) {
        this("Biff the Understudy", c);
    }

    public Person(String name, Campaign c) {
        this.name = name;
        callsign = "";
        portraitCategory = Crew.ROOT_PORTRAIT;
        portraitFile = Crew.PORTRAIT_NONE;
        xp = 0;
        acquisitions = 0;
        gender = G_MALE;
        birthday = new GregorianCalendar(3042, Calendar.JANUARY, 1);
        rankOrder = 0;
        status = S_ACTIVE;
        hits = 0;
        skills = new Hashtable<String, Skill>();
        salary = -1;
        campaign = c;
        doctorId = null;
        unitId = null;
        oldDoctorId = -1;
        oldUnitId = -1;
        toughness = 0;
        biography = "";
        nTasks = 0;
        personnelLog = new ArrayList<LogEntry>();
        idleMonths = -1;
        daysToWaitForHealing = 15;
        resetMinutesLeft();
        prisonerStatus = PRISONER_NOT;
        dependent = false;
        commander = false;
        isClanTech = false;
        techUnitIds = new ArrayList<UUID>();
        salary = -1;
        phenotype = PHENOTYPE_NONE;
        clan = campaign.getFaction().isClan();
    }

    public int getPhenotype() {
        return phenotype;
    }

    public void setPhenotype(int i) {
        phenotype = i;
    }

    public boolean isClanner() {
        return clan;
    }

    public void setClanner(boolean b) {
        clan = b;
    }

    public String getBackgroundName() {
        if (isClanner()) {
            return getPhenotypeName();
        } else {
            return "Inner Sphere";
        }
    }

    public boolean isCommander() {
        return commander;
    }

    public void setCommander(boolean tf) {
        commander = tf;
    }

    public boolean isDependent() {
        return dependent;
    }

    public void setDependent(boolean tf) {
        dependent = tf;
    }

    public boolean isPrisoner() {
        if (prisonerStatus == PRISONER_YES) {
            return true;
        }
        return false;
    }

    public void setPrisoner() {
        prisonerStatus = PRISONER_YES;
    }

    public boolean isBondsman() {
        if (prisonerStatus == PRISONER_BONDSMAN) {
            return true;
        }
        return false;
    }

    public void setBondsman() {
        prisonerStatus = PRISONER_BONDSMAN;
    }

    public boolean isFree() {
        return (isPrisoner() || isBondsman());
    }

    public void setFreeMan() {
        prisonerStatus = PRISONER_NOT;
    }

    public void setPrisonerStatus(int status) {
        prisonerStatus = status;
    }

    public int getPrisonerStatus() {
        return prisonerStatus;
    }

    public String getGenderName() {
        return getGenderName(gender);
    }

    public static String getGenderName(int gender) {
        switch (gender) {
            case G_MALE:
                return "Male";
            case G_FEMALE:
                return "Female";
            default:
                return "?";
        }
    }

    public String getGenderPronoun(int variant) {
        return getGenderPronoun(gender, variant);
    }

    public static String getGenderPronoun(int gender, int variant) {
        if (variant == PRONOUN_HESHE) {
            switch (gender) {
                case G_MALE:
                    return "he";
                case G_FEMALE:
                    return "she";
                default:
                    return "?";
            }
        } else if (variant == PRONOUN_HIMHER) {
            switch (gender) {
                case G_MALE:
                    return "him";
                case G_FEMALE:
                    return "her";
                default:
                    return "?";
            }
        } else if (variant == PRONOUN_HISHER) {
            switch (gender) {
                case G_MALE:
                    return "his";
                case G_FEMALE:
                    return "her";
                default:
                    return "?";
            }
        } else if (variant == PRONOUN_HISHERS) {
            switch (gender) {
                case G_MALE:
                    return "his";
                case G_FEMALE:
                    return "hers";
                default:
                    return "?";
            }
        } else {
            return "UNKNOWN ERROR IN GENDER PRONOUN";
        }
    }

    public static String getStatusName(int status) {
        switch (status) {
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

    public String getStatusName() {
        return getStatusName(status);
    }

    public static String getPhenotypeName(int pheno) {
        switch (pheno) {
            case PHENOTYPE_NONE:
                return "Freeborn";
            case PHENOTYPE_MW:
                return "Trueborn Mechwarrior";
            case PHENOTYPE_AERO:
                return "Trueborn Pilot";
            case PHENOTYPE_VEE:
                return "Trueborn Vehicle Crew";
            case PHENOTYPE_BA:
                return "Trueborn Elemental";
            default:
                return "?";
        }
    }

    public static String getPhenotypeShortName(int pheno) {
        switch (pheno) {
            case PHENOTYPE_NONE:
                return "Freeborn";
            case PHENOTYPE_MW:
            case PHENOTYPE_AERO:
            case PHENOTYPE_VEE:
            case PHENOTYPE_BA:
                return "Trueborn";
            default:
                return "?";
        }
    }

    public String getPhenotypeName() {
        return getPhenotypeName(phenotype);
    }

    public String getPhenotypeShortName() {
        return getPhenotypeShortName(phenotype);
    }

    public static String getPrisonerStatusName(int status) {
        switch (status) {
            case PRISONER_NOT:
                return "Free";
            case PRISONER_YES:
                return "Prisoner";
            case PRISONER_BONDSMAN:
                return "Bondsman";
            default:
                return "?";
        }
    }

    public String getPrisonerStatusName() {
        return getPrisonerStatusName(prisonerStatus);
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public String getHyperlinkedName() {
        return "<a href='PERSON:" + getId() + "'>" + getName() + "</a>";
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
        if ((isTechPrimary() && secondaryRole == T_ASTECH)
            || (isTechSecondary() && primaryRole == T_ASTECH)) {
            secondaryRole = T_NONE;
        }
        if ((primaryRole == T_DOCTOR && secondaryRole == T_MEDIC)
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

    public int getIdleMonths() {
        return idleMonths;
    }

    public void setIdleMonths(int m) {
        this.idleMonths = m;
    }

    public int getDaysToWaitForHealing() {
        return daysToWaitForHealing;
    }

    public void setDaysToWaitForHealing(int d) {
        this.daysToWaitForHealing = d;
    }


    public static String getRoleDesc(int type, boolean clan) {
        switch (type) {
            case (T_NONE):
                return "None";
            case (T_MECHWARRIOR):
                return "Mechwarrior";
            case (T_GVEE_DRIVER):
                return "Vehicle Driver";
            case (T_NVEE_DRIVER):
                return "Naval Driver";
            case (T_VTOL_PILOT):
                return "VTOL Pilot";
            case (T_VEE_GUNNER):
                return "Vehicle Gunner";
            case (T_CONV_PILOT):
                return "Conventional Aircraft Pilot";
            case (T_AERO_PILOT):
                return "Aero Pilot";
            case (T_PROTO_PILOT):
                return "Proto Pilot";
            case (T_BA):
                if (clan) {
                    return "Elemental";
                } else {
                    return "Battle Armor Pilot";
                }
            case (T_INFANTRY):
                return "Soldier";
            case (T_SPACE_PILOT):
                return "Vessel Pilot";
            case (T_SPACE_CREW):
                return "Vessel Crewmember";
            case (T_SPACE_GUNNER):
                return "Vessel Gunner";
            case (T_NAVIGATOR):
                return "Hyperspace Navigator";
            case (T_MECH_TECH):
                return "Mech Tech";
            case (T_MECHANIC):
                return "Mechanic";
            case (T_AERO_TECH):
                return "Aero Tech";
            case (T_BA_TECH):
                return "Battle Armor Tech";
            case (T_ASTECH):
                return "Astech";
            case (T_DOCTOR):
                return "Doctor";
            case (T_MEDIC):
                return "Medic";
            case (T_ADMIN_COM):
                return "Admin/Command";
            case (T_ADMIN_LOG):
                return "Admin/Logistical";
            case (T_ADMIN_TRA):
                return "Admin/Transport";
            case (T_ADMIN_HR):
                return "Admin/HR";
            default:
                return "??";
        }
    }

    public String getRoleDesc() {
        String role = getPrimaryRoleDesc();
        if (secondaryRole != T_NONE && secondaryRole != -1) {
            role += "/" + getSecondaryRoleDesc();
        }
        return role;
    }

    public String getPrimaryRoleDesc() {
        String bgPrefix = "";
        if (isClanner()) {
            bgPrefix = getPhenotypeShortName() + " ";
        }
        return bgPrefix + getRoleDesc(primaryRole, campaign.getFaction().isClan());
    }

    public String getSecondaryRoleDesc() {
        return getRoleDesc(secondaryRole, campaign.getFaction().isClan());
    }

    public boolean canPerformRole(int role) {
        switch (role) {
            case (T_NONE):
                return true;
            case (T_MECHWARRIOR):
                return hasSkill(SkillType.S_GUN_MECH) && hasSkill(SkillType.S_PILOT_MECH);
            case (T_GVEE_DRIVER):
                return hasSkill(SkillType.S_PILOT_GVEE);
            case (T_NVEE_DRIVER):
                return hasSkill(SkillType.S_PILOT_NVEE);
            case (T_VTOL_PILOT):
                return hasSkill(SkillType.S_PILOT_VTOL);
            case (T_VEE_GUNNER):
                return hasSkill(SkillType.S_GUN_VEE);
            case (T_AERO_PILOT):
                return hasSkill(SkillType.S_GUN_AERO) && hasSkill(SkillType.S_PILOT_AERO);
            case (T_CONV_PILOT):
                return hasSkill(SkillType.S_GUN_JET) && hasSkill(SkillType.S_PILOT_JET);
            case (T_PROTO_PILOT):
                return hasSkill(SkillType.S_GUN_PROTO);
            case (T_BA):
                return hasSkill(SkillType.S_GUN_BA);
            case (T_INFANTRY):
                return hasSkill(SkillType.S_SMALL_ARMS);
            case (T_SPACE_PILOT):
                return hasSkill(SkillType.S_PILOT_SPACE);
            case (T_SPACE_CREW):
                return hasSkill(SkillType.S_TECH_VESSEL);
            case (T_SPACE_GUNNER):
                return hasSkill(SkillType.S_GUN_SPACE);
            case (T_NAVIGATOR):
                return hasSkill(SkillType.S_NAV);
            case (T_MECH_TECH):
                return hasSkill(SkillType.S_TECH_MECH) && getSkill(SkillType.S_TECH_MECH).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
            case (T_MECHANIC):
                return hasSkill(SkillType.S_TECH_MECHANIC) && getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
            case (T_AERO_TECH):
                return hasSkill(SkillType.S_TECH_AERO) && getSkill(SkillType.S_TECH_AERO).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
            case (T_BA_TECH):
                return hasSkill(SkillType.S_TECH_BA) && getSkill(SkillType.S_TECH_BA).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
            case (T_ASTECH):
                return hasSkill(SkillType.S_ASTECH);
            case (T_DOCTOR):
                return hasSkill(SkillType.S_DOCTOR) && getSkill(SkillType.S_DOCTOR).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
            case (T_MEDIC):
                return hasSkill(SkillType.S_MEDTECH);
            case (T_ADMIN_COM):
            case (T_ADMIN_LOG):
            case (T_ADMIN_TRA):
            case (T_ADMIN_HR):
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

    public GregorianCalendar getDeathday() {
        return deathday;
    }

    public void setDeathday(GregorianCalendar date) {
        this.deathday = date;
    }

    public int getAge(GregorianCalendar today) {
        // Get age based on year
        if (null != deathday) {
            //use deathday instead of birthdate
            today = deathday;
        }

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

    public int getAcquisitions() {
        return acquisitions;
    }

    public void setAcquisition(int a) {
        acquisitions = a;
    }

    public void incrementAcquisition() {
        acquisitions++;
    }

    public UUID getAssignedTeamId() {
        return doctorId;
    }

    public void setDoctorId(UUID t, int daysToWait) {
        this.doctorId = t;
        this.daysToWaitForHealing = daysToWait;
    }

    public boolean checkNaturalHealing(int daysToWait) {
        if (needsFixing() && daysToWaitForHealing <= 0 && doctorId == null) {
            heal();
            daysToWaitForHealing = daysToWait;
            return true;
        }
        return false;
    }

    public void decrementDaysToWaitForHealing() {
        if (daysToWaitForHealing > 0) {
            daysToWaitForHealing--;
        }
    }

    public boolean isDeployed() {
        Unit u = campaign.getUnit(unitId);
        if (null != u) {
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

    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<person id=\""
                    + id.toString()
                    + "\" type=\""
                    + this.getClass().getName()
                    + "\">");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<name>"
                    + MekHqXmlUtil.escape(name)
                    + "</name>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<callsign>"
                    + MekHqXmlUtil.escape(callsign)
                    + "</callsign>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<primaryRole>"
                    + primaryRole
                    + "</primaryRole>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<secondaryRole>"
                    + secondaryRole
                    + "</secondaryRole>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<commander>"
                    + MekHqXmlUtil.escape(Boolean.toString(commander))
                    + "</commander>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<dependent>"
                    + MekHqXmlUtil.escape(Boolean.toString(dependent))
                    + "</dependent>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<clan>"
                    + clan
                    + "</clan>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<phenotype>"
                    + phenotype
                    + "</phenotype>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<biography>"
                    + MekHqXmlUtil.escape(biography)
                    + "</biography>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<idleMonths>"
                    + idleMonths
                    + "</idleMonths>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<id>"
                    + this.id.toString()
                    + "</id>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<portraitCategory>"
                    + portraitCategory
                    + "</portraitCategory>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<portraitFile>"
                    + portraitFile
                    + "</portraitFile>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<xp>"
                    + xp
                    + "</xp>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<daysToWaitForHealing>"
                    + daysToWaitForHealing
                    + "</daysToWaitForHealing>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<gender>"
                    + gender
                    + "</gender>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<rank>"
                    + rankOrder
                    + "</rank>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<nTasks>"
                    + nTasks
                    + "</nTasks>");
        if (null != doctorId) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                        + "<doctorId>"
                        + doctorId.toString()
                        + "</doctorId>");
        }
        if (null != unitId) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                        + "<unitId>"
                        + unitId.toString()
                        + "</unitId>");
        }
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<salary>"
                    + salary
                    + "</salary>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<status>"
                    + status
                    + "</status>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<prisonerstatus>"
                    + prisonerStatus
                    + "</prisonerstatus>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<hits>"
                    + hits
                    + "</hits>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<toughness>"
                    + toughness
                    + "</toughness>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<minutesLeft>"
                    + minutesLeft
                    + "</minutesLeft>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<overtimeLeft>"
                    + overtimeLeft
                    + "</overtimeLeft>");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<birthday>"
                    + df.format(birthday.getTime())
                    + "</birthday>");
        if (null != deathday) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                        + "<deathday>"
                        + df.format(deathday.getTime())
                        + "</deathday>");
        }
        for (String skName : skills.keySet()) {
            Skill skill = skills.get(skName);
            skill.writeToXml(pw1, indent + 1);
        }
        if (countOptions(PilotOptions.LVL3_ADVANTAGES) > 0) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                        + "<advantages>"
                        + String.valueOf(getOptionList("::", PilotOptions.LVL3_ADVANTAGES))
                        + "</advantages>");
        }
        if (countOptions(PilotOptions.EDGE_ADVANTAGES) > 0) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                        + "<edge>"
                        + String.valueOf(getOptionList("::", PilotOptions.EDGE_ADVANTAGES))
                        + "</edge>");
        }
        if (countOptions(PilotOptions.MD_ADVANTAGES) > 0) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                        + "<implants>"
                        + String.valueOf(getOptionList("::", PilotOptions.MD_ADVANTAGES))
                        + "</implants>");
        }
        if (!techUnitIds.isEmpty()) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<techUnitIds>");
            for (UUID id : techUnitIds) {
                pw1.println(MekHqXmlUtil.indentStr(indent + 2)
                            + "<id>"
                            + id.toString()
                            + "</id>");
            }
            pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "</techUnitIds>");
        }
        if (!personnelLog.isEmpty()) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<personnelLog>");
            for (LogEntry entry : personnelLog) {
                entry.writeToXml(pw1, indent + 2);
            }
            pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "</personnelLog>");
        }
        if (injuries.size() > 0) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<injuries>");
            for (Injury injury : injuries) {
                injury.writeToXml(pw1, indent + 2);
            }
            pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "</injuries>");
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</person>");
    }

    public static Person generateInstanceFromXML(Node wn, Campaign c, Version version) {
        Person retVal = null;

        try {
            // Instantiate the correct child class, and call its parsing function.
            retVal = new Person(c);

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

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.name = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("callsign")) {
                    retVal.callsign = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("commander")) {
                    if (wn2.getTextContent().equalsIgnoreCase("true")) {
                        retVal.commander = true;
                    } else {
                        retVal.commander = false;
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("dependent")) {
                    if (wn2.getTextContent().equalsIgnoreCase("true")) {
                        retVal.dependent = true;
                    } else {
                        retVal.dependent = false;
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("isClanTech")
                           || wn2.getNodeName().equalsIgnoreCase("clan")) {
                    retVal.clan = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("phenotype")) {
                    retVal.phenotype = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("biography")) {
                    retVal.biography = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("primaryRole")) {
                    retVal.primaryRole = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("secondaryRole")) {
                    retVal.secondaryRole = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("daysToWaitForHealing")) {
                    retVal.daysToWaitForHealing = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("idleMonths")) {
                    retVal.idleMonths = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("id")) {
                    if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
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
                } else if (wn2.getNodeName().equalsIgnoreCase("nTasks")) {
                    retVal.nTasks = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("hits")) {
                    retVal.hits = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("gender")) {
                    retVal.gender = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("rank")) {
                    retVal.rankOrder = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("doctorId")) {
                    if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
                        retVal.oldDoctorId = Integer.parseInt(wn2.getTextContent());
                    } else {
                        if (!wn2.getTextContent().equals("null")) {
                            retVal.doctorId = UUID.fromString(wn2.getTextContent());
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("unitId")) {
                    if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 14) {
                        retVal.oldUnitId = Integer.parseInt(wn2.getTextContent());
                    } else {
                        if (!wn2.getTextContent().equals("null")) {
                            retVal.unitId = UUID.fromString(wn2.getTextContent());
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("status")) {
                    retVal.status = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("prisonerstatus")) {
                    retVal.prisonerStatus = Integer.parseInt(wn2.getTextContent());
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
                } else if (wn2.getNodeName().equalsIgnoreCase("deathday")) {
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    retVal.deathday = (GregorianCalendar) GregorianCalendar.getInstance();
                    retVal.deathday.setTime(df.parse(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("advantages")) {
                    advantages = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("edge")) {
                    edge = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("implants")) {
                    implants = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("toughness")) {
                    retVal.toughness = Integer.parseInt(wn2.getTextContent());
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
                } else if (wn2.getNodeName().equalsIgnoreCase("skill")) {
                    Skill s = Skill.generateInstanceFromXML(wn2);
                    if (null != s && null != s.getType()) {
                        retVal.skills.put(s.getType().getName(), s);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("techUnitIds")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("id")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.logMessage("Unknown node type not loaded in techUnitIds nodes: " + wn3.getNodeName());
                            continue;
                        }
                        retVal.techUnitIds.add(UUID.fromString(wn3.getTextContent()));

                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelLog")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("logEntry")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.logMessage("Unknown node type not loaded in personnel log nodes: " + wn3.getNodeName());
                            continue;
                        }
                        retVal.addLogEntry(LogEntry.generateInstanceFromXML(wn3));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("injuries")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("injury")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.logMessage("Unknown node type not loaded in injury nodes: " + wn3.getNodeName());
                            continue;
                        }
                        retVal.injuries.add(Injury.generateInstanceFromXML(wn3));
                    }
                }
            }

            if (version.getMajorVersion() == 0 && version.getMinorVersion() < 2 && version.getSnapshot() < 13) {
                if (retVal.primaryRole > T_INFANTRY) {
                    retVal.primaryRole += 4;

                }
                if (retVal.secondaryRole > T_INFANTRY) {
                    retVal.secondaryRole += 4;
                }
            }

            if (version.getMajorVersion() == 0 && version.getMinorVersion() < 3 && version.getMinorVersion() > 1) {
                //adjust for conventional fighter pilots
                if (retVal.primaryRole >= T_CONV_PILOT) {
                    retVal.primaryRole += 1;
                }
                if (retVal.secondaryRole >= T_CONV_PILOT) {
                    retVal.secondaryRole += 1;
                }
            }

            if (version.getMajorVersion() == 0 && version.getMinorVersion() == 3 && version.getSnapshot() < 1) {
                //adjust for conventional fighter pilots
                if (retVal.primaryRole == T_CONV_PILOT && retVal.hasSkill(SkillType.S_PILOT_SPACE) && !retVal.hasSkill(SkillType.S_PILOT_JET)) {
                    retVal.primaryRole += 1;
                }
                if (retVal.secondaryRole == T_CONV_PILOT && retVal.hasSkill(SkillType.S_PILOT_SPACE) && !retVal.hasSkill(SkillType.S_PILOT_JET)) {
                    retVal.secondaryRole += 1;
                }
                if (retVal.primaryRole == T_AERO_PILOT && !retVal.hasSkill(SkillType.S_PILOT_SPACE) && retVal.hasSkill(SkillType.S_PILOT_JET)) {
                    retVal.primaryRole += 8;
                }
                if (retVal.secondaryRole == T_AERO_PILOT && !retVal.hasSkill(SkillType.S_PILOT_SPACE) && retVal.hasSkill(SkillType.S_PILOT_JET)) {
                    retVal.secondaryRole += 8;
                }
            }

            if ((null != advantages) && (advantages.trim().length() > 0)) {
                StringTokenizer st = new StringTokenizer(advantages, "::");
                while (st.hasMoreTokens()) {
                    String adv = st.nextToken();
                    String advName = Crew.parseAdvantageName(adv);
                    Object value = Crew.parseAdvantageValue(adv);

                    try {
                        retVal.getOptions().getOption(advName).setValue(value);
                    } catch (Exception e) {
                        MekHQ.logMessage("Error restoring advantage: " + adv);
                    }
                }
            }
            if ((null != edge) && (edge.trim().length() > 0)) {
                StringTokenizer st = new StringTokenizer(edge, "::");
                while (st.hasMoreTokens()) {
                    String adv = st.nextToken();
                    String advName = Crew.parseAdvantageName(adv);
                    Object value = Crew.parseAdvantageValue(adv);

                    try {
                        retVal.getOptions().getOption(advName).setValue(value);
                    } catch (Exception e) {
                        MekHQ.logMessage("Error restoring edge: " + adv);
                    }
                }
            }
            if ((null != implants) && (implants.trim().length() > 0)) {
                StringTokenizer st = new StringTokenizer(implants, "::");
                while (st.hasMoreTokens()) {
                    String adv = st.nextToken();
                    String advName = Crew.parseAdvantageName(adv);
                    Object value = Crew.parseAdvantageValue(adv);

                    try {
                        retVal.getOptions().getOption(advName).setValue(value);
                    } catch (Exception e) {
                        MekHQ.logMessage("Error restoring implants: " + adv);
                    }
                }
            }
            //check to see if we are dealing with a PilotPerson from 0.1.8 or earlier
            if (pilotGunnery != -1) {
                switch (type) {
                    case 0:
                        retVal.addSkill(SkillType.S_GUN_MECH, 7 - pilotGunnery, 0);
                        retVal.addSkill(SkillType.S_PILOT_MECH, 8 - pilotPiloting, 0);
                        retVal.primaryRole = T_MECHWARRIOR;
                        break;
                    case 1:
                        retVal.addSkill(SkillType.S_GUN_VEE, 7 - pilotGunnery, 0);
                        retVal.addSkill(SkillType.S_PILOT_GVEE, 8 - pilotPiloting, 0);
                        retVal.primaryRole = T_GVEE_DRIVER;
                        break;
                    case 2:
                        retVal.addSkill(SkillType.S_GUN_AERO, 7 - pilotGunnery, 0);
                        retVal.addSkill(SkillType.S_PILOT_AERO, 8 - pilotPiloting, 0);
                        retVal.primaryRole = T_AERO_PILOT;
                        break;
                    case 4:
                        retVal.addSkill(SkillType.S_GUN_BA, 7 - pilotGunnery, 0);
                        retVal.addSkill(SkillType.S_ANTI_MECH, 8 - pilotPiloting, 0);
                        retVal.primaryRole = T_BA;
                        break;

                }
                retVal.addSkill(SkillType.S_TACTICS, pilotCommandBonus, 0);
            }
            if (null != pilotName) {
                retVal.setName(pilotName);
            }
            if (null != pilotNickname) {
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

    public void setSalary(int s) {
        salary = s;
    }

    public int getSalary() {

        if (isPrisoner() || isBondsman()) {
            return 0;
        }

        if (salary > -1) {
            return salary;
        }

        //if salary is -1, then use the standard amounts
        int primaryBase = campaign.getCampaignOptions().getBaseSalary(getPrimaryRole());
        primaryBase *= campaign.getCampaignOptions().getSalaryXpMultiplier(getExperienceLevel(false));
        if (hasSkill(SkillType.S_ANTI_MECH) && (getPrimaryRole() == T_INFANTRY || getPrimaryRole() == T_BA)) {
            primaryBase *= campaign.getCampaignOptions().getSalaryAntiMekMultiplier();
        }

        int secondaryBase = campaign.getCampaignOptions().getBaseSalary(getSecondaryRole()) / 2;
        secondaryBase *= campaign.getCampaignOptions().getSalaryXpMultiplier(getExperienceLevel(true));
        if (hasSkill(SkillType.S_ANTI_MECH) && (getSecondaryRole() == T_INFANTRY || getSecondaryRole() == T_BA)) {
            secondaryBase *= campaign.getCampaignOptions().getSalaryAntiMekMultiplier();
        }

        int totalBase = primaryBase + secondaryBase;

        if (getRank().isOfficer()) {
            totalBase *= campaign.getCampaignOptions().getSalaryCommissionMultiplier();
        } else {
            totalBase *= campaign.getCampaignOptions().getSalaryEnlistedMultiplier();
        }

        totalBase *= getRank().getPayMultiplier();

        return totalBase;
        //TODO: distinguish dropship, jumpship, and warship crew
        //TODO: Add era mod to salary calc..
    }

    public int getRankOrder() {
        return rankOrder;
    }

    public Rank getRank() {
        return campaign.getRanks().getRank(rankOrder);
    }

    public void setRank(int r) {
        this.rankOrder = r;
    }

    public String getSkillSummary() {
        return SkillType.getExperienceLevelName(getExperienceLevel(false));
    }

    public String toString() {
        return getName();
    }

    public int getExperienceLevel(boolean secondary) {
        int role = primaryRole;
        if (secondary) {
            role = secondaryRole;
        }
        switch (role) {
            case T_MECHWARRIOR:
                if (hasSkill(SkillType.S_GUN_MECH) && hasSkill(SkillType.S_PILOT_MECH)) {
                    return (int) Math.floor((getSkill(SkillType.S_GUN_MECH).getExperienceLevel()
                                             + getSkill(SkillType.S_PILOT_MECH).getExperienceLevel()) / 2.0);
                } else {
                    return -1;
                }
            case T_GVEE_DRIVER:
                if (hasSkill(SkillType.S_PILOT_GVEE)) {
                    return getSkill(SkillType.S_PILOT_GVEE).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_NVEE_DRIVER:
                if (hasSkill(SkillType.S_PILOT_NVEE)) {
                    return getSkill(SkillType.S_PILOT_NVEE).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_VTOL_PILOT:
                if (hasSkill(SkillType.S_PILOT_VTOL)) {
                    return getSkill(SkillType.S_PILOT_VTOL).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_VEE_GUNNER:
                if (hasSkill(SkillType.S_GUN_VEE)) {
                    return getSkill(SkillType.S_GUN_VEE).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_AERO_PILOT:
                if (hasSkill(SkillType.S_GUN_AERO) && hasSkill(SkillType.S_PILOT_AERO)) {
                    return (int) Math.floor((getSkill(SkillType.S_GUN_AERO).getExperienceLevel()
                                             + getSkill(SkillType.S_PILOT_AERO).getExperienceLevel()) / 2.0);
                } else {
                    return -1;
                }
            case T_CONV_PILOT:
                if (hasSkill(SkillType.S_GUN_JET) && hasSkill(SkillType.S_PILOT_JET)) {
                    return (int) Math.floor((getSkill(SkillType.S_GUN_JET).getExperienceLevel()
                                             + getSkill(SkillType.S_PILOT_JET).getExperienceLevel()) / 2.0);
                } else {
                    return -1;
                }
            case T_BA:
                if (hasSkill(SkillType.S_GUN_BA) && hasSkill(SkillType.S_ANTI_MECH)) {
                    return (int) Math.floor((getSkill(SkillType.S_GUN_BA).getExperienceLevel()
                                             + getSkill(SkillType.S_ANTI_MECH).getExperienceLevel()) / 2.0);
                } else {
                    return -1;
                }
            case T_PROTO_PILOT:
                if (hasSkill(SkillType.S_GUN_PROTO)) {
                    return getSkill(SkillType.S_GUN_PROTO).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_INFANTRY:
                if (hasSkill(SkillType.S_SMALL_ARMS)) {
                    return getSkill(SkillType.S_SMALL_ARMS).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_SPACE_PILOT:
                if (hasSkill(SkillType.S_PILOT_SPACE)) {
                    return getSkill(SkillType.S_PILOT_SPACE).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_SPACE_CREW:
                if (hasSkill(SkillType.S_TECH_VESSEL)) {
                    return getSkill(SkillType.S_TECH_VESSEL).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_SPACE_GUNNER:
                if (hasSkill(SkillType.S_GUN_SPACE)) {
                    return getSkill(SkillType.S_GUN_SPACE).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_NAVIGATOR:
                if (hasSkill(SkillType.S_NAV)) {
                    return getSkill(SkillType.S_NAV).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_MECH_TECH:
                if (hasSkill(SkillType.S_TECH_MECH)) {
                    return getSkill(SkillType.S_TECH_MECH).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_MECHANIC:
                if (hasSkill(SkillType.S_TECH_MECHANIC)) {
                    return getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_AERO_TECH:
                if (hasSkill(SkillType.S_TECH_AERO)) {
                    return getSkill(SkillType.S_TECH_AERO).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_BA_TECH:
                if (hasSkill(SkillType.S_TECH_BA)) {
                    return getSkill(SkillType.S_TECH_BA).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_ASTECH:
                if (hasSkill(SkillType.S_ASTECH)) {
                    return getSkill(SkillType.S_ASTECH).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_DOCTOR:
                if (hasSkill(SkillType.S_DOCTOR)) {
                    return getSkill(SkillType.S_DOCTOR).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_MEDIC:
                if (hasSkill(SkillType.S_MEDTECH)) {
                    return getSkill(SkillType.S_MEDTECH).getExperienceLevel();
                } else {
                    return -1;
                }
            case T_ADMIN_COM:
            case T_ADMIN_LOG:
            case T_ADMIN_TRA:
            case T_ADMIN_HR:
                if (hasSkill(SkillType.S_ADMIN)) {
                    return getSkill(SkillType.S_ADMIN).getExperienceLevel();
                } else {
                    return -1;
                }
            default:
                return -1;
        }
    }

    public String getPatientDesc() {
        String toReturn = "<html><font size='2'><b>" + getFullTitle() + "</b><br/>";
        toReturn += getHits() + " hit(s)<br>[next check in " + getDaysToWaitForHealing() + " days]";
        toReturn += "</font></html>";
        return toReturn;
    }

    /**
     * returns a full description in html format that will be used for the graphical display in the personnel table
     *
     * @return
     */
    public String getFullDesc() {
        String toReturn = "<html><font size='2'><b>" + getFullTitle() + "</b><br/>";
        toReturn += getSkillSummary() + " " + getRoleDesc();
        toReturn += "</font></html>";
        return toReturn;
    }

    public String getFullTitle() {
        String rank = getRank().getName();
        if (rank.equalsIgnoreCase("None")) {
            if (isPrisoner()) {
                return "Prisoner " + getName();
            }
            if (isBondsman()) {
                return "Bondsman " + getName();
            }
            return getName();
        }
        return rank + " " + getName();
    }

    public String getHyperlinkedFullTitle() {
        return "<a href='PERSON:" + getId() + "'>" + getFullTitle() + "</a>";
    }

    @Override
    public int getMode() {
        return Modes.MODE_NORMAL;
    }


    @Override
    public int getDifficulty() {
        if (campaign.getCampaignOptions().useTougherHealing()) {
            return Math.max(0, getHits() - 2);
        }
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

    public void addSkill(String skillName, int xpLvl, boolean random, int bonus) {
        skills.put(skillName, new Skill(skillName, xpLvl, random, bonus));
    }

    public void removeSkill(String skillName) {
        if (hasSkill(skillName)) {
            skills.remove(skillName);
        }
    }

    public void improveSkill(String skillName) {
        if (hasSkill(skillName)) {
            getSkill(skillName).improve();
        } else {
            addSkill(skillName, 0, 0);
        }
    }

    public int getCostToImprove(String skillName) {
        if (hasSkill(skillName)) {
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
        if (!needsFixing()) {
            doctorId = null;
        }
    }

    @Override
    public boolean needsFixing() {
        return (hits > 0 || injuries.size() > 0) && status != S_KIA && status == S_ACTIVE;
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
        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (group.getKey().equalsIgnoreCase(grpKey)) {
                return group.getOptions();
            }
        }

        // no pilot advantages -- return an empty Enumeration
        return new Vector<IOption>().elements();
    }

    public ArrayList<String> getAvailableOptions() {
        ArrayList<String> available = new ArrayList<String>();
        for (String name : SkillType.getAbilitiesFor(getPrimaryRole())) {
            IOption option = getOptions().getOption(name);
            if (null == option || option.booleanValue()) {
                continue;
            }
            available.add(option.getName());
        }
        return available;
    }

    public int countOptions() {
        int count = 0;

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
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

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(grpKey)) {
                continue;
            }

            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();

                if (option.booleanValue()) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Returns a string of all the option "codes" for this pilot, for a given group, using sep as the separator
     */
    public String getOptionList(String sep, String grpKey) {
        StringBuffer adv = new StringBuffer();

        if (null == sep) {
            sep = "";
        }

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            if (!group.getKey().equalsIgnoreCase(grpKey)) {
                continue;
            }
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
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
        for (Enumeration<IOption> i = getOptions(PilotOptions.EDGE_ADVANTAGES); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (ability.getName().equals("edge")) {
                ability.setValue(e);
            }
        }
    }

    /**
     * This will flip the boolean status of the current edge trigger
     *
     * @param name
     */
    public void changeEdgeTrigger(String name) {
        for (Enumeration<IOption> i = getOptions(PilotOptions.EDGE_ADVANTAGES); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (ability.getName().equals(name)) {
                ability.setValue(!ability.booleanValue());
            }
        }
    }

    /**
     * This function returns an html-coded tooltip that says what edge will be used
     *
     * @return
     */
    public String getEdgeTooltip() {
        String edgett = "";
        for (Enumeration<IOption> i = getOptions(PilotOptions.EDGE_ADVANTAGES); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            //yuck, it would be nice to have a more fool-proof way of identifying edge triggers
            if (ability.getName().contains("edge_when") && ability.booleanValue()) {
                edgett = edgett + ability.getDescription() + "<br>";
            }
        }
        if (edgett.equals("")) {
            return "No triggers set";
        }
        return "<html>" + edgett + "</html>";
    }

    /**
     * This function returns an html-coded list that says what abilities are enabled for this pilot
     *
     * @return
     */
    public String getAbilityList(String type) {
        String abilityString = "";
        for (Enumeration<IOption> i = getOptions(type); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (ability.booleanValue()) {
                abilityString = abilityString + Utilities.getOptionDisplayName(ability) + "<br>";
            }
        }
        if (abilityString.equals("")) {
            return null;
        }
        return "<html>" + abilityString + "</html>";
    }

    public void acquireAbility(String type, String name, Object value) {
        for (Enumeration<IOption> i = getOptions(type); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (ability.getName().equals(name)) {
                ability.setValue(value);
            }
        }
    }

    public boolean isSupport() {
        return primaryRole >= T_MECH_TECH || secondaryRole >= T_MECH_TECH;
    }

    public boolean canDrive(Entity ent) {
        if (ent instanceof Mech) {
            return hasSkill(SkillType.S_PILOT_MECH);
        } else if (ent instanceof VTOL) {
            return hasSkill(SkillType.S_PILOT_VTOL);
        } else if (ent instanceof Tank) {
            if (ent.getMovementMode() == EntityMovementMode.NAVAL
                || ent.getMovementMode() == EntityMovementMode.HYDROFOIL
                || ent.getMovementMode() == EntityMovementMode.SUBMARINE) {
                return hasSkill(SkillType.S_PILOT_NVEE);
            } else {
                return hasSkill(SkillType.S_PILOT_GVEE);
            }
        } else if (ent instanceof ConvFighter) {
            return hasSkill(SkillType.S_PILOT_JET) || hasSkill(SkillType.S_PILOT_AERO);
        } else if (ent instanceof SmallCraft || ent instanceof Jumpship) {
            return hasSkill(SkillType.S_PILOT_SPACE);
        } else if (ent instanceof Aero) {
            return hasSkill(SkillType.S_PILOT_AERO);
        } else if (ent instanceof BattleArmor) {
            return hasSkill(SkillType.S_GUN_BA);
        } else if (ent instanceof Infantry) {
            return hasSkill(SkillType.S_SMALL_ARMS);
        } else if (ent instanceof Protomech) {
            return hasSkill(SkillType.S_GUN_PROTO);
        }
        return false;
    }

    public boolean canGun(Entity ent) {
        if (ent instanceof Mech) {
            return hasSkill(SkillType.S_GUN_MECH);
        } else if (ent instanceof Tank) {
            return hasSkill(SkillType.S_GUN_VEE);
        } else if (ent instanceof ConvFighter) {
            return hasSkill(SkillType.S_GUN_JET) || hasSkill(SkillType.S_GUN_AERO);
        } else if (ent instanceof SmallCraft || ent instanceof Jumpship) {
            return hasSkill(SkillType.S_GUN_SPACE);
        } else if (ent instanceof Aero) {
            return hasSkill(SkillType.S_GUN_AERO);
        } else if (ent instanceof BattleArmor) {
            return hasSkill(SkillType.S_GUN_BA);
        } else if (ent instanceof Infantry) {
            return hasSkill(SkillType.S_SMALL_ARMS);
        } else if (ent instanceof Protomech) {
            return hasSkill(SkillType.S_GUN_PROTO);
        }
        return false;
    }

    public boolean canTech(Entity ent) {
        if (ent instanceof Mech || ent instanceof Protomech) {
            return hasSkill(SkillType.S_TECH_MECH);
        } else if (ent instanceof Aero) {
            return hasSkill(SkillType.S_TECH_AERO);
        } else if (ent instanceof BattleArmor) {
            return hasSkill(SkillType.S_TECH_BA);
        } else if (ent instanceof Tank) {
            return hasSkill(SkillType.S_TECH_MECHANIC);
        }
        return false;
    }

    public int getMaintenanceTimeUsing() {
        int time = 0;
        for (UUID id : getTechUnitIDs()) {
            Unit u = campaign.getUnit(id);
            if (null != u) {
                time += u.getMaintenanceTime();
            }
        }
        return time;
    }

    public boolean isMothballing() {
        if (!isTech()) {
            return false;
        }
        for (Unit u : campaign.getUnits()) {
            if (u.isMothballing() && u.getTechId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public UUID getUnitId() {
        return unitId;
    }

    public void setUnitId(UUID i) {
        unitId = i;
    }

    public void removeTechUnitId(UUID i) {
        techUnitIds.remove(i);
    }

    public void addTechUnitID(UUID i) {
        techUnitIds.add(i);
    }

    public ArrayList<UUID> getTechUnitIDs() {
        return techUnitIds;
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
        if (isTechPrimary() || getPrimaryRole() == T_DOCTOR) {
            this.minutesLeft = 480;
            this.overtimeLeft = 240;
        }
        if (isTechSecondary() || getSecondaryRole() == T_DOCTOR) {
            this.minutesLeft = 240;
            this.overtimeLeft = 240;
        }
    }

    public boolean isAdmin() {
        return (isAdminPrimary() || isAdminSecondary());
    }

    public boolean isAdminPrimary() {
        return primaryRole == T_ADMIN_HR || primaryRole == T_ADMIN_COM || primaryRole == T_ADMIN_LOG || primaryRole == T_ADMIN_TRA;
    }

    public boolean isAdminSecondary() {
        return secondaryRole == T_ADMIN_HR || secondaryRole == T_ADMIN_COM || secondaryRole == T_ADMIN_LOG || secondaryRole == T_ADMIN_TRA;
    }

    public Skill getBestTechSkill() {
        Skill skill = null;
        int lvl = -1;
        if (hasSkill(SkillType.S_TECH_MECH) && getSkill(SkillType.S_TECH_MECH).getExperienceLevel() > lvl) {
            skill = getSkill(SkillType.S_TECH_MECH);
            lvl = getSkill(SkillType.S_TECH_MECH).getExperienceLevel();
        }
        if (hasSkill(SkillType.S_TECH_AERO) && getSkill(SkillType.S_TECH_AERO).getExperienceLevel() > lvl) {
            skill = getSkill(SkillType.S_TECH_AERO);
            lvl = getSkill(SkillType.S_TECH_AERO).getExperienceLevel();
        }
        if (hasSkill(SkillType.S_TECH_MECHANIC) && getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() > lvl) {
            skill = getSkill(SkillType.S_TECH_MECHANIC);
            lvl = getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel();
        }
        if (hasSkill(SkillType.S_TECH_BA) && getSkill(SkillType.S_TECH_BA).getExperienceLevel() > lvl) {
            skill = getSkill(SkillType.S_TECH_BA);
            lvl = getSkill(SkillType.S_TECH_BA).getExperienceLevel();
        }
        return skill;
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
        return primaryRole == T_MECH_TECH || primaryRole == T_AERO_TECH || primaryRole == T_MECHANIC || primaryRole == T_BA_TECH || primaryRole == T_SPACE_CREW;
    }

    public boolean isTechSecondary() {
        return secondaryRole == T_MECH_TECH || secondaryRole == T_AERO_TECH || secondaryRole == T_MECHANIC || secondaryRole == T_BA_TECH;
    }

    public boolean isTaskOvertime(IPartWork partWork) {
        return partWork.getTimeLeft() > getMinutesLeft()
               && (partWork.getTimeLeft() - getMinutesLeft()) <= getOvertimeLeft();
    }

    public Skill getSkillForWorkingOn(IPartWork part) {
        Unit unit = part.getUnit();
        Skill skill = getSkillForWorkingOn(unit);
        if (null != skill) {
            return skill;
        }
        //check spare parts
        //return the best one
        if (part.isRightTechType(SkillType.S_TECH_MECH) && hasSkill(SkillType.S_TECH_MECH)) {
            skill = getSkill(SkillType.S_TECH_MECH);
        }
        if (part.isRightTechType(SkillType.S_TECH_BA) && hasSkill(SkillType.S_TECH_BA)) {
            if (null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_BA).getFinalSkillValue()) {
                skill = getSkill(SkillType.S_TECH_BA);
            }
        }
        if (part.isRightTechType(SkillType.S_TECH_AERO) && hasSkill(SkillType.S_TECH_AERO)) {
            if (null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_AERO).getFinalSkillValue()) {
                skill = getSkill(SkillType.S_TECH_AERO);
            }
        }
        if (part.isRightTechType(SkillType.S_TECH_MECHANIC) && hasSkill(SkillType.S_TECH_MECHANIC)) {
            if (null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_MECHANIC).getFinalSkillValue()) {
                skill = getSkill(SkillType.S_TECH_MECHANIC);
            }
        }
        if (part.isRightTechType(SkillType.S_TECH_VESSEL) && hasSkill(SkillType.S_TECH_VESSEL)) {
            if (null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_VESSEL).getFinalSkillValue()) {
                skill = getSkill(SkillType.S_TECH_VESSEL);
            }
        }
        if (null != skill) {
            return skill;
        }
        //if we are still here then we didn't have the right tech skill, so return the highest
        //of any tech skills that we do have
        if (hasSkill(SkillType.S_TECH_MECH)) {
            skill = getSkill(SkillType.S_TECH_MECH);
        }
        if (hasSkill(SkillType.S_TECH_BA)) {
            if (null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_BA).getFinalSkillValue()) {
                skill = getSkill(SkillType.S_TECH_BA);
            }
        }
        if (hasSkill(SkillType.S_TECH_MECHANIC)) {
            if (null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_MECHANIC).getFinalSkillValue()) {
                skill = getSkill(SkillType.S_TECH_MECHANIC);
            }
        }
        if (hasSkill(SkillType.S_TECH_AERO)) {
            if (null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_AERO).getFinalSkillValue()) {
                skill = getSkill(SkillType.S_TECH_AERO);
            }
        }
        return skill;
    }

    public Skill getSkillForWorkingOn(Unit unit) {
        if (null != unit && unit.getEntity() instanceof Mech && hasSkill(SkillType.S_TECH_MECH)) {
            return getSkill(SkillType.S_TECH_MECH);
        }
        if (null != unit && unit.getEntity() instanceof BattleArmor && hasSkill(SkillType.S_TECH_BA)) {
            return getSkill(SkillType.S_TECH_BA);
        }
        if (null != unit && unit.getEntity() instanceof Tank && hasSkill(SkillType.S_TECH_MECHANIC)) {
            return getSkill(SkillType.S_TECH_MECHANIC);
        }
        if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)
            && hasSkill(SkillType.S_TECH_VESSEL)) {
            return getSkill(SkillType.S_TECH_VESSEL);
        }
        if (null != unit && unit.getEntity() instanceof Aero
            && !(unit.getEntity() instanceof Dropship)
            && !(unit.getEntity() instanceof Jumpship)
            && hasSkill(SkillType.S_TECH_AERO)) {
            return getSkill(SkillType.S_TECH_AERO);
        }
        return null;
    }

    public Skill getSkillForWorkingOn(IAcquisitionWork acquisition, String skillName) {
        if (skillName.equals(CampaignOptions.S_TECH)) {
            return getBestTechSkill();
        }
        if (hasSkill(skillName)) {
            return getSkill(skillName);
        }
        return null;
    }

    public String getDocDesc() {
        String toReturn = "<html><font size='2'><b>" + getName() + "</b><br/>";
        Skill skill = getSkill(SkillType.S_DOCTOR);
        if (null != skill) {
            toReturn += SkillType.getExperienceLevelName(skill.getExperienceLevel()) + " " + SkillType.S_DOCTOR;
        }
        if (campaign.getMedicsPerDoctor() < 4) {
            toReturn += "</font><font size='2' color='red'>" + ", " + campaign.getMedicsPerDoctor() + " medics</font><font size='2'><br>";
        } else {
            toReturn += ", " + campaign.getMedicsPerDoctor() + " medics<br>";
        }
        toReturn += campaign.getPatientsFor(this) + " patient(s)";
        toReturn += "</font></html>";
        return toReturn;
    }

    public int getBestTechLevel() {
        int lvl = -1;
        Skill mechSkill = getSkill(SkillType.S_TECH_MECH);
        Skill mechanicSkill = getSkill(SkillType.S_TECH_MECHANIC);
        Skill baSkill = getSkill(SkillType.S_TECH_BA);
        Skill aeroSkill = getSkill(SkillType.S_TECH_AERO);
        if (null != mechSkill && mechSkill.getLevel() > lvl) {
            lvl = mechSkill.getLevel();
        }
        if (null != mechanicSkill && mechanicSkill.getLevel() > lvl) {
            lvl = mechanicSkill.getLevel();
        }
        if (null != baSkill && baSkill.getLevel() > lvl) {
            lvl = baSkill.getLevel();
        }
        if (null != aeroSkill && aeroSkill.getLevel() > lvl) {
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
        if (null != mechSkill) {
            if (!first) {
                toReturn += "; ";
            }
            toReturn += SkillType.getExperienceLevelName(mechSkill.getExperienceLevel()) + " " + SkillType.S_TECH_MECH;
            first = false;
        }
        if (null != mechanicSkill) {
            if (!first) {
                toReturn += "; ";
            }
            toReturn += SkillType.getExperienceLevelName(mechanicSkill.getExperienceLevel()) + " " + SkillType.S_TECH_MECHANIC;
            first = false;
        }
        if (null != baSkill) {
            if (!first) {
                toReturn += "; ";
            }
            toReturn += SkillType.getExperienceLevelName(baSkill.getExperienceLevel()) + " " + SkillType.S_TECH_BA;
            first = false;
        }
        if (null != aeroSkill) {
            if (!first) {
                toReturn += "; ";
            }
            toReturn += SkillType.getExperienceLevelName(aeroSkill.getExperienceLevel()) + " " + SkillType.S_TECH_AERO;
            first = false;
        }
        toReturn += "<br/>";
        toReturn += getMinutesLeft() + " minutes left";
        if (overtimeAllowed) {
            toReturn += " + (" + getOvertimeLeft() + " overtime)";
        }
        toReturn += "</font></html>";
        return toReturn;
    }

    public boolean isRightTechTypeFor(IPartWork part) {
        Unit unit = part.getUnit();
        if (null == unit) {
            if ((hasSkill(SkillType.S_TECH_MECH) && part.isRightTechType(SkillType.S_TECH_MECH))
                || (hasSkill(SkillType.S_TECH_AERO) && part.isRightTechType(SkillType.S_TECH_AERO))
                || (hasSkill(SkillType.S_TECH_MECHANIC) && part.isRightTechType(SkillType.S_TECH_MECHANIC))
                || (hasSkill(SkillType.S_TECH_BA) && part.isRightTechType(SkillType.S_TECH_BA))
                || (hasSkill(SkillType.S_TECH_VESSEL) && part.isRightTechType(SkillType.S_TECH_VESSEL))) {
                return true;
            }
            return false;
        }
        if (unit.getEntity() instanceof Mech || unit.getEntity() instanceof Protomech) {
            return hasSkill(SkillType.S_TECH_MECH);
        }
        if (unit.getEntity() instanceof BattleArmor) {
            return hasSkill(SkillType.S_TECH_BA);
        }
        if (unit.getEntity() instanceof Tank) {
            return hasSkill(SkillType.S_TECH_MECHANIC);
        }
        if (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship) {
            return hasSkill(SkillType.S_TECH_VESSEL);
        }
        if (unit.getEntity() instanceof Aero) {
            return hasSkill(SkillType.S_TECH_AERO);
        }
        return false;
    }

    public UUID getDoctorId() {
        return doctorId;
    }

    public boolean isDoctor() {
        return hasSkill(SkillType.S_DOCTOR) && (primaryRole == T_DOCTOR || secondaryRole == T_DOCTOR);
    }

    public int getToughness() {
        return toughness;
    }

    public void setToughness(int t) {
        toughness = t;
    }

    public void resetSkillTypes() {
        for (String skillName : skills.keySet()) {
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

    public int getNTasks() {
        return nTasks;
    }

    public void setNTasks(int n) {
        nTasks = n;
    }

    public ArrayList<LogEntry> getPersonnelLog() {
        Collections.sort(personnelLog, new Comparator<LogEntry>() {
            public int compare(final LogEntry u1, final LogEntry u2) {
                return u1.getDate().compareTo(u2.getDate());
            }
        });
        return personnelLog;
    }

    public void addLogEntry(Date d, String desc) {
        personnelLog.add(new LogEntry(d, desc));
    }

    public void addLogEntry(LogEntry entry) {
        personnelLog.add(entry);
    }

    /**
     * All methods below are for the Advanced Medical option **
     */

    public ArrayList<Injury> getInjuries() {
        return injuries;
    }

    public void clearInjuries() {
        injuries.clear();
    }

    public void removeInjury(Injury i) {
        injuries.remove(i);
    }

    public String getInjuriesDesc() {
        String toReturn = "<html><font size='2'><b>" + getFullTitle() + "</b><br/>";
        toReturn += "&nbsp;&nbsp;&nbsp;Injuries:";
        String sep = "<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
        for (Injury injury : injuries) {
            toReturn += sep + injury.getFluff();
            if (sep.contains("<br/>")) {
                sep = ", ";
            } else {
                sep = ",<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
            }
        }
        toReturn += "</font></html>";
        return toReturn;
    }

    public void diagnose(int hits) {
        if (getStatus() == S_KIA || getStatus() == S_MIA) {
            return;
        }
        resolveSpecialDamage(hits);
        Entity en = null;
        Unit u = campaign.getUnit(getUnitId());
        boolean mwasf = false;
        if (u != null) {
            en = u.getEntity();
        }
        if (en != null && (en instanceof Mech || en instanceof Aero)) {
            mwasf = true;
        }
        int critMod = mwasf ? 0 : 2;
        for (int i = 0; i < hits; i++) {
            int location = -1;
            // If MW or ASF
            if (mwasf) {
                while (location == -1) {
                    location = getBodyHitLocationMWASF(Compute.randomInt(200));
                }
            } else { // If Anything else...
                while (location == -1) {
                    location = getBodyHitLocation(Compute.randomInt(200));
                }
            }

            // apply hit here
            applyBodyHit(location);
            int roll = Compute.d6(2);
            if ((roll + hits + critMod) > 12) {
                // apply another hit to the same location if critical
                applyBodyHit(location);
            }
        }
        ArrayList<Injury> new_injuries = new ArrayList<Injury>();
        for (int i = 0; i < BODY_NUM; i++) {
            if (!(i == BODY_LEFT_ARM || i == BODY_RIGHT_ARM || i == BODY_LEFT_LEG || i == BODY_RIGHT_LEG)) {
                resolvePostDamage(i);
            }
            new_injuries.addAll(applyDamage(i));
            hit_location[i] = 0;
        }
        String ni_report = "";
        for (Injury ni : new_injuries) {
            ni_report += "\n\t\t" + ni.getFluff();
        }
        if (new_injuries.size() > 0) {
            addLogEntry(campaign.getDate(), "Returned from combat with the following new injuries:" + ni_report);
        }
        injuries.addAll(new_injuries);
        //removeDuplicateInjuries();
        setHits(0);
    }

    public void resolvePostDamage(int location) {
        for (Injury injury : injuries) {
            if (location == injury.getLocation()
                && (injury.getType() == Injury.INJ_INTERNAL_BLEEDING
                    || location == BODY_HEAD
                    || injury.getType() == Injury.INJ_BROKEN_BACK)
                && hit_location[location] > 5) {
                hit_location[location] = 0;
                changeStatus(S_KIA);
            }
        }
    }

    public void resolveSpecialDamage(int hits) {
        ArrayList<Injury> new_injuries = new ArrayList<Injury>();
        for (Injury injury : injuries) {
            if (injury.getType() == Injury.INJ_CTE || injury.getType() == Injury.INJ_CONCUSSION || injury.getType() == Injury.INJ_CEREBRAL_CONTUSION
                || injury.getType() == Injury.INJ_INTERNAL_BLEEDING || injury.getType() == Injury.INJ_BROKEN_LIMB
                || injury.getType() == Injury.INJ_BROKEN_COLLAR_BONE || injury.getType() == Injury.INJ_PUNCTURED_LUNG) {
                injury.setTime(Injury.generateHealingTime(campaign, injury.getType(), injury.getHits(), this));
            }

            if (injury.getType() == Injury.INJ_BROKEN_BACK && Compute.randomInt(100) < 20) {
                changeStatus(S_RETIRED);
                injury.setPermanent(true);
            }

            if (injury.getType() == Injury.INJ_BROKEN_RIB) {
                int rib = Compute.randomInt(100);
                if (rib < 1) {
                    changeStatus(S_KIA);
                } else if (rib < 10) {
                    new_injuries.add(new Injury(Injury.generateHealingTime(campaign, Injury.INJ_PUNCTURED_LUNG, hits, this), Injury.generateInjuryFluffText(Injury.INJ_PUNCTURED_LUNG, injury.getLocation(), PRONOUN_HISHER), injury.getLocation(), Injury.INJ_PUNCTURED_LUNG, hit_location[injury.getLocation()], false));
                }
            }

            if (injury.getType() == Injury.INJ_BRUISED_KIDNEY) {
                if (Compute.randomInt(100) < 10) {
                    hit_location[injury.getLocation()] = 3;
                }
            }

            // Now reset all messages and healing times.
            if (((Compute.d6() + hits) > 5) && (injury.getType() == Injury.INJ_CTE || injury.getType() == Injury.INJ_CONCUSSION
                                                || injury.getType() == Injury.INJ_CEREBRAL_CONTUSION || injury.getType() == Injury.INJ_INTERNAL_BLEEDING)) {
                injury.setHits(injury.getHits() + 1);
                injury.setFluff(Injury.generateInjuryFluffText(injury.getType(), injury.getLocation(), PRONOUN_HISHER));
            }
        }

        injuries.addAll(new_injuries);
    }

    public void changeStatus(int status) {
        if (status == getStatus()) {
            return;
        }
        Unit u = campaign.getUnit(getUnitId());
        if (status == Person.S_KIA) {
            addLogEntry(campaign.getDate(), "Died from " + getGenderPronoun(PRONOUN_HISHER) + " wounds");
            //set the deathday
            setDeathday((GregorianCalendar) campaign.calendar.clone());
        }
        if (status == Person.S_RETIRED) {
            addLogEntry(campaign.getDate(), "Retired from active duty due to " + getGenderPronoun(PRONOUN_HISHER) + " wounds");
        }
        setStatus(status);
        if (status != Person.S_ACTIVE) {
            setDoctorId(null, campaign.getCampaignOptions().getNaturalHealingWaitingPeriod());
            if (null != u) {
                u.remove(this, true);
            }
        }
    }

    public int getAbilityTimeModifier() {
        int modifier = 100;
        if (campaign.getCampaignOptions().useToughness()) {
            if (getToughness() == 1) {
                modifier -= 10;
            }
            if (getToughness() > 1) {
                modifier -= 15;
            }
        } // TODO: Fully implement this for advanced healing
        if (getOptions().booleanOption("pain_resistance")) {
            modifier -= 15;
        } else if (getOptions().booleanOption("iron_man")) {
            modifier -= 10;
        }

        return modifier;
    }

    public void applyBodyHit(int location) {
        hit_location[location]++;
    }

    public boolean hasInjury(int loc, int type) {
        if (getInjuryByLocationAndType(loc, type) != null) {
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<Injury> applyDamage(int location) {
        ArrayList<Injury> new_injuries = new ArrayList<Injury>();
        int roll = Compute.randomInt(2);
        int type = Injury.getInjuryTypeByLocation(location, roll, hit_location[location]);
        if (hasInjury(location, type)) {
            Injury injury = getInjuryByLocationAndType(location, type);
            injury.setTime(Injury.generateHealingTime(campaign, injury.getType(), injury.getHits(), this));
            return new_injuries;
        }
        switch (location) {
            case BODY_LEFT_ARM:
            case BODY_RIGHT_ARM:
            case BODY_LEFT_LEG:
            case BODY_RIGHT_LEG:
                if (hit_location[location] == 1) {
                    if (roll == 2) {
                        new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_CUT, this), Injury.generateInjuryFluffText(Injury.INJ_CUT, location, PRONOUN_HISHER), location, Injury.INJ_CUT, hit_location[location], false));
                    } else {
                        new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_BRUISE, this), Injury.generateInjuryFluffText(Injury.INJ_BRUISE, location, PRONOUN_HISHER), location, Injury.INJ_BRUISE, hit_location[location], false));
                    }
                } else if (hit_location[location] == 2) {
                    new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_SPRAIN, this), Injury.generateInjuryFluffText(Injury.INJ_SPRAIN, location, PRONOUN_HISHER), location, Injury.INJ_SPRAIN, hit_location[location], false));
                } else if (hit_location[location] == 3) {
                    new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_BROKEN_LIMB, this), Injury.generateInjuryFluffText(Injury.INJ_BROKEN_LIMB, location, PRONOUN_HISHER), location, Injury.INJ_BROKEN_LIMB, hit_location[location], false));
                } else if (hit_location[location] > 3) {
                    new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_LOST_LIMB, this), Injury.generateInjuryFluffText(Injury.INJ_LOST_LIMB, location, PRONOUN_HISHER), location, Injury.INJ_LOST_LIMB, hit_location[location], true));
                }
                break;
            case BODY_HEAD:
                if (hit_location[location] == 1) {
                    new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_LACERATION, this), Injury.generateInjuryFluffText(Injury.INJ_LACERATION, location, PRONOUN_HISHER), location, Injury.INJ_LACERATION, hit_location[location], false));
                } else if (hit_location[location] == 2 || hit_location[location] == 3) {
                    new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_CONCUSSION, this), Injury.generateInjuryFluffText(Injury.INJ_CONCUSSION, location, PRONOUN_HISHER), location, Injury.INJ_CONCUSSION, hit_location[location], false));
                } else if (hit_location[location] == 4) {
                    new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_CEREBRAL_CONTUSION, this), Injury.generateInjuryFluffText(Injury.INJ_CEREBRAL_CONTUSION, location, PRONOUN_HISHER), location, Injury.INJ_CEREBRAL_CONTUSION, hit_location[location], false));
                } else if (hit_location[location] > 4) {
                    new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_CTE, this), Injury.generateInjuryFluffText(Injury.INJ_CTE, location, PRONOUN_HISHER), location, Injury.INJ_CTE, hit_location[location], true));
                }
                break;
            case BODY_CHEST:
                if (hit_location[location] == 1) {
                    if (roll == 2) {
                        new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_CUT, this), Injury.generateInjuryFluffText(Injury.INJ_CUT, location, PRONOUN_HISHER), location, Injury.INJ_CUT, hit_location[location], false));
                    } else {
                        new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_BRUISE, this), Injury.generateInjuryFluffText(Injury.INJ_BRUISE, location, PRONOUN_HISHER), location, Injury.INJ_BRUISE, hit_location[location], false));
                    }
                } else if (hit_location[location] == 2) {
                    new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_BROKEN_RIB, this), Injury.generateInjuryFluffText(Injury.INJ_BROKEN_RIB, location, PRONOUN_HISHER), location, Injury.INJ_BROKEN_RIB, hit_location[location], false));
                } else if (hit_location[location] == 3) {
                    new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_BROKEN_COLLAR_BONE, this), Injury.generateInjuryFluffText(Injury.INJ_BROKEN_COLLAR_BONE, location, PRONOUN_HISHER), location, Injury.INJ_BROKEN_COLLAR_BONE, hit_location[location], false));
                } else if (hit_location[location] == 4) {
                    new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_PUNCTURED_LUNG, this), Injury.generateInjuryFluffText(Injury.INJ_PUNCTURED_LUNG, location, PRONOUN_HISHER), location, Injury.INJ_PUNCTURED_LUNG, hit_location[location], false));
                } else if (hit_location[location] > 4) {
                    if (Compute.randomInt(100) < 15) {
                        changeStatus(Person.S_RETIRED);
                        new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_BROKEN_BACK, this), Injury.generateInjuryFluffText(Injury.INJ_BROKEN_BACK, location, PRONOUN_HISHER), location, Injury.INJ_BROKEN_BACK, hit_location[location], true));
                    } else {
                        new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_BROKEN_BACK, this), Injury.generateInjuryFluffText(Injury.INJ_BROKEN_BACK, location, PRONOUN_HISHER), location, Injury.INJ_BROKEN_BACK, hit_location[location], false));
                    }
                }
                break;
            case BODY_ABDOMEN:
                if (hit_location[location] == 1) {
                    if (roll == 2) {
                        new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_CUT, this), Injury.generateInjuryFluffText(Injury.INJ_CUT, location, PRONOUN_HISHER), location, Injury.INJ_CUT, hit_location[location], false));
                    } else {
                        new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_BRUISE, this), Injury.generateInjuryFluffText(Injury.INJ_BRUISE, location, PRONOUN_HISHER), location, Injury.INJ_BRUISE, hit_location[location], false));
                    }
                } else if (hit_location[location] == 2) {
                    new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_BRUISED_KIDNEY, this), Injury.generateInjuryFluffText(Injury.INJ_BRUISED_KIDNEY, location, PRONOUN_HISHER), location, Injury.INJ_BRUISED_KIDNEY, hit_location[location], false));
                } else if (hit_location[location] > 2) {
                    new_injuries.add(new Injury(Injury.generateHealingTime(campaign, hit_location[location], Injury.INJ_INTERNAL_BLEEDING, this), Injury.generateInjuryFluffText(Injury.INJ_INTERNAL_BLEEDING, location, PRONOUN_HISHER), location, Injury.INJ_INTERNAL_BLEEDING, hit_location[location], false));
                }
                break;
            default:
                System.err.println("ERROR: Default CASE reached in (Advanced Medical Section) Person.applyDamage()");
        }
        if (location == BODY_HEAD) {
            Injury inj = getInjuryByLocation(BODY_HEAD);
            if (inj != null && new_injuries != null && new_injuries.size() > 0) {
                if (inj.getType() > new_injuries.get(0).getType()) {
                    inj.setTime(Injury.generateHealingTime(campaign, inj.getHits(), inj.getType(), this));
                    new_injuries.clear();
                } else if (inj.getType() < new_injuries.get(0).getType()) {
                    injuries.remove(inj);
                }
            }
        }
        return new_injuries;
    }

    public int getBodyHitLocationMWASF(int roll) {
        int result = -1;
        if (roll < 25) {
            result = BODY_HEAD;
        } else if (roll < 41) {
            result = BODY_CHEST;
        } else if (roll < 48) {
            result = BODY_ABDOMEN;
        } else if (roll < 61 && !locationIsMissing(BODY_LEFT_ARM)) {
            result = BODY_LEFT_ARM;
        } else if (roll < 74 && !locationIsMissing(BODY_RIGHT_ARM)) {
            result = BODY_RIGHT_ARM;
        } else if (roll < 100 && !locationIsMissing(BODY_LEFT_LEG)) {
            result = BODY_LEFT_LEG;
        } else if (roll < 126 && !locationIsMissing(BODY_RIGHT_LEG)) {
            result = BODY_RIGHT_LEG;
        } else if (roll < 139 && !locationIsMissing(BODY_RIGHT_ARM)) {
            result = BODY_RIGHT_ARM;
        } else if (roll < 152 && !locationIsMissing(BODY_LEFT_ARM)) {
            result = BODY_LEFT_ARM;
        } else if (roll < 159) {
            result = BODY_ABDOMEN;
        } else if (roll < 176) {
            result = BODY_CHEST;
        } else {
            result = BODY_HEAD;
        }
        return result;
    }

    public int getBodyHitLocation(int roll) {
        int result = -1;
        if (roll < 10) {
            result = BODY_HEAD;
        } else if (roll < 30) {
            result = BODY_CHEST;
        } else if (roll < 40) {
            result = BODY_ABDOMEN;
        } else if (roll < 55 && !locationIsMissing(BODY_LEFT_ARM)) {
            result = BODY_LEFT_ARM;
        } else if (roll < 70 && !locationIsMissing(BODY_RIGHT_ARM)) {
            result = BODY_RIGHT_ARM;
        } else if (roll < 100 && !locationIsMissing(BODY_LEFT_LEG)) {
            result = BODY_LEFT_LEG;
        } else if (roll < 130 && !locationIsMissing(BODY_RIGHT_LEG)) {
            result = BODY_RIGHT_LEG;
        } else if (roll < 145 && !locationIsMissing(BODY_RIGHT_ARM)) {
            result = BODY_RIGHT_ARM;
        } else if (roll < 160 && !locationIsMissing(BODY_LEFT_ARM)) {
            result = BODY_LEFT_ARM;
        } else if (roll < 170) {
            result = BODY_ABDOMEN;
        } else if (roll < 190) {
            result = BODY_CHEST;
        } else {
            result = BODY_HEAD;
        }
        return result;
    }

    public void AMheal() {
        ArrayList<Injury> removals = new ArrayList<Injury>();
        for (Injury i : injuries) {
            if (i.getTime() > 0) {
                i.setTime(i.getTime() - 1);
            }
            if (i.getTime() < 1 && !i.getPermanent()) {
                if ((i.getType() == Injury.INJ_BROKEN_LIMB || i.getType() == Injury.INJ_SPRAIN || i.getType() == Injury.INJ_CONCUSSION
                     || i.getType() == Injury.INJ_BROKEN_COLLAR_BONE) && Compute.d6() == 1) {
                    i.setPermanent(true);
                } else {
                    removals.add(i);
                }
            }
        }
        injuries.removeAll(removals);
        if (!needsAMFixing()) {
            doctorId = null;
        }
    }

    public boolean needsAMFixing() {
        boolean retVal = false;
        if (injuries.size() > 0) {
            for (Injury i : injuries) {
                if (i.getTime() > 0) {
                    retVal = true;
                    break;
                }
            }
        }
        return retVal;
    }

    public int getPilotingInjuryMod() {
        int mod = 0;
        for (Injury injury : injuries) {
            if (injury.getType() == Injury.INJ_SPRAIN && (injury.getLocation() == BODY_LEFT_LEG || injury.getLocation() == BODY_RIGHT_LEG)) {
                mod += 1;
            }
            if (injury.getType() == Injury.INJ_BROKEN_LIMB && (injury.getLocation() == BODY_LEFT_LEG || injury.getLocation() == BODY_RIGHT_LEG)) {
                if (injury.getPermanent()) {
                    mod += 1;
                } else {
                    mod += 2;
                }
            }
            if (injury.getType() == Injury.INJ_LOST_LIMB && (injury.getLocation() == BODY_LEFT_LEG || injury.getLocation() == BODY_RIGHT_LEG)) {
                mod += 3;
            }
            if (injury.getType() == Injury.INJ_CONCUSSION) {
                mod += 1;
            }
            if (injury.getType() == Injury.INJ_CEREBRAL_CONTUSION) {
                mod += 2;
            }
            if (injury.getType() == Injury.INJ_CTE || injury.getType() == Injury.INJ_BROKEN_BACK) {
                mod += 3;
            }
        }
        return mod;
    }

    public int getGunneryInjuryMod() {
        int mod = 0;
        for (Injury injury : injuries) {
            if ((injury.getType() == Injury.INJ_SPRAIN && (injury.getLocation() == BODY_LEFT_ARM || injury.getLocation() == BODY_RIGHT_ARM)) || injury.getType() == Injury.INJ_BROKEN_COLLAR_BONE) {
                mod += 1;
            }
            if (injury.getType() == Injury.INJ_BROKEN_LIMB && (injury.getLocation() == BODY_LEFT_ARM || injury.getLocation() == BODY_RIGHT_ARM)) {
                if (injury.getPermanent()) {
                    mod += 1;
                } else {
                    mod += 2;
                }
            }
            if (injury.getType() == Injury.INJ_BROKEN_BACK || (injury.getType() == Injury.INJ_LOST_LIMB && (injury.getLocation() == BODY_LEFT_ARM || injury.getLocation() == BODY_RIGHT_ARM))) {
                mod += 3;
            }
        }
        return mod;
    }

    /*
     * Returns an HTML encoded string of effects
     */
    public String getEffects() {
        String nl = "<br>";
        String buffer = "";
        if (getPilotingInjuryMod() > 0) {
            buffer = buffer + "  Piloting +" + getPilotingInjuryMod() + nl;
        }
        if (getGunneryInjuryMod() > 0) {
            buffer = buffer + "  Gunnery +" + getGunneryInjuryMod() + nl;
        }
        return "<html>" + buffer + "</html>";
    }

    public String getInjuriesText() {
        String nl = System.getProperty("line.separator");
        String buffer = "";
        for (Injury injury : injuries) {
            buffer = buffer + injury.getFluff() + nl;
        }
        return buffer + getEffects();
    }

    public boolean hasInjuries(boolean permCheck) {
        boolean tf = false;
        if (injuries.size() > 0) {
            if (permCheck) {
                for (Injury injury : injuries) {
                    if (!injury.getPermanent()) {
                        tf = true;
                        break;
                    }
                }
            } else {
                tf = true;
            }
        }
        return tf;
    }

    public ArrayList<Injury> getInjuriesByLocation(int loc) {
        ArrayList<Injury> i = new ArrayList<Injury>();
        for (Injury injury : getInjuries()) {
            if (injury.getLocation() == loc) {
                i.add(injury);
            }
        }
        return i;
    }


    // Returns only the first injury in a location
    public Injury getInjuryByLocation(int loc) {
        Injury i = null;
        for (Injury injury : getInjuries()) {
            if (injury.getLocation() == loc) {
                i = injury;
                break;
            }
        }
        return i;
    }

    public Injury getInjuryByType(int t) {
        Injury i = null;
        for (Injury injury : getInjuries()) {
            if (injury.getType() == t) {
                i = injury;
                break;
            }
        }
        return i;
    }

    public Injury getInjuryByLocationAndType(int loc, int t) {
        Injury i = null;
        for (Injury injury : injuries) {
            if (injury.getType() == t && injury.getLocation() == loc) {
                i = injury;
            }
        }
        return i;
    }

    public boolean locationIsMissing(int loc) {
        boolean retVal = false;
        for (Injury i : getInjuriesByLocation(loc)) {
            if (i.getType() == Injury.INJ_LOST_LIMB) {
                retVal = true;
                break;
            }
        }
        return retVal;
    }

    public void addInjury(Injury i) {
        injuries.add(i);
    }
}
