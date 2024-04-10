/*
 * StoryPortrait.java
 *
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.storyarc;

import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Portrait;
import mekhq.MHQStaticDirectoryManager;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;

import java.awt.*;
import java.io.PrintWriter;

/**
 * This class extends the Portrait class to look in the story arc data directory
 * for additional portraits that may be available. It could be extended in other ways in the future
 * if needed.
 */
public class StoryPortrait extends Portrait {

    public static final String XML_TAG = "portrait";

    //region Constructors
    public StoryPortrait() {
        super();
    }

    public StoryPortrait(final @Nullable String category, final @Nullable String filename) {
        super(category, filename);
    }
    //endregion Constructors

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
                //ok lets see if this portrait is in the story arc data
                if (null != MHQStaticDirectoryManager.getUserStoryPortraits()) {
                    portrait = (Image) MHQStaticDirectoryManager.getUserStoryPortraits().getItem(category, filename);
                }
                if (portrait == null) {
                    //if it's still null, then try the default image
                    portrait = (Image) MMStaticDirectoryManager.getPortraits().getItem("",
                            DEFAULT_PORTRAIT_FILENAME);
                }
            }
        } catch (Exception e) {
            LogManager.getLogger().error(e);
        }

        return portrait;
    }
    //region File I/O
    @Override
    public void writeToXML(final PrintWriter pw, final int indent) {
        writeToXML(pw, indent, XML_TAG);
    }

    public static StoryPortrait parseFromXML(final Node wn) {
        final StoryPortrait icon = new StoryPortrait();
        try {
            icon.parseNodes(wn.getChildNodes());
        } catch (Exception e) {
            LogManager.getLogger().error(e);
            return new StoryPortrait();
        }
        return icon;
    }
    //endregion File I/O

    @Override
    public StoryPortrait clone() {
        return new StoryPortrait(getCategory(), getFilename());
    }

}
