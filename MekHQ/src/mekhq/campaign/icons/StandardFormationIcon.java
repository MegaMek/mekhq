/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.icons;

import java.awt.Image;
import java.io.PrintWriter;

import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.logging.MMLogger;
import mekhq.MHQStaticDirectoryManager;
import org.w3c.dom.Node;

/**
 * StandardFormationIcon is an implementation of AbstractIcon that contains and displays a StandardFormationIcon from the Force
 * Icon Directory.
 *
 * <p>Known as {@code StandardForceIcon} prior to 0.50.12</p>
 *
 * @see AbstractIcon
 *
 * @since 0.50.12
 */
public class StandardFormationIcon extends AbstractIcon {
    private static final MMLogger LOGGER = MMLogger.create(StandardFormationIcon.class);

    // region Variable Declarations
    public static final String DEFAULT_FORCE_ICON_FILENAME = "empty.png";
    public static final String XML_TAG = "standardFormationIcon";
    // endregion Variable Declarations

    // region Constructors
    public StandardFormationIcon() {
        this(ROOT_CATEGORY, DEFAULT_FORCE_ICON_FILENAME);
    }

    public StandardFormationIcon(final @Nullable String category, final @Nullable String filename) {
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
        // If we can't create the formation icon directory, return null
        if (MHQStaticDirectoryManager.getFormationIcons() == null) {
            return null;
        }

        final String category = hasDefaultCategory() ? "" : getCategory();
        final String filename = hasDefaultFilename() ? DEFAULT_FORCE_ICON_FILENAME : getFilename();

        // Try to get the player's formation icon file.
        Image formationIcon = null;
        try {
            formationIcon = (Image) MHQStaticDirectoryManager.getFormationIcons().getItem(category, filename);
            if (formationIcon == null) {
                formationIcon = (Image) MHQStaticDirectoryManager.getFormationIcons().getItem("",
                      DEFAULT_FORCE_ICON_FILENAME);
            }
        } catch (Exception ex) {
            LOGGER.error("", ex);
        }

        return formationIcon;
    }

    // region File I/O
    @Override
    public void writeToXML(final PrintWriter pw, final int indent) {
        writeToXML(pw, indent, XML_TAG);
    }

    public static StandardFormationIcon parseFromXML(final Node wn) {
        final StandardFormationIcon icon = new StandardFormationIcon();
        try {
            icon.parseNodes(wn.getChildNodes());
        } catch (Exception ex) {
            LOGGER.error("", ex);
            return new StandardFormationIcon();
        }
        return icon;
    }
    // endregion File I/O

    @Override
    public StandardFormationIcon clone() {
        return new StandardFormationIcon(getCategory(), getFilename());
    }
}
