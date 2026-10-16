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

import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Entity;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;
import testUtilities.parts.UnitFixture;

/**
 * Aerospace units come back from a battle with full tanks (issue #840). Before, only the tank size was reset and the
 * fuel burned in the battle stayed burned.
 */
class AerospaceRefuelingTest {
    private static final int FUEL_BURNED = 50;

    @Test
    void fuelBurnedInBattleIsPutBack() {
        Entity designEntity = UnitFixture.SHILONE_SL_17.loadEntity();
        AeroSpaceFighter fighter = (AeroSpaceFighter) UnitFixture.SHILONE_SL_17.loadEntity();
        int fullFuel = fighter.getFuel();
        fighter.useFuel(FUEL_BURNED);

        AerospaceRefueling.refuel(fighter, designEntity);

        assertEquals(fullFuel, fighter.getCurrentFuel());
        assertEquals(fullFuel, fighter.getFuel());
    }

    @Test
    void unitWithoutAUnitFileKeepsItsOwnCapacity() {
        AeroSpaceFighter fighter = (AeroSpaceFighter) UnitFixture.SHILONE_SL_17.loadEntity();
        int fullFuel = fighter.getFuel();
        fighter.useFuel(FUEL_BURNED);

        AerospaceRefueling.refuel(fighter, null);

        assertEquals(fullFuel, fighter.getCurrentFuel(), "A custom design not in the unit cache is refuelled too");
    }

    @Test
    void tanksSavedHalfEmptyAreRefilledToTheDesignCapacity() {
        Entity designEntity = UnitFixture.SHILONE_SL_17.loadEntity();
        AeroSpaceFighter fighter = (AeroSpaceFighter) UnitFixture.SHILONE_SL_17.loadEntity();
        int fullFuel = fighter.getFuel();
        // Loading a save sets both the capacity and the fuel left to the fuel the save holds
        fighter.setFuel(fullFuel - FUEL_BURNED);

        AerospaceRefueling.refuel(fighter, designEntity);

        assertEquals(fullFuel, fighter.getCurrentFuel());
    }

    @Test
    void landAirMekInMekModeIsRefuelled() {
        LandAirMek landAirMek = new LandAirMek(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD, LandAirMek.LAM_STANDARD);
        landAirMek.setFuel(80);
        landAirMek.setCurrentFuel(20);
        landAirMek.setConversionMode(LandAirMek.CONV_MODE_MEK);
        assertFalse(landAirMek.isAero(), "Precondition: a LAM in Mek mode does not count as aerospace");

        AerospaceRefueling.refuel(landAirMek, null);

        assertEquals(80, landAirMek.getCurrentFuel());
    }
}
