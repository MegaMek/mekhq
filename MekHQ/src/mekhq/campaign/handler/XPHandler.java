/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.handler;

import java.time.DayOfWeek;

import megamek.common.Compute;
import megamek.common.event.Subscribe;
import mekhq.campaign.Campaign;
import mekhq.campaign.ExtraData;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.personnel.Person;

/**
 * Event handler for all kind of XP calculations
 */
public class XPHandler {
    private static final ExtraData.Key<Integer> NEXT_ADMIN_XP_DELAY = new ExtraData.IntKey("next_admin_xp_delay");
    private int adminXP;
    private int adminXPPeriod;

    @Subscribe
    public void campaignOptionsHandler(OptionsChangedEvent event) {
        this.adminXP = event.getOptions().getAdminXP();
        this.adminXPPeriod = event.getOptions().getAdminXPPeriod();
    }

    @Subscribe
    public void processAdminXP(NewDayEvent event) {
        final Campaign campaign = event.getCampaign();
        if ((adminXP <= 0) || (campaign.getLocalDate().getDayOfWeek() != DayOfWeek.MONDAY)) {
            return;
        }
        for (Person person : campaign.getAdmins()) {
            if (person.getPrimaryRole().isAdministrator()) {
                if (adminXPPeriod > 1) {
                    Integer weeksLeft = person.getExtraData().get(NEXT_ADMIN_XP_DELAY);
                    if (null == weeksLeft) {
                        // Assign a random value between 1 and the max
                        weeksLeft = Compute.randomInt(adminXPPeriod) + 1;
                    }

                    if (--weeksLeft == 0) {
                        person.awardXP(campaign, adminXP);
                        weeksLeft = adminXPPeriod;
                    }
                    person.getExtraData().set(NEXT_ADMIN_XP_DELAY, weeksLeft);
                } else {
                    person.awardXP(campaign, adminXP);
                }
            }
        }
    }
}
