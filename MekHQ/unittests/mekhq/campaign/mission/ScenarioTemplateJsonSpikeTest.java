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
package mekhq.campaign.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import org.junit.jupiter.api.Test;

/**
 * Phase 3.0 spike: validates the {@link ScenarioTemplateJson} field-based mapper on {@link ScenarioMapParameters}, a
 * class that exercises int fields, boolean {@code is}-getters, an enum, and a list. Confirms a clean round-trip and a
 * clean JSON shape (no duplicate keys from the {@code getAllowedTerrainType()} / {@code allowedTerrainTypes}
 * mismatch).
 */
class ScenarioTemplateJsonSpikeTest {

    private static ScenarioMapParameters buildMaximalMapParameters() {
        ScenarioMapParameters mapParameters = new ScenarioMapParameters();
        mapParameters.setBaseWidth(35);
        mapParameters.setBaseHeight(45);
        mapParameters.setWidthScalingIncrement(6);
        mapParameters.setHeightScalingIncrement(7);
        mapParameters.setAdditionalMapSheetWide(2);
        mapParameters.setAdditionalMapSheetTall(3);
        mapParameters.setAllowRotation(true);
        mapParameters.setUseStandardAtBSizing(true);
        mapParameters.setMapLocation(MapLocation.SpecificGroundTerrain);
        mapParameters.getAllowedTerrainType().add("Woods");
        mapParameters.getAllowedTerrainType().add("Rough");
        return mapParameters;
    }

    @Test
    void mapParametersRoundTripsThroughJson() throws Exception {
        ScenarioMapParameters original = buildMaximalMapParameters();

        String json = ScenarioTemplateJson.toJson(original);
        ScenarioMapParameters reloaded = ScenarioTemplateJson.fromJson(json, ScenarioMapParameters.class);

        assertEquals(original.getBaseWidth(), reloaded.getBaseWidth());
        assertEquals(original.getBaseHeight(), reloaded.getBaseHeight());
        assertEquals(original.getWidthScalingIncrement(), reloaded.getWidthScalingIncrement());
        assertEquals(original.getHeightScalingIncrement(), reloaded.getHeightScalingIncrement());
        assertEquals(original.getAdditionalMapSheetWide(), reloaded.getAdditionalMapSheetWide());
        assertEquals(original.getAdditionalMapSheetTall(), reloaded.getAdditionalMapSheetTall());
        assertEquals(original.isAllowRotation(), reloaded.isAllowRotation());
        assertEquals(original.isUseStandardAtBSizing(), reloaded.isUseStandardAtBSizing());
        assertEquals(original.getMapLocation(), reloaded.getMapLocation());
        assertEquals(original.getAllowedTerrainType(), reloaded.getAllowedTerrainType());
    }

    @Test
    void jsonShapeUsesFieldNamesWithoutDuplicateTerrainKey() throws Exception {
        String json = ScenarioTemplateJson.toJson(buildMaximalMapParameters());

        // Enum serialized by name.
        assertTrue(json.contains("\"mapLocation\" : \"SpecificGroundTerrain\""), () -> json);
        // The field name is used, and the singular getter-derived "allowedTerrainType" key must NOT appear.
        assertTrue(json.contains("\"allowedTerrainTypes\""), () -> json);
        assertFalse(json.contains("\"allowedTerrainType\" :"), () -> json);
    }
}
