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

package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.Collections;

import megamek.common.units.Dropship;
import mekhq.campaign.Campaign;
import mekhq.campaign.base.AbstractBase;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.actions.ActivateUnitAction;
import mekhq.campaign.unit.actions.MothballUnitAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A vessel tech is taken aboard a DropShip to mothball or activate it. A tech holding a support carrier seat gives it
 * up to come aboard, but only once the transfer is known to succeed, and a tech is never added to the crew twice.
 */
class UnitVesselTechBoardingTest {
    private Campaign campaign;
    private Dropship entity;
    private Unit unit;
    private Unit carrier;
    private Person tech;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        entity = mock(Dropship.class);
        when(entity.isLargeCraft()).thenReturn(true);
        unit = spy(new Unit(entity, campaign));
        doNothing().when(unit).resetPilotAndEntity();
        doReturn(true).when(unit).canTakeMoreVesselCrew();

        carrier = mock(Unit.class);
        when(carrier.isCarrier()).thenReturn(true);

        tech = mock(Person.class);
        when(tech.isTechLargeVessel()).thenReturn(true);
    }

    @Test
    void techInACarrierSeatGivesItUpToComeAboard() {
        when(tech.getUnit()).thenReturn(carrier);

        assertTrue(unit.takeAboardVesselTech(tech));

        verify(carrier).remove(tech, false);
        assertTrue(unit.getVesselCrew().contains(tech));
    }

    @Test
    void techAtAnotherBaseKeepsTheirCarrierSeat() {
        when(tech.getUnit()).thenReturn(carrier);
        placeAtAnotherBase(tech);

        assertFalse(unit.takeAboardVesselTech(tech));

        verify(carrier, never()).remove(tech, false);
        assertFalse(unit.getVesselCrew().contains(tech));
    }

    @Test
    void techCrewingAnotherVesselIsRefused() {
        Unit otherVessel = mock(Unit.class);
        when(tech.getUnit()).thenReturn(otherVessel);

        assertFalse(unit.takeAboardVesselTech(tech));

        verify(otherVessel, never()).remove(tech, false);
        assertFalse(unit.getVesselCrew().contains(tech));
    }

    @Test
    void activatingAnUncrewedVesselTakesTheTechAboardOnce() {
        unit.setMothballed(true);

        new ActivateUnitAction(tech, false).execute(campaign, unit);

        assertEquals(1, Collections.frequency(unit.getVesselCrew(), tech));
        verify(campaign).activate(unit);
    }

    @Test
    void activationIsNotStartedWhenTheTechCannotComeAboard() {
        when(tech.getUnit()).thenReturn(carrier);
        placeAtAnotherBase(tech);
        unit.setMothballed(true);

        new ActivateUnitAction(tech, false).execute(campaign, unit);

        verify(carrier, never()).remove(tech, false);
        verify(campaign, never()).activate(unit);
    }

    @Test
    void mothballingIsNotStartedWhenTheTechCannotComeAboard() {
        when(tech.getUnit()).thenReturn(carrier);
        placeAtAnotherBase(tech);

        new MothballUnitAction(tech, false).execute(campaign, unit);

        verify(carrier, never()).remove(tech, false);
        verify(unit, never()).startMothballing(tech);
    }

    /** Puts a person at a base of their own, away from the main force where the vessel is. */
    private static void placeAtAnotherBase(Person person) {
        when(person.getLocationNode()).thenReturn(new LocationNode(mock(AbstractBase.class)));
    }
}
