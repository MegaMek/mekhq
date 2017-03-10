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

import megamek.common.event.MMEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.unit.Unit;

/**
 * Triggered when the TO&E structure changes by adding, removing, or moving a force or adding or removing
 * a unit.
 *
 */
public class OrganizationChangedEvent extends MMEvent {

    private final Force force;
    private final Unit unit;
    
    public OrganizationChangedEvent(Force force) {
        this.force = force;
        this.unit = null;
    }
    
    public OrganizationChangedEvent(Unit unit) {
        this.force = null;
        this.unit = unit;
    }
    
    public OrganizationChangedEvent(Force force, Unit unit) {
        this.force = force;
        this.unit = unit;
    }
    
    public Force getForce() {
        return force;
    }
    
    public Unit getUnit() {
        return unit;
    }
}
