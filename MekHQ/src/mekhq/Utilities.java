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
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.ConvFighter;
import megamek.common.Coords;
import megamek.common.Crew;
import megamek.common.Entity;
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
import megamek.common.options.IOption;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingAmmoBin;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Utilities {
	private static final int MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;

	// A couple of arrays for use in the getLevelName() method
    private static int[]    arabicNumbers = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static String[] romanNumerals = "M,CM,D,CD,C,XC,L,XL,X,IX,V,IV,I".split(","); //$NON-NLS-1$ //$NON-NLS-2$

    public static int roll3d6() {
        Vector<Integer> rolls = new Vector<Integer>();
        rolls.add(Compute.d6());
        rolls.add(Compute.d6());
        rolls.add(Compute.d6());
        Collections.sort(rolls);
        return (rolls.elementAt(0) + rolls.elementAt(1));
    }

    /*
     * Roll a certain number of dice with a certain number of faces
     */
    public static int dice(int num, int faces) {
    	int result = 0;

    	// Roll however many dice as necessary
    	for (int i = 0; i < num; i++) {
            result += Compute.randomInt(faces) + 1;
        }

    	return result;
    }

    /**
     * Get a random element out of a collection, with equal probability.
     * <p>
     * This is the same as calling the following code, only plays nicely with
     * all collections (including ones like Set which don't implement RandomAccess)
     * and deals gracefully with empty collections.
     * <pre>
     * collection.get(Compute.randomInt(collection.size());
     * </pre>
     * 
     * @return <i>null</i> if the collection itself is null or empty;
     * can return <i>null</i> if the collection contains <i>null</i> items.
     * 
     */
    public static <T> T getRandomItem(Collection<? extends T> collection) {
        if((null == collection) || collection.isEmpty()) {
            return null;
        }
        int index = Compute.randomInt(collection.size());
        Iterator<? extends T> iterator = collection.iterator();
        for(int i = 0; i < index; ++ i) {
            iterator.next();
        }
        return iterator.next();
    }

    /**
     * Get a random element out of a list, with equal probability.
     * <p>
     * This is the same as calling the following code,
     * only deals gracefully with empty lists.
     * <pre>
     * list.get(Compute.randomInt(list.size());
     * </pre>
     * 
     * @return <i>null</i> if the list itself is null or empty;
     * can return <i>null</i> if the list contains <i>null</i> items.
     * 
     */
    public static <T> T getRandomItem(List<? extends T> list) {
        if((null == list) || list.isEmpty() ) {
            return null;
        }
        int index = Compute.randomInt(list.size());
        return list.get(index);
    }
    
    /**
     * @return linear interpolation value between min and max
     */
    public static double lerp(double min, double max, double f) {
        // The order of operations is important here, to not lose precision
        return min * (1.0 - f) + max * f;
    }
    
    /**
     * @return linear interpolation value between min and max, rounded to the nearest integer
     */
    public static int lerp(int min, int max, double f) {
        // The order of operations is important here, to not lose precision
        return (int)Math.round(min * (1.0 - f) + max * f);
    }
    
    /**
     * @return linear interpolation value between min and max, rounded to the nearest coordinate
     * <p>
     * For theory behind the method used, see: http://www.redblobgames.com/grids/hexagons/
     */
    public static Coords lerp(Coords min, Coords max, double f) {
    	int minX = min.getX();
    	int minZ = min.getY() - (min.getX() - (min.getX() & 1)) / 2;
    	int minY = - minX - minZ;
    	int maxX = max.getX();
    	int maxZ = max.getY() - (max.getX() - (max.getX() & 1)) / 2;
    	int maxY = - maxX - maxZ;
    	double lerpX = lerp((double)minX, (double)maxX, f);
    	double lerpY = lerp((double)minY, (double)maxY, f);
    	double lerpZ = lerp((double)minZ, (double)maxZ, f);
    	int resultX = (int) Math.round(lerpX);
    	int resultY = (int) Math.round(lerpY);
    	int resultZ = (int) Math.round(lerpZ);
    	double diffX = Math.abs(resultX * 1.0 - lerpX);
    	double diffY = Math.abs(resultY * 1.0 - lerpY);
    	double diffZ = Math.abs(resultZ * 1.0 - lerpZ);
    	if((diffX > diffY) && (diffX > diffZ)) {
    		resultX = - resultY - resultZ;
    	} else if(diffY > diffZ) {
    		resultY = - resultX - resultZ;
    	} else {
    		resultZ = - resultX - resultY;
    	}
    	return new Coords(resultX, resultZ + (resultX - (resultX & 1)) / 2);
    }

    /**
     * The method is returns the same as a call to the following code:
     * <pre>T result = (null != getFirst()) ? getFirst() : getSecond();</pre>
     * ... with the major difference that getFirst() and getSecond() get evaluated exactly once.
     * <p>
     * This means that it doesn't matter if getFirst() is relatively expensive to evaluate
     * or has side effects. It also means that getSecond() gets evaluated <i>regardless</i> if
     * it is needed or not. Since Java guarantees the order of evaluation for arguments to be
     * the same as the order in which they appear (JSR 15.7.4), this makes it more suitable
     * for re-playable procedural generation and similar method calls with side effects.
     * 
     * @return the first argument if it's not <i>null</i>, else the second argument
     */
    public static <T> T nonNull(T first, T second) {
        return (null != first) ? first : second;
    }
    
    /**
     * For details and caveats, see the two-argument method.
     * 
     * @return the first non-<i>null</i> argument, else <i>null</i> if all are <i>null</i>
     */
    @SafeVarargs
    public static <T> T nonNull(T first, T second, T ... others) {
        if(null != first) {
            return first;
        }
        if(null != second) {
            return second;
        }
        T result = others[0];
        int index = 1;
        while((null == result) && (index < others.length)) {
            result = others[index];
            ++ index;
        }
        return result;
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

	public static File[] getAllFiles(String dir, FilenameFilter filter) {
		File fl = new File(dir);
        File[] files = fl.listFiles(filter);
        return files;
	}

	public static ArrayList<String> getAllVariants(Entity en, int year, CampaignOptions options) {
		ArrayList<String> variants = new ArrayList<String>();
		for(MechSummary summary : MechSummaryCache.getInstance().getAllMechs()) {
			// If this isn't the same chassis, is our current unit, or is a different weight we continue
			if(!en.getChassis().equalsIgnoreCase(summary.getChassis())
					|| en.getModel().equalsIgnoreCase(summary.getModel())
					|| summary.getTons() != en.getWeight()) {
				continue;
			}
			// If we only allow canon units and this isn't canon we continue
			if(!summary.isCanon() && options.allowCanonOnly()) {
				continue;
			}
			// If we're limiting by year and aren't to this unit's year yet we continue
			if(options.limitByYear() && summary.getYear() > year) {
				continue;
			}
			// If the tech level doesn't meet the game's tech level we continue
			if(options.getTechLevel() < Utilities.getSimpleTechLevel(summary.getType())) {
				continue;
			}
			// Otherwise, we can offer it for selection
			variants.add(summary.getModel());
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
    		if(null != p && p.getRankNumeric() > bestRank) {
    			commander = p;
    			bestRank = p.getRankNumeric();
    		}
    	}
    	for(Person p : gunners) {
    		if(p.getRankNumeric() > bestRank) {
    			commander = p;
    			bestRank = p.getRankNumeric();
    		}
    	}
    	for(Person p : drivers) {
    		if(null != p && p.getRankNumeric() > bestRank) {
    			commander = p;
    			bestRank = p.getRankNumeric();
    		}
    	}
    	if(navigator != null) {
    		if(null != navigator && navigator.getRankNumeric() > bestRank) {
    			commander = navigator;
    			bestRank = navigator.getRankNumeric();
    		}
    	}
    	return commander;
    }

    /*
     * Simple utility function to take a specified number and randomize it a little bit
     * roll 1d6 results in:
     * 1: target - 2
     * 2: target - 1
     * 3 & 4: target
     * 5: target + 1
     * 6: target + 2
     */
    public static int randomSkillFromTarget(int target) {
    	int dice = Compute.d6();
    	if (dice == 1) {
    		target -= 2;
    	} else if (dice == 2) {
    		target -= 1;
    	} else if (dice == 5) {
    		target += 1;
    	} else if (dice == 6) {
    		target += 2;
    	}
    	return Math.max(target, 0);
    }

    /*
     * If an infantry platoon or vehicle crew took damage, perform the personnel injuries
     */
    public static ArrayList<Person> doCrewInjuries(Entity e, Campaign c, ArrayList<Person> newCrew) {
    	int casualties = 0;
		if(null != e && e instanceof Infantry) {
			e.applyDamage();
			casualties = newCrew.size() - ((Infantry)e).getShootingStrength();
			for (Person p : newCrew) {
				for (int i = 0; i < casualties; i++) {
					if(Compute.d6(2) >= 7) {
						int hits = c.getCampaignOptions().getMinimumHitsForVees();
					    if (c.getCampaignOptions().useAdvancedMedical() || c.getCampaignOptions().useRandomHitsForVees()) {
					        int range = 6 - hits;
	                        hits = hits + Compute.randomInt(range);
	                    }
	                    p.setHits(hits);
					} else {
						p.setHits(6);
					}
				}
			}
		}

		return newCrew;
    }

    public static boolean isDeadCrew(Entity e) {
		if (Compute.getFullCrewSize(e) == 0 || e.getCrew().isDead()) {
			return true;
		}

		return false;
    }

	public static ArrayList<Person> generateRandomCrewWithCombinedSkill(Unit unit, Campaign c, boolean addToUnit) {
        ArrayList<Person> newCrew = new ArrayList<Person>();
        Crew oldCrew = unit.getEntity().getCrew();
		String commanderName = oldCrew.getName();
		int averageGunnery = 0;
		int averagePiloting = 0;
		ArrayList<Person> drivers = new ArrayList<Person>();
		ArrayList<Person> gunners = new ArrayList<Person>();
		ArrayList<Person> vesselCrew = new ArrayList<Person>();
		Person navigator = null;
		int totalGunnery = 0;
		int totalPiloting = 0;
		drivers.clear();
		gunners.clear();
		vesselCrew.clear();
		navigator = null;

		// If the entire crew is dead, we don't want to generate them.
		// Actually, we do because they might not be truly dead - this will be the case for BA for example
		// Also, the user may choose to GM make them un-dead in the resolve scenario dialog. I am disabling
		// this because it is causing problems for BA.
		/*if (isDeadCrew(unit.getEntity())) {
			return new ArrayList<Person>();
		}*/

		// Generate solo crews
		if (unit.usesSoloPilot()) {
			Person p = null;
			if(unit.getEntity() instanceof Mech) {
    			p = c.newPerson(Person.T_MECHWARRIOR);
    			p.addSkill(SkillType.S_PILOT_MECH, SkillType.getType(SkillType.S_PILOT_MECH).getTarget() - oldCrew.getPiloting(), 0);
    			p.addSkill(SkillType.S_GUN_MECH, SkillType.getType(SkillType.S_GUN_MECH).getTarget() - oldCrew.getGunnery(), 0);
    		}
    		else if(unit.getEntity() instanceof Aero) {
    			p = c.newPerson(Person.T_AERO_PILOT);
    			p.addSkill(SkillType.S_PILOT_AERO, SkillType.getType(SkillType.S_PILOT_AERO).getTarget() - oldCrew.getPiloting(), 0);
    			p.addSkill(SkillType.S_GUN_AERO, SkillType.getType(SkillType.S_GUN_AERO).getTarget() - oldCrew.getGunnery(), 0);
    		}
    		else if(unit.getEntity() instanceof ConvFighter) {
    			p = c.newPerson(Person.T_CONV_PILOT);
    			p.addSkill(SkillType.S_PILOT_JET, SkillType.getType(SkillType.S_PILOT_JET).getTarget() - oldCrew.getPiloting(), 0);
    			p.addSkill(SkillType.S_GUN_JET, SkillType.getType(SkillType.S_GUN_JET).getTarget() - oldCrew.getPiloting(), 0);
    		}
    		else if(unit.getEntity() instanceof Protomech) {
    			p = c.newPerson(Person.T_PROTO_PILOT);
    			//p.addSkill(SkillType.S_PILOT_PROTO, SkillType.getType(SkillType.S_PILOT_PROTO).getTarget() - oldCrew.getPiloting(), 0);
    			p.addSkill(SkillType.S_GUN_PROTO, SkillType.getType(SkillType.S_GUN_PROTO).getTarget() - oldCrew.getGunnery(), 0);
    		}
    		else if(unit.getEntity() instanceof VTOL) {
    			p = c.newPerson(Person.T_VTOL_PILOT);
    			p.addSkill(SkillType.S_PILOT_VTOL, SkillType.getType(SkillType.S_PILOT_VTOL).getTarget() - oldCrew.getPiloting(), 0);
    			p.addSkill(SkillType.S_GUN_VEE, SkillType.getType(SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery(), 0);
    		}
    		else {
    			//assume tanker if we got here
    			p = c.newPerson(Person.T_GVEE_DRIVER);
    			p.addSkill(SkillType.S_PILOT_GVEE, SkillType.getType(SkillType.S_PILOT_GVEE).getTarget() - oldCrew.getPiloting(), 0);
    			p.addSkill(SkillType.S_GUN_VEE, SkillType.getType(SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery(), 0);
    		}
			drivers.add(p);
		} else {
			// Generate drivers for multi-crewed vehicles.

			//Uggh, BA are a nightmare. The getTotalDriverNeeds will adjust for missing/destroyed suits
			//but we cant change that because lots of other stuff needs that to be right, so we will hack
			//it here to make it the starting squad size
			int driversNeeded  = unit.getTotalDriverNeeds();
			if(unit.getEntity() instanceof BattleArmor && !addToUnit) {
				driversNeeded = ((BattleArmor)unit.getEntity()).getSquadSize();
			}
			while(drivers.size() < driversNeeded) {
	    		Person p = null;
	    		if(unit.getEntity() instanceof SmallCraft || unit.getEntity() instanceof Jumpship) {
	    			p = c.newPerson(Person.T_SPACE_PILOT);
	    			p.addSkill(SkillType.S_PILOT_SPACE, randomSkillFromTarget(SkillType.getType(SkillType.S_PILOT_SPACE).getTarget() - oldCrew.getPiloting()), 0);
	    			totalPiloting += p.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue();
	    		}
	    		else if(unit.getEntity() instanceof BattleArmor) {
	    			p = c.newPerson(Person.T_BA);
	    			p.addSkill(SkillType.S_GUN_BA, randomSkillFromTarget(SkillType.getType(SkillType.S_GUN_BA).getTarget() - oldCrew.getGunnery()), 0);
	    			totalGunnery += p.getSkill(SkillType.S_GUN_BA).getFinalSkillValue();
	    		}
	    		else if(unit.getEntity() instanceof Infantry) {
	    			p = c.newPerson(Person.T_INFANTRY);
	    			p.addSkill(SkillType.S_SMALL_ARMS, randomSkillFromTarget(SkillType.getType(SkillType.S_SMALL_ARMS).getTarget() - oldCrew.getGunnery()), 0);
	    			totalGunnery += p.getSkill(SkillType.S_SMALL_ARMS).getFinalSkillValue();
	    		}
	    		else if(unit.getEntity() instanceof VTOL) {
	    			p = c.newPerson(Person.T_VTOL_PILOT);
	    			p.addSkill(SkillType.S_PILOT_VTOL, SkillType.getType(SkillType.S_PILOT_VTOL).getTarget() - oldCrew.getPiloting(), 0);
	    			p.addSkill(SkillType.S_GUN_VEE, SkillType.getType(SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery(), 0);
	    		}
	    		else {
	    			//assume tanker if we got here
	    			p = c.newPerson(Person.T_GVEE_DRIVER);
	    			p.addSkill(SkillType.S_PILOT_GVEE, SkillType.getType(SkillType.S_PILOT_GVEE).getTarget() - oldCrew.getPiloting(), 0);
	    			p.addSkill(SkillType.S_GUN_VEE, SkillType.getType(SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery(), 0);
	    		}
	    		drivers.add(p);
	    	}

			// Regenerate as needed to balance
			if (drivers.size() != 0) {
				averageGunnery = (int)Math.round(((double)totalGunnery)/drivers.size());
				averagePiloting = (int)Math.round(((double)totalPiloting)/drivers.size());
				if (unit.getEntity() instanceof SmallCraft || unit.getEntity() instanceof Jumpship) {
					while (averagePiloting != oldCrew.getPiloting()) {
						totalPiloting = 0;
						for (Person p : drivers) {
							p.addSkill(SkillType.S_PILOT_SPACE, randomSkillFromTarget(SkillType.getType(SkillType.S_PILOT_SPACE).getTarget() - oldCrew.getPiloting()), 0);
							totalPiloting += p.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue();
						}
						averagePiloting = (int)Math.round(((double)totalPiloting)/drivers.size());
					}
				} else if (unit.getEntity() instanceof BattleArmor) {
					while (averageGunnery != oldCrew.getGunnery()) {
						totalGunnery = 0;
						for (Person p : drivers) {
							p.addSkill(SkillType.S_GUN_BA, randomSkillFromTarget(SkillType.getType(SkillType.S_GUN_BA).getTarget() - oldCrew.getGunnery()), 0);
							totalGunnery += p.getSkill(SkillType.S_GUN_BA).getFinalSkillValue();
						}
						averageGunnery = (int)Math.round(((double)totalGunnery)/drivers.size());
					}
				} else if (unit.getEntity() instanceof Infantry) {
					while (averageGunnery != oldCrew.getGunnery()) {
						totalGunnery = 0;
						for (Person p : drivers) {
							p.addSkill(SkillType.S_SMALL_ARMS, randomSkillFromTarget(SkillType.getType(SkillType.S_SMALL_ARMS).getTarget() - oldCrew.getGunnery()), 0);
							totalGunnery += p.getSkill(SkillType.S_SMALL_ARMS).getFinalSkillValue();
						}
						averageGunnery = (int)Math.round(((double)totalGunnery)/drivers.size());
					}
				}
			}
			if(!unit.usesSoldiers()) {
				// Generate gunners for multi-crew vehicles
		    	while(gunners.size() < unit.getTotalGunnerNeeds()) {
		    		Person p = null;
		    		if (unit.getEntity() instanceof SmallCraft || unit.getEntity() instanceof Jumpship) {
		    			p = c.newPerson(Person.T_SPACE_GUNNER);
		    			p.addSkill(SkillType.S_GUN_SPACE, randomSkillFromTarget(SkillType.getType(SkillType.S_GUN_SPACE).getTarget() - oldCrew.getGunnery()), 0);
		    			totalGunnery += p.getSkill(SkillType.S_GUN_SPACE).getFinalSkillValue();
		    		} else {
		    			//assume tanker if we got here
		    			p = c.newPerson(Person.T_VEE_GUNNER);
		    			p.addSkill(SkillType.S_GUN_VEE, randomSkillFromTarget(SkillType.getType(SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery()), 0);
		    			totalGunnery += p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue();
		    		}
		    		gunners.add(p);
		    	}

		    	// Regenerate gunners as needed to balance
		    	if (gunners.size() != 0) {
		    		averageGunnery = (int)Math.round(((double)totalGunnery)/gunners.size());
		    		if (unit.getEntity() instanceof Tank) {
						while (averageGunnery != oldCrew.getGunnery()) {
							totalGunnery = 0;
							for (Person p : gunners) {
								p.addSkill(SkillType.S_GUN_VEE, randomSkillFromTarget(SkillType.getType(SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery()), 0);
								totalGunnery += p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue();
							}
							averageGunnery = (int)Math.round(((double)totalGunnery)/gunners.size());
						}
		    		} else if (unit.getEntity() instanceof SmallCraft || unit.getEntity() instanceof Jumpship) {
						while (averageGunnery != oldCrew.getGunnery()) {
							totalGunnery = 0;
							for (Person p : gunners) {
								p.addSkill(SkillType.S_GUN_SPACE, randomSkillFromTarget(SkillType.getType(SkillType.S_GUN_SPACE).getTarget() - oldCrew.getGunnery()), 0);
								totalGunnery += p.getSkill(SkillType.S_GUN_SPACE).getFinalSkillValue();
							}
							averageGunnery = (int)Math.round(((double)totalGunnery)/gunners.size());
						}
		    		}
		    	}
			}
		}

		boolean nameset = false;
    	while(vesselCrew.size() < unit.getTotalCrewNeeds()) {
    		Person p = c.newPerson(Person.T_SPACE_CREW);
    		if (!nameset) {
    		    p.setName(commanderName);
    		    nameset = true;
    		}
			vesselCrew.add(p);
    	}

    	if(unit.canTakeNavigator()) {
    		Person p = c.newPerson(Person.T_NAVIGATOR);
    		navigator = p;
    	}

		for (Person p : drivers) {
            if (!nameset) {
                p.setName(commanderName);
                nameset = true;
            }
            if(addToUnit) {
            	if(null == c.getPerson(p.getId())) {
            		c.recruitPerson(p);
            	}
				if(unit.usesSoloPilot() || unit.usesSoldiers()) {
					unit.addPilotOrSoldier(p);
				} else {
					unit.addDriver(p);
				}
            }
		}
		newCrew.addAll(drivers);

		for (Person p : gunners) {
            if (!nameset) {
                p.setName(commanderName);
                nameset = true;
            }
            if(addToUnit) {
				if (!(unit.usesSoloPilot() || unit.usesSoldiers()) && unit.canTakeMoreGunners()) {
	            	if(null == c.getPerson(p.getId())) {
	            		c.recruitPerson(p);
	            	}
					unit.addGunner(p);
				}
            }
		}
        newCrew.addAll(gunners);

		for (Person p : vesselCrew) {
            if (!nameset) {
                p.setName(commanderName);
                nameset = true;
            }
            if(addToUnit) {
				if (!(unit.usesSoloPilot() || unit.usesSoldiers()) && unit.canTakeMoreVesselCrew()) {
	            	if(null == c.getPerson(p.getId())) {
	            		c.recruitPerson(p);
	            	}
					unit.addVesselCrew(p);
				}
            }
		}
        newCrew.addAll(vesselCrew);

		if (navigator != null & unit.canTakeNavigator()) {
            if (!nameset) {
                navigator.setName(commanderName);
                nameset = true;
            }
			if(addToUnit) {
            	if(null == c.getPerson(navigator.getId())) {
            		c.recruitPerson(navigator);
            	}
				unit.setNavigator(navigator);
			}
	        newCrew.add(navigator);
		}
		if(addToUnit) {
			unit.resetPilotAndEntity();
		}
		return newCrew;
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

	public static int getAgeByExpLevel(int expLevel, boolean clan) {
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
			//reroll all sixes once
			if(roll == 6) {
				roll += (Compute.d6()-1);
			}
			if(clan) {
			    roll = (int)Math.ceil(roll/2.0);
			}
			age += roll;
			ndice--;
		}
		return age;
	}

	public static String getOptionDisplayName(IOption option) {
		String name = option.getDisplayableNameWithValue();
		name = name.replaceAll("\\(.+?\\)", ""); //$NON-NLS-1$ //$NON-NLS-2$
		if(option.getType() == IOption.CHOICE) {
			name += " - " + option.getValue(); //$NON-NLS-1$
		}
		return name;
	}

	public static String printIntegerArray(int[] array) {
		String values = ""; //$NON-NLS-1$
		for(int i = 0; i < array.length; i++) {
			values += Integer.toString(array[i]);
			if(i < (array.length-1)) {
				values += ","; //$NON-NLS-1$
			}
		}
		return values;
	}

	public static String printDoubleArray(double[] array) {
        String values = ""; //$NON-NLS-1$
        for(int i = 0; i < array.length; i++) {
            values += Double.toString(array[i]);
            if(i < (array.length-1)) {
                values += ","; //$NON-NLS-1$
            }
        }
        return values;
    }

	public static String printBooleanArray(boolean[] array) {
		String values = ""; //$NON-NLS-1$
		for(int i = 0; i < array.length; i++) {
			values += Boolean.toString(array[i]);
			if(i < (array.length-1)) {
				values += ","; //$NON-NLS-1$
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
        case TechConstants.T_TECH_UNKNOWN:
            return CampaignOptions.TECH_UNKNOWN;
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
			System.out.println("File copied."); //$NON-NLS-1$
		}
		catch(FileNotFoundException ex){
			System.out.println(ex.getMessage() + " in the specified directory."); //$NON-NLS-1$
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
            if(!(part instanceof EquipmentPart)) {
                continue;
            }
            EquipmentPart ep = ((EquipmentPart) part);
            Mounted m = unit.getEntity().getEquipment(ep.getEquipmentNum());
            //Taharqa: I am not sure what was supposed to go in here, but it doesn't actually
            //do anything at this point and it is producing an NPE on some refits, so I am
            //commenting it out
            //if (m.getType().getInternalName().equals(ep.getType().getInternalName())) {

            //}
            if(part instanceof AmmoBin) {
                AmmoBin bin = (AmmoBin)part;
                int i = -1;
                boolean found = false;
                for(int equipNum : equipNums) {
                    i++;
                    m = unit.getEntity().getEquipment(equipNum);
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
            else if(part instanceof MissingAmmoBin) {
                MissingAmmoBin bin = (MissingAmmoBin)part;
                int i = -1;
                boolean found = false;
                for(int equipNum : equipNums) {
                    i++;
                    m = unit.getEntity().getEquipment(equipNum);
                    if(!(m.getType() instanceof AmmoType)) {
                        continue;
                    }
                    if(m.getType().getInternalName().equals(bin.getType().getInternalName())
                            && m.isDestroyed()) {
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
                    m = unit.getEntity().getEquipment(equipNum);
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
                    m = unit.getEntity().getEquipment(equipNum);
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

	/**
	 * export a jtable to TSV
	 * code derived from:
	 * https://sites.google.com/site/teachmemrxymon/java/export-records-from-jtable-to-ms-excel
	 * @param table
	 * @param file
	 */
	public static void exportTabletoCSV(JTable table, File file){
	    try{
	        TableModel model = table.getModel();
	        FileWriter csv = new FileWriter(file);

	        for(int i = 0; i < model.getColumnCount(); i++) {
	            String s = model.getColumnName(i);
	            if(null == s) {
                    s = ""; //$NON-NLS-1$
                }
                if (s.contains("\"")) { //$NON-NLS-1$
                    s = s.replace("\"", "\"\""); //$NON-NLS-1$ //$NON-NLS-2$
                }
                s = "\"" + s + "\""; //$NON-NLS-1$ //$NON-NLS-2$
                csv.write(s+","); //$NON-NLS-1$
	        }
	        csv.write("\n"); //$NON-NLS-1$

	        for(int i=0; i< model.getRowCount(); i++) {
	            for(int j=0; j < model.getColumnCount(); j++) {
	                String s = model.getValueAt(i,j).toString();
	                if(null == s) {
	                    s = ""; //$NON-NLS-1$
	                }
	                if (s.contains("\"")) { //$NON-NLS-1$
	                    s = s.replace("\"", "\"\""); //$NON-NLS-1$ //$NON-NLS-2$
	                }
	                s = "\"" + s + "\""; //$NON-NLS-1$ //$NON-NLS-2$
	                csv.write(s+","); //$NON-NLS-1$
	            }
	            csv.write("\n"); //$NON-NLS-1$
	        }
	        csv.close();

	    }catch(IOException e){ System.out.println(e); }
	}

	public static Vector<String> splitString(String str, String sep) {
        StringTokenizer st = new StringTokenizer(str, sep);
        Vector<String> output = new Vector<String>();
        while(st.hasMoreTokens()) {
            output.add(st.nextToken());
        }
        return output;
    }

    public static String combineString(Collection<String> vec, String sep) {
        if((null == vec) || (null == sep)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for( String part : vec ) {
            if( first ) {
                first = false;
            } else {
                sb.append(sep);
            }
            sb.append(part);
        }
        return sb.toString();
    }

    public static String getRomanNumeralsFromArabicNumber(int level, boolean checkZero) {
    	// If we're 0, then we just return an empty string
    	if (checkZero && level == 0) {
    		return ""; //$NON-NLS-1$
    	}

    	// Roman numeral, prepended with a space for display purposes
    	String roman = " "; //$NON-NLS-1$
        int num = level+1;

        for (int i = 0; i < arabicNumbers.length; i++) {
			while (num > arabicNumbers[i]) {
				roman += romanNumerals[i];
				num -= arabicNumbers[i];
			}
        }

        return roman;
    }

    // TODO: Optionize this to allow user to choose roman or arabic numerals
    public static int getArabicNumberFromRomanNumerals(String name) {
    	// If we're 0, then we just return an empty string
    	if (name.equals("")) { //$NON-NLS-1$
    		return 0;
    	}

    	// Roman numeral, prepended with a space for display purposes
    	int arabic = 0;
        String roman = name;

        for (int i = 0; i < roman.length(); i++) {
        	int num = romanNumerals.toString().indexOf(roman.charAt(i));
        	if (i < roman.length()) {
        		int temp = romanNumerals.toString().indexOf(roman.charAt(i+1));
        		// If this is a larger number, then we need to combine them
        		if (temp > num) {
        			num = temp - num;
        			i++;
        		}
        	}

        	arabic += num;
        }

        return arabic-1;
    }

    public static Map<String, Integer> sortMapByValue(Map<String, Integer> unsortMap, boolean highFirst) {

		// Convert Map to List
		List<Map.Entry<String, Integer>> list =
			new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());

		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Map.Entry<String, Integer> o1,
                                           Map.Entry<String, Integer> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		// Convert sorted map back to a Map
		Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
		if(highFirst) {
			ListIterator<Map.Entry<String, Integer>> li = list.listIterator(list.size());
			while(li.hasPrevious()) {
				Map.Entry<String, Integer> entry = li.previous();
				sortedMap.put(entry.getKey(), entry.getValue());
			}
		} else {
			for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext();) {
				Map.Entry<String, Integer> entry = it.next();
				sortedMap.put(entry.getKey(), entry.getValue());
			}
		}

		return sortedMap;
	}

    public static boolean isLikelyCapture(Entity en) {
    	//most of these conditions are now controlled better in en.canEscape, but there
    	//are some additional ones we want to add
    	if(!en.canEscape()) {
    		return true;
    	}
    	return en.isDestroyed()
    	        || en.isDoomed()
    	        || en.isStalled()
    	        || en.isStuck();
    }

}