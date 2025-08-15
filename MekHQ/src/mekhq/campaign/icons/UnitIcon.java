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
package mekhq.campaign.icons;

import java.awt.Image;
import java.io.PrintWriter;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Unit Icon is an implementation of StandardForceIcon that permits a null filename, thereby allowing it to purposefully
 * return a null image when a unit icon is absent.
 *
 * @see StandardForceIcon
 */
public class UnitIcon extends StandardForceIcon {
    private static final MMLogger logger = MMLogger.create(UnitIcon.class);

    // region Variable Declarations
    public static final String XML_TAG = "unitIcon";
    // endregion Variable Declarations

    // region Constructors
    public UnitIcon() {
        this(ROOT_CATEGORY, DEFAULT_FORCE_ICON_FILENAME);
    }

    public UnitIcon(final @Nullable String category, final @Nullable String filename) {
        super(category, filename);
    }
    // endregion Constructors

    // region Getters/Setters
    @Override
    public @Nullable String getFilename() {
        return super.getFilename();
    }

    @Override
    public void setFilename(final @Nullable String filename) {
        // We allow filename to be null here to indicate no icons
        this.filename = filename;
    }
    // endregion Getters/Setters

    // region Boolean Methods
    @Override
    public boolean hasDefaultFilename() {
        return DEFAULT_ICON_FILENAME.equals(getFilename())
                     || ((getFilename() != null) && getFilename().isBlank());
    }
    // endregion Boolean Methods

    @Override
    public @Nullable Image getImage(final int width, final int height) {
        if (getFilename() == null) {
            return null;
        }
        final Image image = getBaseImage();
        return (image == null) ? null : super.getImage(image, width, height);
    }

    // region File I/O
    @Override
    public void writeToXML(final PrintWriter pw, final int indent) {
        writeToXML(pw, indent, XML_TAG);
    }

    @Override
    protected void writeBodyToXML(final PrintWriter pw, int indent) {
        if (!hasDefaultCategory()) {
            MMXMLUtility.writeSimpleXMLTag(pw, indent, "category", getCategory());
        }

        if (!hasDefaultFilename()) {
            MMXMLUtility.writeSimpleXMLTag(pw, indent, "filename", (getFilename() == null) ? "null" : getFilename());
        }
    }

    public static UnitIcon parseFromXML(final Node wn) {
        final UnitIcon icon = new UnitIcon();
        try {
            icon.parseNodes(wn.getChildNodes());
        } catch (Exception ex) {
            logger.error("", ex);
            return new UnitIcon();
        }
        return icon;
    }

    @Override
    public void parseNodes(final NodeList nl) {
        super.parseNodes(nl);

        if ("null".equalsIgnoreCase(getFilename())) {
            setFilename(null);
        }
    }
    // endregion File I/O

    @Override
    public UnitIcon clone() {
        return new UnitIcon(getCategory(), getFilename());
    }
}
