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
package mekhq.gui.scenarioTemplateEditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.mission.scenarios.ScenarioObjective;
import org.junit.jupiter.api.Test;

/**
 * Covers {@link ObjectiveEditPanel#saveInto}: the objective editor edits a copy, so saving must put that copy in place
 * of the original (keeping its position) rather than adding a duplicate.
 */
class ObjectiveEditPanelTest {

    @Test
    void savingAnEditReplacesTheOriginalInPlace() {
        ScenarioObjective first = new ScenarioObjective();
        ScenarioObjective second = new ScenarioObjective();
        ScenarioObjective third = new ScenarioObjective();
        List<ScenarioObjective> objectives = new ArrayList<>(List.of(first, second, third));

        ScenarioObjective edited = new ScenarioObjective(second);
        ObjectiveEditPanel.saveInto(objectives, second, edited);

        assertEquals(3, objectives.size());
        assertSame(first, objectives.get(0));
        assertSame(edited, objectives.get(1));
        assertSame(third, objectives.get(2));
    }

    @Test
    void savingANewObjectiveAppendsIt() {
        ScenarioObjective existing = new ScenarioObjective();
        List<ScenarioObjective> objectives = new ArrayList<>(List.of(existing));

        ScenarioObjective added = new ScenarioObjective();
        ObjectiveEditPanel.saveInto(objectives, null, added);

        assertEquals(List.of(existing, added), objectives);
    }

    @Test
    void savingAnEditWhoseOriginalWasRemovedAppendsIt() {
        // the original can be gone if the list was changed while the editor was open; the edit is still kept
        ScenarioObjective original = new ScenarioObjective();
        List<ScenarioObjective> objectives = new ArrayList<>();

        ScenarioObjective edited = new ScenarioObjective(original);
        ObjectiveEditPanel.saveInto(objectives, original, edited);

        assertEquals(List.of(edited), objectives);
    }
}
