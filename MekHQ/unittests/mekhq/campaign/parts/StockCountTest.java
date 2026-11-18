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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * The one stock line every Repair Bay task shows (#11071): what is on the shelf, whether that is short of the job, and
 * what is on its way or on order, each counted on its own.
 */
class StockCountTest {
    private static String plainText(String html) {
        return html.replaceAll("<[^>]*>", "");
    }

    private static PartInventory coming(int inTransit, int onOrder, String unit, int... daysToEachArrival) {
        PartInventory inventory = new PartInventory();
        inventory.setTransit(inTransit);
        inventory.setOrdered(onOrder);
        inventory.setCountModifier(unit);
        for (int daysToArrival : daysToEachArrival) {
            inventory.recordArrival(daysToArrival);
        }
        return inventory;
    }

    @Test
    void nothingOnTheShelfStillSaysWhatIsOnItsWay() {
        StockCount stock = new StockCount(0, 0, coming(8, 0, "", 3));

        assertEquals("Stock: 0 (8 due in 3 days)", plainText(stock.describe()));
    }

    @Test
    void fewerThanTheJobNeedsIsShort() {
        StockCount stock = new StockCount(3, 5, coming(0, 0, ""));

        assertEquals("Stock: 3/5", plainText(stock.describe()));
    }

    @Test
    void enoughForTheJobIsInStock() {
        StockCount stock = new StockCount(5, 5, coming(0, 0, ""));

        assertEquals("Stock: 5", plainText(stock.describe()));
    }

    @Test
    void partsBothArrivingAndOnOrderAreCountedTogether() {
        StockCount stock = new StockCount(0, 1, coming(2, 3, "", 1));

        assertEquals("Stock: 0 (5 coming)", plainText(stock.describe()));
        assertEquals("2 due in 1 day<br>3 ordered", stock.describeDetails());
    }

    @Test
    void theLineThatSaysItAllHasNoDetails() {
        StockCount stock = new StockCount(0, 0, coming(8, 0, "", 3));

        assertNull(stock.describeDetails());
    }

    @Test
    void partsOnlyOnOrderSayHowMany() {
        StockCount stock = new StockCount(0, 1, coming(0, 3, ""));

        assertEquals("Stock: 0 (3 ordered)", plainText(stock.describe()));
        assertNull(stock.describeDetails());
    }

    @Test
    void ammunitionNamesItsUnitOnce() {
        StockCount stock = new StockCount(0, 20, coming(100, 0, "shots", 5));

        assertEquals("Stock: 0 shots (100 due in 5 days)", plainText(stock.describe()));
    }

    @Test
    void ammunitionOnItsWayIsCountedInShots() {
        StockCount stock = new StockCount(8, 20, coming(0, 16, "shots"));

        assertEquals("Stock: 8/20 shots (16 ordered)", plainText(stock.describe()));
    }

    @Test
    void armorOnTheShelfIsCountedInPoints() {
        StockCount stock = new StockCount(12, 0, coming(0, 0, "points"));

        assertEquals("Stock: 12 points", plainText(stock.describe()));
    }

    @Test
    void lotsArrivingOnDifferentDaysGiveTheSoonest() {
        StockCount stock = new StockCount(0, 1, coming(8, 0, "", 5, 2));

        assertEquals("Stock: 0 (8 coming)", plainText(stock.describe()));
        assertEquals("8 due, the first in 2 days", stock.describeDetails());
    }
}
