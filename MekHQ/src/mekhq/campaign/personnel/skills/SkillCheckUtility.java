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

import static megamek.common.compute.Compute.d6;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.List;

import megamek.common.compute.Compute;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

/**
 * This class calculates the target number for a skill check based on the person's attributes, skills, and the
 * associated skill type. It determines if the skill check succeeds or fails by rolling dice and calculates the
 * resulting margin of success and corresponding text description.
 *
 * @author Illiani
 * @since 0.50.05
 */
public class SkillCheckUtility {

    // only static methods
    private SkillCheckUtility() {
    }

    private static final MMLogger LOGGER = MMLogger.create(SkillCheckUtility.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SkillCheckUtility";

    /**
     * The target number for an untrained skill check with one linked attribute.
     */
    protected static final int UNTRAINED_TARGET_NUMBER_ONE_LINKED_ATTRIBUTE = 12; // ATOW pg 43

    /**
     * The target number for an untrained skill check with two linked attributes.
     */
    protected static final int UNTRAINED_TARGET_NUMBER_TWO_LINKED_ATTRIBUTES = 18; // ATOW pg 43

    /**
     * The penalty for attempting a skill check with an untrained skill.
     */
    protected static final int UNTRAINED_SKILL_MODIFIER = 4; // ATOW pg 43

    /**
     * Determines the target number for a skill check based on the person's attributes, skill type, and whether they are
     * trained in the skill.
     *
     * <p>If the person is untrained, the target number is based on constants for untrained rolls and the number of
     * linked attributes. Otherwise, it is based on the final skill value and attribute modifiers.</p>
     *
     * @param person            the {@link Person} performing the skill check
     * @param skillType         the associated {@link SkillType} for the {@link Skill} being used.
     * @param isUseAgingEffects if {@code true}, considers aging effects during the check
     * @param isClanCampaign    if {@code true}, applies rules specific to clan campaigns
     * @param today             the current date, used for time-dependent logic
     *
     * @return the {@link TargetRoll} for the skill check
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static TargetRoll determineTargetNumber(Person person, SkillType skillType,
          boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today) {
        if (person == null) {
            throw new IllegalArgumentException("person == null in determineTargetNumber");
        }

        final String skillName = skillType.getName();
        boolean isUntrained = !person.hasSkill(skillName);
        TargetRoll targetNumber = new TargetRoll();

        if (isUntrained) {
            int linkedAttributeCount = skillType.getLinkedAttributeCount();

            if (linkedAttributeCount > 1) {
                targetNumber.addModifier(UNTRAINED_TARGET_NUMBER_TWO_LINKED_ATTRIBUTES,
                      getFormattedTextAt(RESOURCE_BUNDLE, "skillCheck.untrained.twoLinkedAttributes"));
            } else {
                targetNumber.addModifier(UNTRAINED_TARGET_NUMBER_ONE_LINKED_ATTRIBUTE,
                      getFormattedTextAt(RESOURCE_BUNDLE, "skillCheck.untrained.oneLinkedAttribute"));
            }

            getTotalAttributeScoreForSkill(targetNumber, person.getATOWAttributes(), skillType);

            targetNumber.addModifier(UNTRAINED_SKILL_MODIFIER,
                  getFormattedTextAt(RESOURCE_BUNDLE, "skillCheck.untrained.skill"));
        } else {
            Skill skill = person.getSkill(skillName);
            int skillValue = skill.getFinalSkillValue(person.getSkillModifierData(isUseAgingEffects, isClanCampaign,
                  today));
            targetNumber.addModifier(skillValue, skillName);
        }

        return targetNumber;
    }

    /**
     * Generates the roll value for a check, applying the natural aptitude if applicable.
     *
     * @param hasNaturalAptitude {@code true} to roll 3d6 and keep the highest 2; {@code false} to roll 2d6
     * @return the result of the dice roll
     */
    static int getRoll(boolean hasNaturalAptitude) {
        int roll1 = d6(1);
        int roll2 = d6(1);
        int roll3 = hasNaturalAptitude ? d6(1) : 0;

        return Compute.getHighestTwoIntegers(roll1, roll2, roll3);
    }

    /**
     * Applies attribute-based modifiers to a target roll and calculates the total attribute score for a given skill.
     *
     * <p>This method retrieves the attributes linked to a specified {@link SkillType} and calculates
     * their total contribution to both:</p>
     * <ul>
     *   <li>The target roll by applying modifiers (negative of the attribute values), and</li>
     *   <li>The total attribute score, which it returns as an integer.</li>
     * </ul>
     * <p>Attributes that are set to {@link SkillAttribute#NONE} are ignored during this process.</p>
     *
     * <p>For each relevant attribute:</p>
     * <ul>
     *   <li>The method adds the negative of the attribute value as a modifier to the {@link TargetRoll}
     *       using {@link TargetRoll#addModifier(int, String)}, where the second parameter is the attribute's label.</li>
     *   <li>The total attribute score is incremented by the raw attribute value.</li>
     * </ul>
     *
     * @param targetNumber        the {@link TargetRoll} representing the current target number, which will be adjusted
     *                            based on the character's attribute values
     * @param characterAttributes the {@link Attributes} object representing the character's attributes that contribute
     *                            to the skill check
     * @param skillType           the {@link SkillType} being assessed, whose linked attributes determine the modifiers
     *                            to be applied
     *
     * @return the total attribute score summed from all relevant attributes linked to the skill. If any of the
     *       parameters are {@code null}, the method will log an error and return {@code 0}.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static int getTotalAttributeScoreForSkill(TargetRoll targetNumber, final Attributes characterAttributes,
          final SkillType skillType) {
        // Validation
        if (targetNumber == null || characterAttributes == null || skillType == null) {
            LOGGER.error("Null parameter passed into SkillCheckUtility.getTotalAttributeScoreForSkill." +
                               " targetNumber: {}, characterAttributes: {}, skillType: {}",
                  targetNumber,
                  characterAttributes,
                  skillType);
            return 0;
        }

        int totalModifier = 0;
        List<SkillAttribute> linkedAttributes = List.of(skillType.getFirstAttribute(), skillType.getSecondAttribute());

        for (SkillAttribute attribute : linkedAttributes) {
            if (attribute == SkillAttribute.NONE) {
                continue;
            }

            int attributeScore = characterAttributes.getAttribute(attribute);
            totalModifier += attributeScore;
            targetNumber.addModifier(-attributeScore, attribute.getLabel());
        }

        return totalModifier;
    }
}
