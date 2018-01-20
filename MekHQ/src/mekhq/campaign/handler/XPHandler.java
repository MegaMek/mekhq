/*
 * Copyright (C) 2016 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.handler;

import java.util.Calendar;

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
        if((adminXP <= 0) || (campaign.getCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)) {
            return;
        }
        for (Person person : campaign.getAdmins()) {
            if (person.isAdminPrimary()) {
                if(adminXPPeriod > 1) {
                    Integer weeksLeft = person.getExtraData().get(NEXT_ADMIN_XP_DELAY);
                    if(null == weeksLeft) {
                        // Assign a random value between 1 and the max
                        weeksLeft = Compute.randomInt(adminXPPeriod) + 1;
                    }
                    -- weeksLeft;
                    if(weeksLeft == 0) {
                        person.awardXP(adminXP);
                        weeksLeft = adminXPPeriod;
                    }
                    person.getExtraData().set(NEXT_ADMIN_XP_DELAY, weeksLeft);
                } else {
                    person.awardXP(adminXP);
                }
            }
        }
    }
}
