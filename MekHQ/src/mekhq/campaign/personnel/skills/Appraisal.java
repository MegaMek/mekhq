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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.List;

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

    private final static double MULTIPLIER_PER_MARGIN_OF_SUCCESS = 0.05;

    /**
     * Performs an appraisal skill check for a given person on the specified date and calculates the appraisal cost
     * multiplier based on the result.
     *
     * @param person     the {@link Person} performing the appraisal skill check
     * @param currentDay the current date of the appraisal check
     *
     * @return the calculated appraisal cost multiplier as a {@code double}
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static double performAppraisalMultiplierCheck(Person person, LocalDate currentDay) {
        SkillCheckUtility skillCheckUtility = new SkillCheckUtility(person, SkillType.S_APPRAISAL, List.of(), 0,
              false, false, false, false, currentDay);
        int marginOfSuccessValue = skillCheckUtility.getMarginOfSuccess();

        return getAppraisalCostMultiplier(marginOfSuccessValue);
    }

    /**
     * Calculates the appraisal cost multiplier for a person on a given date.
     *
     * <p>The multiplier increases or decreases based on the negative margin of success from an appraisal skill
     * check.</p>
     *
     * @param marginOfSuccessValue The return value of {@link MarginOfSuccess#getMarginValue(MarginOfSuccess)}
     *
     * @return The appraisal cost multiplier as a {@code double}.
     *
     * @author Illiani
     * @since 0.50.07
     */
    static double getAppraisalCostMultiplier(int marginOfSuccessValue) {
        return 1 - (marginOfSuccessValue * MULTIPLIER_PER_MARGIN_OF_SUCCESS);
    }

    /**
     * Generates an appraisal report string based on the provided cost multiplier.
     *
     * <p>The report includes a colored message representing the outcome of the appraisal as determined by the
     * corresponding margin of success.</p>
     *
     * @param appraisalCostMultiplier The calculated appraisal cost multiplier.
     *
     * @return An HTML-formatted String describing the appraisal result.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static String getAppraisalReport(double appraisalCostMultiplier) {
        MarginOfSuccess marginOfSuccess = getMarginOfSuccess(appraisalCostMultiplier);
        String reportColor = MarginOfSuccess.getMarginOfSuccessColor(marginOfSuccess);
        String reportKey = "Appraisal.report." + marginOfSuccess.name();

        return getFormattedTextAt(RESOURCE_BUNDLE,
              reportKey,
              spanOpeningWithCustomColor(reportColor),
              CLOSING_SPAN_TAG);
    }

    /**
     * Determines the {@link MarginOfSuccess} corresponding to the provided appraisal cost multiplier.
     *
     * <p>This converts the multiplier back to a margin value and looks up the appropriate result category.</p>
     *
     * @param appraisalCostMultiplier the appraisal cost multiplier to evaluate
     *
     * @return the {@link MarginOfSuccess} category for the given multiplier
     *
     * @author Illiani
     * @since 0.50.07
     */
    static MarginOfSuccess getMarginOfSuccess(double appraisalCostMultiplier) {
        LOGGER.debug("Appraisal report requested with multiplier: {}", appraisalCostMultiplier);
        int normalizedMarginValue = (int) Math.round(-(appraisalCostMultiplier - 1) / MULTIPLIER_PER_MARGIN_OF_SUCCESS);
        LOGGER.debug("Margin value: {}", normalizedMarginValue);

        return MarginOfSuccess.getMarginOfSuccessObjectFromMarginValue(normalizedMarginValue);
    }
}
