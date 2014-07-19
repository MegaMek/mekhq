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

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;

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
public class SkillPrereq implements MekHqXmlSerializable {
    private Hashtable<String, Integer> skillset;
    
    public SkillPrereq() {
        skillset = new Hashtable<String, Integer>();
    }
    
    @SuppressWarnings("unchecked") // FIXME: Broken Java with it's Object clones
	public SkillPrereq clone() {
    	SkillPrereq clone = new SkillPrereq();
    	clone.skillset = (Hashtable<String, Integer>)this.skillset.clone();
    	return clone;
    }
    
    public boolean isEmpty() {
        return skillset.isEmpty();
    }
            
    public boolean qualifies(Person p) {
        for(String skillName : skillset.keySet()) {
            if(p.hasSkill(skillName)) {
                if(p.getSkill(skillName).getExperienceLevel() >= skillset.get(skillName)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public int getSkillLevel(String skillName) {
    	if(null != skillset.get(skillName)) {
    		return skillset.get(skillName);
    	}
    	return -1;
    }

    public void addPrereq(String type, int lvl) {
    	skillset.put(type, lvl);
    }
    
    @Override
    public String toString() {
        String toReturn = "";
        Enumeration<String> enumKeys = skillset.keys();
        while(enumKeys.hasMoreElements()) {
            String key = enumKeys.nextElement();
            SkillType.getType(key).getName();
            int lvl = skillset.get(key);
            String skillLvl = "";
            if(lvl >= SkillType.EXP_GREEN) {
                skillLvl = SkillType.getExperienceLevelName(lvl) + " ";
            }
            toReturn += skillLvl + SkillType.getType(key).getName();
            if(enumKeys.hasMoreElements()) {
                toReturn += "<br>OR ";
            }
        }
        return "{" + toReturn + "}";
    }
    
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<skillPrereq>");
        for(String key : skillset.keySet()) {
            int lvl = skillset.get(key);
            if(lvl <= 0) {
                pw1.println(MekHqXmlUtil.indentStr(indent+1)
                        +"<displayName>"
                        +key
                        +"</displayName>");
            } else {
                pw1.println(MekHqXmlUtil.indentStr(indent+1)
                        +"<displayName>"
                        +key + "::" + SkillType.getExperienceLevelName(lvl)
                        +"</displayName>");
            }
        }
        
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</skillPrereq>");
    }
    
    public static SkillPrereq generateInstanceFromXML(Node wn) {
        SkillPrereq retVal = null;
        
        try {       
            retVal = new SkillPrereq();
            NodeList nl = wn.getChildNodes();
                
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("skill")) {
                    String skillName = parseStringForName(wn2.getTextContent());
                    //if the skill name does not match existing skills, then ignore
                    if(null != SkillType.getType(skillName)) {
                        retVal.skillset.put(skillName, parseStringForLevel(wn2.getTextContent()));
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
    
    private static String parseStringForName(String s) {
        return s.split("::")[0];
    }
    
    private static int parseStringForLevel(String s) {
        String[] temp = s.split("::");
        if(temp.length < 2) {
            return 0;
        } else {
            switch(temp[1].substring(0, 1)) {
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