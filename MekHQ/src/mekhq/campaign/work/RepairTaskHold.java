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

import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import megamek.common.annotations.Nullable;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * Stops a repair task that cannot go ahead today from being attempted anyway. An impossible task always fails its roll,
 * and a failure raises the task's difficulty and can destroy the part reserved for it, so such a task is held instead:
 * no time is spent and nothing is rolled. When the reason is temporary - the unit is deployed, refitting or mothballed,
 * or the tech has no time left after maintenance - the tech stays assigned and the task carries on once the reason
 * passes. Otherwise the tech is taken off the task, which keeps the work already done.
 */
public final class RepairTaskHold {
    private static final MMLogger LOGGER = MMLogger.create(RepairTaskHold.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";

    private RepairTaskHold() {}

    /**
     * @param campaign the campaign
     * @param partWork the task
     * @param tech     the tech working on it
     * @param target   the task's target number for this tech
     *
     * @return the report when the task is held today, or {@code null} when it can go ahead
     */
    public static @Nullable String holdIfItCannotGoAhead(Campaign campaign, IPartWork partWork, Person tech,
          TargetRoll target) {
        boolean isImpossible = target.getValue() == TargetRoll.IMPOSSIBLE;
        boolean isAutomaticFailure = target.getValue() == TargetRoll.AUTOMATIC_FAIL;
        if (!isImpossible && !isAutomaticFailure) {
            return null;
        }

        String report;
        if (isReasonTemporary(campaign, partWork, tech)) {
            report = getFormattedTextAt(RESOURCE_BUNDLE, "fixPart.taskWaits.report", tech.getHyperlinkedFullTitle(),
                  partWork.getPartName(), target.getDesc());
            LOGGER.debug("[RepairTask] {}: {} waits, {}", tech.getFullName(), partWork.getPartName(),
                  target.getDesc());
        } else {
            partWork.cancelAssignment(false);
            report = getFormattedTextAt(RESOURCE_BUNDLE, "fixPart.taskStopped.report", tech.getHyperlinkedFullTitle(),
                  partWork.getPartName(), target.getDesc());
            LOGGER.debug("[RepairTask] {}: {} stopped, progress kept, {}", tech.getFullName(),
                  partWork.getPartName(), target.getDesc());
        }
        campaign.addReport(TECHNICAL, report);
        return report;
    }

    /**
     * @return {@code true} if the task is blocked only for today: its unit is away, or the tech has no time left
     */
    private static boolean isReasonTemporary(Campaign campaign, IPartWork partWork, Person tech) {
        Unit unit = partWork.getUnit();
        boolean isUnitAway = (unit != null) && !unit.isAvailable(partWork instanceof Refit);
        int techTimeLeft = campaign.isOvertimeAllowed() ? (tech.getMinutesLeft() + tech.getOvertimeLeft())
              : tech.getMinutesLeft();
        boolean hasNoTimeToday = techTimeLeft <= 0;
        return isUnitAway || hasNoTimeToday;
    }
}
