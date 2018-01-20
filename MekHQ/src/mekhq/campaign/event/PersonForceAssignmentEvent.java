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

import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;

/**
 * Triggered when a tech is assigned to or removed from a force.
 *
 */
public class PersonForceAssignmentEvent extends PersonChangedEvent {

    private final Force force;

    public PersonForceAssignmentEvent(Person person, Force force) {
        super(person);
        this.force = force;
    }

    public Force getForce() {
        return force;
    }

}
