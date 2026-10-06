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
package mekhq.campaign.digitalGM.stratCon.sectorGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.loaders.MapSettings;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.mission.scenarios.AtBScenario;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConMapTuner}: terrain emphasis, tileset theme, and the road / water / urban overlays derived
 * from a scenario's StratCon hex context.
 */
class StratConMapTunerTest {

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    /** A scenario stub whose hex carries no road, no water, and no city; tests override the parts they exercise. */
    private static AtBScenario scenario(String terrain) {
        AtBScenario scenario = mock(AtBScenario.class);
        when(scenario.getTerrainType()).thenReturn(terrain);
        when(scenario.getStratConRoadEntryEdges()).thenReturn(List.of());
        when(scenario.isStratConWaterAdjacent()).thenReturn(false);
        when(scenario.isStratConUrban()).thenReturn(false);
        when(scenario.getStratConUrbanization()).thenReturn(0.0);
        return scenario;
    }

    @Test
    void barrenTerrain_emphasizesRoughAndSetsDesertTheme() {
        MapSettings mapSettings = MapSettings.getInstance();
        StratConMapTuner.tune(mapSettings, scenario("Desert"));

        assertTrue(mapSettings.getMinRoughSpots() >= 4, "barren terrain should get plenty of rough");
        assertEquals("desert", mapSettings.getTheme());
    }

    @Test
    void vegetationTerrain_emphasizesForest() {
        MapSettings mapSettings = MapSettings.getInstance();
        StratConMapTuner.tune(mapSettings, scenario("Forest"));

        assertTrue(mapSettings.getMinForestSpots() >= 4, "forest terrain should get plenty of woods");
    }

    @Test
    void coldMountain_setsSnowThemeAndSnowCappedPeaks() {
        MapSettings mapSettings = MapSettings.getInstance();
        StratConMapTuner.tune(mapSettings, scenario("ColdMountain"));

        assertEquals("snow", mapSettings.getTheme());
        assertEquals(MapSettings.MOUNTAIN_SNOW_CAPPED, mapSettings.getMountainStyle());
        assertTrue(mapSettings.getMountainPeaks() >= 1, "a mountain hex should raise at least one peak");
    }

    @Test
    void farmland_emphasizesPlantedFields() {
        MapSettings mapSettings = MapSettings.getInstance();
        StratConMapTuner.tune(mapSettings, scenario(StratConBiomeManifest.FARMLAND));

        assertTrue(mapSettings.getMinPlantedFieldSpots() >= 6, "farmland should get plenty of planted fields");
    }

    @Test
    void marsAndGrayLunar_setDistinctTilesetThemes() {
        MapSettings mars = MapSettings.getInstance();
        StratConMapTuner.tune(mars, scenario("Mars"));
        assertEquals("mars", mars.getTheme());

        MapSettings lunar = MapSettings.getInstance();
        StratConMapTuner.tune(lunar, scenario("GrayLunar"));
        assertEquals("lunar", lunar.getTheme());
    }

    @Test
    void roadHex_forcesRoadGeneration() {
        MapSettings mapSettings = MapSettings.getInstance();
        AtBScenario scenario = scenario("Plains");
        when(scenario.getStratConRoadEntryEdges()).thenReturn(List.of(0, 3));

        StratConMapTuner.tune(mapSettings, scenario);

        assertEquals(100, mapSettings.getProbRoad());
    }

    @Test
    void waterAdjacentHex_guaranteesWater() {
        MapSettings mapSettings = MapSettings.getInstance();
        mapSettings.setWaterParams(0, 0, 5, 10, 33); // a dry theme
        AtBScenario scenario = scenario("Plains");
        when(scenario.isStratConWaterAdjacent()).thenReturn(true);

        StratConMapTuner.tune(mapSettings, scenario);

        assertTrue(mapSettings.getMinWaterSpots() >= 1, "a water-adjacent hex should guarantee at least one lake");
    }

    @Test
    void denseCityHex_laysADenseCityOverAnyTerrain() {
        MapSettings mapSettings = MapSettings.getInstance();
        AtBScenario scenario = scenario("Mountain"); // a city in the mountains
        when(scenario.isStratConUrban()).thenReturn(true);
        when(scenario.getStratConUrbanization()).thenReturn(0.9);

        StratConMapTuner.tune(mapSettings, scenario);

        assertEquals("METRO", mapSettings.getCityType());
        assertTrue(mapSettings.getCityBlocks() > 6, "a dense city should have many blocks");
        assertTrue(mapSettings.getMinPavementSpots() >= 1, "a city should pave some ground");
    }

    @Test
    void sparseCityHex_laysATown() {
        MapSettings mapSettings = MapSettings.getInstance();
        AtBScenario scenario = scenario("Plains");
        when(scenario.isStratConUrban()).thenReturn(true);
        when(scenario.getStratConUrbanization()).thenReturn(0.1);

        StratConMapTuner.tune(mapSettings, scenario);

        assertEquals("TOWN", mapSettings.getCityType());
    }

    @Test
    void facilityTerrain_isTunedAsTheGroundItStandsOn() {
        // A facility scenario's terrain type is a map-pool key, not a terrain. Left unresolved it matched no category
        // and no theme, so a base on a volcano drew volcanic boards on a generic tileset.
        MapSettings mapSettings = MapSettings.getInstance();
        StratConMapTuner.tune(mapSettings, scenario("VolcanoFacility"));

        assertEquals("volcano", mapSettings.getTheme(), "a facility on a volcano should use the volcanic tileset");
    }

    @Test
    void facilityTerrain_resolvesColdGroundConsistently() {
        // Previously only keys literally starting with "Cold" reached the snow branch, by accident of a prefix test -
        // so ColdForestFacility got snow while GlacierFacility did not.
        for (String terrain : List.of("GlacierFacility", "TundraFacility", "SnowFieldFacility", "ColdForestFacility")) {
            MapSettings mapSettings = MapSettings.getInstance();
            StratConMapTuner.tune(mapSettings, scenario(terrain));

            assertEquals("snow", mapSettings.getTheme(), terrain + " should use the snow tileset");
        }
    }

    @Test
    void genericFacilityKeys_pickUpNoSpuriousTheme() {
        // "Temperate" is not a terrain, so there is nothing truer to resolve TemperateFacility to. It must not throw,
        // and must not be mistaken for volcanic or cold ground.
        MapSettings mapSettings = MapSettings.getInstance();
        String before = mapSettings.getTheme();

        StratConMapTuner.tune(mapSettings, scenario("TemperateFacility"));

        assertEquals(before, mapSettings.getTheme(), "a generic facility key should leave the theme alone");
    }
}
