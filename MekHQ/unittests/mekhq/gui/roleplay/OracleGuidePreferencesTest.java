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
package mekhq.gui.roleplay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import megamek.client.ui.preferences.SuitePreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OracleGuidePreferencesTest {
    @Test
    void defaultsShowTheTutorials() {
        OracleGuidePreferences preferences = new OracleGuidePreferences(
              new SuitePreferences().forClass(OracleGuidePreferences.class));
        assertFalse(preferences.isWelcomeDismissed());
        assertTrue(preferences.isGuideAutoOpen());
        for (ConsolePage page : ConsolePage.values()) {
            assertFalse(preferences.isGuideSeen(page), page.name());
        }
    }

    @Test
    void settingsSurviveSavingAndLoadingThePreferencesFile(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("mhq.preferences");
        SuitePreferences saved = new SuitePreferences();
        OracleGuidePreferences before = new OracleGuidePreferences(saved.forClass(OracleGuidePreferences.class));
        before.setWelcomeDismissed(true);
        before.setGuideAutoOpen(false);
        before.setGuideSeen(ConsolePage.THREADS, true);
        saved.saveToFile(file.toString());

        String json = Files.readString(file);
        assertTrue(json.contains(OracleGuidePreferences.class.getName()), json);
        assertTrue(json.contains("guideSeen.threads"), json);

        SuitePreferences loaded = new SuitePreferences();
        loaded.loadFromFile(file.toString());
        OracleGuidePreferences after = new OracleGuidePreferences(loaded.forClass(OracleGuidePreferences.class));
        assertTrue(after.isWelcomeDismissed());
        assertFalse(after.isGuideAutoOpen());
        assertTrue(after.isGuideSeen(ConsolePage.THREADS));
        assertFalse(after.isGuideSeen(ConsolePage.CAST));
    }
}
