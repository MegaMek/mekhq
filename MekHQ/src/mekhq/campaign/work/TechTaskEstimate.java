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

import megamek.common.compute.Compute;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.Person;

/**
 * What a tech can expect from a task: the target number, the chance of success, how long it takes and when it would
 * be done. The tech list shows this for every tech offered the selected task, so a player can compare techs without
 * assigning each one in turn.
 *
 * @param targetNumber    the number the tech must roll on 2d6
 * @param targetDetails   the modifiers that make up the target number, or why the task cannot be done
 * @param isImpossible    {@code true} if the tech cannot do the task at all
 * @param successPercent  the chance of rolling the target number or better, from 0 to 100
 * @param minutesNeeded   the minutes of work the task still needs
 * @param daysToFinish    {@code 0} if the tech finishes today, otherwise how many more days the work runs into
 */
public record TechTaskEstimate(int targetNumber, String targetDetails, boolean isImpossible, int successPercent,
      int minutesNeeded, int daysToFinish) {

    /**
     * @param campaign the campaign
     * @param partWork the task
     * @param tech     the tech
     *
     * @return the tech's estimate for the task
     */
    public static TechTaskEstimate estimate(Campaign campaign, IPartWork partWork, Person tech) {
        TargetRoll target = findTarget(campaign, partWork, tech);
        int targetNumber = target.getValue();
        boolean isImpossible = (targetNumber == TargetRoll.IMPOSSIBLE) || (targetNumber == TargetRoll.AUTOMATIC_FAIL);
        int minutesNeeded = partWork.getTimeLeft();
        return new TechTaskEstimate(targetNumber, target.getDesc(), isImpossible, findSuccessPercent(targetNumber),
              minutesNeeded, findDaysToFinish(campaign, tech, minutesNeeded));
    }

    /**
     * Works out the target number as if this tech had the task, the same way the Repair tab does: some modifiers,
     * such as a Clan tech working on Clan equipment, look at the tech on the task.
     */
    private static TargetRoll findTarget(Campaign campaign, IPartWork partWork, Person tech) {
        boolean hasNoTech = partWork.getTech() == null;
        if (hasNoTech) {
            partWork.setTech(tech);
        }
        try {
            return campaign.getTargetFor(partWork, tech);
        } finally {
            if (hasNoTech) {
                partWork.setTech(null);
            }
        }
    }

    private static int findSuccessPercent(int targetNumber) {
        if (targetNumber == TargetRoll.AUTOMATIC_SUCCESS) {
            return 100;
        }
        boolean isRollable = (targetNumber != TargetRoll.IMPOSSIBLE) && (targetNumber != TargetRoll.AUTOMATIC_FAIL);
        return isRollable ? (int) Math.round(Compute.oddsAbove(targetNumber)) : 0;
    }

    /**
     * @return {@code 0} if the work fits in the tech's time today (overtime included when allowed), otherwise how many
     *       more full working days it runs into
     */
    private static int findDaysToFinish(Campaign campaign, Person tech, int minutesNeeded) {
        int overtimeToday = campaign.isOvertimeAllowed() ? tech.getOvertimeLeft() : 0;
        int minutesToday = tech.getMinutesLeft() + overtimeToday;
        if (minutesNeeded <= minutesToday) {
            return 0;
        }
        boolean techsUseAdministration = campaign.getCampaignOptions().get(CampaignOption.TECHS_USE_ADMINISTRATION);
        int minutesPerDay = Math.max(1, tech.getDailyAvailableTechTime(techsUseAdministration));
        int minutesAfterToday = minutesNeeded - Math.max(0, minutesToday);
        return (int) Math.ceil((double) minutesAfterToday / minutesPerDay);
    }
}
