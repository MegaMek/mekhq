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
package mekhq.gui.scenarioTemplateEditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mekhq.campaign.mission.scenarios.ScenarioMapParameters;
import mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation;
import org.junit.jupiter.api.Test;

/**
 * Phase 4.1 panel test for {@link MapParametersPanel}. Confirms the load/writeInto round-trip (headless-safe, since the
 * panel takes terrain types as a parameter instead of reading the biome manifest) and the validation path.
 */
class MapParametersPanelTest {

    private static final List<String> TERRAIN = List.of("Woods", "Rough", "Urban", "ColdUrban");

    @Test
    void loadThenWriteIntoRoundTripsMapParameters() {
        ScenarioMapParameters source = new ScenarioMapParameters();
        source.setBaseWidth(35);
        source.setBaseHeight(45);
        source.setWidthScalingIncrement(6);
        source.setHeightScalingIncrement(7);
        source.setAdditionalMapSheetWide(2);
        source.setAdditionalMapSheetTall(3);
        source.setAllowRotation(true);
        source.setUseStandardAtBSizing(true);
        source.setMapLocation(MapLocation.SpecificGroundTerrain);
        source.getAllowedTerrainType().add("Woods");
        source.getAllowedTerrainType().add("Urban");

        MapParametersPanel panel = new MapParametersPanel(TERRAIN);
        panel.load(source);

        ScenarioMapParameters target = new ScenarioMapParameters();
        panel.writeInto(target);

        assertEquals(35, target.getBaseWidth());
        assertEquals(45, target.getBaseHeight());
        assertEquals(6, target.getWidthScalingIncrement());
        assertEquals(7, target.getHeightScalingIncrement());
        assertEquals(2, target.getAdditionalMapSheetWide());
        assertEquals(3, target.getAdditionalMapSheetTall());
        assertTrue(target.isAllowRotation());
        assertTrue(target.isUseStandardAtBSizing());
        assertEquals(MapLocation.SpecificGroundTerrain, target.getMapLocation());
        assertEquals(List.of("Woods", "Urban"), target.getAllowedTerrainType());
    }

    @Test
    void validateInputIsEmptyForLoadedDefaults() {
        ScenarioMapParameters source = new ScenarioMapParameters();
        MapParametersPanel panel = new MapParametersPanel(TERRAIN);
        panel.load(source);
        assertTrue(panel.validateInput().isEmpty(),
              "Default map parameters should validate cleanly (rejection logic is covered by MapDimensionInputTest)");
    }
}
