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
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
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
    private boolean useClanBonuses;
    private int antiMekProb;
    private int[] specialAbilBonus;
    private int combatSmallArmsBonus;
    private int supportSmallArmsBonus;
    private int[] tacticsMod;
    private int artilleryProb;
    private int artilleryBonus;
    private int secondSkillProb;
    private int secondSkillBonus;
    
    //special abilities
    
    public RandomSkillPreferences() {
    	overallRecruitBonus = 0;
    	recruitBonuses = new int[Person.T_NUM];
    	randomizeSkill = true;
    	useClanBonuses = true;
    	antiMekProb = 10;
    	combatSmallArmsBonus = -3;
    	supportSmallArmsBonus = -10;
    	specialAbilBonus = new int[]{-10,-10,-2,0,1};
    	tacticsMod = new int[]{-10,-10,-7,-4,-1};
    	artilleryProb = 10;
    	artilleryBonus = -2;
    	secondSkillProb = 0;
    	secondSkillBonus = -4;
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
    
    public void setRecruitBonus(int type, int bonus) {
    	if(type > recruitBonuses.length) {
    		return;
    	}
    	recruitBonuses[type] = bonus;
    }
    
    public int getSpecialAbilBonus(int type) {
    	if(type > specialAbilBonus.length) {
    		return 0;
    	} 
    	return specialAbilBonus[type];
    }
    
    public void setSpecialAbilBonus(int type, int bonus) {
    	if(type > specialAbilBonus.length) {
    		return;
    	}
    	specialAbilBonus[type] = bonus;
    }
    
    public void setRandomizeSkill(boolean b) {
    	this.randomizeSkill = b;
    }
    
    public boolean randomizeSkill() {
    	return randomizeSkill;
    }
    
    public void setUseClanBonuses(boolean b) {
    	this.useClanBonuses = b;
    }
    
    public boolean useClanBonuses() {
    	return useClanBonuses;
    }
    
    public int getAntiMekProb() {
    	return antiMekProb;
    }
    
    public void setAntiMekProb(int b) {
    	antiMekProb = b;
    }
    
    public int getCombatSmallArmsBonus() {
    	return combatSmallArmsBonus;
    }
    
    public void setCombatSmallArmsBonus(int b) {
    	combatSmallArmsBonus = b;
    }
    
    public int getSupportSmallArmsBonus() {
    	return supportSmallArmsBonus;
    }
    
    public void setSupportSmallArmsBonus(int b) {
    	supportSmallArmsBonus = b;
    }
    
    public int getTacticsMod(int lvl) {
    	return tacticsMod[lvl];
    }
    
    public void setTacticsMod(int lvl, int bonus) {
    	if(lvl > tacticsMod.length) {
    		return;
    	}
    	tacticsMod[lvl] = bonus;
    }
    
    public void setArtilleryProb(int b) {
    	this.artilleryProb = b;
    }
    
    public int getArtilleryProb() {
    	return artilleryProb;
    }
    
    public void setArtilleryBonus(int b) {
    	this.artilleryBonus = b;
    }
    
    public int getArtilleryBonus() {
    	return artilleryBonus;
    }
    
    public void setSecondSkillProb(int b) {
    	this.secondSkillProb = b;
    }
    
    public int getSecondSkillProb() {
    	return secondSkillProb;
    }
    
    public void setSecondSkillBonus(int b) {
    	this.secondSkillBonus = b;
    }
    
    public int getSecondSkillBonus() {
    	return secondSkillBonus;
    }
    
    public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<randomSkillPreferences>");
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "overallRecruitBonus", overallRecruitBonus);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<recruitBonuses>"
				+Utilities.printIntegerArray(recruitBonuses)
				+"</recruitBonuses>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<specialAbilBonus>"
				+Utilities.printIntegerArray(specialAbilBonus)
				+"</specialAbilBonus>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<tacticsMod>"
				+Utilities.printIntegerArray(tacticsMod)
				+"</tacticsMod>");
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "randomizeSkill", randomizeSkill);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useClanBonuses", useClanBonuses);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "antiMekProb", antiMekProb);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "combatSmallArmsBonus", combatSmallArmsBonus);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "supportSmallArmsBonus", supportSmallArmsBonus);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "artilleryProb", artilleryProb);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "artilleryBonus", artilleryBonus);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "secondSkillProb", secondSkillProb);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "secondSkillBonus", secondSkillBonus);
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
			else if (wn2.getNodeName().equalsIgnoreCase("useClanBonuses")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useClanBonuses = true;
				else
					retVal.useClanBonuses = false;
			}
			else if (wn2.getNodeName().equalsIgnoreCase("antiMekProb")) {
				retVal.antiMekProb = Integer.parseInt(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("combatSmallArmsBonus")) {
				retVal.combatSmallArmsBonus = Integer.parseInt(wn2.getTextContent().trim());
			} 
			else if (wn2.getNodeName().equalsIgnoreCase("supportSmallArmsBonus")) {
				retVal.supportSmallArmsBonus = Integer.parseInt(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("artilleryProb")) {
				retVal.artilleryProb = Integer.parseInt(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("artilleryBonus")) {
				retVal.artilleryBonus = Integer.parseInt(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("secondSkillProb")) {
				retVal.secondSkillProb = Integer.parseInt(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("secondSkillBonus")) {
				retVal.secondSkillBonus = Integer.parseInt(wn2.getTextContent().trim());
			}
			else if (wn2.getNodeName().equalsIgnoreCase("recruitBonuses")) {
				String[] values = wn2.getTextContent().split(",");
				for(int i = 0; i < values.length; i++) {
					retVal.recruitBonuses[i] = Integer.parseInt(values[i]);
				}
			} 
			else if (wn2.getNodeName().equalsIgnoreCase("tacticsMod")) {
				String[] values = wn2.getTextContent().split(",");
				for(int i = 0; i < values.length; i++) {
					retVal.tacticsMod[i] = Integer.parseInt(values[i]);
				}
			} 
			else if (wn2.getNodeName().equalsIgnoreCase("specialAbilBonus")) {
				String[] values = wn2.getTextContent().split(",");
				for(int i = 0; i < values.length; i++) {
					retVal.specialAbilBonus[i] = Integer.parseInt(values[i]);
				}
			} 
		}

		MekHQ.logMessage("Load Random Skill Preferences Complete!", 4);

		return retVal;
	}
}