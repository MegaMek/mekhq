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
package mekhq.campaign.stratCon;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConTerrainPlacer}
 */
class StratConTerrainPlacerTest {

    /**
     * Test that InitializeTrackTerrain handles extreme cold temperatures without throwing NPE. This regression test
     * covers issue #8712 where planets with temperature below -273C (below absolute zero in data) caused floorEntry()
     * to return null.
     */
    @Test
    void initializeTrackTerrain_withExtremeColdTemperature_doesNotThrowNPE() {
        // Create a track with temperature below absolute zero (-273C)
        // This simulates bad planet data that caused issue #8712
        StratConTrackState track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);
        track.setTemperature(-500); // Well below -273C (absolute zero)

        // This should not throw NPE - it should fall back to the coldest biome
        assertDoesNotThrow(() -> StratConTerrainPlacer.InitializeTrackTerrain(track));

        // Verify terrain was actually set
        assertNotNull(track.getTerrainTile(new StratConCoords(0, 0)));
    }

    /**
     * Test that InitializeTrackTerrain works with normal temperatures.
     */
    @Test
    void initializeTrackTerrain_withNormalTemperature_succeeds() {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);
        track.setTemperature(20); // Normal room temperature in Celsius

        assertDoesNotThrow(() -> StratConTerrainPlacer.InitializeTrackTerrain(track));
        assertNotNull(track.getTerrainTile(new StratConCoords(0, 0)));
    }

    /**
     * Test that InitializeTrackTerrain works at boundary temperature (0 Kelvin = -273C).
     */
    @Test
    void initializeTrackTerrain_atAbsoluteZero_succeeds() {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);
        track.setTemperature(-273); // Absolute zero in Celsius

        assertDoesNotThrow(() -> StratConTerrainPlacer.InitializeTrackTerrain(track));
        assertNotNull(track.getTerrainTile(new StratConCoords(0, 0)));
    }
}
