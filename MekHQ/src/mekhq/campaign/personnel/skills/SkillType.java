/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.campaign.personnel.skills;

import static mekhq.campaign.personnel.skills.enums.SkillAttribute.CHARISMA;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.DEXTERITY;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.INTELLIGENCE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.NONE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.REFLEXES;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.STRENGTH;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.WILLPOWER;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_GUNNERY;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_PILOTING;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.ROLEPLAY_ART;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.ROLEPLAY_GENERAL;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.ROLEPLAY_INTEREST;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.ROLEPLAY_SCIENCE;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.ROLEPLAY_SECURITY;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.SUPPORT;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.SUPPORT_COMMAND;
import static mekhq.utilities.ReportingUtilities.messageSurroundedBySpanWithColor;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import megamek.codeUtilities.MathUtility;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.ConvFighter;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.ProtoMek;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillSubType;
import mekhq.utilities.MHQXMLUtility;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Skill type will hold static information for each skill type like base target number, whether to count up, and XP
 * costs for advancement.
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class SkillType {
    private static final MMLogger logger = MMLogger.create(SkillType.class);

    // combat skills
    public static final String S_PILOT_MEK = "Piloting/Mek";
    public static final String S_PILOT_AERO = "Piloting/Aerospace";
    public static final String S_PILOT_JET = "Piloting/Aircraft";
    public static final String S_PILOT_GVEE = "Piloting/Ground Vehicle";
    public static final String S_PILOT_VTOL = "Piloting/VTOL";
    public static final String S_PILOT_NVEE = "Piloting/Naval";
    public static final String S_PILOT_SPACE = "Piloting/Spacecraft";
    public static final String S_GUN_MEK = "Gunnery/Mek";
    public static final String S_GUN_AERO = "Gunnery/Aerospace";
    public static final String S_GUN_JET = "Gunnery/Aircraft";
    public static final String S_GUN_VEE = "Gunnery/Vehicle";
    public static final String S_GUN_SPACE = "Gunnery/Spacecraft";
    public static final String S_GUN_BA = "Gunnery/BattleArmor";
    public static final String S_GUN_PROTO = "Gunnery/ProtoMek";
    public static final String S_ARTILLERY = "Artillery";
    public static final String S_SMALL_ARMS = "Small Arms";
    public static final String S_ANTI_MEK = "Anti-Mek";

    // support skills
    public static final String S_TECH_MEK = "Tech/Mek";
    public static final String S_TECH_MECHANIC = "Tech/Mechanic";
    public static final String S_TECH_AERO = "Tech/Aero";
    public static final String S_TECH_BA = "Tech/BattleArmor";
    public static final String S_TECH_VESSEL = "Tech/Vessel";
    public static final String S_ASTECH = "Astech";
    public static final String S_DOCTOR = "Doctor";
    public static final String S_MEDTECH = "MedTech";
    public static final String S_NAV = "Hyperspace Navigation";
    public static final String S_ADMIN = "Administration";
    public static final String S_NEG = "Negotiation";
    public static final String S_LEADER = "Leadership";
    public static final String S_SCROUNGE = "Scrounge";
    public static final String S_STRATEGY = "Strategy";
    public static final String S_TACTICS = "Tactics";

    // roleplay skills
    public static final String S_ACROBATICS = "Acrobatics (RP Only)";
    public static final String S_ACTING = "Acting (RP Only)";
    public static final String S_ANIMAL_HANDLING = "Animal Handling (RP Only)";
    public static final String S_APPRAISAL = "Appraisal (RP Only)";
    public static final String S_ARCHERY = "Archery (RP Only)";
    public static final String S_ART_DANCING = "Art/Dancing (RP Only)";
    public static final String S_ART_DRAWING = "Art/Drawing (RP Only)";
    public static final String S_ART_PAINTING = "Art/Painting (RP Only)";
    public static final String S_ART_WRITING = "Art/Writing (RP Only)";
    public static final String S_CLIMBING = "Climbing (RP Only)";
    public static final String S_COMMUNICATIONS = "Communications (RP Only)";
    public static final String S_COMPUTERS = "Computers (RP Only)";
    public static final String S_CRYPTOGRAPHY = "Cryptography (RP Only)";
    public static final String S_DEMOLITIONS = "Demolitions (RP Only)";
    public static final String S_DISGUISE = "Disguise (RP Only)";
    public static final String S_ESCAPE_ARTIST = "Escape Artist (RP Only)";
    public static final String S_FORGERY = "Forgery (RP Only)";
    public static final String S_INTEREST_HISTORY = "Interest/History (RP Only)";
    public static final String S_INTEREST_LITERATURE = "Interest/Literature (RP Only)";
    public static final String S_INTEREST_HOLO_GAMES = "Interest/Holo-Games (RP Only)";
    public static final String S_INTEREST_SPORTS = "Interest/Sports (RP Only)";
    public static final String S_INTERROGATION = "Interrogation (RP Only)";
    public static final String S_INVESTIGATION = "Investigation (RP Only)";
    public static final String S_LANGUAGES = "Languages (RP Only)";
    public static final String S_MARTIAL_ARTS = "Martial Arts (RP Only)";
    public static final String S_PERCEPTION = "Perception (RP Only)";
    public static final String S_SLEIGHT_OF_HAND = "Sleight of Hand (RP Only)";
    public static final String S_PROTOCOLS = "Protocols (RP Only)";
    public static final String S_SCIENCE_BIOLOGY = "Science/Biology (RP Only)";
    public static final String S_SCIENCE_CHEMISTRY = "Science/Chemistry (RP Only)";
    public static final String S_SCIENCE_MATHEMATICS = "Science/Mathematics (RP Only)";
    public static final String S_SCIENCE_PHYSICS = "Science/Physics (RP Only)";
    public static final String S_SECURITY_SYSTEMS_ELECTRONIC = "Security Systems/Electronic (RP Only)";
    public static final String S_SCIENCE_SYSTEMS_MECHANICAL = "Security Systems/Mechanical (RP Only)";
    public static final String S_SENSOR_OPERATIONS = "Sensor Operations (RP Only)";
    public static final String S_STEALTH = "Stealth (RP Only)";
    public static final String S_STREETWISE = "Streetwise (RP Only)";
    public static final String S_SURVIVAL = "Survival (RP Only)";
    public static final String S_TRACKING = "Tracking (RP Only)";
    public static final String S_TRAINING = "Training (RP Only)";

    public static final int NUM_LEVELS = 11;

    public static final String[] skillList = { S_PILOT_MEK, S_GUN_MEK, S_PILOT_AERO, S_GUN_AERO, S_PILOT_GVEE,
                                               S_PILOT_VTOL, S_PILOT_NVEE, S_GUN_VEE, S_PILOT_JET, S_GUN_JET,
                                               S_PILOT_SPACE, S_GUN_SPACE, S_ARTILLERY, S_GUN_BA, S_GUN_PROTO,
                                               S_SMALL_ARMS, S_ANTI_MEK, S_TECH_MEK, S_TECH_MECHANIC, S_TECH_AERO,
                                               S_TECH_BA, S_TECH_VESSEL, S_ASTECH, S_DOCTOR, S_MEDTECH, S_NAV, S_ADMIN,
                                               S_TACTICS, S_STRATEGY, S_NEG, S_LEADER, S_SCROUNGE, S_ACROBATICS,
                                               S_ACTING, S_ANIMAL_HANDLING, S_APPRAISAL, S_ARCHERY, S_ART_DANCING,
                                               S_ART_DRAWING, S_ART_PAINTING, S_ART_WRITING, S_CLIMBING,
                                               S_COMMUNICATIONS, S_COMPUTERS, S_CRYPTOGRAPHY, S_DEMOLITIONS, S_DISGUISE,
                                               S_ESCAPE_ARTIST, S_FORGERY, S_INTEREST_HISTORY, S_INTEREST_LITERATURE,
                                               S_INTEREST_HOLO_GAMES, S_INTEREST_SPORTS, S_INTERROGATION,
                                               S_INVESTIGATION, S_LANGUAGES, S_MARTIAL_ARTS, S_PERCEPTION,
                                               S_SLEIGHT_OF_HAND, S_PROTOCOLS, S_SCIENCE_BIOLOGY, S_SCIENCE_CHEMISTRY,
                                               S_SCIENCE_MATHEMATICS, S_SCIENCE_PHYSICS, S_SECURITY_SYSTEMS_ELECTRONIC,
                                               S_SCIENCE_SYSTEMS_MECHANICAL, S_SENSOR_OPERATIONS, S_STEALTH,
                                               S_STREETWISE, S_SURVIVAL, S_TRACKING, S_TRAINING };


    public static Map<String, SkillType> lookupHash;

    public static final int SKILL_NONE = 0;

    public static final int EXP_NONE = -1;
    public static final int EXP_ULTRA_GREEN = 0;
    public static final int EXP_GREEN = 1;
    public static final int EXP_REGULAR = 2;
    public static final int EXP_VETERAN = 3;
    public static final int EXP_ELITE = 4;

    private String name;
    private int target;
    private boolean countUp;
    private SkillSubType subType;
    private SkillAttribute firstAttribute;
    private SkillAttribute secondAttribute;
    private int greenLvl;
    private int regLvl;
    private int vetLvl;
    private int eliteLvl;
    private Integer[] costs;

    /**
     * @param level skill level integer to get name for
     *
     * @return String skill name
     */
    public static String getExperienceLevelName(int level) {
        return switch (level) {
            case EXP_ULTRA_GREEN -> "Ultra-Green";
            case EXP_GREEN -> "Green";
            case EXP_REGULAR -> "Regular";
            case EXP_VETERAN -> "Veteran";
            case EXP_ELITE -> "Elite";
            case -1 -> "Unknown";
            default -> "Impossible";
        };
    }

    /**
     * @param level skill level integer to get color for
     *
     * @return String hex code for a font tag
     */
    public static String getExperienceLevelColor(int level) {
        return switch (level) {
            case EXP_ULTRA_GREEN -> MekHQ.getMHQOptions().getFontColorSkillUltraGreenHexColor();
            case EXP_GREEN -> MekHQ.getMHQOptions().getFontColorSkillGreenHexColor();
            case EXP_REGULAR -> MekHQ.getMHQOptions().getFontColorSkillRegularHexColor();
            case EXP_VETERAN -> MekHQ.getMHQOptions().getFontColorSkillVeteranHexColor();
            case EXP_ELITE -> MekHQ.getMHQOptions().getFontColorSkillEliteHexColor();
            default -> "";
        };
    }

    /**
     * @param level SkillLevel enum to get color for
     *
     * @return String hex code for a font tag
     */
    public static String getExperienceLevelColor(SkillLevel level) {
        return switch (level) {
            case ULTRA_GREEN -> MekHQ.getMHQOptions().getFontColorSkillUltraGreenHexColor();
            case GREEN -> MekHQ.getMHQOptions().getFontColorSkillGreenHexColor();
            case REGULAR -> MekHQ.getMHQOptions().getFontColorSkillRegularHexColor();
            case VETERAN -> MekHQ.getMHQOptions().getFontColorSkillVeteranHexColor();
            case ELITE -> MekHQ.getMHQOptions().getFontColorSkillEliteHexColor();
            case HEROIC -> MekHQ.getMHQOptions().getFontColorSkillEliteHexColor();
            case LEGENDARY -> MekHQ.getMHQOptions().getFontColorSkillEliteHexColor();
            default -> "";
        };
    }

    /**
     * @deprecated was previously used by Campaign Options, now unused.
     *
     *       <p>Use {@link #isSubTypeOf(SkillSubType)} instead.</p>
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public static boolean isCombatSkill(SkillType skill) {
        return skill.isSubTypeOf(COMBAT_GUNNERY) || skill.isSubTypeOf(COMBAT_PILOTING);
    }

    /**
     * @param level - skill level integer to get tagged name for
     *
     * @return "Skillname" wrapped by coloring span or bare if no color exists
     */
    public static String getColoredExperienceLevelName(int level) {
        if (getExperienceLevelColor(level).isEmpty()) {
            return getExperienceLevelName(level);
        }

        return messageSurroundedBySpanWithColor(getExperienceLevelColor(level), getExperienceLevelName(level));
    }

    /**
     * @param level - SkillLevel enum to get tagged name for
     *
     * @return "Skillname" wrapped by coloring span or bare if no color exists
     */
    public static String getColoredExperienceLevelName(SkillLevel level) {
        if (getExperienceLevelColor(level).isEmpty()) {
            return level.toString();
        }

        return messageSurroundedBySpanWithColor(getExperienceLevelColor(level), level.toString());
    }


    public static void setSkillTypes(Map<String, SkillType> skills) {
        // we are going to cycle through all skills in case ones have been added since
        // this hash was created
        for (String name : skillList) {
            if (null != skills.get(name)) {
                lookupHash.put(name, skills.get(name));
            }
        }
    }

    public static Map<String, SkillType> getSkillHash() {
        return lookupHash;
    }

    public static void setSkillHash(final Map<String, SkillType> hash) {
        lookupHash = hash;
    }

    public static String[] getSkillList() {
        return skillList;
    }

    /** Creates new SkillType */
    public SkillType() {
        greenLvl = 1;
        regLvl = 3;
        vetLvl = 4;
        eliteLvl = 5;
        costs = new Integer[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
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

    /**
     * @deprecated replaced by {@link #isCountUp()}
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public boolean countUp() {
        return countUp;
    }

    public boolean isCountUp() {
        return countUp;
    }

    public SkillSubType getSubType() {
        return subType;
    }

    public boolean isSubTypeOf(SkillSubType subType) {
        return this.subType == subType;
    }

    public boolean isRoleplaySkill() {
        return this.subType == ROLEPLAY_GENERAL ||
                     this.subType == ROLEPLAY_ART ||
                     this.subType == ROLEPLAY_INTEREST ||
                     this.subType == ROLEPLAY_SCIENCE ||
                     this.subType == ROLEPLAY_SECURITY;
    }

    public SkillAttribute getFirstAttribute() {
        return firstAttribute;
    }

    public SkillAttribute getSecondAttribute() {
        return secondAttribute;
    }

    /**
     * Calculates the number of linked attributes.
     *
     * <p>This method checks the primary and secondary attributes to determine how many are valid (i.e., not {@code
     * null} and not {@code NONE}). It returns the total count of linked attributes.</p>
     *
     * @return the number of linked attributes, which can be 0, 1, or 2 depending on the validity of the attributes.
     */
    public int getLinkedAttributeCount() {
        int count = 0;
        count += (firstAttribute != null && firstAttribute != NONE) ? 1 : 0;
        count += (secondAttribute != null && secondAttribute != NONE) ? 1 : 0;
        return count;
    }

    public int getLevelFromExperience(int expLvl) {
        return switch (expLvl) {
            case EXP_REGULAR -> regLvl;
            case EXP_VETERAN -> vetLvl;
            case EXP_ELITE -> eliteLvl;
            case EXP_GREEN -> greenLvl;
            default ->
                // for ultra-green we take the midpoint between green and 0, rounding down.
                // If the user has set green as zero, then this will be the same
                  (int) Math.floor(greenLvl / 2.0);
        };
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
        if ((lvl > 10) || (lvl < 0)) {
            return -1;
        } else {
            return costs[lvl];
        }
    }

    /**
     * Retrieves the cost values associated with this skill type for different levels.
     *
     * @return an array of Integer representing the costs for each level of the skill.
     */
    public Integer[] getCosts() {
        return costs;
    }

    /** get the cost to acquire this skill at the given level from scratch **/
    public int getTotalCost(int lvl) {
        int totalCost = 0;
        for (int i = 0; i <= lvl; i++) {
            totalCost = totalCost + costs[i];
        }
        return totalCost;
    }

    /**
     * @return the maximum level of that skill (the last one not set to cost = -1, or 10)
     */
    public int getMaxLevel() {
        for (int lvl = 0; lvl < costs.length; ++lvl) {
            if (costs[lvl] < 0) {
                return lvl - 1;
            }
        }

        return costs.length - 1;
    }

    public static void setCost(String name, int cost, int lvl) {
        SkillType type = lookupHash.get(name);
        if ((name != null) && (lvl < 11)) {
            type.costs[lvl] = cost;
        }
    }

    /**
     * @deprecated use {@link #isSubTypeOf(SkillSubType)} instead.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public boolean isPiloting() {
        return name.equals(S_PILOT_MEK) ||
                     name.equals(S_PILOT_AERO) ||
                     name.equals(S_PILOT_GVEE) ||
                     name.equals(S_PILOT_VTOL) ||
                     name.equals(S_PILOT_NVEE) ||
                     name.equals(S_PILOT_JET) ||
                     name.equals(S_PILOT_SPACE);
    }

    /**
     * @deprecated use {@link #isSubTypeOf(SkillSubType)} instead.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public boolean isGunnery() {
        return name.equals(S_GUN_MEK) ||
                     name.equals(S_GUN_AERO) ||
                     name.equals(S_GUN_VEE) ||
                     name.equals(S_GUN_BA) ||
                     name.equals(S_SMALL_ARMS) ||
                     name.equals(S_GUN_JET) ||
                     name.equals(S_GUN_SPACE) ||
                     name.equals(S_GUN_PROTO) ||
                     name.equals(S_ARTILLERY);
    }

    public int getExperienceLevel(int lvl) {
        if (lvl >= eliteLvl) {
            return EXP_ELITE;
        } else if (lvl >= vetLvl) {
            return EXP_VETERAN;
        } else if (lvl >= regLvl) {
            return EXP_REGULAR;
        } else if (lvl >= greenLvl) {
            return EXP_GREEN;
        } else {
            return EXP_ULTRA_GREEN;
        }
    }

    public static void initializeTypes() {
        lookupHash = new Hashtable<>();
        lookupHash.put(S_PILOT_MEK, createPilotingMek());
        lookupHash.put(S_GUN_MEK, createGunneryMek());
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
        lookupHash.put(S_GUN_PROTO, createGunneryProto());
        lookupHash.put(S_SMALL_ARMS, createSmallArms());
        lookupHash.put(S_ANTI_MEK, createAntiMek());
        lookupHash.put(S_TECH_MEK, createTechMek());
        lookupHash.put(S_TECH_MECHANIC, createTechMechanic());
        lookupHash.put(S_TECH_AERO, createTechAero());
        lookupHash.put(S_TECH_BA, createTechBA());
        lookupHash.put(S_TECH_VESSEL, createTechVessel());
        lookupHash.put(S_ASTECH, createAstech());
        lookupHash.put(S_DOCTOR, createDoctor());
        lookupHash.put(S_MEDTECH, createMedTech());
        lookupHash.put(S_NAV, createNav());
        lookupHash.put(S_TACTICS, createTactics());
        lookupHash.put(S_STRATEGY, createStrategy());
        lookupHash.put(S_ADMIN, createAdmin());
        lookupHash.put(S_LEADER, createLeadership());
        lookupHash.put(S_NEG, createNegotiation());
        lookupHash.put(S_SCROUNGE, createScrounge());
        lookupHash.put(S_ACROBATICS, createAcrobatics());
        lookupHash.put(S_ACTING, createActing());
        lookupHash.put(S_ANIMAL_HANDLING, createAnimalHandling());
        lookupHash.put(S_APPRAISAL, createAppraisal());
        lookupHash.put(S_ARCHERY, createArchery());
        lookupHash.put(S_ART_DANCING, createArtDancing());
        lookupHash.put(S_ART_DRAWING, createArtDrawing());
        lookupHash.put(S_ART_PAINTING, createArtPainting());
        lookupHash.put(S_ART_WRITING, createArtWriting());
        lookupHash.put(S_CLIMBING, createClimbing());
        lookupHash.put(S_COMMUNICATIONS, createCommunications());
        lookupHash.put(S_COMPUTERS, createComputers());
        lookupHash.put(S_CRYPTOGRAPHY, createCryptography());
        lookupHash.put(S_DEMOLITIONS, createDemolitions());
        lookupHash.put(S_DISGUISE, createDisguise());
        lookupHash.put(S_ESCAPE_ARTIST, createEscapeArtist());
        lookupHash.put(S_FORGERY, createForgery());
        lookupHash.put(S_INTEREST_HISTORY, createInterestHistory());
        lookupHash.put(S_INTEREST_LITERATURE, createInterestLiterature());
        lookupHash.put(S_INTEREST_HOLO_GAMES, createInterestHoloGames());
        lookupHash.put(S_INTEREST_SPORTS, createInterestSports());
        lookupHash.put(S_INTERROGATION, createInterrogation());
        lookupHash.put(S_INVESTIGATION, createInvestigation());
        lookupHash.put(S_LANGUAGES, createLanguages());
        lookupHash.put(S_MARTIAL_ARTS, createMartialArts());
        lookupHash.put(S_PERCEPTION, createPerception());
        lookupHash.put(S_SLEIGHT_OF_HAND, createSleightOfHand());
        lookupHash.put(S_PROTOCOLS, createProtocols());
        lookupHash.put(S_SCIENCE_BIOLOGY, createScienceBiology());
        lookupHash.put(S_SCIENCE_CHEMISTRY, createScienceChemistry());
        lookupHash.put(S_SCIENCE_MATHEMATICS, createScienceMathematics());
        lookupHash.put(S_SCIENCE_PHYSICS, createSciencePhysics());
        lookupHash.put(S_SECURITY_SYSTEMS_ELECTRONIC, createSecuritySystemsElectronic());
        lookupHash.put(S_SCIENCE_SYSTEMS_MECHANICAL, createSecuritySystemsMechanical());
        lookupHash.put(S_SENSOR_OPERATIONS, createSensorOperations());
        lookupHash.put(S_STEALTH, createStealth());
        lookupHash.put(S_STREETWISE, createStreetwise());
        lookupHash.put(S_SURVIVAL, createSurvival());
        lookupHash.put(S_TRACKING, createTracking());
        lookupHash.put(S_TRAINING, createTraining());

        // Remove below after Milestone Release post 0.49.19
        lookupHash.put("Piloting/Mech", createPilotingMek());
        lookupHash.put("Gunnery/Mech", createGunneryMek());
        lookupHash.put("Anti-Mech", createAntiMek());
        lookupHash.put("Tech/Mech", createTechMek());
        lookupHash.put("Gunnery/ProtoMech", createGunneryProto());
        lookupHash.put("Medtech", createMedTech());
        lookupHash.put("Gunnery/Battlesuit", createGunneryBA());
        lookupHash.put("Tech/BA", createTechBA());
    }

    public static @Nullable SkillType getType(String skillName) {
        return lookupHash.get(skillName);
    }

    public static String getDrivingSkillFor(Entity en) {
        if (en instanceof Tank) {
            return switch (en.getMovementMode()) {
                case VTOL -> S_PILOT_VTOL;
                case NAVAL, HYDROFOIL, SUBMARINE -> S_PILOT_NVEE;
                default -> S_PILOT_GVEE;
            };
        } else if ((en instanceof SmallCraft) || (en instanceof Jumpship)) {
            return S_PILOT_SPACE;
        } else if (en instanceof ConvFighter) {
            return S_PILOT_JET;
        } else if (en instanceof Aero) {
            return S_PILOT_AERO;
        } else if (en instanceof Infantry) {
            return S_ANTI_MEK;
        } else if (en instanceof ProtoMek) {
            return S_GUN_PROTO;
        } else {
            return S_PILOT_MEK;
        }
    }

    public static String getGunnerySkillFor(Entity en) {
        if (en instanceof Tank) {
            return S_GUN_VEE;
        } else if ((en instanceof SmallCraft) || (en instanceof Jumpship)) {
            return S_GUN_SPACE;
        } else if (en instanceof ConvFighter) {
            return S_GUN_JET;
        } else if (en instanceof Aero) {
            return S_GUN_AERO;
        } else if (en instanceof Infantry) {
            if (en instanceof BattleArmor) {
                return S_GUN_BA;
            } else {
                return S_SMALL_ARMS;
            }
        } else if (en instanceof ProtoMek) {
            return S_GUN_PROTO;
        } else {
            return S_GUN_MEK;
        }
    }

    public static List<SkillType> getRoleplaySkills() {
        List<SkillType> roleplaySkills = new ArrayList<>();
        List<SkillType> roleplaySkillsArt = new ArrayList<>();
        List<SkillType> roleplaySkillsInterest = new ArrayList<>();
        List<SkillType> roleplaySkillsScience = new ArrayList<>();
        List<SkillType> roleplaySkillsSecurity = new ArrayList<>();

        for (SkillType type : lookupHash.values()) {
            if (type.isSubTypeOf(SkillSubType.ROLEPLAY_GENERAL)) {
                roleplaySkills.add(type);
                continue;
            }

            if (type.isSubTypeOf(SkillSubType.ROLEPLAY_ART)) {
                roleplaySkillsArt.add(type);
                continue;
            }

            if (type.isSubTypeOf(SkillSubType.ROLEPLAY_INTEREST)) {
                roleplaySkillsInterest.add(type);
                continue;
            }

            if (type.isSubTypeOf(SkillSubType.ROLEPLAY_SCIENCE)) {
                roleplaySkillsScience.add(type);
                continue;
            }

            if (type.isSubTypeOf(SkillSubType.ROLEPLAY_SECURITY)) {
                roleplaySkillsSecurity.add(type);
            }
        }

        // These next few steps are so that we don't overweight skill specializations. Without this, the chances of
        // having a Science-related skill, for example, skyrocket and make those skills feel 'spammy'.
        if (!roleplaySkillsArt.isEmpty()) {
            roleplaySkills.add(ObjectUtility.getRandomItem(roleplaySkillsArt));
        }

        if (!roleplaySkillsInterest.isEmpty()) {
            roleplaySkills.add(ObjectUtility.getRandomItem(roleplaySkillsInterest));
        }

        if (!roleplaySkillsScience.isEmpty()) {
            roleplaySkills.add(ObjectUtility.getRandomItem(roleplaySkillsScience));
        }

        if (!roleplaySkillsSecurity.isEmpty()) {
            roleplaySkills.add(ObjectUtility.getRandomItem(roleplaySkillsSecurity));
        }

        return roleplaySkills;
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "skillType");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "name", name);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "target", target);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "isCountUp", countUp);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "subType", subType.toString());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "firstAttribute", firstAttribute.toString());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "secondAttribute", secondAttribute.toString());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "greenLvl", greenLvl);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "regLvl", regLvl);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "vetLvl", vetLvl);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "eliteLvl", eliteLvl);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "costs", StringUtils.join(costs, ','));
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "skillType");
    }

    /**
     * Generates an instance of {@link SkillType} from an XML node.
     *
     * @param workingNode The XML node containing the skill data.
     */
    public static void generateInstanceFromXML(Node workingNode) {
        try {
            SkillType skillType = new SkillType();
            NodeList nodeList = workingNode.getChildNodes();

            for (int x = 0; x < nodeList.getLength(); x++) {
                Node wn2 = nodeList.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    // skillType.name = wn2.getTextContent();

                    //Start <50.01 compatibility handler.
                    // The above code can be uncommented once these handlers have been removed
                    skillType.name = switch (wn2.getTextContent().toLowerCase()) {
                        case "piloting/mech" -> "Piloting/Mek";
                        case "gunnery/mech" -> "Gunnery/Mek";
                        case "gunnery/battlesuit" -> "Gunnery/BattleArmor";
                        case "gunnery/protomech" -> "Gunnery/ProtoMek";
                        case "anti-mech" -> "Anti-Mek";
                        case "tech/mech" -> "Tech/Mek";
                        case "tech/ba" -> "Tech/BattleArmor";
                        case "medtech" -> "MedTech";
                        default -> wn2.getTextContent();
                    };
                    //End <50.01 compatibility handler
                } else if (wn2.getNodeName().equalsIgnoreCase("target")) {
                    skillType.target = MathUtility.parseInt(wn2.getTextContent(), skillType.target);
                } else if (wn2.getNodeName().equalsIgnoreCase("greenLvl")) {
                    skillType.greenLvl = MathUtility.parseInt(wn2.getTextContent(), skillType.greenLvl);
                } else if (wn2.getNodeName().equalsIgnoreCase("regLvl")) {
                    skillType.regLvl = MathUtility.parseInt(wn2.getTextContent(), skillType.regLvl);
                } else if (wn2.getNodeName().equalsIgnoreCase("vetLvl")) {
                    skillType.vetLvl = MathUtility.parseInt(wn2.getTextContent(), skillType.vetLvl);
                } else if (wn2.getNodeName().equalsIgnoreCase("eliteLvl")) {
                    skillType.eliteLvl = MathUtility.parseInt(wn2.getTextContent(), skillType.eliteLvl);
                } else if (wn2.getNodeName().equalsIgnoreCase("isCountUp")) {
                    skillType.countUp = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("subType")) {
                    skillType.subType = SkillSubType.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("firstAttribute")) {
                    skillType.firstAttribute = SkillAttribute.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("secondAttribute")) {
                    skillType.secondAttribute = SkillAttribute.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("costs")) {
                    String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        skillType.costs[i] = MathUtility.parseInt(values[i], skillType.costs[i]);
                    }
                }
            }

            // <50.05 compatibility handler
            if (skillType.getSubType() == null ||
                      skillType.getFirstAttribute() == null ||
                      skillType.getSecondAttribute() == null) {
                compatibilityHandler(skillType);
            }

            lookupHash.put(skillType.name, skillType);
        } catch (Exception ex) {
            logger.error("", ex);
        }
    }

    public static void generateSeparateInstanceFromXML(final Node wn, final Map<String, SkillType> hash) {
        try {
            SkillType skillType = new SkillType();
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    skillType.name = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("target")) {
                    skillType.target = MathUtility.parseInt(wn2.getTextContent(), skillType.target);
                } else if (wn2.getNodeName().equalsIgnoreCase("greenLvl")) {
                    skillType.greenLvl = MathUtility.parseInt(wn2.getTextContent(), skillType.greenLvl);
                } else if (wn2.getNodeName().equalsIgnoreCase("regLvl")) {
                    skillType.regLvl = MathUtility.parseInt(wn2.getTextContent(), skillType.regLvl);
                } else if (wn2.getNodeName().equalsIgnoreCase("vetLvl")) {
                    skillType.vetLvl = MathUtility.parseInt(wn2.getTextContent(), skillType.vetLvl);
                } else if (wn2.getNodeName().equalsIgnoreCase("eliteLvl")) {
                    skillType.eliteLvl = MathUtility.parseInt(wn2.getTextContent(), skillType.eliteLvl);
                } else if (wn2.getNodeName().equalsIgnoreCase("isCountUp")) {
                    skillType.countUp = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("subType")) {
                    skillType.subType = SkillSubType.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("firstAttribute")) {
                    skillType.firstAttribute = SkillAttribute.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("secondAttribute")) {
                    skillType.secondAttribute = SkillAttribute.fromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("costs")) {
                    String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        skillType.costs[i] = MathUtility.parseInt(values[i], skillType.costs[i]);
                    }
                }
            }

            // <50.05 compatibility handler
            if (skillType.getSubType() == null ||
                      skillType.getFirstAttribute() == null ||
                      skillType.getSecondAttribute() == null) {
                compatibilityHandler(skillType);
            }

            hash.put(skillType.name, skillType);
        } catch (Exception ex) {
            logger.error("", ex);
        }
    }

    /**
     * Handles compatibility upgrades for outdated or incomplete {@link SkillType} definitions.
     *
     * <p>This method ensures that instances of {@link SkillType} are updated to meet current standards by:
     * assigning suitable subtypes and attributes based on predefined mappings. If the skill type is recognized as
     * outdated or missing attributes, the method creates a temporary reference skill and updates the original skill
     * instance. All changes are logged, and unrecognized skill types are flagged with an error log.</p>
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *     <li>Checks if the {@code skillType} is {@code null} and logs a message. If {@code null}, no processing is
     *     performed.</li>
     *     <li>Matches the name of the {@link SkillType} against predefined mappings to check for compatibility issues.</li>
     *     <li>Uses a factory method to create a temporary {@link SkillType} instance designed for the corresponding skill
     *     (e.g., {@code createPilotingMek()}, {@code createGunneryAero()}).</li>
     *     <li>Updates incomplete attributes (subtype, first attribute, or second attribute) on the original
     *     {@link SkillType} to match the temporary reference. Logs the updates for traceability.</li>
     *     <li>If the skill type name is not recognized, logs an error and halts further processing.</li>
     * </ul>
     *
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *     <li>If the skill type is {@code null}, logs a warning and skips the update process.</li>
     *     <li>If the skill type name is invalid or unmapped, logs an error with the message
     *     "Unexpected value in compatibilityHandler".</li>
     * </ul>
     *
     * @param skillType the {@link SkillType} instance to be checked and updated for compatibility. If the
     *                  {@code skillType} is {@code null}, no action is performed.
     */
    private static void compatibilityHandler(SkillType skillType) {
        if (skillType == null) {
            logger.info("SkillType is null, unable to update compatibility. " +
                              "This suggests a deeper issue and should be reported.");
            return;
        }

        SkillType temporarySkillType = switch (skillType.getName()) {
            case S_PILOT_MEK, "Piloting/Mech" -> createPilotingMek();
            case S_GUN_MEK, "Gunnery/Mech" -> createGunneryMek();
            case S_PILOT_AERO -> createPilotingAero();
            case S_GUN_AERO -> createGunneryAero();
            case S_PILOT_JET -> createPilotingJet();
            case S_GUN_JET -> createGunneryJet();
            case S_PILOT_SPACE -> createPilotingSpace();
            case S_GUN_SPACE -> createGunnerySpace();
            case S_PILOT_GVEE -> createPilotingGroundVee();
            case S_PILOT_NVEE -> createPilotingNavalVee();
            case S_PILOT_VTOL -> createPilotingVTOL();
            case S_GUN_VEE -> createGunneryVehicle();
            case S_ARTILLERY -> createArtillery();
            case S_GUN_BA, "Gunnery/Battlesuit" -> createGunneryBA();
            case S_GUN_PROTO, "Gunnery/ProtoMech" -> createGunneryProto();
            case S_SMALL_ARMS -> createSmallArms();
            case S_ANTI_MEK, "Anti-Mech" -> createAntiMek();
            case S_TECH_MEK, "Tech/Mech" -> createTechMek();
            case S_TECH_MECHANIC -> createTechMechanic();
            case S_TECH_AERO -> createTechAero();
            case S_TECH_BA, "Tech/BA" -> createTechBA();
            case S_TECH_VESSEL -> createTechVessel();
            case S_ASTECH -> createAstech();
            case S_DOCTOR -> createDoctor();
            case S_MEDTECH, "Medtech" -> createMedTech();
            case S_NAV -> createNav();
            case S_ADMIN -> createAdmin();
            case S_NEG -> createNegotiation();
            case S_LEADER -> createLeadership();
            case S_SCROUNGE -> createScrounge();
            case S_STRATEGY -> createStrategy();
            case S_TACTICS -> createTactics();
            case S_ACROBATICS -> createAcrobatics();
            case S_ACTING -> createActing();
            case S_ANIMAL_HANDLING -> createAnimalHandling();
            case S_APPRAISAL -> createAppraisal();
            case S_ARCHERY -> createArchery();
            case S_ART_DANCING -> createArtDancing();
            case S_ART_DRAWING -> createArtDrawing();
            case S_ART_PAINTING -> createArtPainting();
            case S_ART_WRITING -> createArtWriting();
            case S_CLIMBING -> createClimbing();
            case S_COMMUNICATIONS -> createCommunications();
            case S_COMPUTERS -> createComputers();
            case S_CRYPTOGRAPHY -> createCryptography();
            case S_DEMOLITIONS -> createDemolitions();
            case S_DISGUISE -> createDisguise();
            case S_ESCAPE_ARTIST -> createEscapeArtist();
            case S_FORGERY -> createForgery();
            case S_INTEREST_HISTORY -> createInterestHistory();
            case S_INTEREST_LITERATURE -> createInterestLiterature();
            case S_INTEREST_HOLO_GAMES -> createInterestHoloGames();
            case S_INTEREST_SPORTS -> createInterestSports();
            case S_INTERROGATION -> createInterrogation();
            case S_INVESTIGATION -> createInvestigation();
            case S_LANGUAGES -> createLanguages();
            case S_MARTIAL_ARTS -> createMartialArts();
            case S_PERCEPTION -> createPerception();
            case S_SLEIGHT_OF_HAND -> createSleightOfHand();
            case S_PROTOCOLS -> createProtocols();
            case S_SCIENCE_BIOLOGY -> createScienceBiology();
            case S_SCIENCE_CHEMISTRY -> createScienceChemistry();
            case S_SCIENCE_MATHEMATICS -> createScienceMathematics();
            case S_SCIENCE_PHYSICS -> createSciencePhysics();
            case S_SECURITY_SYSTEMS_ELECTRONIC -> createSecuritySystemsElectronic();
            case S_SCIENCE_SYSTEMS_MECHANICAL -> createSecuritySystemsMechanical();
            case S_SENSOR_OPERATIONS -> createSensorOperations();
            case S_STEALTH -> createStealth();
            case S_STREETWISE -> createStreetwise();
            case S_SURVIVAL -> createSurvival();
            case S_TRACKING -> createTracking();
            case S_TRAINING -> createTraining();
            default -> {
                logger.errorDialog("REPORT TO MEGAMEK TEAM",
                      "Unexpected value in compatibilityHandler: {}",
                      skillType.getName());
                yield null;
            }
        };

        if (temporarySkillType == null) {
            return;
        }

        if (skillType.getSubType() == null) {
            skillType.subType = temporarySkillType.getSubType();
            logger.info("SkillType {} has been updated to sub type {}",
                  skillType.getName(),
                  temporarySkillType.getSubType());
        }

        if (skillType.getFirstAttribute() == null) {
            skillType.firstAttribute = temporarySkillType.getFirstAttribute();
            logger.info("SkillType {} has been updated to first attribute {}",
                  skillType.getName(),
                  temporarySkillType.getFirstAttribute());
        }

        if (skillType.getSecondAttribute() == null) {
            skillType.secondAttribute = temporarySkillType.getSecondAttribute();
            logger.info("SkillType {} has been updated to second attribute {}",
                  skillType.getName(),
                  temporarySkillType.getSecondAttribute());
        }
    }


    public static SkillType createPilotingMek() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_MEK;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_PILOTING;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, -1, -1 };

        return skill;
    }

    public static SkillType createGunneryMek() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_MEK;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_GUNNERY;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, -1, -1, -1 };

        return skill;
    }

    public static SkillType createPilotingAero() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_AERO;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_PILOTING;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, -1, -1 };

        return skill;
    }

    public static SkillType createGunneryAero() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_AERO;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_GUNNERY;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, -1, -1, -1 };

        return skill;
    }

    public static SkillType createPilotingJet() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_JET;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_PILOTING;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, -1, -1 };

        return skill;
    }

    public static SkillType createGunneryJet() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_JET;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_GUNNERY;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, -1, -1, -1 };

        return skill;
    }

    public static SkillType createPilotingSpace() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_SPACE;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_PILOTING;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, -1, -1 };

        return skill;
    }

    public static SkillType createGunnerySpace() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_SPACE;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_GUNNERY;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, -1, -1, -1 };

        return skill;
    }

    public static SkillType createPilotingGroundVee() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_GVEE;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_PILOTING;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, -1, -1 };

        return skill;
    }

    public static SkillType createPilotingNavalVee() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_NVEE;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_PILOTING;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, -1, -1 };

        return skill;
    }

    public static SkillType createPilotingVTOL() {
        SkillType skill = new SkillType();
        skill.name = S_PILOT_VTOL;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_PILOTING;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, -1, -1 };

        return skill;
    }

    public static SkillType createGunneryVehicle() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_VEE;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_GUNNERY;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, -1, -1, -1 };

        return skill;
    }

    public static SkillType createArtillery() {
        SkillType skill = new SkillType();
        skill.name = S_ARTILLERY;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_GUNNERY;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, -1, -1, -1 };

        return skill;
    }

    public static SkillType createGunneryBA() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_BA;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_GUNNERY;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, -1, -1, -1 };

        return skill;
    }

    public static SkillType createGunneryProto() {
        SkillType skill = new SkillType();
        skill.name = S_GUN_PROTO;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_GUNNERY;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, -1, -1, -1 };

        return skill;
    }

    public static SkillType createSmallArms() {
        SkillType skill = new SkillType();
        skill.name = S_SMALL_ARMS;
        skill.target = 7;
        skill.greenLvl = 2;
        skill.countUp = false;
        skill.subType = COMBAT_GUNNERY;
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, -1, -1 };

        return skill;
    }

    public static SkillType createAntiMek() {
        SkillType skill = new SkillType();
        skill.name = S_ANTI_MEK;
        skill.target = 8;
        skill.greenLvl = 2;
        skill.countUp = false;
        // Anti-Mek is the 'piloting' skill of Conventional Infantry
        skill.subType = COMBAT_PILOTING;
        // Anti-Mek doesn't exist in ATOW so we use the linked attributes Demolitions
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 12, 6, 6, 6, 6, 6, 6, 6, 6, -1, -1 };

        return skill;
    }

    public static SkillType createTechMek() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_MEK;
        skill.target = 10;
        skill.countUp = false;
        skill.subType = SUPPORT;
        // This corresponds to the ATOW skill 'Technician'
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 12, 6, 0, 6, 6, 6, -1, -1, -1, -1, -1 };

        return skill;
    }

    public static SkillType createTechMechanic() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_MECHANIC;
        skill.target = 10;
        skill.countUp = false;
        skill.subType = SUPPORT;
        // This corresponds to the ATOW skill 'Technician'
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 12, 6, 0, 6, 6, 6, -1, -1, -1, -1, -1 };

        return skill;
    }

    public static SkillType createTechAero() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_AERO;
        skill.target = 10;
        skill.countUp = false;
        skill.subType = SUPPORT;
        // This corresponds to the ATOW skill 'Technician'
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 12, 6, 0, 6, 6, 6, -1, -1, -1, -1, -1 };

        return skill;
    }

    public static SkillType createTechBA() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_BA;
        skill.target = 10;
        skill.countUp = false;
        skill.subType = SUPPORT;
        // This corresponds to the ATOW skill 'Technician'
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 12, 6, 0, 6, 6, 6, -1, -1, -1, -1, -1 };

        return skill;
    }

    public static SkillType createTechVessel() {
        SkillType skill = new SkillType();
        skill.name = S_TECH_VESSEL;
        skill.target = 10;
        skill.countUp = false;
        skill.subType = SUPPORT;
        // This corresponds to the ATOW skill 'Technician'
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 12, 6, 0, 6, 6, 6, -1, -1, -1, -1, -1 };

        return skill;
    }

    public static SkillType createAstech() {
        SkillType skill = new SkillType();
        skill.name = S_ASTECH;
        skill.target = 10;
        skill.countUp = false;
        skill.subType = SUPPORT;
        // This doesn't correspond to an ATOW skill, so we went with INTELLIGENCE as the tech equivalent of MedTech
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 12, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };

        return skill;
    }

    public static SkillType createDoctor() {
        SkillType skill = new SkillType();
        skill.name = S_DOCTOR;
        skill.target = 11;
        skill.countUp = false;
        skill.subType = SUPPORT;
        // This corresponds to the ATOW skill 'Surgery'
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 16, 8, 0, 8, 8, 8, -1, -1, -1, -1, -1 };

        return skill;
    }

    public static SkillType createMedTech() {
        SkillType skill = new SkillType();
        skill.name = S_MEDTECH;
        skill.target = 11;
        skill.countUp = false;
        skill.subType = SUPPORT;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 16, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };

        return skill;
    }

    public static SkillType createNav() {
        SkillType skill = new SkillType();
        skill.name = S_NAV;
        skill.target = 8;
        skill.countUp = false;
        skill.subType = SUPPORT;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, -1, -1 };

        return skill;
    }

    public static SkillType createTactics() {
        SkillType skill = new SkillType();
        skill.name = S_TACTICS;
        skill.target = 0;
        skill.countUp = true;
        skill.subType = SUPPORT_COMMAND;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 12, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6 };

        return skill;
    }

    public static SkillType createStrategy() {
        SkillType skill = new SkillType();
        skill.name = S_STRATEGY;
        skill.target = 0;
        skill.countUp = true;
        skill.subType = SUPPORT_COMMAND;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 12, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6 };

        return skill;
    }

    public static SkillType createAdmin() {
        SkillType skill = new SkillType();
        skill.name = S_ADMIN;
        skill.target = 10;
        skill.countUp = false;
        skill.subType = SUPPORT;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 8, 4, 0, 4, 4, 4, -1, -1, -1, -1, -1 };

        return skill;
    }

    public static SkillType createLeadership() {
        SkillType skill = new SkillType();
        skill.name = S_LEADER;
        skill.target = 0;
        skill.countUp = true;
        skill.subType = SUPPORT_COMMAND;
        skill.firstAttribute = WILLPOWER;
        skill.secondAttribute = CHARISMA;
        skill.costs = new Integer[] { 12, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6 };

        return skill;
    }

    public static SkillType createNegotiation() {
        SkillType skill = new SkillType();
        skill.name = S_NEG;
        skill.target = 10;
        skill.countUp = false;
        skill.subType = SUPPORT;
        skill.firstAttribute = CHARISMA;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 };

        return skill;
    }

    public static SkillType createScrounge() {
        SkillType skill = new SkillType();
        skill.name = S_SCROUNGE;
        skill.target = 10;
        skill.countUp = false;
        skill.subType = SUPPORT;
        // This corresponds to the ATOW skill 'Negotiation'
        skill.firstAttribute = CHARISMA;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 };

        return skill;
    }

    public static SkillType createAcrobatics() {
        SkillType skill = new SkillType();
        skill.name = S_ACROBATICS;
        skill.target = 7;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createActing() {
        SkillType skill = new SkillType();
        skill.name = S_ACTING;
        skill.target = 8;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = CHARISMA;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createAnimalHandling() {
        SkillType skill = new SkillType();
        skill.name = S_ANIMAL_HANDLING;
        skill.target = 8;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = WILLPOWER;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createAppraisal() {
        SkillType skill = new SkillType();
        skill.name = S_APPRAISAL;
        skill.target = 8;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createArchery() {
        SkillType skill = new SkillType();
        skill.name = S_ARCHERY;
        skill.target = 7;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createArtDancing() {
        SkillType skill = new SkillType();
        skill.name = S_ART_DANCING;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_ART;
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createArtDrawing() {
        SkillType skill = new SkillType();
        skill.name = S_ART_DRAWING;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_ART;
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createArtPainting() {
        SkillType skill = new SkillType();
        skill.name = S_ART_PAINTING;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_ART;
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createArtWriting() {
        SkillType skill = new SkillType();
        skill.name = S_ART_WRITING;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_ART;
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createClimbing() {
        SkillType skill = new SkillType();
        skill.name = S_CLIMBING;
        skill.target = 7;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createCommunications() {
        SkillType skill = new SkillType();
        skill.name = S_COMMUNICATIONS;
        skill.target = 7;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createComputers() {
        SkillType skill = new SkillType();
        skill.name = S_COMPUTERS;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createCryptography() {
        SkillType skill = new SkillType();
        skill.name = S_CRYPTOGRAPHY;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createDemolitions() {
        SkillType skill = new SkillType();
        skill.name = S_DEMOLITIONS;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createDisguise() {
        SkillType skill = new SkillType();
        skill.name = S_DISGUISE;
        skill.target = 7;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = CHARISMA;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createEscapeArtist() {
        SkillType skill = new SkillType();
        skill.name = S_ESCAPE_ARTIST;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = STRENGTH;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createForgery() {
        SkillType skill = new SkillType();
        skill.name = S_FORGERY;
        skill.target = 8;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createInterestHistory() {
        SkillType skill = new SkillType();
        skill.name = S_INTEREST_HISTORY;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_INTEREST;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createInterestLiterature() {
        SkillType skill = new SkillType();
        skill.name = S_INTEREST_LITERATURE;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_INTEREST;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createInterestHoloGames() {
        SkillType skill = new SkillType();
        skill.name = S_INTEREST_HOLO_GAMES;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_INTEREST;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createInterestSports() {
        SkillType skill = new SkillType();
        skill.name = S_INTEREST_SPORTS;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_INTEREST;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createInterrogation() {
        SkillType skill = new SkillType();
        skill.name = S_INTERROGATION;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = WILLPOWER;
        skill.secondAttribute = CHARISMA;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createInvestigation() {
        SkillType skill = new SkillType();
        skill.name = S_INVESTIGATION;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createLanguages() {
        SkillType skill = new SkillType();
        skill.name = S_LANGUAGES;
        skill.target = 8;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = CHARISMA;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createMartialArts() {
        SkillType skill = new SkillType();
        skill.name = S_MARTIAL_ARTS;
        skill.target = 8;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createPerception() {
        SkillType skill = new SkillType();
        skill.name = S_PERCEPTION;
        skill.target = 7;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createSleightOfHand() {
        // We don't call this skill Prestidigitation because then we'll get 100 questions asking what
        // 'Prestidigitation' is.
        SkillType skill = new SkillType();
        skill.name = S_SLEIGHT_OF_HAND;
        skill.target = 8;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = DEXTERITY;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createProtocols() {
        SkillType skill = new SkillType();
        skill.name = S_PROTOCOLS;
        skill.target = 8;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = WILLPOWER;
        skill.secondAttribute = CHARISMA;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createScienceBiology() {
        SkillType skill = new SkillType();
        skill.name = S_SCIENCE_BIOLOGY;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_SCIENCE;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createScienceChemistry() {
        SkillType skill = new SkillType();
        skill.name = S_SCIENCE_CHEMISTRY;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_SCIENCE;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createScienceMathematics() {
        SkillType skill = new SkillType();
        skill.name = S_SCIENCE_MATHEMATICS;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_SCIENCE;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createSciencePhysics() {
        SkillType skill = new SkillType();
        skill.name = S_SCIENCE_PHYSICS;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_SCIENCE;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createSecuritySystemsElectronic() {
        SkillType skill = new SkillType();
        skill.name = S_SECURITY_SYSTEMS_ELECTRONIC;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_SECURITY;
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createSecuritySystemsMechanical() {
        SkillType skill = new SkillType();
        skill.name = S_SCIENCE_SYSTEMS_MECHANICAL;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_SECURITY;
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createSensorOperations() {
        SkillType skill = new SkillType();
        skill.name = S_SENSOR_OPERATIONS;
        skill.target = 8;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createStealth() {
        SkillType skill = new SkillType();
        skill.name = S_STEALTH;
        skill.target = 8;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = REFLEXES;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createStreetwise() {
        SkillType skill = new SkillType();
        skill.name = S_STREETWISE;
        skill.target = 8;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = CHARISMA;
        skill.secondAttribute = NONE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createSurvival() {
        SkillType skill = new SkillType();
        skill.name = S_SURVIVAL;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = DEXTERITY;
        skill.secondAttribute = INTELLIGENCE;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createTracking() {
        SkillType skill = new SkillType();
        skill.name = S_TRACKING;
        skill.target = 8;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = WILLPOWER;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }

    public static SkillType createTraining() {
        SkillType skill = new SkillType();
        skill.name = S_TRAINING;
        skill.target = 9;
        skill.countUp = false;
        skill.subType = ROLEPLAY_GENERAL;
        skill.firstAttribute = INTELLIGENCE;
        skill.secondAttribute = CHARISMA;
        skill.costs = new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        return skill;
    }
}
