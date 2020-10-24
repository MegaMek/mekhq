/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.icons.AbstractIcon;
import megamek.common.util.fileUtils.DirectoryItems;
import mekhq.campaign.force.Force;
import mekhq.gui.enums.LayeredForceIcon;
import mekhq.gui.utilities.PortraitFileFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Vector;

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
                        "", PortraitFileFactory.getInstance());
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
                        "", PortraitFileFactory.getInstance());
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

    //region Force Icon
    public static Image buildForceIcon(String category, String filename,
                                       LinkedHashMap<String, Vector<String>> iconMap) {
        Image retVal = null;

        if (AbstractIcon.ROOT_CATEGORY.equals(category)) {
            category = "";
        }

        // Return a null if the player has selected no force icon file.
        if ((null == category) || (null == filename)
                || (AbstractIcon.DEFAULT_ICON_FILENAME.equals(filename) && !Force.ROOT_LAYERED.equals(category))) {
            filename = "empty.png";
        }

        // Layered force icon
        if (Force.ROOT_LAYERED.equals(category)) {
            GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            BufferedImage base = null;
            Graphics2D g2d = null;
            try {
                int width = 0;
                int height = 0;
                // Gather height/width
                for (LayeredForceIcon layeredForceIcon : LayeredForceIcon.getInDrawOrder()) {
                    String layer = layeredForceIcon.getLayerPath();
                    if (iconMap.containsKey(layer)) {
                        for (String value : iconMap.get(layer)) {
                            // Load up the image piece
                            BufferedImage img = (BufferedImage) getForceIcons().getItem(layer, value);
                            width = Math.max(img.getWidth(), width);
                            height = Math.max(img.getHeight(), height);
                        }
                    }
                }
                base = config.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
                g2d = base.createGraphics();
                for (LayeredForceIcon layeredForceIcon : LayeredForceIcon.getInDrawOrder()) {
                    String layer = layeredForceIcon.getLayerPath();
                    if (iconMap.containsKey(layer)) {
                        for (String value : iconMap.get(layer)) {
                            BufferedImage img = (BufferedImage) getForceIcons().getItem(layer, value);
                            // Draw the current buffered image onto the base, aligning bottom and right side
                            g2d.drawImage(img, width - img.getWidth() + 1, height - img.getHeight() + 1, null);
                        }
                    }
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            } finally {
                if (null != g2d) {
                    g2d.dispose();
                }
                if (null == base) {
                    try {
                        base = (BufferedImage) getForceIcons().getItem("", "empty.png");
                    } catch (Exception e) {
                        MekHQ.getLogger().error(e);
                    }
                }
                retVal = base;
            }
        } else { // Standard force icon
            // Try to get the player's force icon file.
            Image scaledImage;
            try {
                scaledImage = (Image) getForceIcons().getItem(category, filename);
                if (null == scaledImage) {
                    scaledImage = (Image) getForceIcons().getItem("", "empty.png");
                }
                retVal = scaledImage;
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }

        return retVal;
    }
    //endregion Force Icon
}
