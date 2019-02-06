/*
 * Campaign.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.AtBScenarioModifier;

import mekhq.campaign.mission.atb.AtBScenarioModifier;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author NickAragua
 */
@RunWith(JUnit4.class)
public class ScenarioModifierTest {

    /*
     * Tests that the initial loading of the scenario modifier manifest works.
     */
    @Test
    public void testLoadScenarioModifierManifest() {
        //AtBPreAddForceModifier atbsm = new AtBPreAddForceModifier();
        Assert.assertNotNull(AtBScenarioModifier.getScenarioFileNames());
        Assert.assertNotEquals(0, AtBScenarioModifier.getScenarioFileNames().size());
    }
    
    /*
     * Tests that loading scenario modifers from the manifest works.
     */
    @Test
    public void testLoadScenarioModifiersFromManifest() {
        AtBScenarioModifier atbsm =  new AtBScenarioModifier();
        Assert.assertNotNull(AtBScenarioModifier.getScenarioModifiers());
    }

}
