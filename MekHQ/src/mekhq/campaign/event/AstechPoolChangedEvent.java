/*
 * Copyright (C) 2017 MegaMek team
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
 * Triggered when astechs are added to or removed from the pool.
 *
 */
public class AstechPoolChangedEvent extends CampaignEvent {

    private final int increase;

    public AstechPoolChangedEvent(Campaign campaign, int increase) {
        super(campaign);
        this.increase = increase;
    }

    public int getIncrease() {
        return increase;
    }

}
