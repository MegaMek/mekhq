/*
 * Copyright (c) 2018 The MegaMek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
