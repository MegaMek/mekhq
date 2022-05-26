/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission;

import megamek.common.Board;
import megamek.common.UnitType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests relevant to the AtBDynamicScenarioFactory
 * @author NickAragua
 */
public class DynamicScenarioFactoryTest {
    @Test
    public void testGetOppositeEdge() {
        int startingEdge = Board.START_EDGE;
        assertEquals(Board.START_CENTER, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));
        
        startingEdge = Board.START_CENTER;
        assertEquals(Board.START_EDGE, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));
        
        startingEdge = Board.START_ANY;
        assertEquals(Board.START_ANY, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));

        startingEdge = Board.START_N;
        assertEquals(Board.START_S, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));

        startingEdge = Board.START_E;
        assertEquals(Board.START_W, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));

        startingEdge = Board.START_S;
        assertEquals(Board.START_N, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));

        startingEdge = Board.START_W;
        assertEquals(Board.START_E, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));

        startingEdge = Board.START_NW;
        assertEquals(Board.START_SE, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));
    }

    @Test
    public void testAeroLanceSize() {
        assertEquals(2, AtBDynamicScenarioFactory.getAeroLanceSize(UnitType.AERO, true, "FC"));
        assertEquals(3, AtBDynamicScenarioFactory.getAeroLanceSize(UnitType.AERO, true, "CC"));
        assertEquals(2,
                AtBDynamicScenarioFactory.getAeroLanceSize(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX, false, "FC"));
        assertEquals(3,
                AtBDynamicScenarioFactory.getAeroLanceSize(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX, false, "CC"));

        // the number of conv fighters is randomly generated, but should be between 2 and 6, inclusively
        // we run it a bunch of times, all but guaranteeing that we hit the extremes
        for (int x = 0; x < 40; x++) {
            int numConvFighters = AtBDynamicScenarioFactory.getAeroLanceSize(UnitType.CONV_FIGHTER, true, "FC");
            assertTrue((numConvFighters >= 2) && (numConvFighters <= 6),
                    String.format("Conv Fighter count: %d for FC faction not between 2 and 6 inclusive", numConvFighters));

            // for Capellans, between 3 and 9
            numConvFighters = AtBDynamicScenarioFactory.getAeroLanceSize(UnitType.CONV_FIGHTER, true, "CC");
            assertTrue((numConvFighters >= 3) && (numConvFighters <= 9),
                    String.format("Conv Fighter count: %d for CC faction not between 3 and 9 inclusive", numConvFighters));

            numConvFighters = AtBDynamicScenarioFactory.getAeroLanceSize(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX, true, "FC");
            assertTrue((numConvFighters >= 2) && (numConvFighters <= 6),
                    String.format("Conv Fighter count: %d for FC faction not between 2 and 6 inclusive", numConvFighters));

            numConvFighters = AtBDynamicScenarioFactory.getAeroLanceSize(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX, true, "CC");
            assertTrue((numConvFighters >= 3) && (numConvFighters <= 9),
                    String.format("Conv Fighter count: %d for CC faction not between 3 and 9 inclusive", numConvFighters));
        }
    }
}
