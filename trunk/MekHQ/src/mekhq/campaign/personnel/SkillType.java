/*
 * SkillType.java
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.ConvFighter;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.VTOL;
import mekhq.MekHQ;
import mekhq.Version;
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Skill type will hold static information for each skill type like base target number,
 * whether to count up, and XP costs for advancement.
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class SkillType implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5569555585715305914L;
	
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
	public static final String S_GUN_SPACE   = "Gunnery/Spacecraft";
	public static final String S_GUN_BA      = "Gunnery/Battlesuit";
	public static final String S_ARTILLERY   = "Artillery";
	public static final String S_SMALL_ARMS  = "Small Arms";
	public static final String S_ANTI_MECH   = "Anti-Mech";
	public static final String S_TACTICS     = "Tactics";
	//non-combat skills
	public static final String S_TECH_MECH     = "Tech/Mech";
	public static final String S_TECH_MECHANIC = "Tech/Mechanic";
	public static final String S_TECH_AERO     = "Tech/Aero";
	public static final String S_TECH_BA       = "Tech/BA";
	public static final String S_TECH_VESSEL   = "Tech/Vessel";
	public static final String S_ASTECH        = "Astech";
	public static final String S_DOCTOR        = "Doctor";
	public static final String S_MEDTECH       = "Medtech";
	public static final String S_NAV           = "Hyperspace Navigation";
	public static final String S_ADMIN         = "Administration";
	public static final String S_NEG           = "Negotiation";
	public static final String S_LEADER        = "Leadership";
	public static final String S_SCROUNGE      = "Scrounge";
	public static final String S_STRATEGY      = "Strategy";
	
	public static final String[] skillList = {S_PILOT_MECH,S_GUN_MECH,S_PILOT_AERO,S_GUN_AERO,
											  S_PILOT_GVEE,S_PILOT_VTOL,S_PILOT_NVEE,S_GUN_VEE,
						                      S_PILOT_JET,S_GUN_JET,S_PILOT_SPACE,S_GUN_SPACE,S_ARTILLERY,
						                      S_GUN_BA,S_SMALL_ARMS,S_ANTI_MECH,
						                      S_TECH_MECH,S_TECH_MECHANIC,S_TECH_AERO,S_TECH_BA,S_TECH_VESSEL,S_ASTECH,
						                      S_DOCTOR,S_MEDTECH,S_NAV,
						                      S_ADMIN,
						                      S_TACTICS,S_STRATEGY,
						                      S_NEG,S_LEADER,S_SCROUNGE};
	
    private static Hashtable<String, SkillType> lookupHash;
    private static Map<String, Integer> abilityCosts;
	private static int defaultAbilityCost = 8;
	

    public static final int EXP_ULTRA_GREEN = 0;
    public static final int EXP_GREEN = 1;
	public static final int EXP_REGULAR = 2;
	public static final int EXP_VETERAN = 3;
	public static final int EXP_ELITE = 4;
    
	private String name;
	private int target;
	private boolean countUp;
	private int greenLvl;
	private int regLvl;
	private int vetLvl;
	private int eliteLvl;	
	private Integer[] costs;
	
	public static String[] getSkillList() {
		return skillList;
	}	
	
	/** Creates new SkillType */
    public SkillType() {
        greenLvl = 1;
        regLvl = 3;
        vetLvl = 4;
        eliteLvl = 5;
        costs = new Integer[]{0,0,0,0,0,0,0,0,0,0,0};
    }
    
    public String getName() {
    	return name;
    }
    
    public int getTarget() {
		return target;
	}
    
    public void setTarget(int t) {
    	target = t;
    }
	
	public boolean countUp() {
		return countUp;
	}
	
	public int getLevelFromExperience(int expLvl) {
		switch(expLvl) {
		case(EXP_REGULAR):
			return regLvl;
		case(EXP_VETERAN):
			return vetLvl;
		case(EXP_ELITE):
			return eliteLvl;
		default:
			return greenLvl;
		}
	}
	
	public int getGreenLevel() {
		return greenLvl;
	}
	
	public void setGreenLevel(int l) {
		greenLvl = l;
	}
	
	public int getRegularLevel() {
		return regLvl;
	}
	
	public void setRegularLevel(int l) {
		regLvl = l;
	}
	
	public int getVeteranLevel() {
		return vetLvl;
	}
	
	public void setVeteranLevel(int l) {
		vetLvl = l;
	}
	
	public int getEliteLevel() {
		return eliteLvl;
	}
	
	public void setEliteLevel(int l) {
		eliteLvl = l;
	}
	
	public int getCost(int lvl) {
		if(lvl > 10 || lvl < 0) {
			return -1;
		}
		return costs[lvl];
	}
	
	public static void setCost(String name, int cost, int lvl) {
		SkillType type = lookupHash.get(name);
		if(null != name && lvl < 11) {
			type.costs[lvl] = cost;
		}
	}
	
	public boolean isPiloting() {
		return name.equals(S_PILOT_MECH) || name.equals(S_PILOT_AERO)
					|| name.equals(S_PILOT_GVEE) || name.equals(S_PILOT_VTOL)
					|| name.equals(S_PILOT_NVEE) || name.equals(S_PILOT_JET)
					|| name.equals(S_PILOT_SPACE);
	}
	
	public boolean isGunnery() {
		return name.equals(S_GUN_MECH) || name.equals(S_GUN_AERO)
					|| name.equals(S_GUN_VEE) || name.equals(S_GUN_BA)
					|| name.equals(S_SMALL_ARMS) || name.equals(S_GUN_JET)
					|| name.equals(S_GUN_SPACE) || name.equals(S_ARTILLERY);
	}
	
	public int getExperienceLevel(int lvl) {
		if(lvl >= eliteLvl) {
			return EXP_ELITE;
		}
		else if(lvl >= vetLvl) {
			return EXP_VETERAN;
		}
		else if(lvl >= regLvl) {
			return EXP_REGULAR;
		}
		else if(lvl >= greenLvl) {
			return EXP_GREEN;
		}
		return EXP_ULTRA_GREEN;
	}
	
	public int getExperienceLevelFromTarget(int tgt) {
		int level = target - tgt;
		return getExperienceLevel(level);
	}
	
	public static void initializeTypes() {
		lookupHash = new Hashtable<String, SkillType>();
		lookupHash.put(S_PILOT_MECH, createPilotingMech());
		lookupHash.put(S_GUN_MECH, createGunneryMech());
		lookupHash.put(S_PILOT_AERO, createPilotingAero());
		lookupHash.put(S_GUN_AERO, createGunneryAero());
		lookupHash.put(S_PILOT_JET, createPilotingJet());
		lookupHash.put(S_GUN_JET, createGunneryJet());
		lookupHash.put(S_PILOT_SPACE, createPilotingSpace());
		lookupHash.put(S_GUN_SPACE, createGunnerySpace());
		lookupHash.put(S_PILOT_GVEE, createPilotingGroundVee());
		lookupHash.put(S_PILOT_NVEE, createPilotingNavalVee());
		lookupHash.put(S_PILOT_VTOL, createPilotingVTOL());
		lookupHash.put(S_GUN_VEE, createGunneryVehicle());
		lookupHash.put(S_ARTILLERY, createArtillery());
		lookupHash.put(S_GUN_BA, createGunneryBA());
		lookupHash.put(S_SMALL_ARMS, createSmallArms());
		lookupHash.put(S_ANTI_MECH, createAntiMech());
		lookupHash.put(S_TECH_MECH, createTechMech());
		lookupHash.put(S_TECH_MECHANIC, createTechMechanic());
		lookupHash.put(S_TECH_AERO, createTechAero());
		lookupHash.put(S_TECH_BA, createTechBA());
		lookupHash.put(S_TECH_VESSEL, createTechVessel());
		lookupHash.put(S_ASTECH, createAstech());
		lookupHash.put(S_DOCTOR, createDoctor());
		lookupHash.put(S_MEDTECH, createMedtech());
		lookupHash.put(S_NAV, createNav());
		lookupHash.put(S_TACTICS, createTactics());
		lookupHash.put(S_STRATEGY, createStrategy());
		lookupHash.put(S_ADMIN, createAdmin());
		lookupHash.put(S_LEADER, createLeadership());
		lookupHash.put(S_NEG, createNegotiation());
		lookupHash.put(S_SCROUNGE, createScrounge());

		abilityCosts = new HashMap<String, Integer>();
		abilityCosts.put("hot_dog", 4);
		abilityCosts.put("jumping_jack", 12);
		abilityCosts.put("multi_tasker", 4);
		abilityCosts.put("oblique_attacker", 4);
		abilityCosts.put("pain_resistance", 4);
		abilityCosts.put("sniper", 12);
		abilityCosts.put("weapon_specialist", 1);
		abilityCosts.put("specialist", 4);
		abilityCosts.put("tactical_genius", 12);
		abilityCosts.put("aptitude_gunnery", 40);
		abilityCosts.put("gunnery_laser", 4);
		abilityCosts.put("gunnery_ballistic", 4);
		abilityCosts.put("gunnery_missile", 4);
		abilityCosts.put("ei_implant", 0);
		abilityCosts.put("clan_pilot_training", 0);
	}
	
	public static ArrayList<String> getAbilitiesFor(int type) {
		ArrayList<String> abils = new ArrayList<String>();
		switch(type) {
		case Person.T_MECHWARRIOR:
			abils.add("dodge_maneuver");
			abils.add("hot_dog");
			abils.add("jumping_jack");
			abils.add("maneuvering_ace");
			abils.add("melee_specialist");
			abils.add("multi_tasker");
			abils.add("pain_resistance");
			abils.add("sniper");
			abils.add("tactical_genius");
			abils.add("specialist");
			abils.add("weapon_specialist");
			abils.add("aptitude_gunnery");
			abils.add("gunnery_laser");
			abils.add("gunnery_missile");
			abils.add("gunnery_ballistic");
			abils.add("iron_man");
			break;
		case Person.T_PROTO_PILOT:
			abils.add("dodge_maneuver");
			abils.add("jumping_jack");
			abils.add("melee_specialist");
			abils.add("multi_tasker");
			abils.add("pain_resistance");
			abils.add("sniper");
			abils.add("tactical_genius");
			abils.add("specialist");
			abils.add("weapon_specialist");
			abils.add("aptitude_gunnery");
			abils.add("gunnery_laser");
			abils.add("gunnery_missile");
			abils.add("gunnery_ballistic");
			break;
		case Person.T_AERO_PILOT:
			abils.add("maneuvering_ace");
			abils.add("pain_resistance");
			abils.add("sniper");
			abils.add("tactical_genius");
			abils.add("specialist");
			abils.add("weapon_specialist");
			abils.add("aptitude_gunnery");
			abils.add("gunnery_laser");
			abils.add("gunnery_missile");
			abils.add("gunnery_ballistic");
			break;
		case Person.T_BA:
			abils.add("sniper");
			abils.add("tactical_genius");
			abils.add("specialist");
			abils.add("weapon_specialist");
			abils.add("aptitude_gunnery");
			abils.add("gunnery_laser");
			abils.add("gunnery_missile");
			abils.add("gunnery_ballistic");
			break;
		case Person.T_VEE_GUNNER:
			abils.add("maneuvering_ace");
			abils.add("multi_tasker");
			abils.add("sniper");
			abils.add("tactical_genius");
			abils.add("specialist");
			abils.add("weapon_specialist");
			abils.add("aptitude_gunnery");
			abils.add("gunnery_laser");
			abils.add("gunnery_missile");
			abils.add("gunnery_ballistic");
			break;
		case Person.T_GVEE_DRIVER:
		case Person.T_NVEE_DRIVER:
		case Person.T_VTOL_PILOT:
			abils.add("maneuvering_ace");
			abils.add("tactical_genius");
			break;
		}
		return abils;
	}
	
	public static SkillType getType(String t) {
		//legacy check for typo in earlier version
		if(t.equalsIgnoreCase("administation")) {
			return lookupHash.get(S_ADMIN);
		}
		return lookupHash.get(t);
	}
	
	public static String getExperienceLevelName(int level) {
    	switch(level) {
    	case EXP_ULTRA_GREEN:
    		return "Ultra-Green";
    	case EXP_GREEN:
    		return "Green";
    	case EXP_REGULAR:
    		return "Regular";
    	case EXP_VETERAN:
    		return "Veteran";
    	case EXP_ELITE:
    		return "Elite";
    	case -1:
    		return "Unknown";
    	default:
    		return "Impossible";
    	}
    }
	
	public static String getDrivingSkillFor(Entity en) {
		if(en instanceof Tank) {
			if(en instanceof VTOL) {
				return S_PILOT_VTOL;
			}
			//TODO: identify naval vessel
			return S_PILOT_GVEE;
		}
		else if(en instanceof SmallCraft || en instanceof Jumpship) {
			return S_PILOT_SPACE;
		}
		else if(en instanceof ConvFighter) {
			return S_PILOT_JET;
		}
		else if(en instanceof Aero) {
			return S_PILOT_AERO;
		}
		else if(en instanceof Infantry) {
			return S_ANTI_MECH;
		}
		return S_PILOT_MECH;
	}
	
	public static String getGunnerySkillFor(Entity en) {
		if(en instanceof Tank) {
			return S_GUN_VEE;
		}
		else if(en instanceof SmallCraft || en instanceof Jumpship) {
			return S_GUN_SPACE;
		}
		else if(en instanceof ConvFighter) {
			return S_GUN_JET;
		}
		else if(en instanceof Aero) {
			return S_GUN_AERO;
		}
		else if(en instanceof Infantry) {
			if(en instanceof BattleArmor) {
				return S_GUN_BA;
			}
			return S_SMALL_ARMS;
		}
		return S_GUN_MECH;
	}
	
	public static int getAbilityCost(String ability) {
		if(null == abilityCosts.get(ability)) {
			return defaultAbilityCost;
		} else {
			return abilityCosts.get(ability);
		}
	}
	
	public static void setAbilityCost(String name, int cost) {
		abilityCosts.put(name, cost);
	}
	
	public static String[][] getSkillCostsArray() {
		String[][] array = new String[skillList.length][11];
		int i = 0;
		for(String name : skillList) {
			SkillType type = lookupHash.get(name);
			for(int j = 0; j< 11; j++) {
				array[i][j] = Integer.toString(type.getCost(j));
			}
			i++;
		}
		return array;
	}
	
	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<skillType>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<name>"
				+name
				+"</name>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<target>"
				+target
				+"</target>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<countUp>"
				+countUp
				+"</countUp>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<greenLvl>"
				+greenLvl
				+"</greenLvl>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<regLvl>"
				+regLvl
				+"</regLvl>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<vetLvl>"
				+vetLvl
				+"</vetLvl>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<eliteLvl>"
				+eliteLvl
				+"</eliteLvl>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<costs>"
				+printCosts()
				+"</costs>");
		pw1.println(MekHqXmlUtil.indentStr(indent) + "</skillType>");	
	}
	
	public static void writeAbilityCostsToXML(PrintWriter pw1, int indent) {
		for(String optionName : abilityCosts.keySet()) {
			pw1.println(MekHqXmlUtil.indentStr(indent)
					+"<ability-" + optionName + ">"
					+abilityCosts.get(optionName)
					+"</ability-" + optionName + ">");
		}
	}
	
	public static void generateInstanceFromXML(Node wn, Version version) {
		SkillType retVal = null;
			
		try {		
			retVal = new SkillType();
			NodeList nl = wn.getChildNodes();
				
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				if (wn2.getNodeName().equalsIgnoreCase("name")) {
					retVal.name = wn2.getTextContent();
				} else if (wn2.getNodeName().equalsIgnoreCase("target")) {
					retVal.target = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("greenLvl")) {
					retVal.greenLvl = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("regLvl")) {
					retVal.regLvl = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("vetLvl")) {
					retVal.vetLvl = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("eliteLvl")) {
					retVal.eliteLvl = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("countUp")) {
					if(wn2.getTextContent().equalsIgnoreCase(("true"))) {
						retVal.countUp = true;
					} else {
						retVal.countUp = false;
					}
				} else if (wn2.getNodeName().equalsIgnoreCase("costs")) {
					String[] values = wn2.getTextContent().split(",");
					for(int i = 0; i < values.length; i++) {
						retVal.costs[i] = Integer.parseInt(values[i]);
					}
				} 
			}		
		} catch (Exception ex) {
			// Errrr, apparently either the class name was invalid...
			// Or the listed name doesn't exist.
			// Doh!
			MekHQ.logError(ex);
		}
		if(version.getMinorVersion() < 3) {
		    //need to change negotiation and scrounge to be countUp=false with
		    //TNs of 10
		    if(retVal.name.equals(SkillType.S_NEG) || retVal.name.equals(SkillType.S_SCROUNGE)) {
		        retVal.countUp = false;
		        retVal.target = 10;
		    }
		}
		lookupHash.put(retVal.name, retVal);
	}
	
	public static void readAbilityCostFromXML(Node wn) {
		if (wn.getNodeName().startsWith("ability-")) {
			abilityCosts.put(wn.getNodeName().split("-")[1], Integer.parseInt(wn.getTextContent()));
		}
	}
	
	private String printCosts() {
		String values = "";
		for(int i = 0; i < costs.length; i++) {
			values += Integer.toString(costs[i]);
			if(i < 10) {
				values += ",";
			}
		}
		return values;
	}
	
	public static SkillType createPilotingMech() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_MECH;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};
        
        return skill;
    }
	
	public static SkillType createGunneryMech() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_MECH;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createPilotingAero() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_AERO;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};

        return skill;
    }
	
	public static SkillType createGunneryAero() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_AERO;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createPilotingJet() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_JET;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};
     
        return skill;
    }
	
	public static SkillType createGunneryJet() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_JET;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createPilotingSpace() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_SPACE;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};
     
        return skill;
    }
	
	public static SkillType createGunnerySpace() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_SPACE;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createPilotingGroundVee() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_GVEE;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};
     
        return skill;
    }
	
	public static SkillType createPilotingNavalVee() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_NVEE;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};
     
        return skill;
    }
	
	public static SkillType createPilotingVTOL() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_VTOL;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};
     
        return skill;
    }
	
	public static SkillType createGunneryVehicle() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_VEE;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createArtillery() {
        SkillType skill = new SkillType();
        skill.name = S_ARTILLERY;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createGunneryBA() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_BA;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createSmallArms() {
        SkillType skill = new SkillType();
        skill.name = S_SMALL_ARMS;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};
     
        return skill;
    }
	
	public static SkillType createAntiMech() {
        SkillType skill = new SkillType();
        skill.name = S_ANTI_MECH;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,6,6,6,6,6,6,6,-1,-1};
     
        return skill;
    }
	
	public static SkillType createTechMech() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_MECH;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,0,6,6,6,-1,-1,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createTechMechanic() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_MECHANIC;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,0,6,6,6,-1,-1,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createTechAero() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_AERO;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,0,6,6,6,-1,-1,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createTechBA() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_BA;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,0,6,6,6,-1,-1,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createTechVessel() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_VESSEL;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,0,6,6,6,-1,-1,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createAstech() {
        SkillType skill = new SkillType();
        skill.name = S_ASTECH;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
     
        return skill;
    }

	public static SkillType createDoctor() {
        SkillType skill = new SkillType();
        skill.name = S_DOCTOR;
        skill.target = 11;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,0,8,8,8,-1,-1,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createMedtech() {
        SkillType skill = new SkillType();
        skill.name = S_MEDTECH;
        skill.target = 11;
        skill.countUp = false;
        skill.costs = new Integer[]{16,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createNav() {
        SkillType skill = new SkillType();
        skill.name = S_NAV;
        skill.target = 8;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,-1,-1};
     
        return skill;
    }
	
	public static SkillType createTactics() {
        SkillType skill = new SkillType();
        skill.name = S_TACTICS;
        skill.target = 0;
        skill.countUp = true;
        skill.costs = new Integer[]{12,6,6,6,6,6,6,6,6,6,6};
     
        return skill;
    }
	
	public static SkillType createStrategy() {
        SkillType skill = new SkillType();
        skill.name = S_STRATEGY;
        skill.target = 0;
        skill.countUp = true;
        skill.costs = new Integer[]{12,6,6,6,6,6,6,6,6,6,6};
     
        return skill;
    }
	
	public static SkillType createAdmin() {
        SkillType skill = new SkillType();
        skill.name = S_ADMIN;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,0,4,4,4,-1,-1,-1,-1,-1};
     
        return skill;
    }
	
	public static SkillType createLeadership() {
        SkillType skill = new SkillType();
        skill.name = S_LEADER;
        skill.target = 0;
        skill.countUp = true;
        skill.costs = new Integer[]{12,6,6,6,6,6,6,6,6,6,6};
     
        return skill;
    }
	
	public static SkillType createNegotiation() {
        SkillType skill = new SkillType();
        skill.name = S_NEG;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};
     
        return skill;
    }
	
	public static SkillType createScrounge() {
        SkillType skill = new SkillType();
        skill.name = S_SCROUNGE;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};
     
        return skill;
    }
}