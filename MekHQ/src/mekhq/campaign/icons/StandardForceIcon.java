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
package mekhq.campaign.icons;

import java.awt.Image;
import java.io.PrintWriter;

import org.w3c.dom.Node;

import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.logging.MMLogger;
import mekhq.MHQStaticDirectoryManager;

/**
 * StandardForceIcon is an implementation of AbstractIcon that contains and
 * displays a
 * StandardForceIcon from the Force Icon Directory.
 * 
 * @see AbstractIcon
 */
public class StandardForceIcon extends AbstractIcon {
    private static final MMLogger logger = MMLogger.create(StandardForceIcon.class);

    // region Variable Declarations
    public static final String DEFAULT_FORCE_ICON_FILENAME = "empty.png";
    public static final String XML_TAG = "standardForceIcon";
    // endregion Variable Declarations

    // region Constructors
    public StandardForceIcon() {
        this(ROOT_CATEGORY, DEFAULT_FORCE_ICON_FILENAME);
    }

    public StandardForceIcon(final @Nullable String category, final @Nullable String filename) {
        super(category, filename);
    }
    // endregion Constructors

    // region Boolean Methods
    @Override
    public boolean hasDefaultFilename() {
        return super.hasDefaultFilename() || DEFAULT_FORCE_ICON_FILENAME.equals(getFilename());
    }
    // endregion Boolean Methods

    @Override
    public @Nullable Image getBaseImage() {
        // If we can't create the force icon directory, return null
        if (MHQStaticDirectoryManager.getForceIcons() == null) {
            return null;
        }

        final String category = hasDefaultCategory() ? "" : getCategory();
        final String filename = hasDefaultFilename() ? DEFAULT_FORCE_ICON_FILENAME : getFilename();

        // Try to get the player's force icon file.
        Image forceIcon = null;
        try {
            forceIcon = (Image) MHQStaticDirectoryManager.getForceIcons().getItem(category, filename);
            if (forceIcon == null) {
                forceIcon = (Image) MHQStaticDirectoryManager.getForceIcons().getItem("",
                        DEFAULT_FORCE_ICON_FILENAME);
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }

        return forceIcon;
    }

    // region File I/O
    @Override
    public void writeToXML(final PrintWriter pw, final int indent) {
        writeToXML(pw, indent, XML_TAG);
    }

    public static StandardForceIcon parseFromXML(final Node wn) {
        final StandardForceIcon icon = new StandardForceIcon();
        try {
            icon.parseNodes(wn.getChildNodes());
        } catch (Exception ex) {
            logger.error("", ex);
            return new StandardForceIcon();
        }
        return icon;
    }
    // endregion File I/O

    @Override
    public StandardForceIcon clone() {
        return new StandardForceIcon(getCategory(), getFilename());
    }
}
