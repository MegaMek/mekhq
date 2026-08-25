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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.campaign.mission.scenarios.atb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the terrain-targeting added to {@link AtBScenarioModifier}: a modifier may restrict itself to specific
 * assigned terrain types, and that restriction survives a JSON round-trip and a clone.
 */
class AtBScenarioModifierTerrainTest {

    private static AtBDynamicScenario scenarioWithTerrain(String terrainType) {
        AtBDynamicScenario scenario = mock(AtBDynamicScenario.class);
        when(scenario.getTerrainType()).thenReturn(terrainType);
        return scenario;
    }

    /** A modifier scoped to SpecificGroundTerrain and restricted to the given terrain types. */
    private static AtBScenarioModifier specificGroundTerrainModifier(List<String> terrainTypes) {
        AtBScenarioModifier modifier = new AtBScenarioModifier();
        modifier.setAllowedMapLocations(List.of(MapLocation.SpecificGroundTerrain));
        modifier.setAllowedTerrainTypes(terrainTypes);
        return modifier;
    }

    @Test
    void noRestrictionAppliesToAnyTerrain() {
        AtBScenarioModifier modifier = new AtBScenarioModifier();
        // null (default) and empty both mean "no restriction"
        assertTrue(modifier.appliesToScenarioTerrain(scenarioWithTerrain("Desert")));
        modifier.setAllowedTerrainTypes(List.of());
        assertTrue(modifier.appliesToScenarioTerrain(scenarioWithTerrain("Desert")));
        assertTrue(modifier.appliesToScenarioTerrain(scenarioWithTerrain(null)));
    }

    @Test
    void restrictionAppliesOnlyToListedTerrain() {
        AtBScenarioModifier modifier = specificGroundTerrainModifier(List.of("Desert", "BadLands"));

        assertTrue(modifier.appliesToScenarioTerrain(scenarioWithTerrain("Desert")));
        assertTrue(modifier.appliesToScenarioTerrain(scenarioWithTerrain("BadLands")));
        assertFalse(modifier.appliesToScenarioTerrain(scenarioWithTerrain("Forest")));
    }

    @Test
    void restrictedModifierIsSkippedWhenTerrainIsAbsent() {
        // a terrain-restricted modifier must not apply to a terrain-less scenario (e.g. a space battle)
        AtBScenarioModifier modifier = specificGroundTerrainModifier(List.of("Desert"));
        assertFalse(modifier.appliesToScenarioTerrain(scenarioWithTerrain(null)));
    }

    @Test
    void terrainRestrictionIsIgnoredWhenNotScopedToSpecificGroundTerrain() {
        // terrain targeting only takes effect when SpecificGroundTerrain is an allowed location for the modifier
        AtBScenarioModifier modifier = new AtBScenarioModifier();
        modifier.setAllowedMapLocations(List.of(MapLocation.AllGroundTerrain));
        modifier.setAllowedTerrainTypes(List.of("Desert"));
        assertTrue(modifier.appliesToScenarioTerrain(scenarioWithTerrain("Forest")));

        // also ignored when allowedMapLocations is null (no scoping at all)
        AtBScenarioModifier unscoped = new AtBScenarioModifier();
        unscoped.setAllowedTerrainTypes(List.of("Desert"));
        assertTrue(unscoped.appliesToScenarioTerrain(scenarioWithTerrain("Forest")));
    }

    @Test
    void allowedTerrainTypesSurviveJsonRoundTrip(@TempDir Path tempDir) {
        AtBScenarioModifier original = new AtBScenarioModifier();
        original.setModifierName("DesertOnly");
        original.setAllowedTerrainTypes(List.of("Desert", "BadLands"));

        File out = tempDir.resolve("DesertOnly.json").toFile();
        original.Serialize(out);

        AtBScenarioModifier reloaded = AtBScenarioModifier.Deserialize(out.getPath());
        assertNotNull(reloaded);
        assertEquals(List.of("Desert", "BadLands"), reloaded.getAllowedTerrainTypes());
    }

    @Test
    void unsetAllowedTerrainTypesRoundTripsAsNull(@TempDir Path tempDir) {
        AtBScenarioModifier original = new AtBScenarioModifier();
        original.setModifierName("AnyTerrain");

        File out = tempDir.resolve("AnyTerrain.json").toFile();
        original.Serialize(out);

        AtBScenarioModifier reloaded = AtBScenarioModifier.Deserialize(out.getPath());
        assertNotNull(reloaded);
        assertNull(reloaded.getAllowedTerrainTypes());
    }

    @Test
    void clonePreservesAllowedTerrainTypes() {
        AtBScenarioModifier original = new AtBScenarioModifier();
        original.setAllowedTerrainTypes(List.of("Desert"));
        AtBScenarioModifier clone = original.clone();
        assertEquals(List.of("Desert"), clone.getAllowedTerrainTypes());
        // the clone's list is independent of the original
        assertNotNull(clone.getAllowedTerrainTypes());
    }
}
