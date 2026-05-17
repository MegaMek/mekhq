/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq;

import static mekhq.MHQConstants.AWARDS_IMAGE_DIRECTORY_PATH;

import java.io.File;

import megamek.client.ui.tileset.MMStaticDirectoryManager;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.fileUtils.AbstractDirectory;
import megamek.common.util.fileUtils.DirectoryItems;
import megamek.common.util.fileUtils.ImageFileFactory;
import megamek.logging.MMLogger;
import mekhq.io.AwardFileFactory;

public class MHQStaticDirectoryManager extends MMStaticDirectoryManager {
    private static final MMLogger LOGGER = MMLogger.create(MHQStaticDirectoryManager.class);

    // region Variable Declarations
    private static AbstractDirectory formationIconDirectory;
    private static AbstractDirectory awardIconDirectory;
    private static AbstractDirectory storySplashDirectory;
    private static AbstractDirectory userStorySplashDirectory;
    private static AbstractDirectory userStoryPortraitDirectory;

    // Reparsing Prevention Variables: They are True at startup and when the
    // specified directory
    // should be reparsed, and are used to avoid reparsing the directory
    // repeatedly when there's
    // an error.
    private static boolean parseFormationIconDirectory = true;
    private static boolean parseAwardIconDirectory = true;
    private static boolean parseStorySplashDirectory = true;
    private static boolean parseUserStorySplashDirectory = true;
    private static boolean parseUserStoryPortraitDirectory = true;
    // endregion Variable Declarations

    // region Constructors
    protected MHQStaticDirectoryManager() {
        // This class is not to be instantiated
    }
    // endregion Constructors

    // region Initialization

    /**
     * This initializes all the directories under this manager
     */
    public static void initialize() {
        MMStaticDirectoryManager.initialize();
        initializeFormationIcons();
        initializeAwardIcons();
        initializeStorySplash();
    }

    /**
     * Parses MekHQ's formation icon folder when first called or when it was refreshed.
     *
     * @see #refreshFormationIcons()
     */
    private static void initializeFormationIcons() {
        // Read in and parse MekHQ's formation icon folder only when first called or when
        // refreshed
        if (parseFormationIconDirectory) {
            // Set parseFormationIconDirectory too false to avoid parsing repeatedly when
            // something fails
            parseFormationIconDirectory = false;
            try {
                formationIconDirectory = new DirectoryItems(new File(MHQConstants.FORCE_ICON_PATH), new ImageFileFactory());

                String userDir = PreferenceManager.getClientPreferences().getUserDir();
                File formationIconUserDir = new File(userDir + "/" + MHQConstants.FORCE_ICON_PATH);
                if (!userDir.isBlank() && formationIconUserDir.isDirectory()) {
                    DirectoryItems userDirFormationIcon = new DirectoryItems(formationIconUserDir, new ImageFileFactory());
                    formationIconDirectory.merge(userDirFormationIcon);
                }

            } catch (Exception e) {
                LOGGER.error("Could not parse the formation icon directory!", e);
            }
        }
    }

    /**
     * Parses MekHQ's awards icon folder when first called or when it was refreshed.
     *
     * @see #refreshAwardIcons()
     */
    private static void initializeAwardIcons() {
        // Read in and parse MekHQ's award icon folder only when first called or when
        // refreshed
        if (parseAwardIconDirectory) {
            // Set parseAwardIconDirectory too false to avoid parsing repeatedly when
            // something fails
            parseAwardIconDirectory = false;
            try {
                awardIconDirectory = new DirectoryItems(new File(AWARDS_IMAGE_DIRECTORY_PATH), new AwardFileFactory());

                String userDirectory = PreferenceManager.getClientPreferences().getUserDir();
                File iconUserDirectory = new File(userDirectory + '/' + AWARDS_IMAGE_DIRECTORY_PATH);
                if (!userDirectory.isBlank() && iconUserDirectory.isDirectory()) {
                    DirectoryItems userAwardIcons = new DirectoryItems(iconUserDirectory, new AwardFileFactory());
                    awardIconDirectory.merge(userAwardIcons);
                }
            } catch (Exception e) {
                LOGGER.error("Could not parse the award icon directory!", e);
            }
        }
    }

    /**
     * Parses MekHQ's story arcs icon folder when first called or when it was refreshed.
     *
     * @see #refreshStorySplash()
     */
    private static void initializeStorySplash() {
        // Read in and parse MekHQ's formation icon folder only when first called or when
        // refreshed
        if (parseStorySplashDirectory) {
            // Set parseFormationIconDirectory too false to avoid parsing repeatedly when
            // something fails
            parseStorySplashDirectory = false;
            try {
                File f = new File("data/images/storysplash");
                if (f.exists()) {
                    storySplashDirectory = new DirectoryItems(f, new ImageFileFactory());
                }
            } catch (Exception e) {
                LOGGER.error("Could not parse the storyarc icon directory!", e);
            }
        }
    }

    /**
     * Parses the user's Story Arc portraits directory when first called or when it was refreshed
     */
    public static void initializeUserStoryPortraits(String path) {
        // Read in and parse MekHQ's formation icon folder only when first called or when
        // refreshed
        if (parseUserStoryPortraitDirectory) {
            // Set parseFormationIconDirectory too false to avoid parsing repeatedly when
            // something fails
            parseUserStoryPortraitDirectory = false;
            try {
                File f = new File(path);
                if (f.exists()) {
                    userStoryPortraitDirectory = new DirectoryItems(f, new ImageFileFactory());
                }
            } catch (Exception e) {
                LOGGER.error("Could not parse the storyarc portrait directory!", e);
            }
        }
    }

    /**
     * Parses the user's Story Arc story arcs directory when first called or when it was refreshed
     */
    public static void initializeUserStorySplash(String path) {
        // Read in and parse MekHQ's formation icon folder only when first called or when
        // refreshed
        if (parseUserStorySplashDirectory) {
            // Set parseFormationIconDirectory too false to avoid parsing repeatedly when
            // something fails
            parseUserStorySplashDirectory = false;
            try {
                File f = new File(path);
                if (f.exists()) {
                    userStorySplashDirectory = new DirectoryItems(f, new ImageFileFactory());
                }
            } catch (Exception e) {
                LOGGER.error("Could not parse the story arc splash image directory!", e);
            }
        }
    }
    // endregion Initialization

    // region Getters

    /**
     * Returns an AbstractDirectory object containing all formation icon filenames found in MekHQ's formation icon folder.
     *
     * @return an AbstractDirectory object with the formation icon folders and filenames. May be null if the directory
     *       cannot be parsed.
     */
    public static @Nullable AbstractDirectory getFormationIcons() {
        initializeFormationIcons();
        return formationIconDirectory;
    }

    /**
     * Returns an AbstractDirectory object containing all award icon filenames found in MekHQ's award icon folder.
     *
     * @return an AbstractDirectory object with the award icon folders and filenames. May be null if the directory
     *       cannot be parsed.
     */
    public static @Nullable AbstractDirectory getAwardIcons() {
        initializeAwardIcons();
        return awardIconDirectory;
    }

    /**
     * Returns an AbstractDirectory object containing all story icon filenames found in MekHQ's story arc icon folder.
     *
     * @return an AbstractDirectory object with the story icon folders and filenames. May be null if the directory
     *       cannot be parsed.
     */
    public static @Nullable AbstractDirectory getStorySplash() {
        initializeStorySplash();
        return storySplashDirectory;
    }

    /**
     * Returns an AbstractDirectory object containing all story portrait filenames found in the user's story arc
     * portraits folder.
     *
     * @return an AbstractDirectory object with the story portrait folders and filenames. May be null if the directory
     *       cannot be parsed.
     */
    public static @Nullable AbstractDirectory getUserStoryPortraits() {
        // we do not initialize here because initialization requires a specific path
        return userStoryPortraitDirectory;
    }

    /**
     * Returns an AbstractDirectory object containing all story arc image filenames found in the user's story arc
     * folder.
     *
     * @return an AbstractDirectory object with the story portrait folders and filenames. May be null if the directory
     *       cannot be parsed.
     */
    public static @Nullable AbstractDirectory getUserStorySplash() {
        // we do not initialize here because initialization requires a specific path
        return userStorySplashDirectory;
    }
    // endregion Getters

    // region Refreshers

    /**
     * Re-reads MekHQ's formation icon folder and returns the updated AbstractDirectory object. This will update the
     * AbstractDirectory object with changes to the formation icons (like added image files and folders) while MekHQ is
     * running.
     *
     * @see #getFormationIcons()
     */
    public static AbstractDirectory refreshFormationIcons() {
        parseFormationIconDirectory = true;
        return getFormationIcons();
    }

    /**
     * Re-reads MekHQ's award icon folder and returns the updated AbstractDirectory object. This will update the
     * AbstractDirectory object with changes to the award icons (like added image files and folders) while MekHQ is
     * running.
     *
     * @see #getAwardIcons()
     */
    public static AbstractDirectory refreshAwardIcons() {
        parseAwardIconDirectory = true;
        return getAwardIcons();
    }

    /**
     * Re-reads MekHQ's story icon folder and returns the updated AbstractDirectory object. This will update the
     * AbstractDirectory object with changes to the story icons (like added image files and folders) while MekHQ is
     * running.
     *
     * @see #getStorySplash()
     */
    public static AbstractDirectory refreshStorySplash() {
        parseStorySplashDirectory = true;
        return getStorySplash();
    }
    // endregion Refreshers
}
