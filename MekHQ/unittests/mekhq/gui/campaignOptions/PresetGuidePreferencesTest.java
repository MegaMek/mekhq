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
package mekhq.gui.campaignOptions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import megamek.client.ui.preferences.SuitePreferences;
import mekhq.gui.campaignOptions.PresetGuidePreferences.GuideScreen;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PresetGuidePreferencesTest {
    private static PresetGuidePreferences fresh(SuitePreferences preferences) {
        return new PresetGuidePreferences(preferences.forClass(PresetGuidePreferences.class));
    }

    @Test
    void opensTheFirstTimeOnly() {
        PresetGuidePreferences preferences = fresh(new SuitePreferences());
        assertTrue(preferences.shouldOpenGuide(GuideScreen.UPGRADE));
        preferences.setGuideSeen(GuideScreen.UPGRADE, true);
        assertFalse(preferences.shouldOpenGuide(GuideScreen.UPGRADE));
        preferences.setGuideAlwaysOpen(GuideScreen.UPGRADE, true);
        assertTrue(preferences.shouldOpenGuide(GuideScreen.UPGRADE));
    }

    @Test
    void screensAreRememberedSeparately() {
        PresetGuidePreferences preferences = fresh(new SuitePreferences());
        preferences.setGuideSeen(GuideScreen.UPGRADE, true);
        assertTrue(preferences.shouldOpenGuide(GuideScreen.NEW_CAMPAIGN));
    }

    @Test
    void settingsSurviveSavingAndLoadingThePreferencesFile(@TempDir Path directory) throws Exception {
        String file = directory.resolve("mhq.preferences").toString();
        SuitePreferences saved = new SuitePreferences();
        PresetGuidePreferences before = fresh(saved);
        before.setGuideSeen(GuideScreen.NEW_CAMPAIGN, true);
        before.setGuideAlwaysOpen(GuideScreen.UPGRADE, true);
        saved.saveToFile(file);

        SuitePreferences loaded = new SuitePreferences();
        loaded.loadFromFile(file);
        PresetGuidePreferences after = fresh(loaded);
        assertTrue(after.isGuideSeen(GuideScreen.NEW_CAMPAIGN));
        assertFalse(after.isGuideSeen(GuideScreen.UPGRADE));
        assertTrue(after.isGuideAlwaysOpen(GuideScreen.UPGRADE));
    }
}
