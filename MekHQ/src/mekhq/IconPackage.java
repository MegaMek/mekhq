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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * This is a convenience class that will keep all the various graphics
 * @author Jay Lawson
 */
public class IconPackage {
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
}
