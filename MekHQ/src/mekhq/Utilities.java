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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.ConvFighter;
import megamek.common.Coords;
import megamek.common.Crew;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.LandAirMech;
import megamek.common.Mech;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import megamek.common.VTOL;
import megamek.common.logging.LogLevel;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.BattleArmorEquipmentPart;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.CrewType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTechProgression;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Utilities {
    private static final int MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;
    
    private static ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.Utilities", new EncodeControl()); //$NON-NLS-1$

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
                    && !atype.canAeroUse()) {
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
            
            if (atype.hasFlag(AmmoType.F_NUCLEAR) && atype.hasFlag(AmmoType.F_CAP_MISSILE)
                    && !entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AT2_NUKES)) {
                continue;
            }

            // Battle Armor ammo can't be selected at all.
            // All other ammo types need to match on rack size and tech.
            if ((atype.getRackSize() == cur_atype.getRackSize())
                    && (atype.hasFlag(AmmoType.F_BATTLEARMOR) == cur_atype.hasFlag(AmmoType.F_BATTLEARMOR))
                    && (atype.hasFlag(AmmoType.F_ENCUMBERING) == cur_atype.hasFlag(AmmoType.F_ENCUMBERING))
                    && ((atype.getTonnage(entity) == cur_atype.getTonnage(entity))
                            || atype.hasFlag(AmmoType.F_CAP_MISSILE))) {
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

    public static ArrayList<String> getAllVariants(Entity en, Campaign campaign) {
        final String METHOD_NAME = "getAllVariants(Entity, Campaign)"; // $NON-NLS-1$
        CampaignOptions options = campaign.getCampaignOptions();
        ArrayList<String> variants = new ArrayList<String>();
        for(MechSummary summary : MechSummaryCache.getInstance().getAllMechs()) {
            // If this isn't the same chassis, is our current unit, or is a different weight we continue
            if(!en.getChassis().equalsIgnoreCase(summary.getChassis())
                    || en.getModel().equalsIgnoreCase(summary.getModel())
                    || summary.getTons() != en.getWeight()
                    || !summary.getUnitType().equals(UnitType.determineUnitType(en))) {
                continue;
            }
            // If we only allow canon units and this isn't canon we continue
            if(!summary.isCanon() && options.allowCanonRefitOnly()) {
                continue;
            }
            // If the unit doesn't meet the tech filter criteria we continue
            ITechnology techProg = UnitTechProgression.getProgression(summary, campaign.getTechFaction(), true);
            if (null == techProg) {
                // This should never happen unless there was an exception thrown when calculating the progression.
                // In such a case we will log it and take the least restrictive action, which is to let it through.
                MekHQ.getLogger().log(Utilities.class, METHOD_NAME, LogLevel.WARNING,
                        "Could not determine tech progression for " + summary.getName() // $NON-NLS-1$
                        + ", including among available refits."); // $NON-NLS-1$
            } else if (!campaign.isLegal(techProg)) {
                continue;
            }
            // Otherwise, we can offer it for selection
            variants.add(summary.getModel());
        }
        return variants;
    }
    
    public static boolean isOmniVariant(Entity entity1, Entity entity2) {
        if (!entity1.isOmni() || !entity2.isOmni()) {
            return false;
        }
        if (entity1.getWeight() != entity2.getWeight()) {
            return false;
        }
        if (entity1.getClass() != entity2.getClass()) {
            return false;
        }
        if (entity1.getEngine().getRating() != entity2.getEngine().getRating()
                || entity1.getEngine().getEngineType() != entity2.getEngine().getEngineType()
                || entity1.getEngine().getFlags() != entity2.getEngine().getFlags()) {
            return false;
        }
        if (entity1.getStructureType() != entity2.getStructureType()) {
            return false;
        }
        if (entity1 instanceof Mech) {
            if (((Mech)entity1).getCockpitType() != ((Mech)entity2).getCockpitType()) {
                return false;
            }
            if (((Mech)entity1).getGyroType() != ((Mech)entity2).getGyroType()) {
                return false;
            }
        }
        if (entity1 instanceof Aero) {
            if (((Aero)entity1).getCockpitType() != ((Aero)entity2).getCockpitType()) {
                return false;
            }
        }
        if (entity1 instanceof Tank) {
            if (entity1.getMovementMode() != entity2.getMovementMode()) {
                return false;
            }
        }
        List<EquipmentType> fixedEquipment = new ArrayList<>();
        for (int loc = 0; loc < entity1.locations(); loc++) {
            if (entity1.getArmorType(loc) != entity2.getArmorType(loc)
                    || entity1.getOArmor(loc) != entity2.getOArmor(loc)) {
                return false;
            }
            fixedEquipment.clear();
            //Go through the base entity and make a list of all fixed equipment in this location.
            for (int slot = 0; slot < entity1.getNumberOfCriticals(loc); slot++) {
                CriticalSlot crit = entity1.getCritical(loc, slot);
                if (null != crit && crit.getType() == CriticalSlot.TYPE_EQUIPMENT && null != crit.getMount()) {
                    if (!crit.getMount().isOmniPodMounted()) {
                        fixedEquipment.add(crit.getMount().getType());
                        if (null != crit.getMount2()) {
                            fixedEquipment.add(crit.getMount2().getType());
                        }
                    }
                }
            }
            //Go through the critical slots in this location for the second entity and remove all fixed
            //equipment from the list. If not found or something is left over, there is a fixed equipment difference.
            for (int slot = 0; slot < entity2.getNumberOfCriticals(loc); slot++) {
                CriticalSlot crit = entity1.getCritical(loc, slot);
                if (null != crit && crit.getType() == CriticalSlot.TYPE_EQUIPMENT && null != crit.getMount()) {
                    if (!crit.getMount().isOmniPodMounted()) {
                        if (!fixedEquipment.remove(crit.getMount().getType())) {
                            return false;
                        }
                        if (null != crit.getMount2() && !fixedEquipment.remove(crit.getMount2().getType())) {
                            return false;
                        }
                    }
                }                
            }
            if (!fixedEquipment.isEmpty()) {
                return false;
            }
        }
        return true;
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
    
    public static Map<CrewType, Collection<Person>> genRandomCrewWithCombinedSkill(Campaign c, Unit u, String factionCode) {
        Objects.requireNonNull(c);
        Objects.requireNonNull(u);
        Objects.requireNonNull(u.getEntity(), "Unit needs to have a valid Entity attached");
        Crew oldCrew = u.getEntity().getCrew();
        String commanderName = oldCrew.getName();
        int averageGunnery = 0;
        int averagePiloting = 0;
        List<Person> drivers = new ArrayList<Person>();
        List<Person> gunners = new ArrayList<Person>();
        List<Person> vesselCrew = new ArrayList<Person>();
        Person navigator = null;
        Person consoleCmdr = null;
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
        if (u.usesSoloPilot()) {
            Person p = null;
            if (u.getEntity() instanceof LandAirMech) {
                p = c.newPerson(Person.T_MECHWARRIOR, factionCode);
                p.addSkill(SkillType.S_PILOT_MECH, SkillType.getType(SkillType.S_PILOT_MECH).getTarget() - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_MECH, SkillType.getType(SkillType.S_GUN_MECH).getTarget() - oldCrew.getGunnery(), 0);
                p.addSkill(SkillType.S_PILOT_AERO, SkillType.getType(SkillType.S_PILOT_AERO).getTarget() - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_AERO, SkillType.getType(SkillType.S_GUN_AERO).getTarget() - oldCrew.getGunnery(), 0);
                p.setSecondaryRole(Person.T_AERO_PILOT);
            } else if (u.getEntity() instanceof Mech) {
                p = c.newPerson(Person.T_MECHWARRIOR, factionCode);
                p.addSkill(SkillType.S_PILOT_MECH, SkillType.getType(SkillType.S_PILOT_MECH).getTarget() - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_MECH, SkillType.getType(SkillType.S_GUN_MECH).getTarget() - oldCrew.getGunnery(), 0);
            } else if (u.getEntity() instanceof Aero) {
                p = c.newPerson(Person.T_AERO_PILOT, factionCode);
                p.addSkill(SkillType.S_PILOT_AERO, SkillType.getType(SkillType.S_PILOT_AERO).getTarget() - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_AERO, SkillType.getType(SkillType.S_GUN_AERO).getTarget() - oldCrew.getGunnery(), 0);
            } else if (u.getEntity() instanceof ConvFighter) {
                p = c.newPerson(Person.T_CONV_PILOT, factionCode);
                p.addSkill(SkillType.S_PILOT_JET, SkillType.getType(SkillType.S_PILOT_JET).getTarget() - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_JET, SkillType.getType(SkillType.S_GUN_JET).getTarget() - oldCrew.getPiloting(), 0);
            } else if (u.getEntity() instanceof Protomech) {
                p = c.newPerson(Person.T_PROTO_PILOT, factionCode);
                //p.addSkill(SkillType.S_PILOT_PROTO, SkillType.getType(SkillType.S_PILOT_PROTO).getTarget() - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_PROTO, SkillType.getType(SkillType.S_GUN_PROTO).getTarget() - oldCrew.getGunnery(), 0);
            } else if (u.getEntity() instanceof VTOL) {
                p = c.newPerson(Person.T_VTOL_PILOT, factionCode);
                p.addSkill(SkillType.S_PILOT_VTOL, SkillType.getType(SkillType.S_PILOT_VTOL).getTarget() - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_VEE, SkillType.getType(SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery(), 0);
            } else {
                //assume tanker if we got here
                p = c.newPerson(Person.T_GVEE_DRIVER, factionCode);
                p.addSkill(SkillType.S_PILOT_GVEE, SkillType.getType(SkillType.S_PILOT_GVEE).getTarget() - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_VEE, SkillType.getType(SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery(), 0);
            }
            
            populateOptionsFromCrew(p, oldCrew);
            
            drivers.add(p);
        } else if (oldCrew.getSlotCount() > 1) {
            for (int slot = 0; slot < oldCrew.getSlotCount(); slot++) {
                Person p = null;
                if(u.getEntity() instanceof Mech) {
                    p = c.newPerson(Person.T_MECHWARRIOR, factionCode);
                    p.addSkill(SkillType.S_PILOT_MECH, SkillType.getType(SkillType.S_PILOT_MECH).getTarget() - oldCrew.getPiloting(slot), 0);
                    p.addSkill(SkillType.S_GUN_MECH, SkillType.getType(SkillType.S_GUN_MECH).getTarget() - oldCrew.getGunnery(slot), 0);
                } else if(u.getEntity() instanceof Aero) {
                    p = c.newPerson(Person.T_AERO_PILOT, factionCode);
                    p.addSkill(SkillType.S_PILOT_AERO, SkillType.getType(SkillType.S_PILOT_AERO).getTarget() - oldCrew.getPiloting(slot), 0);
                    p.addSkill(SkillType.S_GUN_AERO, SkillType.getType(SkillType.S_GUN_AERO).getTarget() - oldCrew.getGunnery(slot), 0);
                }
                if (null != p) {
                    p.setName(oldCrew.getName(slot));
                    if (!oldCrew.getExternalIdAsString().equals("-1")) {
                        p.setId(UUID.fromString(oldCrew.getExternalIdAsString(slot)));
                    }
                    
                    populateOptionsFromCrew(p, oldCrew);
                    drivers.add(p);
                }
            }
        } else {
            // Generate drivers for multi-crewed vehicles.

            //Uggh, BA are a nightmare. The getTotalDriverNeeds will adjust for missing/destroyed suits
            //but we can't change that because lots of other stuff needs that to be right, so we will hack
            //it here to make it the starting squad size
            int driversNeeded  = u.getTotalDriverNeeds();
            if(u.getEntity() instanceof BattleArmor) {
                driversNeeded = ((BattleArmor)u.getEntity()).getSquadSize();
            }
            while(drivers.size() < driversNeeded) {
                Person p = null;
                if(u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship) {
                    p = c.newPerson(Person.T_SPACE_PILOT, factionCode);
                    p.addSkill(SkillType.S_PILOT_SPACE, randomSkillFromTarget(SkillType.getType(SkillType.S_PILOT_SPACE).getTarget() - oldCrew.getPiloting()), 0);
                    totalPiloting += p.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue();
                }
                else if(u.getEntity() instanceof BattleArmor) {
                    p = c.newPerson(Person.T_BA, factionCode);
                    p.addSkill(SkillType.S_GUN_BA, randomSkillFromTarget(SkillType.getType(SkillType.S_GUN_BA).getTarget() - oldCrew.getGunnery()), 0);
                    totalGunnery += p.getSkill(SkillType.S_GUN_BA).getFinalSkillValue();
                }
                else if(u.getEntity() instanceof Infantry) {
                    p = c.newPerson(Person.T_INFANTRY, factionCode);
                    p.addSkill(SkillType.S_SMALL_ARMS, randomSkillFromTarget(SkillType.getType(SkillType.S_SMALL_ARMS).getTarget() - oldCrew.getGunnery()), 0);
                    totalGunnery += p.getSkill(SkillType.S_SMALL_ARMS).getFinalSkillValue();
                }
                else if(u.getEntity() instanceof VTOL) {
                    p = c.newPerson(Person.T_VTOL_PILOT, factionCode);
                    p.addSkill(SkillType.S_PILOT_VTOL, SkillType.getType(SkillType.S_PILOT_VTOL).getTarget() - oldCrew.getPiloting(), 0);
                    p.addSkill(SkillType.S_GUN_VEE, SkillType.getType(SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery(), 0);
                } else if (u.getEntity() instanceof Mech) {
                    p = c.newPerson(Person.T_MECHWARRIOR, factionCode);
                    p.addSkill(SkillType.S_PILOT_MECH, SkillType.getType(SkillType.S_PILOT_MECH).getTarget() - oldCrew.getPiloting(), 0);
                    p.addSkill(SkillType.S_GUN_MECH, SkillType.getType(SkillType.S_GUN_MECH).getTarget() - oldCrew.getGunnery(), 0);
                } else {
                    //assume tanker if we got here
                    p = c.newPerson(Person.T_GVEE_DRIVER, factionCode);
                    p.addSkill(SkillType.S_PILOT_GVEE, SkillType.getType(SkillType.S_PILOT_GVEE).getTarget() - oldCrew.getPiloting(), 0);
                    p.addSkill(SkillType.S_GUN_VEE, SkillType.getType(SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery(), 0);
                }
                
                // this will have the side effect of giving every driver on the crew
                // the SPAs from the entity's crew.
                // Not really any way around it 
                populateOptionsFromCrew(p, oldCrew);
                drivers.add(p);
            }

            // Regenerate as needed to balance
            if (drivers.size() != 0) {
                averageGunnery = (int)Math.round(((double)totalGunnery)/drivers.size());
                averagePiloting = (int)Math.round(((double)totalPiloting)/drivers.size());
                if (u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship) {
                    while (averagePiloting != oldCrew.getPiloting()) {
                        totalPiloting = 0;
                        for (Person p : drivers) {
                            p.addSkill(SkillType.S_PILOT_SPACE, randomSkillFromTarget(SkillType.getType(SkillType.S_PILOT_SPACE).getTarget() - oldCrew.getPiloting()), 0);
                            totalPiloting += p.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue();
                        }
                        averagePiloting = (int)Math.round(((double)totalPiloting)/drivers.size());
                    }
                } else if (u.getEntity() instanceof BattleArmor) {
                    while (averageGunnery != oldCrew.getGunnery()) {
                        totalGunnery = 0;
                        for (Person p : drivers) {
                            p.addSkill(SkillType.S_GUN_BA, randomSkillFromTarget(SkillType.getType(SkillType.S_GUN_BA).getTarget() - oldCrew.getGunnery()), 0);
                            totalGunnery += p.getSkill(SkillType.S_GUN_BA).getFinalSkillValue();
                        }
                        averageGunnery = (int)Math.round(((double)totalGunnery)/drivers.size());
                    }
                } else if (u.getEntity() instanceof Infantry) {
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
            if(!u.usesSoldiers()) {
                // Generate gunners for multi-crew vehicles
                while(gunners.size() < u.getTotalGunnerNeeds()) {
                    Person p = null;
                    if (u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship) {
                        p = c.newPerson(Person.T_SPACE_GUNNER, factionCode);
                        p.addSkill(SkillType.S_GUN_SPACE, randomSkillFromTarget(SkillType.getType(SkillType.S_GUN_SPACE).getTarget() - oldCrew.getGunnery()), 0);
                        totalGunnery += p.getSkill(SkillType.S_GUN_SPACE).getFinalSkillValue();
                    } else if (u.getEntity() instanceof Mech) {
                        p = c.newPerson(Person.T_MECHWARRIOR, factionCode);
                        p.addSkill(SkillType.S_PILOT_MECH, SkillType.getType(SkillType.S_PILOT_MECH).getTarget() - oldCrew.getPiloting(), 0);
                        p.addSkill(SkillType.S_GUN_MECH, SkillType.getType(SkillType.S_GUN_MECH).getTarget() - oldCrew.getGunnery(), 0);
                        totalGunnery += p.getSkill(SkillType.S_GUN_MECH).getFinalSkillValue();
                    } else {
                        //assume tanker if we got here
                        p = c.newPerson(Person.T_VEE_GUNNER, factionCode);
                        p.addSkill(SkillType.S_GUN_VEE, randomSkillFromTarget(SkillType.getType(SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery()), 0);
                        totalGunnery += p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue();
                    }
                    
                    populateOptionsFromCrew(p, oldCrew);
                    gunners.add(p);
                }

                // Regenerate gunners as needed to balance
                if (gunners.size() != 0) {
                    averageGunnery = (int)Math.round(((double)totalGunnery)/gunners.size());
                    if (u.getEntity() instanceof Tank) {
                        while (averageGunnery != oldCrew.getGunnery()) {
                            totalGunnery = 0;
                            for (Person p : gunners) {
                                p.addSkill(SkillType.S_GUN_VEE, randomSkillFromTarget(SkillType.getType(SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery()), 0);
                                totalGunnery += p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue();
                            }
                            averageGunnery = (int)Math.round(((double)totalGunnery)/gunners.size());
                        }
                    } else if (u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship) {
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
        //Multi-slot crews already have the names set. Single-slot multi-crew units need to assign the commander's name.
        boolean nameset = oldCrew.getSlotCount() > 1;
        while(vesselCrew.size() < u.getTotalCrewNeeds()) {
            Person p = c.newPerson(Person.T_SPACE_CREW, factionCode);
            if (!nameset) {
                p.setName(commanderName);
                nameset = true;
            }
            vesselCrew.add(p);
        }

        if(u.canTakeNavigator()) {
            Person p = c.newPerson(Person.T_NAVIGATOR, factionCode);
            navigator = p;
        }
        
        if (u.canTakeTechOfficer()) {
            Person p = c.newPerson(Person.T_VEE_GUNNER, factionCode);
            consoleCmdr = p;
        }
        
        for(Person p : drivers) {
            if (!nameset) {
                p.setName(commanderName);
                nameset = true;
            }
        }

        for(Person p : gunners) {
            if (!nameset) {
                p.setName(commanderName);
                nameset = true;
            }
        }

        for(Person p : vesselCrew) {
            if (!nameset) {
                p.setName(commanderName);
                nameset = true;
            }
        }

        if (null != navigator) {
            if (!nameset) {
                navigator.setName(commanderName);
                nameset = true;
            }
        }
        
        if (null != consoleCmdr) {
            if (!nameset) {
                consoleCmdr.setName(commanderName);
                nameset = true;
            }
        }
        
        // Gather the data
        Map<CrewType, Collection<Person>> result = new HashMap<>();
        if (!drivers.isEmpty()) {
            if(u.usesSoloPilot()) {
                result.put(CrewType.PILOT, drivers);
            } else if(u.usesSoldiers()) {
                result.put(CrewType.SOLDIER, drivers);
            } else {
                result.put(CrewType.DRIVER, drivers);
            }
        }
        if (!gunners.isEmpty()) {
            result.put(CrewType.GUNNER, gunners);
        }
        if (!vesselCrew.isEmpty()) {
            result.put(CrewType.VESSEL_CREW, vesselCrew);
        }
        if (null != navigator) {
            result.put(CrewType.NAVIGATOR, Collections.singletonList(navigator));
        }
        if (null != consoleCmdr) {
            result.put(CrewType.TECH_OFFICER, Collections.singletonList(consoleCmdr));
        }
        return result;
    }
    
    /**
     * Worker function that takes the PilotOptions (SPAs, in other words) from the given "old crew" and sets them for a person.
     * @param p The person whose SPAs to populate
     * @param oldCrew The entity the SPAs of whose crew we're importing
     */
    private static void populateOptionsFromCrew(Person p, Crew oldCrew) {
        Enumeration<IOption> optionsEnum = oldCrew.getOptions().getOptions();
        while(optionsEnum.hasMoreElements()) {
            IOption currentOption = (IOption) optionsEnum.nextElement();
            p.getOptions().getOption(currentOption.getName()).setValue(currentOption.getValue());
        }
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
        //BA has one part per equipment entry per suit and may need to have trooper fields set following
        //a refit
        if (unit.getEntity() instanceof BattleArmor) {
            assignTroopersAndEquipmentNums(unit);
            return;
        }
        List<Integer> equipNums = new ArrayList<Integer>();
        for(Mounted m : unit.getEntity().getEquipment()) {
            equipNums.add(unit.getEntity().getEquipmentNum(m));
        }
        List<Part> remaining = new ArrayList<>();
        List<Part> notFound = new ArrayList<>();
        for (Part part : unit.getParts()) {
            int eqnum = -1;
            EquipmentType etype = null;
            if (part instanceof EquipmentPart) {
                eqnum = ((EquipmentPart) part).getEquipmentNum();
                etype = ((EquipmentPart) part).getType();
            } else if (part instanceof MissingEquipmentPart) {
                eqnum = ((MissingEquipmentPart) part).getEquipmentNum();
                etype = ((MissingEquipmentPart) part).getType();
            }
            if (null != etype) {
                if (equipNums.contains(eqnum)
                        && etype.equals(unit.getEntity().getEquipment(eqnum).getType())) {
                    equipNums.remove(equipNums.indexOf(eqnum));
                } else {
                    remaining.add(part);
                }
            }
        }
        // For ammo types we want to match the same munition type if possible to avoid the possibility
        // imposing unnecessary ammo swaps.
        boolean allMunitions = false;
        Mounted m;
        while ((remaining.size() > 0) && !allMunitions) {
            for(Part part : remaining) {
                if (part instanceof EquipmentPart) {
                    EquipmentPart epart = (EquipmentPart)part;
                    int i = -1;
                    boolean found = false;
                    for (int equipNum : equipNums) {
                        i++;
                        m = unit.getEntity().getEquipment(equipNum);
                        if (!allMunitions && (part instanceof AmmoBin)
                                && (!(m.getType() instanceof AmmoType)
                                        || (((AmmoType) epart.getType()).getMunitionType()
                                                != ((AmmoType) m.getType()).getMunitionType()))) {
                                continue;
                        }
                        if (m.getType().equals(epart.getType())
                                && !m.isDestroyed()) {
                            epart.setEquipmentNum(equipNum);
                            found = true;
                            break;
                        }
                    }
                    if(found) {
                        equipNums.remove(i);
                    } else {
                        notFound.add(epart);
                    }
                } else if (part instanceof MissingEquipmentPart) {
                    MissingEquipmentPart epart = (MissingEquipmentPart)part;
                    int i = -1;
                    boolean found = false;
                    for(int equipNum : equipNums) {
                        i++;
                        m = unit.getEntity().getEquipment(equipNum);
                        if (!allMunitions && (part instanceof AmmoBin)
                                && (!(m.getType() instanceof AmmoType)
                                        || (((AmmoType) epart.getType()).getMunitionType()
                                                != ((AmmoType) m.getType()).getMunitionType()))) {
                            continue;
                        }
                        if (m.getType().equals(epart.getType())
                                && !m.isDestroyed()) {
                            epart.setEquipmentNum(equipNum);
                            found = true;
                            break;
                        }
                    }
                    if(found) {
                        equipNums.remove(i);
                    } else {
                        notFound.add(epart);
                    }
                }
            }
            allMunitions = true;
            remaining = new ArrayList<>(notFound);
            notFound.clear();
        }
        
        if (remaining.size() > 0) {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("Could not unscramble equipment for %s (%s)\r\n\r\n", unit.getName(), unit.getId()));
            for (Part part : remaining) {
                builder.append(" - " + part.getPartName() + " equipmentNum: ");
                if (part instanceof EquipmentPart) {
                    builder.append(((EquipmentPart)part).getEquipmentNum() + "\r\n");
                }
                else if (part instanceof MissingEquipmentPart) {
                    builder.append(((MissingEquipmentPart)part).getEquipmentNum() + "\r\n");
                }
            }

            builder.append("\r\nAvailable (remaining) equipment:\r\n");
            for (int equipNum : equipNums) {
                m = unit.getEntity().getEquipment(equipNum);
                EquipmentType mType = m.getType();
                builder.append(String.format(" %d: %s %s\r\n", equipNum, m.getName(), mType.getName()));
            }
            MekHQ.getLogger().log(Utilities.class, "unscrambleEquipmentNumbers", LogLevel.WARNING, builder.toString());
        }
    }
    
    public static void assignTroopersAndEquipmentNums(Unit unit) {
        if (!(unit.getEntity() instanceof BattleArmor)) {
            throw new IllegalArgumentException("Attempting to assign trooper values to parts for non-BA unit");
        }
        
        //Create a list that we can remove parts from as we match them
        List<EquipmentPart> tempParts = unit.getParts().stream()
                .filter(p -> p instanceof EquipmentPart)
                .map(p -> (EquipmentPart)p)
                .collect(Collectors.toList());
        
        for (Mounted m : unit.getEntity().getEquipment()) {
            final int eqNum = unit.getEntity().getEquipmentNum(m);
            //Look for parts of the same type with the equipment number already set correctly
            List<EquipmentPart> parts = tempParts.stream()
                    .filter(p -> p.getType().getInternalName().equals(m.getType().getInternalName())
                            && p.getEquipmentNum() == eqNum)
                    .collect(Collectors.toList());
            //If we don't find any, just match the internal name and set the equipment number.
            if (parts.isEmpty()) {
                parts = tempParts.stream()
                        .filter(p -> p.getType().getInternalName().equals(m.getType().getInternalName()))
                        .collect(Collectors.toList());
                parts.forEach(p -> p.setEquipmentNum(eqNum));
            }
            if (parts.stream().allMatch(p -> p instanceof BattleArmorEquipmentPart)) {
                //Try to find one for each trooper; if the Entity has multiple pieces of equipment of this
                //type this will make sure we're only setting one group to this eq number.
                Part[] perTrooper = new Part[unit.getEntity().locations() - 1];
                for (EquipmentPart p : parts) {
                    int trooper = ((BattleArmorEquipmentPart)p).getTrooper();
                    if (trooper > 0) {
                        perTrooper[trooper - 1] = p;
                    }
                }
                //Assign a part to any empty position and set the trooper field
                for (int t = 0; t < perTrooper.length; t++) {
                    if (null == perTrooper[t]) {
                        for (Part p : parts) {
                            if (((BattleArmorEquipmentPart)p).getTrooper() < 1) {
                                ((BattleArmorEquipmentPart)p).setTrooper(t + 1);
                                perTrooper[t] = p;
                                break;
                            }
                        }
                    }
                }
                //Normally there should be a part in each position, but we will leave open the possibility
                //of equipment missing equipment for some troopers in the case of modular/AP mounts or DWPs
                for (Part p : perTrooper) {
                    if (null != p) {
                        tempParts.remove(p);
                    }
                }
            } else {
                //Ammo Bin
                tempParts.removeAll(parts);
            }
        }
        //TODO: Is it necessary to update armor?
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

    /** @return the current date as a DateTime time stamp for midnight in UTC time zone */
    public static DateTime getDateTimeDay(Calendar cal) {
        return new LocalDateTime(cal).toDateTime(DateTimeZone.UTC);
    }
    
    /** @return the current date as a DateTime time stamp for midnight in UTC time zone */
    public static DateTime getDateTimeDay(Date date) {
        return new LocalDateTime(date).toDateTime(DateTimeZone.UTC);
    }

    /**
     * Export a JTable to a CSV file
     * @param table
     * @param file
     * @return report
     */
    public static String exportTabletoCSV(JTable table, File file) {
        String report;
        try {
            TableModel model = table.getModel();
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(file.getPath()));
            String[] columns = new String[model.getColumnCount()];
            for (int i = 0; i < model.getColumnCount(); i++) {
                columns[i] = model.getColumnName(i);
            }
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(columns));

            for (int i = 0; i < model.getRowCount(); i++) {
                Object[] towrite = new String[model.getColumnCount()];
                for (int j = 0; j < model.getColumnCount(); j++) {
                    // use regex to remove any HTML tags
                    towrite[j] = model.getValueAt(i,j).toString().replaceAll("\\<[^>]*>", "");
                }
                csvPrinter.printRecord(towrite);
            }

            csvPrinter.flush();
            csvPrinter.close();

            report = model.getRowCount() + " " + resourceMap.getString("RowsWritten.text");
        } catch(Exception ioe) {
            MekHQ.getLogger().log(Utilities.class, "exportTabletoCSV", LogLevel.INFO, "Error exporting JTable");
            report = "Error exporting JTable. See log for details.";
        }
        return report;
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

    /** @return the input string with all words capitalized */
    public static String capitalize(String str) {
        if((null == str) || str.isEmpty()) {
            return str;
        }
        final char[] buffer = str.toCharArray();
        boolean capitalizeNext = true;
        for(int i = 0; i < buffer.length; ++ i) {
            final char ch = buffer[i];
            if(Character.isWhitespace(ch)) {
                capitalizeNext = true;
            } else if(capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch);
                capitalizeNext = false;
            }
        }
        return new String(buffer);
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

    /**
     * Run through the directory and call parser.parse(fis) for each XML file found. Don't recurse.
     */
    public static void parseXMLFiles(String dirName, FileParser parser) {
        parseXMLFiles(dirName, parser, false);
    }

    /**
     * Run through the directory and call parser.parse(fis) for each XML file found.
     */
    public static void parseXMLFiles(String dirName, FileParser parser, boolean recurse) {
        final String METHOD_NAME = "parseXMLFiles(String,FileParser,boolean)"; //$NON-NLS-1$

        if( null == dirName || null == parser ) {
            throw new NullPointerException();
        }
        File dir = new File(dirName);
        if( dir.isDirectory() ) {
            File[] files = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase(Locale.ROOT).endsWith(".xml"); //$NON-NLS-1$
                }
            });
            if( null != files && files.length > 0 ) {
                // Case-insensitive sorting. Yes, even on Windows. Deal with it.
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return f1.getPath().compareTo(f2.getPath());
                    }
                });
                // Try parsing and updating the main list, one by one
                for( File file : files ) {
                    if( file.isFile() ) {
                        try(FileInputStream fis = new FileInputStream(file)) {
                            parser.parse(fis);
                        } catch(Exception ex) {
                            // Ignore this file then
                            MekHQ.getLogger().log(Utilities.class, METHOD_NAME, LogLevel.ERROR,
                                    "Exception trying to parse " + file.getPath() + " - ignoring."); //$NON-NLS-1$ //$NON-NLS-2$
                            MekHQ.getLogger().error(Utilities.class, METHOD_NAME, ex);
                        }
                    }
                }
            }
            
            if( !recurse ) {
                // We're done
                return;
            }
            
            // Get subdirectories too
            File[] dirs = dir.listFiles();
            if( null != dirs && dirs.length > 0 ) {
                Arrays.sort(dirs, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return f1.getPath().compareTo(f2.getPath());
                    }
                });
                for( File subDirectory : dirs ) {
                    if( subDirectory.isDirectory() ) {
                        parseXMLFiles(subDirectory.getPath(), parser, recurse);
                    }
                }
            }

        }
    }
}