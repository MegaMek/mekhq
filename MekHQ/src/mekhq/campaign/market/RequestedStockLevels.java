/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The per-part "requested stock percent" targets that a single {@link mekhq.campaign.location.IPlace} maintains for its
 * warehouse.
 *
 * <p>Each value maps a stock key (produced by {@link PartsInUseManager#getStockKey}) to the target percentage of the
 * in-use count that should be kept as spares. A place that has no explicit entry for a part falls through to the
 * campaign-option default. Both the campaign's main force and every player base own their own instance, so stock targets
 * are tracked independently per location.</p>
 */
public final class RequestedStockLevels {
    private static final MMLogger LOGGER = MMLogger.create(RequestedStockLevels.class);

    /**
     * A shared, empty, do-nothing instance returned by any {@link mekhq.campaign.location.IPlace} that does not own real
     * stock levels, so callers never have to deal with {@code null}. Its map is empty and unmodifiable; mutating
     * methods are no-ops, so every part falls through to its default stock percentage.
     */
    public static final RequestedStockLevels NO_RESTOCK = new RequestedStockLevels(true);

    private final Map<String, Double> stockMap;
    private final boolean readOnly;

    public RequestedStockLevels() {
        this(false);
    }

    private RequestedStockLevels(boolean readOnly) {
        this.readOnly = readOnly;
        this.stockMap = readOnly ? Collections.emptyMap() : new LinkedHashMap<>();
    }

    /**
     * Returns the live stock-key to requested-percent map. Callers may mutate it directly (except for
     * {@link #NO_RESTOCK}, whose map is unmodifiable).
     */
    public Map<String, Double> getStockMap() {
        return stockMap;
    }

    public boolean isEmpty() {
        return stockMap.isEmpty();
    }

    public void clear() {
        if (!readOnly) {
            stockMap.clear();
        }
    }

    /**
     * Writes this place's stock levels as a {@code <partInUseMap>} element. Reused by both the campaign and each player
     * base so the save format is identical everywhere.
     */
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "partInUseMap");
        for (Map.Entry<String, Double> entry : stockMap.entrySet()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "partInUseMapEntry");
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "partInUseMapKey", entry.getKey());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "partInUseMapVal", entry.getValue());
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "partInUseMapEntry");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "partInUseMap");
    }

    /**
     * Parses a {@code <partInUseMap>} element into a new {@code RequestedStockLevels}.
     *
     * @param mapNode the {@code <partInUseMap>} node
     *
     * @return a populated instance (never {@code null})
     */
    public static RequestedStockLevels generateInstanceFromXML(final Node mapNode) {
        RequestedStockLevels result = new RequestedStockLevels();
        NodeList entries = mapNode.getChildNodes();
        for (int i = 0; i < entries.getLength(); i++) {
            Node entry = entries.item(i);
            if (entry.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (!entry.getNodeName().equalsIgnoreCase("partInUseMapEntry")) {
                LOGGER.error("Unknown node type not loaded in partInUseMap nodes: {}", entry.getNodeName());
                continue;
            }
            readEntry(result.stockMap, entry);
        }
        return result;
    }

    private static void readEntry(final Map<String, Double> stockMap, final Node entryNode) {
        NodeList fields = entryNode.getChildNodes();
        String key = null;
        double value = 0;
        for (int i = 0; i < fields.getLength(); i++) {
            Node field = fields.item(i);
            if (field.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (field.getNodeName().equalsIgnoreCase("partInUseMapKey")) {
                key = field.getTextContent();
            } else if (field.getNodeName().equalsIgnoreCase("partInUseMapVal")) {
                try {
                    value = Double.parseDouble(field.getTextContent());
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid partInUseMapVal '{}', defaulting to 0", field.getTextContent());
                }
            }
        }
        if (key != null) {
            stockMap.put(key, value);
        }
    }
}
