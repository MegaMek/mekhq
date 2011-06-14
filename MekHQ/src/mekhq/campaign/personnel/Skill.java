/*
 * Skill.java
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
import java.util.Calendar;
import java.util.GregorianCalendar;

import megamek.common.Pilot;
import megamek.common.TargetRoll;
import mekhq.MekHQApp;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlSerializable;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Ranks;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.work.IMedicalWork;
import mekhq.campaign.work.IWork;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * As ov v0.1.9, we will be tracking a group of skills on the person. These skills will define
 * personnel rather than subtypes wrapped around pilots and teams. This will allow for considerably
 * more flexibility in the kinds of personnel available.
 * 
 * Four important characteristics will determine how each skill works
 * level - this is the level of the skill. By default this will go from 0 to 10, but the max will
 *         be customizable. These won't necessarily correspond to named levels (e.g. Green, Elite)
 *         By assigning skill costs of 0 to some levels, these can basically be skipped and by 
 *         assigning skill costs of -1, they can be made inaccessible.
 * bonus - this is a bonus that the given person has for this skill which is separable from level.
 *         Primarily this allows for rpg-style attribute bonuses to come into play.
 * target - this is the baseline target number for the skill when level and bonus are zero.
 * countUp - this is a boolean that defines whether this skill's target is a btech style 
 *           "roll greater than or equal to" (false) or an rpg-style bonus to a roll (true)
 * The actual target number for a skill is given by 
 *         countUp: target+lvl+bonus
 *         !countUp: target - level - bonus
 * by clever manipulation of these values and skillcosts in campaignOptions, players should be
 * able to recreate any of the rpg versions or their own homebrew system. The default setup 
 * will follow the core rulebooks (not aToW).
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Skill implements Serializable {
	
	//combat skills
	public static final String S_PILOT_MECH  = "Piloting/Mech";
	public static final String S_PILOT_AERO  = "Piloting/Aerospace";
	public static final String S_PILOT_JET   = "Piloting/Aircraft";
	public static final String S_PILOT_GVEE  = "Piloting/Ground Vehicle";
	public static final String S_PILOT_VTOL  = "Piloting/VTOL";
	public static final String S_PILOT_NVEE  = "Piloting/Naval";
	public static final String S_PILOT_SPACE = "Piloting/Spacecraft";
	public static final String S_GUN_MECH    = "Gunnery/Mech";
	public static final String S_GUN_AERO    = "Gunnery/Aerospace";
	public static final String S_GUN_JET     = "Gunnery/Aircraft";
	public static final String S_GUN_VEE     = "Gunnery/Vehicle";
	public static final String S_GUN_SPACE   = "Gunnery/Spacecract";
	public static final String S_GUN_BA      = "Gunnery/Battlesuit";
	public static final String S_ARTILLERY   = "Artillery";
	public static final String S_SMALL_ARMS  = "Small Arms";
	public static final String S_ANTI_MECH   = "Anti-Mech";
	public static final String S_TAC_GROUND  = "Tactics/Ground";
	public static final String S_TAC_SPACE   = "Tactics/Space";
	public static final String S_INIT        = "Initiative";
	//non-combat skills
	public static final String S_TECH_MECH     = "Tech/Mech";
	public static final String S_TECH_MECHANIC = "Tech/Mechanic";
	public static final String S_TECH_AERO     = "Tech/Aero";
	public static final String S_TECH_BA       = "Tech/BA";
	public static final String S_MEDICAL       = "Medical";
	public static final String S_ADMIN         = "Administation";
	public static final String S_NEG           = "Negotiation";
	public static final String S_LEADER        = "Leadership";
	public static final String S_SCROUNGE      = "Scrounge";
	public static final String S_STRATEGY      = "Strategy";
	
	private String type;
	private int level;
	private int bonus;
	private int target;
	private boolean countUp;

	public Skill(String t) {
		this.type = t;
		this.level = 0;
		this.bonus = 0;
		//TODO: base this on skill type
		this.target = 7;
		this.countUp = false;
	}
	
	public Skill(String t, int lvl) {
		this(t);
		this.level = lvl;
	}

	public int getLevel() {
		return level;
	}
	
	public void setLevel(int l) {
		this.level = l;
	}
	
	public int getBonus() {
		return bonus;
	}
	
	public void setBonus(int b) {
		this.bonus = b;
	}
	
	public int getTarget() {
		return target;
	}
	
	public void setTarget(int t) {
		this.target = t;
	}
	
	public boolean countUp() {
		return countUp;
	}
	
	public void setCountUp(boolean b) {
		this.countUp = b;
	}
	
	public int getFinalSkillValue() {
		if(countUp) {
			return target + level + bonus;
		} else {
			return target - level - bonus;
		}	
	}
	
	
	
}