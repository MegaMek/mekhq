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
package mekhq.campaign.digitalGM.stratCon.biome;

import static mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest.terrainTemperatureOffset;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConBiomeManifest#terrainTemperatureOffset(String)}, the per-hex climate adjustment that sets
 * both the selected-hex readout and the real board temperature of every launched scenario.
 */
class StratConTerrainTemperatureOffsetTest {

    @Test
    void temperateTerrain_hasNoOffset() {
        for (String terrain : List.of("Plains", "Forest", "Hills", "Urban", "Sea", "Steppe")) {
            assertEquals(0,
                  terrainTemperatureOffset(terrain),
                  terrain + " is temperate and should not shift the board temperature");
        }
    }

    @Test
    void volcanicTerrain_bakes() {
        assertEquals(25,
              terrainTemperatureOffset("Volcano"),
              "volcanic ground should be a large positive offset");
        assertTrue(terrainTemperatureOffset("Volcano") > terrainTemperatureOffset("Desert"),
              "volcanic ground should be hotter than any merely hot terrain");
    }

    @Test
    void hotAndDryTerrain_warms() {
        assertEquals(8, terrainTemperatureOffset("Desert"), "desert should warm the board");
        assertEquals(8, terrainTemperatureOffset("Badlands"), "badlands should warm the board");
        assertEquals(8, terrainTemperatureOffset("HotUrban"), "a \"Hot\" prefixed terrain should warm the board");
    }

    @Test
    void mountainTerrain_coolsForElevation() {
        assertEquals(-6,
              terrainTemperatureOffset("Mountain"),
              "mountains should cool the board through elevation alone");
    }

    @Test
    void coldAndFrozenTerrain_chills() {
        for (String terrain : List.of("Glacier", "SnowField", "FrozenSea", "ArcticDesert", "Tundra")) {
            assertEquals(-8, terrainTemperatureOffset(terrain), terrain + " should chill the board");
        }
        assertEquals(-8, terrainTemperatureOffset("ColdSea"), "a \"Cold\" prefixed terrain should chill the board");
    }

    @Test
    void offsetsStack_coldMountainGetsBothAdjustments() {
        // The elevation and climate adjustments are additive rather than exclusive, so a cold mountain is genuinely
        // colder than either a plain mountain or plain cold ground. Scenario board temperature depends on this.
        assertEquals(-14,
              terrainTemperatureOffset("ColdMountain"),
              "a cold mountain should take both the -6 elevation and the -8 cold adjustment");
        assertTrue(terrainTemperatureOffset("ColdMountain") < terrainTemperatureOffset("Mountain"),
              "a cold mountain should be colder than a temperate mountain");
        assertTrue(terrainTemperatureOffset("ColdMountain") < terrainTemperatureOffset("ColdSea"),
              "a cold mountain should be colder than cold lowland");
    }

    @Test
    void offsetsStack_hotMountainIsNetWarmButTemperedByElevation() {
        assertEquals(2,
              terrainTemperatureOffset("HotMountainsDry"),
              "a hot mountain should take the +8 heat and the -6 elevation adjustment together");
        assertTrue(terrainTemperatureOffset("HotMountainsDry") < terrainTemperatureOffset("Desert"),
              "a hot mountain should be cooler than a hot lowland desert");
    }

    @Test
    void volcanicTerrain_shortCircuitsBeforeAnyOtherAdjustment() {
        // Volcanic ground returns its offset immediately, so it never picks up an elevation or climate adjustment on
        // top; the value below is the whole answer for every volcanic terrain.
        assertEquals(25,
              terrainTemperatureOffset("Volcano"),
              "volcanic terrain should return its own flat offset with nothing stacked on it");
    }

    @Test
    void unknownBlankAndNullTerrain_areNeutralAndDoNotThrow() {
        assertEquals(0,
              assertDoesNotThrow(() -> terrainTemperatureOffset(null)),
              "a null terrain must be neutral rather than throwing");
        assertEquals(0, terrainTemperatureOffset(""), "an empty terrain name must be neutral");
        assertEquals(0, terrainTemperatureOffset("   "), "a blank terrain name must be neutral");
        assertEquals(0,
              terrainTemperatureOffset("ThisTerrainDoesNotExist"),
              "an unrecognized terrain name must be neutral rather than guessing an offset");
    }
}
