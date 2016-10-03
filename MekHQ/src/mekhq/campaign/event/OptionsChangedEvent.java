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

import java.util.Objects;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;

/**
 * An event thrown after the campaign options were changed. The event handlers aren't supposed
 * to modify those options.
 * <p>
 * The structure of the campaign options dialogue right now doesn't allow for the
 * event to be intercepted/cancelled. This will likely change in the near future.
 */
public class OptionsChangedEvent extends CampaignEvent {
    private CampaignOptions options;

    public OptionsChangedEvent(Campaign campaign) {
        this(campaign, campaign.getCampaignOptions());
    }
    
    public OptionsChangedEvent(Campaign campaign, CampaignOptions options) {
        super(campaign);
        this.options = Objects.requireNonNull(options);
    }
    
    public CampaignOptions getOptions() {
        return options;
    }
}