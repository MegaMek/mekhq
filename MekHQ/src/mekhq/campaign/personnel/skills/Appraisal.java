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

import static mekhq.campaign.personnel.skills.SkillType.S_APPRAISAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;

import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;

/**
 * Handles appraisal cost multiplier calculations.
 *
 * <p>The appraisal multiplier is modified based on a character's skill check margin of success.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class Appraisal {
    private static final MMLogger LOGGER = MMLogger.create(Appraisal.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Appraisal";

    private final static double MULTIPLIER_PER_MARGIN_OF_SUCCESS = 0.025;
    private final static int MARGIN_OF_SUCCESS_MIN = -6;
    private final static int MARGIN_OF_SUCCESS_MAX = 6;

    /**
     * Performs an appraisal skill check for a given person on the specified date.
     *
     * @param person     the {@link Person} performing the appraisal skill check
     * @param currentDay the current date of the appraisal check
     * @param isUseEdge  {@code true} if an Edge reroll should be used for a failed check
     *
     * @return the appraisal skill check result
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static ActionCheckResult performAppraisalCheck(Person person, LocalDate currentDay,
          boolean isUseEdge) {
        return person.checkSkill(S_APPRAISAL, false, false, currentDay)
                    .resolve(isUseEdge, getTextAt(RESOURCE_BUNDLE, "Appraisal.skillCheck"));
    }

    /**
     * Calculates the appraisal cost multiplier for a person on a given date.
     *
     * <p>The multiplier increases or decreases based on the negative margin of success from an appraisal skill
     * check.</p>
     *
     * @param marginOfSuccessValue The skill check's margin of success
     *
     * @return The appraisal cost multiplier as a {@code double}.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static double getAppraisalCostMultiplier(int marginOfSuccessValue) {
        int marginOfSuccessCapped = Math.clamp(marginOfSuccessValue, MARGIN_OF_SUCCESS_MIN, MARGIN_OF_SUCCESS_MAX);
        return 1 - (marginOfSuccessCapped * MULTIPLIER_PER_MARGIN_OF_SUCCESS);
    }

    /**
     * Generates an appraisal report string based on the provided cost multiplier.
     *
     * <p>The report includes a colored message representing the outcome of the appraisal as determined by the
     * corresponding margin of success.</p>
     *
     * @param appraisalCostMultiplier The calculated appraisal cost multiplier.
     * @param reportMargin            The {@link MarginOfSuccess} for report text generation.
     *
     * @return An HTML-formatted String describing the appraisal result.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static String getAppraisalReport(double appraisalCostMultiplier, MarginOfSuccess reportMargin) {
        String reportColor = reportMargin.getColor();
        String reportKey = "Appraisal.report." + reportMargin.name();

        return getFormattedTextAt(RESOURCE_BUNDLE,
              reportKey,
              spanOpeningWithCustomColor(reportColor),
              CLOSING_SPAN_TAG,
              appraisalCostMultiplier * 100);
    }

}
