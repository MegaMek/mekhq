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
package mekhq.campaign.utilities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.SpaceStation;
import megamek.common.units.Warship;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.utilities.TransportCostCalculations;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LithiumFusionBatteries}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class LithiumFusionBatteriesTest {
    private TransportCostCalculations transportCalculations;

    @BeforeEach
    void setUp() {
        transportCalculations = mock(TransportCostCalculations.class);
        when(transportCalculations.getJumpShipsRequired()).thenReturn(0.0);
    }

    private static Jumpship jumpship(boolean hasBattery, boolean isBatteryHit, boolean canJump) {
        Jumpship jumpship = mock(Jumpship.class);
        stubDrive(jumpship, hasBattery, isBatteryHit, canJump);
        return jumpship;
    }

    private static void stubDrive(Jumpship jumpship, boolean hasBattery, boolean isBatteryHit, boolean canJump) {
        when(jumpship.hasLF()).thenReturn(hasBattery);
        when(jumpship.getLFBatteryHit()).thenReturn(isBatteryHit);
        when(jumpship.canJump()).thenReturn(canJump);
    }

    private static Jumpship workingLithiumFusionShip() {
        return jumpship(true, false, true);
    }

    private static Unit unit(Entity entity) {
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        return unit;
    }

    @Test
    void noJumpingShips_isNotEligible() {
        Unit mek = unit(mock(Entity.class));
        assertFalse(LithiumFusionBatteries.isFleetEligible(List.of(mek), transportCalculations));
    }

    @Test
    void emptyHangar_isNotEligible() {
        assertFalse(LithiumFusionBatteries.isFleetEligible(List.of(), transportCalculations));
    }

    @Test
    void everyShipHasWorkingBattery_isEligible() {
        List<Unit> units = List.of(unit(workingLithiumFusionShip()), unit(workingLithiumFusionShip()),
              unit(mock(Entity.class)));
        assertTrue(LithiumFusionBatteries.isFleetEligible(units, transportCalculations));
    }

    @Test
    void warshipWithWorkingBattery_isEligible() {
        Warship warship = mock(Warship.class);
        stubDrive(warship, true, false, true);
        assertTrue(LithiumFusionBatteries.isFleetEligible(List.of(unit(warship)), transportCalculations));
    }

    @Test
    void oneShipWithoutBattery_isNotEligible() {
        List<Unit> units = List.of(unit(workingLithiumFusionShip()), unit(jumpship(false, false, true)));
        assertFalse(LithiumFusionBatteries.isFleetEligible(units, transportCalculations));
    }

    @Test
    void damagedBattery_isNotEligible() {
        List<Unit> units = List.of(unit(jumpship(true, true, true)));
        assertFalse(LithiumFusionBatteries.isFleetEligible(units, transportCalculations));
    }

    @Test
    void shipThatCannotJump_isNotEligible() {
        List<Unit> units = List.of(unit(jumpship(true, false, false)));
        assertFalse(LithiumFusionBatteries.isFleetEligible(units, transportCalculations));
    }

    @Test
    void hiredJumpShipsNeeded_isNotEligible() {
        when(transportCalculations.getJumpShipsRequired()).thenReturn(1.0);
        List<Unit> units = List.of(unit(workingLithiumFusionShip()));
        assertFalse(LithiumFusionBatteries.isFleetEligible(units, transportCalculations));
    }

    @Test
    void mothballedShipWithoutBattery_isIgnored() {
        Unit mothballed = unit(jumpship(false, false, true));
        when(mothballed.isMothballed()).thenReturn(true);
        List<Unit> units = List.of(unit(workingLithiumFusionShip()), mothballed);
        assertTrue(LithiumFusionBatteries.isFleetEligible(units, transportCalculations));
    }

    @Test
    void salvageShipWithoutBattery_isIgnored() {
        Unit salvage = unit(jumpship(false, false, true));
        when(salvage.isSalvage()).thenReturn(true);
        List<Unit> units = List.of(unit(workingLithiumFusionShip()), salvage);
        assertTrue(LithiumFusionBatteries.isFleetEligible(units, transportCalculations));
    }

    @Test
    void onlyMothballedShips_isNotEligible() {
        Unit mothballed = unit(workingLithiumFusionShip());
        when(mothballed.isMothballed()).thenReturn(true);
        assertFalse(LithiumFusionBatteries.isFleetEligible(List.of(mothballed), transportCalculations));
    }

    @Test
    void carriedStation_isIgnored() {
        SpaceStation adaptorStation = mock(SpaceStation.class);
        when(adaptorStation.hasKFAdapter()).thenReturn(true);
        SpaceStation modularStation = mock(SpaceStation.class);
        when(modularStation.isModular()).thenReturn(true);

        List<Unit> units = List.of(unit(workingLithiumFusionShip()), unit(adaptorStation), unit(modularStation));
        assertTrue(LithiumFusionBatteries.isFleetEligible(units, transportCalculations));
    }

    @Test
    void selfJumpingStationWithoutBattery_isNotEligible() {
        SpaceStation station = mock(SpaceStation.class);
        stubDrive(station, false, false, true);

        List<Unit> units = List.of(unit(workingLithiumFusionShip()), unit(station));
        assertFalse(LithiumFusionBatteries.isFleetEligible(units, transportCalculations));
    }

    @Test
    void transportCalculations_runWhenNotYetCalculated() {
        when(transportCalculations.getTotalCost()).thenReturn(null);
        LithiumFusionBatteries.isFleetEligible(List.of(unit(workingLithiumFusionShip())), transportCalculations);
        verify(transportCalculations).calculateJumpCostForEachDay();
    }

    @Test
    void transportCalculations_notRerunWhenAlreadyCalculated() {
        when(transportCalculations.getTotalCost()).thenReturn(Money.zero());
        LithiumFusionBatteries.isFleetEligible(List.of(unit(workingLithiumFusionShip())), transportCalculations);
        verify(transportCalculations, never()).calculateJumpCostForEachDay();
    }

    @Test
    void transportCalculations_skippedWhenAShipDisqualifies() {
        LithiumFusionBatteries.isFleetEligible(List.of(unit(jumpship(false, false, true))), transportCalculations);
        verify(transportCalculations, never()).calculateJumpCostForEachDay();
    }
}
