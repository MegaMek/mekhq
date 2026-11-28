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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import megamek.common.Configuration;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A refit that saves its new design looks the design up in the unit list afterwards. Saving it to a folder the list
 * never reads, such as a docs folder, used to fail only after the refit was confirmed, with a message that gave no
 * reason, and deleted the file (issue #11303). The refit now refuses such a folder before writing anything and says
 * where to save instead.
 */
class RefitDesignFolderTest {
    @TempDir
    Path outsideFolder;

    private String savedUserDirectory;
    private final ClientPreferences clientPreferences = PreferenceManager.getClientPreferences();

    @BeforeEach
    void clearUserDirectory() {
        savedUserDirectory = clientPreferences.getUserDir();
        clientPreferences.setUserDir("");
    }

    @AfterEach
    void restoreUserDirectory() {
        clientPreferences.setUserDir(savedUserDirectory);
    }

    private static Refit customRefitSavingItsDesign() throws Exception {
        PartsScenario scenario = PartsScenario.create();
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        return new Refit(locust, UnitFixture.LOCUST_LCT_1E.loadEntity(), true, false, true);
    }

    @Test
    void aDesignSavedOutsideTheUnitFoldersIsRefusedBeforeAnythingIsWritten() throws Exception {
        Refit refit = customRefitSavingItsDesign();
        File docsFile = outsideFolder.resolve("docs").resolve("Locust LCT-1E Mk II.mtf").toFile();
        assertTrue(docsFile.getParentFile().mkdirs());
        refit.setDesignFile(docsFile);

        IOException refusal = assertThrows(IOException.class, refit::begin);

        String message = refusal.getMessage();
        assertTrue(message.contains("MekHQ does not read units from " + docsFile.getParentFile().getPath()), message);
        assertTrue(message.contains(new File(Configuration.unitsDir(), "customs").getAbsolutePath()), message);
        assertFalse(docsFile.exists(), "Nothing is written to the refused folder");
        assertFalse(refit.getOriginalUnit().isRefitting(), "The refit has not started");
    }

    @Test
    void theUserDirectoryIsOfferedOnceItIsSet() {
        assertTrue(RefitDesignFolder.describeUsableFolders().contains("set a user directory"));

        clientPreferences.setUserDir(outsideFolder.toString());
        File userDirectoryFile = outsideFolder.resolve("Locust LCT-1E Mk II.mtf").toFile();

        assertTrue(MekSummaryCache.isInUnitListFolder(userDirectoryFile), "A user directory file is not refused");
        assertTrue(RefitDesignFolder.describeUsableFolders().contains("your user directory " + outsideFolder),
              RefitDesignFolder.describeUsableFolders());
    }

    @Test
    void saveForLaterOutsideTheUnitFoldersSaysTheDesignIsNotOfferedForRefits() {
        File docsFile = outsideFolder.resolve("Locust LCT-1E Mk II.mtf").toFile();

        String warning = RefitDesignFolder.describeSavedOutsideUnitFolders(docsFile);

        assertTrue(warning.contains("will not be offered when you refit a unit"), warning);
        assertTrue(warning.contains(outsideFolder.toFile().getAbsolutePath()), warning);
    }
}
