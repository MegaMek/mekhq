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
package mekhq.campaign.io.Migration;

import megamek.common.icons.AbstractIcon;
import mekhq.campaign.icons.LayeredForceIcon;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.icons.UnitIcon;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * This migrates Force icons from varied sources to Kailan's Pack
 * This migration occurred in 0.49.3
 */
public class ForceIconMigrator {
    public static void migrateForceIcon(final AbstractIcon icon) {
        if (icon instanceof LayeredForceIcon) {
            //migrateLayeredForceIcon((LayeredForceIcon) icon);
        } else if (icon instanceof UnitIcon) {
            //migrateUnitIcon((UnitIcon) icon);
        } else if (icon instanceof StandardForceIcon) {
            //migrateStandardForceIcon((StandardForceIcon) icon);
        }
    }

    public static void migrateLegacyIconMapNodes(final LayeredForceIcon icon, final Node wn) {
        final NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            if ((wn2.getNodeType() != Node.ELEMENT_NODE) || !wn2.hasChildNodes()) {
                continue;
            }
            final String oldKey = wn2.getAttributes().getNamedItem("key").getTextContent();
            LayeredForceIconLayer key = null;
            for (final LayeredForceIconLayer layer : LayeredForceIconLayer.values()) {
                if (layer.getLayerPath().equalsIgnoreCase(oldKey)) {
                    key = layer;
                    break;
                }
            }

            if (key == null) {
                continue;
            }
            final List<String> values = processIconMapSubNodes(wn2.getChildNodes());
            icon.getIconMap().put(key, values);
        }
    }

    private static List<String> processIconMapSubNodes(final NodeList nl) {
        final List<String> values = new ArrayList<>();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final String value = wn2.getAttributes().getNamedItem("name").getTextContent();
            if ((value != null) && !value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }
}
