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

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.ConvFighter;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.VTOL;

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

    public static final int EXP_ULTRA_GREEN = 0;
    public static final int EXP_GREEN = 1;
	public static final int EXP_REGULAR = 2;
	public static final int EXP_VETERAN = 3;
	public static final int EXP_ELITE = 4;
    
	private String name;
	private int target;
	private boolean countUp;
	private int defaultLvl;
	private Integer[] costs;
	
	public static String[] getSkillList() {
		return skillList;
	}	
	
	/** Creates new SkillType */
    public SkillType() {
        defaultLvl = 3;
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
	
	public int getDefaultLevel() {
		return defaultLvl;
	}
	
	public void setDefaultLevel(int lvl) {
		this.defaultLvl = lvl;
	}
	
	public int getCost(int lvl) {
		if(lvl > 10 || lvl < 0) {
			return -1;
		}
		return costs[lvl];
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
	
	public int getExperienceLevel(int value) {
		if(countUp()) {
			if(value <= 0) {
				return EXP_ULTRA_GREEN;
			}
			else if(value == 1) {
				return EXP_GREEN;
			}
			else if(value == 2) {
				return EXP_REGULAR;
			}
			else if(value <= 5) {
				return EXP_VETERAN;
			}
			else {
				return EXP_ELITE;
			}
		}
		else if(isPiloting() || name.equals(S_ANTI_MECH)) {
			if(value >= 8) {
				return EXP_ULTRA_GREEN;
			}
			else if(value >= 6) {
				return EXP_GREEN;
			}
			else if(value == 5) {
				return EXP_REGULAR;
			}
			else if(value == 4) {
				return EXP_VETERAN;
			}
			else {
				return EXP_ELITE;
			}
		}
		else if(isGunnery()) {
			if(value >= 7) {
				return EXP_ULTRA_GREEN;
			}
			else if(value >= 5) {
				return EXP_GREEN;
			}
			else if(value == 4) {
				return EXP_REGULAR;
			}
			else if(value >= 3) {
				return EXP_VETERAN;
			}
			else {
				return EXP_ELITE;
			}
		}
		else if(name.equals(S_MEDICAL)) {
			if(value >= 11) {
				return EXP_ULTRA_GREEN;
			}
			else if(value == 10) {
				return EXP_GREEN;
			}
			else if(value == 8) {
				return EXP_REGULAR;
			}
			else if(value >= 7) {
				return EXP_VETERAN;
			}
			else {
				return EXP_ELITE;
			}
		}
		else {
			if(value >= 10) {
				return EXP_ULTRA_GREEN;
			}
			else if(value >= 8) {
				return EXP_GREEN;
			}
			else if(value == 7) {
				return EXP_REGULAR;
			}
			else if(value >= 6) {
				return EXP_VETERAN;
			}
			else {
				return EXP_ELITE;
			}
		}
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
    	default:
    		return "Unknown";
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
	
	public static SkillType createPilotingMech() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_MECH;
        skill.target = 8;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};
        
        return skill;
    }
	
	public static SkillType createGunneryMech() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_MECH;
        skill.target = 7;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,8,8,8};
     
        return skill;
    }
	
	public static SkillType createPilotingAero() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_AERO;
        skill.target = 8;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};

        return skill;
    }
	
	public static SkillType createGunneryAero() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_AERO;
        skill.target = 7;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,8,8,8};
     
        return skill;
    }
	
	public static SkillType createPilotingJet() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_JET;
        skill.target = 8;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};
     
        return skill;
    }
	
	public static SkillType createGunneryJet() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_JET;
        skill.target = 7;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,8,8,8};
     
        return skill;
    }
	
	public static SkillType createPilotingSpace() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_SPACE;
        skill.target = 8;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};
     
        return skill;
    }
	
	public static SkillType createGunnerySpace() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_SPACE;
        skill.target = 7;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,8,8,8};
     
        return skill;
    }
	
	public static SkillType createPilotingGroundVee() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_GVEE;
        skill.target = 8;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};
     
        return skill;
    }
	
	public static SkillType createPilotingNavalVee() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_NVEE;
        skill.target = 8;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};
     
        return skill;
    }
	
	public static SkillType createPilotingVTOL() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_VTOL;
        skill.target = 8;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};
     
        return skill;
    }
	
	public static SkillType createGunneryVehicle() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_VEE;
        skill.target = 7;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,8,8,8};
     
        return skill;
    }
	
	public static SkillType createArtillery() {
        SkillType skill = new SkillType();
        skill.name = S_ARTILLERY;
        skill.target = 7;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,8,8,8};
     
        return skill;
    }
	
	public static SkillType createGunneryBA() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_BA;
        skill.target = 7;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,8,8,8};
     
        return skill;
    }
	
	public static SkillType createSmallArms() {
        SkillType skill = new SkillType();
        skill.name = S_SMALL_ARMS;
        skill.target = 7;
        skill.countUp = false;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};
     
        return skill;
    }
	
	public static SkillType createAntiMech() {
        SkillType skill = new SkillType();
        skill.name = S_ANTI_MECH;
        skill.target = 8;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,6,6,6,6,6,6,6,6,6};
     
        return skill;
    }
	
	public static SkillType createTechMech() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_MECH;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,6,6,6,6,6,6,6,6,6};
     
        return skill;
    }
	
	public static SkillType createTechMechanic() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_MECHANIC;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,6,6,6,6,6,6,6,6,6};
     
        return skill;
    }
	
	public static SkillType createTechAero() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_AERO;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,6,6,6,6,6,6,6,6,6};
     
        return skill;
    }
	
	public static SkillType createTechBA() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_BA;
        skill.target = 10;
        skill.countUp = false;
        skill.costs = new Integer[]{12,6,6,6,6,6,6,6,6,6,6};
     
        return skill;
    }

	public static SkillType createMedical() {
        SkillType skill = new SkillType();
        skill.name = S_MEDICAL;
        skill.target = 11;
        skill.countUp = false;
        skill.costs = new Integer[]{16,8,8,8,8,8,8,8,8,8,8};
     
        return skill;
    }
	
	public static SkillType createTacticsGround() {
        SkillType skill = new SkillType();
        skill.name = S_TAC_GROUND;
        skill.target = 0;
        skill.countUp = true;
        skill.costs = new Integer[]{12,6,6,6,6,6,6,6,6,6,6};
     
        return skill;
    }
	
	public static SkillType createTacticsSpace() {
        SkillType skill = new SkillType();
        skill.name = S_TAC_SPACE;
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
	
	public static SkillType createInit() {
        SkillType skill = new SkillType();
        skill.name = S_INIT;
        skill.target = 0;
        skill.countUp = true;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};
     
        return skill;
    }
	
	public static SkillType createAdmin() {
        SkillType skill = new SkillType();
        skill.name = S_ADMIN;
        skill.target = 0;
        skill.countUp = true;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};
     
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
        skill.target = 0;
        skill.countUp = true;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};
     
        return skill;
    }
	
	public static SkillType createScrounge() {
        SkillType skill = new SkillType();
        skill.name = S_SCROUNGE;
        skill.target = 0;
        skill.countUp = true;
        skill.costs = new Integer[]{8,4,4,4,4,4,4,4,4,4,4};
     
        return skill;
    }
}