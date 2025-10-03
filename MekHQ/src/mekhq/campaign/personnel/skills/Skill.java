/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.skills;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static mekhq.campaign.personnel.PersonnelOptions.*;
import static mekhq.campaign.personnel.skills.SkillCheckUtility.UNTRAINED_SKILL_MODIFIER;
import static mekhq.campaign.personnel.skills.SkillType.S_ACTING;
import static mekhq.campaign.personnel.skills.SkillType.S_ANIMAL_HANDLING;
import static mekhq.campaign.personnel.skills.SkillType.S_INTEREST_THEOLOGY;
import static mekhq.campaign.personnel.skills.SkillType.S_NEGOTIATION;
import static mekhq.campaign.personnel.skills.SkillType.S_PERCEPTION;
import static mekhq.campaign.personnel.skills.SkillType.S_PROTOCOLS;
import static mekhq.campaign.personnel.skills.SkillType.S_STREETWISE;
import static mekhq.campaign.personnel.skills.SkillUtilities.EXP_REGULAR;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.CHARISMA;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.INTELLIGENCE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

import megamek.codeUtilities.MathUtility;
import megamek.common.compute.Compute;
import megamek.common.enums.SkillLevel;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * As ov v0.1.9, we will be tracking a group of skills on the person. These skills will define personnel rather than
 * subtypes wrapped around pilots and teams. This will allow for considerably more flexibility in the kinds of personnel
 * available.
 * <p>
 * Four important characteristics will determine how each skill works
 * <p>
 * level - this is the level of the skill. By default, this will go from 0 to 10, but the max will be customizable.
 * These won't necessarily correspond to named levels (e.g. Green, Elite).
 * <p>
 * By assigning skill costs of 0 to some levels, these can basically be skipped and by assigning skill costs of -1, they
 * can be made inaccessible.
 * <p>
 * bonus - this is a bonus that the given person has for this skill which is separable from level. Primarily this allows
 * for rpg-style attribute bonuses to come into play.
 * <p>
 * target - this is the baseline target number for the skill when level and bonus are zero.
 * <p>
 * isCountUp - this is a boolean that defines whether this skill's target is a "roll greater than or equal to" (false)
 * or a rpg-style bonus to a roll (true)
 * <p>
 * The actual target number for a skill is given by
 * <p>
 * isCountUp: target + lvl + bonus !isCountUp: target - level - bonus
 * <p>
 * by clever manipulation of these values and skill costs in campaignOptions, players should be able to recreate any of
 * the rpg versions or their own homebrew system. The default setup will follow the core rule books (not aToW).
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Skill {
    public static int COUNT_UP_MAX_VALUE = 10;
    public static int COUNT_DOWN_MIN_VALUE = 0;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Skill";
    private static final MMLogger logger = MMLogger.create(Skill.class);

    private SkillType type;
    private int level;
    private int bonus;
    private int agingModifier;

    protected Skill() {

    }

    public Skill(String type) {
        this.type = SkillType.getType(type);
        this.level = this.type.getLevelFromExperience(EXP_REGULAR);
    }

    public Skill(String type, int level, int bonus) {
        this(SkillType.getType(type), level, bonus);
    }

    public Skill(String type, int level, int bonus, int agingModifier) {
        this(SkillType.getType(type), level, bonus, agingModifier);
    }

    public Skill(SkillType type, int level, int bonus) {
        this.type = type;
        this.level = level;
        this.bonus = bonus;
    }

    public Skill(SkillType type, int level, int bonus, int agingModifier) {
        this.type = type;
        this.level = level;
        this.bonus = bonus;
        this.agingModifier = agingModifier;
    }

    /**
     * Retrieves the maximum value that can be used for skills that count up.
     */
    public static int getCountUpMaxValue() {
        return COUNT_UP_MAX_VALUE;
    }

    /**
     * Retrieves the minimum value that can be used for skills that count down.
     */
    public static int getCountDownMaxValue() {
        return COUNT_DOWN_MIN_VALUE;
    }

    /**
     * @return {@code true} if the progression type is "count up", {@code false} otherwise.
     */
    private boolean isCountUp() {
        return type.isCountUp();
    }

    /**
     * Evaluates whether the current skill or attribute level is eligible for improvement based on progression type,
     * current value, and min-max limitations.
     *
     * <p>For a "count up" progression (increasing value), the improvement is valid only if the calculated skill value
     * is less than the defined maximum value. In contrast, for a "count down" progression (decreasing value), the
     * improvement is valid only if the calculated skill value is greater than the defined minimum value.</p>
     *
     * @return {@code true} if the current state satisfies the eligibility criteria for legal improvement based on the
     *       progression type; {@code false} otherwise.
     */
    public boolean isImprovementLegal() {
        if (isCountUp()) {
            return getSkillValue() < COUNT_UP_MAX_VALUE;
        } else {
            return getSkillValue() > COUNT_DOWN_MIN_VALUE;
        }
    }

    /**
     * Creates a new {@link Skill} from the given experience level and bonus.
     *
     * @param type            The {@link SkillType} name.
     * @param experienceLevel An experience level (e.g. {@link SkillType#EXP_GREEN}).
     * @param bonus           The bonus for the resulting {@link Skill}.
     *
     * @return A new {@link Skill} of the appropriate type, with a level based on {@code experienceLevel} and the bonus.
     */
    public static Skill createFromExperience(String type, int experienceLevel, int bonus) {
        SkillType skillType = SkillType.getType(type);
        int level = skillType.getLevelFromExperience(experienceLevel);
        return new Skill(skillType, level, bonus);
    }

    /**
     * Creates a new {@link Skill} with a randomized level.
     *
     * @param type            The {@link SkillType} name.
     * @param experienceLevel An experience level (e.g. {@link SkillType#EXP_GREEN}).
     * @param bonus           The bonus for the resulting {@link Skill}.
     * @param rollModifier    The roll modifier on a 1D6.
     *
     * @return A new {@link Skill} of the appropriate type, with a randomized level based on the experience level and a
     *       1D6 roll.
     */
    public static Skill randomizeLevel(String type, int experienceLevel, int bonus, int rollModifier) {
        SkillType skillType = SkillType.getType(type);
        int level = skillType.getLevelFromExperience(experienceLevel);

        int roll = Compute.d6() + rollModifier;
        if (roll < 2 && level > 0) {
            level--;
        } else if (roll > 5 && level < 10) {
            level++;
        }

        return new Skill(skillType, level, bonus);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int l) {
        this.level = l;
    }

    public int getBonus() {
        return bonus;
    }

    public void setBonus(int b) {
        this.bonus = b;
    }

    public int getAgingModifier() {
        return agingModifier;
    }

    public void setAgingModifier(int agingModifier) {
        this.agingModifier = agingModifier;
    }

    public SkillType getType() {
        return type;
    }

    /**
     * @deprecated use {@link #getFinalSkillValue(PersonnelOptions, Attributes)} instead.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public int getFinalSkillValue(PersonnelOptions characterOptions) {
        return getFinalSkillValue(characterOptions, new Attributes(), 0);
    }

    /**
     * Calculates the final skill value for the character based on the default reputation modifier (zero).
     *
     * <p>This is a convenience method that delegates to {@link #getFinalSkillValue(PersonnelOptions, Attributes, int)}
     * with a default reputation value of {@code 0}.</p>
     *
     * <p><b>Usage:</b> This method is for when we know, 100%, that the targeted {@link Skill} is not affected by
     * the character's Reputation. If unsure, use {@link #getFinalSkillValue(PersonnelOptions, Attributes, int)}
     * instead.</p>
     *
     * @param characterOptions The {@link PersonnelOptions} to consider for determining skill value modifiers.
     *
     * @return The final skill value after applying progression rules and using a default reputation of zero.
     */
    public int getFinalSkillValue(PersonnelOptions characterOptions, Attributes attributes) {
        return getFinalSkillValue(characterOptions, attributes, 0);
    }

    /**
     * @deprecated use {@link #getFinalSkillValue(PersonnelOptions, Attributes, int)} instead.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public int getFinalSkillValue(PersonnelOptions characterOptions, int reputation) {
        return getFinalSkillValue(characterOptions, new Attributes(), reputation);
    }

    /**
     * Calculates the final skill value for a character by applying progression rules, attribute modifiers, SPA (Special
     * Pilot Abilities) modifiers, and reputation, then clamps the value within the legal range for the skill type.
     *
     * <p>The calculation sequence is as follows:</p>
     * <ul>
     *   <li>SPA modifiers are determined based on the provided character options and reputation value.</li>
     *   <li>Attribute modifiers relevant to the skill type are computed using the given attributes.</li>
     *   <li>For "count up" progression, the summed result is capped at the maximum allowed value.</li>
     *   <li>For "count down" progression, the result is floored at the minimum allowed value.</li>
     * </ul>
     *
     * <p>The reputation value is included as part of the modifiers to the skill value.</p>
     *
     * @param characterOptions the {@link PersonnelOptions} that define modifiers and SPA specifics for the character
     * @param attributes       the {@link Attributes} object providing attribute values for the character
     * @param reputation       a numeric value influencing the skill, positive to improve or negative to penalize it
     *
     * @return the calculated final skill value, after applying all modifiers and bounds
     */
    public int getFinalSkillValue(PersonnelOptions characterOptions, Attributes attributes, int reputation) {
        int modifiers = getModifiers(characterOptions, attributes, reputation);

        if (isCountUp()) {
            return min(COUNT_UP_MAX_VALUE, getSkillValue() + modifiers);
        } else {
            return max(COUNT_DOWN_MIN_VALUE, getSkillValue() - modifiers);
        }
    }

    /**
     * Calculates the skill modifiers for the current skill type based on the character's SPAs.
     *
     * @param characterOptions The {@link PersonnelOptions} with the character's attributes and options.
     * @param reputation       The character's reputation
     *
     * @return The calculated skill modifier for the current skill type.
     */
    public int getSPAModifiers(PersonnelOptions characterOptions, int reputation) {
        int modifier = 0;

        if (characterOptions == null) {
            logger.warn("Character options are null. Cannot calculate SPA Modifiers.", new Exception());
            return modifier;
        }

        final boolean hasReligiousFanaticism = characterOptions.booleanOption(COMPULSION_RELIGIOUS_FANATICISM);

        String name = type.getName();
        // Reputation and Alternate ID
        if (Objects.equals(name, S_NEGOTIATION) ||
                  Objects.equals(name, S_PROTOCOLS) ||
                  Objects.equals(name, S_STREETWISE)) {
            if (characterOptions.booleanOption(ATOW_ALTERNATE_ID) && reputation < 0) {
                reputation = min(0, reputation + 2);
            }

            modifier += reputation;
        }

        // Animal Empathy and Animal Antipathy
        if (Objects.equals(name, S_ANIMAL_HANDLING)) {
            if (characterOptions.booleanOption(FLAW_ANIMAL_ANTIPATHY)) {
                modifier -= 2;
            }

            if (characterOptions.booleanOption(ATOW_ANIMAL_EMPATHY)) {
                modifier += 2;
            }
        }

        // Attractive, Unattractive, Freakish Strength, some compulsions
        if (type.hasAttribute(CHARISMA)) {
            if (characterOptions.booleanOption(FLAW_UNATTRACTIVE)) {
                modifier -= 2;
            }

            if (characterOptions.booleanOption(MUTATION_FREAKISH_STRENGTH)) {
                modifier -= 2;
            }

            if (characterOptions.booleanOption(MADNESS_CLINICAL_PARANOIA)) {
                modifier -= 2;
            }

            if (hasReligiousFanaticism) {
                modifier -= 1;
            }

            if (hasReligiousFanaticism) {
                modifier -= 1;
            }

            if (characterOptions.booleanOption(ATOW_ATTRACTIVE)) {
                modifier += 2;
            }
        }

        // Poor Hearing, Good Hearing, Poor Vision, Good Vision, Sixth Sense, Cat Girl
        if (Objects.equals(name, S_PERCEPTION)) {
            if (characterOptions.booleanOption(FLAW_POOR_HEARING)) {
                modifier -= 1;
            }

            if (characterOptions.booleanOption(ATOW_GOOD_HEARING)) {
                modifier += 1;
            }

            if (characterOptions.booleanOption(MUTATION_CAT_GIRL)) {
                modifier += 1;
            }

            if (characterOptions.booleanOption(MUTATION_CAT_GIRL_UNOFFICIAL)) {
                modifier += 1;
            }

            if (characterOptions.booleanOption(FLAW_POOR_VISION)) {
                modifier -= 1;
            }

            if (characterOptions.booleanOption(ATOW_GOOD_VISION)) {
                modifier += 1;
            }

            if (characterOptions.booleanOption(ATOW_SIXTH_SENSE)) {
                modifier += 3;
            }
        }

        // Introvert, Gregarious
        if (Objects.equals(name, S_ACTING) || Objects.equals(name, S_NEGOTIATION)) {
            if (characterOptions.booleanOption(FLAW_INTROVERT)) {
                modifier -= 1;
            }

            if (characterOptions.booleanOption(ATOW_GREGARIOUS)) {
                modifier += 1;
            }
        }

        // Impatient, Patient
        if (type.isAffectedByImpatientOrPatient()) {
            if (characterOptions.booleanOption(FLAW_IMPATIENT)) {
                modifier -= 1;
            }

            if (characterOptions.booleanOption(ATOW_PATIENT)) {
                modifier += 1;
            }
        }

        // Gremlins, Tech Empathy
        if (type.isAffectedByGremlinsOrTechEmpathy()) {
            if (characterOptions.booleanOption(FLAW_IMPATIENT)) {
                modifier -= 1;
            }

            if (characterOptions.booleanOption(ATOW_PATIENT)) {
                modifier += 1;
            }
        }

        // Trivial Compulsion - Religious Fanaticism
        if (Objects.equals(S_INTEREST_THEOLOGY, name)) {
            if (hasReligiousFanaticism) {
                modifier += 2;
            }
        }

        return modifier;
    }


    /**
     * Calculates the total attribute modifier for a given skill type based on the character's attributes and applies
     * the modifiers to the target roll.
     *
     * <p>This method retrieves the attributes linked to the specified {@link SkillType} and calculates
     * the total contribution of their modifiers to the target roll. Each attribute's score is converted into an
     * individual modifier using {@link #getIndividualAttributeModifier(int)}, and the modifier is then added to
     * both:</p>
     *
     * <ul>
     *   <li>The total attribute modifier (returned by the method), and</li>
     *   <li>The {@link TargetRoll}, where the attribute modifier is applied as a negative value.</li>
     * </ul>
     *
     * <p>Attributes that are set to {@link SkillAttribute#NONE} are ignored during this process.</p>
     *
     * <p>The calculated attribute modifiers are applied directly to the {@link TargetRoll} using
     * {@link TargetRoll#addModifier(int, String)}, where the negative modifier is associated with the
     * attribute's label.</p>
     *
     * @param targetNumber        the {@link TargetRoll} representing the current target number, which will be adjusted
     *                            based on the character's attribute modifiers
     * @param characterAttributes the {@link Attributes} object representing the character's raw attribute scores that
     *                            determine the skill check modifiers
     * @param skillType           the {@link SkillType} being assessed, whose linked attributes contribute to the total
     *                            modifier calculation
     *
     * @return the total attribute modifier calculated for the given skill type, which is the sum of the individual
     *       modifiers for each linked attribute. If any of the parameters are {@code null} returns 0.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static int getTotalAttributeModifier(TargetRoll targetNumber, final Attributes characterAttributes,
          final SkillType skillType) {
        if (targetNumber == null || characterAttributes == null || skillType == null) {
            return 0;
        }

        List<SkillAttribute> linkedAttributes = List.of(skillType.getFirstAttribute(), skillType.getSecondAttribute());

        int totalModifier = 0;
        for (SkillAttribute attribute : linkedAttributes) {
            if (attribute == SkillAttribute.NONE) {
                continue;
            }

            int attributeScore = characterAttributes.getAttribute(attribute);
            int attributeModifier = getIndividualAttributeModifier(attributeScore);
            totalModifier += attributeModifier;
            targetNumber.addModifier(-attributeModifier, attribute.getLabel());
        }

        return totalModifier;
    }

    /**
     * Calculates the individual attribute modifier for a given attribute score.
     *
     * <p>The modification is based on a predefined scale, with higher scores providing positive modifiers and lower
     * scores providing negative modifiers.</p>
     *
     * @param attributeScore the score of the attribute
     *
     * @return the attribute modifier for the given score
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static int getIndividualAttributeModifier(int attributeScore) {
        int actualScore = max(attributeScore, 0);

        return switch (actualScore) { // ATOW pg 41
            case 0 -> -4;
            case 1 -> -2;
            case 2, 3 -> -1;
            case 4, 5, 6 -> 0;
            case 7, 8, 9 -> 1;
            case 10 -> 2;
            default -> min(5, (int) floor((double) actualScore / 3));
        };
    }

    /**
     * Calculates the raw skill value based on the skill type's progression rules, level, bonus, and aging modifier.
     *
     * <p>This method determines the skill value using the following logic:</p>
     * <ul>
     *     <li>If the progression type is "count up," the value is calculated by adding the target value, level, and
     *     bonus. If the aging modifier is set, it is also added to the result.</li>
     *     <li>If the progression type is "count down," the value is calculated by subtracting the level and bonus from
     *     the target value. If the aging modifier is set, it is also subtracted from the result.</li>
     * </ul>
     *
     * @return the calculated raw skill value, including the type's target value, level, bonus, and (when applicable)
     *       the aging modifier.
     */
    private int getSkillValue() {
        int baseValue = type.getTarget();
        int valueAdjustment = isCountUp() ? level + bonus : -level - bonus;

        return baseValue + valueAdjustment + (isCountUp() ? agingModifier : -agingModifier);
    }

    /**
     * @deprecated use {@link #getTotalSkillLevel(PersonnelOptions, Attributes, int)} instead.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public int getTotalSkillLevel() {
        return level + bonus + agingModifier;
    }

    /**
     * Calculates the total skill level for a character, factoring in the level, bonuses, aging modifiers, SPA
     * modifiers, and attribute modifiers.
     *
     * <p>The total skill level is determined by summing:</p>
     * <ul>
     *   <li>The base level of the skill, any additional bonuses, and any modifiers due to aging.</li>
     *   <li>SPA modifiers based on the provided character options and reputation.</li>
     *   <li>Attribute-based modifiers relevant to the skill type derived from the character's attributes.</li>
     * </ul>
     *
     * @param characterOptions the {@link PersonnelOptions} defining character-specific modifiers, including SPAs
     * @param attributes       the {@link Attributes} representing the character's current attribute values
     * @param reputation       a numerical modifier for reputation affecting skill level (positive or negative)
     *
     * @return the complete skill level after all relevant modifiers have been applied
     *
     * @author Illiani
     * @since 0.50.06
     */
    public int getTotalSkillLevel(PersonnelOptions characterOptions, Attributes attributes, int reputation) {
        int baseValue = level + bonus + agingModifier;

        int modifiers = getModifiers(characterOptions, attributes, reputation);

        return baseValue + modifiers;
    }

    /**
     * Calculates the total modifiers for a character based on their SPA (Special Pilot Abilities), attributes, possible
     * illiteracy penalty, and reputation.
     *
     * @param characterOptions the {@link PersonnelOptions} containing the character's options and SPAs
     * @param attributes       the {@link Attributes} object representing the character's attributes
     * @param reputation       the character's reputation value
     *
     * @return the sum of SPA modifiers, attribute-based modifiers, and any additional penalty (such as for illiteracy)
     *
     * @author Illiani
     * @since 0.50.07
     */
    private int getModifiers(PersonnelOptions characterOptions, Attributes attributes, int reputation) {
        int spaModifiers = getSPAModifiers(characterOptions, reputation);
        int attributeModifiers = getTotalAttributeModifier(new TargetRoll(), attributes, type);

        boolean isIntelligenceBased = INTELLIGENCE.equals(type.getFirstAttribute())
                                            || INTELLIGENCE.equals(type.getSecondAttribute());
        int literacyModifier = isIntelligenceBased && attributes.isIlliterate()
                                     ? UNTRAINED_SKILL_MODIFIER : 0;

        return spaModifiers + attributeModifiers + literacyModifier;
    }

    /**
     * Calculates the total skill level for a character using the base level, bonuses, aging modifiers, SPA modifiers,
     * and attribute modifiers. In this version, reputation is not considered.
     *
     * <p>The computation sums:</p>
     * <ul>
     *   <li>The base skill level, any additional bonuses, and modifiers due to aging.</li>
     *   <li>SPA modifiers, determined from the given character options (with a reputation value of zero).</li>
     *   <li>Attribute-based modifiers for the skill type, derived from the provided attributes.</li>
     * </ul>
     *
     * @param characterOptions the {@link PersonnelOptions} defining character-specific modifiers, including SPAs
     * @param attributes       the {@link Attributes} representing the character's current attribute values
     *
     * @return the complete skill level after all relevant modifiers (excluding reputation) have been applied
     */
    public int getTotalSkillLevel(PersonnelOptions characterOptions, Attributes attributes) {
        return getTotalSkillLevel(characterOptions, attributes, 0);
    }

    public void improve() {
        if (level >= SkillType.NUM_LEVELS - 1) {
            // Can't improve past the max
            return;
        }
        level = level + 1;
        // if the cost for the next level is zero (or less than zero), then
        // keep improve until you hit a non-zero cost
        if (type.getCost(level) <= 0) {
            improve();
        }
    }

    /**
     * Calculates the cost required to improve this skill to a higher level.
     *
     * <p>This method iterates through skill levels starting from the current level and returns
     * the cost for the next valid level if it exists.</p>
     *
     * <p><b>Usage:</b> For most use cases you probably want to call {@code getCostToImprove(String)} from a
     * {@link Person} object.</p>
     *
     * @return the cost to improve the skill, or 0 if no valid level with a positive cost is found.
     */
    public int getCostToImprove() {
        int cost = 0;
        int i = 1;
        while (cost <= 0 && (level + i) < SkillType.NUM_LEVELS) {
            cost = type.getCost(level + i);
            ++i;
        }
        return cost;
    }

    /**
     * @deprecated use {@link #getSkillLevel(PersonnelOptions, Attributes, int)} instead.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public SkillLevel getSkillLevel() {
        return getSkillLevel(new PersonnelOptions(), new Attributes(), 0);
    }

    /**
     * Determines the {@link SkillLevel} of a character based on their options, attributes, and reputation.
     *
     * <p>This method calculates the experience level index using the provided {@code characterOptions},
     * {@code attributes}, and {@code reputation}, and returns the corresponding {@link SkillLevel} from the
     * {@link Skills#SKILL_LEVELS} array. The returned value represents the skill proficiency tier for the given
     * parameters.</p>
     *
     * @param characterOptions the SPAs specific to the character
     * @param attributes       the character's attributes used in skill evaluation
     * @param reputation       the reputation value influencing skill evaluation
     *
     * @return the corresponding {@link SkillLevel} for the evaluated experience level
     */
    public SkillLevel getSkillLevel(PersonnelOptions characterOptions, Attributes attributes, int reputation) {
        // Returns the SkillLevel Enum value equivalent to the Experience Level Magic Number
        return Skills.SKILL_LEVELS[getExperienceLevel(characterOptions, attributes, reputation) + 1];
    }

    /**
     * Determines the {@link SkillLevel} of a character based on their options and attributes.
     *
     * <p>This method calculates the experience level index using the provided {@code characterOptions} and
     * {@code attributes}, and returns the corresponding {@link SkillLevel} from the {@code Skills.SKILL_LEVELS} array.
     * The returned value represents the skill proficiency tier for the given parameters.</p>
     *
     * @param characterOptions the SPAs specific to the character
     * @param attributes       the character's attributes used in skill evaluation
     *
     * @return the corresponding {@link SkillLevel} for the evaluated experience level
     */
    public SkillLevel getSkillLevel(PersonnelOptions characterOptions, Attributes attributes) {
        // Returns the SkillLevel Enum value equivalent to the Experience Level Magic Number
        return getSkillLevel(characterOptions, attributes, 0);
    }

    /**
     * @deprecated use {@link #getExperienceLevel(PersonnelOptions, Attributes, int)} or
     *       {@link #getExperienceLevel(PersonnelOptions, Attributes)} instead.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public int getExperienceLevel() {
        return type.getExperienceLevel(getTotalSkillLevel(new PersonnelOptions(), new Attributes(), 0));
    }

    /**
     * Calculates and returns the experience level for this skill based on the given personnel options, attributes, and
     * reputation.
     *
     * <p>This method uses the specified character's options, attributes, and reputation to
     * determine the total skill level and then delegates to the skill type to derive the corresponding experience
     * level.</p>
     *
     * @param characterOptions the {@link PersonnelOptions} representing character-specific options
     * @param attributes       the {@link Attributes} possessed by the character
     * @param reputation       the reputation value to factor into the skill calculation
     *
     * @return the computed experience level as determined by the underlying skill type
     *
     * @author Illiani
     * @since 0.50.06
     */
    public int getExperienceLevel(PersonnelOptions characterOptions, Attributes attributes, int reputation) {
        int totalSkillLevel = getTotalSkillLevel(characterOptions, attributes, reputation);
        return type.getExperienceLevel(totalSkillLevel);
    }

    /**
     * Calculates and returns the experience level for this skill based on the given personnel options and attributes,
     * ignoring reputation.
     *
     * <p><b>Usage:</b> This is a convenience method that assumes a reputation value of zero. It should only be used
     * in situations where we are 100% sure Reputation has no impact on the skill level.</p>
     *
     * @param characterOptions the {@link PersonnelOptions} representing character-specific options
     * @param attributes       the {@link Attributes} possessed by the character
     *
     * @return the computed experience level as determined by the underlying skill type
     *
     * @author Illiani
     * @since 0.50.06
     */
    public int getExperienceLevel(PersonnelOptions characterOptions, Attributes attributes) {
        int totalSkillLevel = getTotalSkillLevel(characterOptions, attributes);
        return type.getExperienceLevel(totalSkillLevel);
    }

    /**
     * Returns a string representation of the object using default parameters.
     *
     * <p>This method calls {@link #toString(PersonnelOptions, Attributes, int)} with a default
     * {@link PersonnelOptions} instance and a reputation value of {@code 0}.</p>
     *
     * <p><b>Usage:</b> Generally you want to use the above-cited method, and pass in the character's SPAs and
     * Reputation. As those can have an effect on the skill's Target Number. If you don't care about SPAs and
     * Reputation, though, this method is great shortcut.</p>
     *
     * @return A string representation of the object based on default options and reputation.
     */
    @Override
    public String toString() {
        return toString(new PersonnelOptions(), new Attributes(), 0);
    }

    /**
     * Returns a string representation of the object based on the given parameters.
     *
     * <ul>
     *   <li>If {@link #isCountUp()} is {@code true}, the final skill value is prefixed with a plus sign (<code>+</code>).</li>
     *   <li>Otherwise, the final skill value is suffixed with a plus sign (<code>+</code>).</li>
     * </ul>
     *
     * @param options            The {@link PersonnelOptions} to use for calculating the final skill value.
     * @param adjustedReputation The reputation value used in the calculation.
     *
     * @return A string representation of the calculated final skill value, formatted depending on the state of
     *       {@link #isCountUp()}.
     *
     * @see #isCountUp()
     * @see #getFinalSkillValue(PersonnelOptions, Attributes, int)
     */
    public String toString(PersonnelOptions options, Attributes attributes, int adjustedReputation) {
        String display;

        if (isCountUp()) {
            display = "+" + getFinalSkillValue(options, attributes, adjustedReputation);
        } else {
            display = getFinalSkillValue(options, attributes, adjustedReputation) + "+";
        }

        if (type.isSkillLevelsMatter()) {
            int totalSkillLevel = getTotalSkillLevel(options, attributes, adjustedReputation);
            display += String.format(" (%d)", totalSkillLevel);
        }

        return display;
    }

    /**
     * * @deprecated use {@link #toString(PersonnelOptions, Attributes, int)} instead
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public String toString(PersonnelOptions options, int reputation) {
        if (isCountUp()) {
            return "+" + getFinalSkillValue(options, new Attributes(), reputation);
        } else {
            return getFinalSkillValue(options, new Attributes(), reputation) + "+";
        }
    }

    /**
     * Creates an HTML-formatted tooltip string for the skill, incorporating flavor text, aging modifiers, SPA
     * modifiers, and linked attribute modifiers. The content is constructed using resource bundle formatting and the
     * provided personnel options, attribute values, and reputation adjustment.
     *
     * @param options            the personnel options affecting SPA calculation
     * @param attributes         the set of attributes used to determine modifier values
     * @param adjustedReputation the reputation value impacting the tooltip details
     *
     * @return an HTML-formatted string representing the generated skill tooltip
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getTooltip(PersonnelOptions options, Attributes attributes, int adjustedReputation) {
        StringBuilder tooltip = new StringBuilder();

        String flavorText = getType().getFlavorText(false, false);
        if (!flavorText.isBlank()) {
            tooltip.append(flavorText).append("<br><br>");
        }

        if (agingModifier != 0) {
            tooltip.append(getFormattedTextAt(RESOURCE_BUNDLE,
                  "tooltip.format.aging",
                  (agingModifier > 0 ? "+" : "") + agingModifier));
        }

        int spaModifier = getSPAModifiers(options, adjustedReputation);
        if (spaModifier != 0) {
            tooltip.append(getFormattedTextAt(RESOURCE_BUNDLE,
                  "tooltip.format.spa",
                  (spaModifier > 0 ? "+" : "") + spaModifier));
        }

        SkillAttribute firstLinkedAttribute = type.getFirstAttribute();
        int firstLinkedAttributeModifier = attributes.getAttributeModifier(firstLinkedAttribute);
        String additionSymbol = getTextAt(RESOURCE_BUNDLE, "tooltip.format.addition");
        tooltip.append(getFormattedTextAt(RESOURCE_BUNDLE,
              "tooltip.format.linkedAttribute",
              firstLinkedAttribute.getLabel(),
              (firstLinkedAttributeModifier > 0 ? additionSymbol : "") + firstLinkedAttributeModifier));

        SkillAttribute secondLinkedAttribute = type.getSecondAttribute();
        if (secondLinkedAttribute != SkillAttribute.NONE) {
            int secondLinkedAttributeModifier = attributes.getAttributeModifier(secondLinkedAttribute);
            tooltip.append(getFormattedTextAt(RESOURCE_BUNDLE,
                  "tooltip.format.linkedAttribute",
                  secondLinkedAttribute.getLabel(),
                  (secondLinkedAttributeModifier > 0 ? additionSymbol : "") + secondLinkedAttributeModifier));
        }

        return tooltip.toString();
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "skill");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", type.getName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "level", level);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "bonus", bonus);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "agingModifier", agingModifier);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "skill");
    }

    public static Skill generateInstanceFromXML(final Node wn) {
        Skill retVal = null;

        try {
            retVal = new Skill();

            // Okay, now load Skill-specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("type")) {
                    String text = wn2.getTextContent();
                    retVal.type = SkillType.getType(text);
                } else if (wn2.getNodeName().equalsIgnoreCase("level")) {
                    retVal.level = MathUtility.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("bonus")) {
                    retVal.bonus = MathUtility.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("agingModifier")) {
                    retVal.agingModifier = MathUtility.parseInt(wn2.getTextContent());
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }

        return retVal;
    }

    public void updateType() {
        type = SkillType.getType(type.getName());
    }
}
