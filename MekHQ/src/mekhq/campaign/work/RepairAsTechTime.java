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

import megamek.logging.MMLogger;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.unit.Unit;

/**
 * Charges the AsTech pool for the time its AsTechs spend helping a tech with a repair. Every day of a multi-day job is
 * charged for the minutes worked that day, not just the day the job finishes. A self-crewed unit, such as a DropShip
 * or self-maintained infantry, is repaired with the help of its own crew, so it never draws on the AsTech pool.
 */
public class RepairAsTechTime {
    private static final MMLogger LOGGER = MMLogger.create(RepairAsTechTime.class);

    private final ForceHumanResources humanResources;
    private final boolean isOvertimeAllowed;
    private final CampaignOptions campaignOptions;

    /**
     * @param humanResources    the force whose AsTech pool helps with the repair
     * @param isOvertimeAllowed whether overtime is allowed, which lets AsTechs help from their overtime too
     * @param campaignOptions   the campaign options, which decide how many AsTechs the force has
     */
    public RepairAsTechTime(ForceHumanResources humanResources, boolean isOvertimeAllowed,
          CampaignOptions campaignOptions) {
        this.humanResources = humanResources;
        this.isOvertimeAllowed = isOvertimeAllowed;
        this.campaignOptions = campaignOptions;
    }

    /**
     * Charges the AsTech pool for the minutes a tech has just worked on a task, taking the pool's normal minutes
     * first and then its overtime.
     *
     * @param partWork      the task being worked on
     * @param minutesWorked the minutes the tech worked on it today, overtime included
     * @param usedOvertime  whether the tech worked overtime on it today
     *
     * @return how many AsTechs helped, {@code 0} when the unit is self-crewed
     */
    public int chargeForMinutesWorked(IPartWork partWork, int minutesWorked, boolean usedOvertime) {
        Unit unit = partWork.getUnit();
        if ((unit != null) && unit.isSelfCrewed()) {
            LOGGER.debug("[RepairAsTechTime] {}: no AsTech time charged, {} is repaired with its own crew",
                  partWork.getPartName(), unit.getName());
            return 0;
        }
        int helpers = humanResources.getAvailableAsTechs(minutesWorked, usedOvertime, isOvertimeAllowed,
              campaignOptions);
        int asTechMinutes = minutesWorked * helpers;
        int poolMinutes = humanResources.getAsTechPoolMinutes();
        if (poolMinutes < asTechMinutes) {
            humanResources.setAsTechPoolMinutes(0);
            humanResources.setAsTechPoolOvertime(humanResources.getAsTechPoolOvertime() - (asTechMinutes
                                                                                                 - poolMinutes));
        } else {
            humanResources.setAsTechPoolMinutes(poolMinutes - asTechMinutes);
        }
        LOGGER.debug("[RepairAsTechTime] {}: {} AsTechs helped for {} minutes, {} AsTech minutes charged",
              partWork.getPartName(), helpers, minutesWorked, asTechMinutes);
        return helpers;
    }
}
