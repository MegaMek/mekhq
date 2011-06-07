/*
 * PilotPerson.java
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
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Pilot;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.options.IOption;
import megamek.common.options.PilotOptions;
import mekhq.MekHQApp;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Ranks;
import mekhq.campaign.SkillCosts;
import mekhq.campaign.Unit;
import mekhq.campaign.team.MedicalTeam;
import mekhq.campaign.team.SupportTeam;
import mekhq.campaign.work.IMedicalWork;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A Person wrapper for pilots and vee crews
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class PilotPerson extends Person implements IMedicalWork {
	private static final long serialVersionUID = -4758195062070601267L;
	
    private Pilot pilot;
    private Unit unit;
    private int unitId;
    
    //track name separately in MHQ
    private String name;
   
    public PilotPerson() {
    	this(null, 0, null);
    }
    
    public PilotPerson(Pilot p, int t, Ranks r) {
        super(r);
        this.pilot = p;
        if(null != p) {
        	this.name = p.getName();
        }
        setType(t);
        reCalc();
    }
    
    public PilotPerson(Pilot p, int t, Unit u, Ranks r) {
        this(p,t,r);
        this.unit = u;
    }

    @Override
    public void reCalc() {
        if (pilot == null)
        	return;
        
        this.portraitCategory = pilot.getPortraitCategory();
        this.portraitFile = pilot.getPortraitFileName();
    }
    
    @Override
    public int getExperienceLevel() {
    	double average = (pilot.getGunnery() + pilot.getPiloting())/2.0;
    	int level = EXP_GREEN;;
    	if(average<=2.5) {
    		level = EXP_ELITE;
    	} else if (average <=3.5) {
    		level = EXP_VETERAN;
    	} else if (average<=4.5) {
    		level = EXP_REGULAR;
    	}
    	return level;
    }
    
    @Override
    public String getSkillSummary() {
    	return getExperienceLevelName(getExperienceLevel()) + " (" + pilot.getGunnery() + "/" + pilot.getPiloting() + ")";
    }
    
    public static int getTypeBy(Entity en) {
        if(en instanceof Mech) {
            return T_MECHWARRIOR;
        }
        else if(en instanceof Protomech) {
            return T_PROTO_PILOT;
        } 
        else if(en instanceof Aero) {
            return T_AERO_PILOT;
        }
        else if(en instanceof Tank) {
            return T_VEE_CREW;
        }
        else if(en instanceof BattleArmor) {
            return T_BA;
        }
        return -1;
    }
    
    public boolean canPilot(Entity en) {
        if(en instanceof Mech && getType() == T_MECHWARRIOR) {
            return true;
        }
        else if(en instanceof Protomech && getType() == T_PROTO_PILOT) {
            return true;
        } 
        else if(en instanceof Aero && getType() == T_AERO_PILOT) {
            return true;
        }
        else if(en instanceof Tank && getType() == T_VEE_CREW) {
            return true;
        }
        else if(en instanceof BattleArmor && getType() == T_BA) {
            return true;
        }
        return false;
    }
    
    public Pilot getPilot() {
        return pilot;
    }
    
    public void setPilot(Pilot p) {
        this.pilot = p;
    }

    @Override
    public String getName() {
    	return name;
    }
    
    public void setName(String n) {
    	this.name = n;
    	resetPilotName();
    }
    
    /**This sets the MM pilot name from the name and rank in PilotPerson
     * Doing it this way gives us some flexibility in how names are reported
     * in MM vs. MHQ
     */
    public void resetPilotName() {
    	pilot.setName(getFullTitle());
    }
    
    @Override
    public void setRank(int r) {
    	super.setRank(r);
    	resetPilotName();
    }
    
    @Override
    public String getDesc() {
        String care = "";
        String status = "";
        if(pilot.getHits() > 0) {
            status = " (" + pilot.getStatusDesc() + ")";
        }
        return care + pilot.getName() + " [" + pilot.getGunnery() + "/" + pilot.getPiloting() + " " + getTypeDesc() + "]" + status;
    }
    
    @Override
    public String toString() {
    	String s = pilot.getName() + " (" + pilot.getGunnery() + "/" + pilot.getPiloting() + ")";
    	if(null != unit) {
    		s = s + ", <i>" + unit.getEntity().getDisplayName() + "</i>";
    	}
    	return "<html>" + s + "</html>";
    }
    
    @Override
    public String getDescHTML() {
        String toReturn = "<html><font size='2'><b>" + pilot.getName() + "</b><br/>";
        toReturn += getTypeDesc() + " (" + pilot.getGunnery() + "/" + pilot.getPiloting() + ")<br/>";
        toReturn += pilot.getStatusDesc();
        if(isDeployed()) {
            toReturn += " (DEPLOYED)";
        }
        toReturn += "</font></html>";
        return toReturn;
    }
    
    public boolean isAssigned() {
        return null != unit;
    }
    
    public Unit getAssignedUnit() {
        return unit;
    }
    
    public void setAssignedUnit(Unit u) {
        this.unit = u;
        if(null != u) {
        	this.unitId = u.getId();
        } else {
        	this.unitId = -1;
        }
    }

    /**
     * heal one hit on the pilot/crew
     */
    @Override
    public void heal() {
    	if(getPilot().getHits() > 0) {
    		getPilot().setHits(getPilot().getHits() - 1);
    	}
    	if(!needsFixing()) {
			medicalTeamId = -1;
		}
    }
    
    @Override
    public void setPortraitCategory(String s) {
        super.setPortraitCategory(s);
        pilot.setPortraitCategory(s);
        
    }

    @Override
    public void setPortraitFileName(String s) {
        super.setPortraitFileName(s);
        pilot.setPortraitFileName(s);
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+getType()
				+"</type>");
		
		// If a pilot doesn't have a unit, well...
		// This should be null.
		if (unit != null) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<unitId>"
					+unit.getId()
					+"</unitId>");
		}

		// Pilot is a megamek class with no XML serialization support.
		// But there's a constructor for building them...
		// Plus a bunch of "set" functions...
		//TODO: Are any other items on Pilot important for XML serialization?
		//TODO: Handle separate ballistic/missile/energy gunneries
		//TODO: Handle pilot special abilities
		if (pilot != null) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<name>"
					+name
					+"</name>");
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<pilotGunnery>"
					+pilot.getGunnery()
					+"</pilotGunnery>");
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<pilotPiloting>"
					+pilot.getPiloting()
					+"</pilotPiloting>");
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<pilotHits>"
					+pilot.getHits()
					+"</pilotHits>");
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<pilotCommandBonus>"
					+pilot.getCommandBonus()
					+"</pilotCommandBonus>");
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<pilotInitBonus>"
					+pilot.getInitBonus()
					+"</pilotInitBonus>");
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<pilotNickname>"
					+pilot.getNickname()
					+"</pilotNickname>");
			if (pilot.countOptions(PilotOptions.LVL3_ADVANTAGES) > 0) {
				pw1.println(MekHqXmlUtil.indentStr(indent+1)
						+"<advantages>"
						+String.valueOf(pilot.getOptionList("::", PilotOptions.LVL3_ADVANTAGES))
						+"</advantages>");
			}
			if (pilot.countOptions(PilotOptions.EDGE_ADVANTAGES) > 0) {
				pw1.println(MekHqXmlUtil.indentStr(indent+1)
						+"<edge>"
						+String.valueOf(pilot.getOptionList("::", PilotOptions.EDGE_ADVANTAGES))
						+"</edge>");
			}
			if (pilot.countOptions(PilotOptions.MD_ADVANTAGES) > 0) {
				pw1.println(MekHqXmlUtil.indentStr(indent+1)
						+"<implants>"
						+String.valueOf(pilot.getOptionList("::", PilotOptions.MD_ADVANTAGES))
						+"</implants>");
			}
		}
		
		writeToXmlEnd(pw1, indent, id);
	}
	
	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		String pilotName = null;
		int pilotGunnery = -1;
		int pilotPiloting = -1;
		int pilotHits = -1;
		int pilotCommandBonus = -1;
		int pilotInitBonus = -1;
		String pilotNickname = null;
		String advantages = null;
		String edge = null;
		String implants = null;
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("pilotNickname")) {
				pilotNickname = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("pilotName")) {
				pilotName = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("name")) {
				name = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("pilotGunnery")) {
				pilotGunnery = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("pilotPiloting")) {
				pilotPiloting = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("pilotHits")) {
				pilotHits = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("pilotCommandBonus")) {
				pilotCommandBonus = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("pilotInitBonus")) {
				pilotInitBonus = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("advantages")) {
				advantages = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("edge")) {
				edge = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("implants")) {
				implants = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("type")) {
				setType(Integer.parseInt(wn2.getTextContent()));
			} else if (wn2.getNodeName().equalsIgnoreCase("unitId")) {
				unitId = Integer.parseInt(wn2.getTextContent());
			}
		}
		
		//handle backwards compatability for pilot names
		if(null == name) {
			name = pilotName;
		}
		
		pilot = new Pilot(pilotName, pilotGunnery, pilotPiloting);
		
		if (pilotHits >= 0)
			pilot.setHits(pilotHits);
		
		if (pilotNickname != null)
			pilot.setNickname(pilotNickname);
		
		if (pilotInitBonus >= 0)
			pilot.setInitBonus(pilotInitBonus);

		if (pilotCommandBonus >= 0)
			pilot.setCommandBonus(pilotCommandBonus);
		
		if ((null != advantages) && (advantages.trim().length() > 0)) {
            StringTokenizer st = new StringTokenizer(advantages,"::");
            while (st.hasMoreTokens()) {
                String adv = st.nextToken();
                String advName = Pilot.parseAdvantageName(adv);
                Object value = Pilot.parseAdvantageValue(adv);

                try {
                    pilot.getOptions().getOption(advName).setValue(value);
                } catch (Exception e) {
                    MekHQApp.logMessage("Error restoring advantage: " +  adv);
                }
            }
        }
		if ((null != edge) && (edge.trim().length() > 0)) {
            StringTokenizer st = new StringTokenizer(edge,"::");
            while (st.hasMoreTokens()) {
                String adv = st.nextToken();
                String advName = Pilot.parseAdvantageName(adv);
                Object value = Pilot.parseAdvantageValue(adv);

                try {
                    pilot.getOptions().getOption(advName).setValue(value);
                } catch (Exception e) {
                    MekHQApp.logMessage("Error restoring edge: " +  adv);
                }
            }
        }
		if ((null != implants) && (implants.trim().length() > 0)) {
            StringTokenizer st = new StringTokenizer(implants,"::");
            while (st.hasMoreTokens()) {
                String adv = st.nextToken();
                String advName = Pilot.parseAdvantageName(adv);
                Object value = Pilot.parseAdvantageValue(adv);

                try {
                    pilot.getOptions().getOption(advName).setValue(value);
                } catch (Exception e) {
                    MekHQApp.logMessage("Error restoring implants: " +  adv);
                }
            }
        }
		
	}

	public int getUnitId() {
		return unitId;
	}

	@Override
	public String getCallsign() {
		if(null != pilot) {
			String nick = pilot.getNickname();
			if(null == nick || nick.equals("")) {
				nick = "-";
			}
			return nick;
		} else {
			return "-";
		}
	}
	
	@Override
	public void improveSkill(int type) {
		switch(type) {
		case SkillCosts.SK_GUN:
			pilot.setGunnery(pilot.getGunnery() - 1);
			break;
		case SkillCosts.SK_PILOT:
			pilot.setPiloting(pilot.getPiloting() - 1);
			break;
		case SkillCosts.SK_ARTY:
			pilot.setArtillery(pilot.getArtillery() - 1);
			break;
		case SkillCosts.SK_TAC:
			pilot.setCommandBonus(pilot.getCommandBonus() + 1);
			break;
		case SkillCosts.SK_INIT:
			pilot.setInitBonus(pilot.getInitBonus() + 1);
			break;
		case SkillCosts.SK_TOUGH:
			pilot.setToughness(pilot.getToughness() + 1);
			break;
		default:
			Logger.getLogger(Pilot.class.getName()).log(Level.WARNING,
					"Could not improve " + SkillCosts.getSkillName(type) +" skill for  : " + getName());
		}
	}
	

    public int getEdge() {
    	return pilot.getOptions().intOption("edge");
    }
    
    public void setEdge(int e) {
    	for (Enumeration<IOption> i = getPilot().getOptions(PilotOptions.EDGE_ADVANTAGES); i.hasMoreElements();) {
        	IOption ability = i.nextElement();
        	if(ability.getName().equals("edge")) {
        		ability.setValue(e);
        	}
        }
    }
    
    /**
     * This will flip the boolean status of the current edge trigger
     * @param name
     */
    public void changeEdgeTrigger(String name) {
    	for (Enumeration<IOption> i = getPilot().getOptions(PilotOptions.EDGE_ADVANTAGES); i.hasMoreElements();) {
        	IOption ability = i.nextElement();
        	if(ability.getName().equals(name)) {
        		ability.setValue(!ability.booleanValue());
        	}
        }
    }
    
    /**
     * This function returns an html-coded tooltip that says what 
     * edge will be used
     * @return
     */
    public String getEdgeTooltip() {
    	String edgett = "";
    	for (Enumeration<IOption> i = getPilot().getOptions(PilotOptions.EDGE_ADVANTAGES); i.hasMoreElements();) {
        	IOption ability = i.nextElement();
        	//yuck, it would be nice to have a more fool-proof way of identifying edge triggers
        	if(ability.getName().contains("edge_when") && ability.booleanValue()) {
        		edgett = edgett + ability.getDescription() + "<br>";
        	}
        }
    	if(edgett.equals("")) {
    		return "No triggers set";
    	}
    	return "<html>" + edgett + "</html>";
    }
    
    /**
     * This function returns an html-coded list that says what 
     * abilities are enabled for this pilot
     * @return
     */
    public String getAbilityList(String type) {
    	String abilityString = "";
        for (Enumeration<IOption> i = getPilot().getOptions(type); i.hasMoreElements();) {
        	IOption ability = i.nextElement();
        	if(ability.booleanValue()) {
        		abilityString = abilityString + ability.getDisplayableNameWithValue() + "<br>";
        	}
        }
        if(abilityString.equals("")) {
        	return null;
        }
        return "<html>" + abilityString + "</html>";
    }
    
    public void acquireAbility(String type, String name, Object value) {
    	for (Enumeration<IOption> i = getPilot().getOptions(type); i.hasMoreElements();) {
        	IOption ability = i.nextElement();
        	if(ability.getName().equals(name)) {
        		ability.setValue(value);
        	}
    	}
    }

	@Override
	public boolean needsFixing() {
		return pilot.getHits() > 0;
	}

	@Override
	public String succeed() {
		heal();
		return " <font color='green'><b>Successfully healed one hit.</b></font>";
	}

	@Override
	public boolean canFix(SupportTeam team) {
		return team instanceof MedicalTeam && ((MedicalTeam)team).getPatients() < 25;
	}
	
}
