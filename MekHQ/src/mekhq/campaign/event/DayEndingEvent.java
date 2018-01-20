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
package mekhq.campaign.event;

import mekhq.campaign.Campaign;

/**
 * An event occurring before a day ends. If cancelled, the day end routine will not run.
 * <p>
 * The current about-to-end day can be queried via <code>event.getCampaign().getDate()</code>
 * <p>
 * Since it's a cancellable event, it's not guaranteed that an event handler will get to see
 * it every time the player tries to advance the day. The <i>first</i> event which cancel
 * it will make the game skip querying all the other events. Consequently, this event
 * like all cancellable events should not change any game state - use NewDayEvent for that.
 */
public final class DayEndingEvent extends CampaignEvent {
    public DayEndingEvent(Campaign campaign) {
        super(campaign);
    }

    @Override
    public boolean isCancellable() {
        return true;
    }
}
