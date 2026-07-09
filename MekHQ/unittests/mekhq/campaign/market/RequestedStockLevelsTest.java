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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class RequestedStockLevelsTest {

    private static RequestedStockLevels roundTrip(RequestedStockLevels source) throws Exception {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            source.writeToXML(pw, 0);
        }
        String xml = sw.toString();

        Document document = MHQXMLUtility.newSafeDocumentBuilder()
                                  .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        Element root = document.getDocumentElement();
        assertEquals("partInUseMap", root.getNodeName());
        return RequestedStockLevels.generateInstanceFromXML(root);
    }

    @Test
    void xmlRoundTripPreservesEntriesAndOrder() throws Exception {
        RequestedStockLevels original = new RequestedStockLevels();
        original.getStockMap().put("Large LaserIS", 50.0);
        original.getStockMap().put("Medium LaserClan", 125.0);
        original.getStockMap().put("SRM 6IS", 0.0);

        RequestedStockLevels restored = roundTrip(original);

        assertEquals(original.getStockMap(), restored.getStockMap());
        // LinkedHashMap ordering must survive the round trip.
        assertEquals(new ArrayList<>(original.getStockMap().keySet()),
              new ArrayList<>(restored.getStockMap().keySet()));
    }

    @Test
    void emptyLevelsRoundTripToEmpty() throws Exception {
        RequestedStockLevels restored = roundTrip(new RequestedStockLevels());
        assertTrue(restored.isEmpty());
    }

    @Test
    void noRestockMapIsUnmodifiable() {
        Map<String, Double> map = RequestedStockLevels.NO_RESTOCK.getStockMap();
        assertThrows(UnsupportedOperationException.class, () -> map.put("Anything", 100.0));
        assertTrue(RequestedStockLevels.NO_RESTOCK.isEmpty());
    }
}
