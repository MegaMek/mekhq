/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.events.persons.PersonCrewAssignmentEvent;
import mekhq.campaign.events.persons.PersonEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;

/**
 * For processing events that should trigger for any kind of campaign, AtB or otherwise.
 *
 * @param campaign the campaign whose events this processor will handle
 */
public record CampaignEventProcessor(Campaign campaign) {

    public CampaignEventProcessor(Campaign campaign) {
        this.campaign = campaign;
        MekHQ.registerHandler(this);
    }

    /**
     * Unregisters this processor from MekHQ's event handling system.
     *
     * <p>This should be called when the associated campaign is being shut down
     * or replaced, to avoid memory leaks from lingering event handlers.</p>
     */
    public void shutdown() {
        MekHQ.unregisterHandler(this);
    }

    /**
     * Handles updates to personnel records.
     *
     * <p>Clears cached values</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param personEvent the event containing updates related to a person in the campaign
     */
    @Subscribe
    public void handlePersonUpdate(PersonEvent personEvent) {
        campaign().invalidateActivePersonnelCache();
        Person person = personEvent.getPerson();
        person.invalidateAdvancedAsTechContribution();
    }

    /**
     * Handles unit crew assignment events.
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param personCrewAssignmentEvent the event containing the unit and crew assignment information
     */
    @Subscribe
    public void handlePersonUnitAssignmentEvent(PersonCrewAssignmentEvent personCrewAssignmentEvent) {
        Unit unit = personCrewAssignmentEvent.getUnit();

        // If this unit has no commander, clear out any temporary crew assignments
        if (unit != null && !unit.hasCommander() && unit.getTotalTempCrew() > 0) {
            unit.setTempCrew(unit.getDriverRole(), 0);
            unit.setTempCrew(unit.getGunnerRole(), 0);

            // TODO: Better way to handle this case
            unit.setTempCrew(PersonnelRole.VESSEL_CREW, 0);
        }
    }
}
