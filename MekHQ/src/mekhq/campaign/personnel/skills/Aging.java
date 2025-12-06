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

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.enums.AgingMilestone;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

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
     * Returns the age-based skill modifier for a given attribute.
     *
     * <p>This method determines which {@link AgingMilestone} applies to the character's current age, retrieves the
     * milestone's raw attribute modifier, and scales it according to the global {@code AGING_SKILL_MODIFIER_DIVIDER}.
     * The result is rounded to the nearest whole number before returning.
     *
     * <p>Age modifiers represent the gradual impact of aging on physical and mental attributes. Depending on the
     * milestone, modifiers may range from positive (reflecting growth and maturation) to negative (reflecting aging
     * penalties).
     *
     * @param characterAge the character's current age in years
     * @param attribute    the {@link SkillAttribute} being tested
     *
     * @return the rounded, scaled age modifier affecting the specified attribute
     */
    public static int getAgeModifier(int characterAge, SkillAttribute attribute) {
        AgingMilestone milestone = getMilestone(characterAge);
        int modifier = milestone.getAttributeModifier(attribute);

        double totalModifier = (double) modifier / AGING_SKILL_MODIFIER_DIVIDER;
        return (int) round(totalModifier);
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
}
