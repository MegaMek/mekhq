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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.io.File;

import megamek.common.Configuration;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.preference.PreferenceManager;

/**
 * Explains to the player why a design file cannot be used by a refit, and where it can be saved instead. A refit
 * looks its new design up in the unit list, and the campaign save copies custom designs from it, so the file must be
 * in a folder the unit list reads (issue #11303). Which folders those are is decided by
 * {@link MekSummaryCache#isInUnitListFolder(File)}.
 */
public final class RefitDesignFolder {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    private RefitDesignFolder() {
    }

    /**
     * @return where a design can be saved so a refit finds it: the customs folder, and the user directory if one is
     *       set
     */
    public static String describeUsableFolders() {
        String customsFolder = new File(Configuration.unitsDir(), "customs").getAbsolutePath();
        String userDirectory = PreferenceManager.getClientPreferences().getUserDir();
        if (userDirectory.isBlank()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "RefitDesignFolder.usable.noUserDirectory", customsFolder);
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "RefitDesignFolder.usable.withUserDirectory", customsFolder,
              userDirectory);
    }

    /**
     * @param designFile the file the player chose for a refit's new design, in a folder the unit list does not read
     *
     * @return why the refit cannot use that file, and where to save it instead
     */
    public static String describeRefitRefused(File designFile) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "RefitDesignFolder.refitRefused",
              folderOf(designFile), describeUsableFolders());
    }

    /**
     * @param savedFile a design saved with "Save For Later" to a folder the unit list does not read
     *
     * @return that the design will not be offered for refits, and where to save it instead
     */
    public static String describeSavedOutsideUnitFolders(File savedFile) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "RefitDesignFolder.savedForLater",
              folderOf(savedFile), describeUsableFolders());
    }

    private static String folderOf(File file) {
        File folder = file.getAbsoluteFile().getParentFile();
        return (folder == null) ? file.getAbsolutePath() : folder.getPath();
    }
}
