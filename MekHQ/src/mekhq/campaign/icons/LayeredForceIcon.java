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

import megamek.MegaMek;
import megamek.common.annotations.Nullable;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LayeredForceIcon extends StandardForceIcon {
    //region Variable Declarations
    private static final long serialVersionUID = -2366003293807482568L;

    public static final String LAYERED_CATEGORY = "Layered";
    public static final String XML_TAG = "layeredForceIcon";

    private Map<LayeredForceIconLayer, List<ForcePieceIcon>> pieces = new LinkedHashMap<>();
    //endregion Variable Declarations

    //region Constructors
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
    //endregion Constructors

    //region Getters/Setters
    public Map<LayeredForceIconLayer, List<ForcePieceIcon>> getPieces() {
        return pieces;
    }

    public void setPieces(final Map<LayeredForceIconLayer, List<ForcePieceIcon>> pieces) {
        this.pieces = pieces;
    }
    //endregion Getters/Setters

    @Override
    public Image getBaseImage() {
        // If we can't create the force icon directory, return null
        if (MHQStaticDirectoryManager.getForceIcons() == null) {
            return null;
        }

        // Try to get the player's force icon file.
        BufferedImage base = null;

        try {
            int width = 0;
            int height = 0;

            // Gather height/width
            for (final LayeredForceIconLayer layer : LayeredForceIconLayer.getInDrawOrder()) {
                if (getPieces().containsKey(layer)) {
                    for (final ForcePieceIcon value : getPieces().get(layer)) {
                        final String categoryPath = value.getCategoryPath();
                        final BufferedImage image = (BufferedImage) MHQStaticDirectoryManager
                                .getForceIcons().getItem(categoryPath, value.getFilename());
                        if (image != null) {
                            width = Math.max(image.getWidth(), width);
                            height = Math.max(image.getHeight(), height);
                        }
                    }
                }
            }

            if ((width > 0) && (height > 0)) {
                base = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                        .getDefaultConfiguration().createCompatibleImage(width, height, Transparency.TRANSLUCENT);

                final Graphics2D g2d = base.createGraphics();
                for (final LayeredForceIconLayer layer : LayeredForceIconLayer.getInDrawOrder()) {
                    if (getPieces().containsKey(layer)) {
                        for (final ForcePieceIcon value : getPieces().get(layer)) {
                            final BufferedImage image = (BufferedImage) MHQStaticDirectoryManager
                                    .getForceIcons().getItem(value.getCategoryPath(), value.getFilename());
                            if (image != null) {
                                // Draw the current buffered image onto the base, aligning bottom and right side
                                g2d.drawImage(image, width - image.getWidth() + 1, height - image.getHeight() + 1, null);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }

        if (base == null) {
            try {
                base = (BufferedImage) MHQStaticDirectoryManager.getForceIcons().getItem("",
                        DEFAULT_FORCE_ICON_FILENAME);
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }

        return base;
    }

    //region File I/O
    @Override
    public void writeToXML(final PrintWriter pw, final int indent) {
        writeToXML(pw, indent, XML_TAG);
    }

    @Override
    protected void writeBodyToXML(final PrintWriter pw, int indent) {
        super.writeBodyToXML(pw, indent);

        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw, indent++, "map");
        for (final Map.Entry<LayeredForceIconLayer, List<ForcePieceIcon>> entry : getPieces().entrySet()) {
            if ((entry.getValue() != null) && !entry.getValue().isEmpty()) {
                MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw, indent++, entry.getKey().name());
                for (final ForcePieceIcon value : entry.getValue()) {
                    value.writeToXML(pw, indent);
                }
                MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, --indent, entry.getKey().name());
            }
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, --indent, "map");
    }

    public static LayeredForceIcon parseFromXML(final Node wn) {
        final LayeredForceIcon icon = new LayeredForceIcon();
        try {
            icon.parseNodes(wn.getChildNodes());
        } catch (Exception e) {
            MegaMek.getLogger().error(e);
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
            final ForcePieceIcon piece = new ForcePieceIcon();
            piece.parseNodes(nl.item(i).getChildNodes());
            pieces.add(piece);
        }
        return pieces;
    }
    //endregion File I/O

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
