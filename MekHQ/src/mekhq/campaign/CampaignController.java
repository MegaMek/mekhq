/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;

import java.util.UUID;

/**
 * Manages the timeline of a {@link Campaign}.
 */
public class CampaignController {
    private final Campaign localCampaign;
    private boolean isHost;
    private UUID host;

    /**
     * Creates a new {@code CampaignController} for the given {@link Campaign}
     *
     * @param c The {@link Campaign} being used locally.
     */
    public CampaignController(Campaign c) {
        localCampaign = c;
    }

    /**
     * Gets the local {@link Campaign}.
     *
     * @return The local {@link Campaign}.
     */
    public Campaign getLocalCampaign() {
        return localCampaign;
    }

    /**
     * Gets the unique identifier of the campaign hosting this session.
     *
     * @return The unique identifier of the host campaign.
     */
    public UUID getHost() {
        return host;
    }

    /**
     * Sets the unique identifier of the campaign hosting this session.
     *
     * @param id The unique identifier of the host campaign.
     */
    public void setHost(UUID id) {
        host = id;
        isHost = getLocalCampaign().getId().equals(id);
    }

    /**
     * Gets a value indicating whether the local Campaign is hosting this session.
     *
     * @return {@code true} if the local campaign is hosting this session, otherwise {@code false}.
     */
    public boolean isHost() {
        return isHost;
    }

    /**
     * Advances the local {@link Campaign} to the next day.
     */
    public void advanceDay() {
    }
}
