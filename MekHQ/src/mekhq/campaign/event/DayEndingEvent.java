/*
 * Copyright (C)2016-2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
