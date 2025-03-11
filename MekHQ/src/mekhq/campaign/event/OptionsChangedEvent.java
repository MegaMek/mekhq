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
