/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq;

import megamek.MegaMek;
import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.common.annotations.Nullable;
import megamek.common.util.fileUtils.DirectoryItems;
import megamek.common.util.fileUtils.ImageFileFactory;
import mekhq.io.AwardFileFactory;

import java.io.File;

public class MHQStaticDirectoryManager extends MMStaticDirectoryManager {
    //region Variable Declarations
    private static DirectoryItems forceIconDirectory;
    private static DirectoryItems awardIconDirectory;

    // Re-parsing Prevention Variables: The are True at startup and when the specified directory
    // should be re-parsed, and are used to avoid re-parsing the directory repeatedly when there's an error.
    private static boolean parseForceIconDirectory = true;
    private static boolean parseAwardIconDirectory = true;
    //endregion Variable Declarations

    //region Constructors
    protected MHQStaticDirectoryManager() {
        // This class is not to be instantiated
    }
    //endregion Constructors

    //region Initialization
    /**
     * This initialized all of the directories under this manager
     */
    public static void initialize() {
        MMStaticDirectoryManager.initialize();
        initializeForceIcons();
        initializeAwardIcons();
    }

    /**
     * Parses MekHQ's force icon folder when first called or when it was refreshed.
     *
     * @see #refreshForceIcons()
     */
    private static void initializeForceIcons() {
        // Read in and parse MekHQ's force icon folder only when first called or when refreshed
        if (parseForceIconDirectory) {
            // Set parseForceIconDirectory to false to avoid parsing repeatedly when something fails
            parseForceIconDirectory = false;
            try {
                forceIconDirectory = new DirectoryItems(new File("data/images/force"),
                        "", new ImageFileFactory());
            } catch (Exception e) {
                MegaMek.getLogger().error("Could not parse the force icon directory!", e);
            }
        }
    }

    /**
     * Parses MekHQ's awards icon folder when first called or when it was refreshed.
     *
     * @see #refreshAwardIcons()
     */
    private static void initializeAwardIcons() {
        // Read in and parse MekHQ's award icon folder only when first called or when refreshed
        if (parseAwardIconDirectory) {
            // Set parseAwardIconDirectory to false to avoid parsing repeatedly when something fails
            parseAwardIconDirectory = false;
            try {
                awardIconDirectory = new DirectoryItems(new File("data/images/awards"),
                        "", new AwardFileFactory());
            } catch (Exception e) {
                MegaMek.getLogger().error("Could not parse the award icon directory!", e);
            }
        }
    }
    //endregion Initialization

    //region Getters
    /**
     * Returns a DirectoryItems object containing all force icon filenames
     * found in MekHQ's force icon folder.
     * @return a DirectoryItems object with the force icon folders and filenames.
     * May be null if the directory cannot be parsed.
     */
    public static @Nullable DirectoryItems getForceIcons() {
        initializeForceIcons();
        return forceIconDirectory;
    }

    /**
     * Returns a DirectoryItems object containing all award icon filenames
     * found in MekHQ's award icon folder.
     * @return a DirectoryItems object with the award icon folders and filenames.
     * May be null if the directory cannot be parsed.
     */
    public static @Nullable DirectoryItems getAwardIcons() {
        initializeAwardIcons();
        return awardIconDirectory;
    }

    //endregion Getters

    //region Refreshers
    /**
     * Re-reads MekHQ's force icon folder and returns the updated
     * DirectoryItems object. This will update the DirectoryItems object
     * with changes to the force icons (like added image files and folders)
     * while MekHQ is running.
     *
     * @see #getForceIcons()
     */
    public static DirectoryItems refreshForceIcons() {
        parseForceIconDirectory = true;
        return getForceIcons();
    }

    /**
     * Re-reads MekHQ's award icon folder and returns the updated
     * DirectoryItems object. This will update the DirectoryItems object
     * with changes to the award icons (like added image files and folders)
     * while MekHQ is running.
     *
     * @see #getAwardIcons()
     */
    public static DirectoryItems refreshAwardIcons() {
        parseAwardIconDirectory = true;
        return getAwardIcons();
    }
    //endregion Refreshers
}
