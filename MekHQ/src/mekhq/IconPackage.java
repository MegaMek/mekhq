/*
 * Copyright (c) 2013, 2020 - The MegaMek Team. All Rights Reserved
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

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import megamek.client.ui.swing.tileset.MechTileset;
import megamek.client.ui.swing.util.ImageFileFactory;
import megamek.common.Configuration;
import megamek.common.Crew;
import megamek.common.util.DirectoryItems;
import mekhq.campaign.force.Force;
import mekhq.gui.enums.LayeredForceIcon;
import mekhq.gui.utilities.PortraitFileFactory;

/**
 * This is a convenience class that will keep all the various directories and tilesets
 * for tracking graphics and icons
 * @author Jay Lawson
 */
public class IconPackage {
    //the various directory items we need to access
    private DirectoryItems portraits;
    private DirectoryItems camos;
    private DirectoryItems forceIcons;
    private DirectoryItems awardIcons;
    protected static MechTileset mt;

    /** A map of keys to various gui elements, for future skinning purposes */
    private final Map<String, String> guiElements = new HashMap<>();
    {
        // Skin defaults
        guiElements.put("infirmary_background", "data/images/misc/field_hospital.jpg");
        guiElements.put("default_male_paperdoll", "data/images/misc/paperdoll/default_male.xml");
        guiElements.put("default_female_paperdoll", "data/images/misc/paperdoll/default_female.xml");
    }

    /** A map of resolution widths to file names for the startup screen */
    private final TreeMap<Integer, String> startupScreenImages = new TreeMap<>();
    {
        startupScreenImages.put(0, "data/images/misc/MekHQ Start_spooky_hd.png");
        startupScreenImages.put(1441, "data/images/misc/MekHQ Start_spooky_fhd.png");
        startupScreenImages.put(1921, "data/images/misc/MekHQ Start_spooky_uhd.png");
    }

    /** A map of resolution widths to file names for the loading screen */
    private final TreeMap<Integer, String> loadingScreenImages = new TreeMap<>();
    {
        loadingScreenImages.put(0, "data/images/misc/MekHQ Load_spooky_hd.png");
        loadingScreenImages.put(1441, "data/images/misc/MekHQ Load_spooky_fhd.png");
        loadingScreenImages.put(1921, "data/images/misc/MekHQ Load_spooky_uhd.png");
    }

    public IconPackage() {

    }

    public void loadDirectories() {
        if (null == portraits) {
            try {
                portraits = new DirectoryItems(new File("data/images/portraits"), "", //$NON-NLS-1$ //$NON-NLS-2$
                        PortraitFileFactory.getInstance());
            } catch (Exception e) {
                MekHQ.getLogger().error(getClass(), "loadDirectories", "Unable to load portraits Directory", e);
                portraits = null;
            }
        }
        if (null == camos) {
            try {
                camos = new DirectoryItems(new File("data/images/camo"), "", //$NON-NLS-1$ //$NON-NLS-2$
                        ImageFileFactory.getInstance());
            } catch (Exception e) {
                MekHQ.getLogger().error(getClass(), "loadDirectories", "Unable to load camo Directory", e);
                camos = null;
            }
        }
        if (null == forceIcons) {
            try {
                forceIcons = new DirectoryItems(new File("data/images/force"), "", //$NON-NLS-1$ //$NON-NLS-2$
                        PortraitFileFactory.getInstance());
            } catch (Exception e) {
                MekHQ.getLogger().error(getClass(), "loadDirectories", "Unable to load forceIcons Directory", e);
                forceIcons = null;
            }
        }
        if (null == mt) {
            mt = new MechTileset(Configuration.unitImagesDir());
            try {
                mt.loadFromFile("mechset.txt");
            } catch (Exception e) {
                MekHQ.getLogger().error(getClass(), "loadDirectories", "Unable to load mech tileset", e);
            }
        }
        if (null == awardIcons) {
            try {
                awardIcons = new DirectoryItems(new File("data/images/awards"), "", PortraitFileFactory.getInstance());
            } catch (Exception e) {
                MekHQ.getLogger().error(getClass(), "loadDirectories", "Unable to load awardIcons Directory", e);
                awardIcons = null;
            }
        }
    }

    public DirectoryItems getPortraits() {
        return portraits;
    }

    public DirectoryItems getCamos() {
        return camos;
    }

    public DirectoryItems getForceIcons() {
        return forceIcons;
    }

    public DirectoryItems getAwardIcons() { return awardIcons; }

    public MechTileset getMechTiles() {
        return mt;
    }

    public String getGuiElement(String key) {
        return guiElements.get(key);
    }

    /**
     * Gets the name of the startup screen image appropriate for the given horizontal resolution.
     * @param resolutionWidth Screen width
     * @return Path to the appropriate startup screen image.
     */
    public String getStartupScreenImage(int resolutionWidth) {
        return startupScreenImages.floorEntry(resolutionWidth).getValue();
    }

    /**
     * Gets the name of the loading screen image appropriate for the given horizontal resolution.
     * @param resolutionWidth Screen width
     * @return Path to the appropriate loading screen image.
     */
    public String getLoadingScreenImage(int resolutionWidth) {
        return loadingScreenImages.floorEntry(resolutionWidth).getValue();
    }

    public static Image buildForceIcon(String category, String filename, DirectoryItems items, LinkedHashMap<String, Vector<String>> iconMap) {
        final String METHOD_NAME = "buildForceIcon(String,String, DirectoryItems,LinkedHashMap<String,Vector<String>>)"; //$NON-NLS-1$
        Image retVal = null;

        if (Crew.ROOT_PORTRAIT.equals(category)) {
            category = "";
        }

        // Return a null if the player has selected no force icon file.
        if ((null == category) || (null == filename) || (Crew.PORTRAIT_NONE.equals(filename) && !Force.ROOT_LAYERED.equals(category))) {
            filename = "empty.png";
        }

        // Layered force icon
        if (Force.ROOT_LAYERED.equals(category)) {
            GraphicsConfiguration config = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
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
                            BufferedImage img = (BufferedImage) items.getItem(layer, value);
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
                            BufferedImage img = (BufferedImage) items.getItem(layer, value);
                            // Draw the current buffered image onto the base, aligning bottom and right side
                            g2d.drawImage(img, width - img.getWidth() + 1, height - img.getHeight() + 1, null);
                        }
                    }
                }
            } catch (Exception err) {
                MekHQ.getLogger().error(IconPackage.class, METHOD_NAME, err);
            } finally {
                if (null != g2d) {
                    g2d.dispose();
                }
                if (null == base) {
                    try {
                        base = (BufferedImage) items.getItem("", "empty.png");
                    } catch (Exception e) {
                        MekHQ.getLogger().error(IconPackage.class, "buildForceIcon", e);
                    }
                }
                retVal = base;
            }
        } else { // Standard force icon
            // Try to get the player's force icon file.
            Image scaledImage;
            try {
                scaledImage = (Image) items.getItem(category, filename);
                if (null == scaledImage) {
                    scaledImage = (Image) items.getItem("", "empty.png");
                }
                retVal = scaledImage;
            } catch (Exception err) {
                MekHQ.getLogger().error(IconPackage.class, METHOD_NAME, err);
            }
        }

        return retVal;
    }
}
