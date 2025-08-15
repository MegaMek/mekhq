/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * This is a convenience class that will keep all the various graphics
 *
 * @author Jay Lawson
 */
public class IconPackage {
    /** A map of keys to various gui elements, for future skinning purposes */
    private final Map<String, String> guiElements = new HashMap<>();

    {
        // Skin defaults
        guiElements.put("default_male_paperdoll", "data/images/misc/paperdoll/default_male.xml");
        guiElements.put("default_female_paperdoll", "data/images/misc/paperdoll/default_female.xml");
    }

    /** A map of resolution widths to file names for the startup screen */
    private final TreeMap<Integer, String> startupScreenImages = new TreeMap<>();

    {
        startupScreenImages.put(370, "data/images/misc/MekHQ Start_hd.png");
        startupScreenImages.put(556, "data/images/misc/MekHQ Start_fhd.png");
        startupScreenImages.put(1112, "data/images/misc/MekHQ Start_uhd.png");
    }

    /** A map of resolution widths to file names for the loading screen */
    private final TreeMap<Integer, String> loadingScreenImages = new TreeMap<>();

    {
        loadingScreenImages.put(370, "data/images/misc/MekHQ Load_hd.png");
        loadingScreenImages.put(556, "data/images/misc/MekHQ Load_fhd.png");
        loadingScreenImages.put(1112, "data/images/misc/MekHQ Load_uhd.png");
    }

    private final TreeMap<Integer, String> autoResolveScreenImages = new TreeMap<>(Map.of(
          0, "data/images/misc/MekHQ AutoResolve.png"));

    public TreeMap<Integer, String> getLoadingScreenImages() {
        return loadingScreenImages;
    }

    public TreeMap<Integer, String> getAutoResolveScreenImages() {
        return autoResolveScreenImages;
    }

    public TreeMap<Integer, String> getStartupScreenImagesScreenImages() {
        return startupScreenImages;
    }

    public IconPackage() {

    }

    public String getGuiElement(String key) {
        return guiElements.get(key);
    }

}
