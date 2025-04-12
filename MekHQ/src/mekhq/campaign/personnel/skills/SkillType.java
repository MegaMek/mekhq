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
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import megamek.Version;
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
import mekhq.MHQConstants;
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

    /**
     * A constant string value representing the suffix " (RP Only)".
     *
     * <p><b>Usage:</b> This is used to denote a skill that has no mechanical benefits. This tag should be
     * progressively removed as mechanics are expanded to use these skills.</p>
     */
    public static final String RP_ONLY_TAG = " (RP Only)";

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
    public static final String S_ACROBATICS = "Acrobatics" + RP_ONLY_TAG;
    public static final String S_ACTING = "Acting" + RP_ONLY_TAG;
    public static final String S_ANIMAL_HANDLING = "Animal Handling" + RP_ONLY_TAG;
    public static final String S_APPRAISAL = "Appraisal" + RP_ONLY_TAG;
    public static final String S_ARCHERY = "Archery" + RP_ONLY_TAG;
    public static final String S_ART_DANCING = "Art/Dancing" + RP_ONLY_TAG;
    public static final String S_ART_DRAWING = "Art/Drawing" + RP_ONLY_TAG;
    public static final String S_ART_PAINTING = "Art/Painting" + RP_ONLY_TAG;
    public static final String S_ART_WRITING = "Art/Writing" + RP_ONLY_TAG;
    public static final String S_CLIMBING = "Climbing" + RP_ONLY_TAG;
    public static final String S_COMMUNICATIONS = "Communications" + RP_ONLY_TAG;
    public static final String S_COMPUTERS = "Computers" + RP_ONLY_TAG;
    public static final String S_CRYPTOGRAPHY = "Cryptography" + RP_ONLY_TAG;
    public static final String S_DEMOLITIONS = "Demolitions" + RP_ONLY_TAG;
    public static final String S_DISGUISE = "Disguise" + RP_ONLY_TAG;
    public static final String S_ESCAPE_ARTIST = "Escape Artist" + RP_ONLY_TAG;
    public static final String S_FORGERY = "Forgery" + RP_ONLY_TAG;
    public static final String S_INTEREST_HISTORY = "Interest/History" + RP_ONLY_TAG;
    public static final String S_INTEREST_LITERATURE = "Interest/Literature" + RP_ONLY_TAG;
    public static final String S_INTEREST_HOLO_GAMES = "Interest/Holo-Games" + RP_ONLY_TAG;
    public static final String S_INTEREST_SPORTS = "Interest/Sports" + RP_ONLY_TAG;
    public static final String S_INTERROGATION = "Interrogation" + RP_ONLY_TAG;
    public static final String S_INVESTIGATION = "Investigation" + RP_ONLY_TAG;
    public static final String S_LANGUAGES = "Languages" + RP_ONLY_TAG;
    public static final String S_MARTIAL_ARTS = "Martial Arts" + RP_ONLY_TAG;
    public static final String S_PERCEPTION = "Perception" + RP_ONLY_TAG;
    public static final String S_SLEIGHT_OF_HAND = "Sleight of Hand" + RP_ONLY_TAG;
    public static final String S_PROTOCOLS = "Protocols" + RP_ONLY_TAG;
    public static final String S_SCIENCE_BIOLOGY = "Science/Biology" + RP_ONLY_TAG;
    public static final String S_SCIENCE_CHEMISTRY = "Science/Chemistry" + RP_ONLY_TAG;
    public static final String S_SCIENCE_MATHEMATICS = "Science/Mathematics" + RP_ONLY_TAG;
    public static final String S_SCIENCE_PHYSICS = "Science/Physics" + RP_ONLY_TAG;
    public static final String S_SECURITY_SYSTEMS_ELECTRONIC = "Security Systems/Electronic" + RP_ONLY_TAG;
    public static final String S_SCIENCE_SYSTEMS_MECHANICAL = "Security Systems/Mechanical" + RP_ONLY_TAG;
    public static final String S_SENSOR_OPERATIONS = "Sensor Operations" + RP_ONLY_TAG;
    public static final String S_STEALTH = "Stealth" + RP_ONLY_TAG;
    public static final String S_STREETWISE = "Streetwise" + RP_ONLY_TAG;
    public static final String S_SURVIVAL = "Survival" + RP_ONLY_TAG;
    public static final String S_TRACKING = "Tracking" + RP_ONLY_TAG;
    public static final String S_TRAINING = "Training" + RP_ONLY_TAG;

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
    public static final int DISABLED_SKILL_LEVEL = -1;

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

    /**
     * Default constructor for the {@code SkillType} class.
     *
     * <p>Initializes a default skill type with placeholder values, primarily for testing or fallback purposes.</p>
     *
     * <p><b>Usage:</b> Generally you don't want to be calling this, outside of loading from xml or in Unit Tests.
     * Instead, you want to use the full constructor.</p>
     */
    public SkillType() {
        this.name = "MISSING_NAME";
        this.target = 7;
        this.countUp = false;
        this.subType = SkillSubType.NONE;
        this.firstAttribute = REFLEXES;
        this.secondAttribute = DEXTERITY;
        this.greenLvl = 1;
        this.regLvl = 3;
        this.vetLvl = 4;
        this.eliteLvl = 5;
        this.costs = new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                                     DISABLED_SKILL_LEVEL };
    }

    /**
     * Constructs a {@code SkillType} instance with the specified parameters.
     *
     * <p>If certain parameters are {@code null}, default values will be used.</p>
     *
     * <p>The {@code costs} parameter is validated to ensure it contains exactly 11 entries,
     * corresponding to skill levels 0 through 10 inclusive. If the provided array is {@code null} or has fewer than 11
     * elements, a new array will be created with missing entries filled with {@link #DISABLED_SKILL_LEVEL}. If the
     * array has more than 11 entries, it will be trimmed to size. Additionally, the input array is copied to prevent
     * accidental external changes to the internal state of the instance.</p>
     *
     * @param name            The name of the skill type. <b>Cannot</b> be {@code null}.
     * @param target          The target value representing a threshold or goal for the skill. If {@code null}, the
     *                        default value is {@code 7}.
     * @param isCountUp       {@code true} if the skill counts up toward a goal, {@code false} otherwise. If
     *                        {@code null}, the default value is {@code false}.
     * @param subType         The {@link SkillSubType} category of the skill. This indicates the broader classification
     *                        of the skill (e.g., combat-related, role-playing).
     *                        <b>Cannot</b> be {@code null}.
     * @param firstAttribute  The primary {@link SkillAttribute} associated with the skill, influencing its calculation
     *                        or behavior. <b>Cannot</b> be {@code null}.
     * @param secondAttribute The secondary {@link SkillAttribute} associated with the skill. If {@code null}, the
     *                        default value is {@link SkillAttribute#NONE}.
     * @param greenLvl        The value representing the skill's "Green" proficiency level. If {@code null}, the default
     *                        value is {@code 1}.
     * @param regLvl          The value representing the skill's "Regular" proficiency level. If {@code null}, the
     *                        default value is {@code 3}.
     * @param vetLvl          The value representing the skill's "Veteran" proficiency level. If {@code null}, the
     *                        default value is {@code 4}.
     * @param eliteLvl        The value representing the skill's "Elite" proficiency level. If {@code null}, the default
     *                        value is {@code 5}.
     * @param costs           An {@code Integer[]} array representing the skill's progression costs for each level from
     *                        0 to 10 inclusive. If the array is {@code null} or its length is not exactly 11, a new
     *                        array is created with default values. Missing entries are filled with
     *                        {@link #DISABLED_SKILL_LEVEL}, and extra entries beyond the 11th are ignored. A clean copy
     *                        of the array is always used to ensure the integrity of the internal state.
     *
     *                        <p>For example:</p>
     *                        <pre>
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        Integer[] costs = new Integer[] {8, 4, 4, 4, 4, 4, 4, 4, 4, -1, -1};
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        SkillType skillType = new SkillType("Example Skill", 7, false, SkillSubType.COMBAT,
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               SkillAttribute.DEXTERITY, SkillAttribute.INTELLIGENCE, 1, 3, 4, 5, costs);
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        </pre>
     *
     * @author Illiani
     * @since 0.50.05
     */
    public SkillType(String name, @Nullable Integer target, @Nullable Boolean isCountUp, SkillSubType subType,
          SkillAttribute firstAttribute, @Nullable SkillAttribute secondAttribute, @Nullable Integer greenLvl,
          @Nullable Integer regLvl, @Nullable Integer vetLvl, @Nullable Integer eliteLvl, Integer[] costs) {
        this.name = name;
        this.target = target == null ? 7 : target;
        this.countUp = isCountUp != null && isCountUp;
        this.subType = subType;
        this.firstAttribute = firstAttribute;
        this.secondAttribute = secondAttribute == null ? NONE : secondAttribute;
        this.greenLvl = greenLvl == null ? 1 : greenLvl;
        this.regLvl = regLvl == null ? 3 : regLvl;
        this.vetLvl = vetLvl == null ? 4 : vetLvl;
        this.eliteLvl = eliteLvl == null ? 5 : eliteLvl;

        // This validates the length of costs to ensure that valid entries exist for all possible skill levels (0-10,
        // inclusive)
        if (costs == null || costs.length != 11) {
            logger.warn(
                  "The costs array is null or does not have exactly 11 entries. Filling missing levels with default values.");
            Integer[] validCosts = new Integer[11];
            Arrays.fill(validCosts, DISABLED_SKILL_LEVEL);

            // If costs is not null, copy in existing elements
            if (costs != null) {
                System.arraycopy(costs, 0, validCosts, 0, Math.min(costs.length, 11));
            }

            this.costs = validCosts;
        } else {
            // Ensure a clean copy of the given array so we can't accidentally make dirty edits.
            this.costs = Arrays.copyOf(costs, 11);
        }
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

    /**
     * Checks if the current instance is affected by the "Gremlins" or "Tech Empathy" SPAs.
     *
     * @return {@code true} if the {@code name} field matches one of the known tech or electronic-related skills,
     *       {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public boolean isAffectedByGremlinsOrTechEmpathy() {
        return Objects.equals(this.name, S_TECH_BA) ||
                     Objects.equals(this.name, S_TECH_AERO) ||
                     Objects.equals(this.name, S_TECH_MECHANIC) ||
                     Objects.equals(this.name, S_TECH_MEK) ||
                     Objects.equals(this.name, S_TECH_VESSEL) ||
                     Objects.equals(this.name, S_COMPUTERS) ||
                     Objects.equals(this.name, S_COMMUNICATIONS) ||
                     Objects.equals(this.name, S_SECURITY_SYSTEMS_ELECTRONIC);
    }

    /**
     * Checks if the current instance is affected by the "Impatient" or "Patient" SPAs.
     *
     * @return {@code true} if the instance is related to one of the affected subtypes or names, {@code false}
     *       otherwise.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public boolean isAffectedByImpatientOrPatient() {
        return this.isSubTypeOf(ROLEPLAY_ART) ||
                     this.isSubTypeOf(ROLEPLAY_SECURITY) ||
                     Objects.equals(this.name, S_CRYPTOGRAPHY) ||
                     Objects.equals(this.name, S_DEMOLITIONS) ||
                     Objects.equals(this.name, S_INVESTIGATION) ||
                     Objects.equals(this.name, S_PROTOCOLS) ||
                     Objects.equals(this.name, S_STRATEGY) ||
                     Objects.equals(this.name, S_TACTICS) ||
                     Objects.equals(this.name, S_TRAINING);
    }

    public SkillAttribute getFirstAttribute() {
        return firstAttribute;
    }

    public SkillAttribute getSecondAttribute() {
        return secondAttribute;
    }

    /**
     * Determines whether the skill type has the specified attribute.
     *
     * <p>This method checks if the provided {@link SkillAttribute} matches either of the two attributes associated
     * with the skill type.</p>
     *
     * @param attribute the {@link SkillAttribute} to check
     *
     * @return {@code true} if the skill type includes the specified attribute; {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public boolean hasAttribute(SkillAttribute attribute) {
        return (firstAttribute == attribute) || (secondAttribute == attribute);
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

    /**
     * Sets the first {@link SkillAttribute} associated with the skill type.
     *
     * <p>If {@code firstAttribute} is {@code null}, no action is taken, and the current value of the first attribute
     * remains unchanged.
     *
     * @param firstAttribute the {@link SkillAttribute} to be used as the second attribute. If {@code null}, the
     *                       existing value is preserved.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public void setFirstAttribute(@Nullable SkillAttribute firstAttribute) {
        this.firstAttribute = firstAttribute == null ? NONE : firstAttribute;
    }

    /**
     * Sets the second {@link SkillAttribute} associated with the skill type.
     *
     * <p>If {@code secondAttribute} is {@code null}, no action is taken, and the current value of the second
     * attribute remains unchanged.
     *
     * @param secondAttribute the {@link SkillAttribute} to be used as the second attribute. If {@code null}, the
     *                        existing value is preserved.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public void setSecondAttribute(@Nullable SkillAttribute secondAttribute) {
        this.secondAttribute = secondAttribute == null ? NONE : secondAttribute;
    }

    public int getCost(int lvl) {
        if ((lvl > 10) || (lvl < 0)) {
            return DISABLED_SKILL_LEVEL;
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
     * @return the maximum level of that skill (the last one not set to cost {@link #DISABLED_SKILL_LEVEL}, or 10)
     */
    public int getMaxLevel() {
        for (int lvl = 0; lvl < costs.length; ++lvl) {
            if (costs[lvl] == DISABLED_SKILL_LEVEL) {
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
     * @deprecated use {@link #generateInstanceFromXML(Node, Version)} instead
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public static void generateInstanceFromXML(Node workingNode) {
        generateInstanceFromXML(workingNode, MHQConstants.VERSION);
    }

    /**
     * Generates an instance of {@link SkillType} from an XML node.
     *
     * @param workingNode The XML node containing the skill data.
     * @param version     The current version.
     */
    public static void generateInstanceFromXML(Node workingNode, Version version) {
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
                    if (skillType.costs == null) {
                        skillType.costs = new Integer[11];
                        // Fill with default values, this protects us from NPEs
                        Arrays.fill(skillType.costs, DISABLED_SKILL_LEVEL);
                    }

                    for (int i = 0; i < values.length; i++) {
                        skillType.costs[i] = MathUtility.parseInt(values[i], skillType.costs[i]);
                    }
                }
            }

            // Skill settings from prior to this are incompatible and cannot be used, so we use the default values instead.
            boolean preDatesSkillChanges = version.isLowerThan(new Version("0.50.05"));
            if (preDatesSkillChanges) {
                compatibilityHandler(skillType);
            }

            lookupHash.put(skillType.name, skillType);
        } catch (Exception ex) {
            logger.error("", ex);
        }
    }

    /**
     * @deprecated use {@link #generateSeparateInstanceFromXML(Node, Map, Version)} instead
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public static void generateSeparateInstanceFromXML(final Node wn, final Map<String, SkillType> hash) {
        generateSeparateInstanceFromXML(wn, hash, MHQConstants.VERSION);
    }

    public static void generateSeparateInstanceFromXML(final Node wn, final Map<String, SkillType> hash,
          Version version) {

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

            // Skill settings from prior to this are incompatible and cannot be used, so we use the default values instead.
            boolean preDatesSkillChanges = version.isLowerThan(new Version("0.50.05"));
            if (preDatesSkillChanges) {
                compatibilityHandler(skillType);
            }

            hash.put(skillType.name, skillType);
        } catch (Exception ex) {
            logger.error("", ex);
        }
    }

    /**
     * Updates {@link SkillType} from <0.50.05 by setting its subtype and attributes based on the skill name.
     *
     * <p>The method creates a temporary {@link SkillType} with the correct configuration based on the input skill
     * name,
     * then copies the {@link SkillType#subType}, {@link SkillType#firstAttribute}, and
     * {@link SkillType#secondAttribute} values to the provided {@link SkillType}.<p>
     *
     * <p>For each skill type, it logs the updates made to help with debugging and tracking compatibility changes.</p>
     *
     * @param skillType the {@link SkillType} to update with compatible configuration If {@code null}, the method logs
     *                  an error and returns without making changes
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

        skillType.subType = temporarySkillType.getSubType();
        logger.info("SkillType {} has been updated to sub type {}",
              skillType.getName(),
              temporarySkillType.getSubType());

        skillType.firstAttribute = temporarySkillType.getFirstAttribute();
        logger.info("SkillType {} has been updated to first attribute {}",
              skillType.getName(),
              temporarySkillType.getFirstAttribute());

        skillType.secondAttribute = temporarySkillType.getSecondAttribute();
        logger.info("SkillType {} has been updated to second attribute {}",
              skillType.getName(),
              temporarySkillType.getSecondAttribute());
    }


    public static SkillType createPilotingMek() {
        return new SkillType(S_PILOT_MEK,
              8,
              false,
              COMBAT_PILOTING,
              REFLEXES,
              DEXTERITY,
              2,
              null,
              null,
              null,
              new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createGunneryMek() {
        return new SkillType(S_GUN_MEK,
              7,
              false,
              COMBAT_GUNNERY,
              REFLEXES,
              DEXTERITY,
              2,
              null,
              null,
              null,
              new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL });
    }

    public static SkillType createPilotingAero() {
        return new SkillType(S_PILOT_AERO,
              8,
              false,
              COMBAT_PILOTING,
              REFLEXES,
              DEXTERITY,
              2,
              null,
              null,
              null,
              new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createGunneryAero() {
        return new SkillType(S_GUN_AERO,
              7,
              false,
              COMBAT_GUNNERY,
              REFLEXES,
              DEXTERITY,
              2,
              null,
              null,
              null,
              new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL });
    }

    public static SkillType createPilotingJet() {
        return new SkillType(S_PILOT_JET,
              8,
              false,
              COMBAT_PILOTING,
              REFLEXES,
              DEXTERITY,
              2,
              null,
              null,
              null,
              new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createGunneryJet() {
        return new SkillType(S_GUN_JET,
              7,
              false,
              COMBAT_GUNNERY,
              REFLEXES,
              DEXTERITY,
              2,
              null,
              null,
              null,
              new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL });
    }

    public static SkillType createPilotingSpace() {
        return new SkillType(S_PILOT_SPACE,
              8,
              false,
              COMBAT_PILOTING,
              REFLEXES,
              DEXTERITY,
              2,
              null,
              null,
              null,
              new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createGunnerySpace() {
        return new SkillType(S_GUN_SPACE,
              7,
              false,
              COMBAT_GUNNERY,
              REFLEXES,
              DEXTERITY,
              2,
              null,
              null,
              null,
              new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL });
    }

    public static SkillType createPilotingGroundVee() {
        return new SkillType(S_PILOT_GVEE,
              8,
              false,
              COMBAT_PILOTING,
              REFLEXES,
              DEXTERITY,
              2,
              null,
              null,
              null,
              new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createPilotingNavalVee() {
        return new SkillType(S_PILOT_NVEE,
              8,
              false,
              COMBAT_PILOTING,
              REFLEXES,
              DEXTERITY,
              2,
              null,
              null,
              null,
              new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createPilotingVTOL() {
        return new SkillType(S_PILOT_VTOL,
              8,
              false,
              COMBAT_PILOTING,
              REFLEXES,
              DEXTERITY,
              2,
              null,
              null,
              null,
              new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createGunneryVehicle() {
        return new SkillType(S_GUN_VEE,
              7,
              false,
              COMBAT_GUNNERY,
              REFLEXES,
              DEXTERITY,
              2,
              null,
              null,
              null,
              new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL });
    }

    public static SkillType createArtillery() {
        return new SkillType(S_ARTILLERY,
              7,
              false,
              COMBAT_GUNNERY,
              INTELLIGENCE,
              WILLPOWER,
              2,
              null,
              null,
              null,
              new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL });
    }

    public static SkillType createGunneryBA() {
        return new SkillType(S_GUN_BA,
              7,
              false,
              COMBAT_GUNNERY,
              REFLEXES,
              DEXTERITY,
              2,
              null,
              null,
              null,
              new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL });
    }

    public static SkillType createGunneryProto() {
        return new SkillType(S_GUN_PROTO,
              7,
              false,
              COMBAT_GUNNERY,
              REFLEXES,
              DEXTERITY,
              2,
              null,
              null,
              null,
              new Integer[] { 16, 8, 8, 8, 8, 8, 8, 8, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL });
    }

    public static SkillType createSmallArms() {
        return new SkillType(S_SMALL_ARMS,
              7,
              false,
              COMBAT_GUNNERY,
              DEXTERITY,
              NONE,
              2,
              null,
              null,
              null,
              new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createAntiMek() {
        // Anti-Mek is the 'piloting' skill of Conventional Infantry so it has a subtype of 'COMBAT_PILOTING'.
        // As Anti-Mek doesn't exist in ATOW so we use the linked attributes for Demolitions (and technically
        // Climbing, though the linked attribute for Climbing is just Dexterity which is shared with Demolitions).
        return new SkillType(S_ANTI_MEK,
              8,
              false,
              COMBAT_PILOTING,
              DEXTERITY,
              INTELLIGENCE,
              2,
              null,
              null,
              null,
              new Integer[] { 12, 6, 6, 6, 6, 6, 6, 6, 6, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createTechMek() {
        // This skill corresponds to the ATOW skill 'Technician'
        return new SkillType(S_TECH_MEK,
              10,
              false,
              SUPPORT,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 12, 6, 0, 6, 6, 6, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createTechMechanic() {
        // This skill corresponds to the ATOW skill 'Technician'
        return new SkillType(S_TECH_MECHANIC,
              10,
              false,
              SUPPORT,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 12, 6, 0, 6, 6, 6, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createTechAero() {
        // This skill corresponds to the ATOW skill 'Technician'
        return new SkillType(S_TECH_AERO,
              10,
              false,
              SUPPORT,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 12, 6, 0, 6, 6, 6, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createTechBA() {
        // This skill corresponds to the ATOW skill 'Technician'
        return new SkillType(S_TECH_BA,
              10,
              false,
              SUPPORT,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 12, 6, 0, 6, 6, 6, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createTechVessel() {
        // This skill corresponds to the ATOW skill 'Technician'
        return new SkillType(S_TECH_VESSEL,
              10,
              false,
              SUPPORT,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 12, 6, 0, 6, 6, 6, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createAstech() {
        // This doesn't correspond to an ATOW skill, so we went with INTELLIGENCE as the tech equivalent of MedTech
        return new SkillType(S_ASTECH,
              10,
              false,
              SUPPORT,
              INTELLIGENCE,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 12, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createDoctor() {
        // This corresponds to the ATOW skill 'Surgery'
        return new SkillType(S_DOCTOR,
              11,
              false,
              SUPPORT,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 16, 8, 0, 8, 8, 8, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createMedTech() {
        return new SkillType(S_MEDTECH,
              11,
              false,
              SUPPORT,
              INTELLIGENCE,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 16, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createNav() {
        // This skill corresponds to the ATOW skill Navigation
        return new SkillType(S_NAV,
              8,
              false,
              SUPPORT,
              INTELLIGENCE,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createTactics() {
        return new SkillType(S_TACTICS,
              0,
              true,
              SUPPORT_COMMAND,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 12, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6 });
    }

    public static SkillType createStrategy() {
        return new SkillType(S_STRATEGY,
              0,
              true,
              SUPPORT_COMMAND,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 12, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6 });
    }

    public static SkillType createAdmin() {
        return new SkillType(S_ADMIN,
              10,
              false,
              SUPPORT,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 8, 4, 0, 4, 4, 4, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL,
                              DISABLED_SKILL_LEVEL, DISABLED_SKILL_LEVEL });
    }

    public static SkillType createLeadership() {
        return new SkillType(S_LEADER,
              0,
              true,
              SUPPORT_COMMAND,
              WILLPOWER,
              CHARISMA,
              null,
              null,
              null,
              null,
              new Integer[] { 12, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6 });
    }

    public static SkillType createNegotiation() {
        return new SkillType(S_NEG,
              10,
              false,
              SUPPORT,
              CHARISMA,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 });
    }

    public static SkillType createScrounge() {
        // This skill doesn't exist in ATOW, so we copied the linked attributes from Negotiation
        return new SkillType(S_SCROUNGE,
              10,
              false,
              SUPPORT,
              CHARISMA,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 8, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 });
    }

    public static SkillType createAcrobatics() {
        return new SkillType(S_ACROBATICS,
              7,
              false,
              ROLEPLAY_GENERAL,
              REFLEXES,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createActing() {
        return new SkillType(S_ACTING,
              8,
              false,
              ROLEPLAY_GENERAL,
              CHARISMA,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createAnimalHandling() {
        return new SkillType(S_ANIMAL_HANDLING,
              8,
              false,
              ROLEPLAY_GENERAL,
              WILLPOWER,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createAppraisal() {
        return new SkillType(S_APPRAISAL,
              8,
              false,
              ROLEPLAY_GENERAL,
              INTELLIGENCE,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createArchery() {
        return new SkillType(S_ARCHERY,
              7,
              false,
              ROLEPLAY_GENERAL,
              DEXTERITY,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createArtDancing() {
        return new SkillType(S_ART_DANCING,
              9,
              false,
              ROLEPLAY_ART,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createArtDrawing() {
        return new SkillType(S_ART_DRAWING,
              9,
              false,
              ROLEPLAY_ART,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createArtPainting() {
        return new SkillType(S_ART_PAINTING,
              9,
              false,
              ROLEPLAY_ART,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createArtWriting() {
        return new SkillType(S_ART_WRITING,
              9,
              false,
              ROLEPLAY_ART,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createClimbing() {
        return new SkillType(S_CLIMBING,
              7,
              false,
              ROLEPLAY_GENERAL,
              DEXTERITY,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createCommunications() {
        return new SkillType(S_COMMUNICATIONS,
              7,
              false,
              ROLEPLAY_GENERAL,
              INTELLIGENCE,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createComputers() {
        return new SkillType(S_COMPUTERS,
              9,
              false,
              ROLEPLAY_GENERAL,
              INTELLIGENCE,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createCryptography() {
        return new SkillType(S_CRYPTOGRAPHY,
              9,
              false,
              ROLEPLAY_GENERAL,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createDemolitions() {
        return new SkillType(S_DEMOLITIONS,
              9,
              false,
              ROLEPLAY_GENERAL,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createDisguise() {
        return new SkillType(S_DISGUISE,
              7,
              false,
              ROLEPLAY_GENERAL,
              CHARISMA,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createEscapeArtist() {
        return new SkillType(S_ESCAPE_ARTIST,
              9,
              false,
              ROLEPLAY_GENERAL,
              STRENGTH,
              DEXTERITY,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createForgery() {
        return new SkillType(S_FORGERY,
              8,
              false,
              ROLEPLAY_GENERAL,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createInterestHistory() {
        return new SkillType(S_INTEREST_HISTORY,
              9,
              false,
              ROLEPLAY_INTEREST,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createInterestLiterature() {
        return new SkillType(S_INTEREST_LITERATURE,
              9,
              false,
              ROLEPLAY_INTEREST,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createInterestHoloGames() {
        return new SkillType(S_INTEREST_HOLO_GAMES,
              9,
              false,
              ROLEPLAY_INTEREST,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createInterestSports() {
        return new SkillType(S_INTEREST_SPORTS,
              9,
              false,
              ROLEPLAY_INTEREST,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createInterrogation() {
        return new SkillType(S_INTERROGATION,
              9,
              false,
              ROLEPLAY_GENERAL,
              WILLPOWER,
              CHARISMA,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createInvestigation() {
        return new SkillType(S_INVESTIGATION,
              9,
              false,
              ROLEPLAY_GENERAL,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createLanguages() {
        return new SkillType(S_LANGUAGES,
              8,
              false,
              ROLEPLAY_GENERAL,
              INTELLIGENCE,
              CHARISMA,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createMartialArts() {
        return new SkillType(S_MARTIAL_ARTS,
              8,
              false,
              ROLEPLAY_GENERAL,
              REFLEXES,
              DEXTERITY,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createPerception() {
        return new SkillType(S_PERCEPTION,
              7,
              false,
              ROLEPLAY_GENERAL,
              INTELLIGENCE,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createSleightOfHand() {
        // We don't call this skill Prestidigitation because then we'll get 100 questions asking what
        // 'Prestidigitation' means.
        return new SkillType(S_SLEIGHT_OF_HAND,
              8,
              false,
              ROLEPLAY_GENERAL,
              REFLEXES,
              DEXTERITY,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createProtocols() {
        return new SkillType(S_PROTOCOLS,
              8,
              false,
              ROLEPLAY_GENERAL,
              WILLPOWER,
              CHARISMA,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createScienceBiology() {
        return new SkillType(S_SCIENCE_BIOLOGY,
              9,
              false,
              ROLEPLAY_SCIENCE,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createScienceChemistry() {
        return new SkillType(S_SCIENCE_CHEMISTRY,
              9,
              false,
              ROLEPLAY_SCIENCE,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createScienceMathematics() {
        return new SkillType(S_SCIENCE_MATHEMATICS,
              9,
              false,
              ROLEPLAY_SCIENCE,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createSciencePhysics() {
        return new SkillType(S_SCIENCE_PHYSICS,
              9,
              false,
              ROLEPLAY_SCIENCE,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createSecuritySystemsElectronic() {
        return new SkillType(S_SECURITY_SYSTEMS_ELECTRONIC,
              9,
              false,
              ROLEPLAY_SECURITY,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createSecuritySystemsMechanical() {
        return new SkillType(S_SCIENCE_SYSTEMS_MECHANICAL,
              9,
              false,
              ROLEPLAY_SECURITY,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createSensorOperations() {
        return new SkillType(S_SENSOR_OPERATIONS,
              8,
              false,
              ROLEPLAY_GENERAL,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createStealth() {
        return new SkillType(S_STEALTH,
              8,
              false,
              ROLEPLAY_GENERAL,
              REFLEXES,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createStreetwise() {
        return new SkillType(S_STREETWISE,
              8,
              false,
              ROLEPLAY_GENERAL,
              CHARISMA,
              NONE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createSurvival() {
        return new SkillType(S_SURVIVAL,
              9,
              false,
              ROLEPLAY_GENERAL,
              DEXTERITY,
              INTELLIGENCE,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createTracking() {
        return new SkillType(S_TRACKING,
              8,
              false,
              ROLEPLAY_GENERAL,
              INTELLIGENCE,
              WILLPOWER,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }

    public static SkillType createTraining() {
        return new SkillType(S_TRAINING,
              9,
              false,
              ROLEPLAY_GENERAL,
              INTELLIGENCE,
              CHARISMA,
              null,
              null,
              null,
              null,
              new Integer[] { 20, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 });
    }
}
