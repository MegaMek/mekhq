/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.personnel.enums.GenderDescriptors.HIS_HER_THEIR;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.BARELY_MADE_IT;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginOfSuccessObjectFromMarginValue;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.List;

import megamek.common.TargetRollModifier;
import megamek.common.annotations.Nullable;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;
import mekhq.utilities.ReportingUtilities;

/**
 * Base abstract class for configuring character skill, attribute, and other action checks.
 *
 * <p>This class utilizes a builder pattern to allow the caller to attach external modifiers
 * and miscellaneous adjustments before resolving the check via {@link #resolve(boolean, String, boolean)}.
 * Subclasses must implement the abstract methods to define the specific mechanics of the action being checked.</p>
 *
 * @param <T> the concrete subclass type, used to enable method chaining
 *
 * @author Hokk
 * @since 0.51.01
 */
public abstract class ActionCheck<T extends ActionCheck<T>> {

    private static final MMLogger LOGGER = MMLogger.create(ActionCheck.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SkillCheckUtility";

    protected final Person person;
    protected final TargetRoll targetNumber;

    /**
     * Initializes a new action check for the specified person and target number.
     *
     * @param person       the {@link Person} performing the action
     * @param targetNumber the base {@link TargetRoll} required to succeed
     */
    protected ActionCheck(Person person, TargetRoll targetNumber) {
        this.person = person;
        this.targetNumber = targetNumber;
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
     * Returns the concrete instance of this class for method chaining.
     *
     * @return the current instance of {@code T}
     */
    abstract protected T getThis();

    /**
     * Determines if the action uses a "count up" resolution mechanic.
     *
     * <p>In count up mechanics, miscellaneous bonuses are subtracted from the target number, while
     * penalties are added.</p>
     *
     * @return {@code true} if the action counts up, {@code false} otherwise
     */
    abstract protected boolean isCountUp();

    /**
     * Determines if the character has a natural aptitude for this action.
     *
     * <p>Natural aptitude generally allows the character to roll an extra die and keep the highest two results.</p>
     *
     * @return {@code true} if the character has a natural aptitude, {@code false} otherwise
     */
    abstract protected boolean hasNaturalAptitude();

    /**
     * Retrieves the localized name of the action or skill being checked.
     *
     * @return the name of the action
     */
    abstract protected String getActionName();

    /**
     * Applies external modifiers to the action check's target number.
     *
     * <p>External modifiers can optionally influence the target number. Using edge allows the person to attempt a
     * re-roll if the initial roll fails. Additionally, the constructor can include margins of success text as
     * part of the results, if desired.</p>
     *
     * @param modifiers a list of {@link TargetRollModifier}s that affect the target number
     * @return updated action check
     */
    public T withExternalModifiers(List<TargetRollModifier> modifiers) {
        for (TargetRollModifier modifier : modifiers) {
            targetNumber.addModifier(modifier);
        }
        return getThis();
    }

    /**
     * Applies a miscellaneous numerical modifier to the action check's target number.
     *
     * @param miscModifier any special modifiers, as an {@link Integer}. These values are subtracted from the
     *                     target number, if the associated skill is classified as 'count up', otherwise they are
     *                     added to the target number. This means negative values are bonuses, positive values are
     *                     penalties.
     * @return updated action check
     */
    public T withMiscModifier(int miscModifier) {
        int finalModifier = isCountUp() ? -miscModifier : miscModifier;
        targetNumber.addModifier(finalModifier, getFormattedTextAt(RESOURCE_BUNDLE, "skillCheck.miscModifier"));
        return getThis();
    }


    /**
     * Executes action check for the specified person.
     *
     * <p>External modifiers can optionally influence the target number, while miscellaneous modifiers
     * alter the target based on whether the skill is classified as 'count up' or not. Using edge allows the person to
     * attempt a re-roll if the initial roll fails. Additionally, the constructor can include margins of success text as
     * part of the results, if desired.</p>
     *
     * <p><b>Usage:</b> This constructor offers detailed control over the skill check process.
     * </p>
     *
     * @param useEdge                     whether the person should use edge to re-roll if the initial attempt fails
     * @param reason                      the reason for the check; can be {@code null}
     * @param includeMarginsOfSuccessText whether to include detailed margins of success information in the results
     */
    public ActionCheckResult resolve(boolean useEdge, @Nullable String reason,
          boolean includeMarginsOfSuccessText) {

        int roll = SkillCheckUtility.getRoll(hasNaturalAptitude());
        boolean usedEdge = false;

        boolean failed = roll < targetNumber.getValue();
        boolean canSucceed = !targetNumber.cannotSucceed() && targetNumber.getValue() <= 12;
        boolean canSpendEdge = useEdge && person.getCurrentEdge() > 0;

        if (failed && canSucceed && canSpendEdge) {
            // reroll using edge
            roll = SkillCheckUtility.getRoll(hasNaturalAptitude());
            usedEdge = true;

            person.changeCurrentEdge(-1);
            MekHQ.triggerEvent(new PersonChangedEvent(person));
        }

        int difference = targetNumber.getValue() - roll;
        int marginOfSuccess = MarginOfSuccess.getMarginOfSuccess(isCountUp() ? difference : -difference);
        String resultsText = generateResultsText(roll, marginOfSuccess, usedEdge, person,
              includeMarginsOfSuccessText, hasNaturalAptitude(), reason);

        LOGGER.info(resultsText);

        return new ActionCheckResult(roll, marginOfSuccess, usedEdge, resultsText);
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
    private String generateResultsText(int roll, int marginOfSuccess, boolean usedEdge,
          Person person, boolean includeMarginsOfSuccessText, boolean hasNaturalAptitude, @Nullable String reason) {
        String fullTitle = person.getHyperlinkedFullTitle();
        String genderedReferenced = HIS_HER_THEIR.getDescriptor(person.getGender());

        String colorOpen;
        int neutralMarginValue = BARELY_MADE_IT.getValue();
        if (marginOfSuccess == neutralMarginValue) {
            colorOpen = spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());
        } else if (marginOfSuccess < neutralMarginValue) {
            colorOpen = spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());
        } else {
            colorOpen = spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor());
        }

        String status = getTextAt(RESOURCE_BUNDLE,
              ActionCheckResult.isSuccess(marginOfSuccess) ? "skillCheck.results.success" : "skillCheck.results.failure");

        StringBuilder resultsText = new StringBuilder(getFormattedTextAt(RESOURCE_BUNDLE,
              "skillCheck.results",
              reason == null ? "" : "<b>" + reason + ":</b> ",
              fullTitle,
              colorOpen,
              status,
              CLOSING_SPAN_TAG,
              genderedReferenced,
              getActionName(),
              roll,
              targetNumber.getValue()));

        if (hasNaturalAptitude) {
            resultsText.append(" ").append(getTextAt(RESOURCE_BUNDLE, "skillCheck.naturalAptitude"));
        }

        if (usedEdge) {
            resultsText.append(" ").append(getTextAt(RESOURCE_BUNDLE, "skillCheck.rerolled"));
        }

        if (includeMarginsOfSuccessText) {
            MarginOfSuccess marginOfSuccessObject = getMarginOfSuccessObjectFromMarginValue(marginOfSuccess);
            String marginOfSuccessText = colorOpen + marginOfSuccessObject.getLabel() + CLOSING_SPAN_TAG;
            resultsText.append(" ").append(marginOfSuccessText);
        }

        return resultsText.toString();
    }

}
