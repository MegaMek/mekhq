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

import static mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest.isBarrenTerrain;
import static mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest.isHillsTerrain;
import static mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest.isLunarTerrain;
import static mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest.isMountainTerrain;
import static mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest.isOceanTerrain;
import static mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest.isUrbanTerrain;
import static mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest.isVegetationTerrain;
import static mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest.isVolcanicTerrain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for the data-driven terrain classification on {@link StratConBiomeManifest}, backed by the authored
 * {@code terrainType} definitions in the biome manifest.
 */
class StratConBiomeClassificationTest {

    private static StratConTerrainCategory category(String terrainType) {
        return StratConBiomeManifest.getInstance().getTerrainCategory(terrainType);
    }

    @Test
    void oceanPredicate_matchesEveryClimateVariant() {
        assertTrue(isOceanTerrain("Sea"));
        assertTrue(isOceanTerrain("ColdSea"));
        assertTrue(isOceanTerrain("HotSea"));
        assertTrue(isOceanTerrain("FrozenSea"));
        assertFalse(isOceanTerrain("Plains"));
    }

    @Test
    void mountainPredicate_matchesEveryClimateVariant() {
        assertTrue(isMountainTerrain("Mountain"));
        assertTrue(isMountainTerrain("ColdMountain"));
        assertTrue(isMountainTerrain("HotMountainsWet"));
        assertTrue(isMountainTerrain("HotMountainsDry"));
        assertFalse(isMountainTerrain("Hills"));
    }

    @Test
    void urbanPredicate_matchesEveryClimateVariant() {
        assertTrue(isUrbanTerrain("Urban"));
        assertTrue(isUrbanTerrain("ColdUrban"));
        assertTrue(isUrbanTerrain("HotUrban"));
        assertFalse(isUrbanTerrain("Plains"));
    }

    @Test
    void vegetationPredicate_coversForestsAndPlains() {
        assertTrue(isVegetationTerrain("Forest"));
        assertTrue(isVegetationTerrain("Jungle"));
        assertTrue(isVegetationTerrain("Swamp"));
        assertTrue(isVegetationTerrain("Savannah"));
        assertTrue(isVegetationTerrain("Plains"));
        assertFalse(isVegetationTerrain("Desert"));
    }

    @Test
    void barrenPredicate_includesDrySteppesAndFrozenGround() {
        assertTrue(isBarrenTerrain("Desert"));
        assertTrue(isBarrenTerrain("Badlands"));
        assertTrue(isBarrenTerrain("Steppe"));
        assertTrue(isBarrenTerrain("ArcticDesert"));
        assertTrue(isBarrenTerrain("Glacier"));
        assertTrue(isBarrenTerrain("SnowField"));
        assertTrue(isBarrenTerrain("Tundra"));
        assertFalse(isBarrenTerrain("Forest"));
    }

    @Test
    void volcanicAndLunarPredicates_matchTheirSprites() {
        assertTrue(isVolcanicTerrain("Volcano"));
        assertTrue(isLunarTerrain("GrayLunar"));
        assertTrue(isLunarTerrain("Mars"));
        assertFalse(isVolcanicTerrain("GrayLunar"));
        assertFalse(isLunarTerrain("Volcano"));
    }

    @Test
    void hillsPredicate_coversEveryHillVariantAndIsNotVegetationOrBarren() {
        for (String hills : List.of("Hills", "ColdHills", "HotHillsWet", "HotHillsDry")) {
            assertTrue(isHillsTerrain(hills), hills + " should be hills");
            assertEquals(StratConTerrainCategory.HILLS, category(hills));
            assertFalse(isVegetationTerrain(hills));
            assertFalse(isBarrenTerrain(hills));
            assertFalse(isMountainTerrain(hills));
        }
    }

    @Test
    void unknownTerrain_isNeutral() {
        assertEquals(StratConTerrainCategory.NEUTRAL, category("ThisTerrainDoesNotExist"));
    }

    @Test
    void nullTerrain_isNeverClassified() {
        assertEquals(StratConTerrainCategory.NEUTRAL, category(null));
        assertFalse(isOceanTerrain(null));
        assertFalse(isMountainTerrain(null));
        assertFalse(isUrbanTerrain(null));
        assertFalse(isVolcanicTerrain(null));
        assertFalse(isLunarTerrain(null));
        assertFalse(isVegetationTerrain(null));
        assertFalse(isBarrenTerrain(null));
    }

    @Test
    void terrainCategory_readsAuthoredValueFromManifest() {
        assertEquals(StratConTerrainCategory.OCEAN, category("Sea"));
        assertEquals(StratConTerrainCategory.MOUNTAIN, category("Mountain"));
        assertEquals(StratConTerrainCategory.URBAN, category("Urban"));
        assertEquals(StratConTerrainCategory.VOLCANIC, category("Volcano"));
        assertEquals(StratConTerrainCategory.LUNAR, category("GrayLunar"));
        assertEquals(StratConTerrainCategory.VEGETATION, category("Forest"));
        assertEquals(StratConTerrainCategory.BARREN, category("Desert"));
    }
}
