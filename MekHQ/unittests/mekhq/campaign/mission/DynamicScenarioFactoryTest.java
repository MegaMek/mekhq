/*
 * Copyright (c) 2020-2024 - The MegaMek Team. All Rights Reserved.
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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests relevant to the AtBDynamicScenarioFactory
 *
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
}
