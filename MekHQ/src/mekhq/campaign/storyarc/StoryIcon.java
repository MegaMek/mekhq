/*
 * StoryIcon.java
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

import megamek.MegaMek;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import mekhq.MHQStaticDirectoryManager;
import org.w3c.dom.Node;

import java.awt.*;
import java.io.PrintWriter;

public class StoryIcon extends AbstractIcon {

    //region Variable Declarations
    //TODO: We could declare a default image here
    public static final String XML_TAG = "storyIcon";
    //endregion Variable Declarations

    //region Constructors
    public StoryIcon() {
        super();
    }

    public StoryIcon(final @Nullable String category, final @Nullable String filename) {
        super(category, filename);
    }
    //endregion Constructors

    @Override
    public Image getBaseImage() {
        // If we can't create the portrait directory, return null
        if (MHQStaticDirectoryManager.getStoryIcons() == null) {
            return null;
        }

        final String category = hasDefaultCategory() ? "" : getCategory();
        final String filename = hasDefaultFilename() ? DEFAULT_ICON_FILENAME : getFilename();

        // Try to get the player's storyarc file.
        Image storyIcon = null;
        try {
            storyIcon = (Image) MHQStaticDirectoryManager.getStoryIcons().getItem(category, filename);
            if (storyIcon == null) {
                //ok lets see if this image is in the story arc data
                if(null != MHQStaticDirectoryManager.getUserStoryIcons()) {
                    storyIcon = (Image) MHQStaticDirectoryManager.getUserStoryIcons().getItem(category, filename);
                }

                if (storyIcon == null) {
                    //if still null, then try default
                    storyIcon = (Image) MHQStaticDirectoryManager.getStoryIcons().getItem("",
                            DEFAULT_ICON_FILENAME);
                }
            }
        } catch (Exception e) {
            MegaMek.getLogger().error(e);
        }

        return storyIcon;
    }

    //region File I/O
    @Override
    public void writeToXML(PrintWriter pw, int indent) {
        writeToXML(pw, indent, XML_TAG);
    }

    public static StoryIcon parseFromXML(final Node wn) {
        final StoryIcon icon = new StoryIcon();
        try {
            icon.parseNodes(wn.getChildNodes());
        } catch (Exception e) {
            MegaMek.getLogger().error(e);
            return new StoryIcon();
        }
        return icon;
    }
    //endregion File I/O

    @Override
    public StoryIcon clone() {
        return new StoryIcon(getCategory(), getFilename());
    }
}
