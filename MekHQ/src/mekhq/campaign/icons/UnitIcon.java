/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
import org.w3c.dom.NodeList;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;

/**
 * Unit Icon is an implementation of StandardForceIcon that permits a null
 * filename, thereby
 * allowing it to purposefully return a null image when a unit icon is absent.
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
