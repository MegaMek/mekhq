/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.panels;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import megamek.common.preference.PreferenceManager;

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
