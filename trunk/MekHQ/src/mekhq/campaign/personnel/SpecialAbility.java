/*
 * SpecialAbility.java
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

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.Compute;
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.options.IOption;
import megamek.common.options.PilotOptions;
import megamek.common.weapons.BayWeapon;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.Version;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This object will serve as a wrapper for a specific pilot special ability. In the actual
 * person object we will use PilotOptions (and maybe at some point NonPilotOptions), so these
 * objects will not get written to actual personnel. Instead, we will we will keep track of a full static
 * hash of SPAs that will contain important information on XP costs and pre-reqs that can be
 * looked up to see if a person is eligible for a particular option. All of this
 * will be customizable via an external XML file that can be user selected in the campaign
 * options (and possibly user editable).
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class SpecialAbility implements MekHqXmlSerializable {

    private static Hashtable<String, SpecialAbility> specialAbilities;
    private static Hashtable<String, SpecialAbility> defaultSpecialAbilities;

    private String displayName;
    private String lookupName;
    private String desc;

    private int xpCost;

    //this determines how much weight to give this SPA when creating new personnel
    private int weight;

    //prerequisite skills and options
    private Vector<String> prereqAbilities;
    private Vector<SkillPrereq> prereqSkills;

    //these are abilities that will disqualify the person from getting the current ability
    private Vector<String> invalidAbilities;

    //these are abilities that should be removed if the person gets this ability
    //(typically this is a lower value ability on the same chain (e.g. Cluster Hitter removed when you get Cluster Master)
    private Vector<String> removeAbilities;


    public SpecialAbility() {
        this("unknown");
    }

    public SpecialAbility(String name) {
        lookupName = name;
        displayName = "";
        desc = "";
        prereqAbilities = new Vector<String>();
        invalidAbilities = new Vector<String>();
        removeAbilities = new Vector<String>();
        prereqSkills = new Vector<SkillPrereq>();
        xpCost = 1;
        weight = 1;
    }

    public SpecialAbility(String name, String display, String description) {
        lookupName = name;
        displayName = display;
        desc = description;
        prereqAbilities = new Vector<String>();
        invalidAbilities = new Vector<String>();
        removeAbilities = new Vector<String>();
        prereqSkills = new Vector<SkillPrereq>();
        xpCost = 1;
        weight = 1;
    }

    @SuppressWarnings("unchecked") // FIXME: Broken Java with it's Object clones
	public SpecialAbility clone() {
    	SpecialAbility clone = new SpecialAbility(lookupName);
    	clone.displayName = this.displayName;
    	clone.desc = this.desc;
    	clone.xpCost = this.xpCost;
    	clone.weight = this.weight;
    	clone.prereqAbilities = (Vector<String>)this.prereqAbilities.clone();
    	clone.invalidAbilities = (Vector<String>)this.invalidAbilities.clone();
    	clone.removeAbilities = (Vector<String>)this.removeAbilities.clone();
    	clone.prereqSkills = (Vector<SkillPrereq>)this.prereqSkills.clone();
    	return clone;
    }

    public boolean isEligible(Person p) {
        for(SkillPrereq sp : prereqSkills) {
            if(!sp.qualifies(p)) {
                return false;
            }
        }
        for(String ability : prereqAbilities) {
            //TODO: will this work for choice options like weapon specialist?
            if(!p.getOptions().booleanOption(ability)) {
                return false;
            }
        }
        for(String ability : invalidAbilities) {
            //TODO: will this work for choice options like weapon specialist?
            if(p.getOptions().booleanOption(ability)) {
                return false;
            }
        }
        return true;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return desc;
    }

    public String getName() {
        return lookupName;
    }

    public int getCost() {
        return xpCost;
    }

    public void setCost(int cost) {
    	xpCost = cost;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
    	this.weight = weight;
    }

    public Vector<SkillPrereq> getPrereqSkills() {
        return prereqSkills;
    }

    public void setPrereqSkills(Vector<SkillPrereq> prereq) {
    	prereqSkills = prereq;
    }

    public Vector<String> getPrereqAbilities() {
        return prereqAbilities;
    }

    public void setPrereqAbilities(Vector<String> prereq) {
    	prereqAbilities = prereq;
    }

    public Vector<String> getInvalidAbilities() {
        return invalidAbilities;
    }

    public void setInvalidAbilities(Vector<String> invalid) {
    	invalidAbilities = invalid;
    }

    public Vector<String> getRemovedAbilities() {
        return removeAbilities;
    }

    public void setRemovedAbilities(Vector<String> remove) {
    	removeAbilities = remove;
    }

    public void clearPrereqSkills() {
        prereqSkills = new Vector<SkillPrereq>();
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<ability>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<displayName>"
                +MekHqXmlUtil.escape(displayName)
                +"</displayName>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<lookupName>"
                +lookupName
                +"</lookupName>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<desc>"
                +MekHqXmlUtil.escape(desc)
                +"</desc>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<xpCost>"
                +xpCost
                +"</xpCost>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<weight>"
                +weight
                +"</weight>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<prereqAbilities>"
                +Utilities.combineString(prereqAbilities, "::")
                +"</prereqAbilities>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<invalidAbilities>"
                +Utilities.combineString(invalidAbilities, "::")
                +"</invalidAbilities>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<removeAbilities>"
                +Utilities.combineString(removeAbilities, "::")
                +"</removeAbilities>");
        for(SkillPrereq skillpre : prereqSkills) {
            skillpre.writeToXml(pw1, indent+1);
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</ability>");

    }


    @SuppressWarnings("unchecked")
    public static void generateInstanceFromXML(Node wn, PilotOptions options, Version v) {
        SpecialAbility retVal = null;

        try {
            retVal = new SpecialAbility();
            NodeList nl = wn.getChildNodes();

            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("displayName")) {
                    retVal.displayName = wn2.getTextContent();
                }
                else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                    retVal.desc = wn2.getTextContent();
                }
                else if (wn2.getNodeName().equalsIgnoreCase("lookupName")) {
                    retVal.lookupName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("xpCost")) {
                    retVal.xpCost = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("weight")) {
                    retVal.weight = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("prereqAbilities")) {
                    retVal.prereqAbilities = Utilities.splitString(wn2.getTextContent(), "::");
                } else if (wn2.getNodeName().equalsIgnoreCase("invalidAbilities")) {
                    retVal.invalidAbilities = Utilities.splitString(wn2.getTextContent(), "::");
                } else if (wn2.getNodeName().equalsIgnoreCase("removeAbilities")) {
                    retVal.removeAbilities = Utilities.splitString(wn2.getTextContent(), "::");
                } else if (wn2.getNodeName().equalsIgnoreCase("skillPrereq")) {
                    SkillPrereq skill = SkillPrereq.generateInstanceFromXML(wn2);
                    if(!skill.isEmpty()) {
                        retVal.prereqSkills.add(skill);
                    }
                }
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.logError(ex);
        }

        if(retVal.displayName.isEmpty()) {
            IOption option = options.getOption(retVal.lookupName);
            if(null != option) {
                retVal.displayName = option.getDisplayableName();
            }
        }

        if(retVal.desc.isEmpty()) {
            IOption option = options.getOption(retVal.lookupName);
            if(null != option) {
                retVal.desc = option.getDescription();
            }
        }
        if (v != null) {
            if (defaultSpecialAbilities != null && Version.versionCompare(v, "0.3.6-r1965")) {
                if (defaultSpecialAbilities.get(retVal.lookupName) != null
                        && defaultSpecialAbilities.get(retVal.lookupName).getPrereqSkills() != null) {
                    retVal.prereqSkills = (Vector<SkillPrereq>) defaultSpecialAbilities.get(retVal.lookupName).getPrereqSkills().clone();
                }
            }
        }
        specialAbilities.put(retVal.lookupName, retVal);
    }
    
    @SuppressWarnings("unchecked")
    public static void generateSeparateInstanceFromXML(Node wn, Hashtable<String, SpecialAbility> spHash, PilotOptions options) {
        SpecialAbility retVal = null;

        try {
            retVal = new SpecialAbility();
            NodeList nl = wn.getChildNodes();

            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("displayName")) {
                    retVal.displayName = wn2.getTextContent();
                }
                else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                    retVal.desc = wn2.getTextContent();
                }
                else if (wn2.getNodeName().equalsIgnoreCase("lookupName")) {
                    retVal.lookupName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("xpCost")) {
                    retVal.xpCost = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("weight")) {
                    retVal.weight = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("prereqAbilities")) {
                    retVal.prereqAbilities = Utilities.splitString(wn2.getTextContent(), "::");
                } else if (wn2.getNodeName().equalsIgnoreCase("invalidAbilities")) {
                    retVal.invalidAbilities = Utilities.splitString(wn2.getTextContent(), "::");
                } else if (wn2.getNodeName().equalsIgnoreCase("removeAbilities")) {
                    retVal.removeAbilities = Utilities.splitString(wn2.getTextContent(), "::");
                } else if (wn2.getNodeName().equalsIgnoreCase("skillPrereq")) {
                    SkillPrereq skill = SkillPrereq.generateInstanceFromXML(wn2);
                    if(!skill.isEmpty()) {
                        retVal.prereqSkills.add(skill);
                    }
                }
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.logError(ex);
        }

        if(retVal.displayName.isEmpty()) {
            IOption option = options.getOption(retVal.lookupName);
            if(null != option) {
                retVal.displayName = option.getDisplayableName();
            }
        }

        if(retVal.desc.isEmpty()) {
            IOption option = options.getOption(retVal.lookupName);
            if(null != option) {
                retVal.desc = option.getDescription();
            }
        }
        spHash.put(retVal.lookupName, retVal);
    }

    public static void initializeSPA() {
        specialAbilities = new Hashtable<String, SpecialAbility>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document xmlDoc = null;


        try {
            FileInputStream fis = new FileInputStream("data/universe/defaultspa.xml");
            // Using factory get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            MekHQ.logError(ex);
        }

        Element spaEle = xmlDoc.getDocumentElement();
        NodeList nl = spaEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML.  At least this cleans it up.
        spaEle.normalize();

        PilotOptions options = new PilotOptions();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);

            if (wn.getParentNode() != spaEle)
                continue;

            int xc = wn.getNodeType();

            if (xc == Node.ELEMENT_NODE) {
                // This is what we really care about.
                // All the meat of our document is in this node type, at this
                // level.
                // Okay, so what element is it?
                String xn = wn.getNodeName();

                if (xn.equalsIgnoreCase("ability")) {
                    SpecialAbility.generateInstanceFromXML(wn, options, null);
                }
            }
        }

    }

    public static SpecialAbility getAbility(String name) {
        return specialAbilities.get(name);
    }

    public static Hashtable<String, SpecialAbility> getAllSpecialAbilities() {
        return specialAbilities;
    }

    public static void replaceSpecialAbilities(Hashtable<String, SpecialAbility> spas) {
    	specialAbilities = spas;
    }

    public static String chooseWeaponSpecialization(int type, boolean isClan, int techLvl, int year) {
        ArrayList<String> candidates = new ArrayList<String>();
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if(!(et instanceof WeaponType)) {
                continue;
            }
            if(et instanceof InfantryWeapon
                    || et instanceof BayWeapon
                    || et instanceof InfantryAttack) {
                continue;
            }
            WeaponType wt = (WeaponType)et;
            if(wt.isCapital()
                    || wt.isSubCapital()
                    || wt.hasFlag(WeaponType.F_INFANTRY)
                    || wt.hasFlag(WeaponType.F_ONESHOT)
                    || wt.hasFlag(WeaponType.F_PROTOTYPE)) {
                continue;
            }
            if(!((wt.hasFlag(WeaponType.F_MECH_WEAPON) && type == Person.T_MECHWARRIOR)
                    || (wt.hasFlag(WeaponType.F_AERO_WEAPON) && type != Person.T_AERO_PILOT)
                    || (wt.hasFlag(WeaponType.F_TANK_WEAPON) && !(type == Person.T_VEE_GUNNER
                            || type == Person.T_NVEE_DRIVER
                            || type == Person.T_GVEE_DRIVER
                            || type == Person.T_VTOL_PILOT))
                    || (wt.hasFlag(WeaponType.F_BA_WEAPON) && type != Person.T_BA)
                    || (wt.hasFlag(WeaponType.F_PROTO_WEAPON) && type != Person.T_PROTO_PILOT))) {
                continue;
            }
            if(wt.getAtClass() == WeaponType.CLASS_NONE ||
                    wt.getAtClass() == WeaponType.CLASS_POINT_DEFENSE ||
                    wt.getAtClass() >= WeaponType.CLASS_CAPITAL_LASER) {
                continue;
            }
            if(TechConstants.isClan(wt.getTechLevel(year)) != isClan) {
                continue;
            }
            int lvl = wt.getTechLevel(year);
            if(lvl < 0) {
                continue;
            }
            if(techLvl < Utilities.getSimpleTechLevel(lvl)) {
                continue;
            }
            if(techLvl == TechConstants.T_IS_UNOFFICIAL) {
                continue;
            }
            int ntimes = 10;
            if(techLvl >= TechConstants.T_IS_ADVANCED) {
                ntimes = 1;
            }
            while(ntimes > 0) {
                candidates.add(et.getName());
                ntimes--;
            }
        }
        if(candidates.isEmpty()) {
            return "??";
        }
        return candidates.get(Compute.randomInt(candidates.size()));
    }

    public String getAllPrereqDesc() {
        String toReturn = "";
        for(String prereq : prereqAbilities) {
            toReturn += getDisplayName(prereq) + "<br>";
        }
        for(SkillPrereq skPr : prereqSkills) {
            toReturn += skPr.toString() + "<br>";
        }
        if(toReturn.isEmpty()) {
        	toReturn = "None";
        }
        return toReturn;
    }

    public String getPrereqAbilDesc() {
        String toReturn = "";
        for(String prereq : prereqAbilities) {
            toReturn += getDisplayName(prereq) + "<br>";
        }
        if(toReturn.isEmpty()) {
        	toReturn = "None";
        }
        return toReturn;
    }

    public String getInvalidDesc() {
        String toReturn = "";
        for(String invalid : invalidAbilities) {
            toReturn += getDisplayName(invalid) + "<br>";
        }
        if(toReturn.isEmpty()) {
        	toReturn = "None";
        }
        return toReturn;
    }

    public String getRemovedDesc() {
        String toReturn = "";
        for(String remove : removeAbilities) {
            toReturn += getDisplayName(remove) + "<br>";
        }
        if(toReturn.isEmpty()) {
        	toReturn = "None";
        }
        return toReturn;
    }

    public static String getDisplayName(String name) {
        PilotOptions options = new PilotOptions();
        IOption option = options.getOption(name);
        if(null != option) {
            return option.getDisplayableName();
        }
        return "??";
    }

    public static void clearSPA() {
        specialAbilities.clear();
    }

    @SuppressWarnings("unchecked")
    public static void trackDefaultSPA() {
        defaultSpecialAbilities = (Hashtable<String, SpecialAbility>)specialAbilities.clone();
    }

    public static void nullifyDefaultSPA() {
        defaultSpecialAbilities = null;
    }
    
    public static void setSpecialAbilities(Hashtable<String, SpecialAbility> spHash) {
    	specialAbilities = spHash;
    }

    //TODO: also put some static methods here that return the available options for a given SPA, so
    //we can take that out of the GUI code

}
