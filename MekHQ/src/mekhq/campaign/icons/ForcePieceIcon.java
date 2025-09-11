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
import mekhq.MHQConstants;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;

/**
 * ForcePieceIcon is an implementation of StandardForceIcon that contains and displays a Force Icon Piece from the Force
 * Icon Directory's Pieces Subdirectory. These are then combined to form a single LayeredForceIcon.
 *
 * @see LayeredForceIconLayer
 * @see LayeredForceIcon
 * @see StandardForceIcon
 */
public class ForcePieceIcon extends StandardForceIcon {
    private static final MMLogger logger = MMLogger.create(ForcePieceIcon.class);

    // region Variable Declarations
    public static final String XML_TAG = "forcePieceIcon";
    private LayeredForceIconLayer layer;
    // endregion Variable Declarations

    // region Constructors
    public ForcePieceIcon() {
        this(LayeredForceIconLayer.FRAME, ROOT_CATEGORY, MHQConstants.LAYERED_FORCE_ICON_DEFAULT_FRAME_FILENAME);
    }

    public ForcePieceIcon(final LayeredForceIconLayer layer, final @Nullable String category,
          final @Nullable String filename) {
        super(category, filename);
        setLayer(layer);
    }
    // endregion Constructors

    // region Getters/Setters
    public LayeredForceIconLayer getLayer() {
        return layer;
    }

    public void setLayer(final LayeredForceIconLayer layer) {
        this.layer = layer;
    }

    public String getCategoryPath() {
        return hasDefaultCategory() ? getLayer().getLayerPath() : getLayer().getLayerPath() + getCategory();
    }
    // endregion Getters/Setters

    @Override
    public @Nullable Image getBaseImage() {
        // If we can't create the force icon directory, return null
        if (MHQStaticDirectoryManager.getForceIcons() == null) {
            return null;
        }

        try {
            return (Image) MHQStaticDirectoryManager.getForceIcons().getItem(getCategoryPath(), getFilename());
        } catch (Exception ex) {
            logger.error("", ex);
            return null;
        }
    }

    // region File I/O
    @Override
    public void writeToXML(final PrintWriter pw, final int indent) {
        writeToXML(pw, indent, XML_TAG);
    }

    @Override
    protected void writeBodyToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "layer", getLayer().name());
        super.writeBodyToXML(pw, indent);
    }

    public static ForcePieceIcon parseFromXML(final Node wn) {
        final ForcePieceIcon icon = new ForcePieceIcon();
        try {
            icon.parseNodes(wn.getChildNodes());
        } catch (Exception ex) {
            logger.error("", ex);
            return new ForcePieceIcon();
        }
        return icon;
    }

    @Override
    protected void parseNode(final Node wn) {
        super.parseNode(wn);

        if ("layer".equals(wn.getNodeName())) {
            setLayer(LayeredForceIconLayer.valueOf(wn.getTextContent().trim()));
        }
    }
    // endregion File I/O

    @Override
    public String toString() {
        return hasDefaultCategory() ? getLayer().getLayerPath() + getFilename()
                     : getLayer().getLayerPath() + getCategory() + getFilename();
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (other == null) {
            return false;
        } else if (this == other) {
            return true;
        } else if (other instanceof ForcePieceIcon dOther) {
            return (dOther.getLayer() == getLayer()) && dOther.getCategory().equals(getCategory())
                         && dOther.getFilename().equals(getFilename());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (getLayer().name() + getCategory() + getFilename()).hashCode();
    }

    @Override
    public ForcePieceIcon clone() {
        return new ForcePieceIcon(getLayer(), getCategory(), getFilename());
    }
}
