/*
 * Copyright (c) 2017 The MegaMek Team. All rights reserved.
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

import java.util.Objects;

import megamek.common.event.MMEvent;
import mekhq.campaign.mission.Mission;

/**
 * Triggered when a mission or contract is modified, including resolution or deletion.
 *
 */
public class MissionChangedEvent extends MMEvent {

    private final Mission mission;
    private final boolean removed;
    
    public MissionChangedEvent(Mission mission, boolean removed) {
        this.mission = Objects.requireNonNull(mission);
        this.removed = removed;
    }
    
    public MissionChangedEvent(Mission mission) {
        this(mission, false);
    }
    
    public Mission getMission() {
        return mission;
    }
    
    public boolean wasRemoved() {
        return removed;
    }
}
