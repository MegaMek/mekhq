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
 */
package mekhq.campaign.personnel.skills;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static megamek.codeUtilities.MathUtility.clamp;
import static megamek.common.Compute.d6;
import static mekhq.campaign.personnel.skills.Attributes.MAXIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MINIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Skill.COUNT_DOWN_MIN_VALUE;
import static mekhq.campaign.personnel.skills.Skill.COUNT_UP_MAX_VALUE;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.BARELY_MADE_IT;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginOfSuccessString;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginValue;

import java.util.List;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

/**
 * This class calculates the target number for a skill check based on the person's attributes, skills, and the
 * associated skill type. It determines if the skill check succeeds or fails by rolling dice and calculates the
 * resulting margin of success and corresponding text description.
 *
 * @since 0.50.5
 */
public class SkillCheckUtility {
    /**
     * The target number for an untrained skill check with one linked attribute.
     */
    private static final int UNTRAINED_TARGET_NUMBER_ONE_LINKED_ATTRIBUTE = 12; // ATOW pg 43

    /**
     * The target number for an untrained skill check with two linked attributes.
     */
    private static final int UNTRAINED_TARGET_NUMBER_TWO_LINKED_ATTRIBUTES = 18; // ATOW pg 43

    private int marginOfSuccess;
    private String resultsText;
    private int targetNumber;
    private int roll;
    private boolean usedEdge;

    /**
     * Performs a skill check for the given person and skill.
     *
     * <p>This constructor performs a skill check by rolling dice and determining the outcome based on the person's
     * target number and an optional use of edge.</p>
     *
     * <p><b>Usage:</b> This constructor gives you a lot of control over what information you need, but for most
     * use-cases you can get away with using the lazy method: {@link #performQuickSkillCheck(Person, String)}.</p>
     *
     * @param person    the {@link Person} performing the skill check
     * @param skillName the name of the skill being used
     * @param useEdge   whether the person should use edge for a re-roll if the first attempt fails
     *
     * @since 0.50.5
     */
    public SkillCheckUtility(final Person person, final String skillName, final boolean useEdge) {
        targetNumber = determineTargetNumber(person, skillName);
        roll = d6(2);
        int availableEdge = person.getCurrentEdge();

        if (roll >= targetNumber || !useEdge || availableEdge < 1) {
            marginOfSuccess = MarginOfSuccess.getMarginOfSuccess(roll);
            resultsText = getMarginOfSuccessString(marginOfSuccess);
            return;
        }

        person.changeCurrentEdge(-1);
        usedEdge = true;

        roll = d6(2);
        marginOfSuccess = MarginOfSuccess.getMarginOfSuccess(roll);
        resultsText = getMarginOfSuccessString(marginOfSuccess);
    }

    /**
     * Performs a quick skill check for a person based on the specified skill name.
     *
     * <p>This method creates a {@link SkillCheckUtility} instance to evaluate whether the given person is successful
     * in performing the specified skill.</p>
     *
     * <p><b>Usage:</b> This is a nice, quick lazy method for performing a skill check. For most use-cases across
     * MekHQ this is the method you want to use. If you need more control use the class constructor, instead.</p>
     *
     * @param person    the {@link Person} performing the skill check
     * @param skillName the name of the skill being checked
     *
     * @return {@code true} if the skill check is successful, {@code false} otherwise
     *
     * @since 0.50.5
     */
    public boolean performQuickSkillCheck(final Person person, final String skillName) {
        SkillCheckUtility skillCheck = new SkillCheckUtility(person, skillName, false);
        return skillCheck.isSuccess();
    }

    /**
     * Gets the calculated margin of success for this skill check.
     *
     * <p>The margin of success represents how much better (or worse) the roll was compared to the target number.</p>
     *
     * <p><b>Usage:</b> You want to call this method whenever you care about how well a check was passed. Or how
     * badly it was failed. If you only care whether the check was passed or failed use {@link #isSuccess()}
     * instead.</p>
     *
     * @return the margin of success
     *
     * @since 0.50.5
     */
    public int getMarginOfSuccess() {
        return marginOfSuccess;
    }

    /**
     * Determines whether the skill check was successful.
     *
     * <p>A skill check is considered successful if the calculated margin of success is greater than or equal to the
     * margin value of {@link MarginOfSuccess#BARELY_MADE_IT}.</p>
     *
     * <p><b>Usage:</b> You want to call this method whenever you only care whether the check was passed or failed.
     * If you want to know how well the character did use {@link #getMarginOfSuccess()} instead.</p>
     *
     * @return {@code true} if the skill check succeeded, {@code false} otherwise
     *
     * @since 0.50.5
     */
    public boolean isSuccess() {
        return marginOfSuccess >= getMarginValue(BARELY_MADE_IT);
    }

    /**
     * Gets the results text for the margin of success.
     *
     * <p>This is a descriptive string representing the outcome of the skill check, based on the calculated margin of
     * success.</p>
     *
     * @return the results text for the skill check
     *
     * @since 0.50.5
     */
    public String getResultsText() {
        return resultsText;
    }

    /**
     * Gets the target number for the skill check.
     *
     * <p>The target number represents the value that the rolled number must meet or exceed for the skill check to
     * succeed.</p>
     *
     * @return the target number for the skill check
     *
     * @since 0.50.5
     */
    public int getTargetNumber() {
        return targetNumber;
    }


    /**
     * Gets the roll result for the skill check.
     *
     * <p>The roll is the result of the dice roll used to determine whether the skill check succeeded or failed.</p>
     *
     * @return the roll result for the skill check
     *
     * @since 0.50.5
     */
    public int getRoll() {
        return roll;
    }


    /**
     * Checks whether edge was used during the skill check.
     *
     * <p>Edge provides the opportunity to re-roll if the initial skill check fails, allowing a chance to improve the
     * outcome.</p>
     *
     * @return {@code true} if edge was used during the skill check, {@code false} otherwise
     *
     * @since 0.50.5
     */
    public boolean isUsedEdge() {
        return usedEdge;
    }

    /**
     * Determines the target number for a skill check based on the person's attributes, skill type, and whether they are
     * trained in the skill.
     *
     * <p>If the person is untrained, the target number is based on constants for untrained rolls and the number of
     * linked attributes. Otherwise, it is based on the final skill value and attribute modifiers.</p>
     *
     * @param person    the {@link Person} performing the skill check
     * @param skillName the name of the skill being used
     *
     * @return the target number for the skill check
     *
     * @since 0.50.5
     */
    private static int determineTargetNumber(Person person, String skillName) {
        final SkillType skillType = SkillType.getType(skillName);
        final Attributes characterAttributes = person.getATOWAttributes();

        boolean isUntrained = person.hasSkill(skillName);
        int linkedAttributeCount = skillType.getLinkedAttributeCount();

        int targetNumber;
        int attributeModifier;

        if (isUntrained) {
            targetNumber = switch (linkedAttributeCount) {
                case 1 -> UNTRAINED_TARGET_NUMBER_ONE_LINKED_ATTRIBUTE;
                case 2 -> UNTRAINED_TARGET_NUMBER_TWO_LINKED_ATTRIBUTES;
                default -> 0;
            };

            attributeModifier = getTotalAttributeScoreForSkill(characterAttributes, skillType);
        } else {
            Skill skill = person.getSkill(skillName);
            targetNumber = skill.getFinalSkillValue(person.getOptions(), person.getReputation());
            attributeModifier = getTotalAttributeModifier(characterAttributes, skillType);
        }

        if (skillType.isCountUp()) {
            targetNumber += attributeModifier;
            return min(targetNumber, COUNT_UP_MAX_VALUE);
        } else {
            targetNumber -= attributeModifier;
            return max(targetNumber, COUNT_DOWN_MIN_VALUE);
        }
    }

    /**
     * Calculates the total attribute modifier for a given skill type based on the character's attributes.
     *
     * <p>The modifier is determined by summing up the individual attribute modifiers for the skill's linked
     * attributes.</p>
     *
     * @param characterAttributes the {@link Attributes} of the person performing the skill check
     * @param skillType           the {@link SkillType} being checked
     *
     * @return the total attribute modifier for the skill check
     *
     * @since 0.50.5
     */
    public static int getTotalAttributeModifier(final Attributes characterAttributes, final SkillType skillType) {
        List<SkillAttribute> linkedAttributes = List.of(skillType.getFirstAttribute(), skillType.getSecondAttribute());

        int totalModifier = 0;
        for (SkillAttribute attribute : linkedAttributes) {
            int attributeScore = characterAttributes.getAttribute(attribute);
            totalModifier += getIndividualAttributeModifier(attributeScore);
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
     * @since 0.50.5
     */
    public static int getIndividualAttributeModifier(int attributeScore) {
        int actualScore = clamp(attributeScore, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
        return switch (actualScore) {
            case 0 -> -4;
            case 1 -> -2;
            case 2, 3 -> -1;
            case 4, 5, 6 -> 0;
            case 7, 8, 9 -> 1;
            case 10 -> 2;
            default -> min(5, (int) floor((double) actualScore / 3)); // ATOW pg 41
        };
    }

    /**
     * Calculates the total score for all attributes linked to a given skill type.
     *
     * <p>This method sums the raw values of the attributes linked to the skill, without applying any modifiers.</p>
     *
     * @param characterAttributes the {@link Attributes} of the person performing the skill check
     * @param skillType           the {@link SkillType} being checked
     *
     * @return the total raw attribute score for the given skill type
     *
     * @since 0.50.5
     */
    public static int getTotalAttributeScoreForSkill(final Attributes characterAttributes, final SkillType skillType) {
        List<SkillAttribute> linkedAttributes = List.of(skillType.getFirstAttribute(), skillType.getSecondAttribute());

        int totalScore = 0;
        for (SkillAttribute attribute : linkedAttributes) {
            totalScore += characterAttributes.getAttribute(attribute);
        }
        return totalScore;
    }
}
