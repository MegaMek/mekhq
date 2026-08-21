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
package mekhq.campaign.mission.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.mission.scenarios.ScenarioTemplate.BattlefieldControlType;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link AtBDynamicScenario#getBattlefieldControlDescription()} derives the briefing line from the
 * template's {@link BattlefieldControlType}, so it no longer has to be written into the briefing by hand.
 */
class AtBDynamicScenarioBattlefieldControlTest {

    private static AtBDynamicScenario scenarioWithControl(BattlefieldControlType control) {
        AtBDynamicScenario scenario = new AtBDynamicScenario();
        ScenarioTemplate template = new ScenarioTemplate();
        template.battlefieldControl = control;
        scenario.setTemplate(template);
        return scenario;
    }

    @Test
    void victorControlDescribesTheWinner() {
        String description = scenarioWithControl(BattlefieldControlType.VICTOR).getBattlefieldControlDescription();
        assertTrue(description.toLowerCase().contains("winner"),
              "VICTOR should describe the winner controlling the field, was: " + description);
    }

    @Test
    void playerControlDescribesThePlayer() {
        String description = scenarioWithControl(BattlefieldControlType.PLAYER).getBattlefieldControlDescription();
        assertTrue(description.toLowerCase().contains("you"),
              "PLAYER should describe the player controlling the field, was: " + description);
    }

    @Test
    void enemyControlDescribesTheEnemy() {
        String description = scenarioWithControl(BattlefieldControlType.ENEMY).getBattlefieldControlDescription();
        assertTrue(description.toLowerCase().contains("enemy"),
              "ENEMY should describe the enemy controlling the field, was: " + description);
    }

    @Test
    void undefinedControlYieldsNoLine() {
        assertEquals("", scenarioWithControl(BattlefieldControlType.UNDEFINED).getBattlefieldControlDescription());
    }

    @Test
    void missingTemplateYieldsNoLine() {
        AtBDynamicScenario scenario = new AtBDynamicScenario();
        assertEquals("", scenario.getBattlefieldControlDescription(),
              "A scenario without a template should show no battlefield-control line");
    }
}
