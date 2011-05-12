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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This object will keep track of all the various skill types and their associated experience point costs to advance
 * This does NOT track skills for individual Persons - different kinds of skills are tracked differently on Persons 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class SkillCosts implements Serializable {
	
	public static final int SK_GUN      = 0;
	public static final int SK_PILOT    = 1;
	public static final int SK_AMECH    = 2;
	public static final int SK_TECH     = 3;
	public static final int SK_MED      = 4;
	public static final int SK_ARTY     = 5;
	public static final int SK_TAC      = 6;
	public static final int SK_INIT     = 7;
	public static final int SK_TOUGH    = 8;
	
	public static String getSkillName(int skill) {
		switch(skill) {
		case SK_GUN:
			return "Gunnery";
		case SK_PILOT:
			return "Piloting";
		case SK_AMECH:
			return "Anti-Mech";
		case SK_TECH:
			return "Tech";
		case SK_MED:
			return "Medical";
		case SK_ARTY:
			return "Artillery";
		case SK_TAC:
			return "Tactics";
		case SK_INIT:
			return "Init Bonus";
		case SK_TOUGH:
			return "Toughness";
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
}