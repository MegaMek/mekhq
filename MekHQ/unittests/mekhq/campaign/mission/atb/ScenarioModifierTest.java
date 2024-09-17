/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission.atb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author NickAragua
 */
public class ScenarioModifierTest {

    /**
     * Tests that the initial loading of the scenario modifier manifest works.
     */
    @Test
    public void testLoadScenarioModifierManifest() {
        assertNotNull(AtBScenarioModifier.getScenarioFileNames());
        assertNotEquals(0, AtBScenarioModifier.getScenarioFileNames().size());
    }

    /**
     * Tests that loading scenario modifiers from the manifest works.
     */
    @Test
    public void testLoadScenarioModifiersFromManifest() {
        assertNotNull(AtBScenarioModifier.getScenarioModifiers());
    }
}
