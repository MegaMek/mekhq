/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.events.persons;

import static mekhq.campaign.force.Formation.FORCE_NONE;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * Represents an event triggered when a {@link Person} is assigned to or removed from a {@link Unit}, either as a pilot
 * or part of the crew.
 *
 * <p>This event is a subclass of {@link PersonChangedEvent} and adds the context of the
 * {@link Unit} involved in the assignment or removal.</p>
 *
 * <p>If the {@link Unit} is associated with a force, the force's commander information will
 * be updated accordingly through the {@link Formation#updateCommander(Campaign)} method.</p>
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
     *                 <p>If the {@code unit} is associated with a force, the force's commander information is updated
     *                 during the construction of this event by calling {@link Formation#updateCommander(Campaign)}.</p>
     */
    public PersonCrewAssignmentEvent(Campaign campaign, Person crew, Unit unit) {
        super(crew);
        this.unit = unit;

        int forceId = unit.getForceId();

        if (forceId != FORCE_NONE) {
            Formation formation = campaign.getForce(forceId);

            if (formation != null) {
                formation.updateCommander(campaign);
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
