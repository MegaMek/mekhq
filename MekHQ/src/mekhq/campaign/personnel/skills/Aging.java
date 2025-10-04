/*
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.skills;

import static java.lang.Math.round;
import static megamek.common.options.PilotOptions.LVL3_ADVANTAGES;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_FAST_LEARNER;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_TOUGHNESS;
import static mekhq.campaign.personnel.PersonnelOptions.FLAW_GLASS_JAW;
import static mekhq.campaign.personnel.PersonnelOptions.FLAW_SLOW_LEARNER;
import static mekhq.campaign.personnel.skills.enums.AgingMilestone.CLAN_REPUTATION_MULTIPLIER;
import static mekhq.campaign.personnel.skills.enums.AgingMilestone.NONE;
import static mekhq.campaign.personnel.skills.enums.AgingMilestone.STAR_CAPTAIN_RANK_INDEX;
import static mekhq.campaign.personnel.skills.enums.AgingMilestone.STAR_CAPTAIN_REPUTATION_MULTIPLIER;
import static mekhq.campaign.personnel.skills.enums.AgingMilestone.STAR_COLONEL_RANK_INDEX;
import static mekhq.campaign.personnel.skills.enums.AgingMilestone.STAR_COLONEL_REPUTATION_MULTIPLIER;
import static mekhq.campaign.personnel.skills.enums.AgingMilestone.TWENTY_FIVE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.NO_SKILL_ATTRIBUTE;

import java.time.LocalDate;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.enums.AgingMilestone;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillTypeNew;

public class Aging {
    /**
     * A constant used to divide the sum of skill attribute modifiers in the aging calculations.
     *
     * <p>When calculating skill modifiers from aging, we take the ATOW values and then divide them by this value.
     * This is because modifying the experience a character has spent on {@code x} skill would get complicated quickly,
     * so we use this workaround.</p>
     */
    public static final int AGING_SKILL_MODIFIER_DIVIDER = 100;

    /**
     * Updates the aging modifiers for all skills of a given {@link Person} based on their current age and attributes.
     *
     * <p>This method calculates the appropriate aging modifier for each skill of a person using the person's age (as
     * of the provided date), associated {@link AgingMilestone}, and skill attributes. If {@code updateMissingOnly} is
     * true, only skills with an unset aging modifier are updated.</p>
     *
     * <p><strong>Behavior:</strong></p>
     * <ul>
     *     <li>Determines the {@link AgingMilestone} based on the person's age.</li>
     *     <li>Iterates over all skills defined in {@link SkillTypeNew}.</li>
     *     <li>If the person doesn't have a specific skill, it skips processing that skill.</li>
     *     <li>Retrieves the skill's attributes and calculates the aging modifier using {@link #getAgeModifier(AgingMilestone,
     *     SkillAttribute, SkillAttribute)}.</li>
     *     <li>Sets the calculated modifier for each applicable skill.</li>
     * </ul>
     *
     * @param today  the current date used to calculate the person's age
     * @param person the {@link Person} whose skills should be updated
     */
    public static void updateAllSkillAgeModifiers(LocalDate today, Person person) {
        AgingMilestone milestone = getMilestone(person.getAge(today));

        for (SkillTypeNew skillType : SkillTypeNew.values()) {
            boolean hasSkill = person.hasSkill(skillType.name());

            if (!hasSkill) {
                continue;
            }

            Skill skill = person.getSkill(skillType.name());

            SkillTypeNew type = SkillTypeNew.getType(skillType.name());
            SkillAttribute firstAttribute = type.getFirstAttribute();
            SkillAttribute secondAttribute = type.getSecondAttribute();

            int modifier = getAgeModifier(milestone, firstAttribute, secondAttribute);

            skill.setAgingModifier(modifier);
        }
    }

    /**
     * Resets all age-related modifiers for the skills of a given person to zero.
     *
     * <p>This method iterates through all skills in {@link SkillTypeNew} and, for each skill that
     * the person possesses, sets its aging modifier to zero. Skills that the person does not have are ignored.</p>
     *
     * @param person The person whose skill age modifiers will be cleared.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static void clearAllAgeModifiers(Person person) {
        for (SkillTypeNew typeNew : SkillTypeNew.values()) {
            boolean hasSkill = person.hasSkill(typeNew.name());

            if (!hasSkill) {
                continue;
            }

            Skill skill = person.getSkill(typeNew.name());
            skill.setAgingModifier(0);
        }
    }

    /**
     * Calculates the age-related skill modifiers for a character based on their age and provided skill attributes.
     *
     * <p>This method determines the character's {@link AgingMilestone} based on their age and delegates the
     * computation of the age modifier to {@link #getAgeModifier(AgingMilestone, SkillAttribute, SkillAttribute)}.</p>
     *
     * <p><b>Usage:</b> This is an overload of the above method. You should try to use this method in the event that
     * you're only checking a single skill. As otherwise, it's better to calculate {@link AgingMilestone} once, and pass
     * that into all calls. Instead of needing to calculate it for each skill individually.</p>
     *
     * @param characterAge    the age of the character
     * @param firstAttribute  the first skill attribute to consider
     * @param secondAttribute the second skill attribute to consider
     *
     * @return the calculated skill attribute modifier after applying aging-related adjustments, or {@code 0} if no
     *       valid aging milestone applies
     */
    public static int getAgeModifier(int characterAge, SkillAttribute firstAttribute, SkillAttribute secondAttribute) {
        // Get the milestone for the character's age
        AgingMilestone milestone = getMilestone(characterAge);

        return getAgeModifier(milestone, firstAttribute, secondAttribute);
    }

    /**
     * Calculates the age-related skill attribute modifier for a character based on a given {@link AgingMilestone}.
     *
     * <p>This method retrieves the attribute modifiers for the provided skill attributes from the milestone, applies
     * a predefined adjustment formula, and returns the result. If both attributes have no valid skill modifiers, it
     * returns {@code 0}. If only one valid modifier exists, it applies the adjustment to that single modifier. If both
     * attributes have valid modifiers, their modifiers are summed and adjusted.</p>
     *
     * <p><b>Usage:</b> This is an alternative to the {@link #getAgeModifier(int, SkillAttribute, SkillAttribute)}
     * method and is best suited to instances where you're wanting to calculate the modifiers for multiple skills. In
     * those instances you calculate {@link AgingMilestone} once, and then pass it into this method for each skill. If
     * you're only needing to calculate a single skill, the above-cited method is better suited as it will lazily
     * calculate {@link AgingMilestone} for you.</p>
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *     <li>If the milestone is {@code NONE}, it returns {@code 0} immediately.</li>
     *     <li>If both skill attributes are invalid (e.g., {@code NO_SKILL_ATTRIBUTE}), the result is {@code 0}.</li>
     *     <li>If one attribute is valid, its modifier is adjusted and returned as the result.</li>
     *     <li>If both attributes are valid, the sum of their modifiers is adjusted and returned.</li>
     * </ul>
     *
     * @param milestone       the {@link AgingMilestone} applicable to the character's age
     * @param firstAttribute  the first skill attribute to consider
     * @param secondAttribute the second skill attribute to consider
     *
     * @return the calculated skill attribute modifier after applying aging-related adjustments, or {@code 0} if no 0}
     *       0} if no valid combination or milestone exists
     */
    public static int getAgeModifier(AgingMilestone milestone, SkillAttribute firstAttribute,
          SkillAttribute secondAttribute) {
        // If no milestone applies, return no modifier
        if (milestone == NONE) {
            return 0;
        }

        // Get the attribute modifiers for the provided attributes
        int firstModifier = milestone.getAttributeModifier(firstAttribute);
        int secondModifier = milestone.getAttributeModifier(secondAttribute);

        // Check for modifiers without skill attributes and compute accordingly
        if (firstModifier == NO_SKILL_ATTRIBUTE && secondModifier == NO_SKILL_ATTRIBUTE) {
            // No valid skill attributes, return no modifier (this likely suggests a malformed SkillValue)
            return 0;
        } else if (firstModifier == NO_SKILL_ATTRIBUTE) {
            return applyAgingModifier(secondModifier);
        } else if (secondModifier == NO_SKILL_ATTRIBUTE) {
            return applyAgingModifier(firstModifier);
        }

        // Average the two modifiers and apply the aging skill adjustment
        return applyAgingModifier((firstModifier + secondModifier) / 2);
    }

    /**
     * Calculates the reputation age modifier for a character based on their age, clan affiliation, blood name status,
     * and military rank.
     *
     * <p>This method determines a character's reputation age modifier by evaluating their age against a predefined
     * aging milestone, their clan affiliation, their possession of a blood name, and their rank in the clan hierarchy.
     * If the character meets specific conditions, such as holding a high enough rank or possessing a blood name, the
     * reputation multiplier is adjusted. The final result is scaled by a clan-specific reputation multiplier.</p>
     *
     * @param characterAge The age of the character for which the reputation modifier is being calculated.
     * @param isClan       Indicates whether the character is part of a clan. If {@code false}, the method returns 0.
     * @param hasBloodName Indicates whether the character possesses a blood name, which can decrease the reputation
     *                     multiplier under certain conditions.
     * @param rankIndex    The rank index of the character, used to determine if they meet rank-specific milestone
     *                     conditions for reputation adjustment.
     *
     * @return The calculated reputation age modifier. Returns 0 if the character is not a clan member.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static int getReputationAgeModifier(int characterAge, boolean isClan, boolean hasBloodName, int rankIndex) {
        if (!isClan) {
            return 0;
        }

        AgingMilestone milestone = getMilestone(characterAge);

        int reputationMultiplier = milestone.getReputation();
        boolean hasHitRankTarget = reputationMultiplier == STAR_CAPTAIN_REPUTATION_MULTIPLIER &&
                                         rankIndex >= STAR_CAPTAIN_RANK_INDEX;

        if (reputationMultiplier == STAR_COLONEL_REPUTATION_MULTIPLIER && rankIndex >= STAR_COLONEL_RANK_INDEX) {
            hasHitRankTarget = true;
        }

        if (hasHitRankTarget || hasBloodName) {
            reputationMultiplier--;
        }

        int modifier = reputationMultiplier * CLAN_REPUTATION_MULTIPLIER;
        double totalModifier = (double) modifier / AGING_SKILL_MODIFIER_DIVIDER;
        return (int) round(totalModifier);
    }

    /**
     * Applies age-related special abilities or flaws to a given person based on their age.
     *
     * <p>This method evaluates the character's age against predefined aging milestones, and if the age matches
     * a milestone, specific effects such as applying flaws or adjusting abilities are triggered. For example, it may
     * apply the "Glass Jaw" flaw or interact with existing abilities like "Toughness".</p>
     *
     * @param characterAge The age of the character, used to determine applicable aging milestones and effects.
     * @param person       The person to whom the aging-related effects will be applied.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static void applyAgingSPA(int characterAge, Person person) {
        PersonnelOptions options = person.getOptions();
        for (AgingMilestone milestone : AgingMilestone.values()) {
            if (characterAge == milestone.getMinimumAge()) {
                // Glass Jaw
                if (milestone.isGlassJaw()) {
                    boolean hasGlassJaw = options.booleanOption(FLAW_GLASS_JAW);
                    boolean hasToughness = options.booleanOption(ATOW_TOUGHNESS);

                    if (hasToughness) {
                        person.getOptions().getOption(ATOW_TOUGHNESS).setValue(false);
                    } else if (!hasGlassJaw) {
                        options.acquireAbility(LVL3_ADVANTAGES, FLAW_GLASS_JAW, true);
                    }
                }

                // Slow Learner
                if (milestone.isSlowLearner()) {
                    boolean hasSlowLearner = options.booleanOption(FLAW_SLOW_LEARNER);
                    boolean hasFastLearner = options.booleanOption(ATOW_FAST_LEARNER);

                    if (hasFastLearner) {
                        person.getOptions().getOption(ATOW_FAST_LEARNER).setValue(false);
                    } else if (!hasSlowLearner) {
                        options.acquireAbility(LVL3_ADVANTAGES, FLAW_SLOW_LEARNER, true);
                    }
                }

                break;
            }
        }
    }

    /**
     * Determines the appropriate {@link AgingMilestone} for a given character's age.
     *
     * <p>If a character's age does not fall into the range of any milestone, it defaults to {@code NONE}.
     * This method is optimized to exit early for young characters whose age is below the milestone threshold.</p>
     *
     * @param characterAge the age of the character
     *
     * @return the matching {@link AgingMilestone} for the character's age, or {@code NONE} if no milestone is
     *       applicable
     */
    public static AgingMilestone getMilestone(int characterAge) {
        // Early exit, so we don't need to loop through all values for young characters
        if (characterAge < TWENTY_FIVE.getMinimumAge()) {
            return NONE;
        }

        for (AgingMilestone milestone : AgingMilestone.values()) {
            if ((characterAge >= milestone.getMinimumAge()) && (characterAge < milestone.getMaximumAge())) {
                return milestone;
            }
        }

        return NONE;
    }

    /**
     * Applies an aging adjustment to a given skill modifier sum.
     *
     * <p>The adjustment divides the sum by a predefined constant value, {@code AGING_SKILL_MODIFIER_DIVIDER},
     * and rounds to the nearest integer using {@code Math.round}.</p>
     *
     * @param modifierSum the sum of the skill attribute modifiers
     *
     * @return the adjusted skill attribute modifier after applying aging rules
     */
    private static int applyAgingModifier(int modifierSum) {
        return (int) round((double) modifierSum / AGING_SKILL_MODIFIER_DIVIDER);
    }
}
