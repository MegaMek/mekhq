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
package mekhq.campaign.utilities;

import static megamek.common.units.Jumpship.DRIVE_CORE_NONE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.SpaceStation;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

class JumpBlockersTest {

    @Test
    void areAllUnitsJumpCapable_returnsTrue_whenNoJumpShipsPresent() {
        Campaign campaign = mock(Campaign.class);
        Unit nonJumpUnit = mock(Unit.class);
        when(nonJumpUnit.getEntity()).thenReturn(mock(Entity.class));
        when(campaign.getUnits()).thenReturn(List.of(nonJumpUnit));

        assertTrue(JumpBlockers.areAllUnitsJumpCapable(campaign));
    }

    @Test
    void areAllUnitsJumpCapable_returnsTrue_whenAllJumpShipsAreCapable() {
        Campaign campaign = mock(Campaign.class);

        Unit okJumpShipUnit = mock(Unit.class);
        Jumpship okJumpShip = mock(Jumpship.class);
        when(okJumpShip.getDriveCoreType()).thenReturn(123); // anything but DRIVE_CORE_NONE
        when(okJumpShip.canJump()).thenReturn(true);
        when(okJumpShipUnit.getEntity()).thenReturn(okJumpShip);

        when(campaign.getUnits()).thenReturn(List.of(okJumpShipUnit));

        assertTrue(JumpBlockers.areAllUnitsJumpCapable(campaign));
    }

    @Test
    void areAllUnitsJumpCapable_doesNotTreatSpaceStationsWithKfAdapterAsBlockers() {
        Campaign campaign = mock(Campaign.class);

        Unit stationUnit = mock(Unit.class);
        SpaceStation station = mock(SpaceStation.class);
        when(station.hasKFAdapter()).thenReturn(true);
        when(stationUnit.getEntity()).thenReturn(station);

        // Even if these would otherwise block, the code continues early for KF adapter stations
        when(station.getDriveCoreType()).thenReturn(DRIVE_CORE_NONE);
        when(station.canJump()).thenReturn(false);

        when(campaign.getUnits()).thenReturn(List.of(stationUnit));

        assertTrue(JumpBlockers.areAllUnitsJumpCapable(campaign));
    }

    @Test
    void areAllUnitsJumpCapable_doesNotTreatModularSpaceStationsAsBlockers() {
        Campaign campaign = mock(Campaign.class);

        Unit stationUnit = mock(Unit.class);
        SpaceStation station = mock(SpaceStation.class);
        when(station.hasKFAdapter()).thenReturn(false);
        when(station.isModular()).thenReturn(true);
        when(stationUnit.getEntity()).thenReturn(station);

        // Would block if checked, but modular stations are skipped
        when(station.getDriveCoreType()).thenReturn(DRIVE_CORE_NONE);
        when(station.canJump()).thenReturn(false);

        when(campaign.getUnits()).thenReturn(List.of(stationUnit));

        assertTrue(JumpBlockers.areAllUnitsJumpCapable(campaign));
    }
}
