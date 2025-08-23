/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.board.Board;
import org.junit.jupiter.api.Test;

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
