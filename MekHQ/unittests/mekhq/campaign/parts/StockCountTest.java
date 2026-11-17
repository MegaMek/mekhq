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

        assertEquals("None in stock (8 arriving in 3 days)", plainText(stock.describe()));
    }

    @Test
    void fewerThanTheJobNeedsIsShort() {
        StockCount stock = new StockCount(3, 5, coming(0, 0, ""));

        assertEquals("3 of 5 in stock", plainText(stock.describe()));
    }

    @Test
    void enoughForTheJobIsInStock() {
        StockCount stock = new StockCount(5, 5, coming(0, 0, ""));

        assertEquals("5 in stock", plainText(stock.describe()));
    }

    @Test
    void partsInTransitAndOnOrderAreListedSeparately() {
        StockCount stock = new StockCount(0, 1, coming(2, 3, "", 1));

        assertEquals("None in stock (2 arriving in 1 day, 3 on order)", plainText(stock.describe()));
    }

    @Test
    void ammunitionOnItsWayIsCountedInShots() {
        StockCount stock = new StockCount(8, 20, coming(0, 16, "shots"));

        assertEquals("8 of 20 shots in stock (16 shots on order)", plainText(stock.describe()));
    }

    @Test
    void armorOnTheShelfIsCountedInPoints() {
        StockCount stock = new StockCount(12, 0, coming(0, 0, "points"));

        assertEquals("12 points in stock", plainText(stock.describe()));
    }

    @Test
    void lotsArrivingOnDifferentDaysGiveTheSoonest() {
        StockCount stock = new StockCount(0, 1, coming(8, 0, "", 5, 2));

        assertEquals("None in stock (8 arriving, the first in 2 days)", plainText(stock.describe()));
    }
}
