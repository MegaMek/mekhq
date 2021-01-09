/*
 * Utilities.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 * Copyright (c) 2019 The MekHQ Team.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
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
import java.util.Set;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import megamek.client.generator.RandomNameGenerator;
import megamek.common.enums.Gender;
import megamek.common.icons.AbstractIcon;
import megamek.common.util.StringUtil;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.equipment.*;
import mekhq.campaign.personnel.enums.Phenotype;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import megamek.client.Client;
import megamek.common.ASFBay;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Bay;
import megamek.common.Compute;
import megamek.common.ConvFighter;
import megamek.common.Crew;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.HeavyVehicleBay;
import megamek.common.ITechnology;
import megamek.common.Infantry;
import megamek.common.InfantryBay;
import megamek.common.Jumpship;
import megamek.common.LandAirMech;
import megamek.common.LightVehicleBay;
import megamek.common.Mech;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.SmallCraftBay;
import megamek.common.SuperHeavyVehicleBay;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import megamek.common.VTOL;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.CrewType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTechProgression;
import org.w3c.dom.Node;

public class Utilities {
    private static ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.Utilities", new EncodeControl());

    // A couple of arrays for use in the getLevelName() method
    private static int[] arabicNumbers = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static String[] romanNumerals = "M,CM,D,CD,C,XC,L,XL,X,IX,V,IV,I".split(",");

    public static int roll3d6() {
        Vector<Integer> rolls = new Vector<>();
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
     * @param num   the number of dice to roll
     * @param faces the number of faces on those dice
     * @return an Integer list of every dice roll, with index 0 containing the summed result
     */
    public static List<Integer> individualDice(int num, int faces) {
        List<Integer> individualRolls = new ArrayList<>();
        int result = 0, roll;
        individualRolls.add(result);

        for (int i = 0; i < num; i++) {
            roll = Compute.randomInt(faces) + 1;
            individualRolls.add(roll);
            result += roll;
        }

        individualRolls.set(0, result);

        return individualRolls;
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
        if ((null == collection) || collection.isEmpty()) {
            return null;
        }
        int index = Compute.randomInt(collection.size());
        Iterator<? extends T> iterator = collection.iterator();
        for (int i = 0; i < index; ++ i) {
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
        if ((null == list) || list.isEmpty() ) {
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
        return (int) Math.round(min * (1.0 - f) + max * f);
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
        if (null != first) {
            return first;
        }
        if (null != second) {
            return second;
        }
        T result = others[0];
        int index = 1;
        while ((null == result) && (index < others.length)) {
            result = others[index];
            ++ index;
        }
        return result;
    }

    public static List<AmmoType> getMunitionsFor(Entity entity, AmmoType currentAmmoType, int techLvl) {
        if (currentAmmoType == null) {
            return Collections.emptyList();
        }

        List<AmmoType> ammoTypes = new ArrayList<>();
        for (AmmoType ammoType : AmmoType.getMunitionsFor(currentAmmoType.getAmmoType())) {
            //this is an abbreviated version of setupMunitions in the CustomMechDialog
            //TODO: clan/IS limitations?

            if ((entity instanceof Aero)
                    && !ammoType.canAeroUse(entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_ARTILLERY_MUNITIONS))) {
                continue;
            }

            int lvl = ammoType.getTechLevel(entity.getTechLevelYear());
            if (lvl < 0) {
                lvl = 0;
            }

            if (techLvl < Utilities.getSimpleTechLevel(lvl)) {
                continue;
            } else if (TechConstants.isClan(currentAmmoType.getTechLevel(entity.getTechLevelYear())) != TechConstants.isClan(lvl)) {
                continue;
            }

            // Only Protos can use Proto-specific ammo
            if (ammoType.hasFlag(AmmoType.F_PROTOMECH) && !(entity instanceof Protomech)) {
                continue;
            }

            // When dealing with machine guns, Protos can only use proto-specific machine gun ammo
            if ((entity instanceof Protomech)
                    && ammoType.hasFlag(AmmoType.F_MG)
                    && !ammoType.hasFlag(AmmoType.F_PROTOMECH)) {
                continue;
            }

            if (ammoType.hasFlag(AmmoType.F_NUCLEAR) && ammoType.hasFlag(AmmoType.F_CAP_MISSILE)
                    && !entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AT2_NUKES)) {
                continue;
            }

            // Battle Armor ammo can't be selected at all.
            // All other ammo types need to match on rack size and tech.
            if ((ammoType.getRackSize() == currentAmmoType.getRackSize())
                    && (ammoType.hasFlag(AmmoType.F_BATTLEARMOR) == currentAmmoType.hasFlag(AmmoType.F_BATTLEARMOR))
                    && (ammoType.hasFlag(AmmoType.F_ENCUMBERING) == currentAmmoType.hasFlag(AmmoType.F_ENCUMBERING))
                    && ((ammoType.getTonnage(entity) == currentAmmoType.getTonnage(entity))
                    || ammoType.hasFlag(AmmoType.F_CAP_MISSILE))) {
                ammoTypes.add(ammoType);
            }
        }
        return ammoTypes;
    }

    public static boolean compareMounted(Mounted a, Mounted b) {
        if (!a.getType().equals(b.getType()))
            return false;
        if (!a.getClass().equals(b.getClass()))
            return false;
        if (!a.getName().equals(b.getName()))
            return false;
        return a.getLocation() == b.getLocation();
    }

    /**
     * Returns the last file modified in a directory and all subdirectories
     * that conforms to a FilenameFilter
     * @param dir       direction name
     * @param filter    filter for the file's name
     * @return          the last file modified in that dir that fits the filter
     */
    public static File lastFileModified(String dir, FilenameFilter filter) {
        File fl = new File(dir);
        long lastMod = Long.MIN_VALUE;
        File choice = null;

        File[] files = fl.listFiles(filter);
        if (null == files) {
            return null;
        }

        for (File file : files) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        //ok now we need to recursively search any subdirectories,
        //so see if they contain more recent files
        files = fl.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) {
                    continue;
                }

                File subFile = lastFileModified(file.getPath(), filter);
                if (null != subFile && subFile.lastModified() > lastMod) {
                    choice = subFile;
                    lastMod = subFile.lastModified();
                }
            }
        }

        return choice;
    }

    public static File[] getAllFiles(String dir, FilenameFilter filter) {
        File fl = new File(dir);
        return fl.listFiles(filter);
    }

    public static ArrayList<String> getAllVariants(Entity en, Campaign campaign) {
        CampaignOptions options = campaign.getCampaignOptions();
        ArrayList<String> variants = new ArrayList<>();
        for(MechSummary summary : MechSummaryCache.getInstance().getAllMechs()) {
            // If this isn't the same chassis, is our current unit, we continue
            if (!en.getChassis().equalsIgnoreCase(summary.getChassis())
                    || en.getModel().equalsIgnoreCase(summary.getModel())
                    || !summary.getUnitType().equals(UnitType.getTypeName(en.getUnitType()))) {
                continue;
            }

            // Weight of the two units must match or we continue, but BA weight gets
            // checked differently
            if (en instanceof BattleArmor) {
                if (((BattleArmor) en).getTroopers() != (int) summary.getTWweight()) {
                    continue;
                }
            } else {
                if (summary.getTons() != en.getWeight()) {
                    continue;
                }
            }

            // If we only allow canon units and this isn't canon we continue
            if (!summary.isCanon() && options.allowCanonRefitOnly()) {
                continue;
            }

            // If the unit doesn't meet the tech filter criteria we continue
            ITechnology techProg = UnitTechProgression.getProgression(summary, campaign.getTechFaction(), true);
            if (null == techProg) {
                // This should never happen unless there was an exception thrown when calculating the progression.
                // In such a case we will log it and take the least restrictive action, which is to let it through.
                MekHQ.getLogger().warning(Utilities.class, "Could not determine tech progression for " + summary.getName()
                                + ", including among available refits.");
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
        } else if (entity1.getWeight() != entity2.getWeight()) {
            return false;
        } else if (entity1.getClass() != entity2.getClass()) {
            return false;
        } else if (entity1.getEngine().getRating() != entity2.getEngine().getRating()
                || entity1.getEngine().getEngineType() != entity2.getEngine().getEngineType()
                || entity1.getEngine().getFlags() != entity2.getEngine().getFlags()) {
            return false;
        } else if (entity1.getStructureType() != entity2.getStructureType()) {
            return false;
        }
        if (entity1 instanceof Mech) {
            if (((Mech) entity1).getCockpitType() != ((Mech) entity2).getCockpitType()) {
                return false;
            }
            if (entity1.getGyroType() != entity2.getGyroType()) {
                return false;
            }
        } else if (entity1 instanceof Aero) {
            if (((Aero) entity1).getCockpitType() != ((Aero) entity2).getCockpitType()) {
                return false;
            }
        } else if (entity1 instanceof Tank) {
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
                if ((null != crit) && (crit.getType() == CriticalSlot.TYPE_EQUIPMENT) && (null != crit.getMount())) {
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
        if (roll < 2) {
            return SkillType.EXP_ULTRA_GREEN;
        } else if (roll < 6) {
            return SkillType.EXP_GREEN;
        } else if(roll < 10) {
            return SkillType.EXP_REGULAR;
        } else if(roll < 12) {
            return SkillType.EXP_VETERAN;
        } else {
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
            if(navigator.getRankNumeric() > bestRank) {
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
        int casualties;
        if(e instanceof Infantry) {
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

    public static Map<CrewType, Collection<Person>> genRandomCrewWithCombinedSkill(Campaign c, Unit u, String factionCode) {
        Objects.requireNonNull(c);
        Objects.requireNonNull(u);
        Objects.requireNonNull(u.getEntity(), "Unit needs to have a valid Entity attached");
        Crew oldCrew = u.getEntity().getCrew();

        int averageGunnery;
        int averagePiloting;
        int numberPeopleGenerated = 0;
        List<Person> drivers = new ArrayList<>();
        List<Person> gunners = new ArrayList<>();
        List<Person> vesselCrew = new ArrayList<>();
        Person navigator = null;
        Person consoleCmdr = null;
        int totalGunnery = 0;
        int totalPiloting = 0;

        // If the entire crew is dead, we still want to generate them. This is because they might
        // not be truly dead - this will be the case for BA for example
        // Also, the user may choose to GM make them un-dead in the resolve scenario dialog

        // Generate solo crews
        if (u.usesSoloPilot()) {
            //region Solo Pilot
            Person p;
            if (u.getEntity() instanceof LandAirMech) {
                p = c.newPerson(Person.T_MECHWARRIOR, Person.T_NONE, factionCode, oldCrew.getGender());
                p.addSkill(SkillType.S_PILOT_MECH, SkillType.getType(SkillType.S_PILOT_MECH).getTarget()
                        - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_MECH, SkillType.getType(SkillType.S_GUN_MECH).getTarget()
                        - oldCrew.getGunnery(), 0);
                p.addSkill(SkillType.S_PILOT_AERO, SkillType.getType(SkillType.S_PILOT_AERO).getTarget()
                        - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_AERO, SkillType.getType(SkillType.S_GUN_AERO).getTarget()
                        - oldCrew.getGunnery(), 0);
                p.setSecondaryRole(Person.T_AERO_PILOT);
            } else if (u.getEntity() instanceof Mech) {
                p = c.newPerson(Person.T_MECHWARRIOR, Person.T_NONE, factionCode, oldCrew.getGender());
                p.addSkill(SkillType.S_PILOT_MECH, SkillType.getType(SkillType.S_PILOT_MECH).getTarget()
                        - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_MECH, SkillType.getType(SkillType.S_GUN_MECH).getTarget()
                        - oldCrew.getGunnery(), 0);
            } else if (u.getEntity() instanceof Aero) {
                p = c.newPerson(Person.T_AERO_PILOT, Person.T_NONE, factionCode, oldCrew.getGender());
                p.addSkill(SkillType.S_PILOT_AERO, SkillType.getType(SkillType.S_PILOT_AERO).getTarget()
                        - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_AERO, SkillType.getType(SkillType.S_GUN_AERO).getTarget()
                        - oldCrew.getGunnery(), 0);
            } else if (u.getEntity() instanceof ConvFighter) {
                p = c.newPerson(Person.T_CONV_PILOT, Person.T_NONE, factionCode, oldCrew.getGender());
                p.addSkill(SkillType.S_PILOT_JET, SkillType.getType(SkillType.S_PILOT_JET).getTarget()
                        - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_JET, SkillType.getType(SkillType.S_GUN_JET).getTarget()
                        - oldCrew.getPiloting(), 0);
            } else if (u.getEntity() instanceof Protomech) {
                p = c.newPerson(Person.T_PROTO_PILOT, Person.T_NONE, factionCode, oldCrew.getGender());
                p.addSkill(SkillType.S_GUN_PROTO, SkillType.getType(SkillType.S_GUN_PROTO).getTarget()
                        - oldCrew.getGunnery(), 0);
            } else if (u.getEntity() instanceof VTOL) {
                p = c.newPerson(Person.T_VTOL_PILOT, Person.T_NONE, factionCode, oldCrew.getGender());
                p.addSkill(SkillType.S_PILOT_VTOL, SkillType.getType(SkillType.S_PILOT_VTOL).getTarget()
                        - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_VEE, SkillType.getType(SkillType.S_GUN_VEE).getTarget()
                        - oldCrew.getGunnery(), 0);
            } else {
                //assume tanker if we got here
                p = c.newPerson(Person.T_GVEE_DRIVER, Person.T_NONE, factionCode, oldCrew.getGender());
                p.addSkill(SkillType.S_PILOT_GVEE, SkillType.getType(SkillType.S_PILOT_GVEE).getTarget()
                        - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_VEE, SkillType.getType(SkillType.S_GUN_VEE).getTarget()
                        - oldCrew.getGunnery(), 0);
            }

            migrateCrewData(p, oldCrew, 0, true);
            drivers.add(p);
            //endregion Solo Pilot
        } else {
            if (oldCrew.getSlotCount() > 1) {
                //region Multi-Slot Crew
                for (int slot = 0; slot < oldCrew.getSlotCount(); slot++) {
                    Person p = null;
                    if (u.getEntity() instanceof Mech) {
                        p = c.newPerson(Person.T_MECHWARRIOR, Person.T_NONE, factionCode, oldCrew.getGender(slot));
                        p.addSkill(SkillType.S_PILOT_MECH, SkillType.getType(SkillType.S_PILOT_MECH).getTarget()
                                - oldCrew.getPiloting(slot), 0);
                        p.addSkill(SkillType.S_GUN_MECH, SkillType.getType(SkillType.S_GUN_MECH).getTarget()
                                - oldCrew.getGunnery(slot), 0);
                    } else if (u.getEntity() instanceof Aero) {
                        p = c.newPerson(Person.T_AERO_PILOT, Person.T_NONE, factionCode, oldCrew.getGender(slot));
                        p.addSkill(SkillType.S_PILOT_AERO, SkillType.getType(SkillType.S_PILOT_AERO).getTarget()
                                - oldCrew.getPiloting(slot), 0);
                        p.addSkill(SkillType.S_GUN_AERO, SkillType.getType(SkillType.S_GUN_AERO).getTarget()
                                - oldCrew.getGunnery(slot), 0);
                    }
                    if (null != p) {
                        if (!oldCrew.getExternalIdAsString(numberPeopleGenerated).equals("-1")) {
                            p.setId(UUID.fromString(oldCrew.getExternalIdAsString(numberPeopleGenerated)));
                        }

                        migrateCrewData(p, oldCrew, numberPeopleGenerated++, true);
                        drivers.add(p);
                    }
                }
                //endregion Multi-Slot Crew
            }
            // This is a nightmare case, not just for BA. We are also currently assuming that MM and
            // therefore the MUL will contain the correct number of crew if more than 1 is included.
            // TODO : This should not be an else statement, but rather based on a comparison between
            // TODO : the numberPeopleGenerated and a fixed u.getFullCrewSize() (because that doesn't
            // TODO : necessarily provide the correct number when called based on my current read on
            // TODO : 26-Feb-2020)
            else {
                // Generate drivers for multi-crewed vehicles and vessels

                //Uggh, BA are a nightmare. The getTotalDriverNeeds will adjust for missing/destroyed suits
                //but we can't change that because lots of other stuff needs that to be right, so we will hack
                //it here to make it the starting squad size
                int driversNeeded  = u.getTotalDriverNeeds();
                if (u.getEntity() instanceof BattleArmor) {
                    driversNeeded = ((BattleArmor) u.getEntity()).getSquadSize();
                }

                for (int slot = 0; slot < driversNeeded; slot++) {
                    Person p;
                    if (u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship) {
                        p = c.newPerson(Person.T_SPACE_PILOT, Person.T_NONE, factionCode,
                                oldCrew.getGender(numberPeopleGenerated));
                        p.addSkill(SkillType.S_PILOT_SPACE, randomSkillFromTarget(
                                SkillType.getType(SkillType.S_PILOT_SPACE).getTarget()
                                        - oldCrew.getPiloting()),
                                0);
                        totalPiloting += p.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue();
                    } else if(u.getEntity() instanceof BattleArmor) {
                        p = c.newPerson(Person.T_BA, Person.T_NONE, factionCode,
                                oldCrew.getGender(numberPeopleGenerated));
                        p.addSkill(SkillType.S_GUN_BA, randomSkillFromTarget(
                                SkillType.getType(SkillType.S_GUN_BA).getTarget()
                                        - oldCrew.getGunnery()),
                                0);
                        totalGunnery += p.getSkill(SkillType.S_GUN_BA).getFinalSkillValue();
                    } else if(u.getEntity() instanceof Infantry) {
                        p = c.newPerson(Person.T_INFANTRY, Person.T_NONE, factionCode,
                                oldCrew.getGender(numberPeopleGenerated));
                        p.addSkill(SkillType.S_SMALL_ARMS, randomSkillFromTarget(
                                SkillType.getType(SkillType.S_SMALL_ARMS).getTarget() - oldCrew.getGunnery()),
                                0);
                        totalGunnery += p.getSkill(SkillType.S_SMALL_ARMS).getFinalSkillValue();
                    } else if(u.getEntity() instanceof VTOL) {
                        p = c.newPerson(Person.T_VTOL_PILOT, Person.T_NONE, factionCode,
                                oldCrew.getGender(numberPeopleGenerated));
                        p.addSkill(SkillType.S_PILOT_VTOL, SkillType.getType(
                                SkillType.S_PILOT_VTOL).getTarget() - oldCrew.getPiloting(),
                                0);
                        p.addSkill(SkillType.S_GUN_VEE, SkillType.getType(
                                SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery(),
                                0);
                    } else if (u.getEntity() instanceof Mech) {
                        p = c.newPerson(Person.T_MECHWARRIOR, Person.T_NONE, factionCode,
                                oldCrew.getGender(numberPeopleGenerated));
                        p.addSkill(SkillType.S_PILOT_MECH, SkillType.getType(
                                SkillType.S_PILOT_MECH).getTarget() - oldCrew.getPiloting(),
                                0);
                        p.addSkill(SkillType.S_GUN_MECH, SkillType.getType(
                                SkillType.S_GUN_MECH).getTarget() - oldCrew.getGunnery(),
                                0);
                    } else {
                        //assume tanker if we got here
                        p = c.newPerson(Person.T_GVEE_DRIVER, Person.T_NONE, factionCode,
                                oldCrew.getGender(numberPeopleGenerated));
                        p.addSkill(SkillType.S_PILOT_GVEE, SkillType.getType(
                                SkillType.S_PILOT_GVEE).getTarget() - oldCrew.getPiloting(),
                                0);
                        p.addSkill(SkillType.S_GUN_VEE, SkillType.getType(
                                SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery(),
                                0);
                    }

                    migrateCrewData(p, oldCrew, numberPeopleGenerated++, true);
                    drivers.add(p);
                }

                // Regenerate as needed to balance
                if (drivers.size() != 0) {
                    averageGunnery = (int) Math.round(((double) totalGunnery) / drivers.size());
                    averagePiloting = (int) Math.round(((double) totalPiloting) / drivers.size());
                    if (u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship) {
                        while (averagePiloting != oldCrew.getPiloting()) {
                            totalPiloting = 0;
                            for (Person p : drivers) {
                                p.addSkill(SkillType.S_PILOT_SPACE, randomSkillFromTarget(
                                        SkillType.getType(SkillType.S_PILOT_SPACE).getTarget()
                                                - oldCrew.getPiloting()),
                                        0);
                                totalPiloting += p.getSkill(SkillType.S_PILOT_SPACE).getFinalSkillValue();
                            }
                            averagePiloting = (int) Math.round(((double) totalPiloting) / drivers.size());
                        }
                    } else if (u.getEntity() instanceof BattleArmor) {
                        while (averageGunnery != oldCrew.getGunnery()) {
                            totalGunnery = 0;
                            for (Person p : drivers) {
                                p.addSkill(SkillType.S_GUN_BA, randomSkillFromTarget(
                                        SkillType.getType(SkillType.S_GUN_BA).getTarget()
                                                - oldCrew.getGunnery()),
                                        0);
                                totalGunnery += p.getSkill(SkillType.S_GUN_BA).getFinalSkillValue();
                            }
                            averageGunnery = (int) Math.round(((double) totalGunnery) / drivers.size());
                        }
                    } else if (u.getEntity() instanceof Infantry) {
                        while (averageGunnery != oldCrew.getGunnery()) {
                            totalGunnery = 0;
                            for (Person p : drivers) {
                                p.addSkill(SkillType.S_SMALL_ARMS, randomSkillFromTarget(
                                        SkillType.getType(SkillType.S_SMALL_ARMS).getTarget()
                                                - oldCrew.getGunnery()),
                                        0);
                                totalGunnery += p.getSkill(SkillType.S_SMALL_ARMS).getFinalSkillValue();
                            }
                            averageGunnery = (int) Math.round(((double) totalGunnery) / drivers.size());
                        }
                    }
                }

                if (!u.usesSoldiers()) {
                    // Generate gunners for multi-crew vehicles
                    for (int slot = 0; slot < u.getTotalGunnerNeeds(); slot++) {
                        Person p;
                        if (u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship) {
                            p = c.newPerson(Person.T_SPACE_GUNNER, Person.T_NONE, factionCode,
                                    oldCrew.getGender(numberPeopleGenerated));
                            p.addSkill(SkillType.S_GUN_SPACE, randomSkillFromTarget(
                                    SkillType.getType(SkillType.S_GUN_SPACE).getTarget()
                                            - oldCrew.getGunnery()),
                                    0);
                            totalGunnery += p.getSkill(SkillType.S_GUN_SPACE).getFinalSkillValue();
                        } else if (u.getEntity() instanceof Mech) {
                            p = c.newPerson(Person.T_MECHWARRIOR, Person.T_NONE, factionCode,
                                    oldCrew.getGender(numberPeopleGenerated));
                            p.addSkill(SkillType.S_PILOT_MECH,
                                    SkillType.getType(SkillType.S_PILOT_MECH).getTarget()
                                            - oldCrew.getPiloting(),
                                    0);
                            p.addSkill(SkillType.S_GUN_MECH,
                                    SkillType.getType(SkillType.S_GUN_MECH).getTarget()
                                            - oldCrew.getGunnery(),
                                    0);
                            totalGunnery += p.getSkill(SkillType.S_GUN_MECH).getFinalSkillValue();
                        } else {
                            //assume tanker if we got here
                            p = c.newPerson(Person.T_VEE_GUNNER, Person.T_NONE, factionCode,
                                    oldCrew.getGender(numberPeopleGenerated));
                            p.addSkill(SkillType.S_GUN_VEE, randomSkillFromTarget(
                                    SkillType.getType(SkillType.S_GUN_VEE).getTarget()
                                            - oldCrew.getGunnery()), 0);
                            totalGunnery += p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue();
                        }

                        migrateCrewData(p, oldCrew, numberPeopleGenerated++, true);
                        gunners.add(p);
                    }

                    // Regenerate gunners as needed to balance
                    if (gunners.size() != 0) {
                        averageGunnery = (int) Math.round(((double) totalGunnery) / gunners.size());
                        if (u.getEntity() instanceof Tank) {
                            while (averageGunnery != oldCrew.getGunnery()) {
                                totalGunnery = 0;
                                for (Person p : gunners) {
                                    p.addSkill(SkillType.S_GUN_VEE, randomSkillFromTarget(
                                            SkillType.getType(SkillType.S_GUN_VEE).getTarget()
                                                    - oldCrew.getGunnery()), 0);
                                    totalGunnery += p.getSkill(SkillType.S_GUN_VEE).getFinalSkillValue();
                                }
                                averageGunnery = (int) Math.round(((double) totalGunnery) / gunners.size());
                            }
                        } else if (u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship) {
                            while (averageGunnery != oldCrew.getGunnery()) {
                                totalGunnery = 0;
                                for (Person p : gunners) {
                                    p.addSkill(SkillType.S_GUN_SPACE, randomSkillFromTarget(
                                            SkillType.getType(SkillType.S_GUN_SPACE).getTarget()
                                                    - oldCrew.getGunnery()), 0);
                                    totalGunnery += p.getSkill(SkillType.S_GUN_SPACE).getFinalSkillValue();
                                }
                                averageGunnery = (int) Math.round(((double) totalGunnery) / gunners.size());
                            }
                        }
                    }
                }
            }

            for (int slot = 0; slot < u.getTotalCrewNeeds(); slot++) {
                Person p = c.newPerson(u.getEntity().isSupportVehicle()
                                ? Person.T_VEHICLE_CREW
                                : Person.T_SPACE_CREW,
                        Person.T_NONE, factionCode, oldCrew.getGender(numberPeopleGenerated));

                migrateCrewData(p, oldCrew, numberPeopleGenerated++, false);
                vesselCrew.add(p);
            }

            if (u.canTakeNavigator()) {
                navigator = c.newPerson(Person.T_NAVIGATOR, Person.T_NONE, factionCode,
                        oldCrew.getGender(numberPeopleGenerated));
                migrateCrewData(navigator, oldCrew, numberPeopleGenerated++, false);
            }

            if (u.canTakeTechOfficer()) {
                consoleCmdr = c.newPerson(Person.T_VEE_GUNNER, Person.T_NONE, factionCode,
                        oldCrew.getGender(numberPeopleGenerated));
                migrateCrewData(consoleCmdr, oldCrew, numberPeopleGenerated, false);
            }
        }

        //region Data Gathering
        Map<CrewType, Collection<Person>> result = new HashMap<>();
        if (!drivers.isEmpty()) {
            if (u.usesSoloPilot()) {
                result.put(CrewType.PILOT, drivers);
            } else if (u.usesSoldiers()) {
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
        //endregion Data Gathering
        return result;
    }

    /**
     * Function that determines what name should be used by a person that is created through crew
     * And then assigns them a pre-selected portrait, provided one is to their index
     * Additionally, any extraData parameters should be migrated here
     * @param p           the person to be renamed, if applicable
     * @param oldCrew     the crew object they were a part of
     * @param crewIndex   the index of the person in the crew
     * @param crewOptions whether or not to run the populateOptionsFromCrew for this person
     */
    private static void migrateCrewData(Person p, Crew oldCrew, int crewIndex, boolean crewOptions) {
        if (crewOptions) {
            populateOptionsFromCrew(p, oldCrew);
        }

        // this is a bit of a hack, but instead of tracking it elsewhere we only set gender to
        // male or female when a name is generated. G_RANDOMIZE will therefore only be returned for
        // crew that don't have names, so we can just leave them with their randomly generated name
        if (oldCrew.getGender(crewIndex) != Gender.RANDOMIZE) {
            String givenName = oldCrew.getExtraDataValue(crewIndex, Crew.MAP_GIVEN_NAME);

            if (StringUtil.isNullOrEmpty(givenName)) {
                String name = oldCrew.getName(crewIndex);

                if (!(name.equalsIgnoreCase(RandomNameGenerator.UNNAMED) || name.equalsIgnoreCase(RandomNameGenerator.UNNAMED_FULL_NAME))) {
                    p.migrateName(name);
                }
            } else {
                p.setGivenName(givenName);
                p.setSurname(oldCrew.getExtraDataValue(crewIndex, Crew.MAP_SURNAME));
                if (p.getSurname() == null) {
                    p.setSurname("");
                }

                String phenotype = oldCrew.getExtraDataValue(crewIndex, Crew.MAP_PHENOTYPE);
                if (phenotype != null) {
                    p.setPhenotype(Phenotype.parseFromString(phenotype));
                }

                p.setBloodname(oldCrew.getExtraDataValue(crewIndex, Crew.MAP_BLOODNAME));
            }

            // Only created crew can be assigned a portrait, so this is safe to put in here
            if (!AbstractIcon.DEFAULT_ICON_FILENAME.equals(oldCrew.getPortraitFileName(crewIndex))) {
                p.setPortrait(oldCrew.getPortrait(crewIndex));
            }
        }
    }

    /**
     * Worker function that takes the PilotOptions (SPAs, in other words) from the given "old crew" and sets them for a person.
     * @param p The person whose SPAs to populate
     * @param oldCrew The entity the SPAs of whose crew we're importing
     */
    private static void populateOptionsFromCrew(Person p, Crew oldCrew) {
        Enumeration<IOption> optionsEnum = oldCrew.getOptions().getOptions();
        while (optionsEnum.hasMoreElements()) {
            IOption currentOption = optionsEnum.nextElement();
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
        } else {
            return 0; // 20% chance of no xp
        }
    }

    public static int rollSpecialAbilities(int bonus) {
        int roll = Compute.d6(2) + bonus;
        if (roll < 10) {
            return 0;
        } else if (roll < 12) {
            return 1;
        } else {
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
        while (ndice > 0) {
            int roll = Compute.d6();
            //reroll all sixes once
            if (roll == 6) {
                roll += (Compute.d6() - 1);
            }
            if (clan) {
                roll = (int) Math.ceil(roll / 2.0);
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

    public static String printMoneyArray(Money[] array) {
        StringJoiner joiner = new StringJoiner(","); //$NON-NLS-1$
        for (Money value : array) {
            joiner.add(value.toXmlString());
        }
        return joiner.toString();
    }

    public static Money[] readMoneyArray(Node node) {
        return readMoneyArray(node, 0);
    }

    public static Money[] readMoneyArray(Node node, int minimumSize) {
        String[] values = node.getTextContent().split(",");
        Money[] result = new Money[Math.max(values.length, minimumSize)];

        for (int i = 0; i < values.length; i++) {
            result[i] = Money.fromXmlString(values[i]);
        }

        for (int i = values.length; i < result.length; i++) {
            result[i] = Money.zero();
        }

        return result;
    }

    public static int getSimpleTechLevel(int level) {
        switch(level) {
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
            case TechConstants.T_ALLOWED_ALL:
            case TechConstants.T_INTRO_BOXSET:
            default:
                return CampaignOptions.TECH_INTRO;
        }
    }

    //copied from http://www.roseindia.net/java/beginners/copyfile.shtml
    public static void copyfile(File inFile, File outFile) {
        try {
            InputStream in = new FileInputStream(inFile);

            //For Append the file.
            //  OutputStream out = new FileOutputStream(f2,true);

            //For Overwrite the file.
            OutputStream out = new FileOutputStream(outFile);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            MekHQ.getLogger().info(Utilities.class, "File copied.");
        } catch (FileNotFoundException e) {
            MekHQ.getLogger().error(Utilities.class, e.getMessage() + " in the specified directory.");
        } catch (IOException e) {
            MekHQ.getLogger().error(Utilities.class, e.getMessage());
        }
    }

    public static void unscrambleEquipmentNumbers(Unit unit, boolean refit) {
        //BA has one part per equipment entry per suit and may need to have trooper fields set following
        //a refit
        if (unit.getEntity() instanceof BattleArmor) {
            assignTroopersAndEquipmentNums(unit);
            return;
        }
        
        Map<Integer, Part> partMap = new HashMap<>();
        List<Integer> equipNums = new ArrayList<>();
        for (Mounted m : unit.getEntity().getEquipment()) {
            equipNums.add(unit.getEntity().getEquipmentNum(m));
        }

        // Handle exact matches first
        List<Part> remaining = new ArrayList<>();
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

            if (etype != null) {
                Mounted mounted = unit.getEntity().getEquipment(eqnum);
                if (equipNums.contains(eqnum)
                        && (mounted != null)
                        && etype.equals(mounted.getType())) {
                    equipNums.remove((Integer) eqnum);
                    partMap.put(eqnum, part);
                } else {
                    remaining.add(part);
                }
            }
        }

        // Handle approximate matches (AmmoBins with munition or bomb type changes)
        List<Part> notFound = new ArrayList<>();
        for (Part part : remaining) {
            int eqnum = -1;
            EquipmentType etype = null;
            if (part instanceof EquipmentPart) {
                eqnum = ((EquipmentPart) part).getEquipmentNum();
                etype = ((EquipmentPart) part).getType();
            } else if (part instanceof MissingEquipmentPart) {
                eqnum = ((MissingEquipmentPart) part).getEquipmentNum();
                etype = ((MissingEquipmentPart) part).getType();
            } else {
                continue;
            }

            // Invalid equipment or already found
            if ((etype == null) || partMap.containsKey(eqnum)) {
                notFound.add(part);
                continue;
            }

            Mounted mounted = unit.getEntity().getEquipment(eqnum);
            if ((part instanceof AmmoBin)
                    && (etype instanceof AmmoType)
                    && (mounted != null)
                    && (mounted.getType() instanceof AmmoType)) {
                // Handle AmmoBins which had their AmmoType changed but did not get reloaded yet.
                AmmoBin ammoBin = (AmmoBin) part;
                AmmoType mountedType = (AmmoType) mounted.getType();
                if (mountedType.equalsAmmoTypeOnly(ammoBin.getType())
                        && (mountedType.getRackSize() == ammoBin.getType().getRackSize())) {
                    equipNums.remove((Integer) eqnum);
                    partMap.put(eqnum, part);
                    continue;
                }
            }

            notFound.add(part);
        }

        remaining = new ArrayList<>(notFound);
        notFound.clear();

        // For ammo types we want to match the same munition type if possible to avoid
        // imposing unnecessary ammo swaps.
        // However, if we've just done a refit we may very well have changed ammo types,
        // so we need to set the equipment numbers in this case.
        for (Part part : remaining) {
            boolean found = false;
            int i = -1;

            if (part instanceof EquipmentPart) {
                EquipmentPart epart = (EquipmentPart) part;
                for (int equipNum : equipNums) {
                    i++;
                    Mounted m = unit.getEntity().getEquipment(equipNum);
                    if (part instanceof AmmoBin) {
                        if (!(m.getType() instanceof AmmoType)) {
                            continue;
                        }

                        AmmoBin ammoBin = (AmmoBin) part;
                        AmmoType ammoType = (AmmoType) m.getType();

                        // If this is a refit, we want to update our ammo bin parts to match
                        // the munitions specified in the refit, then reassign the equip number
                        if (refit) {
                            if (ammoBin.getType().equalsAmmoTypeOnly(ammoType)
                                    && (ammoType.getRackSize() == ammoBin.getType().getRackSize())
                                    && !m.isDestroyed()) {
                                ammoBin.setEquipmentNum(equipNum);
                                // Ensure Entity is synch'd with part
                                ammoBin.updateConditionFromPart();
                                // Unload bin before munition change
                                ammoBin.unload();
                                ammoBin.changeMunition(ammoType);
                                partMap.put(equipNum, part);
                                found = true;
                                break;
                            }
                        }
                    }
                    if (m.getType().equals(epart.getType()) && !m.isDestroyed()) {
                        epart.setEquipmentNum(equipNum);
                        partMap.put(equipNum, part);
                        found = true;
                        break;
                    }
                }
            } else if (part instanceof MissingEquipmentPart) {
                MissingEquipmentPart epart = (MissingEquipmentPart)part;
                for (int equipNum : equipNums) {
                    i++;
                    Mounted m = unit.getEntity().getEquipment(equipNum);
                    if (part instanceof MissingAmmoBin
                            && !(m.getType() instanceof AmmoType)) {
                        continue;
                    }
                    if (m.getType().equals(epart.getType()) && !m.isDestroyed()) {
                        epart.setEquipmentNum(equipNum);
                        partMap.put(equipNum, part);
                        found = true;
                        break;
                    }
                }
            }

            if (found) {
                equipNums.remove(i);
            } else {
                notFound.add(part);
            }
        }

        remaining = new ArrayList<>(notFound);
        notFound.clear();

        if (remaining.size() > 0) {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("Could not unscramble equipment for %s (%s)\r\n\r\n", unit.getName(), unit.getId()));
            for (Part part : remaining) {
                builder.append(" - ").append(part.getPartName()).append(" equipmentNum: ");
                if (part instanceof EquipmentPart) {
                    builder.append(((EquipmentPart) part).getEquipmentNum()).append("\r\n");
                }
                else if (part instanceof MissingEquipmentPart) {
                    builder.append(((MissingEquipmentPart) part).getEquipmentNum()).append("\r\n");
                }
            }

            builder.append("\r\nEquipment Parts:\r\n");
            for (Part p : unit.getParts()) {
                if (!(p instanceof EquipmentPart) &&
                        (!(p instanceof MissingEquipmentPart))) {
                    continue;
                }
                int equipNum;
                if (p instanceof EquipmentPart) {
                    EquipmentPart ePart = (EquipmentPart) p;
                    equipNum = ePart.getEquipmentNum();
                } else {
                    MissingEquipmentPart mePart = (MissingEquipmentPart) p;
                    equipNum = mePart.getEquipmentNum();
                }
                boolean isMissing = remaining.contains(p);
                String eName = equipNum >= 0 ? unit.getEntity().getEquipment(equipNum).getName() : "<None>";
                if (isMissing) {
                    eName = "<Incorrect>";

                    // Break the incorrect equipment number linkage if there is already a valid part at the location.
                    // This ensures only one part is mapped to each equipment number, avoiding crashes and other
                    // problems when these parts get out of sync.
                    if ((equipNum >= 0) && partMap.containsKey(equipNum)) {
                        if (p instanceof EquipmentPart) {
                            EquipmentPart ePart = (EquipmentPart) p;
                            ePart.setEquipmentNum(-1);
                        } else {
                            MissingEquipmentPart mePart = (MissingEquipmentPart) p;
                            mePart.setEquipmentNum(-1);
                        }
                    }
                }
                builder.append(String.format(" %d: %s %s %s %s\r\n", equipNum, p.getName(), p.getLocationName(), eName, isMissing ? " (Missing)" : ""));
            }

            builder.append("\r\nEquipment:\r\n");
            for (Mounted m : unit.getEntity().getEquipment()) {
                int equipNum = unit.getEntity().getEquipmentNum(m);
                EquipmentType mType = m.getType();
                boolean isAvailable = equipNums.contains(equipNum);
                builder.append(String.format(" %d: %s %s%s\r\n", equipNum, m.getName(), mType.getName(), isAvailable ? " (Available)" : ""));
            }
            MekHQ.getLogger().warning(builder.toString());
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

    /**
     * Export a JTable to a CSV file
     * @param table     the table to save to csv
     * @param file      the file to save to
     * @return a csv formatted export of the table
     */
    public static String exportTableToCSV(JTable table, File file) {
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
                Object[] toWrite = new String[model.getColumnCount()];
                for (int j = 0; j < model.getColumnCount(); j++) {
                    // use regex to remove any HTML tags
                    toWrite[j] = model.getValueAt(i,j).toString().replaceAll("<[^>]*>", "");
                }
                csvPrinter.printRecord(toWrite);
            }

            csvPrinter.flush();
            csvPrinter.close();

            report = model.getRowCount() + " " + resourceMap.getString("RowsWritten.text");
        } catch (Exception ioe) {
            MekHQ.getLogger().error("Error exporting JTable", ioe);
            report = "Error exporting JTable. See log for details.";
        }
        return report;
    }

    public static Vector<String> splitString(String str, String sep) {
        StringTokenizer st = new StringTokenizer(str, sep);
        Vector<String> output = new Vector<>();
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
        StringBuilder roman = new StringBuilder(" "); //$NON-NLS-1$
        int num = level+1;

        for (int i = 0; i < arabicNumbers.length; i++) {
            while (num > arabicNumbers[i]) {
                roman.append(romanNumerals[i]);
                num -= arabicNumbers[i];
            }
        }

        return roman.toString();
    }

    public static Map<String, Integer> sortMapByValue(Map<String, Integer> unsortMap, boolean highFirst) {

        // Convert Map to List
        List<Map.Entry<String, Integer>> list =
                new LinkedList<>(unsortMap.entrySet());

        // Sort list with comparator, to compare the Map values
        list.sort(Map.Entry.comparingByValue());

        // Convert sorted map back to a Map
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        if(highFirst) {
            ListIterator<Map.Entry<String, Integer>> li = list.listIterator(list.size());
            while(li.hasPrevious()) {
                Map.Entry<String, Integer> entry = li.previous();
                sortedMap.put(entry.getKey(), entry.getValue());
            }
        } else {
            for (Map.Entry<String, Integer> entry : list) {
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
        return en.isDestroyed() || en.isDoomed() || en.isStalled() || en.isStuck();
    }

    /**
     * Run through the directory and call parser.parse(fis) for each XML file found. Don't recurse.
     */
    public static void parseXMLFiles(String dirName, Consumer<FileInputStream> parser) {
        parseXMLFiles(dirName, parser, false);
    }

    /**
     * Run through the directory and call parser.parse(fis) for each XML file found.
     */
    public static void parseXMLFiles(String dirName, Consumer<FileInputStream> parser, boolean recurse) {
        if ((null == dirName) || (null == parser)) {
            throw new NullPointerException();
        }
        File dir = new File(dirName);
        if (dir.isDirectory()) {
            File[] files = dir.listFiles((dir1, name) -> name.toLowerCase(Locale.ROOT).endsWith(".xml"));
            if ((null != files) && (files.length > 0)) {
                // Case-insensitive sorting. Yes, even on Windows. Deal with it.
                Arrays.sort(files, Comparator.comparing(File::getPath));
                // Try parsing and updating the main list, one by one
                for (File file : files) {
                    if (file.isFile()) {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            parser.accept(fis);
                        } catch (Exception ex) {
                            // Ignore this file then
                            MekHQ.getLogger().error(Utilities.class, "Exception trying to parse " + file.getPath() + " - ignoring.");
                            MekHQ.getLogger().error(Utilities.class, ex);
                        }
                    }
                }
            }

            if (!recurse) {
                // We're done
                return;
            }

            // Get subdirectories too
            File[] dirs = dir.listFiles();
            if (null != dirs && dirs.length > 0) {
                Arrays.sort(dirs, Comparator.comparing(File::getPath));
                for (File subDirectory : dirs) {
                    if (subDirectory.isDirectory() ) {
                        parseXMLFiles(subDirectory.getPath(), parser, true);
                    }
                }
            }
        }
    }

    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */
    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

    /**
     * Handles loading a player's transported units onto their transports once a megamek scenario has actually started.
     * This separates loading air and ground units, since map type and planetary conditions may prohibit ground unit deployment
     * while the player wants fighters launched to defend the transport
     * @param trnId - The MM id of the transport entity we want to load
     * @param toLoad - List of Entity ids for the units we want to load into this transport
     * @param client - the player's Client instance
     * @param loadFighters - Should aero type units be loaded?
     * @param loadGround - should ground units be loaded?
     */
    public static void loadPlayerTransports(int trnId, Set<Integer> toLoad, Client client,
                                            boolean loadFighters, boolean loadGround) {
        if (!loadFighters && !loadGround) {
            //Nothing to do. Get outta here!
            return;
        }
        Entity transport = client.getEntity(trnId);
        // Reset transporter status, as currentSpace might still retain updates from when the Unit
        // was assigned to the Transport on the TO&E tab
        transport.resetTransporter();
        for (int id : toLoad) {
            Entity cargo = client.getEntity(id);
            if (cargo == null) {
                continue;
            }
            // Find a bay with space in it and update that space so the next unit can process
            cargo.setTargetBay(selectBestBayFor(cargo, transport));
        }
        // Reset transporter status again so that sendLoadEntity can process correctly
        transport.resetTransporter();
        for (int id : toLoad) {
            Entity cargo = client.getEntity(id);
            // And now load the units
            if (cargo.isFighter() && loadFighters && transport.canLoad(cargo, false) && cargo.getTargetBay() != -1) {
                client.sendLoadEntity(id, trnId, cargo.getTargetBay());
                // Add a wait to make sure that we don't start processing client.sendLoadEntity out of order
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    MekHQ.getLogger().error(Utilities.class, e);
                }
            } else if(loadGround && transport.canLoad(cargo, false) && cargo.getTargetBay() != -1) {
                client.sendLoadEntity(id, trnId, cargo.getTargetBay());
                // Add a wait to make sure that we don't start processing client.sendLoadEntity out of order
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    MekHQ.getLogger().error(Utilities.class, e);
                }
            }
        }
    }

    /**
     * Method that loops through a Transport ship's bays and finds one with enough available space to load the Cargo unit
     * Helps assign a bay number to the Unit record so that transport bays can be automatically filled once a game of MegaMek is started
     * @param cargo The Entity we wish to load into a bay
     * @param transport The Bay-equipped Entity we want to load Cargo aboard
     * @return integer representing the (lowest) bay number on Transport that has space to carry Cargo
     */
    public static int selectBestBayFor(Entity cargo, Entity transport) {
        if (cargo.isFighter()) {
            // Try to load ASF bays first, so as not to hog SC bays
            for (Bay b: transport.getTransportBays()) {
                if (b instanceof ASFBay && b.canLoad(cargo)) {
                    //Load 1 unit into the bay
                    b.setCurrentSpace(1);
                    return b.getBayNumber();
                }
            }
            for (Bay b: transport.getTransportBays()) {
                if (b instanceof SmallCraftBay && b.canLoad(cargo)) {
                    //Load 1 unit into the bay
                    b.setCurrentSpace(1);
                    return b.getBayNumber();
                }
            }
        } else if (cargo.getUnitType() == UnitType.TANK) {
            // Try to fit lighter tanks into smaller bays first
            for (Bay b: transport.getTransportBays()) {
                if (b instanceof LightVehicleBay && b.canLoad(cargo)) {
                    //Load 1 unit into the bay
                    b.setCurrentSpace(1);
                    return b.getBayNumber();
                }
            }
            for (Bay b: transport.getTransportBays()) {
                if (b instanceof HeavyVehicleBay && b.canLoad(cargo)) {
                    //Load 1 unit into the bay
                    b.setCurrentSpace(1);
                    return b.getBayNumber();
                }
            }
            for (Bay b: transport.getTransportBays()) {
                if (b instanceof SuperHeavyVehicleBay && b.canLoad(cargo)) {
                    //Load 1 unit into the bay
                    b.setCurrentSpace(1);
                    return b.getBayNumber();
                }
            }
        } else if (cargo.getUnitType() == UnitType.INFANTRY) {
            for (Bay b: transport.getTransportBays()) {
                if (b instanceof InfantryBay && b.canLoad(cargo)) {
                    //Update bay tonnage based on platoon/squad weight
                    b.setCurrentSpace(b.spaceForUnit(cargo));
                    return b.getBayNumber();
                }
            }
        } else {
            // Just return the first available bay
            for (Bay b : transport.getTransportBays()) {
                if (b.canLoad(cargo)) {
                    //Load 1 unit into the bay
                    b.setCurrentSpace(1);
                    return b.getBayNumber();
                }
            }
        }
        // Shouldn't happen
        return -1;
    }
}
