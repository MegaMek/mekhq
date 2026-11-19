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

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import mekhq.CampaignPreset;
import mekhq.MHQConstants;
import mekhq.campaign.personnel.ranks.Ranks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PresetBrowserTest {
    @BeforeAll
    static void loadRankSystems() {
        // Full presets carry a rank system, which can only be read once the rank systems are loaded
        Ranks.initializeRankSystems();
    }

    private static CampaignPreset titled(String title) {
        CampaignPreset preset = new CampaignPreset();
        preset.setTitle(title);
        return preset;
    }

    @Test
    void dropsTheSortNumberFromStockTitles() {
        assertEquals("Veteran Experience", PresetBrowser.displayTitle(titled("2 - Veteran Experience")));
        assertEquals("My Rules", PresetBrowser.displayTitle(titled("My Rules")));
        assertEquals("3025 Start", PresetBrowser.displayTitle(titled("3025 Start")));
    }

    @Test
    void presetKeysMatchTheResourceKeys() {
        assertEquals("new_players_start_here",
              PresetBrowser.presetKey(titled("1 - New Players Start Here")));
        assertEquals("veteran_experience", PresetBrowser.presetKey(titled("2 - Veteran Experience")));
    }

    @Test
    void firstSentenceStripsHtmlAndShortens() {
        assertEquals("Expect pain.", PresetBrowser.firstSentence("<b>Expect</b> pain. <br>More text."));
        String longText = "A".repeat(200) + ". Second.";
        String sentence = PresetBrowser.firstSentence(longText);
        assertTrue(sentence.length() <= 80);
        assertTrue(sentence.endsWith("…"));
        assertEquals("", PresetBrowser.firstSentence(null));
    }

    @Test
    void everyStockPresetHasATagAndSummary() {
        List<CampaignPreset> stock = CampaignPreset.loadCampaignPresetsFromDirectory(
              new File(MHQConstants.CAMPAIGN_PRESET_DIRECTORY));
        File[] stockFiles = new File(MHQConstants.CAMPAIGN_PRESET_DIRECTORY)
                                  .listFiles((dir, name) -> name.endsWith(".xml"));
        assertNotNull(stockFiles);
        assertEquals(stockFiles.length, stock.size(), "a stock preset failed to load");
        for (CampaignPreset preset : stock) {
            assertNotNull(preset.getPresetFile(), preset.getTitle() + " has no source file");
            assertTrue(PresetBrowser.isStockPreset(preset), preset.getTitle());
            String prefix = "PresetBrowser.preset." + PresetBrowser.presetKey(preset);
            assertTrue(isResourceKeyValid(getTextAt(PresetBrowser.RESOURCE_BUNDLE, prefix + ".tag")),
                  preset.getTitle() + " has no tag");
            assertTrue(isResourceKeyValid(getTextAt(PresetBrowser.RESOURCE_BUNDLE, prefix + ".sub")),
                  preset.getTitle() + " has no summary line");
        }
    }

    @Test
    void presetsOutsideTheStockFolderAreThePlayers(@TempDir Path userFolder) throws IOException {
        File[] stockFiles = new File(MHQConstants.CAMPAIGN_PRESET_DIRECTORY)
                                  .listFiles((dir, name) -> name.endsWith(".xml"));
        assertNotNull(stockFiles);
        assertTrue(stockFiles.length > 0, "no stock presets found");
        Path copy = Files.copy(stockFiles[0].toPath(), userFolder.resolve(stockFiles[0].getName()));

        CampaignPreset preset = CampaignPreset.parseFromFile(copy.toFile());

        assertNotNull(preset);
        assertFalse(PresetBrowser.isStockPreset(preset), "a fully loaded user preset counted as stock");
    }
}
