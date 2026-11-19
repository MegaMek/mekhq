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
package mekhq.gui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.CampaignPreset;
import org.junit.jupiter.api.Test;

class CampaignUpgradeConsoleTest {
    private static CampaignPreset titled(String title) {
        CampaignPreset preset = new CampaignPreset();
        preset.setTitle(title);
        return preset;
    }

    @Test
    void dropsTheSortNumberFromStockTitles() {
        assertEquals("Veteran Experience", CampaignUpgradeConsole.displayTitle(titled("2 - Veteran Experience")));
        assertEquals("My Rules", CampaignUpgradeConsole.displayTitle(titled("My Rules")));
        assertEquals("3025 Start", CampaignUpgradeConsole.displayTitle(titled("3025 Start")));
    }

    @Test
    void presetKeysMatchTheResourceKeys() {
        assertEquals("new_players_start_here",
              CampaignUpgradeConsole.presetKey(titled("1 - New Players Start Here")));
        assertEquals("veteran_experience", CampaignUpgradeConsole.presetKey(titled("2 - Veteran Experience")));
    }

    @Test
    void firstSentenceStripsHtmlAndShortens() {
        assertEquals("Expect pain.", CampaignUpgradeConsole.firstSentence("<b>Expect</b> pain. <br>More text."));
        String longText = "A".repeat(200) + ". Second.";
        String sentence = CampaignUpgradeConsole.firstSentence(longText);
        assertTrue(sentence.length() <= 80);
        assertTrue(sentence.endsWith("…"));
        assertEquals("", CampaignUpgradeConsole.firstSentence(null));
    }
}
