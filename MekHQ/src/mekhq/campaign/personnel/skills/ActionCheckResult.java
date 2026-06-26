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

import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;
import mekhq.utilities.ReportingUtilities;

import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.BARELY_MADE_IT;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginOfSuccessObjectFromMarginValue;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;

/**
 * An immutable record representing the outcome of an action check.
 *
 * @param roll            Roll result for the action check
 * @param marginOfSuccess Calculated margin of success for this action check. Represents how much better (or worse)
 *                        the roll was compared to the target number
 * @param usedEdge        Indicates whether edge was used during the action check
 * @param resultsText     A string representing the outcome of the action check
 *
 * @author Hokk
 * @since 0.51.01
 */
public record ActionCheckResult(
      int roll,
      int marginOfSuccess,
      boolean usedEdge,
      String resultsText
) {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ActionCheck";

    /**
     * Determines whether the action check was successful.
     *
     * <p>An action check is considered successful if the calculated margin of success is equal or exceeds
     * {@link MarginOfSuccess#BARELY_MADE_IT}.</p>
     *
     * @return {@code true} if the action check succeeded, {@code false} otherwise
     */
    public boolean isSuccess() {
        return isSuccess(marginOfSuccess);
    }

    /**
     * Determines whether a given margin of success represents a successful action check.
     *
     * <p>A margin is considered successful if it is equal or exceeds {@link MarginOfSuccess#BARELY_MADE_IT}.</p>
     *
     * @param marginOfSuccess the margin of success to evaluate
     * @return {@code true} if the margin qualifies as a success, {@code false} otherwise
     */
    public static boolean isSuccess(int marginOfSuccess) {
        return marginOfSuccess >= BARELY_MADE_IT.getValue();
    }

    /**
     * Returns a skill check report. It takes resultsText generated during the skill check and extends it with
     * information about edge usage and, optionally, margin of success.
     *
     * <p>See more in {@link AttributeCheck#generateResultsText}.</p>
     *
     * <p>If edge was used to reroll the skill check, the results will include an additional note with
     * information about the reroll. If the caller requests it, margin of success details can also be
     * appended to the results text.</p>
     *
     * @param includeMarginsOfSuccessText whether to include detailed margins of success information in the results
     */
    public String getReport(boolean includeMarginsOfSuccessText) {
        StringBuilder report = new StringBuilder(resultsText);
        if (usedEdge) {
            report.append(" ").append(getTextAt(RESOURCE_BUNDLE, "actionCheckResult.rerolled"));
        }
        if (includeMarginsOfSuccessText) {
            String color;
            int neutralMarginValue = BARELY_MADE_IT.getValue();
            if (marginOfSuccess == neutralMarginValue) {
                color = ReportingUtilities.getWarningColor();
            } else if (marginOfSuccess < neutralMarginValue) {
                color = ReportingUtilities.getNegativeColor();
            } else {
                color = ReportingUtilities.getPositiveColor();
            }
            MarginOfSuccess marginOfSuccessObject = getMarginOfSuccessObjectFromMarginValue(marginOfSuccess);
            String marginOfSuccessText =
                  ReportingUtilities.messageSurroundedBySpanWithColor(color, marginOfSuccessObject.getLabel());
            report.append(" ").append(marginOfSuccessText);
        }
        return report.toString();
    }
}
