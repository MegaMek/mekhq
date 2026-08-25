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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.mission.scenarios.ScenarioObjective;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;

/**
 * Commits an added or edited force into the scenario template's force roster (a map keyed by force ID). This logic is
 * separated from {@link ScenarioTemplateEditorDialog} so it can be exercised without the Swing UI.
 *
 * <p>It closes three long-standing defects in the editor:
 * <ul>
 *     <li>adding a force whose ID already exists silently overwrote the existing force;</li>
 *     <li>editing a force and changing its ID inserted a new entry while leaving the original key orphaned in the
 *     map; and</li>
 *     <li>renaming a force left every reference to it dangling on the old ID - other forces' synchronized-deployment
 *     targets and objective links, and objectives' associated forces - so synchronized deployment fell back to a
 *     random edge and objectives no longer matched the renamed force.</li>
 * </ul>
 */
public final class ForceRosterEditor {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ScenarioTemplateEditorDialog";

    private ForceRosterEditor() {
    }

    /**
     * Outcome of a commit attempt.
     *
     * @param committed    whether the force was written to the roster
     * @param errorMessage a user-facing explanation when {@code committed} is false, otherwise empty
     */
    public record CommitResult(boolean committed, String errorMessage) {
        private static CommitResult success() {
            return new CommitResult(true, "");
        }

        private static CommitResult failure(String message) {
            return new CommitResult(false, message);
        }
    }

    /**
     * Writes {@code force} into {@code template}'s force roster under its current force ID, enforcing uniqueness and,
     * on a rename, re-pointing every template-owned reference to the force so none is left dangling on the old ID.
     *
     * @param template      the scenario template whose roster (and objectives) are being edited
     * @param editedForceId the ID of the force currently being edited, or {@code null} when adding a brand-new force
     * @param force         the force to store; its {@link ScenarioForceTemplate#getForceName()} is used as the key
     *
     * @return a successful result if the force was written, otherwise a failure carrying a message to show the user
     */
    public static CommitResult commit(ScenarioTemplate template, @Nullable String editedForceId,
          ScenarioForceTemplate force) {
        Map<String, ScenarioForceTemplate> roster = template.getScenarioForces();
        String newForceId = force.getForceName();
        boolean editingExisting = editedForceId != null;
        boolean renamed = !newForceId.equals(editedForceId);

        // A brand-new force, or a rename, must not collide with a different existing force.
        if ((!editingExisting || renamed) && roster.containsKey(newForceId)) {
            return CommitResult.failure(getFormattedTextAt(RESOURCE_BUNDLE,
                  "ForceRosterEditor.duplicateId",
                  newForceId));
        }

        // On a rename, re-point every reference from the old ID to the new one and drop the orphaned old key.
        if (editingExisting && renamed) {
            propagateRename(template, editedForceId, newForceId, force);
            roster.remove(editedForceId);
        }

        roster.put(newForceId, force);
        return CommitResult.success();
    }

    /**
     * Re-points every template-owned reference to a force from {@code oldForceId} to {@code newForceId} so a rename
     * does not leave synchronized deployment or objectives resolving to a removed roster entry.
     *
     * @param template     the template whose forces and objectives may reference the renamed force
     * @param oldForceId   the force ID being replaced
     * @param newForceId   the force ID to point references at
     * @param renamedForce the freshly built force replacing the old entry; it is not yet in the roster, so any
     *                     self-reference it carries must be updated directly
     */
    private static void propagateRename(ScenarioTemplate template, String oldForceId, String newForceId,
          ScenarioForceTemplate renamedForce) {
        for (ScenarioForceTemplate other : template.getScenarioForces().values()) {
            repointForceReferences(other, oldForceId, newForceId);
        }
        repointForceReferences(renamedForce, oldForceId, newForceId);

        for (ScenarioObjective objective : template.scenarioObjectives) {
            // getAssociatedForceNames() returns a defensive copy, so mutate through remove/add instead.
            if (objective.getAssociatedForceNames().contains(oldForceId)) {
                objective.removeForce(oldForceId);
                objective.addForce(newForceId);
            }
        }
    }

    /**
     * Re-points a single force's synchronized-deployment target and objective links from {@code oldForceId} to
     * {@code newForceId}.
     */
    private static void repointForceReferences(ScenarioForceTemplate force, String oldForceId, String newForceId) {
        if (oldForceId.equals(force.getSyncedForceName())) {
            force.setSyncedForceName(newForceId);
        }

        List<String> objectiveLinkedForces = force.getObjectiveLinkedForces();
        if (objectiveLinkedForces != null) {
            objectiveLinkedForces.replaceAll(id -> oldForceId.equals(id) ? newForceId : id);
        }
    }
}
