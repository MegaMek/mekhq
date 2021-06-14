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
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Portrait;
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

    private LinkedHashMap<String, List<String>> iconMap = new LinkedHashMap<>();
    //endregion Variable Declarations

    //region Constructors
    public LayeredForceIcon() {
        this(LAYERED_CATEGORY, DEFAULT_ICON_FILENAME);
    }

    public LayeredForceIcon(AbstractIcon icon) {
        this(icon.getCategory(), icon.getFilename());
    }

    public LayeredForceIcon(String category, String filename) {
        super(category, filename);
        createDefaultIconMap();
    }

    public LayeredForceIcon(String category, String filename, LinkedHashMap<String, List<String>> iconMap) {
        super(category, filename);
        setIconMap(iconMap);
    }
    //endregion Constructors

    //region Getters/Setters
    @Override
    public void setFilename(@Nullable String filename) {
        this.filename = (filename == null) ? DEFAULT_FORCE_ICON_FILENAME : filename;
    }

    public LinkedHashMap<String, List<String>> getIconMap() {
        return iconMap;
    }

    public void setIconMap(LinkedHashMap<String, List<String>> iconMap) {
        this.iconMap = iconMap;
    }
    //endregion Getters/Setters

    private void createDefaultIconMap() {
        List<String> frame = new ArrayList<>();
        frame.add("Frame.png");
        iconMap.put(LayeredForceIconLayer.FRAME.getLayerPath(), frame);
    }

    @Override
    public Image getBaseImage() {
        return LAYERED_CATEGORY.equals(getCategory()) ? createLayeredForceIcon() : super.getBaseImage();
    }

    private Image createLayeredForceIcon() {
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
            for (LayeredForceIconEnum layeredForceIcon : LayeredForceIconEnum.getInDrawOrder()) {
                String layer = layeredForceIcon.getLayerPath();
                if (getIconMap().containsKey(layer)) {
                    for (String value : getIconMap().get(layer)) {
                        // Load up the image piece
                        BufferedImage img = (BufferedImage) MHQStaticDirectoryManager.getForceIcons().getItem(layer, value);
                        width = Math.max(img.getWidth(), width);
                        height = Math.max(img.getHeight(), height);
                    }
                }
            }
            base = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration().createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            Graphics2D g2d = base.createGraphics();
            for (LayeredForceIconEnum layeredForceIcon : LayeredForceIconEnum.getInDrawOrder()) {
                String layer = layeredForceIcon.getLayerPath();
                if (getIconMap().containsKey(layer)) {
                    for (String value : getIconMap().get(layer)) {
                        BufferedImage img = (BufferedImage) MHQStaticDirectoryManager.getForceIcons().getItem(layer, value);
                        // Draw the current buffered image onto the base, aligning bottom and right side
                        g2d.drawImage(img, width - img.getWidth() + 1, height - img.getHeight() + 1, null);
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

    public static LayeredForceIcon parseFromXML(final Node wn) {
        final Portrait icon = new Portrait();
        try {
            icon.parseNodes(wn.getChildNodes());
        } catch (Exception e) {
            MegaMek.getLogger().error(e);
            return new Portrait();
        }
        return icon;
    }
    //endregion File I/O

    //region FileIO
    public void writeToXML(PrintWriter pw1, int indent) {
        if (!hasDefaultCategory()) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "iconCategory", getCategory());

            if (LayeredForceIcon.LAYERED_CATEGORY.equals(getCategory())) {
                MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent++, "iconHashMap");
                for (Map.Entry<String, List<String>> entry : getIconMap().entrySet()) {
                    if ((entry.getValue() != null) && !entry.getValue().isEmpty()) {
                        pw1.println(MekHqXmlUtil.indentStr(indent++) + "<iconentry key=\"" + MekHqXmlUtil.escape(entry.getKey()) + "\">");
                        for (String value : entry.getValue()) {
                            pw1.println(MekHqXmlUtil.indentStr(indent) + "<value name=\"" + MekHqXmlUtil.escape(value) + "\"/>");
                        }
                        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "iconentry");
                    }
                }
                MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "iconHashMap");
            }
        }

        if (!hasDefaultFilename()) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "iconFileName", getFilename());
        }
    }

    public void processIconMapNodes(Node wn) {
        NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            List<String> values = wn2.hasChildNodes() ? processIconMapSubNodes(wn2) : null;
            getIconMap().put(wn2.getAttributes().getNamedItem("key").getTextContent(), values);
        }
    }

    private List<String> processIconMapSubNodes(Node wn) {
        List<String> values = new ArrayList<>();
        NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String key = wn2.getAttributes().getNamedItem("name").getTextContent();
            if ((key != null) && !key.isEmpty()) {
                values.add(key);
            }
        }
        return values;
    }
    //endregion FileIO

    @Override
    public LayeredForceIcon clone() {
        LinkedHashMap<String, List<String>> iconMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : getIconMap().entrySet()) {
            if ((entry.getValue() != null) && !entry.getValue().isEmpty()) {
                iconMap.put(entry.getKey(), new ArrayList<>());
                for (String value : entry.getValue()) {
                    iconMap.get(entry.getKey()).add(value);
                }
            }
        }
        return new LayeredForceIcon(getCategory(), getFilename(), iconMap);
    }
}
