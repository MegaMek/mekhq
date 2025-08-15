/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.storyarc;

import java.awt.Image;
import java.io.PrintWriter;

import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.logging.MMLogger;
import mekhq.MHQStaticDirectoryManager;
import org.w3c.dom.Node;

/**
 * Extension of AbstractIcon to handle splash images associated with a StoryDialog
 */
public class StorySplash extends AbstractIcon {
    private static final MMLogger logger = MMLogger.create(StorySplash.class);

    // region Variable Declarations
    // TODO: We could declare a default image here
    public static final String XML_TAG = "storySplash";
    // endregion Variable Declarations

    // region Constructors
    public StorySplash() {
        super();
    }

    public StorySplash(final @Nullable String category, final @Nullable String filename) {
        super(category, filename);
    }
    // endregion Constructors

    @Override
    public Image getBaseImage() {
        // If we can't create the portrait directory, return null
        if (MHQStaticDirectoryManager.getStorySplash() == null) {
            return null;
        }

        final String category = hasDefaultCategory() ? "" : getCategory();
        final String filename = hasDefaultFilename() ? DEFAULT_ICON_FILENAME : getFilename();

        // Try to get the player's storyarc file.
        Image storyIcon = null;
        try {
            storyIcon = (Image) MHQStaticDirectoryManager.getStorySplash().getItem(category, filename);
            if (storyIcon == null) {
                // ok lets see if this image is in the story arc data
                if (null != MHQStaticDirectoryManager.getUserStorySplash()) {
                    storyIcon = (Image) MHQStaticDirectoryManager.getUserStorySplash().getItem(category, filename);
                }

                if (storyIcon == null) {
                    // if still null, then try default
                    storyIcon = (Image) MHQStaticDirectoryManager.getStorySplash().getItem("",
                          DEFAULT_ICON_FILENAME);
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }

        return storyIcon;
    }

    // region File I/O
    @Override
    public void writeToXML(PrintWriter pw, int indent) {
        writeToXML(pw, indent, XML_TAG);
    }

    public static StorySplash parseFromXML(final Node wn) {
        final StorySplash icon = new StorySplash();
        try {
            icon.parseNodes(wn.getChildNodes());
        } catch (Exception e) {
            logger.error(e);
            return new StorySplash();
        }
        return icon;
    }
    // endregion File I/O

    @Override
    public StorySplash clone() {
        return new StorySplash(getCategory(), getFilename());
    }
}
