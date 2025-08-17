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
import static mekhq.campaign.personnel.enums.GenderDescriptors.HIS_HER_THEIR;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.BARELY_MADE_IT;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.DISASTROUS;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginOfSuccessObjectFromMarginValue;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginOfSuccessString;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginValue;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.List;

import megamek.common.rolls.TargetRoll;
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
 * Utility class for executing attribute checks.
 *
 * <p>The {@link AttributeCheckUtility} class encapsulates the logic needed to perform attribute checks, including
 * calculating target numbers based on supplied attributes, applying external and miscellaneous modifiers, optionally
 * handling re-rolls using the Edge mechanic, and generating detailed results including margin of success or failure. It
 * supports both single-attribute and double-attribute checks and can generate localized, formatted result text suitable
 * for UI feedback or logs.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class AttributeCheckUtility {
    private static final MMLogger LOGGER = MMLogger.create(AttributeCheckUtility.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AttributeCheckUtility";

    /**
     * The target number for an attribute check with one attribute.
     */
    protected static final int TARGET_NUMBER_ONE_LINKED_ATTRIBUTE = 12; // ATOW pg 43

    /**
     * The target number for an attribute check with two attributes.
     */
    protected static final int TARGET_NUMBER_TWO_LINKED_ATTRIBUTES = 18; // ATOW pg 43

    private final Person person;
    private final SkillAttribute firstSkillAttribute;
    private final SkillAttribute secondSkillAttribute;
    private int marginOfSuccess;
    private String resultsText;
    private TargetRoll targetNumber;
    boolean isCountUp;
    private int roll;
    private boolean usedEdge;


    /**
     * Executes an attribute check for the specified person and attribute type(s).
     *
     * <p>This constructor creates a {@link AttributeCheckUtility} instance which calculates the target number for
     * the attribute check and performs the roll, determining the outcome based on factors such as the person's
     * attribute levels, external modifiers, miscellaneous modifiers, and whether edge is used.</p>
     *
     * <p>External modifiers can optionally influence the target number, while miscellaneous modifiers alter the
     * target based on whether the attribute is classified as 'count up' or not. Using edge allows the person to attempt
     * a re-roll if the initial roll fails. Additionally, the constructor can include margins of success text as part of
     * the results, if desired.</p>
     *
     * <p><b>Usage:</b> This constructor offers detailed control over the attribute check process. For simpler
     * use-cases, the {@link #performQuickAttributeCheck(Person, SkillAttribute, SkillAttribute, List, int)} provides a
     * more streamlined approach.</p>
     *
     * @param person                      the {@link Person} performing the attribute check
     * @param firstSkillAttribute         the primary attribute to be used in the check.
     * @param secondSkillAttribute        the secondary optional attribute to be used in the check. Can be null.
     * @param externalModifiers           an optional list of {@link TargetRollModifier}s that affect the target number
     * @param miscModifier                a miscellaneous modifier that affects the target number:
     *                                    <ul>
     *                                        <li>For 'count up' attributes, this value is subtracted from
     *                                            the target number (i.e., negative values are bonuses,
     *                                            positive values are penalties).</li>
     *                                        <li>For non-'count up' attributes, this value is added to the
     *                                            target number (i.e., positive values are penalties).</li>
     *                                    </ul>
     * @param useEdge                     whether the person should use edge to re-roll if the initial attempt fails
     * @param includeMarginsOfSuccessText whether to include detailed margins of success information in the results
     *
     * @author Illiani
     * @since 0.50.07
     */
    public AttributeCheckUtility(final Person person, final SkillAttribute firstSkillAttribute,
          final @Nullable SkillAttribute secondSkillAttribute, @Nullable List<TargetRollModifier> externalModifiers,
          final int miscModifier, final boolean useEdge, final boolean includeMarginsOfSuccessText) {
        this.person = person;
        this.firstSkillAttribute = firstSkillAttribute;
        this.secondSkillAttribute = secondSkillAttribute;

        if (isPersonNull()) {
            return;
        }

        targetNumber = determineTargetNumber(person, firstSkillAttribute, secondSkillAttribute, miscModifier);

        if (externalModifiers != null) {
            for (TargetRollModifier modifier : externalModifiers) {
                targetNumber.addModifier(modifier);
            }
        }

        performCheck(useEdge, includeMarginsOfSuccessText);
    }

    /**
     * Performs a quick and simple attribute check for a person based on the specified attributes.
     *
     * <p>This method evaluates whether the given {@link Person} successfully performs a specified attribute check by
     * creating a {@link AttributeCheckUtility} instance to handle the calculations. The attribute check's success or
     * failure is determined based on the person's attribute levels, the provided modifiers (if any), and any
     * campaign-specific rules.</p>
     *
     * <p><b>Usage:</b> This method is designed for common use cases and provides a streamlined approach to attribute
     * checks. For cases that require greater customization, such as support for edge re-rolls or detailed success
     * metrics, use the {@link AttributeCheckUtility} constructor instead.</p>
     *
     * <p>This method is often the preferred choice for attribute checks in MekHQ due to its simplicity.</p>
     *
     * @param person               the {@link Person} performing the Attribute check
     * @param firstSkillAttribute  the primary attribute to be used in the check.
     * @param secondSkillAttribute the secondary optional attribute to be used in the check. Can be null.
     * @param externalModifiers    an optional list of {@link TargetRollModifier}s to apply additional adjustments to
     *                             the target number
     * @param miscModifier         a miscellaneous modifier that affects the target number:
     *                             <ul>
     *                                 <li>For 'count up' Attributes, this value is subtracted from the target number
     *                                     (i.e., negative values are bonuses, positive values are penalties).</li>
     *                                 <li>For non-'count up' Attributes, this value is added to the target number
     *                                     (i.e., positive values are penalties).</li>
     *                             </ul>
     *
     * @return {@code true} if the Attribute check succeeds, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static boolean performQuickAttributeCheck(final Person person, final SkillAttribute firstSkillAttribute,
          final @Nullable SkillAttribute secondSkillAttribute,
          final @Nullable List<TargetRollModifier> externalModifiers,
          final int miscModifier) {
        AttributeCheckUtility AttributeCheck = new AttributeCheckUtility(person,
              firstSkillAttribute,
              secondSkillAttribute,
              externalModifiers,
              miscModifier,
              false,
              false);
        return AttributeCheck.isSuccess();
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
     * @since 0.50.07
     */
    private boolean isPersonNull() {
        if (person == null) {
            LOGGER.debug("Null person passed into AttributeCheckUtility." +
                               " Auto-failing check with bogus results so the bug stands out.");

            marginOfSuccess = getMarginValue(DISASTROUS);
            resultsText = getFormattedTextAt(RESOURCE_BUNDLE, "AttributeCheck.nullPerson");
            targetNumber = new TargetRoll(Integer.MAX_VALUE, "ERROR");
            roll = Integer.MIN_VALUE;
            return true;
        }

        return false;
    }

    /**
     * Generates a formatted and localized results text describing the outcome of an Attribute check.
     *
     * <p>This method produces a detailed summary of the Attribute check results, including:</p>
     * <ul>
     *   <li>The person's title, name, and gender-based pronoun</li>
     *   <li>The name of the Attribute being checked</li>
     *   <li>The dice roll, target number, and margin of success or failure</li>
     *   <li>A status message indicating success or failure</li>
     *   <li>Use of edge (if applicable)</li>
     * </ul>
     *
     * <p>The result text is color-coded using custom span tags based on the margin of success:
     * <ul>
     *   <li><b>Neutral Margin:</b> Displayed using a warning color (e.g., yellow).</li>
     *   <li><b>Failure:</b> Displayed using a negative color (e.g., red).</li>
     *   <li><b>Success:</b> Displayed using a positive color (e.g., green).</li>
     * </ul>
     * </p>
     *
     * <p>If edge was used to reroll the Attribute check, the results will include an additional note with
     * information about the reroll. If the caller requests it, margin of success details can also be appended to the
     * result text.</p>
     *
     * <p>If the first Attribute name is {@code null}, the method returns a localized error message indicating that the
     * Attribute name could not be resolved and that an error occurred during the result generation process.</p>
     *
     * @param includeMarginsOfSuccessText whether to include detailed margin of success information in the result text
     *
     * @return a localized and formatted {@link String} representing the outcomes of the Attribute check:
     *       <ul>
     *         <li>If successful, the string provides details of the roll, Attribute, and margin of success.</li>
     *         <li>If edge was used, additional information about the reroll is included.</li>
     *         <li>If the first Attribute name is {@code null}, an error message is returned instead.</li>
     *       </ul>
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String generateResultsText(boolean includeMarginsOfSuccessText) {
        if (firstSkillAttribute == null) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "AttributeCheck.nullAttributeName");
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
              "AttributeCheck.results." + (isSuccess() ? "success" : "failure"));
        String label = getAttributeCheckLabel();
        String mainMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              "AttributeCheck.results",
              fullTitle,
              colorOpen,
              status,
              CLOSING_SPAN_TAG,
              genderedReferenced,
              label,
              roll,
              targetNumber.getValue());

        String edgeUseText = !usedEdge ? "" : getFormattedTextAt(RESOURCE_BUNDLE, "AttributeCheck.rerolled", firstName);

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
     * Gets the calculated margin of success for this attribute check.
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
     * @since 0.50.07
     */
    public int getMarginOfSuccess() {
        return marginOfSuccess;
    }

    /**
     * Determines whether the Attribute check was successful.
     *
     * <p>An attribute check is considered successful if the calculated margin of success is greater than or equal to
     * the margin value of {@link MarginOfSuccess#BARELY_MADE_IT}.</p>
     *
     * <p><b>Usage:</b> You want to call this method whenever you only care whether the check was passed or failed.
     * If you want to know how well the character did use {@link #getMarginOfSuccess()} instead.</p>
     *
     * @return {@code true} if the attribute check succeeded, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isSuccess() {
        return marginOfSuccess >= getMarginValue(BARELY_MADE_IT);
    }

    /**
     * Gets the result text for the margin of success.
     *
     * <p>This is a descriptive string representing the outcome of the attribute check, based on the calculated
     * margin of success.</p>
     *
     * @return the results text for the attribute check
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getResultsText() {
        return resultsText;
    }

    /**
     * Gets the target number for the attribute check.
     *
     * <p>The target number represents the value that the rolled number must meet or exceed for the attribute check to
     * succeed.</p>
     *
     * @return the target number for the attribute check
     *
     * @author Illiani
     * @since 0.50.07
     */
    public TargetRoll getTargetNumber() {
        return targetNumber;
    }

    /**
     * Gets the roll result for the attribute check.
     *
     * <p>The roll is the result of the dice roll used to determine whether the attribute check succeeded or
     * failed.</p>
     *
     * @return the roll result for the attribute check
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getRoll() {
        return roll;
    }

    /**
     * Checks whether edge was used during the attribute check.
     *
     * <p>Edge provides the opportunity to re-roll if the initial attribute check fails, allowing a chance to improve
     * the outcome.</p>
     *
     * @return {@code true} if edge was used during the attribute check, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isUsedEdge() {
        return usedEdge;
    }

    /**
     * Determines the target number for a roll based on the given person's attributes, the requested attributes, and a
     * miscellaneous modifier.
     *
     * @param person               the person whose attributes will be used to calculate the target number
     * @param firstSkillAttribute  the primary skill attribute to be used for calculations can be null
     * @param secondSkillAttribute the secondary skill attribute to be used for calculations
     * @param miscModifier         an additional modifier to be applied to the target number
     *
     * @return a {@link TargetRoll} object containing the calculated target number
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static TargetRoll determineTargetNumber(Person person, @Nullable SkillAttribute firstSkillAttribute,
          SkillAttribute secondSkillAttribute, int miscModifier) {
        final Attributes characterAttributes = person.getATOWAttributes();

        TargetRoll targetNumber = new TargetRoll();

        getBaseTargetNumber(targetNumber, secondSkillAttribute != null);
        getAttributeModifiers(firstSkillAttribute, secondSkillAttribute, characterAttributes, targetNumber);

        targetNumber.addModifier(miscModifier, getFormattedTextAt(RESOURCE_BUNDLE, "AttributeCheck.miscModifier"));

        return targetNumber;
    }

    /**
     * Calculates and applies attribute modifiers to the target roll based on the provided skill attributes and
     * character attributes. If the second skill attribute is not null, it also calculates and applies its modifier.
     *
     * @param firstSkillAttribute  the first skill attribute used for modifier calculation
     * @param secondSkillAttribute the second skill attribute used for modifier calculation; can be null
     * @param characterAttributes  the set of character attributes that provide the modifiers
     * @param targetNumber         the target roll object to which the calculated modifiers will be added
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void getAttributeModifiers(SkillAttribute firstSkillAttribute, SkillAttribute secondSkillAttribute,
          Attributes characterAttributes, TargetRoll targetNumber) {
        int firstAttributeModifier = -characterAttributes.getAttributeScore(firstSkillAttribute);
        targetNumber.addModifier(firstAttributeModifier, firstSkillAttribute.getLabel());

        if (secondSkillAttribute != null) {
            int secondAttributeModifier = -characterAttributes.getAttributeScore(secondSkillAttribute);
            targetNumber.addModifier(secondAttributeModifier, secondSkillAttribute.getLabel());
        }
    }

    /**
     * Modifies the base target number based on whether a single or double attribute check is performed.
     *
     * @param targetNumber           the {@link TargetRoll} object to which the modifier will be added
     * @param isDoubleAttributeCheck a boolean indicating whether the check involves two linked attributes (true) or one
     *                               linked attribute (false)
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void getBaseTargetNumber(TargetRoll targetNumber, boolean isDoubleAttributeCheck) {
        if (isDoubleAttributeCheck) {
            targetNumber.addModifier(TARGET_NUMBER_TWO_LINKED_ATTRIBUTES,
                  getFormattedTextAt(RESOURCE_BUNDLE, "AttributeCheck.twoLinkedAttributes"));
        } else {
            targetNumber.addModifier(TARGET_NUMBER_ONE_LINKED_ATTRIBUTE,
                  getFormattedTextAt(RESOURCE_BUNDLE, "AttributeCheck.oneLinkedAttribute"));
        }
    }

    /**
     * Performs an attribute check for a specified person, determining the outcome based on dice rolls and optionally
     * allowing the use of edge points for a re-roll upon failure.
     *
     * <p>This method begins by rolling two six-sided dice (2d6) and comparing the result against a pre-calculated
     * target number. If the initial roll meets or exceeds the target number, the Attribute check succeeds, and the
     * results are calculated and stored. If the initial roll fails, and edge use is enabled and available, one edge
     * point is consumed, and a re-roll is performed.</p>
     *
     * <p>The method concludes by storing the final Attribute check results, including the margin of success and
     * optional descriptive text, for later use.</p>
     *
     * <p>When edge is used, the method records this information and updates the results accordingly to reflect the
     * additional roll.</p>
     *
     * @param useEdge                     whether to allow the person to use an edge point to re-roll if the initial
     *                                    roll fails. Edge use is conditional on the person's current edge
     *                                    availability.
     * @param includeMarginsOfSuccessText whether to include detailed information about the margin of success in the
     *                                    final results text
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void performCheck(boolean useEdge, boolean includeMarginsOfSuccessText) {
        roll = d6(2);
        if (performInitialRoll(useEdge, includeMarginsOfSuccessText)) {
            return;
        }

        roll = d6(2);
        rollWithEdge(includeMarginsOfSuccessText);
    }

    /**
     * Handles the initial dice roll in the Attribute check and determines whether the check is resolved or if further
     * action (re-roll with edge) is required.
     *
     * <p>This method evaluates the outcome of the initial roll by comparing it against the target number. If the
     * roll meets or exceeds the target number, the Attribute check is deemed successful, and the results are finalized.
     * If the roll fails, the method decides whether Edge can or should be used to perform a re-roll. Edge use is
     * allowed only if the {@code useEdge} parameter is {@code true} and there are available edge points for the
     * person.</p>
     *
     * <p>The method stores key results from the initial roll, including:</p>
     * <ul>
     *   <li>The margin of success or failure, calculated based on the target number and roll</li>
     *   <li>A descriptive results text, optionally including margin details if requested</li>
     * </ul>
     *
     * <p>If the roll does not succeed and edge use is feasible, the method signals the need for a re-roll, otherwise
     * finalizes the results based on the initial roll.</p>
     *
     * @param useEdge                     whether to allow using edge points for a re-roll if the initial roll fails
     * @param includeMarginsOfSuccessText whether to include detailed margin of success information in the result text
     *
     * @return {@code true} if the Attribute check is resolved using the initial roll (success or no edge re-roll
     *       possible); {@code false} if a re-roll using edge is required
     *
     * @author Illiani
     * @since 0.50.07
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

    private String getAttributeCheckLabel() {
        String label = firstSkillAttribute.getLabel();
        if (secondSkillAttribute != null) {
            label = label + "-" + secondSkillAttribute.getLabel();
        }

        return label;
    }

    /**
     * Executes the re-roll logic for an Attribute check when edge is used, updating the person's edge points and
     * recalculating the outcome based on the new roll.
     *
     * <p>This method is invoked only after the failure of the initial roll, provided edge usage is allowed and the
     * person has at least one edge point available. When called, it decrements the person's edge points by one,
     * triggers an event to update the game state reflecting this change, and marks that edge was used for this
     * Attribute check. The results of the Attribute check are then recalculated based on the new roll, including the
     * margin of success and the corresponding results text.</p>
     *
     * <p>The result text can optionally include detailed information about the margin of success, depending on the
     * value of the {@code includeMarginsOfSuccessText} parameter.</p>
     *
     * @param includeMarginsOfSuccessText whether to include detailed margin of success information in the result text
     *
     * @author Illiani
     * @since 0.50.07
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
}
