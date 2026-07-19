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
package mekhq.campaign.digitalGM.stratCon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.digitalGM.stratCon.StratConBiomeManifest.MapTypeList;
import org.junit.jupiter.api.Test;

/**
 * Tests for battle-map pool resolution ({@link StratConBiomeManifest#getMapTypesForTerrain}): exact-name pools win,
 * pool-less terrains fall back to their category pool, and unknown terrain falls back to NEUTRAL.
 */
class StratConBiomeMapSelectionTest {
    private static final StratConBiomeManifest MANIFEST = StratConBiomeManifest.getInstance();

    @Test
    void exactTerrainPool_winsOverCategoryFallback() {
        // Forest has its own pool, so it must resolve to that, not to the VEGETATION category pool.
        MapTypeList forest = MANIFEST.getMapTypesForTerrain("Forest");
        assertSame(MANIFEST.getBiomeMapTypes().get("Forest"), forest, "Forest should resolve to its own pool");
        assertFalse(forest.mapTypes.isEmpty(), "Forest pool should not be empty");
    }

    @Test
    void farmland_fallsBackToAgriculturePool() {
        MapTypeList resolved = MANIFEST.getMapTypesForTerrain(StratConBiomeManifest.FARMLAND);
        assertSame(MANIFEST.getBiomeMapTypes().get("AGRICULTURE"), resolved, "Farmland should resolve via AGRICULTURE");
        assertNotNull(resolved);
        assertFalse(resolved.mapTypes.isEmpty(), "AGRICULTURE fallback pool should not be empty");
    }

    @Test
    void marsAndGrayLunar_resolveToTheirOwnDistinctPools() {
        MapTypeList mars = MANIFEST.getMapTypesForTerrain("Mars");
        MapTypeList grayLunar = MANIFEST.getMapTypesForTerrain("GrayLunar");

        assertSame(MANIFEST.getBiomeMapTypes().get("Mars"), mars, "Mars should resolve to its own pool");
        assertSame(MANIFEST.getBiomeMapTypes().get("GrayLunar"), grayLunar, "GrayLunar should resolve to its own pool");
        assertTrue(mars.mapTypes.contains("StratconMars"), "Mars should use the Mars mapgen theme");
        assertTrue(grayLunar.mapTypes.contains("StratconLunar"), "GrayLunar should use the lunar mapgen theme");
    }

    @Test
    void volcano_resolvesToItsOwnPool() {
        MapTypeList resolved = MANIFEST.getMapTypesForTerrain("Volcano");
        assertSame(MANIFEST.getBiomeMapTypes().get("Volcano"), resolved, "Volcano should resolve to its own pool");
        assertTrue(resolved.mapTypes.contains("StratconVolcanic"), "Volcano should use the volcanic mapgen theme");
    }

    @Test
    void unknownTerrain_fallsBackToNeutralPool() {
        MapTypeList resolved = MANIFEST.getMapTypesForTerrain("NoSuchTerrainXyz");
        assertSame(MANIFEST.getBiomeMapTypes().get("NEUTRAL"), resolved, "unknown terrain should resolve via NEUTRAL");
        assertNotNull(resolved, "a NEUTRAL fallback pool should be authored");
        assertFalse(resolved.mapTypes.isEmpty());
    }

    @Test
    void nullTerrain_resolvesToNeutralRatherThanCrashing() {
        // getTerrainCategory(null) is NEUTRAL, so a null terrain resolves to the NEUTRAL pool.
        assertSame(MANIFEST.getBiomeMapTypes().get("NEUTRAL"), MANIFEST.getMapTypesForTerrain(null));
    }

    @Test
    void everySpawnableCategoryHasANonEmptyFallbackPool() {
        for (StratConTerrainCategory category : StratConTerrainCategory.values()) {
            if (category == StratConTerrainCategory.OCEAN) {
                // Scenarios never spawn on water, so no OCEAN fallback pool is authored.
                assertNull(MANIFEST.getBiomeMapTypes().get(category.name()),
                      "OCEAN should have no fallback pool");
                continue;
            }
            MapTypeList pool = MANIFEST.getBiomeMapTypes().get(category.name());
            assertNotNull(pool, "missing fallback pool for category " + category);
            assertFalse(pool.mapTypes.isEmpty(), "empty fallback pool for category " + category);
        }
    }
}
