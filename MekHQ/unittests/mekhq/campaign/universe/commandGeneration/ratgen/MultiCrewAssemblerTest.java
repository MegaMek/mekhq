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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.Test;

/**
 * Covers how many named people the assembler puts in a unit's seats when a role is filled from the temporary
 * crew pool.
 */
class MultiCrewAssemblerTest {

    @Test
    void withoutTemporaryCrewEverySeatIsNamed() {
        assertEquals(6, MultiCrewAssembler.namedSeats(false, PersonnelRole.VESSEL_GUNNER, 6, true));
        assertEquals(4, MultiCrewAssembler.namedSeats(false, PersonnelRole.VEHICLE_CREW_GROUND, 4, false));
    }

    @Test
    void aVehicleKeepsOneNamedPersonForTheWholeUnit() {
        assertEquals(1, MultiCrewAssembler.namedSeats(true, PersonnelRole.VEHICLE_CREW_GROUND, 2, false));
        assertEquals(0, MultiCrewAssembler.namedSeats(true, PersonnelRole.VEHICLE_CREW_GROUND, 3, true),
              "a later role on a unit that already has its named person adds none");
    }

    @Test
    void aShipKeepsOneNamedPersonInEveryVesselRole() {
        assertEquals(1, MultiCrewAssembler.namedSeats(true, PersonnelRole.VESSEL_PILOT, 3, false));
        assertEquals(1, MultiCrewAssembler.namedSeats(true, PersonnelRole.VESSEL_GUNNER, 6, true),
              "temporary gunners only count once a named gunner is aboard");
        assertEquals(1, MultiCrewAssembler.namedSeats(true, PersonnelRole.VESSEL_CREW, 17, true),
              "the engineer who maintains the ship comes from its named vessel crew");
    }

    @Test
    void noSeatsMeansNoOne() {
        assertEquals(0, MultiCrewAssembler.namedSeats(true, PersonnelRole.VESSEL_GUNNER, 0, false));
    }
}
