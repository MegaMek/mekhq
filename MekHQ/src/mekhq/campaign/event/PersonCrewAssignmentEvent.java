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

import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * Triggered when a person is assigned to or removed from a unit as pilot or crew.
 *
 */
public class PersonCrewAssignmentEvent extends PersonChangedEvent {

    private final Unit unit;

    public PersonCrewAssignmentEvent(Person crew, Unit unit) {
        super(crew);
        this.unit = unit;
    }

    public Unit getUnit() {
        return unit;
    }

}
