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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.options.IOption;
import megamek.common.options.PilotOptions;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.autocannons.ACWeapon;
import megamek.common.weapons.autocannons.LBXACWeapon;
import megamek.common.weapons.autocannons.UACWeapon;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.Version;

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

    // Keys for miscellaneous prerequisites (i.e. not skill or ability)
    private static final String PREREQ_MISC_CLANNER = "clanner";

    private static Hashtable<String, SpecialAbility> specialAbilities;
    private static Hashtable<String, SpecialAbility> defaultSpecialAbilities;
    private static Hashtable<String, SpecialAbility> edgeTriggers;
    private static Hashtable<String, SpecialAbility> implants;

    private String displayName;
    private String lookupName;
    private String desc;

    private int xpCost;

    //this determines how much weight to give this SPA when creating new personnel
    private int weight;

    //prerequisite skills and options
    private Vector<String> prereqAbilities;
    private Vector<SkillPrereq> prereqSkills;
    private Map<String,String> prereqMisc;

    //these are abilities that will disqualify the person from getting the current ability
    private Vector<String> invalidAbilities;

    //these are abilities that should be removed if the person gets this ability
    //(typically this is a lower value ability on the same chain (e.g. Cluster Hitter removed when you get Cluster Master)
    private Vector<String> removeAbilities;

    // For custom SPAs of type CHOICE the legal values need to be provided.
    private Vector<String> choiceValues;

    public SpecialAbility() {
        this("unknown");
    }

    public SpecialAbility(String name) {
        this(name, "", "");
    }

    public SpecialAbility(String name, String display, String description) {
        lookupName = name;
        displayName = display;
        desc = description;
        prereqAbilities = new Vector<>();
        invalidAbilities = new Vector<>();
        removeAbilities = new Vector<>();
        choiceValues = new Vector<>();
        prereqSkills = new Vector<>();
        prereqMisc = new HashMap<>();
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
        clone.choiceValues = (Vector<String>)this.choiceValues.clone();
        clone.prereqSkills = (Vector<SkillPrereq>)this.prereqSkills.clone();
        clone.prereqMisc = new HashMap<>(this.prereqMisc);
        return clone;
    }

    public boolean isEligible(Person p) {
        for (SkillPrereq sp : prereqSkills) {
            if (!sp.qualifies(p)) {
                return false;
            }
        }
        for (String ability : prereqAbilities) {
            //TODO: will this work for choice options like weapon specialist?
            if (!p.getOptions().booleanOption(ability)) {
                return false;
            }
        }
        for (String ability : invalidAbilities) {
            //TODO: will this work for choice options like weapon specialist?
            if (p.getOptions().booleanOption(ability)) {
                return false;
            }
        }
        return !prereqMisc.containsKey(PREREQ_MISC_CLANNER)
                || (p.isClanner() == Boolean.parseBoolean(prereqMisc.get(PREREQ_MISC_CLANNER)));
    }

    public boolean isEligible(boolean isClanner, Skills skills, PersonnelOptions options) {
        for (SkillPrereq sp : prereqSkills) {
            if (!sp.qualifies(skills)) {
                return false;
            }
        }
        for (String ability : prereqAbilities) {
            //TODO: will this work for choice options like weapon specialist?
            if (!options.booleanOption(ability)) {
                return false;
            }
        }
        for (String ability : invalidAbilities) {
            //TODO: will this work for choice options like weapon specialist?
            if (options.booleanOption(ability)) {
                return false;
            }
        }
        return !prereqMisc.containsKey(PREREQ_MISC_CLANNER)
                || (isClanner == Boolean.parseBoolean(prereqMisc.get(PREREQ_MISC_CLANNER)));
    }

    public boolean isEligible(int unitType) {
        for (SkillPrereq sp : prereqSkills) {
            if (!sp.qualifies(unitType)) {
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

    public Map<String, String> getPrereqMisc() {
        return prereqMisc;
    }

    public void setPrereqMisc(Map<String, String> prereq) {
        prereqMisc = new HashMap<>(prereq);
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

    public Vector<String> getChoiceValues() {
        return choiceValues;
    }

    public void setChoiceValues(Vector<String> values) {
        choiceValues = values;
    }

    public void clearPrereqSkills() {
        prereqSkills = new Vector<>();
    }

    public void clearPrereqMisc() {
        prereqMisc = new HashMap<>();
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
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<choiceValues>"
                +Utilities.combineString(choiceValues, "::")
                +"</choiceValues>");
        for (SkillPrereq skillpre : prereqSkills) {
            skillpre.writeToXml(pw1, indent+1);
        }
        for (String pre : prereqMisc.keySet()) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "miscPrereq", pre + ":" + prereqMisc.get(pre));
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</ability>");

    }


    @SuppressWarnings("unchecked")
    public static void generateInstanceFromXML(Node wn, PilotOptions options, Version v) {
        final String METHOD_NAME = "generateInstanceFromXML(Node,PilotOptions,Version)"; //$NON-NLS-1$

        try {
            SpecialAbility retVal = new SpecialAbility();
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
                } else if (wn2.getNodeName().equalsIgnoreCase("choiceValues")) {
                    retVal.choiceValues = Utilities.splitString(wn2.getTextContent(), "::");
                } else if (wn2.getNodeName().equalsIgnoreCase("skillPrereq")) {
                    SkillPrereq skill = SkillPrereq.generateInstanceFromXML(wn2);
                    if (!skill.isEmpty()) {
                        retVal.prereqSkills.add(skill);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("miscPrereq")) {
                    String[] fields = wn2.getTextContent().split(":");
                    retVal.prereqMisc.put(fields[0], fields[1]);
                }
            }

            if (retVal.displayName.isEmpty()) {
                IOption option = options.getOption(retVal.lookupName);
                if (null != option) {
                    retVal.displayName = option.getDisplayableName();
                }
            }

            if (retVal.desc.isEmpty()) {
                IOption option = options.getOption(retVal.lookupName);
                if (null != option) {
                    retVal.desc = option.getDescription();
                }
            }
            if (v != null) {
                if (defaultSpecialAbilities != null && v.isLowerThan("0.3.6-r1965")) {
                    if (defaultSpecialAbilities.get(retVal.lookupName) != null
                            && defaultSpecialAbilities.get(retVal.lookupName).getPrereqSkills() != null) {
                        retVal.prereqSkills = (Vector<SkillPrereq>) defaultSpecialAbilities.get(retVal.lookupName).getPrereqSkills().clone();
                    }
                }
            }

            if (wn.getNodeName().equalsIgnoreCase("edgetrigger")) {
                edgeTriggers.put(retVal.lookupName, retVal);
            } else if (wn.getNodeName().equalsIgnoreCase("implant")) {
                implants.put(retVal.lookupName,  retVal);
            } else {
                specialAbilities.put(retVal.lookupName, retVal);
            }
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.getLogger().error(SpecialAbility.class, METHOD_NAME, ex);
        }
    }

    public static void generateSeparateInstanceFromXML(Node wn, Hashtable<String, SpecialAbility> spHash, PilotOptions options) {
        final String METHOD_NAME = "generateSeparateInstanceFromXML(Node,Hashtable<String, SpecialAbility>,PilotOptions)"; //$NON-NLS-1$

        try {
            SpecialAbility retVal = new SpecialAbility();
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
                } else if (wn2.getNodeName().equalsIgnoreCase("choiceValues")) {
                    retVal.choiceValues = Utilities.splitString(wn2.getTextContent(), "::");
                } else if (wn2.getNodeName().equalsIgnoreCase("skillPrereq")) {
                    SkillPrereq skill = SkillPrereq.generateInstanceFromXML(wn2);
                    if (!skill.isEmpty()) {
                        retVal.prereqSkills.add(skill);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("miscPrereq")) {
                    String[] fields = wn2.getTextContent().split(":");
                    retVal.prereqMisc.put(fields[0], fields[1]);
                }
            }

            if (retVal.displayName.isEmpty()) {
                IOption option = options.getOption(retVal.lookupName);
                if (null != option) {
                    retVal.displayName = option.getDisplayableName();
                }
            }

            if (retVal.desc.isEmpty()) {
                IOption option = options.getOption(retVal.lookupName);
                if (null != option) {
                    retVal.desc = option.getDescription();
                }
            }
            spHash.put(retVal.lookupName, retVal);
        } catch (Exception ex) {
            // Errrr, apparently either the class name was invalid...
            // Or the listed name doesn't exist.
            // Doh!
            MekHQ.getLogger().error(SpecialAbility.class, METHOD_NAME, ex);
        }
    }

    public static void initializeSPA() {
        final String METHOD_NAME = "initializeSPA()"; //$NON-NLS-1$
        specialAbilities = new Hashtable<>();
        edgeTriggers = new Hashtable<>();
        implants = new Hashtable<>();

        Document xmlDoc;

        try (InputStream is = new FileInputStream("data/universe/defaultspa.xml")) {
            // Using factory get an instance of document builder
            DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(is);
        } catch (Exception ex) {
            MekHQ.getLogger().error(SpecialAbility.class, METHOD_NAME, ex);
            return;
        }

        Element spaEle = xmlDoc.getDocumentElement();
        NodeList nl = spaEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML.  At least this cleans it up.
        spaEle.normalize();

        PersonnelOptions options = new PersonnelOptions();

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

                if (xn.equalsIgnoreCase("ability")
                        || xn.equalsIgnoreCase("edgeTrigger")
                        || xn.equalsIgnoreCase("implant")) {
                    SpecialAbility.generateInstanceFromXML(wn, options, null);
                }
            }
        }
        SpecialAbility.trackDefaultSPA();
    }

    public static SpecialAbility getAbility(String name) {
        return specialAbilities.get(name);
    }

    public static Hashtable<String, SpecialAbility> getAllSpecialAbilities() {
        return specialAbilities;
    }

    public static SpecialAbility getDefaultAbility(String name) {
        if (null != defaultSpecialAbilities) {
            return defaultSpecialAbilities.get(name);
        }
        return null;
    }

    public static Hashtable<String, SpecialAbility> getAllDefaultSpecialAbilities() {
        return defaultSpecialAbilities;
    }

    public static SpecialAbility getEdgeTrigger(String name) {
        return edgeTriggers.get(name);
    }

    public static Hashtable<String, SpecialAbility> getAllEdgeTriggers() {
        return edgeTriggers;
    }

    public static SpecialAbility getImplant(String name) {
        return implants.get(name);
    }

    public static Hashtable<String, SpecialAbility> getAllImplants() {
        return implants;
    }

    public static void replaceSpecialAbilities(Hashtable<String, SpecialAbility> spas) {
        specialAbilities = spas;
    }

    public static SpecialAbility getOption(String name) {
        SpecialAbility retVal = specialAbilities.get(name);
        if (null != retVal) {
            return retVal;
        }
        if (null != defaultSpecialAbilities) {
            retVal = defaultSpecialAbilities.get(name);
            if (null != retVal) {
                return retVal;
            }
        }
        retVal = edgeTriggers.get(name);
        if (null != retVal) {
            return retVal;
        }
        return implants.get(name);
    }

    public static String chooseWeaponSpecialization(int type, boolean isClan, int techLvl, int year, boolean clusterOnly) {
        ArrayList<String> candidates = new ArrayList<>();
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();

            if (!isWeaponEligibleForSPA(et, type, clusterOnly)) {
                continue;
            }

            WeaponType wt = (WeaponType) et;

            if (TechConstants.isClan(wt.getTechLevel(year)) != isClan) {
                continue;
            }

            int lvl = wt.getTechLevel(year);
            if (lvl < 0) {
                continue;
            }

            if (techLvl < Utilities.getSimpleTechLevel(lvl)) {
                continue;
            }

            if (techLvl == TechConstants.T_IS_UNOFFICIAL) {
                continue;
            }

            int ntimes = 10;
            if (techLvl >= TechConstants.T_IS_ADVANCED) {
                ntimes = 1;
            }

            while (ntimes > 0) {
                candidates.add(et.getName());
                ntimes--;
            }
        }
        if (candidates.isEmpty()) {
            return "??";
        }
        return Utilities.getRandomItem(candidates);
    }

    /**
     * Worker function that determines if a piece of equipment is eligible
     * for being selected for an SPA.
     * @param et Equipment type to check
     * @param type Person role, e.g. Person.T_MECHWARRIOR. This check is ignored if Person.T_NONE is passed in.
     * @param clusterOnly All weapon types or just ones that do rolls on the cluster table
     */
    public static boolean isWeaponEligibleForSPA(EquipmentType et, int type, boolean clusterOnly) {
        if (!(et instanceof WeaponType)) {
            return false;
        }
        if (et instanceof InfantryWeapon
                || et instanceof BayWeapon
                || et instanceof InfantryAttack) {
            return false;
        }
        WeaponType wt = (WeaponType)et;
        if (wt.isCapital()
                || wt.isSubCapital()
                || wt.hasFlag(WeaponType.F_INFANTRY)
                || wt.hasFlag(WeaponType.F_ONESHOT)
                || wt.hasFlag(WeaponType.F_PROTOTYPE)) {
            return false;
        }

        if (type > Person.T_NONE &&
                !((wt.hasFlag(WeaponType.F_MECH_WEAPON) && type == Person.T_MECHWARRIOR)
                || (wt.hasFlag(WeaponType.F_AERO_WEAPON) && type != Person.T_AERO_PILOT)
                || (wt.hasFlag(WeaponType.F_TANK_WEAPON) && !(type == Person.T_VEE_GUNNER
                        || type == Person.T_NVEE_DRIVER
                        || type == Person.T_GVEE_DRIVER
                        || type == Person.T_VTOL_PILOT))
                || (wt.hasFlag(WeaponType.F_BA_WEAPON) && type != Person.T_BA)
                || (wt.hasFlag(WeaponType.F_PROTO_WEAPON) && type != Person.T_PROTO_PILOT))) {
            return false;
        }

        if (wt.getAtClass() == WeaponType.CLASS_NONE ||
                wt.getAtClass() == WeaponType.CLASS_POINT_DEFENSE ||
                wt.getAtClass() >= WeaponType.CLASS_CAPITAL_LASER) {
            return false;
        }

        if (clusterOnly && !(
                (wt.getDamage() == WeaponType.DAMAGE_BY_CLUSTERTABLE) ||
                (wt instanceof ACWeapon) ||
                (wt instanceof UACWeapon) ||
                (wt instanceof LBXACWeapon))) {
            return false;
        }

        return true;
    }

    public String getAllPrereqDesc() {
        String toReturn = "";
        for (String prereq : prereqAbilities) {
            toReturn += getDisplayName(prereq) + "<br>";
        }
        for (SkillPrereq skPr : prereqSkills) {
            toReturn += skPr.toString() + "<br>";
        }
        for (String pr : prereqMisc.keySet()) {
            toReturn += pr + ": " + prereqMisc.get(pr) + "<br/>";
        }
        if (toReturn.isEmpty()) {
            toReturn = "None";
        }
        return toReturn;
    }

    public String getPrereqAbilDesc() {
        String toReturn = "";
        for (String prereq : prereqAbilities) {
            toReturn += getDisplayName(prereq) + "<br>";
        }
        if (toReturn.isEmpty()) {
            toReturn = "None";
        }
        return toReturn;
    }

    public String getInvalidDesc() {
        String toReturn = "";
        for (String invalid : invalidAbilities) {
            toReturn += getDisplayName(invalid) + "<br>";
        }
        if (toReturn.isEmpty()) {
            toReturn = "None";
        }
        return toReturn;
    }

    public String getRemovedDesc() {
        StringBuilder toReturn = new StringBuilder();
        for (String remove : removeAbilities) {
            toReturn.append(getDisplayName(remove)).append("<br>");
        }
        if (toReturn.length() == 0) {
            toReturn = new StringBuilder("None");
        }
        return toReturn.toString();
    }

    public static String getDisplayName(String name) {
        PilotOptions options = new PilotOptions();
        IOption option = options.getOption(name);
        if (null != option) {
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

    public static List<SpecialAbility> getWeightedSpecialAbilities() {
        return getWeightedSpecialAbilities(getAllSpecialAbilities().values());
    }

    public static List<SpecialAbility> getWeightedSpecialAbilities(Collection<SpecialAbility> source) {
        List<SpecialAbility> retVal = new ArrayList<>();

        for (SpecialAbility spa : source) {
            int weight = spa.getWeight();
            while (weight > 0) {
                retVal.add(spa);
                weight--;
            }
        }

        return retVal;
    }

    //TODO: also put some static methods here that return the available options for a given SPA, so
    //we can take that out of the GUI code

}
