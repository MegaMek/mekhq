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
package mekhq.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;

import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.PlayerForce;
import mekhq.gui.utilities.ObservableString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

class AutosaveServiceTest {
    @TempDir
    private Path campaignsDirectory;

    private MockedStatic<MekHQ> mekHQ;
    private MHQOptions options;
    private Campaign campaign;

    @BeforeEach
    void beforeEach() {
        options = mock(MHQOptions.class);
        when(options.getMaximumNumberOfAutoSavesValue()).thenReturn(5);
        when(options.getDateLocale()).thenReturn(Locale.ENGLISH);

        PlayerForce playerForce = mock(PlayerForce.class);
        when(playerForce.getName()).thenReturn("Test Force");
        campaign = mock(Campaign.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 6, 15));

        mekHQ = mockStatic(MekHQ.class);
        mekHQ.when(MekHQ::getMHQOptions).thenReturn(options);
        mekHQ.when(MekHQ::getCampaignsDirectory)
              .thenReturn(new ObservableString("campaignsDirectory", campaignsDirectory.toString()));
    }

    @AfterEach
    void afterEach() {
        mekHQ.close();
    }

    @Test
    void afterScenarioAutosaveWritesTheCampaignWhenEnabled() {
        when(options.getAutosaveAfterScenariosValue()).thenReturn(true);

        new AutosaveService().requestAfterScenarioAutosave(campaign);

        verify(campaign).writeToXML(any(), eq(false));
        File[] files = campaignsDirectory.toFile().listFiles();
        assertNotNull(files);
        assertEquals(1, files.length);
        assertEquals("Autosave-1-Test Force-30250615.cpnx.gz", files[0].getName());
    }

    @Test
    void afterScenarioAutosaveIgnoresTheOtherTriggersWhenDisabled() {
        when(options.getAutosaveBeforeScenariosValue()).thenReturn(true);
        when(options.getAutosaveBeforeMissionEndValue()).thenReturn(true);
        when(options.getAutosaveAfterScenariosValue()).thenReturn(false);

        new AutosaveService().requestAfterScenarioAutosave(campaign);

        verify(campaign, never()).writeToXML(any(), anyBoolean());
        File[] files = campaignsDirectory.toFile().listFiles();
        assertNotNull(files);
        assertEquals(0, files.length);
    }
}
