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

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.logging.MMLogger;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;
import mekhq.utilities.MHQXMLUtility;

/**
 * LayeredForceIcon is an implementation of StandardForceIcon that contains
 * ForcePieceIcons for the
 * LayeredForceIconLayer layers. The icons stored are merged in a set order when
 * the base image is
 * drawn, thereby allowing for the creation of a custom Force Icon from the
 * various Pieces located
 * in the Force Icon directory's Pieces category.
 * 
 * @see LayeredForceIconLayer
 * @see ForcePieceIcon
 * @see StandardForceIcon
 * @see AbstractIcon
 */
public class LayeredForceIcon extends StandardForceIcon {
    private static final MMLogger logger = MMLogger.create(LayeredForceIcon.class);

    // region Variable Declarations
    public static final String LAYERED_CATEGORY = "Layered";
    public static final String XML_TAG = "layeredForceIcon";

    private Map<LayeredForceIconLayer, List<ForcePieceIcon>> pieces = new HashMap<>();
    // endregion Variable Declarations

    // region Constructors
    public LayeredForceIcon() {
        this(LAYERED_CATEGORY, DEFAULT_ICON_FILENAME);
    }

    public LayeredForceIcon(final @Nullable String category, final @Nullable String filename) {
        this(category, filename, null);
    }

    public LayeredForceIcon(final String category, final String filename,
            final @Nullable Map<LayeredForceIconLayer, List<ForcePieceIcon>> pieces) {
        super(category, filename);

        if (pieces == null) {
            getPieces().putIfAbsent(LayeredForceIconLayer.FRAME, new ArrayList<>());
            getPieces().get(LayeredForceIconLayer.FRAME).add(new ForcePieceIcon());
        } else {
            setPieces(pieces);
        }
    }
    // endregion Constructors

    // region Getters/Setters
    public Map<LayeredForceIconLayer, List<ForcePieceIcon>> getPieces() {
        return pieces;
    }

    public void setPieces(final Map<LayeredForceIconLayer, List<ForcePieceIcon>> pieces) {
        this.pieces = pieces;
    }
    // endregion Getters/Setters

    @Override
    public Image getBaseImage() {
        // If we can't create the force icon directory, return null
        if (MHQStaticDirectoryManager.getForceIcons() == null) {
            return null;
        }

        // Try to get the player's force icon file.
        BufferedImage base = null;
        final List<BufferedImage> images = new ArrayList<>();
        int width = 0;
        int height = 0;

        try {
            // Gather height/width
            for (final LayeredForceIconLayer layer : LayeredForceIconLayer.getInDrawOrder()) {
                if (!getPieces().containsKey(layer)) {
                    continue;
                }

                for (final ForcePieceIcon value : getPieces().get(layer)) {
                    final BufferedImage image = (BufferedImage) MHQStaticDirectoryManager
                            .getForceIcons().getItem(value.getCategoryPath(), value.getFilename());
                    if (image != null) {
                        width = Math.max(image.getWidth(), width);
                        height = Math.max(image.getHeight(), height);
                        images.add(image);
                    }
                }
            }

            // If there are valid images to draw
            if ((width > 0) && (height > 0)) {
                base = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                        .getDefaultConfiguration().createCompatibleImage(width, height, Transparency.TRANSLUCENT);

                final Graphics2D g2d = base.createGraphics();
                for (final BufferedImage image : images) {
                    // Draw the current buffered image onto the base, aligning bottom and right side
                    g2d.drawImage(image, width - image.getWidth() + 1, height - image.getHeight() + 1, null);
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }

        // Fallback to the default force icon
        if (base == null) {
            try {
                base = (BufferedImage) MHQStaticDirectoryManager.getForceIcons().getItem("",
                        DEFAULT_FORCE_ICON_FILENAME);
            } catch (Exception ex) {
                logger.error("", ex);
            }
        }

        return base;
    }

    // region File I/O
    @Override
    public void writeToXML(final PrintWriter pw, final int indent) {
        writeToXML(pw, indent, XML_TAG);
    }

    @Override
    protected void writeBodyToXML(final PrintWriter pw, int indent) {
        super.writeBodyToXML(pw, indent);

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "map");
        for (final Map.Entry<LayeredForceIconLayer, List<ForcePieceIcon>> entry : getPieces().entrySet()) {
            if ((entry.getValue() != null) && !entry.getValue().isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, entry.getKey().name());
                for (final ForcePieceIcon value : entry.getValue()) {
                    value.writeToXML(pw, indent);
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, entry.getKey().name());
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "map");
    }

    public static LayeredForceIcon parseFromXML(final Node wn) {
        final LayeredForceIcon icon = new LayeredForceIcon();
        try {
            icon.parseNodes(wn.getChildNodes());
        } catch (Exception ex) {
            logger.error("", ex);
            return new LayeredForceIcon();
        }
        return icon;
    }

    @Override
    protected void parseNode(final Node wn) {
        super.parseNode(wn);
        if ("map".equals(wn.getNodeName())) {
            if (wn.hasChildNodes()) {
                processIconMapNodes(wn.getChildNodes());
            }
        }
    }

    private void processIconMapNodes(final NodeList nl) {
        for (int i = 0; i < nl.getLength(); i++) {
            final Node wn = nl.item(i);
            if ((wn.getNodeType() != Node.ELEMENT_NODE) || !wn.hasChildNodes()) {
                continue;
            }
            getPieces().put(LayeredForceIconLayer.valueOf(wn.getNodeName()),
                    processIconMapSubNodes(wn.getChildNodes()));
        }
    }

    private List<ForcePieceIcon> processIconMapSubNodes(final NodeList nl) {
        final List<ForcePieceIcon> pieces = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) {
            final Node wn = nl.item(i);
            if (wn.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final ForcePieceIcon piece = new ForcePieceIcon();
            piece.parseNodes(wn.getChildNodes());
            pieces.add(piece);
        }
        return pieces;
    }
    // endregion File I/O

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getCategory()).append("/");
        for (final Map.Entry<LayeredForceIconLayer, List<ForcePieceIcon>> entry : getPieces().entrySet()) {
            stringBuilder.append(entry.getKey()).append(":");
            for (final ForcePieceIcon icon : entry.getValue()) {
                stringBuilder.append(icon).append("/");
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof LayeredForceIcon) {
            return ((LayeredForceIcon) other).getPieces().equals(getPieces());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getPieces().hashCode();
    }

    @Override
    public LayeredForceIcon clone() {
        final Map<LayeredForceIconLayer, List<ForcePieceIcon>> pieces = new LinkedHashMap<>();
        for (final Map.Entry<LayeredForceIconLayer, List<ForcePieceIcon>> entry : getPieces().entrySet()) {
            if ((entry.getValue() != null) && !entry.getValue().isEmpty()) {
                pieces.put(entry.getKey(), new ArrayList<>());
                for (final ForcePieceIcon value : entry.getValue()) {
                    pieces.get(entry.getKey()).add(value.clone());
                }
            }
        }
        return new LayeredForceIcon(getCategory(), getFilename(), pieces);
    }
}
