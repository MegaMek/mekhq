/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.events.persons.PersonCrewAssignmentEvent;
import mekhq.campaign.events.persons.PersonEvent;
import mekhq.campaign.events.persons.PersonNewEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.commandGeneration.SupportCarrierReconciler;

/**
 * For processing events that should trigger for any kind of campaign, AtB or otherwise.
 *
 * @param campaign the campaign whose events this processor will handle
 */
public record CampaignEventProcessor(Campaign campaign) {

    public CampaignEventProcessor(Campaign campaign) {
        this.campaign = campaign;
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
        this.campaign().getPlayerForce().getHumanResources().invalidateActivePersonnelCache();
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
    /**
     * Seats a newly arrived character in a support carrier when they belong in one.
     *
     * <p>Covers hiring, GM additions and story-arc characters. Retraining and returns to duty arrive as
     * {@link PersonChangedEvent} instead and are handled by {@link #handleSupportRoleChange}.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param personNewEvent the event carrying the character who joined the campaign
     */
    @Subscribe
    public void handleNewPersonForCarrier(PersonNewEvent personNewEvent) {
        SupportCarrierReconciler.seatIfEligible(campaign(), personNewEvent.getPerson());
    }

    /**
     * Keeps a character's carrier seat matching their role and status.
     *
     * <p>{@link mekhq.campaign.events.persons.PersonStatusChangedEvent} extends {@link PersonChangedEvent}, so this
     * one subscription covers retraining into and out of a support role, returning from leave, academy or MIA, and
     * being freed from captivity. Deliberately not subscribed to the abstract {@code PersonEvent}, because
     * {@code PersonLogEvent} extends that and fires on every personnel-log entry.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param personChangedEvent the event carrying the character whose record changed
     */
    @Subscribe
    public void handleSupportRoleChange(PersonChangedEvent personChangedEvent) {
        // PersonCrewAssignmentEvent extends PersonChangedEvent, so it arrives here as well as at
        // handlePersonUnitAssignmentEvent. A crew change is not a role or status change and must never seat anyone:
        // the event that Unit.remove fires during removePerson would otherwise re-seat the character being deleted,
        // leaving a ghost crew reference in the carrier once the roster entry is gone.
        if (personChangedEvent instanceof PersonCrewAssignmentEvent) {
            return;
        }
        Person person = personChangedEvent.getPerson();
        SupportCarrierReconciler.releaseIfIneligible(campaign(), person);
        SupportCarrierReconciler.seatIfEligible(campaign(), person);
    }

    @Subscribe
    public void handlePersonUnitAssignmentEvent(PersonCrewAssignmentEvent personCrewAssignmentEvent) {
        Unit unit = personCrewAssignmentEvent.getUnit();
        // Seating a character fires this event back into us. Safe because the reconciler only ever deletes a carrier
        // that has reached zero crew, and seating moves crew the other way.
        SupportCarrierReconciler.onCarrierCrewChanged(campaign(), unit);

        // If this unit has no commander, clear out any temporary crew assignments
        if (unit != null && !unit.hasCommander() && unit.getTotalTempCrew() > 0) {
            unit.setTempCrew(unit.getDriverRole(), 0);
            unit.setTempCrew(unit.getGunnerRole(), 0);

            // TODO: Better way to handle this case
            unit.setTempCrew(PersonnelRole.VESSEL_CREW, 0);
        }
    }
}
