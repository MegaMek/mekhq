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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.UUID;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.enums.CrewSeat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that a unit reports the crew seat each person holds, and can give a seat back.
 *
 * @author Illiani
 * @since 0.51.01
 */
class UnitCrewSeatTest {
    private Campaign campaign;
    private Unit unit;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        unit = spy(new Unit(mock(Entity.class), campaign));
        when(unit.getId()).thenReturn(UUID.randomUUID());
        doNothing().when(unit).resetPilotAndEntity();
    }

    private Person createPerson() {
        UUID personId = UUID.randomUUID();
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(personId);
        when(campaign.getPlayerForce().getHumanResources().getPerson(eq(personId))).thenReturn(person);
        return person;
    }

    @Test
    void driverHoldsDriverSeat() {
        Person driver = createPerson();
        unit.addDriver(driver);

        assertEquals(CrewSeat.DRIVER, unit.getCrewSeat(driver));
    }

    @Test
    void gunnerHoldsGunnerSeat() {
        Person gunner = createPerson();
        unit.addGunner(gunner);

        assertEquals(CrewSeat.GUNNER, unit.getCrewSeat(gunner));
    }

    @Test
    void driverAndGunnerHoldsPilotSeat() {
        Person pilot = createPerson();
        unit.addDriver(pilot);
        unit.addGunner(pilot);

        assertEquals(CrewSeat.PILOT_OR_SOLDIER, unit.getCrewSeat(pilot));
    }

    @Test
    void vesselCrewHoldsVesselCrewSeat() {
        Person crewMember = createPerson();
        unit.addVesselCrew(crewMember);

        assertEquals(CrewSeat.VESSEL_CREW, unit.getCrewSeat(crewMember));
    }

    @Test
    void navigatorHoldsNavigatorSeat() {
        Person navigator = createPerson();
        unit.setNavigator(navigator);

        assertEquals(CrewSeat.NAVIGATOR, unit.getCrewSeat(navigator));
    }

    @Test
    void techOfficerHoldsTechOfficerSeat() {
        Person techOfficer = createPerson();
        unit.setTechOfficer(techOfficer);

        assertEquals(CrewSeat.TECH_OFFICER, unit.getCrewSeat(techOfficer));
    }

    @Test
    void outsiderHoldsNoSeat() {
        Person driver = createPerson();
        unit.addDriver(driver);

        assertNull(unit.getCrewSeat(createPerson()));
        assertNull(unit.getCrewSeat(null));
    }

    @Test
    void removedCrewHoldsNoSeat() {
        Person driver = createPerson();
        unit.addDriver(driver);
        unit.remove(driver, false);

        assertNull(unit.getCrewSeat(driver));
    }

    @Test
    void addToCrewSeatUsesMatchingAssignment() {
        Person driver = createPerson();
        unit.addToCrewSeat(driver, CrewSeat.DRIVER);
        verify(unit).addDriver(driver);

        Person gunner = createPerson();
        unit.addToCrewSeat(gunner, CrewSeat.GUNNER);
        verify(unit).addGunner(gunner);

        Person crewMember = createPerson();
        unit.addToCrewSeat(crewMember, CrewSeat.VESSEL_CREW);
        verify(unit).addVesselCrew(crewMember);

        Person navigator = createPerson();
        unit.addToCrewSeat(navigator, CrewSeat.NAVIGATOR);
        verify(unit).setNavigator(navigator);

        Person techOfficer = createPerson();
        unit.addToCrewSeat(techOfficer, CrewSeat.TECH_OFFICER);
        verify(unit).setTechOfficer(techOfficer);
    }

    @Test
    void openSeatChecksMatchingCapacity() {
        doReturn(true).when(unit).canTakeMoreDrivers();
        doReturn(false).when(unit).canTakeMoreGunners();
        doReturn(true).when(unit).canTakeMoreVesselCrew();
        doReturn(false).when(unit).canTakeNavigator();
        doReturn(true).when(unit).canTakeTechOfficer();

        assertTrue(unit.hasOpenCrewSeat(CrewSeat.DRIVER));
        assertFalse(unit.hasOpenCrewSeat(CrewSeat.GUNNER));
        assertTrue(unit.hasOpenCrewSeat(CrewSeat.VESSEL_CREW));
        assertFalse(unit.hasOpenCrewSeat(CrewSeat.NAVIGATOR));
        assertTrue(unit.hasOpenCrewSeat(CrewSeat.TECH_OFFICER));
    }

    @Test
    void openPilotSeatUsesGunnersForSoldiers() {
        doReturn(true).when(unit).canTakeMoreDrivers();
        doReturn(false).when(unit).canTakeMoreGunners();

        doReturn(false).when(unit).usesSoldiers();
        assertTrue(unit.hasOpenCrewSeat(CrewSeat.PILOT_OR_SOLDIER));

        doReturn(true).when(unit).usesSoldiers();
        assertFalse(unit.hasOpenCrewSeat(CrewSeat.PILOT_OR_SOLDIER));
    }
}
