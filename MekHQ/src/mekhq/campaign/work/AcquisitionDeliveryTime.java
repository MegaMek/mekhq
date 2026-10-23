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
package mekhq.campaign.work;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;

/**
 * Shortens the delivery time of a successful acquisition based on the Margin of Success of the roll that found it,
 * when the 'Margin of Success Affects Delivery' campaign option is on.
 *
 * <p>Each point of Margin of Success reduces the delivery time by {@value #REDUCTION_PER_MARGIN_OF_SUCCESS_PERCENT}%,
 * up to {@value #MARGIN_OF_SUCCESS_MAX} points. A delivery is never reduced below one day.</p>
 */
public final class AcquisitionDeliveryTime {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";

    static final int REDUCTION_PER_MARGIN_OF_SUCCESS_PERCENT = 10;
    static final int MARGIN_OF_SUCCESS_MAX = 5;

    private AcquisitionDeliveryTime() {}

    /**
     * @param skillCheck  the acquisition check that was rolled
     * @param checkResult the result of rolling {@code skillCheck}
     *
     * @return the Margin of Success that should shorten the delivery, or 0 when the roll failed or the check was an
     *       automatic success (there is no real margin to reward)
     */
    public static int getDeliveryMarginOfSuccess(SkillCheck skillCheck, ActionCheckResult checkResult) {
        if (!checkResult.isSuccess() || skillCheck.getTargetNumber().isAutomaticSuccess()) {
            return 0;
        }
        return Math.clamp(checkResult.getMarginOfSuccess(), 0, MARGIN_OF_SUCCESS_MAX);
    }

    /**
     * @param marginOfSuccess the Margin of Success of the acquisition roll
     *
     * @return the percentage the delivery time is reduced by, from 0 to
     *       {@code MARGIN_OF_SUCCESS_MAX * REDUCTION_PER_MARGIN_OF_SUCCESS_PERCENT}
     */
    public static int getDeliveryReductionPercent(int marginOfSuccess) {
        return Math.clamp(marginOfSuccess, 0, MARGIN_OF_SUCCESS_MAX) * REDUCTION_PER_MARGIN_OF_SUCCESS_PERCENT;
    }

    /**
     * @param transitDays     the unmodified delivery time, in days
     * @param marginOfSuccess the Margin of Success of the acquisition roll
     *
     * @return the delivery time shortened by the Margin of Success, never below one day unless it was already zero
     */
    public static int applyMarginOfSuccess(int transitDays, int marginOfSuccess) {
        if (transitDays <= 1) {
            return transitDays;
        }
        int reductionPercent = getDeliveryReductionPercent(marginOfSuccess);
        int reducedDays = (int) Math.round(transitDays * (100 - reductionPercent) / 100.0);
        return Math.max(1, reducedDays);
    }

    /**
     * @param marginOfSuccess the Margin of Success of the acquisition roll
     *
     * @return a report fragment describing the expedited delivery, or an empty string when it was not shortened
     */
    public static String getDeliveryReport(int marginOfSuccess) {
        int reductionPercent = getDeliveryReductionPercent(marginOfSuccess);
        if (reductionPercent <= 0) {
            return "";
        }
        MarginOfSuccess reportMargin = MarginOfSuccess.getMarginOfSuccessObject(marginOfSuccess);
        return getFormattedTextAt(RESOURCE_BUNDLE, "acquireEquipment.expeditedDelivery.format",
              spanOpeningWithCustomColor(reportMargin.getColor()), CLOSING_SPAN_TAG, reductionPercent,
              marginOfSuccess);
    }
}
