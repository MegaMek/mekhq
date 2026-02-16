/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.logging.MMLogger;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.icons.enums.LayeredFormationIconLayer;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * LayeredFormationIcon is an implementation of StandardFormationIcon that contains ForcePieceIcons for the
 * LayeredFormationIconLayer layers. The icons stored are merged in a set order when the base image is drawn, thereby
 * allowing for the creation of a custom Formation Icon from the various Pieces located in the Formation Icon directory's Pieces
 * category.
 *
 * <p>Known as {@code LayeredForceIcon} prior to 0.50.12</p>
 *
 * @see LayeredFormationIconLayer
 * @see FormationPieceIcon
 * @see StandardFormationIcon
 * @see AbstractIcon
 *
 * @since 0.50.12
 */
public class LayeredFormationIcon extends StandardFormationIcon {
    private static final MMLogger logger = MMLogger.create(LayeredFormationIcon.class);

    // region Variable Declarations
    public static final String LAYERED_CATEGORY = "Layered";
    public static final String XML_TAG = "layeredForceIcon";

    private Map<LayeredFormationIconLayer, List<FormationPieceIcon>> pieces = new HashMap<>();
    // endregion Variable Declarations

    // region Constructors
    public LayeredFormationIcon() {
        this(LAYERED_CATEGORY, DEFAULT_ICON_FILENAME);
    }

    public LayeredFormationIcon(final @Nullable String category, final @Nullable String filename) {
        this(category, filename, null);
    }

    public LayeredFormationIcon(final String category, final String filename,
                                final @Nullable Map<LayeredFormationIconLayer, List<FormationPieceIcon>> pieces) {
        super(category, filename);

        if (pieces == null) {
            getPieces().putIfAbsent(LayeredFormationIconLayer.FRAME, new ArrayList<>());
            getPieces().get(LayeredFormationIconLayer.FRAME).add(new FormationPieceIcon());
        } else {
            setPieces(pieces);
        }
    }
    // endregion Constructors

    // region Getters/Setters
    public Map<LayeredFormationIconLayer, List<FormationPieceIcon>> getPieces() {
        return pieces;
    }

    public void setPieces(final Map<LayeredFormationIconLayer, List<FormationPieceIcon>> pieces) {
        this.pieces = pieces;
    }
    // endregion Getters/Setters

    @Override
    public Image getBaseImage() {
        // If we can't create the formation icon directory, return null
        if (MHQStaticDirectoryManager.getFormationIcons() == null) {
            return null;
        }

        // Try to get the player's formation icon file.
        BufferedImage base = null;
        final List<BufferedImage> images = new ArrayList<>();
        int width = 0;
        int height = 0;

        try {
            // Gather height/width
            for (final LayeredFormationIconLayer layer : LayeredFormationIconLayer.getInDrawOrder()) {
                if (!getPieces().containsKey(layer)) {
                    continue;
                }

                for (final FormationPieceIcon value : getPieces().get(layer)) {
                    final BufferedImage image = (BufferedImage) MHQStaticDirectoryManager
                                                                      .getFormationIcons()
                                                                      .getItem(value.getCategoryPath(),
                                                                            value.getFilename());
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

        // Fallback to the default formation icon
        if (base == null) {
            try {
                base = (BufferedImage) MHQStaticDirectoryManager.getFormationIcons().getItem("",
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
        for (final Map.Entry<LayeredFormationIconLayer, List<FormationPieceIcon>> entry : getPieces().entrySet()) {
            if ((entry.getValue() != null) && !entry.getValue().isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, entry.getKey().name());
                for (final FormationPieceIcon value : entry.getValue()) {
                    value.writeToXML(pw, indent);
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, entry.getKey().name());
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "map");
    }

    public static LayeredFormationIcon parseFromXML(final Node wn) {
        final LayeredFormationIcon icon = new LayeredFormationIcon();
        try {
            icon.parseNodes(wn.getChildNodes());
        } catch (Exception ex) {
            logger.error("", ex);
            return new LayeredFormationIcon();
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
            getPieces().put(LayeredFormationIconLayer.valueOf(wn.getNodeName()),
                  processIconMapSubNodes(wn.getChildNodes()));
        }
    }

    private List<FormationPieceIcon> processIconMapSubNodes(final NodeList nl) {
        final List<FormationPieceIcon> pieces = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) {
            final Node wn = nl.item(i);
            if (wn.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final FormationPieceIcon piece = new FormationPieceIcon();
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
        for (final Map.Entry<LayeredFormationIconLayer, List<FormationPieceIcon>> entry : getPieces().entrySet()) {
            stringBuilder.append(entry.getKey()).append(":");
            for (final FormationPieceIcon icon : entry.getValue()) {
                stringBuilder.append(icon).append("/");
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof LayeredFormationIcon) {
            return ((LayeredFormationIcon) other).getPieces().equals(getPieces());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getPieces().hashCode();
    }

    @Override
    public LayeredFormationIcon clone() {
        final Map<LayeredFormationIconLayer, List<FormationPieceIcon>> pieces = new LinkedHashMap<>();
        for (final Map.Entry<LayeredFormationIconLayer, List<FormationPieceIcon>> entry : getPieces().entrySet()) {
            if ((entry.getValue() != null) && !entry.getValue().isEmpty()) {
                pieces.put(entry.getKey(), new ArrayList<>());
                for (final FormationPieceIcon value : entry.getValue()) {
                    pieces.get(entry.getKey()).add(value.clone());
                }
            }
        }
        return new LayeredFormationIcon(getCategory(), getFilename(), pieces);
    }
}
