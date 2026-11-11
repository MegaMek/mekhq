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
package mekhq.gui.developerTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import megamek.common.units.UnitType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceGenerationMethod;
import org.junit.jupiter.api.Test;

/**
 * Covers {@link ModifierForceDefinitionDialog#availableForceIds}: the sync and objective-link choices offered when
 * editing a scenario modifier's force must include every ID the force already refers to, or saving it would drop them.
 */
class ModifierForceDefinitionDialogTest {

    @Test
    void newForceGetsKnownIdsPlusPrimaryForceSorted() {
        List<String> forceIds = ModifierForceDefinitionDialog.availableForceIds(List.of("OpFor", "Convoy"), null);

        assertEquals(List.of("Convoy", "OpFor", ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID), forceIds);
    }

    @Test
    void existingForceReferencesAreKeptAndBlanksSkipped() {
        ScenarioForceTemplate force = new ScenarioForceTemplate(ForceAlignment.Opposing.ordinal(),
              ForceGenerationMethod.BVScaled.ordinal(), 1.0, List.of(1), 5, 50, UnitType.MEK);
        force.setSyncedForceName("Vanguard");
        force.setObjectiveLinkedForces(List.of("VIP", " ", "OpFor"));

        List<String> forceIds = ModifierForceDefinitionDialog.availableForceIds(List.of("OpFor"), force);

        assertEquals(List.of("OpFor", ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID, "VIP", "Vanguard"), forceIds);
    }

    @Test
    void blankSyncedForceNameIsNotOffered() {
        ScenarioForceTemplate force = new ScenarioForceTemplate(ForceAlignment.Allied.ordinal(),
              ForceGenerationMethod.BVScaled.ordinal(), 1.0, List.of(1), 5, 50, UnitType.MEK);
        // several shipped modifiers store an empty synced force name
        force.setSyncedForceName("");

        List<String> forceIds = ModifierForceDefinitionDialog.availableForceIds(List.of(), force);

        assertEquals(List.of(ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID), forceIds);
    }
}
