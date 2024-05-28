/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.Client;
import megamek.client.generator.RandomNameGenerator;
import megamek.codeUtilities.ObjectUtility;
import megamek.codeUtilities.StringUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.IPlayerSettings;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.unit.CrewType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTechProgression;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public class Utilities {
    private static final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.Utilities",
            MekHQ.getMHQOptions().getLocale());

    // A couple of arrays for use in the getLevelName() method
    private static final int[] arabicNumbers = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
    private static final String[] romanNumerals = "M,CM,D,CD,C,XC,L,XL,X,IX,V,IV,I".split(",");

    public static int roll3d6() {
        Vector<Integer> rolls = new Vector<>();
        rolls.add(Compute.d6());
        rolls.add(Compute.d6());
        rolls.add(Compute.d6());
        Collections.sort(rolls);
        return (rolls.elementAt(0) + rolls.elementAt(1));
    }

    /**
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

    public static List<AmmoType> getMunitionsFor(Entity entity, AmmoType currentAmmoType, int techLvl) {
        if (currentAmmoType == null) {
            return Collections.emptyList();
        }

        final Vector<AmmoType> munitions = AmmoType.getMunitionsFor(currentAmmoType.getAmmoType());
        if (munitions == null) {
            LogManager.getLogger().error(String.format("Cannot getMunitions for %s because of a null munitions list for ammo type %d",
                    entity.getDisplayName(), currentAmmoType.getAmmoType()));
            return Collections.emptyList();
        }

        List<AmmoType> ammoTypes = new ArrayList<>();
        for (AmmoType ammoType : munitions) {
            // this is an abbreviated version of setupMunitions in the CustomMechDialog
            // TODO : clan/IS limitations?

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

    /**
     * Returns the last file modified in a directory and all subdirectories
     * that conforms to a FilenameFilter
     * @param dir       direction name
     * @param filter    filter for the file's name
     * @return          the last file modified in that dir that fits the filter
     */
    public static @Nullable File lastFileModified(String dir, FilenameFilter filter) {
        File fl = new File(dir);
        long lastMod = Long.MIN_VALUE;
        File choice = null;

        File[] files = fl.listFiles(filter);
        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }

        // ok now we need to recursively search any subdirectories, so see if they contain more
        // recent files
        files = fl.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) {
                    continue;
                }

                File subFile = lastFileModified(file.getPath(), filter);
                if ((subFile != null) && (subFile.lastModified() > lastMod)) {
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
        for (MechSummary summary : MechSummaryCache.getInstance().getAllMechs()) {
            // If this isn't the same chassis, is our current unit, we continue
            if (!en.getChassis().equalsIgnoreCase(summary.getChassis())
                    || en.getModel().equalsIgnoreCase(summary.getModel())
                    || !summary.getUnitType().equals(UnitType.getTypeName(en.getUnitType()))) {
                continue;
            }

            // Weight of the two units must match or we continue, but BA weight gets checked
            // differently
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
            if (!summary.isCanon() && options.isAllowCanonRefitOnly()) {
                continue;
            }

            // If the unit doesn't meet the tech filter criteria we continue
            ITechnology techProg = UnitTechProgression.getProgression(summary, campaign.getTechFaction(), true);
            if (techProg == null) {
                // This should never happen unless there was an exception thrown when calculating the progression.
                // In such a case we will log it and take the least restrictive action, which is to let it through.
                LogManager.getLogger().warn("Could not determine tech progression for " + summary.getName()
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
        } else if ((entity1.getEngine().getRating() != entity2.getEngine().getRating())
                || (entity1.getEngine().getEngineType() != entity2.getEngine().getEngineType())
                || (entity1.getEngine().getFlags() != entity2.getEngine().getFlags())) {
            return false;
        } else if (entity1.getStructureType() != entity2.getStructureType()) {
            return false;
        }

        if (entity1 instanceof Mech) {
            if (((Mech) entity1).getCockpitType() != ((Mech) entity2).getCockpitType()) {
                return false;
            } else if (entity1.getGyroType() != entity2.getGyroType()) {
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
            if ((entity1.getArmorType(loc) != entity2.getArmorType(loc))
                    || (entity1.getOArmor(loc) != entity2.getOArmor(loc))) {
                return false;
            }
            // Go through the base entity and make a list of all fixed equipment in this location.
            for (int slot = 0; slot < entity1.getNumberOfCriticals(loc); slot++) {
                CriticalSlot crit = entity1.getCritical(loc, slot);
                if ((null != crit) && (crit.getType() == CriticalSlot.TYPE_EQUIPMENT)
                        && (null != crit.getMount())) {
                    if (!crit.getMount().isOmniPodMounted()) {
                        fixedEquipment.add(crit.getMount().getType());
                        if (null != crit.getMount2()) {
                            fixedEquipment.add(crit.getMount2().getType());
                        }
                    }
                }
            }
            // Go through the critical slots in this location for the second entity and remove all
            // fixed equipment from the list. If not found or something is left over, there is a
            // fixed equipment difference.
            for (int slot = 0; slot < entity2.getNumberOfCriticals(loc); slot++) {
                CriticalSlot crit = entity1.getCritical(loc, slot);
                if ((crit != null) && (crit.getType() == CriticalSlot.TYPE_EQUIPMENT)
                        && (crit.getMount() != null)) {
                    if (!crit.getMount().isOmniPodMounted()) {
                        if (!fixedEquipment.remove(crit.getMount().getType())) {
                            return false;
                        } else if ((crit.getMount2() != null)
                                && !fixedEquipment.remove(crit.getMount2().getType())) {
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
        if (roll <= 2) {
            return SkillType.EXP_ULTRA_GREEN;
        } else if (roll < 6) {
            return SkillType.EXP_GREEN;
        } else if (roll < 10) {
            return SkillType.EXP_REGULAR;
        } else if (roll < 12) {
            return SkillType.EXP_VETERAN;
        } else {
            return SkillType.EXP_ELITE;
        }
    }

    /**
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

    public static Map<CrewType, Collection<Person>> genRandomCrewWithCombinedSkill(Campaign c,
                                                                                   Unit u,
                                                                                   String factionCode) {
        Objects.requireNonNull(c);
        Objects.requireNonNull(u);
        Objects.requireNonNull(u.getEntity(), "Unit needs to have a valid Entity attached");
        Crew oldCrew = u.getEntity().getCrew();

        int numberPeopleGenerated = 0;
        List<Person> drivers = new ArrayList<>();
        List<Person> gunners = new ArrayList<>();
        List<Person> vesselCrew = new ArrayList<>();
        Person navigator = null;
        Person consoleCmdr = null;

        // If the entire crew is dead, we still want to generate them. This is because they might
        // not be truly dead - this will be the case for BA for example
        // Also, the user may choose to GM make them un-dead in the resolve scenario dialog

        // Generate solo crews
        if (u.usesSoloPilot()) {
            //region Solo Pilot
            Person p;
            if (u.getEntity() instanceof LandAirMech) {
                p = c.newPerson(PersonnelRole.LAM_PILOT, factionCode, oldCrew.getGender());
                p.addSkill(SkillType.S_PILOT_MECH, SkillType.getType(SkillType.S_PILOT_MECH).getTarget()
                        - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_MECH, SkillType.getType(SkillType.S_GUN_MECH).getTarget()
                        - oldCrew.getGunnery(), 0);
                p.addSkill(SkillType.S_PILOT_AERO, SkillType.getType(SkillType.S_PILOT_AERO).getTarget()
                        - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_AERO, SkillType.getType(SkillType.S_GUN_AERO).getTarget()
                        - oldCrew.getGunnery(), 0);
            } else if (u.getEntity() instanceof Mech) {
                p = c.newPerson(PersonnelRole.MECHWARRIOR, factionCode, oldCrew.getGender());
                p.addSkill(SkillType.S_PILOT_MECH, SkillType.getType(SkillType.S_PILOT_MECH).getTarget()
                        - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_MECH, SkillType.getType(SkillType.S_GUN_MECH).getTarget()
                        - oldCrew.getGunnery(), 0);
            } else if (u.getEntity() instanceof Aero) {
                p = c.newPerson(PersonnelRole.AEROSPACE_PILOT, factionCode, oldCrew.getGender());
                p.addSkill(SkillType.S_PILOT_AERO, SkillType.getType(SkillType.S_PILOT_AERO).getTarget()
                        - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_AERO, SkillType.getType(SkillType.S_GUN_AERO).getTarget()
                        - oldCrew.getGunnery(), 0);
            } else if (u.getEntity() instanceof ConvFighter) {
                p = c.newPerson(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT, factionCode, oldCrew.getGender());
                p.addSkill(SkillType.S_PILOT_JET, SkillType.getType(SkillType.S_PILOT_JET).getTarget()
                        - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_JET, SkillType.getType(SkillType.S_GUN_JET).getTarget()
                        - oldCrew.getPiloting(), 0);
            } else if (u.getEntity() instanceof Protomech) {
                p = c.newPerson(PersonnelRole.PROTOMECH_PILOT, factionCode, oldCrew.getGender());
                p.addSkill(SkillType.S_GUN_PROTO, SkillType.getType(SkillType.S_GUN_PROTO).getTarget()
                        - oldCrew.getGunnery(), 0);
            } else if (u.getEntity() instanceof VTOL) {
                p = c.newPerson(PersonnelRole.VTOL_PILOT, factionCode, oldCrew.getGender());
                p.addSkill(SkillType.S_PILOT_VTOL, SkillType.getType(SkillType.S_PILOT_VTOL).getTarget()
                        - oldCrew.getPiloting(), 0);
                p.addSkill(SkillType.S_GUN_VEE, SkillType.getType(SkillType.S_GUN_VEE).getTarget()
                        - oldCrew.getGunnery(), 0);
            } else {
                //assume tanker if we got here
                p = c.newPerson(PersonnelRole.GROUND_VEHICLE_DRIVER, factionCode, oldCrew.getGender());
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
                        p = c.newPerson(PersonnelRole.MECHWARRIOR, factionCode, oldCrew.getGender(slot));
                        p.addSkill(SkillType.S_PILOT_MECH, SkillType.getType(SkillType.S_PILOT_MECH).getTarget()
                                - oldCrew.getPiloting(slot), 0);
                        p.addSkill(SkillType.S_GUN_MECH, SkillType.getType(SkillType.S_GUN_MECH).getTarget()
                                - oldCrew.getGunnery(slot), 0);
                    } else if (u.getEntity() instanceof Aero) {
                        p = c.newPerson(PersonnelRole.AEROSPACE_PILOT, factionCode, oldCrew.getGender(slot));
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
                        p = c.newPerson(PersonnelRole.VESSEL_PILOT, factionCode,
                                oldCrew.getGender(numberPeopleGenerated));
                        p.addSkill(SkillType.S_PILOT_SPACE, randomSkillFromTarget(
                                SkillType.getType(SkillType.S_PILOT_SPACE).getTarget()
                                        - oldCrew.getPiloting()),
                                0);
                    } else if (u.getEntity() instanceof BattleArmor) {
                        p = c.newPerson(PersonnelRole.BATTLE_ARMOUR, factionCode,
                                oldCrew.getGender(numberPeopleGenerated));
                        p.addSkill(SkillType.S_GUN_BA, randomSkillFromTarget(
                                SkillType.getType(SkillType.S_GUN_BA).getTarget()
                                        - oldCrew.getGunnery()),
                                0);
                    } else if (u.getEntity() instanceof Infantry) {
                        p = c.newPerson(PersonnelRole.SOLDIER, factionCode,
                                oldCrew.getGender(numberPeopleGenerated));
                        p.addSkill(SkillType.S_SMALL_ARMS, randomSkillFromTarget(
                                SkillType.getType(SkillType.S_SMALL_ARMS).getTarget() - oldCrew.getGunnery()),
                                0);
                    } else if (u.getEntity() instanceof VTOL) {
                        p = c.newPerson(PersonnelRole.VTOL_PILOT, factionCode,
                                oldCrew.getGender(numberPeopleGenerated));
                        p.addSkill(SkillType.S_PILOT_VTOL, SkillType.getType(
                                SkillType.S_PILOT_VTOL).getTarget() - oldCrew.getPiloting(),
                                0);
                        p.addSkill(SkillType.S_GUN_VEE, SkillType.getType(
                                SkillType.S_GUN_VEE).getTarget() - oldCrew.getGunnery(),
                                0);
                    } else if (u.getEntity() instanceof Mech) {
                        p = c.newPerson(PersonnelRole.MECHWARRIOR, factionCode,
                                oldCrew.getGender(numberPeopleGenerated));
                        p.addSkill(SkillType.S_PILOT_MECH, SkillType.getType(
                                SkillType.S_PILOT_MECH).getTarget() - oldCrew.getPiloting(),
                                0);
                        p.addSkill(SkillType.S_GUN_MECH, SkillType.getType(
                                SkillType.S_GUN_MECH).getTarget() - oldCrew.getGunnery(),
                                0);
                    } else {
                        //assume tanker if we got here
                        p = c.newPerson(PersonnelRole.GROUND_VEHICLE_DRIVER, factionCode,
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

                // Rebalance as needed to balance
                if (!drivers.isEmpty()) {
                    if (u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship) {
                        rebalanceCrew(oldCrew.getPiloting(), drivers, SkillType.S_PILOT_SPACE);
                    } else if (u.getEntity() instanceof BattleArmor) {
                        rebalanceCrew(oldCrew.getGunnery(), drivers, SkillType.S_GUN_BA);
                    } else if (u.getEntity() instanceof Infantry) {
                        rebalanceCrew(oldCrew.getGunnery(), drivers, SkillType.S_SMALL_ARMS);
                    }
                }

                if (!u.usesSoldiers()) {
                    // Generate gunners for multi-crew vehicles
                    for (int slot = 0; slot < u.getTotalGunnerNeeds(); slot++) {
                        Person p;
                        if (u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship) {
                            p = c.newPerson(PersonnelRole.VESSEL_GUNNER, factionCode,
                                    oldCrew.getGender(numberPeopleGenerated));
                            p.addSkill(SkillType.S_GUN_SPACE, randomSkillFromTarget(
                                    SkillType.getType(SkillType.S_GUN_SPACE).getTarget()
                                            - oldCrew.getGunnery()),
                                    0);
                        } else if (u.getEntity() instanceof Mech) {
                            p = c.newPerson(PersonnelRole.MECHWARRIOR, factionCode,
                                    oldCrew.getGender(numberPeopleGenerated));
                            p.addSkill(SkillType.S_PILOT_MECH,
                                    SkillType.getType(SkillType.S_PILOT_MECH).getTarget()
                                            - oldCrew.getPiloting(),
                                    0);
                            p.addSkill(SkillType.S_GUN_MECH,
                                    SkillType.getType(SkillType.S_GUN_MECH).getTarget()
                                            - oldCrew.getGunnery(),
                                    0);
                        } else {
                            //assume tanker if we got here
                            p = c.newPerson(PersonnelRole.VEHICLE_GUNNER, factionCode,
                                    oldCrew.getGender(numberPeopleGenerated));
                            p.addSkill(SkillType.S_GUN_VEE, randomSkillFromTarget(
                                    SkillType.getType(SkillType.S_GUN_VEE).getTarget()
                                            - oldCrew.getGunnery()), 0);
                        }

                        migrateCrewData(p, oldCrew, numberPeopleGenerated++, true);
                        gunners.add(p);
                    }

                    // Regenerate gunners as needed to balance
                    if (!gunners.isEmpty()) {
                        if (u.getEntity() instanceof Tank) {
                            rebalanceCrew(oldCrew.getGunnery(), gunners, SkillType.S_GUN_VEE);
                        } else if (u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship) {
                            rebalanceCrew(oldCrew.getGunnery(), gunners, SkillType.S_GUN_SPACE);
                        }
                    }
                }
            }

            for (int slot = 0; slot < u.getTotalCrewNeeds(); slot++) {
                Person p = c.newPerson(u.getEntity().isSupportVehicle()
                                ? PersonnelRole.VEHICLE_CREW : PersonnelRole.VESSEL_CREW,
                        factionCode, oldCrew.getGender(numberPeopleGenerated));

                migrateCrewData(p, oldCrew, numberPeopleGenerated++, false);
                vesselCrew.add(p);
            }

            if (u.canTakeNavigator()) {
                navigator = c.newPerson(PersonnelRole.VESSEL_NAVIGATOR, factionCode,
                        oldCrew.getGender(numberPeopleGenerated));
                migrateCrewData(navigator, oldCrew, numberPeopleGenerated++, false);
            }

            if (u.canTakeTechOfficer()) {
                consoleCmdr = c.newPerson(PersonnelRole.VEHICLE_GUNNER, factionCode,
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
     * Adjusts the skill levels of the given list of people in the given skill
     * until the average skill level matches the given desired skill level (desiredSkill)
     */
    private static void rebalanceCrew(int desiredSkill, List<Person> people, String skillType) {
        int totalGunnery = 0;
        int targetNum = SkillType.getType(skillType).getTarget();

        for (Person person : people) {
            totalGunnery += (targetNum - person.getSkill(skillType).getLevel());
        }

        int averageGunnery = (int) Math.round(((double) totalGunnery) / people.size());
        int skillIncrement = averageGunnery > desiredSkill ? 1 : -1;

        List<Person> eligiblePeople = new ArrayList<>(people);

        // instead of using a monte carlo method:
        // pick a random person from the crew, update their desired skill one point in
        // the direction we want to go. Eventually we will reach the desired skill we want.
        while (averageGunnery != desiredSkill) {
            Person person = ObjectUtility.getRandomItem(eligiblePeople);
            int skillLevel = person.getSkill(skillType).getLevel();

            // this is put in place to prevent skills from going below minimum or above maximum
            // we eliminate people from the group of that can have their skills changed
            // if they would do so.
            boolean skillCannotChange = true;
            while (skillCannotChange) {
                if ((skillLevel < 0) && (skillIncrement == -1) ||
                        (skillLevel >= SkillType.NUM_LEVELS) && (skillIncrement == 1)) {
                    eligiblePeople.remove(person);
                    person = ObjectUtility.getRandomItem(eligiblePeople);

                    // if we can't drop anyone's skill any lower or raise it any higher then forget it
                    if (person == null) {
                        return;
                    }

                    skillLevel = person.getSkill(skillType).getLevel();
                } else {
                    skillCannotChange = false;
                }
            }

            // this is counter-intuitive, but skills go from 0 (best) to 8 (worst)
            person.getSkill(skillType).setLevel(skillLevel + skillIncrement);
            totalGunnery -= skillIncrement;
            averageGunnery = (int) Math.round(((double) totalGunnery) / people.size());
        }
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

            if (StringUtility.isNullOrBlank(givenName)) {
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
            if (!oldCrew.getPortrait(crewIndex).isDefault()) {
                p.setPortrait(oldCrew.getPortrait(crewIndex).clone());
            }
        }
    }

    /**
     * Worker function that takes the PersonnelOptions (SPAs, in other words) from the given
     * "old crew" and sets them for a person.
     *
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
        switch (expLevel) {
            case SkillType.EXP_REGULAR:
                ndice = 2;
                break;
            case SkillType.EXP_VETERAN:
                ndice = 3;
                break;
            case SkillType.EXP_ELITE:
                ndice = 4;
                break;
            default:
                break;
        }

        int age = baseage;
        while (ndice > 0) {
            int roll = Compute.d6();
            // reroll all sixes once
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
        name = name.replaceAll("\\(.+?\\)", "");
        return name;
    }

    public static String printMoneyArray(Money... array) {
        StringJoiner joiner = new StringJoiner(",");
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
        switch (level) {
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

    /**
     * Copied an existing file into a new file
     * @param inFile the existing input file
     * @param outFile the new file to copy into
     * @see <a href="http://www.roseindia.net/java/beginners/copyfile.shtml">Rose India's tutorial</a>
     * for the original code source
     */
    public static void copyfile(final File inFile, final File outFile) {
        try (FileInputStream fis = new FileInputStream(inFile);
             FileOutputStream fos = new FileOutputStream(outFile)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = fis.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            LogManager.getLogger().info(String.format("Copied file %s to file %s", inFile.getPath(), outFile.getPath()));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    /**
     * Export a JTable to a CSV file
     * @param table     the table to save to csv
     * @param file      the file to save to
     * @return a csv formatted export of the table
     */
    public static String exportTableToCSV(JTable table, File file) {
        TableModel model = table.getModel();
        String[] columns = new String[model.getColumnCount()];
        for (int i = 0; i < model.getColumnCount(); i++) {
            columns[i] = model.getColumnName(i);
        }

        String report;
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(file.getPath()));
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(columns))) {
            for (int i = 0; i < model.getRowCount(); i++) {
                Object[] toWrite = new String[model.getColumnCount()];
                for (int j = 0; j < model.getColumnCount(); j++) {
                    Object value = model.getValueAt(i, j);
                    // use regex to remove any HTML tags
                    toWrite[j] = (value != null) ? value.toString().replaceAll("<[^>]*>", "") : "";
                }
                csvPrinter.printRecord(toWrite);
            }

            csvPrinter.flush();

            report = model.getRowCount() + " " + resourceMap.getString("RowsWritten.text");
        } catch (Exception ioe) {
            LogManager.getLogger().error("Error exporting JTable", ioe);
            report = "Error exporting JTable. See log for details.";
        }
        return report;
    }

    public static Vector<String> splitString(String str, String sep) {
        StringTokenizer st = new StringTokenizer(str, sep);
        Vector<String> output = new Vector<>();
        while (st.hasMoreTokens()) {
            output.add(st.nextToken());
        }
        return output;
    }

    public static String combineString(Collection<String> vec, String sep) {
        if ((null == vec) || (null == sep)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String part : vec) {
            if (first) {
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
        if ((null == str) || str.isEmpty()) {
            return str;
        }
        final char[] buffer = str.toCharArray();
        boolean capitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (Character.isWhitespace(ch)) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch);
                capitalizeNext = false;
            }
        }
        return new String(buffer);
    }

    public static String getRomanNumeralsFromArabicNumber(int level, boolean checkZero) {
        // If we're 0, then we just return an empty string
        if (checkZero && level == 0) {
            return "";
        }

        // Roman numeral, prepended with a space for display purposes
        StringBuilder roman = new StringBuilder(" ");
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
        if (highFirst) {
            ListIterator<Map.Entry<String, Integer>> li = list.listIterator(list.size());
            while (li.hasPrevious()) {
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
        if (!en.canEscape()) {
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
                            LogManager.getLogger().error("Exception trying to parse " + file.getPath() + " - ignoring.", ex);
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
     * @param loadDropShips - Should DropShip units be loaded?
     * @param loadSmallCraft - Should Small Craft units be loaded?
     * @param loadFighters - Should aero type units be loaded?
     * @param loadGround - should ground units be loaded?
     */
    public static void loadPlayerTransports(int trnId, Set<Integer> toLoad, Client client,
                                            boolean loadDropShips, boolean loadSmallCraft,
                                            boolean loadFighters, boolean loadGround) {
        if (!loadDropShips && !loadSmallCraft && !loadFighters && !loadGround) {
            // Nothing to do. Get outta here!
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
            if (!transport.canLoad(cargo, false) || (cargo.getTargetBay() == -1)) {
                continue;
            }

            // And now load the units
            if (cargo.getUnitType() == UnitType.DROPSHIP) {
                if (loadDropShips) {
                    sendLoadEntity(client, id, trnId, cargo);
                }
            } else if (cargo.getUnitType() == UnitType.SMALL_CRAFT) {
                if (loadSmallCraft) {
                    sendLoadEntity(client, id, trnId, cargo);
                }
            } else if (cargo.isFighter()) {
                if (loadFighters) {
                    sendLoadEntity(client, id, trnId, cargo);
                }
            } else if (loadGround) {
                sendLoadEntity(client, id, trnId, cargo);
            }
        }
    }

    private static void sendLoadEntity(Client client, int id, int trnId, Entity cargo) {
        client.sendLoadEntity(id, trnId, cargo.getTargetBay());
        // Add a wait to make sure that we don't start processing client.sendLoadEntity out of order
        try {
            Thread.sleep(500);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
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
        if (cargo.getUnitType() == UnitType.DROPSHIP) {
            for (final DockingCollar dockingCollar : transport.getDockingCollars()) {
                if (dockingCollar.canLoad(cargo)) {
                    return dockingCollar.getCollarNumber();
                }
            }
        } if (cargo.getUnitType() == UnitType.SMALL_CRAFT) {
            for (Bay b : transport.getTransportBays()) {
                if ((b instanceof SmallCraftBay) && b.canLoad(cargo)) {
                    // Load 1 unit into the bay
                    b.setCurrentSpace(1);
                    return b.getBayNumber();
                }
            }
        } else if (cargo.isFighter()) {
            // Try to load ASF bays first, so as not to hog SC bays
            for (Bay b : transport.getTransportBays()) {
                if ((b instanceof ASFBay) && b.canLoad(cargo)) {
                    // Load 1 unit into the bay
                    b.setCurrentSpace(1);
                    return b.getBayNumber();
                }
            }

            for (Bay b : transport.getTransportBays()) {
                if ((b instanceof SmallCraftBay) && b.canLoad(cargo)) {
                    // Load 1 unit into the bay
                    b.setCurrentSpace(1);
                    return b.getBayNumber();
                }
            }
        } else if (cargo.getUnitType() == UnitType.TANK) {
            // Try to fit lighter tanks into smaller bays first
            for (Bay b : transport.getTransportBays()) {
                if ((b instanceof LightVehicleBay) && b.canLoad(cargo)) {
                    // Load 1 unit into the bay
                    b.setCurrentSpace(1);
                    return b.getBayNumber();
                }
            }

            for (Bay b : transport.getTransportBays()) {
                if ((b instanceof HeavyVehicleBay) && b.canLoad(cargo)) {
                    // Load 1 unit into the bay
                    b.setCurrentSpace(1);
                    return b.getBayNumber();
                }
            }

            for (Bay b : transport.getTransportBays()) {
                if ((b instanceof SuperHeavyVehicleBay) && b.canLoad(cargo)) {
                    // Load 1 unit into the bay
                    b.setCurrentSpace(1);
                    return b.getBayNumber();
                }
            }
        } else if (cargo.getUnitType() == UnitType.INFANTRY) {
            for (Bay b : transport.getTransportBays()) {
                if ((b instanceof InfantryBay) && b.canLoad(cargo)) {
                    // Update bay tonnage based on platoon/squad weight
                    b.setCurrentSpace(b.spaceForUnit(cargo));
                    return b.getBayNumber();
                }
            }
        } else {
            // Just return the first available bay
            for (Bay b : transport.getTransportBays()) {
                if (b.canLoad(cargo)) {
                    // Load 1 unit into the bay
                    b.setCurrentSpace(1);
                    return b.getBayNumber();
                }
            }
        }

        // Shouldn't happen
        return -1;
    }

    /**
     * Testable function to get the original unit based on information from a new unit
     * @param newE new Entity we want to read information from
     * @return MechSummary that most closely represents the original of the new Entity
     * @throws EntityLoadingException
     */
    public static MechSummary retrieveOriginalUnit(Entity newE) throws EntityLoadingException {
        MechSummaryCache cacheInstance = MechSummaryCache.getInstance();
        cacheInstance.loadMechData();

        // I need to change the new entity to the one from the mtf file now, so that equipment numbers will match
        MechSummary summary = cacheInstance.getMech(newE.getFullChassis() + " " + newE.getModel());

        if (null == summary) {
            // Attempt to deal with new naming convention directly
            summary = cacheInstance.getMech(
                    newE.getChassis() + " (" + newE.getClanChassisName() + ") " + newE.getModel());
        }

        // If we got this far with no summary loaded, give up
        if (null == summary) {
            throw new EntityLoadingException(String.format("Could not load %s %s from the mech cache",
                    newE.getChassis(), newE.getModel()));
        }

        return summary;
    }

    public static List<String> generateEntityStub(List<Entity> entities) {
        List<String> stub = new ArrayList<>();
        for (Entity en : entities) {
            if (null == en) {
                stub.add("<html><font color='red'>No random assignment table found for faction</font></html>");
            } else {
                stub.add("<html>" + en.getCrew().getName() + " (" +
                        en.getCrew().getGunnery() + "/" +
                        en.getCrew().getPiloting() + "), " +
                        "<i>" + en.getShortName() + "</i>" +
                        "</html>");
            }
        }
        return stub;
    }

    /**
     * Display a descriptive character string for the deployment parameters in an object that implements IPlayerSettings
     * @param player object that implements IPlayerSettings
     * @return A character string
     */
    public static String getDeploymentString(Player player) {
        StringBuilder result = new StringBuilder("");

        if(player.getStartingPos() >=0
                && player.getStartingPos() <= IStartingPositions.START_LOCATION_NAMES.length) {
            result.append(IStartingPositions.START_LOCATION_NAMES[player.getStartingPos()]);
        }

        if (player.getStartingPos() == 0) {
            int NWx = player.getStartingAnyNWx() + 1;
            int NWy = player.getStartingAnyNWy() + 1;
            int SEx = player.getStartingAnySEx() + 1;
            int SEy = player.getStartingAnySEy() + 1;
            if ((NWx + NWy + SEx + SEy) > 0) {
                result.append(" (" + NWx + ", " + NWy + ")-(" + SEx + ", " + SEy + ")");
            }
        }
        int so = player.getStartOffset();
        int sw = player.getStartWidth();
        if ((so != 0) || (sw != 3)) {
            result.append(", " + so);
            result.append(", " + sw);
        }

        return result.toString();
    }

    public static String getDeploymentString(IPlayerSettings settings) {
        return getDeploymentString(createPlayer(settings));
    }

    /**
     * Create a Player object from IPlayerSettings parameters. Useful for tracking these variables in dialogs.
     * @param settings an object that implements IPlayerSettings
     * @return A Player object
     */
    public static Player createPlayer(IPlayerSettings settings) {
        Player p = new Player(1, "fake");
        p.setStartingPos(settings.getStartingPos());
        p.setStartWidth(settings.getStartWidth());
        p.setStartOffset(settings.getStartOffset());
        p.setStartingAnyNWx(settings.getStartingAnyNWx());
        p.setStartingAnyNWy(settings.getStartingAnyNWy());
        p.setStartingAnySEx(settings.getStartingAnySEx());
        p.setStartingAnySEy(settings.getStartingAnySEy());

        return p;
    }

    /**
     * Update values of an object that implements IPlayerSettings from a player object
     * @param settings An object that implements IPlayerSettings
     * @param player A Player object from which to read values
     */
    public static void updatePlayerSettings(IPlayerSettings settings, Player player) {
        settings.setStartingPos(player.getStartingPos());
        settings.setStartWidth(player.getStartWidth());
        settings.setStartOffset(player.getStartOffset());
        settings.setStartingAnyNWx(player.getStartingAnyNWx());
        settings.setStartingAnyNWy(player.getStartingAnyNWy());
        settings.setStartingAnySEx(player.getStartingAnySEx());
        settings.setStartingAnySEy(player.getStartingAnySEy());

    }
}
