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

import megamek.common.UnitType;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This object tracks a specific skill prerequisite for a special ability. This object can list more
 * than one skill and we will track these skills in a hashmap where the value gives the minimum skill
 * level. The collection of skills is treated as an OR statement such that a person possessing any of the
 * skills at the appropriate level will evaluate as eligible. To create AND conditions, use multiple skill
 * prereqs in the SpecialAbility object.
 *
 * We are going to limit the skill levels by the Green, Regular, Veteran, Elite notation such
 * that:
 * 0 - Any
 * 1 - Green
 * 2 - Regular
 * 3 - Veteran
 * 4 - Elite
 * This way, if the user changes the meaning of various skill levels, they won't have to redo all of
 * their prereqs - we could consider expanding this to allow users to specify a more specific numeric
 * skill level (to allow for better consistency with AToW) for example
 *
 * @author Jay Lawson
 *
 */
public class SkillPrereq {
    private Hashtable<String, Integer> skillSet;

    public SkillPrereq() {
        skillSet = new Hashtable<>();
    }

    @Override
    @SuppressWarnings("unchecked") // FIXME: Broken Java with it's Object clones
    public SkillPrereq clone() {
        SkillPrereq clone = new SkillPrereq();
        clone.skillSet = (Hashtable<String, Integer>) this.skillSet.clone();
        return clone;
    }

    public boolean isEmpty() {
        return skillSet.isEmpty();
    }

    public boolean qualifies(Person p) {
        return qualifies(p.getSkills());
    }

    public boolean qualifies(Skills s) {
        for (String skillName : skillSet.keySet()) {
            if (s.hasSkill(skillName)) {
                if (s.getSkill(skillName).getExperienceLevel() >= skillSet.get(skillName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if the given unit type "qualifies" for this skill pre-requisite.
     * For now, we simply check whether the pre-requisite skills are required for the unit type
     * @param unitType the type of unit that is being checked
     * @return
     */
    public boolean qualifies(int unitType) {
        switch (unitType) {
            case UnitType.AERO:
            case UnitType.AEROSPACEFIGHTER:
                return skillSet.containsKey(SkillType.S_PILOT_AERO) ||
                        skillSet.containsKey(SkillType.S_GUN_AERO);
            case UnitType.BATTLE_ARMOR:
                return skillSet.containsKey(SkillType.S_GUN_BA) ||
                        skillSet.containsKey(SkillType.S_ANTI_MECH);
            case UnitType.CONV_FIGHTER:
                return skillSet.containsKey(SkillType.S_GUN_JET) ||
                        skillSet.containsKey(SkillType.S_PILOT_JET);
            case UnitType.DROPSHIP:
            case UnitType.JUMPSHIP:
            case UnitType.WARSHIP:
            case UnitType.SPACE_STATION:
            case UnitType.SMALL_CRAFT:
                return skillSet.containsKey(SkillType.S_PILOT_SPACE) ||
                        skillSet.containsKey(SkillType.S_GUN_SPACE) ||
                        skillSet.containsKey(SkillType.S_TECH_VESSEL) ||
                        skillSet.containsKey(SkillType.S_NAV);
            case UnitType.GUN_EMPLACEMENT:
            case UnitType.TANK:
                return skillSet.containsKey(SkillType.S_PILOT_GVEE) ||
                        skillSet.containsKey(SkillType.S_GUN_VEE);
            case UnitType.INFANTRY:
                return skillSet.containsKey(SkillType.S_SMALL_ARMS) ||
                        skillSet.containsKey(SkillType.S_ANTI_MECH);
            case UnitType.NAVAL:
                return skillSet.containsKey(SkillType.S_PILOT_NVEE) ||
                        skillSet.containsKey(SkillType.S_GUN_VEE);
            case UnitType.PROTOMEK:
                return skillSet.containsKey(SkillType.S_GUN_PROTO);
            case UnitType.VTOL:
                return skillSet.containsKey(SkillType.S_PILOT_VTOL) ||
                        skillSet.containsKey(SkillType.S_GUN_VEE);
            case UnitType.MEK:
                return skillSet.containsKey(SkillType.S_PILOT_MECH) ||
                        skillSet.containsKey(SkillType.S_GUN_MECH);
            default:
                return false;
        }
    }

    public int getSkillLevel(String skillName) {
        if (null != skillSet.get(skillName)) {
            return skillSet.get(skillName);
        }
        return -1;
    }

    public void addPrereq(String type, int lvl) {
        skillSet.put(type, lvl);
    }

    @Override
    public String toString() {
        String toReturn = "";
        Enumeration<String> enumKeys = skillSet.keys();
        while (enumKeys.hasMoreElements()) {
            String key = enumKeys.nextElement();
            int lvl = skillSet.get(key);
            String skillLvl = "";
            if (lvl >= SkillType.EXP_GREEN) {
                skillLvl = SkillType.getExperienceLevelName(lvl) + " ";
            }
            toReturn += skillLvl + SkillType.getType(key).getName();
            if (enumKeys.hasMoreElements()) {
                toReturn += "<br>OR ";
            }
        }
        return "{" + toReturn + "}";
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "skillPrereq");
        for (String key : skillSet.keySet()) {
            int lvl = skillSet.get(key);
            if (lvl <= 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "skill", key);
            } else {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "skill", key + "::" + SkillType.getExperienceLevelName(lvl));
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "skillPrereq");
    }

    public static SkillPrereq generateInstanceFromXML(Node wn) {
        SkillPrereq retVal = null;

        try {
            retVal = new SkillPrereq();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("skill")) {
                    String skillName = wn2.getTextContent();
                    int level = 0;
                    if (skillName.contains("::")) {
                        level = parseStringForLevel(skillName);
                        skillName = parseStringForName(skillName);
                    }
                    //if the skill name does not match existing skills, then ignore
                    if (null != SkillType.getType(skillName)) {
                        retVal.addPrereq(skillName, level);
                    }
                }
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        return retVal;
    }

    private static String parseStringForName(String s) {
        return s.split("::")[0];
    }

    private static int parseStringForLevel(String s) {
        String[] temp = s.split("::");
        if (temp.length < 2) {
            return 0;
        } else {
            switch (temp[1].substring(0, 1)) {
                case "G":
                    return 1;
                case "R":
                    return 2;
                case "V":
                    return 3;
                case "E":
                    return 4;
                default:
                    return 0;
            }
        }
    }
}
