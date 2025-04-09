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
 */
package mekhq.campaign.personnel.skills;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static mekhq.campaign.personnel.PersonnelOptions.*;
import static mekhq.campaign.personnel.skills.SkillType.S_ACTING;
import static mekhq.campaign.personnel.skills.SkillType.S_ANIMAL_HANDLING;
import static mekhq.campaign.personnel.skills.SkillType.S_NEG;
import static mekhq.campaign.personnel.skills.SkillType.S_PERCEPTION;
import static mekhq.campaign.personnel.skills.SkillType.S_PROTOCOLS;
import static mekhq.campaign.personnel.skills.SkillType.S_STREETWISE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.CHARISMA;

import java.io.PrintWriter;
import java.util.Objects;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.randomEvents.personalities.enums.Reasoning;
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
 * level - this is the level of the skill. By default this will go from 0 to 10, but the max will be customizable. These
 * won't necessarily correspond to named levels (e.g. Green, Elite).
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
 * or an rpg-style bonus to a roll (true)
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

    private static final MMLogger logger = MMLogger.create(Skill.class);

    private SkillType type;
    private int level;
    private int bonus;
    private int agingModifier;

    protected Skill() {

    }

    public Skill(String type) {
        this.type = SkillType.getType(type);
        this.level = this.type.getLevelFromExperience(SkillType.EXP_REGULAR);
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

    public void setAgingModifier(int agingModifier) {
        this.agingModifier = agingModifier;
    }

    public SkillType getType() {
        return type;
    }

    /**
     * @deprecated use {@link #getFinalSkillValue(PersonnelOptions, int)} instead.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public int getFinalSkillValue() {
        return getFinalSkillValue(new PersonnelOptions(), 0);
    }

    /**
     * Calculates the final skill value for the character based on the default reputation modifier (zero).
     *
     * <p>This is a convenience method that delegates to {@link #getFinalSkillValue(PersonnelOptions, int)}
     * with a default reputation value of {@code 0}.</p>
     *
     * <p><b>Usage:</b> This method is for when we know, 100%, that the targeted {@link Skill} is not affected by
     * the character's Reputation. If unsure, use {@link #getFinalSkillValue(PersonnelOptions, int)} instead.</p>
     *
     * @param characterOptions The {@link PersonnelOptions} to consider for determining skill value modifiers.
     *
     * @return The final skill value after applying progression rules and using a default reputation of zero.
     */
    public int getFinalSkillValue(PersonnelOptions characterOptions) {
        return getFinalSkillValue(characterOptions, 0);
    }

    /**
     * Calculates the final skill value based on the skill's progression type, current level, any applicable bonuses,
     * and a reputation modifier, while ensuring the value remains within the legal bounds for the skill type.
     *
     * <p>The calculation follows these rules based on the progression type:</p>
     * <ul>
     *   <li>For "count up" progression types, the final skill value is capped at a predefined maximum allowed value.</li>
     *   <li>For "count down" progression types, the final skill value is capped at a predefined minimum allowed value.</li>
     * </ul>
     * <p>Any reputation values are included as a modifier in the calculation.</p>
     *
     * @param characterOptions The {@link PersonnelOptions} to consider for determining skill value modifiers.
     * @param reputation       A reputation value to apply as a modifier to the skill's final value. Positive values
     *                         increase the skill value, while negative values decrease it.
     *
     * @return The final skill value after applying progression rules and clamping to the legal range.
     */
    public int getFinalSkillValue(PersonnelOptions characterOptions, int reputation) {
        int modifier = getSPAModifiers(characterOptions, reputation);
        if (isCountUp()) {
            return min(COUNT_UP_MAX_VALUE, getSkillValue() + modifier);
        } else {
            return max(COUNT_DOWN_MIN_VALUE, getSkillValue() - modifier);
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
    private int getSPAModifiers(PersonnelOptions characterOptions, int reputation) {
        int modifier = 0;

        String name = type.getName();
        // Reputation and Alternate ID
        if (Objects.equals(name, S_NEG) || Objects.equals(name, S_PROTOCOLS) || Objects.equals(name, S_STREETWISE)) {
            if (characterOptions.booleanOption(ATOW_ALTERNATE_ID) && reputation < 0) {
                reputation = min(0, reputation + 2);
            }

            modifier += reputation;
        }

        // Animal Empathy and Animal Antipathy
        if (Objects.equals(name, S_ANIMAL_HANDLING)) {
            if (characterOptions.booleanOption(FLAW_ANIMAL_ANTIPATHY)) {
                modifier += 2;
            }

            if (characterOptions.booleanOption(ATOW_ANIMAL_EMPATHY)) {
                modifier -= 2;
            }
        }

        // Attractive and Unattractive
        if (type.hasAttribute(CHARISMA)) {
            if (characterOptions.booleanOption(FLAW_UNATTRACTIVE)) {
                modifier += 2;
            }

            if (characterOptions.booleanOption(ATOW_ATTRACTIVE)) {
                modifier -= 2;
            }
        }

        // Poor Hearing, Good Hearing, Poor Vision, Good Vision, Sixth Sense
        if (Objects.equals(name, S_PERCEPTION)) {
            if (characterOptions.booleanOption(FLAW_POOR_HEARING)) {
                modifier += 1;
            }

            if (characterOptions.booleanOption(ATOW_GOOD_HEARING)) {
                modifier -= 1;
            }

            if (characterOptions.booleanOption(FLAW_POOR_VISION)) {
                modifier += 1;
            }

            if (characterOptions.booleanOption(ATOW_GOOD_VISION)) {
                modifier -= 1;
            }

            if (characterOptions.booleanOption(ATOW_SIXTH_SENSE)) {
                modifier -= 3;
            }
        }

        // Introvert, Gregarious
        if (Objects.equals(name, S_ACTING) || Objects.equals(name, S_NEG)) {
            if (characterOptions.booleanOption(FLAW_INTROVERT)) {
                modifier += 1;
            }

            if (characterOptions.booleanOption(ATOW_GREGARIOUS)) {
                modifier -= 1;
            }
        }

        // Impatient, Patient
        if (type.isAffectedByImpatientOrPatient()) {
            if (characterOptions.booleanOption(FLAW_IMPATIENT)) {
                modifier += 1;
            }

            if (characterOptions.booleanOption(ATOW_PATIENT)) {
                modifier -= 1;
            }
        }

        // Gremlins, Tech Empathy
        if (type.isAffectedByGremlinsOrTechEmpathy()) {
            if (characterOptions.booleanOption(FLAW_IMPATIENT)) {
                modifier += 1;
            }

            if (characterOptions.booleanOption(ATOW_PATIENT)) {
                modifier -= 1;
            }
        }

        return modifier;
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
     * Calculates the total skill value by summing the level, bonus, and aging modifier.
     *
     * @return The total skill value.
     */
    public int getTotalSkillLevel() {
        return level + bonus + agingModifier;
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
     * {@link Person} object, as that will factor in things like {@link Reasoning}.</p>
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

    public SkillLevel getSkillLevel() {
        // Returns the SkillLevel Enum value equivalent to the Experience Level Magic
        // Number
        return Skills.SKILL_LEVELS[getExperienceLevel() + 1];
    }

    public int getExperienceLevel() {
        return type.getExperienceLevel(getTotalSkillLevel());
    }

    /**
     * Returns a string representation of the object using default parameters.
     *
     * <p>This method calls {@link #toString(PersonnelOptions, int)} with a default
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
        return toString(new PersonnelOptions(), 0);
    }

    /**
     * Returns a string representation of the object based on the given parameters.
     *
     * <ul>
     *   <li>If {@link #isCountUp()} is {@code true}, the final skill value is prefixed with a plus sign (<code>+</code>).</li>
     *   <li>Otherwise, the final skill value is suffixed with a plus sign (<code>+</code>).</li>
     * </ul>
     *
     * @param options    The {@link PersonnelOptions} to use for calculating the final skill value.
     * @param reputation The reputation value used in the calculation.
     *
     * @return A string representation of the calculated final skill value, formatted depending on the state of
     *       {@link #isCountUp()}.
     *
     * @see #isCountUp()
     * @see #getFinalSkillValue(PersonnelOptions, int)
     */
    public String toString(PersonnelOptions options, int reputation) {
        if (isCountUp()) {
            return "+" + getFinalSkillValue(options, reputation);
        } else {
            return getFinalSkillValue(options, reputation) + "+";
        }
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
