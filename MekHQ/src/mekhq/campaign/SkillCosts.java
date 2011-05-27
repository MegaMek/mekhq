/*
 * Ranks.java
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
import java.util.HashMap;
import java.util.Map;

import mekhq.MekHQApp;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This object will keep track of all the various skill types and their associated experience point costs to advance
 * This does NOT track skills for individual Persons - different kinds of skills are tracked differently on Persons 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class SkillCosts implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5723522838337157640L;
	
	public static final int SK_GUN      = 0;
	public static final int SK_PILOT    = 1;
	public static final int SK_AMECH    = 2;
	public static final int SK_TECH     = 3;
	public static final int SK_MED      = 4;
	public static final int SK_ARTY     = 5;
	public static final int SK_TAC      = 6;
	public static final int SK_INIT     = 7;
	public static final int SK_TOUGH    = 8;
	public static final int SK_NUM      = 9;
	
	public static String getSkillName(int skill) {
		switch(skill) {
		case SK_GUN:
			return "Gunnery";
		case SK_PILOT:
			return "Piloting";
		case SK_AMECH:
			return "Anti-Mek";
		case SK_TECH:
			return "Tech";
		case SK_MED:
			return "Medical";
		case SK_ARTY:
			return "Artillery";
		case SK_TAC:
			return "Tactics";
		case SK_INIT:
			return "Init B";
		case SK_TOUGH:
			return "Tough";
		default:
			return "?";
		}
	}
	
	//costs for each skill are recorded in an array of length 7
	//the value indicates the cost to get the skill at that level, so
	//xpCosts.get(SK_GUN)[2] 
	//is the cost to reduce the gunnery skill from 3 to 2
	//For support ratings:
	//R - 4
	//V - 3
	//E - 2
	//for skills that count up (like tactics), it goes the other way, so
	//xpCosts.get(SK_TACTICS)[2]
	//is the cost of raising tactics from 1 to 2
	Map<Integer, Integer[]> xpCosts = new HashMap<Integer, Integer[]>();
	 
	Map<String, Integer> abilityCosts = new HashMap<String, Integer>();
	private int defaultAbilityCost;
	
	public SkillCosts() {
		initializeCosts();
		defaultAbilityCost = 8;
	}
	
	private void initializeCosts() {
		xpCosts.put(SK_GUN, new Integer[]{8,8,8,8,8,8,8});
		xpCosts.put(SK_PILOT, new Integer[]{4,4,4,4,4,4,4});
		xpCosts.put(SK_AMECH, new Integer[]{6,6,6,6,6,6,6});
		xpCosts.put(SK_TECH, new Integer[]{-1,-1,20,10,5,0,0});
		xpCosts.put(SK_MED, new Integer[]{-1,-1,20,10,5,0,0});
		xpCosts.put(SK_ARTY, new Integer[]{8,8,8,8,8,8,8});
		xpCosts.put(SK_TAC, new Integer[]{4,4,4,4,4,4,4});
		xpCosts.put(SK_INIT, new Integer[]{4,4,4,4,4,4,4});
		xpCosts.put(SK_TOUGH, new Integer[]{4,4,4,4,4,4,4});
		
		abilityCosts.put("hot_dog", 4);
		abilityCosts.put("jumping_jack", 12);
		abilityCosts.put("multi_tasker", 4);
		abilityCosts.put("oblique_attacker", 4);
		abilityCosts.put("pain_resistance", 4);
		abilityCosts.put("sniper", 12);
		abilityCosts.put("weapon_specialist", 12);
		abilityCosts.put("specialist", 4);
		abilityCosts.put("tactical_genius", 12);
		abilityCosts.put("aptitude_gunnery", 40);
		abilityCosts.put("gunnery_laser", 4);
		abilityCosts.put("gunnery_ballistic", 4);
		abilityCosts.put("gunnery_missile", 4);
		abilityCosts.put("ei_implant", 0);
		abilityCosts.put("clan_pilot_training", 0);
	}
	
	public int getCost(int skill, int level, boolean usesSupportRating) {
		if(usesSupportRating) {
			level = 5 - level;
		}
		Integer[] costs = xpCosts.get(skill);
		if(null != costs && level < costs.length && level >= 0) {
			return costs[level];
		}
		return -1;
	}
	
	public int getAbilityCost(String ability) {
		if(null == abilityCosts.get(ability)) {
			return defaultAbilityCost;
		} else {
			return abilityCosts.get(ability);
		}
	}
	
	public String[][] getSkillArray() {
		String[][] array = new String[7][SK_NUM];
		for(int i = 0; i < SK_NUM; i++) {
			Integer[] col = xpCosts.get(i);
			for(int j = 0; j < 7; j++) {
				array[j][i] = Integer.toString(col[j]);
			}
		}
		return array;
	}
	
	public String[] getSkillTitles() {
		String[] titles = new String[SK_NUM];
		for(int i = 0; i < SK_NUM; i++) {
			titles[i] = getSkillName(i);
		}
		return titles;
	}
	
	public void setCost(int cost, int level, int type) {
		xpCosts.get(type)[level] = cost;
	}
	
	public void setAbilityCost(String name, int cost) {
		abilityCosts.put(name, cost);
	}
	
	public static String getLevelNames(int i) {
		String skill = "";
		if(i == 6) {
			skill = "/Novice";
		}
		else if(i == 5) {
			skill = "/Green";
		}
		else if(i == 4) {
			skill ="/Regular";
		}
		else if(i == 3) {
			skill = "/Veteran";
		}
		else if(i == 2) {
			skill = "/Elite";
		}
		else if(i == 1) {
			skill = "/Heroic";
		}
		else if(i == 0) {
			skill = "/Legend";
		}
		return i + skill;
	}
	
	private String printValues(int type) {
		String values = "";
		Integer[] costs = xpCosts.get(type);
		for(int i = 0; i < costs.length; i++) {
			values += Integer.toString(costs[i]);
			if(i < 6) {
				values += ",";
			}
		}
		return values;
	}
	
	private void readValuesFromXML(String text, int type) {
		String[] values = text.split(",");
		for(int i = 0; i < values.length; i++) {
			xpCosts.get(type)[i] = Integer.parseInt(values[i]);
		}
	}
	
	public void writeToXml(PrintWriter pw1, int indent) {
	
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<gunnery>"
				+printValues(SK_GUN)
				+"</gunnery>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<piloting>"
				+printValues(SK_PILOT)
				+"</piloting>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<antiMek>"
				+printValues(SK_AMECH)
				+"</antiMek>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<tech>"
				+printValues(SK_TECH)
				+"</tech>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<medical>"
				+printValues(SK_MED)
				+"</medical>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<artillery>"
				+printValues(SK_ARTY)
				+"</artillery>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<tactics>"
				+printValues(SK_TAC)
				+"</tactics>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<init>"
				+printValues(SK_INIT)
				+"</init>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<tough>"
				+printValues(SK_TOUGH)
				+"</tough>");
		for(String optionName : abilityCosts.keySet()) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<ability-" + optionName + ">"
					+abilityCosts.get(optionName)
					+"</ability-" + optionName + ">");
		}
		
	}
	
	public static SkillCosts generateInstanceFromXML(Node wn) {
		SkillCosts retVal = null;
	
		try {		
			retVal = new SkillCosts();
			NodeList nl = wn.getChildNodes();
			
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				if (wn2.getNodeName().equalsIgnoreCase("gunnery")) {
					retVal.readValuesFromXML(wn2.getTextContent(), SK_GUN);
				} 
				else if (wn2.getNodeName().equalsIgnoreCase("piloting")) {
					retVal.readValuesFromXML(wn2.getTextContent(), SK_PILOT);
				} 
				else if (wn2.getNodeName().equalsIgnoreCase("antiMek")) {
					retVal.readValuesFromXML(wn2.getTextContent(), SK_AMECH);
				} 
				else if (wn2.getNodeName().equalsIgnoreCase("tech")) {
					retVal.readValuesFromXML(wn2.getTextContent(), SK_TECH);
				} 
				else if (wn2.getNodeName().equalsIgnoreCase("medical")) {
					retVal.readValuesFromXML(wn2.getTextContent(), SK_MED);
				} 
				else if (wn2.getNodeName().equalsIgnoreCase("artillery")) {
					retVal.readValuesFromXML(wn2.getTextContent(), SK_ARTY);
				} 
				else if (wn2.getNodeName().equalsIgnoreCase("tactics")) {
					retVal.readValuesFromXML(wn2.getTextContent(), SK_TAC);
				} 
				else if (wn2.getNodeName().equalsIgnoreCase("init")) {
					retVal.readValuesFromXML(wn2.getTextContent(), SK_INIT);
				} 
				else if (wn2.getNodeName().equalsIgnoreCase("tough")) {
					retVal.readValuesFromXML(wn2.getTextContent(), SK_TOUGH);
				} 
				else if (wn2.getNodeName().startsWith("ability-")) {
					retVal.setAbilityCost(wn2.getNodeName().split("-")[1], Integer.parseInt(wn2.getTextContent()));
				} 
			}		
		} catch (Exception ex) {
			// Errrr, apparently either the class name was invalid...
			// Or the listed name doesn't exist.
			// Doh!
			MekHQApp.logError(ex);
		}
		
		return retVal;
	}
}