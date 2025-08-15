/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.panels;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;

import megamek.common.preference.PreferenceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StartupScreenPanelTest {
    File dir;

    @BeforeEach
    void setUp() {
        dir = mock(File.class);
    }

    @Test
    void testSaveFilterAllowsValidCampaignSaves() {
        String fileName = "MySave.xml";
        assertTrue(StartupScreenPanel.saveFilter.accept(dir, fileName));
        fileName = "MySave.CPNX";
        assertTrue(StartupScreenPanel.saveFilter.accept(dir, fileName));
        fileName = "20240306T2344 Save Campaign No 666.cpnx.gz";
        assertTrue(StartupScreenPanel.saveFilter.accept(dir, fileName));
    }

    @Test
    void testSaveFilterNotAllowClientSettingsXML() {
        String fileName = PreferenceManager.DEFAULT_CFG_FILE_NAME;
        boolean allowed = StartupScreenPanel.saveFilter.accept(dir, fileName);
        assertFalse(allowed);
    }
}
