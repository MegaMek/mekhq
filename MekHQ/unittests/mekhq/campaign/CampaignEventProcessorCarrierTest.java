/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.events.persons.PersonCrewAssignmentEvent;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * Pins the one event-routing rule the carrier reconciler depends on: a crew-assignment change must never be treated
 * as a role or status change.
 *
 * <p>{@link PersonCrewAssignmentEvent} extends {@link PersonChangedEvent}, so it is delivered to the role-change
 * subscriber as well as the crew-change one. {@code Unit.remove} fires it during {@code removePerson}, before the
 * roster entry is deleted; if the role-change subscriber acted on it, it would seat the character being deleted and
 * leave a ghost crew reference behind. This test failed on the version that shipped that bug to a playtest.</p>
 */
class CampaignEventProcessorCarrierTest {

    @Test
    void crewAssignmentEvent_doesNotSeatAnUnassignedSupportCharacter() {
        Campaign campaign = mock(Campaign.class);
        PlayerForce playerForce = mock(PlayerForce.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        Person person = unseatedAdministrator();
        Unit carrier = mock(Unit.class);

        new CampaignEventProcessor(campaign).handleSupportRoleChange(new PersonCrewAssignmentEvent(campaign, person,
              carrier));

        // Reaching the reconciler's seating path would resolve the Support Command formation first.
        verify(playerForce, never()).getSupportCommandFormation();
    }

    @Test
    void plainChangedEvent_stillReachesTheReconciler() {
        Campaign campaign = mock(Campaign.class);
        PlayerForce playerForce = mock(PlayerForce.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getSupportCommandFormation()).thenReturn(null);
        Person person = unseatedAdministrator();

        new CampaignEventProcessor(campaign).handleSupportRoleChange(new PersonChangedEvent(person));

        // Same character, a genuine change event: the reconciler is consulted (and stops at "no support structure").
        verify(playerForce, times(1)).getSupportCommandFormation();
    }

    /** An active, free administrator holding no unit - exactly what a GM removal looks like mid-flight. */
    private static Person unseatedAdministrator() {
        Person person = mock(Person.class);
        when(person.getUnit()).thenReturn(null);
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.ADMINISTRATOR);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        return person;
    }
}
