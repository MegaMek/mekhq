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
package mekhq.campaign.events;

import megamek.common.event.MMEvent;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignNewDayManager;

/**
 * Event fired when something requests that any ongoing Advance Multiple Days process be interrupted after the current
 * day completes.
 *
 * <p>Listeners that wish to halt multi-day advancement should fire this event via {@code MekHQ.triggerEvent(new
 * InterruptAdvanceMultipleDaysEvent(campaign))}. {@link CampaignNewDayManager} subscribes to this event and sets
 * {@code startDayWithNoInterruptions = false} upon receipt.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class InterruptAdvanceMultipleDaysEvent extends MMEvent {
    private final Campaign campaign;

    public InterruptAdvanceMultipleDaysEvent(Campaign campaign) {
        this.campaign = campaign;
    }

    public Campaign getCampaign() {
        return campaign;
    }
}
