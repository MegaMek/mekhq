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
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import mekhq.campaign.mission.scenarios.ScenarioTemplate.BattlefieldControlType;
import mekhq.campaign.mission.scenarios.ScenarioType;
import org.junit.jupiter.api.Test;

/**
 * Phase 4.0 spike + first panel test. Confirms {@link TemplatePropertiesPanel} - a {@link javax.swing.JPanel} - can be
 * constructed and its {@link TemplatePropertiesPanel#load}/{@link TemplatePropertiesPanel#writeInto} contract exercised
 * in the (headless) test JVM, which the {@code JDialog} it was extracted from could not.
 */
class TemplatePropertiesPanelTest {

    @Test
    void loadThenWriteIntoRoundTripsTemplateProperties() {
        ScenarioTemplate source = new ScenarioTemplate();
        source.name = "Test Scenario";
        source.shortBriefing = "short briefing";
        source.detailedBriefing = "detailed briefing";
        source.setStratConScenarioType("CONVOY");
        source.battlefieldControl = BattlefieldControlType.ENEMY;
        source.isHostileFacility = true;
        source.isAlliedFacility = true;
        source.isSuitedForAmbushes = true;
        source.isSuitedForBungledPatrols = true;

        TemplatePropertiesPanel panel = new TemplatePropertiesPanel();
        panel.load(source);

        ScenarioTemplate target = new ScenarioTemplate();
        panel.writeInto(target);

        assertEquals("Test Scenario", target.name);
        assertEquals("short briefing", target.shortBriefing);
        assertEquals("detailed briefing", target.detailedBriefing);
        assertEquals(ScenarioType.CONVOY, target.getStratConScenarioType());
        assertEquals(BattlefieldControlType.ENEMY, target.getBattlefieldControl());
        assertTrue(target.isHostileFacility);
        assertTrue(target.isAlliedFacility);
        assertTrue(target.isSuitedForAmbushes());
        assertTrue(target.isSuitedForBungledPatrols());
    }
}
