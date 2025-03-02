/*
 * Copyright (C) 2018-2025 The MegaMek Team
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
 * creating free software for the BattleTech community. BattleMech,
 * BattleTech, and MechWarrior are trademarks of The Topps Company, Inc.
 * The MegaMek organization is not affiliated with The Topps Company, Inc.
 * or Catalyst Game Labs.
 */

package mekhq.campaign.event;

import megamek.common.event.MMEvent;
import mekhq.campaign.CurrentLocation;

/**
 * Event for a change of location (planetary system) for the campaign.
 *
 * @author Neoancient
 *
 */
public class LocationChangedEvent extends MMEvent {

    private final CurrentLocation location;
    private final boolean kfJump;

    /**
     * An event that is triggered when the campaign location changes to a different planetary system.
     *
     * @param location The campaign location object.
     * @param kfJump   Whether the jump occurred as a result of moving to the next location in a jump
     *                 path (as opposed to GM set location)
     */
    public LocationChangedEvent(CurrentLocation location, boolean kfJump) {
        this.location = location;
        this.kfJump = kfJump;
    }

    /**
     * @return The campaign's location object.
     */
    public CurrentLocation getLocation() {
        return location;
    }

    /**
     * @return true if the location change is the result of moving to the next location in a jump
     *         path as part of the campaign new day process (as opposed changing the location using
     *         GM mode).
     */
    public boolean isKFJump() {
        return kfJump;
    }

}
