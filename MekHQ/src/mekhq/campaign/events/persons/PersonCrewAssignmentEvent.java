/*
 * Copyright (C) 2017-2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.force.Formation.FORMATION_NONE;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
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

        int forceId = unit.getFormationId();

        if (forceId != FORMATION_NONE) {
            Formation formation = campaign.getFormation(forceId);

            if (formation != null) {
                formation.updateCommander(campaign);
            }
        }

        removeExcessTempCrew();
    }

    /**
     * Removes any temporary crew from the unit that exceed its full crew requirement.
     *
     * <p>This can occur when a real {@link Person} fills a slot that was already covered by temp
     * crew of a <em>different</em> role (e.g. a vessel gunner replaces a SOLDIER temp-crew slot), or when crew-add
     * methods other than {@code addPilotOrSoldier} assign a person without first trimming temp crew.</p>
     *
     * <p>The person's primary role is preferred — temp crew of that role is removed first. Any
     * residual excess (e.g. the roles don't match) is then drained from whichever other roles still have temp
     * crew.</p>
     */
    private void removeExcessTempCrew() {
        int excess = unit.getTotalCrewSize() - unit.getFullCrewSize();

        if (excess <= 0) {
            return;
        }

        // Prefer removing temp crew whose role matches the person being assigned,
        // since that is the slot semantically being filled.
        PersonnelRole primaryRole = getPerson().getPrimaryRole();
        int matching = unit.getTempCrewByPersonnelRole(primaryRole);
        if (matching > 0) {
            int toRemove = Math.min(excess, matching);
            unit.setTempCrew(primaryRole, matching - toRemove);
            excess -= toRemove;
        }

        // Drain any remaining excess from other roles (order is arbitrary); Should only be needed to get existing
        // saves fixed.
        if (excess > 0) {
            for (PersonnelRole role : unit.getTempCrewRoles()) {
                if (role.equals(primaryRole)) {
                    continue;
                }

                int current = unit.getTempCrewByPersonnelRole(role);
                int toRemove = Math.min(excess, current);
                unit.setTempCrew(role, current - toRemove);
                excess -= toRemove;

                if (excess <= 0) {
                    break;
                }
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
