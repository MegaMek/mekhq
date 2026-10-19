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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.List;
import java.util.UUID;

import megamek.common.units.Dropship;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Issue #10540: a unit's crew list could keep someone who had left it, such as a crew member who retired in 3046 but
 * was still listed on their Excalibur. On load, those entries are dropped, and the people stay in the campaign.
 */
class UnitCrewRepairTest {
    private Campaign campaign;
    private Unit dropShip;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        dropShip = spy(new Unit(mock(Dropship.class), campaign));
        doNothing().when(dropShip).resetPilotAndEntity();
    }

    @Test
    void retiredCrewMemberStillListedIsDropped() {
        Person mate = vesselCrew();
        Person retiredCrewMember = vesselCrew();
        when(retiredCrewMember.getUnit()).thenReturn(null);
        clearInvocations(dropShip);

        assertTrue(UnitCrewRepair.repair(dropShip));

        assertEquals(List.of(mate), dropShip.getVesselCrew());
        verify(dropShip).resetPilotAndEntity();
    }

    @Test
    void crewMemberWhoMovedToAnotherUnitIsDroppedAndKeepsTheirNewPost() {
        Unit otherShip = mock(Unit.class);
        Person movedNavigator = mockPerson();
        dropShip.setNavigator(movedNavigator);
        when(movedNavigator.getUnit()).thenReturn(otherShip);
        clearInvocations(movedNavigator);

        assertTrue(UnitCrewRepair.repair(dropShip));

        assertNull(dropShip.getNavigator());
        verify(movedNavigator, never()).setUnit(any());
    }

    @Test
    void crewWhoAllServeAboardAreKept() {
        Person mate = vesselCrew();
        Person navigator = mockPerson();
        dropShip.setNavigator(navigator);
        clearInvocations(dropShip);

        assertFalse(UnitCrewRepair.repair(dropShip));

        assertEquals(List.of(mate), dropShip.getVesselCrew());
        assertSame(navigator, dropShip.getNavigator());
        verify(dropShip, never()).resetPilotAndEntity();
    }

    /** Adds a vessel crew member who serves aboard the DropShip. */
    private Person vesselCrew() {
        Person person = mockPerson();
        dropShip.addVesselCrew(person);
        return person;
    }

    private Person mockPerson() {
        UUID id = UUID.randomUUID();
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(id);
        when(person.getUnit()).thenReturn(dropShip);
        when(campaign.getPlayerForce().getHumanResources().getPerson(eq(id))).thenReturn(person);
        return person;
    }
}
