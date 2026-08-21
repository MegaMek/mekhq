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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.mission.scenarios.ScenarioObjective;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import mekhq.gui.scenarioTemplateEditor.ForceRosterEditor.CommitResult;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ForceRosterEditor}, covering the duplicate-ID and rename defects it closes, including propagation
 * of a rename through every template-owned reference to the force.
 */
class ForceRosterEditorTest {

    private static ScenarioForceTemplate force(String id) {
        ScenarioForceTemplate force = new ScenarioForceTemplate();
        force.setForceName(id);
        return force;
    }

    private static ScenarioTemplate templateWith(ScenarioForceTemplate... forces) {
        ScenarioTemplate template = new ScenarioTemplate();
        for (ScenarioForceTemplate force : forces) {
            template.getScenarioForces().put(force.getForceName(), force);
        }
        return template;
    }

    @Test
    void addingNewForceStoresItUnderItsId() {
        ScenarioTemplate template = templateWith();
        ScenarioForceTemplate alpha = force("Alpha");

        CommitResult result = ForceRosterEditor.commit(template, null, alpha);

        assertTrue(result.committed());
        assertSame(alpha, template.getScenarioForces().get("Alpha"));
        assertEquals(1, template.getScenarioForces().size());
    }

    @Test
    void addingForceWithExistingIdIsRejectedAndDoesNotOverwrite() {
        ScenarioForceTemplate original = force("Alpha");
        ScenarioTemplate template = templateWith(original);
        ScenarioForceTemplate replacement = force("Alpha");

        CommitResult result = ForceRosterEditor.commit(template, null, replacement);

        assertFalse(result.committed());
        assertTrue(result.errorMessage().contains("Alpha"));
        assertSame(original, template.getScenarioForces().get("Alpha"), "The existing force must not be overwritten");
        assertEquals(1, template.getScenarioForces().size());
    }

    @Test
    void editingInPlaceReplacesTheValueUnderTheSameId() {
        ScenarioTemplate template = templateWith(force("Alpha"));
        ScenarioForceTemplate edited = force("Alpha");

        CommitResult result = ForceRosterEditor.commit(template, "Alpha", edited);

        assertTrue(result.committed());
        assertSame(edited, template.getScenarioForces().get("Alpha"));
        assertEquals(1, template.getScenarioForces().size());
    }

    @Test
    void renamingToAFreeIdMovesTheEntryWithoutOrphaningTheOldKey() {
        ScenarioTemplate template = templateWith(force("Alpha"));
        ScenarioForceTemplate renamed = force("Bravo");

        CommitResult result = ForceRosterEditor.commit(template, "Alpha", renamed);

        assertTrue(result.committed());
        assertFalse(template.getScenarioForces().containsKey("Alpha"), "The old ID must be removed on rename");
        assertSame(renamed, template.getScenarioForces().get("Bravo"));
        assertEquals(1, template.getScenarioForces().size());
    }

    @Test
    void renamingOntoADifferentExistingForceIsRejected() {
        ScenarioForceTemplate alpha = force("Alpha");
        ScenarioForceTemplate bravo = force("Bravo");
        ScenarioTemplate template = templateWith(alpha, bravo);
        ScenarioForceTemplate renamed = force("Bravo");

        CommitResult result = ForceRosterEditor.commit(template, "Alpha", renamed);

        assertFalse(result.committed());
        assertSame(alpha, template.getScenarioForces().get("Alpha"), "The force being edited must be left untouched");
        assertSame(bravo, template.getScenarioForces().get("Bravo"), "The collision target must not be overwritten");
        assertEquals(2, template.getScenarioForces().size());
    }

    @Test
    void renamingUpdatesAnotherForcesSynchronizedDeploymentTarget() {
        ScenarioForceTemplate alpha = force("Alpha");
        ScenarioForceTemplate follower = force("Follower");
        follower.setSyncedForceName("Alpha");
        ScenarioTemplate template = templateWith(alpha, follower);
        ScenarioForceTemplate renamed = force("Bravo");

        CommitResult result = ForceRosterEditor.commit(template, "Alpha", renamed);

        assertTrue(result.committed());
        assertEquals("Bravo", follower.getSyncedForceName(),
              "A synchronized-deployment target must follow the rename, not dangle on the old ID");
    }

    @Test
    void renamingUpdatesAnotherForcesObjectiveLinks() {
        ScenarioForceTemplate alpha = force("Alpha");
        ScenarioForceTemplate linker = force("Linker");
        linker.setObjectiveLinkedForces(new ArrayList<>(List.of("Alpha", "Charlie")));
        ScenarioTemplate template = templateWith(alpha, linker);
        ScenarioForceTemplate renamed = force("Bravo");

        CommitResult result = ForceRosterEditor.commit(template, "Alpha", renamed);

        assertTrue(result.committed());
        assertEquals(List.of("Bravo", "Charlie"), linker.getObjectiveLinkedForces(),
              "Objective-linked force IDs must follow the rename");
    }

    @Test
    void renamingUpdatesObjectiveAssociatedForces() {
        ScenarioForceTemplate alpha = force("Alpha");
        ScenarioTemplate template = templateWith(alpha);
        ScenarioObjective objective = new ScenarioObjective();
        objective.addForce("Alpha");
        objective.addForce("Charlie");
        template.scenarioObjectives.add(objective);
        ScenarioForceTemplate renamed = force("Bravo");

        CommitResult result = ForceRosterEditor.commit(template, "Alpha", renamed);

        assertTrue(result.committed());
        assertFalse(objective.getAssociatedForceNames().contains("Alpha"),
              "The objective must no longer reference the old force ID");
        assertTrue(objective.getAssociatedForceNames().contains("Bravo"),
              "The objective must reference the renamed force ID");
        assertTrue(objective.getAssociatedForceNames().contains("Charlie"),
              "Unrelated associated forces must be left intact");
    }

    @Test
    void renamingUpdatesTheRenamedForcesOwnSelfReference() {
        ScenarioForceTemplate alpha = force("Alpha");
        ScenarioTemplate template = templateWith(alpha);
        // The freshly built replacement carries a self-reference under the old ID.
        ScenarioForceTemplate renamed = force("Bravo");
        renamed.setSyncedForceName("Alpha");

        CommitResult result = ForceRosterEditor.commit(template, "Alpha", renamed);

        assertTrue(result.committed());
        assertEquals("Bravo", renamed.getSyncedForceName(),
              "A self-reference on the renamed force must be updated to its new ID");
    }
}
