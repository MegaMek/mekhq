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
 * An event triggered just after all new day calculations were finished, but before the UI is updated
 * with the new data.
 * <p>
 * The new day can be queried via <code>event.getCampaign().getDate()</code>
 */
public class NewDayEvent extends CampaignEvent {
    public NewDayEvent(Campaign campaign) {
        super(campaign);
    }
}
