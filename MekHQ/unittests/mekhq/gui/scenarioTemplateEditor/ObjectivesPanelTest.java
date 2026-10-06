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

import mekhq.campaign.mission.scenarios.ScenarioObjective;
import mekhq.campaign.mission.scenarios.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import org.junit.jupiter.api.Test;

/**
 * Phase 4.1 panel test for {@link ObjectivesPanel}. The list-management contract (load/refresh, and the model
 * reflecting the bound template's objectives) is headless-testable; only the add/edit child dialog is not.
 */
class ObjectivesPanelTest {

    private static ScenarioObjective objective(String description, ObjectiveCriterion criterion) {
        ScenarioObjective objective = new ScenarioObjective();
        objective.setDescription(description);
        objective.setObjectiveCriterion(criterion);
        return objective;
    }

    @Test
    void loadReflectsTemplateObjectivesInTheList() {
        ScenarioTemplate template = new ScenarioTemplate();
        template.scenarioObjectives.add(objective("Destroy the convoy", ObjectiveCriterion.Destroy));
        template.scenarioObjectives.add(objective("Preserve the VIP", ObjectiveCriterion.Preserve));

        ObjectivesPanel panel = new ObjectivesPanel();
        panel.load(template);

        assertEquals(2, panel.getObjectiveListModelSize());
    }

    @Test
    void refreshPicksUpObjectivesAddedToTheBoundTemplate() {
        ScenarioTemplate template = new ScenarioTemplate();
        ObjectivesPanel panel = new ObjectivesPanel();
        panel.load(template);
        assertEquals(0, panel.getObjectiveListModelSize());

        template.scenarioObjectives.add(objective("Reach the edge", ObjectiveCriterion.ReachMapEdge));
        panel.refresh();

        assertEquals(1, panel.getObjectiveListModelSize());
    }
}
