/*
 * RandomSkillPreferences.java
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

package mekhq.campaign;

import java.io.PrintWriter;
import java.io.Serializable;

import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson
 */
public class RandomSkillPreferences implements Serializable {
	private static final long serialVersionUID = 5698008431749303602L;
	
	
    private int overallRecruitBonus;
    private int[] recruitBonuses;
    private boolean randomizeSkill;

    //probability of tactics skill for combat personnel by level
    //default small arms skill for combat personnel and what level 
    //default prob of anti-mech skill for infantry
    //assign bonus points for clan warriors
    
    //special abilities
    
    public RandomSkillPreferences() {
    	overallRecruitBonus = 0;
    	recruitBonuses = new int[Person.T_NUM];
    	randomizeSkill = false;
    }
    
    
    public int getOverallRecruitBonus() {
    	return overallRecruitBonus;
    }
    
    public void setOverallRecruitBonus(int b) {
    	overallRecruitBonus = b;
    }
    
    public int getRecruitBonus(int type) {
    	if(type > recruitBonuses.length) {
    		return 0;
    	} 
    	return recruitBonuses[type];
    }
    
    public void setRecruitBonus(int bonus, int type) {
    	if(type > recruitBonuses.length) {
    		return;
    	}
    	recruitBonuses[type] = bonus;
    }
    
    public void setRandomizeSkill(boolean b) {
    	this.randomizeSkill = b;
    }
    
    public boolean randomizeSkill() {
    	return randomizeSkill;
    }
    
    public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<randomSkillPreferences>");
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "overallRecruitBonus", overallRecruitBonus);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<recruitBonuses>"
				+printRecruitBonuses()
				+"</recruitBonuses>");
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "randomizeSkill", randomizeSkill);
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</randomSkillPreferences>");
	}

	public static RandomSkillPreferences generateRandomSkillPreferencesFromXml(Node wn) {
		MekHQ.logMessage("Loading Random Skill Preferences from XML...", 4);

		wn.normalize();
		RandomSkillPreferences retVal = new RandomSkillPreferences();
		NodeList wList = wn.getChildNodes();

		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			MekHQ.logMessage("---",5);
			MekHQ.logMessage(wn2.getNodeName(),5);
			MekHQ.logMessage("\t"+wn2.getTextContent(),5);

			if (wn2.getNodeName().equalsIgnoreCase("overallRecruitBonus")) {
				retVal.overallRecruitBonus = Integer.parseInt(wn2.getTextContent().trim());
			} 
			else if (wn2.getNodeName().equalsIgnoreCase("randomizeSkill")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.randomizeSkill = true;
				else
					retVal.randomizeSkill = false;
			}
			else if (wn2.getNodeName().equalsIgnoreCase("recruitBonuses")) {
				String[] values = wn2.getTextContent().split(",");
				for(int i = 0; i < values.length; i++) {
					retVal.recruitBonuses[i] = Integer.parseInt(values[i]);
				}
			} 
		}

		MekHQ.logMessage("Load Random Skill Preferences Complete!", 4);

		return retVal;
	}
	
	private String printRecruitBonuses() {
		String values = "";
		for(int i = 0; i < recruitBonuses.length; i++) {
			values += Integer.toString(recruitBonuses[i]);
			if(i < (recruitBonuses.length-1)) {
				values += ",";
			}
		}
		return values;
	}
}