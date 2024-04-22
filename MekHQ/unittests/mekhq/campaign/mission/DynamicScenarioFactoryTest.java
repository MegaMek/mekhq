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
import megamek.common.Compute;
import megamek.common.UnitType;
import org.junit.jupiter.api.Disabled;
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

    private void testAeroLanceSizeInner(int unitTypeCode, int numFightersPerFlight, boolean isPlanetOwner){
        int weightCountRoll = (Compute.randomInt(3) + 1) * numFightersPerFlight;
        int useASFRoll = isPlanetOwner ? Compute.d6() : 6;
        int expected;
        switch(unitTypeCode){
            case UnitType.AEROSPACEFIGHTER:
                expected = numFightersPerFlight;
                break;
            case UnitType.CONV_FIGHTER:
                expected = weightCountRoll;
                break;
            default:
                expected = (useASFRoll >= 4) ? numFightersPerFlight : weightCountRoll;
        }

        assertEquals(expected,AtBDynamicScenarioFactory.getAeroLanceSize(unitTypeCode, numFightersPerFlight, weightCountRoll, useASFRoll));
    }

    @Test
    public void testAeroLanceSize() {
        assertEquals(2, AtBDynamicScenarioFactory.getAeroLanceSize(UnitType.AEROSPACEFIGHTER, true, "FC"));
        assertEquals(3, AtBDynamicScenarioFactory.getAeroLanceSize(UnitType.AEROSPACEFIGHTER, true, "CC"));
        assertEquals(2,
                AtBDynamicScenarioFactory.getAeroLanceSize(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX, false, "FC"));
        assertEquals(3,
                AtBDynamicScenarioFactory.getAeroLanceSize(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX, false, "CC"));

        // Roll some "random" values and check inner function return values
        int unitTypeCode = UnitType.AEROSPACEFIGHTER;
        int numFightersPerFlight = 2;
        boolean isPlanetOwner = false;
        testAeroLanceSizeInner(unitTypeCode,numFightersPerFlight, isPlanetOwner);
        isPlanetOwner = true;
        testAeroLanceSizeInner(unitTypeCode,numFightersPerFlight, isPlanetOwner);
        numFightersPerFlight = 3;
        isPlanetOwner = false;
        testAeroLanceSizeInner(unitTypeCode,numFightersPerFlight, isPlanetOwner);
        isPlanetOwner = true;
        testAeroLanceSizeInner(unitTypeCode,numFightersPerFlight, isPlanetOwner);

        unitTypeCode = UnitType.CONV_FIGHTER;
        numFightersPerFlight = 2;
        isPlanetOwner = false;
        testAeroLanceSizeInner(unitTypeCode,numFightersPerFlight, isPlanetOwner);
        testAeroLanceSizeInner(unitTypeCode,numFightersPerFlight, isPlanetOwner);
        isPlanetOwner = true;
        testAeroLanceSizeInner(unitTypeCode,numFightersPerFlight, isPlanetOwner);
        numFightersPerFlight = 3;
        isPlanetOwner = false;
        testAeroLanceSizeInner(unitTypeCode,numFightersPerFlight, isPlanetOwner);
        isPlanetOwner = true;
        testAeroLanceSizeInner(unitTypeCode,numFightersPerFlight, isPlanetOwner);
    }
}
