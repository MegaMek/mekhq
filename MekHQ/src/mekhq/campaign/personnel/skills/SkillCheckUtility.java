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

import static megamek.common.Compute.d6;
import static mekhq.campaign.personnel.enums.GenderDescriptors.HIS_HER_THEIR;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.BARELY_MADE_IT;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.DISASTROUS;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginOfSuccessObjectFromMarginValue;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginOfSuccessString;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginValue;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.List;

import megamek.common.TargetRoll;
import megamek.common.TargetRollModifier;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.utilities.ReportingUtilities;

/**
 * This class calculates the target number for a skill check based on the person's attributes, skills, and the
 * associated skill type. It determines if the skill check succeeds or fails by rolling dice and calculates the
 * resulting margin of success and corresponding text description.
 *
 * @author Illiani
 * @since 0.50.05
 */
public class SkillCheckUtility {
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

    private final Person person;
    private final String skillName;
    private int marginOfSuccess;
    private String resultsText;
    private TargetRoll targetNumber;
    boolean isCountUp;
    private int roll;
    private boolean usedEdge;

    /**
     * Use
     * {@link SkillCheckUtility#SkillCheckUtility(Person, String, List, int, boolean, boolean, boolean, boolean,
     * LocalDate)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public SkillCheckUtility(final Person person, final String skillName,
          @Nullable List<TargetRollModifier> externalModifiers, final int miscModifier, final boolean useEdge,
          final boolean includeMarginsOfSuccessText) {
        this.person = person;
        this.skillName = skillName;
        SkillCheckUtility proxy = new SkillCheckUtility(person, skillName, externalModifiers, miscModifier, useEdge,
              includeMarginsOfSuccessText,
              false, false, LocalDate.of(3151, 1, 1));
        marginOfSuccess = proxy.getMarginOfSuccess();
        resultsText = proxy.getResultsText();
        targetNumber = proxy.getTargetNumber();
        roll = proxy.getRoll();
        usedEdge = proxy.isUsedEdge();
    }

    /**
     * Executes a skill check for the specified person and skill type.
     *
     * <p>This constructor creates a {@code SkillCheckUtility} instance which calculates the target number
     * for the skill check and performs the roll, determining the outcome based on factors such as the person's skill
     * level, external modifiers, miscellaneous modifiers, and whether edge is used.</p>
     *
     * <p>External modifiers can optionally influence the target number, while miscellaneous modifiers
     * alter the target based on whether the skill is classified as 'count up' or not. Using edge allows the person to
     * attempt a re-roll if the initial roll fails. Additionally, the constructor can include margins of success text as
     * part of the results, if desired.</p>
     *
     * <p><b>Usage:</b> This constructor offers detailed control over the skill check process.
     * For simpler use-cases, the
     * {@link #performQuickSkillCheck(Person, String, List, int, boolean, boolean, LocalDate)} method provides a more
     * streamlined approach.</p>
     *
     * @param person                      the {@link Person} performing the skill check
     * @param skillName                   the name of the skill being used, corresponding to a {@link SkillType}
     * @param externalModifiers           an optional list of {@link TargetRollModifier}s that affect the target number
     * @param miscModifier                a miscellaneous modifier that affects the target number:
     *                                    <ul>
     *                                        <li>For 'count up' skills, this value is subtracted from
     *                                            the target number (i.e., negative values are bonuses,
     *                                            positive values are penalties).</li>
     *                                        <li>For non-'count up' skills, this value is added to the
     *                                            target number (i.e., positive values are penalties).</li>
     *                                    </ul>
     * @param useEdge                     whether the person should use edge to re-roll if the initial attempt fails
     * @param includeMarginsOfSuccessText whether to include detailed margins of success information in the results
     * @param isUseAgingEffects           if {@code true}, considers aging effects during the check
     * @param isClanCampaign              if {@code true}, applies rules specific to clan campaigns
     * @param today                       the current date, used for time-dependent logic
     *
     * @author Illiani
     * @since 0.50.05
     */
    public SkillCheckUtility(final Person person, final String skillName,
          @Nullable List<TargetRollModifier> externalModifiers, final int miscModifier, final boolean useEdge,
          final boolean includeMarginsOfSuccessText, boolean isUseAgingEffects, boolean isClanCampaign,
          LocalDate today) {
        this.person = person;
        this.skillName = skillName;

        if (isPersonNull()) {
            return;
        }

        final SkillType skillType = SkillType.getType(skillName);
        isCountUp = skillType.isCountUp();
        targetNumber = determineTargetNumber(person, skillType, miscModifier, isUseAgingEffects, isClanCampaign, today);

        if (externalModifiers != null) {
            for (TargetRollModifier modifier : externalModifiers) {
                targetNumber.addModifier(modifier);
            }
        }

        performCheck(useEdge, includeMarginsOfSuccessText);
    }

    /**
     * Use {@link #performQuickSkillCheck(Person, String, List, int, boolean, boolean, LocalDate)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static boolean performQuickSkillCheck(final Person person, final String skillName,
          final @Nullable List<TargetRollModifier> externalModifiers, final int miscModifier) {
        return performQuickSkillCheck(person, skillName, externalModifiers, miscModifier, false, false,
              LocalDate.of(3151, 1, 1));
    }

    /**
     * Performs a quick and simple skill check for a person based on the specified skill name.
     *
     * <p>This method evaluates whether the given {@link Person} successfully performs a specified skill
     * by creating a {@link SkillCheckUtility} instance to handle the calculations. The skill check's success or failure
     * is determined based on the person's skill level, the provided modifiers (if any), and any campaign-specific
     * rules.</p>
     *
     * <p><b>Usage:</b> This method is designed for common use cases and provides a streamlined approach
     * to skill checks. For cases that require greater customization, such as support for edge re-rolls or detailed
     * success metrics, use the {@link SkillCheckUtility} constructor instead.</p>
     *
     * @param person            the {@link Person} performing the skill check
     * @param skillName         the name of the skill to be checked, corresponding to a {@link SkillType}
     * @param externalModifiers an optional list of {@link TargetRollModifier}s to apply additional adjustments to the
     *                          target number
     * @param miscModifier      a miscellaneous modifier that affects the target number:
     *                          <ul>
     *                              <li>For 'count up' skills, this value is subtracted from the target number
     *                                  (i.e., negative values are bonuses, positive values are penalties).</li>
     *                              <li>For non-'count up' skills, this value is added to the target number
     *                                  (i.e., positive values are penalties).</li>
     *                          </ul>
     * @param isUseAgingEffects if {@code true}, considers aging effects during the check
     * @param isClanCampaign    if {@code true}, applies rules specific to clan campaigns
     * @param today             the current date, used for time-dependent logic
     *
     * @return {@code true} if the skill check succeeds, {@code false} otherwise
     *
     *       <p>This method is often the preferred choice for skill checks in MekHQ due to its simplicity and
     *       effectiveness.</p>
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static boolean performQuickSkillCheck(final Person person, final String skillName,
          final @Nullable List<TargetRollModifier> externalModifiers, final int miscModifier,
          boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today) {
        SkillCheckUtility skillCheck = new SkillCheckUtility(person, skillName, externalModifiers, miscModifier,
              false, false, isUseAgingEffects, isClanCampaign, today);
        return skillCheck.isSuccess();
    }

    /**
     * Checks if the {@code person} object is {@code null} and handles the null case by auto-failing the check with
     * obviously wrong results.
     *
     * <p>If the {@code person} is {@code null}, the method logs a debug message, sets a {@code DISASTROUS} failure
     * margin, and assigns out-of-range values to the {@code targetNumber} and {@code roll} to make the issue easily
     * identifiable.</p>
     *
     * @return {@code true} if the {@code person} is {@code null}, {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.05
     */
    private boolean isPersonNull() {
        if (person == null) {
            LOGGER.debug("Null person passed into SkillCheckUtility." +
                               " Auto-failing check with bogus results so the bug stands out.");

            marginOfSuccess = getMarginValue(DISASTROUS);
            resultsText = getFormattedTextAt(RESOURCE_BUNDLE, "skillCheck.nullPerson");
            targetNumber = new TargetRoll(Integer.MAX_VALUE, "ERROR");
            roll = Integer.MIN_VALUE;
            return true;
        }

        return false;
    }

    /**
     * Generates a formatted and localized results text describing the outcome of a skill check.
     *
     * <p>This method produces a detailed summary of the skill check results, including:</p>
     * <ul>
     *   <li>The person's title, name, and gender-based pronoun</li>
     *   <li>The name of the skill being checked</li>
     *   <li>The dice roll, target number, and margin of success or failure</li>
     *   <li>A status message indicating success or failure</li>
     *   <li>Use of edge (if applicable)</li>
     * </ul>
     *
     * <p>The results text is color-coded using custom span tags based on the margin of success:
     * <ul>
     *   <li><b>Neutral Margin:</b> Displayed using a warning color (e.g., yellow).</li>
     *   <li><b>Failure:</b> Displayed using a negative color (e.g., red).</li>
     *   <li><b>Success:</b> Displayed using a positive color (e.g., green).</li>
     * </ul>
     * </p>
     *
     * <p>If edge was used to reroll the skill check, the results will include an additional note with
     * information about the reroll. If the caller requests it, margin of success details can also be
     * appended to the results text.</p>
     *
     * <p>If the skill name is {@code null}, the method returns a localized error message indicating that the
     * skill name could not be resolved and that an error occurred during the results generation process.</p>
     *
     * @param includeMarginsOfSuccessText whether to include detailed margin of success information in the results text
     *
     * @return a localized and formatted {@link String} representing the outcomes of the skill check:
     *       <ul>
     *         <li>If successful, the string provides details of the roll, skill, and margin of success.</li>
     *         <li>If edge was used, additional information about the reroll is included.</li>
     *         <li>If the skill name is {@code null}, an error message is returned instead.</li>
     *       </ul>
     *
     * @author Illiani
     * @since 0.50.05
     */
    private String generateResultsText(boolean includeMarginsOfSuccessText) {
        if (skillName == null) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "skillCheck.nullSkillName");
        }

        String fullTitle = person.getHyperlinkedFullTitle();
        String firstName = person.getFirstName();
        String genderedReferenced = HIS_HER_THEIR.getDescriptor(person.getGender());

        String colorOpen;
        int neutralMarginValue = getMarginValue(BARELY_MADE_IT);
        if (marginOfSuccess == neutralMarginValue) {
            colorOpen = spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());
        } else if (marginOfSuccess < neutralMarginValue) {
            colorOpen = spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());
        } else {
            colorOpen = spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor());
        }
        String status = getFormattedTextAt(RESOURCE_BUNDLE,
              "skillCheck.results." + (isSuccess() ? "success" : "failure"));
        String mainMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              "skillCheck.results",
              fullTitle,
              colorOpen,
              status,
              CLOSING_SPAN_TAG,
              genderedReferenced,
              skillName,
              roll,
              targetNumber.getValue());

        String edgeUseText = !usedEdge ? "" : getFormattedTextAt(RESOURCE_BUNDLE, "skillCheck.rerolled", firstName);

        if (!edgeUseText.isBlank()) {
            mainMessage = mainMessage + "<p>" + edgeUseText + "</p>";
        }

        if (includeMarginsOfSuccessText) {
            MarginOfSuccess marginOfSuccessObject = getMarginOfSuccessObjectFromMarginValue(marginOfSuccess);
            String marginOfSuccessText = getMarginOfSuccessString(marginOfSuccessObject);
            return mainMessage + "<p>" + marginOfSuccessText + "</p>";
        } else {
            return mainMessage;
        }
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
     * @author Illiani
     * @since 0.50.05
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
     * @author Illiani
     * @since 0.50.05
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
     * @author Illiani
     * @since 0.50.05
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
     * @author Illiani
     * @since 0.50.05
     */
    public TargetRoll getTargetNumber() {
        return targetNumber;
    }

    /**
     * Gets the roll result for the skill check.
     *
     * <p>The roll is the result of the dice roll used to determine whether the skill check succeeded or failed.</p>
     *
     * @return the roll result for the skill check
     *
     * @author Illiani
     * @since 0.50.05
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
     * @author Illiani
     * @since 0.50.05
     */
    public boolean isUsedEdge() {
        return usedEdge;
    }

    /**
     * Use {@link #determineTargetNumber(Person, SkillType, int, boolean, boolean, LocalDate)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static TargetRoll determineTargetNumber(Person person, SkillType skillType, int miscModifier) {
        return determineTargetNumber(person, skillType, miscModifier, false, false, LocalDate.of(3151, 1, 1));
    }

    /**
     * Determines the target number for a skill check based on the person's attributes, skill type, and whether they are
     * trained in the skill.
     *
     * <p>If the person is untrained, the target number is based on constants for untrained rolls and the number of
     * linked attributes. Otherwise, it is based on the final skill value and attribute modifiers.</p>
     *
     * @param person            the {@link Person} performing the skill check
     * @param skillType         the associated {@link SkillType} for the {@link Skill} being used.
     * @param miscModifier      any special modifiers, as an {@link Integer}. These values are subtracted from the
     *                          target number, if the associated skill is classified as 'count up', otherwise they are
     *                          added to the target number. This means negative values are bonuses, positive values are
     *                          penalties.
     * @param isUseAgingEffects if {@code true}, considers aging effects during the check
     * @param isClanCampaign    if {@code true}, applies rules specific to clan campaigns
     * @param today             the current date, used for time-dependent logic
     *
     * @return the target number for the skill check
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static TargetRoll determineTargetNumber(Person person, SkillType skillType, int miscModifier,
          boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today) {
        final String skillName = skillType.getName();
        final Attributes characterAttributes = person.getATOWAttributes();

        boolean isUntrained = !person.hasSkill(skillName);
        int linkedAttributeCount = skillType.getLinkedAttributeCount();

        TargetRoll targetNumber = new TargetRoll();

        if (isUntrained) {
            if (linkedAttributeCount > 1) {
                targetNumber.addModifier(UNTRAINED_TARGET_NUMBER_TWO_LINKED_ATTRIBUTES,
                      getFormattedTextAt(RESOURCE_BUNDLE, "skillCheck.untrained.twoLinkedAttributes"));
            } else {
                targetNumber.addModifier(UNTRAINED_TARGET_NUMBER_ONE_LINKED_ATTRIBUTE,
                      getFormattedTextAt(RESOURCE_BUNDLE, "skillCheck.untrained.oneLinkedAttribute"));
            }

            getTotalAttributeScoreForSkill(targetNumber, characterAttributes, skillType);

            targetNumber.addModifier(UNTRAINED_SKILL_MODIFIER,
                  getFormattedTextAt(RESOURCE_BUNDLE, "skillCheck.untrained.skill"));
        } else {
            Skill skill = person.getSkill(skillName);
            int skillValue = skill.getFinalSkillValue(person.getOptions(),
                  person.getATOWAttributes(),
                  person.getAdjustedReputation(isUseAgingEffects, isClanCampaign, today, person.getRankNumeric()));
            targetNumber.addModifier(skillValue, skillName);
        }

        if (skillType.isCountUp()) {
            targetNumber.addModifier(-miscModifier, getFormattedTextAt(RESOURCE_BUNDLE, "skillCheck.miscModifier"));
        } else {
            targetNumber.addModifier(miscModifier, getFormattedTextAt(RESOURCE_BUNDLE, "skillCheck.miscModifier"));
        }

        return targetNumber;
    }

    /**
     * Performs a skill check for a specified person, determining the outcome based on dice rolls and optionally
     * allowing the use of edge points for a re-roll upon failure.
     *
     * <p>This method begins by rolling two six-sided dice (2d6) and comparing the result against
     * a pre-calculated target number. If the initial roll meets or exceeds the target number, the skill check succeeds,
     * and the results are calculated and stored. If the initial roll fails, and edge use is enabled and available, one
     * edge point is consumed, and a re-roll is performed. The method concludes by storing the final skill check
     * results, including the margin of success and optional descriptive text, for later use.</p>
     *
     * <p>When edge is used, the method records this information and updates the results accordingly
     * to reflect the additional roll.</p>
     *
     * @param useEdge                     whether to allow the person to use an edge point to re-roll if the initial
     *                                    roll fails. Edge use is conditional on the person's current edge
     *                                    availability.
     * @param includeMarginsOfSuccessText whether to include detailed information about the margin of success in the
     *                                    final results text
     *
     * @author Illiani
     * @since 0.50.05
     */
    void performCheck(boolean useEdge, boolean includeMarginsOfSuccessText) {
        roll = d6(2);
        if (performInitialRoll(useEdge, includeMarginsOfSuccessText)) {
            return;
        }

        roll = d6(2);
        rollWithEdge(includeMarginsOfSuccessText);
    }

    /**
     * Handles the initial dice roll in the skill check and determines whether the check is resolved or if further
     * action (re-roll with edge) is required.
     *
     * <p>This method evaluates the outcome of the initial roll by comparing it against the target number.
     * If the roll meets or exceeds the target number, the skill check is deemed successful, and the results are
     * finalized. If the roll fails, the method decides whether edge can or should be used to perform a re-roll. Edge
     * use is allowed only if the {@code useEdge} parameter is {@code true} and there are available edge points for the
     * person.</p>
     *
     * <p>The method stores key results from the initial roll, including:</p>
     * <ul>
     *   <li>The margin of success or failure, calculated based on the target number and roll</li>
     *   <li>A descriptive results text, optionally including margin details if requested</li>
     * </ul>
     *
     * <p>If the roll does not succeed and edge use is feasible, the method signals the need for a re-roll,
     * otherwise finalizes the results based on the initial roll.</p>
     *
     * @param useEdge                     whether to allow using edge points for a re-roll if the initial roll fails
     * @param includeMarginsOfSuccessText whether to include detailed margin of success information in the results text
     *
     * @return {@code true} if the skill check is resolved using the initial roll (success or no edge re-roll possible);
     *       {@code false} if a re-roll using edge is required
     *
     * @author Illiani
     * @since 0.50.05
     */
    boolean performInitialRoll(boolean useEdge, boolean includeMarginsOfSuccessText) {
        int availableEdge = person.getCurrentEdge();
        int targetNumberValue = targetNumber.getValue();

        if (roll >= targetNumberValue || !useEdge || availableEdge < 1) {
            int difference = isCountUp ? targetNumberValue - roll : roll - targetNumberValue;

            marginOfSuccess = MarginOfSuccess.getMarginOfSuccess(difference);
            resultsText = generateResultsText(includeMarginsOfSuccessText);
            LOGGER.info(resultsText);
            return true;
        }
        return false;
    }

    /**
     * Executes the re-roll logic for a skill check when edge is used, updating the person's edge points and
     * recalculating the outcome based on the new roll.
     *
     * <p>This method is invoked only after the failure of the initial roll, provided edge usage is
     * allowed and the person has at least one edge point available. When called, it decrements the person's edge points
     * by one, triggers an event to update the game state reflecting this change, and marks that edge was used for this
     * skill check. The results of the skill check are then recalculated based on the new roll, including the margin of
     * success and the corresponding results text.</p>
     *
     * <p>The results text can optionally include detailed information about the margin of success,
     * depending on the value of the {@code includeMarginsOfSuccessText} parameter.</p>
     *
     * @param includeMarginsOfSuccessText whether to include detailed margin of success information in the results text
     *
     * @author Illiani
     * @since 0.50.05
     */
    private void rollWithEdge(boolean includeMarginsOfSuccessText) {
        person.changeCurrentEdge(-1);
        MekHQ.triggerEvent(new PersonChangedEvent(person));
        usedEdge = true;

        int targetNumberValue = targetNumber.getValue();

        int difference = isCountUp ? targetNumberValue - roll : roll - targetNumberValue;
        marginOfSuccess = MarginOfSuccess.getMarginOfSuccess(difference);
        resultsText = generateResultsText(includeMarginsOfSuccessText);
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
