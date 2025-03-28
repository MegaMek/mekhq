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
package mekhq.campaign.personnel;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.PrintWriter;

import megamek.common.Compute;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.randomEvents.personalities.enums.Intelligence;
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
 * countUp - this is a boolean that defines whether this skill's target is a "roll greater than or equal to" (false) or
 * an rpg-style bonus to a roll (true)
 * <p>
 * The actual target number for a skill is given by
 * <p>
 * countUp: target + lvl + bonus !countUp: target - level - bonus
 * <p>
 * by clever manipulation of these values and skill costs in campaignOptions, players should be able to recreate any of
 * the rpg versions or their own homebrew system. The default setup will follow the core rule books (not aToW).
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Skill {
    private static int COUNT_UP_MAX_VALUE = 10;
    private static int COUNT_DOWN_MIN_VALUE = 0;

    private static final MMLogger logger = MMLogger.create(Skill.class);

    private SkillType type;
    private int level;
    private int bonus;

    protected Skill() {

    }

    public Skill(String type) {
        this.type = SkillType.getType(type);
        this.level = this.type.getLevelFromExperience(SkillType.EXP_REGULAR);
    }

    public Skill(String type, int level, int bns) {
        this(SkillType.getType(type), level, bns);
    }

    public Skill(SkillType type, int level, int bonus) {
        this.type = type;
        this.level = level;
        this.bonus = bonus;
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
        return type.countUp();
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

    public SkillType getType() {
        return type;
    }

    /**
     * Calculates the final skill value based on the progression type, the current level, and any applicable bonuses,
     * while ensuring the value stays within the legal bounds for the skill type.
     *
     * <p>For "count up" progression types, the final skill value is capped at the maximum allowed value. For "count
     * down" progression types, the final skill value is capped at the minimum allowed value.</p>
     *
     * @return the final skill value after applying progression rules and limiting it to the legal range.
     */
    public int getFinalSkillValue() {
        if (isCountUp()) {
            return min(COUNT_UP_MAX_VALUE, getSkillValue());
        } else {
            return max(COUNT_DOWN_MIN_VALUE, getSkillValue());
        }
    }

    /**
     * Calculates the raw skill value based on the type's progression rules, level, and bonus.
     *
     * <p>For "count up" progression types, the skill value is determined by adding the target value, level, and bonus.
     * For "count down" progression types, it is determined by subtracting the level and bonus from the target
     * value.</p>
     *
     * @return the calculated raw skill value before applying any boundaries or limits.
     */
    private int getSkillValue() {
        if (isCountUp()) {
            return type.getTarget() + level + bonus;
        } else {
            return type.getTarget() - level - bonus;
        }
    }

    /**
     * Calculates the total skill value by summing the level and bonus.
     *
     * @return The total skill value.
     */
    public int getTotalSkillLevel() {
        return level + bonus;
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
     * {@link Person} object, as that will factor in things like {@link Intelligence}.</p>
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
        return type.getExperienceLevel(getLevel());
    }

    @Override
    public String toString() {
        if (isCountUp()) {
            return "+" + getFinalSkillValue();
        } else {
            return getFinalSkillValue() + "+";
        }
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "skill");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", type.getName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "level", level);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "bonus", bonus);
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
                    retVal.level = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("bonus")) {
                    retVal.bonus = Integer.parseInt(wn2.getTextContent());
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
