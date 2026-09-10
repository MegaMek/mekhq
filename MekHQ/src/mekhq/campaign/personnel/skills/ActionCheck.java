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
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginOfSuccessObject;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;
import java.util.function.Predicate;

import megamek.common.TargetRollModifier;
import megamek.common.annotations.Nullable;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.ActionCheckRoll.RollType;

/**
 * Base abstract class for configuring character skill, attribute, and other action checks.
 *
 * <p>This class utilizes a builder pattern to allow the caller to attach external modifiers
 * and miscellaneous adjustments before resolving the check via {@link #resolve(boolean, String)}.
 * Subclasses must implement the abstract methods to define the specific mechanics of the action being checked.</p>
 *
 * @param <T> the concrete subclass type, used to enable method chaining
 *
 * @author Hokk
 * @since 0.51.01
 */
public abstract class ActionCheck<T extends ActionCheck<T>> {

    private static final MMLogger LOGGER = MMLogger.create(ActionCheck.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ActionCheck";
    private static final int MARGIN_OF_SUCCESS_MAX = 10;
    private static final int MARGIN_OF_SUCCESS_MIN = -10;

    protected final Person person;
    protected final TargetRoll targetNumber;

    /**
     * Optional override for the roll type. When {@code null}, the roll type is derived from natural aptitude
     * ({@link RollType#ADVANTAGE} when the character has aptitude, otherwise {@link RollType#NORMAL}). Callers that
     * need to force a specific roll type - notably {@link RollType#DISADVANTAGE}, which no aptitude-derived default can
     * produce - set this via {@link #withRollType(RollType)}.
     */
    private RollType rollTypeOverride = null;

    /**
     * Whether {@link #resolve(boolean, String)} should log the standard results line. Callers that splice the outcome
     * into their own running report disable this via {@link #withoutLogging()}.
     */
    private boolean logResult = true;

    /**
     * Optional caller-supplied gate deciding whether edge should be spent, evaluated against the first roll. When set,
     * it fully replaces the default {@code failed && canSucceed} gate. See
     * {@link #withEdgeRerollCondition(Predicate)}.
     */
    private Predicate<ActionCheckRoll> edgeRerollCondition = null;

    /**
     * Whether {@link #resolve(boolean, String)} names the person in its results line. Callers that have already
     * introduced the person in their own running report suppress the repeated name via {@link #withoutSubject()}.
     */
    private boolean includeSubject = true;

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
        targetNumber.addModifier(finalModifier, getFormattedTextAt(RESOURCE_BUNDLE, "actionCheck.miscModifier"));
        return getThis();
    }

    /**
     * Forces this check to use a specific {@link RollType}, overriding the default derived from natural aptitude.
     *
     * <p>This is the only way for a caller to request {@link RollType#DISADVANTAGE} (3d6 keeping the lowest two), which
     * the aptitude-derived default - {@link RollType#ADVANTAGE} or {@link RollType#NORMAL} - can never produce. The
     * forced roll type is applied verbatim, so it wins over natural aptitude.</p>
     *
     * @param rollType the roll type to force
     *
     * @return updated action check
     */
    public T withRollType(RollType rollType) {
        this.rollTypeOverride = rollType;
        return getThis();
    }

    /**
     * Suppresses the standard results-line logging performed by {@link #resolve(boolean, String)}.
     *
     * <p>Callers that assemble their own running report and merely need the roll, edge handling, and margin from this
     * utility use this to avoid the utility logging a line that does not slot into their report.</p>
     *
     * @return updated action check
     */
    public T withoutLogging() {
        this.logResult = false;
        return getThis();
    }

    /**
     * Overrides the edge-spending gate with a caller-supplied condition evaluated against the first roll.
     *
     * <p>By default, edge is spent whenever the initial roll fails and the target number is still achievable. Some
     * callers spend edge only under narrower, rules-specific circumstances - for example, a repair only spends edge
     * when the failure would actually destroy the part, not on every failure. When a condition is supplied here it
     * fully replaces the default gate; edge is still only spent when the caller passed {@code useEdge} and the person
     * has edge remaining.</p>
     *
     * @param condition predicate that, given the first {@link ActionCheckRoll}, returns whether edge should be spent
     *
     * @return updated action check
     */
    public T withEdgeRerollCondition(Predicate<ActionCheckRoll> condition) {
        this.edgeRerollCondition = condition;
        return getThis();
    }

    /**
     * Suppresses the person's name in the results line produced by {@link #resolve(boolean, String)}.
     *
     * <p>Callers that have already named the person in their own running report use this so the name is not repeated;
     * the line still opens with the gendered pronoun, so it reads naturally after the introducing sentence.</p>
     *
     * @return updated action check
     */
    public T withoutSubject() {
        this.includeSubject = false;
        return getThis();
    }

    /**
     * Executes action check for the specified person.
     *
     * <p>Performs a roll and determines margin of success as <code>Roll - TN</code> if <code>isCountUp ==
     * false</code>, or <code>TN - Roll</code> otherwise. Margin of success is clamped to [-10; 10] range
     * (inclusively). Using edge allows the person to attempt a re-roll if the initial roll fails.</p>
     *
     * @param useEdge whether the person should use edge to re-roll if the initial attempt fails
     * @param reason  the reason for the check; can be {@code null}
     */
    public ActionCheckResult resolve(boolean useEdge, @Nullable String reason) {
        RollType rollType = (rollTypeOverride != null)
                                  ? rollTypeOverride
                                  : (hasNaturalAptitude() ? RollType.ADVANTAGE : RollType.NORMAL);

        ActionCheckRoll roll = ActionCheckRoll.perform(rollType);
        boolean usedEdge = false;
        boolean canSpendEdge = useEdge && person.getCurrentEdge() > 0;

        // A check that cannot be beaten (AUTOMATIC_FAIL, IMPOSSIBLE) never re-rolls, so edge is never wasted on it -
        // this guard applies to a caller-supplied condition as well, which may only narrow it further.
        boolean canSucceed = !targetNumber.cannotSucceed() && targetNumber.getValue() <= 12;
        final boolean shouldReroll;
        if (edgeRerollCondition != null) {
            shouldReroll = canSpendEdge && canSucceed && edgeRerollCondition.test(roll);
        } else {
            boolean failed = roll.result() < targetNumber.getValue();
            shouldReroll = failed && canSucceed && canSpendEdge;
        }

        if (shouldReroll) {
            // reroll using edge
            roll = ActionCheckRoll.perform(rollType);
            usedEdge = true;
            person.spendEdge();
        }

        long difference = (long) targetNumber.getValue() - roll.result();
        int marginOfSuccess = Math.clamp(isCountUp() ? difference : -difference,
              MARGIN_OF_SUCCESS_MIN, MARGIN_OF_SUCCESS_MAX);
        String resultsText = generateResultsText(roll.result(), marginOfSuccess, reason, rollType);

        if (logResult) {
            LOGGER.info(resultsText);
        }

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
     * @param roll            Roll result for the action check
     * @param marginOfSuccess Calculated margin of success for this action check
     * @param reason          A string describing the reason for the action check
     * @param rollType        the {@link RollType} actually used to produce the roll
     *
     * @return a localized HTML {@link String} representing the outcomes of the skill check
     *
     * @author Illiani
     * @since 0.50.05
     */
    private String generateResultsText(int roll, int marginOfSuccess, @Nullable String reason, RollType rollType) {
        String fullTitle = person.getHyperlinkedFullTitle();
        String genderedReferenced = HIS_HER_THEIR.getDescriptor(person.getGender());

        String reportKey =
              ActionCheckResult.isSuccess(marginOfSuccess) ? "actionCheckResult.success" : "actionCheckResult.failure";
        String reasonText = reason == null ? "" : "<b>" + reason + ":</b> ";
        String color = getMarginOfSuccessObject(marginOfSuccess).getColor();
        // getValueAsString() renders AUTOMATIC_SUCCESS/AUTOMATIC_FAIL/IMPOSSIBLE as words; getValue() would print their
        // raw Integer.MIN_VALUE/MAX_VALUE sentinels.
        String targetText = targetNumber.getValueAsString();

        StringBuilder resultsText = includeSubject
              ? new StringBuilder(getFormattedTextAt(RESOURCE_BUNDLE,
                    reportKey,
                    reasonText,
                    fullTitle,
                    color,
                    genderedReferenced,
                    getActionName(),
                    roll,
                    targetText))
              : new StringBuilder(getFormattedTextAt(RESOURCE_BUNDLE,
                    reportKey + "NoSubject",
                    reasonText,
                    color,
                    genderedReferenced,
                    getActionName(),
                    roll,
                    targetText));

        if (rollType == RollType.ADVANTAGE) {
            resultsText.append(" ").append(getTextAt(RESOURCE_BUNDLE, "actionCheckResult.naturalAptitude"));
        }

        return resultsText.toString();
    }

}
