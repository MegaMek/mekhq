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

import megamek.client.ui.tileset.MMStaticDirectoryManager;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Portrait;
import megamek.logging.MMLogger;
import mekhq.MHQStaticDirectoryManager;
import org.w3c.dom.Node;

/**
 * This class extends the Portrait class to look in the story arc data directory for additional portraits that may be
 * available. It could be extended in other ways in the future if needed.
 */
public class StoryPortrait extends Portrait {
    private static final MMLogger logger = MMLogger.create(StoryPortrait.class);

    public static final String XML_TAG = "portrait";

    // region Constructors
    public StoryPortrait() {
        super();
    }

    public StoryPortrait(final @Nullable String category, final @Nullable String filename) {
        super(category, filename);
    }
    // endregion Constructors

    @Override
    public Image getBaseImage() {
        // If we can't create the portrait directory, return null
        if (MMStaticDirectoryManager.getPortraits() == null) {
            return null;
        }

        final String category = hasDefaultCategory() ? "" : getCategory();
        final String filename = hasDefaultFilename() ? DEFAULT_PORTRAIT_FILENAME : getFilename();

        // Try to get the player's portrait file.
        Image portrait = null;
        try {
            portrait = (Image) MMStaticDirectoryManager.getPortraits().getItem(category, filename);
            if (portrait == null) {
                // ok lets see if this portrait is in the story arc data
                if (null != MHQStaticDirectoryManager.getUserStoryPortraits()) {
                    portrait = (Image) MHQStaticDirectoryManager.getUserStoryPortraits().getItem(category, filename);
                }
                if (portrait == null) {
                    // if it's still null, then try the default image
                    portrait = (Image) MMStaticDirectoryManager.getPortraits().getItem("",
                          DEFAULT_PORTRAIT_FILENAME);
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }

        return portrait;
    }

    // region File I/O
    @Override
    public void writeToXML(final PrintWriter pw, final int indent) {
        writeToXML(pw, indent, XML_TAG);
    }

    public static StoryPortrait parseFromXML(final Node wn) {
        final StoryPortrait icon = new StoryPortrait();
        try {
            icon.parseNodes(wn.getChildNodes());
        } catch (Exception e) {
            logger.error(e);
            return new StoryPortrait();
        }
        return icon;
    }
    // endregion File I/O

    @Override
    public StoryPortrait clone() {
        return new StoryPortrait(getCategory(), getFilename());
    }

}
