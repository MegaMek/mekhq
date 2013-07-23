/*
 * Utilities.java
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

package mekhq;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.ConvFighter;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.VTOL;
import megamek.common.WeaponType;
import megamek.common.options.IOption;
import megamek.common.weapons.BayWeapon;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Utilities {
	private static final int MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;

    public static int roll3d6() {      
        Vector<Integer> rolls = new Vector<Integer>();
        rolls.add(Compute.d6());
        rolls.add(Compute.d6());
        rolls.add(Compute.d6());
        Collections.sort(rolls);
        return (rolls.elementAt(0) + rolls.elementAt(1));       
    }
    
    public static ArrayList<AmmoType> getMunitionsFor(Entity entity, AmmoType cur_atype, int techLvl) {
        ArrayList<AmmoType> atypes = new ArrayList<AmmoType>();
        for(AmmoType atype : AmmoType.getMunitionsFor(cur_atype.getAmmoType())) {
            //this is an abbreviated version of setupMunitions in the CustomMechDialog
            //TODO: clan/IS limitations?
            
            if ((entity instanceof Aero)
                        && !((atype.getAmmoType() == AmmoType.T_MML)
                                || (atype.getAmmoType() == AmmoType.T_ATM)
                                || (atype.getAmmoType() == AmmoType.T_NARC) 
                                || (atype.getAmmoType() == AmmoType.T_AC_LBX))) {
                continue;
            }
            
            int lvl = atype.getTechLevel(entity.getTechLevelYear());
            if(lvl < 0) {
            	lvl = 0;
            }
            if(techLvl < Utilities.getSimpleTechLevel(lvl)) {
            	continue;
            }
            if(TechConstants.isClan(cur_atype.getTechLevel(entity.getTechLevelYear())) != TechConstants.isClan(lvl)) {
            	continue;
            }

            // Only Protos can use Proto-specific ammo
            if (atype.hasFlag(AmmoType.F_PROTOMECH)
                            && !(entity instanceof Protomech)) {
                continue;
            }

            // When dealing with machine guns, Protos can only
            // use proto-specific machine gun ammo
            if ((entity instanceof Protomech)
                            && atype.hasFlag(AmmoType.F_MG)
                            && !atype.hasFlag(AmmoType.F_PROTOMECH)) {
                continue;
            }

            // Battle Armor ammo can't be selected at all.
            // All other ammo types need to match on rack size and tech.
            if ((atype.getRackSize() == cur_atype.getRackSize())
            		&& (atype.hasFlag(AmmoType.F_BATTLEARMOR) == cur_atype.hasFlag(AmmoType.F_BATTLEARMOR))
            		&& (atype.hasFlag(AmmoType.F_ENCUMBERING) == cur_atype.hasFlag(AmmoType.F_ENCUMBERING))
            		&& (atype.getTonnage(entity) == cur_atype.getTonnage(entity))) {
                atypes.add(atype);
            }
        }
        return atypes;
    }

    public static boolean compareMounted (Mounted a, Mounted b) {
        if (!a.getType().equals(b.getType()))
            return false;
        if (!a.getClass().equals(b.getClass()))
            return false;
        if (!a.getName().equals(b.getName()))
            return false;
        if (a.getLocation()!=b.getLocation())
            return false;
        return true;
    }
    

	public static String getCurrencyString(long value) {
		NumberFormat numberFormat = DecimalFormat.getIntegerInstance();
		String text = numberFormat.format(value) + " C-Bills";
		return text;
	}
	
	/**
	 * Returns the last file modified in a directory and all subdirectories
	 * that conforms to a FilenameFilter 
	 * @param dir
	 * @param filter
	 * @return
	 */
	public static File lastFileModified(String dir, FilenameFilter filter) {
        File fl = new File(dir);
        File[] files = fl.listFiles(filter);
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
        	if (file.lastModified() > lastMod) {
        		choice = file;
        		lastMod = file.lastModified();
        	}
        }
        //ok now we need to recursively search any subdirectories, so see if they
        //contain more recent files
        files = fl.listFiles();
        for(File file : files) {
        	if(!file.isDirectory()) {
        		continue;
        	}
        	File subFile =  lastFileModified(file.getPath(), filter);
        	if (null != subFile && subFile.lastModified() > lastMod) {
        		choice = subFile;
        		lastMod = subFile.lastModified();
        	}
        }       
        return choice;
    }
	
	public static ArrayList<String> getAllVariants(Entity en, int year, CampaignOptions options) {
		ArrayList<String> variants = new ArrayList<String>();
		for(MechSummary summary : MechSummaryCache.getInstance().getAllMechs()) {
			if(!summary.isCanon() && options.allowCanonOnly()) {
				continue;
			}
			if(options.limitByYear() && summary.getYear() > year) {
				continue;
			}
			if(options.getTechLevel() < Utilities.getSimpleTechLevel(summary.getType())) {
				continue;
			}
			if(en.getChassis().equalsIgnoreCase(summary.getChassis())
					&& !en.getModel().equalsIgnoreCase(summary.getModel())
					&& summary.getTons() == en.getWeight()) {
				variants.add(summary.getModel());
			}
		}
		return variants;
	}
	
	public static int generateExpLevel(int bonus) {
		int roll = Compute.d6(2) + bonus;
		if(roll < 2) {
			return SkillType.EXP_ULTRA_GREEN;
		}
		if(roll < 6) {
			return SkillType.EXP_GREEN;
		}
		else if(roll < 10) {
			return SkillType.EXP_REGULAR;
		}
		else if(roll < 12) {
			return SkillType.EXP_VETERAN;
		}
		else {
			return SkillType.EXP_ELITE;
		}
	}
	
    public static Person findCommander(Entity entity, ArrayList<Person> vesselCrew, ArrayList<Person> gunners, ArrayList<Person> drivers, Person navigator) {
    	//take first by rank
    	//if rank is tied, take gunners over drivers
    	//if two of the same type are tie rank, take the first one
    	int bestRank = -1;
    	Person commander = null;
    	for(Person p : vesselCrew) {
    		if(null != p && p.getRank() > bestRank) {
    			commander = p;
    			bestRank = p.getRank();
    		}
    	}
    	for(Person p : gunners) {
    		if((entity instanceof Tank || entity instanceof Infantry) && p.getHits() > 0) { 
    			continue;
    		}
    		if(p.getRank() > bestRank) {
    			commander = p;
    			bestRank = p.getRank();
    		}
    	}
    	for(Person p : drivers) {
    		if((entity instanceof Tank || entity instanceof Infantry) && p.getHits() > 0) { 
    			continue;
    		}
    		if(p.getRank() > bestRank) {
    			commander = p;
    			bestRank = p.getRank();
    		}
    	}
    	if(navigator != null) {
    		if(null != navigator && navigator.getRank() > bestRank) {
    			commander = navigator;
    			bestRank = navigator.getRank();
    		}
    	}
    	return commander;
    }
	
	public static ArrayList<Person> generateRandomCrewWithCombinedSkill(Entity e, Campaign c) {
		ArrayList<Person> newCrew = new ArrayList<Person>();
		Unit unit = new Unit(e, c);
		Crew oldCrew = unit.getEntity().getCrew();
		String commanderName = oldCrew.getName();
		int averageGunnery = 0;
		int averagePiloting = 0;
		ArrayList<Person> drivers = new ArrayList<Person>();
		ArrayList<Person> gunners = new ArrayList<Person>();
		ArrayList<Person> vesselCrew = new ArrayList<Person>();
		Person navigator = null;
		int bonus = c.getFaction().isClan() ? 1 : 0;
		int expLvl;
		int totalGunnery = 0;
		int totalPiloting = 0;
		drivers.clear();
		gunners.clear();
		vesselCrew.clear();
		navigator = null;
		
		// Generate solo crews and drivers for multi-crewed vehicles.
		while(drivers.size() < unit.getTotalDriverNeeds()) {
    		Person p = null;
    		if(unit.getEntity() instanceof Mech) {
    			p = c.newPerson(Person.T_MECHWARRIOR);
    			while (p.getSkill(SkillType.S_PILOT_MECH).getFinalSkillValue() != oldCrew.getPiloting()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_MECHWARRIOR));
					p.addSkill(SkillType.S_PILOT_MECH, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    			while (p.getSkill(SkillType.S_GUN_MECH).getFinalSkillValue() != oldCrew.getGunnery()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_MECHWARRIOR));
					p.addSkill(SkillType.S_GUN_MECH, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    		}
    		else if(unit.getEntity() instanceof SmallCraft || unit.getEntity() instanceof Jumpship) {
    			p = c.newPerson(Person.T_SPACE_PILOT);
    			totalPiloting = p.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue();
    		}
    		else if(unit.getEntity() instanceof ConvFighter) {
    			p = c.newPerson(Person.T_CONV_PILOT);
    			while (p.getSkill(SkillType.S_PILOT_JET).getFinalSkillValue() != oldCrew.getPiloting()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_CONV_PILOT));
					p.addSkill(SkillType.S_PILOT_JET, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    			while (p.getSkill(SkillType.S_GUN_JET).getFinalSkillValue() != oldCrew.getGunnery()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_CONV_PILOT));
					p.addSkill(SkillType.S_GUN_JET, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    		}
    		else if(unit.getEntity() instanceof Aero) {
    			p = c.newPerson(Person.T_AERO_PILOT);
    			while (p.getSkill(SkillType.S_PILOT_AERO).getFinalSkillValue() != oldCrew.getPiloting()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_AERO_PILOT));
					p.addSkill(SkillType.S_PILOT_AERO, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    			while (p.getSkill(SkillType.S_GUN_AERO).getFinalSkillValue() != oldCrew.getGunnery()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_AERO_PILOT));
					p.addSkill(SkillType.S_GUN_AERO, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    		}
    		else if(unit.getEntity() instanceof VTOL) {
    			p = c.newPerson(Person.T_VTOL_PILOT);
    			while (p.getSkill(SkillType.S_PILOT_VTOL).getFinalSkillValue() != oldCrew.getPiloting()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_VTOL_PILOT));
					p.addSkill(SkillType.S_PILOT_VTOL, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    		}
    		else if(unit.getEntity() instanceof Tank) {
    			p = c.newPerson(Person.T_GVEE_DRIVER);
    			while (p.getSkill(SkillType.S_PILOT_GVEE).getFinalSkillValue() != oldCrew.getPiloting()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_GVEE_DRIVER));
					p.addSkill(SkillType.S_PILOT_GVEE, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    		}
    		else if(unit.getEntity() instanceof Protomech) {
    			p = c.newPerson(Person.T_PROTO_PILOT);
    			while (p.getSkill(SkillType.S_GUN_PROTO).getFinalSkillValue() != oldCrew.getGunnery()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_PROTO_PILOT));
					p.addSkill(SkillType.S_GUN_PROTO, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    		}
    		else if(unit.getEntity() instanceof BattleArmor) {
    			p = c.newPerson(Person.T_BA);
    			totalGunnery = p.getSkill(SkillType.S_GUN_BA).getFinalSkillValue();
    		}
    		else if(unit.getEntity() instanceof Infantry) {
    			p = c.newPerson(Person.T_INFANTRY);
    			totalGunnery = p.getSkill(SkillType.S_SMALL_ARMS).getFinalSkillValue();
    		}
    		drivers.add(p);
    	}
		if (drivers.size() != 0) {
			averageGunnery = (int)Math.round(((double)totalGunnery)/drivers.size());
			averagePiloting += (int)Math.round(((double)totalPiloting)/drivers.size());
			if (unit.getEntity() instanceof SmallCraft || unit.getEntity() instanceof Jumpship) {
				while (averagePiloting != oldCrew.getPiloting()) {
					totalPiloting = 0;
					for (Person p : drivers) {
						expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_SPACE_PILOT));
						p.addSkill(SkillType.S_PILOT_SPACE, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
						totalPiloting += p.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue();
					}
					averagePiloting += (int)Math.round(((double)totalPiloting)/drivers.size());
				}
			} else if (unit.getEntity() instanceof BattleArmor) {
				while (averageGunnery != oldCrew.getGunnery()) {
					totalGunnery = 0;
					for (Person p : drivers) {
						expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_BA));
						p.addSkill(SkillType.S_GUN_BA, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
						totalGunnery += p.getSkill(SkillType.S_GUN_BA).getFinalSkillValue();
					}
					averageGunnery = (int)Math.round(((double)totalGunnery)/drivers.size());
				}
			} else if (unit.getEntity() instanceof Infantry) {
				while (averageGunnery != oldCrew.getGunnery()) {
					totalGunnery = 0;
					for (Person p : drivers) {
						expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_INFANTRY));
						p.addSkill(SkillType.S_SMALL_ARMS, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
						totalGunnery += p.getSkill(SkillType.S_SMALL_ARMS).getFinalSkillValue();
					}
					averageGunnery = (int)Math.round(((double)totalGunnery)/drivers.size());
				}
			}
		}
		
    	while(gunners.size() < unit.getTotalGunnerNeeds()) {
    		Person p = null;
    		if (unit.getEntity() instanceof Tank) {
    			p = c.newPerson(Person.T_VEE_GUNNER);
    			totalGunnery += p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue();
    		} else if (unit.getEntity() instanceof SmallCraft || unit.getEntity() instanceof Jumpship) {
    			p = c.newPerson(Person.T_SPACE_GUNNER);
    			totalGunnery += p.getSkill(SkillType.S_GUN_SPACE).getFinalSkillValue();
    		}
    		gunners.add(p);
    	}
    	if (gunners.size() != 0) {
    		averageGunnery = (int)Math.round(((double)totalGunnery)/gunners.size());
    		if (unit.getEntity() instanceof Tank) {
				while (averageGunnery != oldCrew.getGunnery()) {
					totalGunnery = 0;
					for (Person p : gunners) {
						expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_VEE_GUNNER));
						p.addSkill(SkillType.S_GUN_VEE, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
						totalGunnery += p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue();
					}
					averageGunnery = (int)Math.round(((double)totalGunnery)/gunners.size());
				}
    		} else if (unit.getEntity() instanceof SmallCraft || unit.getEntity() instanceof Jumpship) {
				while (averageGunnery != oldCrew.getGunnery()) {
					totalGunnery = 0;
					for (Person p : gunners) {
						expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_SPACE_GUNNER));
						p.addSkill(SkillType.S_GUN_SPACE, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
						totalGunnery += p.getSkill(SkillType.S_GUN_SPACE).getFinalSkillValue();
					}
					averageGunnery = (int)Math.round(((double)totalGunnery)/gunners.size());
				}
    		}
    	}
    	
    	if(unit.canTakeNavigator()) {
    		Person p = c.newPerson(Person.T_NAVIGATOR);
    		navigator = p;
    	}
    	
    	findCommander(e, vesselCrew, gunners, drivers, navigator).setName(commanderName);
    	
    	// Add everyone to the crew
    	newCrew.addAll(drivers);
    	newCrew.addAll(gunners);
    	newCrew.addAll(vesselCrew);
    	newCrew.add(navigator);
		
		return newCrew;
	}
	
	public static void generateRandomCrewWithCombinedSkill(Unit unit, Campaign c) {
		Crew oldCrew = unit.getEntity().getCrew();
		String commanderName = oldCrew.getName();
		int averageGunnery = 0;
		int averagePiloting = 0;
		ArrayList<Person> drivers = new ArrayList<Person>();
		ArrayList<Person> gunners = new ArrayList<Person>();
		ArrayList<Person> vesselCrew = new ArrayList<Person>();
		Person navigator = null;
		int bonus = c.getFaction().isClan() ? 1 : 0;
		int expLvl;
		int totalGunnery = 0;
		int totalPiloting = 0;
		drivers.clear();
		gunners.clear();
		vesselCrew.clear();
		navigator = null;
		
		// Generate solo crews and drivers for multi-crewed vehicles.
		while(drivers.size() < unit.getTotalDriverNeeds()) {
    		Person p = null;
    		if(unit.getEntity() instanceof Mech) {
    			p = c.newPerson(Person.T_MECHWARRIOR);
    			while (p.getSkill(SkillType.S_PILOT_MECH).getFinalSkillValue() != oldCrew.getPiloting()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_MECHWARRIOR));
					p.addSkill(SkillType.S_PILOT_MECH, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    			while (p.getSkill(SkillType.S_GUN_MECH).getFinalSkillValue() != oldCrew.getGunnery()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_MECHWARRIOR));
					p.addSkill(SkillType.S_GUN_MECH, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    		}
    		else if(unit.getEntity() instanceof SmallCraft || unit.getEntity() instanceof Jumpship) {
    			p = c.newPerson(Person.T_SPACE_PILOT);
    			totalPiloting = p.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue();
    		}
    		else if(unit.getEntity() instanceof ConvFighter) {
    			p = c.newPerson(Person.T_CONV_PILOT);
    			while (p.getSkill(SkillType.S_PILOT_JET).getFinalSkillValue() != oldCrew.getPiloting()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_CONV_PILOT));
					p.addSkill(SkillType.S_PILOT_JET, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    			while (p.getSkill(SkillType.S_GUN_JET).getFinalSkillValue() != oldCrew.getGunnery()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_CONV_PILOT));
					p.addSkill(SkillType.S_GUN_JET, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    		}
    		else if(unit.getEntity() instanceof Aero) {
    			p = c.newPerson(Person.T_AERO_PILOT);
    			while (p.getSkill(SkillType.S_PILOT_AERO).getFinalSkillValue() != oldCrew.getPiloting()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_AERO_PILOT));
					p.addSkill(SkillType.S_PILOT_AERO, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    			while (p.getSkill(SkillType.S_GUN_AERO).getFinalSkillValue() != oldCrew.getGunnery()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_AERO_PILOT));
					p.addSkill(SkillType.S_GUN_AERO, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    		}
    		else if(unit.getEntity() instanceof VTOL) {
    			p = c.newPerson(Person.T_VTOL_PILOT);
    			while (p.getSkill(SkillType.S_PILOT_VTOL).getFinalSkillValue() != oldCrew.getPiloting()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_VTOL_PILOT));
					p.addSkill(SkillType.S_PILOT_VTOL, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    		}
    		else if(unit.getEntity() instanceof Tank) {
    			p = c.newPerson(Person.T_GVEE_DRIVER);
    			while (p.getSkill(SkillType.S_PILOT_GVEE).getFinalSkillValue() != oldCrew.getPiloting()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_GVEE_DRIVER));
					p.addSkill(SkillType.S_PILOT_GVEE, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    		}
    		else if(unit.getEntity() instanceof Protomech) {
    			p = c.newPerson(Person.T_PROTO_PILOT);
    			while (p.getSkill(SkillType.S_GUN_PROTO).getFinalSkillValue() != oldCrew.getGunnery()) {
    				expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_PROTO_PILOT));
					p.addSkill(SkillType.S_GUN_PROTO, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
    			}
    		}
    		else if(unit.getEntity() instanceof BattleArmor) {
    			p = c.newPerson(Person.T_BA);
    			totalGunnery = p.getSkill(SkillType.S_GUN_BA).getFinalSkillValue();
    		}
    		else if(unit.getEntity() instanceof Infantry) {
    			p = c.newPerson(Person.T_INFANTRY);
    			totalGunnery = p.getSkill(SkillType.S_SMALL_ARMS).getFinalSkillValue();
    		}
    		drivers.add(p);
    	}
		if (drivers.size() != 0) {
			averageGunnery = (int)Math.round(((double)totalGunnery)/drivers.size());
			averagePiloting += (int)Math.round(((double)totalPiloting)/drivers.size());
			if (unit.getEntity() instanceof SmallCraft || unit.getEntity() instanceof Jumpship) {
				while (averagePiloting != oldCrew.getPiloting()) {
					totalPiloting = 0;
					for (Person p : drivers) {
						expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_SPACE_PILOT));
						p.addSkill(SkillType.S_PILOT_SPACE, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
						totalPiloting += p.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue();
					}
					averagePiloting += (int)Math.round(((double)totalPiloting)/drivers.size());
				}
			} else if (unit.getEntity() instanceof BattleArmor) {
				while (averageGunnery != oldCrew.getGunnery()) {
					totalGunnery = 0;
					for (Person p : drivers) {
						expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_BA));
						p.addSkill(SkillType.S_GUN_BA, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
						totalGunnery += p.getSkill(SkillType.S_GUN_BA).getFinalSkillValue();
					}
					averageGunnery = (int)Math.round(((double)totalGunnery)/drivers.size());
				}
			} else if (unit.getEntity() instanceof Infantry) {
				while (averageGunnery != oldCrew.getGunnery()) {
					totalGunnery = 0;
					for (Person p : drivers) {
						expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_INFANTRY));
						p.addSkill(SkillType.S_SMALL_ARMS, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
						totalGunnery += p.getSkill(SkillType.S_SMALL_ARMS).getFinalSkillValue();
					}
					averageGunnery = (int)Math.round(((double)totalGunnery)/drivers.size());
				}
			}
		}
		
    	while(gunners.size() < unit.getTotalGunnerNeeds()) {
    		Person p = null;
    		if (unit.getEntity() instanceof Tank) {
    			p = c.newPerson(Person.T_VEE_GUNNER);
    			totalGunnery += p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue();
    		} else if (unit.getEntity() instanceof SmallCraft || unit.getEntity() instanceof Jumpship) {
    			p = c.newPerson(Person.T_SPACE_GUNNER);
    			totalGunnery += p.getSkill(SkillType.S_GUN_SPACE).getFinalSkillValue();
    		}
    		gunners.add(p);
    	}
    	if (gunners.size() != 0) {
    		averageGunnery = (int)Math.round(((double)totalGunnery)/gunners.size());
    		MekHQ.logMessage("Trying crew average gunnery of: "+averageGunnery+" for "+unit.getName()+" based on a total gunnery of: "+totalGunnery);
    		if (unit.getEntity() instanceof Tank) {
				while (averageGunnery != oldCrew.getGunnery()) {
					totalGunnery = 0;
					for (Person p : gunners) {
						expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_VEE_GUNNER));
						p.addSkill(SkillType.S_GUN_VEE, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
						totalGunnery += p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue();
					}
					averageGunnery = (int)Math.round(((double)totalGunnery)/gunners.size());
					MekHQ.logMessage("Trying crew average gunnery of: "+averageGunnery+" for "+unit.getName()+" based on a total gunnery of: "+totalGunnery);
				}
    		} else if (unit.getEntity() instanceof SmallCraft || unit.getEntity() instanceof Jumpship) {
				while (averageGunnery != oldCrew.getGunnery()) {
					totalGunnery = 0;
					for (Person p : gunners) {
						expLvl = Utilities.generateExpLevel(c.getRandomSkillPreferences().getOverallRecruitBonus() + c.getRandomSkillPreferences().getRecruitBonus(Person.T_SPACE_GUNNER));
						p.addSkill(SkillType.S_GUN_SPACE, expLvl, c.getRandomSkillPreferences().randomizeSkill(), bonus);
						totalGunnery += p.getSkill(SkillType.S_GUN_SPACE).getFinalSkillValue();
					}
					averageGunnery = (int)Math.round(((double)totalGunnery)/gunners.size());
				}
    		}
    	}
    	
    	
    	while(unit.canTakeMoreVesselCrew()) {
    		Person p = c.newPerson(Person.T_SPACE_CREW);
			if (!c.recruitPerson(p)) {
				return;
			}
    		vesselCrew.add(p);
    	}
    	
    	if(unit.canTakeNavigator()) {
    		Person p = c.newPerson(Person.T_NAVIGATOR);
    		navigator = p;
    	}
		
		for (Person p : drivers) {
			if (!c.recruitPerson(p)) {
				return;
			}
			if(unit.usesSoloPilot() || unit.usesSoldiers()) {
				unit.addPilotOrSoldier(p);
			} else {
				unit.addDriver(p);
			}
		}
		
		for (Person p : gunners) {
			if (!c.recruitPerson(p)) {
				return;
			}
			if (!(unit.usesSoloPilot() || unit.usesSoldiers()) && unit.canTakeMoreGunners()) {
				unit.addGunner(p);
			}
		}
		
		for (Person p : vesselCrew) {
			if (!c.recruitPerson(p)) {
				return;
			}
			if (!(unit.usesSoloPilot() || unit.usesSoldiers()) && unit.canTakeMoreVesselCrew()) {
				unit.addVesselCrew(p);
			}
		}
		if (navigator != null & unit.canTakeNavigator()) {
			if (!c.recruitPerson(navigator)) {
				return;
			}
			unit.setNavigator(navigator);
		}
		unit.getCommander().setName(commanderName);
		unit.resetPilotAndEntity();
	}

	public static int generateRandomExp() {
		int roll = Compute.randomInt(100);
		if (roll < 20) { // 20% chance of a randomized xp
			return (Compute.randomInt(8) + 1);
		} else if (roll < 40) { // 20% chance of 3 xp
			return 3;
		} else if (roll < 60) { // 20% chance of 2 xp
			return 2;
		} else if (roll < 80) { // 20% chance of 1 xp
			return 1;
		}
		return 0; // 20% chance of no xp
	}
	
	public static int rollSpecialAbilities(int bonus) {
		int roll = Compute.d6(2) + bonus;
		if(roll < 10) {
			return 0;
		}
		else if(roll < 12) {
			return 1;
		}
		else {
			return 2;
		}
	}
	
	public static boolean rollProbability(int prob) {
		return Compute.randomInt(100) <= prob;
	}
	
	public static int getAgeByExpLevel(int expLevel) {
		int baseage = 19;
		int ndice = 1;
		switch(expLevel) {
		case(SkillType.EXP_REGULAR):
			ndice = 2;
			break;
		case(SkillType.EXP_VETERAN):
			ndice = 3;
			break;
		case(SkillType.EXP_ELITE):
			ndice = 4;
			break;
		}
		
		int age = baseage;
		while(ndice > 0) {
			int roll = Compute.d6();
			age += roll;
			//reroll all sixes once
			if(roll == 6) {
				age += (Compute.d6()-1);
			}
			ndice--;
		}
		return age;
	}
	
	public static String getOptionDisplayName(IOption option) {
		String name = option.getDisplayableNameWithValue();
		name = name.replaceAll("\\(.+?\\)", "");
		if(option.getType() == IOption.CHOICE) {
			name += " - " + option.getValue();
		}
		return name;
	}
	
	public static String chooseWeaponSpecialization(int type, boolean isClan, int techLvl, int year) {
		ArrayList<String> candidates = new ArrayList<String>();
		for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if(!(et instanceof WeaponType)) {
            	continue;
            }
            if(et instanceof InfantryWeapon 
            		|| et instanceof BayWeapon
					|| et instanceof InfantryAttack) {
            	continue;
            }
            WeaponType wt = (WeaponType)et;
            if(wt.isCapital() 
            		|| wt.isSubCapital() 
            		|| wt.hasFlag(WeaponType.F_INFANTRY)
            		|| wt.hasFlag(WeaponType.F_ONESHOT)
            		|| wt.hasFlag(WeaponType.F_PROTOTYPE)) {
            	continue;
            }
            if(!((wt.hasFlag(WeaponType.F_MECH_WEAPON) && type == Person.T_MECHWARRIOR) 
            		|| (wt.hasFlag(WeaponType.F_AERO_WEAPON) && type != Person.T_AERO_PILOT)
            		|| (wt.hasFlag(WeaponType.F_TANK_WEAPON) && !(type == Person.T_VEE_GUNNER 
                    		|| type == Person.T_NVEE_DRIVER 
                    		|| type == Person.T_GVEE_DRIVER 
                    		|| type == Person.T_VTOL_PILOT))
                    || (wt.hasFlag(WeaponType.F_BA_WEAPON) && type != Person.T_BA)
                    || (wt.hasFlag(WeaponType.F_PROTO_WEAPON) && type != Person.T_PROTO_PILOT))) {
            	continue;
            }
            if(wt.getAtClass() == WeaponType.CLASS_NONE ||
            		wt.getAtClass() == WeaponType.CLASS_POINT_DEFENSE ||
            		wt.getAtClass() >= WeaponType.CLASS_CAPITAL_LASER) {
            	continue;
            }
            if(TechConstants.isClan(wt.getTechLevel(year)) != isClan) {
            	continue;
            }
            int lvl = wt.getTechLevel(year);
            if(lvl < 0) {
            	continue;
            }
            if(techLvl < Utilities.getSimpleTechLevel(lvl)) {
            	continue;
            }          
            if(techLvl == TechConstants.T_IS_UNOFFICIAL) {
            	continue;
            }
            int ntimes = 10;
            if(techLvl >= TechConstants.T_IS_ADVANCED) {
            	ntimes = 1;
            }
            while(ntimes > 0) {
            	candidates.add(et.getName());
            	ntimes--;
            }
		}
		if(candidates.isEmpty()) {
			return "??";
		}
		return candidates.get(Compute.randomInt(candidates.size()));
	}
	
	public static String printIntegerArray(int[] array) {
		String values = "";
		for(int i = 0; i < array.length; i++) {
			values += Integer.toString(array[i]);
			if(i < (array.length-1)) {
				values += ",";
			}
		}
		return values;
	}
	
	public static String printBooleanArray(boolean[] array) {
		String values = "";
		for(int i = 0; i < array.length; i++) {
			values += Boolean.toString(array[i]);
			if(i < (array.length-1)) {
				values += ",";
			}
		}
		return values;
	}
	
	public static int getSimpleTechLevel(int level) {
	    switch(level) {
	    case TechConstants.T_ALLOWED_ALL:
	    case TechConstants.T_INTRO_BOXSET:
	        return CampaignOptions.TECH_INTRO;
	    case TechConstants.T_IS_TW_NON_BOX:
	    case TechConstants.T_CLAN_TW:
	    case TechConstants.T_IS_TW_ALL:
        case TechConstants.T_TW_ALL:
            return CampaignOptions.TECH_STANDARD;
        case TechConstants.T_IS_ADVANCED:
        case TechConstants.T_CLAN_ADVANCED:
            return CampaignOptions.TECH_ADVANCED;
        case TechConstants.T_IS_EXPERIMENTAL:
        case TechConstants.T_CLAN_EXPERIMENTAL:
            return CampaignOptions.TECH_EXPERIMENTAL;
        case TechConstants.T_IS_UNOFFICIAL:
        case TechConstants.T_CLAN_UNOFFICIAL:
            return CampaignOptions.TECH_UNOFFICIAL;
        default:
            return CampaignOptions.TECH_INTRO;
	    }
	}
	
	//copied from http://www.roseindia.net/java/beginners/copyfile.shtml
	public static void copyfile(File inFile, File outFile){
		try{
			InputStream in = new FileInputStream(inFile);
		  
			//For Append the file.
			//  OutputStream out = new FileOutputStream(f2,true);

			//For Overwrite the file.
			OutputStream out = new FileOutputStream(outFile);

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0){
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
			System.out.println("File copied.");
		}
		catch(FileNotFoundException ex){
			System.out.println(ex.getMessage() + " in the specified directory.");
		}
		catch(IOException e){
			System.out.println(e.getMessage());  
		}
	}
	
	public static void unscrambleEquipmentNumbers(Unit unit) {
        ArrayList<Integer> equipNums = new ArrayList<Integer>();
        for(Mounted m : unit.getEntity().getEquipment()) {
            equipNums.add(unit.getEntity().getEquipmentNum(m));
        }
        for(Part part : unit.getParts()) {
            if(part instanceof AmmoBin) {
                AmmoBin bin = (AmmoBin)part;
                int i = -1;
                boolean found = false;
                for(int equipNum : equipNums) {
                    i++;
                    Mounted m = unit.getEntity().getEquipment(equipNum);
                    if(!(m.getType() instanceof AmmoType)) {
                        continue;
                    }
                    if(m.getType().getInternalName().equals(bin.getType().getInternalName())
                            && ((AmmoType)m.getType()).getMunitionType() == bin.getMunitionType()
                            && !m.isDestroyed()) {
                        bin.setEquipmentNum(equipNum);
                        found = true;
                        break;
                    }
                }
                if(found) {
                    equipNums.remove(i);
                }
            }
            else if(part instanceof EquipmentPart) {
                EquipmentPart epart = (EquipmentPart)part;
                int i = -1;
                boolean found = false;
                for(int equipNum : equipNums) {
                    i++;
                    Mounted m = unit.getEntity().getEquipment(equipNum);
                    if(m.getType() instanceof AmmoType) {
                        continue;
                    }
                    if(m.getType().getInternalName().equals(epart.getType().getInternalName())
                            && !m.isDestroyed()) {
                        epart.setEquipmentNum(equipNum);
                        found = true;
                        break;
                    }
                }
                if(found) {
                    equipNums.remove(i);
                }
            }
            else if(part instanceof MissingEquipmentPart) {
                MissingEquipmentPart epart = (MissingEquipmentPart)part;
                int i = -1;
                boolean found = false;
                for(int equipNum : equipNums) {
                    i++;
                    Mounted m = unit.getEntity().getEquipment(equipNum);
                    if(m.getType().getInternalName().equals(epart.getType().getInternalName())
                            && m.isDestroyed()) {
                        epart.setEquipmentNum(equipNum);
                        found = true;
                        break;
                    }
                }
                if(found) {
                    equipNums.remove(i);
                }
            }
        }
    }
	
	public static int getDaysBetween(Date date1, Date date2) {
	    return (int) ((date2.getTime() - date1.getTime()) / MILLISECONDS_IN_DAY );
	}
	
    /**
     * Calculates the number of days between start and end dates, taking
     * into consideration leap years, year boundaries etc.
     *
     * @param start the start date
     * @param end the end date, must be later than the start date
     * @return the number of days between the start and end dates
     */
    public static long countDaysBetween(Date start, Date end) {
        if (end.before(start)) {
            throw new IllegalArgumentException("The end date must be later than the start date");
        }

        //reset all hours mins and secs to zero on start date
        Calendar startCal = GregorianCalendar.getInstance();
        startCal.setTime(start);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        long startTime = startCal.getTimeInMillis();

        //reset all hours mins and secs to zero on end date
        Calendar endCal = GregorianCalendar.getInstance();
        endCal.setTime(end);
        endCal.set(Calendar.HOUR_OF_DAY, 0);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.SECOND, 0);
        long endTime = endCal.getTimeInMillis();

        return (endTime - startTime) / MILLISECONDS_IN_DAY;
    }
	
	public static int getDiffFullYears(Date date, GregorianCalendar b) {
	    GregorianCalendar a = new GregorianCalendar();
	    a.setTime(date);
	    int diff = b.get(GregorianCalendar.YEAR) - a.get(GregorianCalendar.YEAR);
	    if (a.get(GregorianCalendar.MONTH) > b.get(GregorianCalendar.MONTH) || 
	        (a.get(GregorianCalendar.MONTH) == b.get(GregorianCalendar.MONTH) && a.get(GregorianCalendar.DATE) > b.get(GregorianCalendar.DATE))) {
	        diff--;
	    }
	    return diff;
	}
	
	public static int getDiffPartialYears(Date date, GregorianCalendar b) {
	    GregorianCalendar a = new GregorianCalendar();
	    a.setTime(date);
	    int diff = b.get(GregorianCalendar.YEAR) - a.get(GregorianCalendar.YEAR);
	    if (diff == 0 && countDaysBetween(a.getTime(), b.getTime()) > 0) {
	    	return 1;
	    }
	    return diff;
	}
}