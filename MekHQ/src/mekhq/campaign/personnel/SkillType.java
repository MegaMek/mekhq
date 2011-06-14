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

import java.io.Serializable;
import java.util.Hashtable;

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
	
	private static final String[] skillList = {S_PILOT_MECH,S_GUN_MECH,S_PILOT_AERO,S_GUN_AERO,
											  S_PILOT_GVEE,S_PILOT_VTOL,S_PILOT_NVEE,S_GUN_VEE,
						                      S_PILOT_JET,S_GUN_JET,S_PILOT_SPACE,S_GUN_SPACE,S_ARTILLERY,
						                      S_GUN_BA,S_SMALL_ARMS,S_ANTI_MECH,
						                      S_TECH_MECH,S_TECH_MECHANIC,S_TECH_AERO,S_TECH_BA,S_MEDICAL,
						                      S_TAC_GROUND,S_TAC_SPACE,S_STRATEGY,S_INIT,
						                      S_ADMIN,S_NEG,S_LEADER,S_SCROUNGE};
	
    private static Hashtable<String, SkillType> lookupHash;

	private String name;
	private int target;
	private boolean countUp;
	
	public static String[] getSkillList() {
		return skillList;
	}	
	
	/** Creates new SkillType */
    public SkillType() {
        // default constructor
    }
    
    public String getName() {
    	return name;
    }
    
    public int getTarget() {
		return target;
	}
	
	public boolean countUp() {
		return countUp;
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
		lookupHash.put(S_GUN_BA, createGunneryBA());
		lookupHash.put(S_SMALL_ARMS, createSmallArms());
		lookupHash.put(S_ANTI_MECH, createAntiMech());
		lookupHash.put(S_TECH_MECH, createTechMech());
		lookupHash.put(S_TECH_MECHANIC, createTechMechanic());
		lookupHash.put(S_TECH_AERO, createTechAero());
		lookupHash.put(S_TECH_BA, createTechBA());
		lookupHash.put(S_MEDICAL, createMedical());
		lookupHash.put(S_TAC_GROUND, createTacticsGround());
		lookupHash.put(S_TAC_SPACE, createTacticsSpace());
		lookupHash.put(S_INIT, createInit());
		lookupHash.put(S_STRATEGY, createStrategy());
		lookupHash.put(S_ADMIN, createAdmin());
		lookupHash.put(S_LEADER, createLeadership());
		lookupHash.put(S_NEG, createNegotiation());
		lookupHash.put(S_SCROUNGE, createScrounge());


	}
	
	public static SkillType getType(String t) {
		return lookupHash.get(t);
	}
	
	public static SkillType createPilotingMech() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_MECH;
        skill.target = 8;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createGunneryMech() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_MECH;
        skill.target = 7;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createPilotingAero() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_AERO;
        skill.target = 8;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createGunneryAero() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_AERO;
        skill.target = 7;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createPilotingJet() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_JET;
        skill.target = 8;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createGunneryJet() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_JET;
        skill.target = 7;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createPilotingSpace() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_SPACE;
        skill.target = 8;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createGunnerySpace() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_SPACE;
        skill.target = 7;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createPilotingGroundVee() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_GVEE;
        skill.target = 8;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createPilotingNavalVee() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_NVEE;
        skill.target = 8;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createPilotingVTOL() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_VTOL;
        skill.target = 8;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createGunneryVehicle() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_VEE;
        skill.target = 7;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createGunneryBA() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_BA;
        skill.target = 7;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createSmallArms() {
        SkillType skill = new SkillType();
        skill.name = S_SMALL_ARMS;
        skill.target = 7;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createAntiMech() {
        SkillType skill = new SkillType();
        skill.name = S_ANTI_MECH;
        skill.target = 8;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createTechMech() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_MECH;
        skill.target = 10;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createTechMechanic() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_MECHANIC;
        skill.target = 10;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createTechAero() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_AERO;
        skill.target = 10;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createTechBA() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_BA;
        skill.target = 10;
        skill.countUp = false;
     
        return skill;
    }

	public static SkillType createMedical() {
        SkillType skill = new SkillType();
        skill.name = S_MEDICAL;
        skill.target = 11;
        skill.countUp = false;
     
        return skill;
    }
	
	public static SkillType createTacticsGround() {
        SkillType skill = new SkillType();
        skill.name = S_TAC_GROUND;
        skill.target = 0;
        skill.countUp = true;
     
        return skill;
    }
	
	public static SkillType createTacticsSpace() {
        SkillType skill = new SkillType();
        skill.name = S_TAC_SPACE;
        skill.target = 0;
        skill.countUp = true;
     
        return skill;
    }
	
	public static SkillType createStrategy() {
        SkillType skill = new SkillType();
        skill.name = S_STRATEGY;
        skill.target = 0;
        skill.countUp = true;
     
        return skill;
    }
	
	public static SkillType createInit() {
        SkillType skill = new SkillType();
        skill.name = S_INIT;
        skill.target = 0;
        skill.countUp = true;
     
        return skill;
    }
	
	public static SkillType createAdmin() {
        SkillType skill = new SkillType();
        skill.name = S_ADMIN;
        skill.target = 0;
        skill.countUp = true;
     
        return skill;
    }
	
	public static SkillType createLeadership() {
        SkillType skill = new SkillType();
        skill.name = S_LEADER;
        skill.target = 0;
        skill.countUp = true;
     
        return skill;
    }
	
	public static SkillType createNegotiation() {
        SkillType skill = new SkillType();
        skill.name = S_NEG;
        skill.target = 0;
        skill.countUp = true;
     
        return skill;
    }
	
	public static SkillType createScrounge() {
        SkillType skill = new SkillType();
        skill.name = S_SCROUNGE;
        skill.target = 0;
        skill.countUp = true;
     
        return skill;
    }
}