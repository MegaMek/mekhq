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
package mekhq.campaign.unit.crewAssignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.units.Dropship;
import megamek.common.units.Infantry;
import megamek.common.units.Jumpship;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.Test;

class CrewSeatLayoutTest {
    private final CrewFixture fixture = new CrewFixture();

    private static List<SeatType> types(List<CrewSeat> seats) {
        return seats.stream().map(CrewSeat::type).toList();
    }

    @Test
    void soloPilotMekHasOnlyAPilotSeat() {
        List<CrewSeat> seats = CrewSeatLayout.forUnit(fixture.soloPilotUnit(mock(Mek.class), "Mek").unit);

        assertEquals(List.of(SeatType.PILOT), types(seats));
        assertEquals(1, seats.get(0).capacity());
    }

    @Test
    void multiCrewMekHasAPilotAndAGunner() {
        CrewFixture.CrewUnit tripod = fixture.vehicle(mock(Mek.class), "Tripod", 1);

        assertEquals(List.of(SeatType.PILOT, SeatType.GUNNER), types(CrewSeatLayout.forUnit(tripod.unit)));
    }

    @Test
    void vehicleHasDriverAndGunnersAndACommandConsoleSeat() {
        CrewFixture.CrewUnit vehicle = fixture.vehicle(mock(Tank.class), "Tank", 3);
        when(vehicle.unit.hasTechOfficerSeat()).thenReturn(true);

        List<CrewSeat> seats = CrewSeatLayout.forUnit(vehicle.unit);

        assertEquals(List.of(SeatType.DRIVER, SeatType.GUNNER, SeatType.CONSOLE_COMMANDER), types(seats));
        assertEquals(3, seats.get(1).capacity());
    }

    @Test
    void jumpShipHasPilotsGunnersCrewAndANavigator() {
        CrewFixture.CrewUnit jumpShip = fixture.vessel(mock(Jumpship.class), "JumpShip", 2, 6, 20);
        when(jumpShip.unit.hasNavigatorSeat()).thenReturn(true);

        assertEquals(List.of(SeatType.DRIVER, SeatType.GUNNER, SeatType.CREW, SeatType.NAVIGATOR),
              types(CrewSeatLayout.forUnit(jumpShip.unit)));
    }

    @Test
    void vesselReportsItsTemporaryCrewPerSeat() {
        CrewFixture.CrewUnit dropShip = fixture.vessel(mock(Dropship.class), "DropShip", 2, 6, 20);
        when(dropShip.unit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_GUNNER)).thenReturn(4);

        CrewSeat gunners = CrewSeatLayout.seatOf(dropShip.unit, SeatType.GUNNER);

        assertEquals(4, gunners.tempCrew());
        // Temporary crew never stop a real person taking a seat
        assertEquals(6, gunners.openSeats());
    }

    @Test
    void infantryHasOneSoldierSeatCountingItsSoldiers() {
        Person soldier = fixture.person("Soldier", PersonnelRole.SOLDIER);
        CrewFixture.CrewUnit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 28)
                                             .with(soldier, SeatType.SOLDIER);

        List<CrewSeat> seats = CrewSeatLayout.forUnit(platoon.unit);

        assertEquals(List.of(SeatType.SOLDIER), types(seats));
        assertEquals(List.of(soldier), seats.get(0).occupants());
        assertEquals(27, seats.get(0).openSeats());
    }

    @Test
    void uncrewedUnitHasNoSeats() {
        CrewFixture.CrewUnit trailer = fixture.vehicle(mock(Tank.class), "Trailer", 0);
        when(trailer.unit.isNotCrewedEntityType()).thenReturn(true);

        assertTrue(CrewSeatLayout.forUnit(trailer.unit).isEmpty());
    }
}
