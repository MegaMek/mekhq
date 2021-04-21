/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package mekhq.campaign.mission;

import megamek.common.Board;
import megamek.common.UnitType;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests relevant to the AtBDynamicScenarioFactory
 * @author NickAragua
 */
public class DynamicScenarioFactoryTest {
    @Test
    public void testGetOppositeEdge() {
        int startingEdge = Board.START_EDGE;
        Assert.assertEquals(Board.START_CENTER, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));
        
        startingEdge = Board.START_CENTER;
        Assert.assertEquals(Board.START_EDGE, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));
        
        startingEdge = Board.START_ANY;
        Assert.assertEquals(Board.START_ANY, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));
        
        startingEdge = Board.START_N;
        Assert.assertEquals(Board.START_S, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));
        
        startingEdge = Board.START_E;
        Assert.assertEquals(Board.START_W, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));
        
        startingEdge = Board.START_S;
        Assert.assertEquals(Board.START_N, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));
        
        startingEdge = Board.START_W;
        Assert.assertEquals(Board.START_E, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));
        
        startingEdge = Board.START_NW;
        Assert.assertEquals(Board.START_SE, AtBDynamicScenarioFactory.getOppositeEdge(startingEdge));
    }
    
    @Test
    public void testAeroLanceSize() {
        Assert.assertEquals(2, AtBDynamicScenarioFactory.getAeroLanceSize(UnitType.AERO, true, "FC"));
        Assert.assertEquals(3, AtBDynamicScenarioFactory.getAeroLanceSize(UnitType.AERO, true, "CC"));
        Assert.assertEquals(2, 
                AtBDynamicScenarioFactory.getAeroLanceSize(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX, false, "FC"));
        Assert.assertEquals(3, 
                AtBDynamicScenarioFactory.getAeroLanceSize(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX, false, "CC"));
        
        // the number of conv fighters is randomly generated, but should be between 2 and 6, inclusively
        // we run it a bunch of times, all but guaranteeing that we hit the extremes
        for (int x = 0; x < 40; x++) {
            int numConvFighters = AtBDynamicScenarioFactory.getAeroLanceSize(UnitType.CONV_FIGHTER, true, "FC");
            Assert.assertTrue(String.format("Conv Fighter count: %d for FC faction not between 2 and 6 inclusive", numConvFighters), 
                    (numConvFighters >= 2) && (numConvFighters <= 6));
            
            // for capellans, between 3 and 9
            numConvFighters = AtBDynamicScenarioFactory.getAeroLanceSize(UnitType.CONV_FIGHTER, true, "CC");
            Assert.assertTrue(String.format("Conv Fighter count: %d for CC faction not between 3 and 9 inclusive", numConvFighters),
                    (numConvFighters >= 3) && (numConvFighters <= 9));
            
            numConvFighters = AtBDynamicScenarioFactory.getAeroLanceSize(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX, true, "FC");
            Assert.assertTrue(String.format("Conv Fighter count: %d for FC faction not between 2 and 6 inclusive", numConvFighters),
                    (numConvFighters >= 2) && (numConvFighters <= 6));
            
            numConvFighters = AtBDynamicScenarioFactory.getAeroLanceSize(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX, true, "CC");
            Assert.assertTrue(String.format("Conv Fighter count: %d for CC faction not between 3 and 9 inclusive", numConvFighters),
                    (numConvFighters >= 3) && (numConvFighters <= 9));
        }
    }
}
