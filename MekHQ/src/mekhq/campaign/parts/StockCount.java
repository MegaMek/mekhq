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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.messageSurroundedBySpanWithColor;

import java.util.ArrayList;
import java.util.List;

/**
 * The spares a Repair Bay task can draw on. Every task, whether it repairs, salvages, reloads or replaces armor, writes
 * its stock line through {@link #describe()}, so they all read the same way: {@code None in stock (8 arriving in 3
 * days)}, or {@code 12 of 40 points in stock} when there is less than the job needs. Salvaging once counted parts
 * still on their way as in stock (#11071).
 *
 * @param inStock   the spares here now and ready to use
 * @param needed    how many the job needs, or {@code 0} when it needs none, as when salvaging
 * @param inventory the spares on their way and on order, when they arrive, and the unit they are counted in, such as
 *                  shots
 */
public record StockCount(int inStock, int needed, PartInventory inventory) {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    /**
     * @return the stock line: how many are in stock against what the job needs, and how many are arriving or on order
     */
    public String describe() {
        String countUnit = inventory.getCountModifier();
        String shelf;
        if (inStock == 0) {
            shelf = messageSurroundedBySpanWithColor(getNegativeColor(),
                  getTextAt(RESOURCE_BUNDLE, "StockCount.none"));
        } else if (inStock < needed) {
            shelf = messageSurroundedBySpanWithColor(getNegativeColor(),
                  getFormattedTextAt(RESOURCE_BUNDLE, "StockCount.short", inStock, needed, countUnit));
        } else {
            shelf = messageSurroundedBySpanWithColor(getPositiveColor(),
                  getFormattedTextAt(RESOURCE_BUNDLE, "StockCount.inStock", inStock, countUnit));
        }
        String coming = describeComing(countUnit);
        if (coming.isEmpty()) {
            return shelf;
        }
        return shelf + messageSurroundedBySpanWithColor(getWarningColor(),
              getFormattedTextAt(RESOURCE_BUNDLE, "StockCount.coming", coming));
    }

    private String describeComing(String countUnit) {
        List<String> coming = new ArrayList<>();
        if (inventory.getTransit() > 0) {
            // Lots arriving on different days give the soonest, since that is when the job could start
            boolean isArrivingTogether = inventory.getDaysToFirstArrival() == inventory.getDaysToLastArrival();
            String arrivingKey = isArrivingTogether ? "StockCount.arriving" : "StockCount.arrivingFirst";
            coming.add(getFormattedTextAt(RESOURCE_BUNDLE, arrivingKey, inventory.getTransit(), countUnit,
                  inventory.getDaysToFirstArrival()));
        }
        if (inventory.getOrdered() > 0) {
            coming.add(getFormattedTextAt(RESOURCE_BUNDLE, "StockCount.onOrder", inventory.getOrdered(), countUnit));
        }
        return String.join(", ", coming);
    }
}
