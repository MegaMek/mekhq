/*
 * SpecialAbility.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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

import megamek.Version;
import megamek.common.EquipmentType;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.common.util.weightedMaps.WeightedIntMap;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.autocannons.ACWeapon;
import megamek.common.weapons.autocannons.LBXACWeapon;
import megamek.common.weapons.autocannons.UACWeapon;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;

/**
 * This object will serve as a wrapper for a specific pilot special ability. In the actual
 * person object we will use PersonnelOptions, so these objects will not get written to actual
 * personnel. Instead, we will we will keep track of a full static
 * hash of SPAs that will contain important information on XP costs and pre-reqs that can be
 * looked up to see if a person is eligible for a particular option. All of this
 * will be customizable via an external XML file that can be user selected in the campaign
 * options (and possibly user editable).
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class SpecialAbility {
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

    @Override
    @SuppressWarnings(value = "unchecked") // FIXME: Broken Java with it's Object clones
    public SpecialAbility clone() {
        SpecialAbility clone = new SpecialAbility(lookupName);
        clone.displayName = this.displayName;
        clone.desc = this.desc;
        clone.xpCost = this.xpCost;
        clone.weight = this.weight;
        clone.prereqAbilities = (Vector<String>) this.prereqAbilities.clone();
        clone.invalidAbilities = (Vector<String>) this.invalidAbilities.clone();
        clone.removeAbilities = (Vector<String>) this.removeAbilities.clone();
        clone.choiceValues = (Vector<String>) this.choiceValues.clone();
        clone.prereqSkills = (Vector<SkillPrereq>) this.prereqSkills.clone();
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

    public void writeToXML(final PrintWriter pw, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenTag(pw, indent++, "ability");
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "displayName", displayName);
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "lookupName", lookupName);
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "desc", desc);
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "xpCost", xpCost);
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "weight", weight);
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "prereqAbilities", Utilities.combineString(prereqAbilities, "::"));
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "invalidAbilities", Utilities.combineString(invalidAbilities, "::"));
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "removeAbilities", Utilities.combineString(removeAbilities, "::"));
        MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "choiceValues", Utilities.combineString(choiceValues, "::"));
        for (SkillPrereq skillpre : prereqSkills) {
            skillpre.writeToXML(pw, indent);
        }

        for (String pre : prereqMisc.keySet()) {
            MekHqXmlUtil.writeSimpleXMLTag(pw, indent, "miscPrereq", pre + ":" + prereqMisc.get(pre));
        }
        MekHqXmlUtil.writeSimpleXMLCloseTag(pw, --indent, "ability");
    }

    @SuppressWarnings("unchecked")
    public static void generateInstanceFromXML(Node wn, PersonnelOptions options, Version v) {
        try {
            SpecialAbility retVal = new SpecialAbility();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
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
            LogManager.getLogger().error("", ex);
        }
    }

    public static void generateSeparateInstanceFromXML(Node wn, Hashtable<String, SpecialAbility> spHash, PersonnelOptions options) {
        try {
            SpecialAbility retVal = new SpecialAbility();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("displayName")) {
                    retVal.displayName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                    retVal.desc = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("lookupName")) {
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
            LogManager.getLogger().error("", ex);
        }
    }

    public static void initializeSPA() {
        specialAbilities = new Hashtable<>();
        edgeTriggers = new Hashtable<>();
        implants = new Hashtable<>();

        Document xmlDoc;

        try (InputStream is = new FileInputStream("data/universe/defaultspa.xml")) { // TODO : Remove inline file path
            // Using factory get an instance of document builder
            DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(is);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
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

            if (wn.getParentNode() != spaEle) {
                continue;
            }

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

    public static @Nullable SpecialAbility getDefaultAbility(String name) {
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

    public static @Nullable SpecialAbility getOption(String name) {
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

    /**
     * This return a random weapon to specialize in, selected based on weightings. Introtech
     * weaponry is weighted at 50, standard weaponry at 25, advanced weaponry at 5, while experimental
     * and unofficial weaponry are both weighted at 1.
     *
     * @param person the person to generate the weapon specialization for
     * @param techLevel the maximum tech level to generate a weapon for
     * @param year the year to generate the specialization for
     * @param clusterOnly whether to only consider cluster weapons or not
     * @return the name of the selected weapon, or null if there are no weapons that can be selected
     */
    public static @Nullable String chooseWeaponSpecialization(final Person person, final int techLevel,
                                                              final int year, final boolean clusterOnly) {
        final WeightedIntMap<EquipmentType> weapons = new WeightedIntMap<>();
        // First try to generate based on the person's unit
        if ((person.getUnit() != null) && (person.getUnit().getEntity() != null)) {
            for (final Mounted mounted : person.getUnit().getEntity().getEquipment()) {
                addValidWeaponryToMap(mounted.getType(), person, techLevel, year, clusterOnly, weapons);
            }
        }

        // If that doesn't generate a valid weapon, then turn to the wider list
        if (weapons.isEmpty()) {
            for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements(); ) {
                final EquipmentType equipmentType = e.nextElement();
                addValidWeaponryToMap(equipmentType, person, techLevel, year, clusterOnly, weapons);
            }
        }
        return weapons.isEmpty() ? null : weapons.randomItem().getName();
    }

    /**
     * This is a worker method to add any valid weaponry to the weighted map used to generate a
     * random weapon specialization
     *
     * @param equipmentType the equipment type to test for validity
     * @param person the person to generate the weapon specialization for
     * @param techLevel the maximum tech level to generate a weapon for
     * @param year the year to generate the specialization for
     * @param clusterOnly whether to only consider cluster weapons or not
     * @param weapons the weighted map of weaponry to add the equipmentType to if valid
     */
    private static void addValidWeaponryToMap(final EquipmentType equipmentType,
                                              final Person person, final int techLevel,
                                              final int year, final boolean clusterOnly,
                                              final WeightedIntMap<EquipmentType> weapons) {
        // Ensure it is a weapon eligible for the SPA in question, and the tech level is IS for
        // IS personnel and Clan for Clan personnel
        if (!isWeaponEligibleForSPA(equipmentType, person.getPrimaryRole(), clusterOnly)
                || (TechConstants.isClan(equipmentType.getTechLevel(year)) != person.isClanner())) {
            return;
        }

        // Ensure the weapon's tech level is valid (zero or above)
        int weaponTechLevel = equipmentType.getTechLevel(year);
        if (weaponTechLevel < 0) {
            return;
        }
        // Ensure that the weapon's tech level is lower than that of the specified tech level
        weaponTechLevel = Utilities.getSimpleTechLevel(weaponTechLevel);
        if (techLevel < weaponTechLevel) {
            return;
        }

        // Determine the weight based on the tech level
        final int weight = (weaponTechLevel < CampaignOptions.TECH_STANDARD) ? 50
                : (weaponTechLevel < CampaignOptions.TECH_ADVANCED) ? 25
                : (weaponTechLevel < CampaignOptions.TECH_EXPERIMENTAL) ? 5
                : 1;
        weapons.add(weight, equipmentType);
    }

    /**
     * Worker function that determines if a piece of equipment is eligible for being selected for an SPA.
     * @param et Equipment type to check
     * @param role Person's primary role. This check is ignored if PersonnelRole.NONE is passed in.
     * @param clusterOnly All weapon types or just ones that do rolls on the cluster table
     */
    public static boolean isWeaponEligibleForSPA(EquipmentType et, PersonnelRole role, boolean clusterOnly) {
        if (!(et instanceof WeaponType)) {
            return false;
        }
        if (et instanceof InfantryWeapon
                || et instanceof BayWeapon
                || et instanceof InfantryAttack) {
            return false;
        }
        WeaponType wt = (WeaponType) et;
        if (wt.isCapital()
                || wt.isSubCapital()
                || wt.hasFlag(WeaponType.F_INFANTRY)
                || wt.hasFlag(WeaponType.F_ONESHOT)
                || wt.hasFlag(WeaponType.F_PROTOTYPE)) {
            return false;
        }

        if (!role.isCivilian()
                && !((wt.hasFlag(WeaponType.F_MECH_WEAPON) && !role.isMechWarrior())
                || (wt.hasFlag(WeaponType.F_AERO_WEAPON) && !role.isAerospacePilot())
                || (wt.hasFlag(WeaponType.F_TANK_WEAPON) && !role.isVehicleCrewmember())
                || (wt.hasFlag(WeaponType.F_BA_WEAPON) && !role.isBattleArmour())
                || (wt.hasFlag(WeaponType.F_PROTO_WEAPON) && !role.isProtoMechPilot()))) {
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
            toReturn += skPr + "<br>";
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
        PersonnelOptions options = new PersonnelOptions();
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
        defaultSpecialAbilities = (Hashtable<String, SpecialAbility>) specialAbilities.clone();
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
