/*
 * Copyright (c) 2017-2025 The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.event;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

import static mekhq.campaign.force.Force.FORCE_NONE;

/**
 * Represents an event triggered when a {@link Person} is assigned to or removed from a {@link Unit},
 * either as a pilot or part of the crew.
 *
 * <p>This event is a subclass of {@link PersonChangedEvent} and adds the context of the
 * {@link Unit} involved in the assignment or removal.</p>
 *
 * <p>If the {@link Unit} is associated with a force, the force's commander information will
 * be updated accordingly through the {@link Force#updateCommander(Campaign)} method.</p>
 */
public class PersonCrewAssignmentEvent extends PersonChangedEvent {

    private final Unit unit;

    /**
     * Creates a new {@code PersonCrewAssignmentEvent}.
     *
     * @param campaign The {@link Campaign} to which this event belongs.
     * @param crew     The {@link Person} assigned or removed from the {@link Unit}.
     * @param unit     The {@link Unit} involved in the assignment or removal.
     *
     * <p>If the {@code unit} is associated with a force, the force's commander information is updated
     * during the construction of this event by calling {@link Force#updateCommander(Campaign)}.</p>
     */
    public PersonCrewAssignmentEvent(Campaign campaign, Person crew, Unit unit) {
        super(crew);
        this.unit = unit;

        int forceId = unit.getForceId();

        if (forceId != FORCE_NONE) {
            Force force = campaign.getForce(forceId);

            if (force != null) {
                force.updateCommander(campaign);
            }
        }
    }

    /**
     * Gets the {@link Unit} associated with this event.
     *
     * @return The {@link Unit} involved in the assignment or removal.
     */
    public Unit getUnit() {
        return unit;
    }

}
