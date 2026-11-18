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
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.messageSurroundedBySpanWithColor;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;

/**
 * The spares a Repair Bay task can draw on. Every task, whether it repairs, salvages, reloads or replaces armor, writes
 * its stock line through {@link #describe()}, so they all read the same way: {@code Stock: 0 (8 due in 3 days)}, or
 * {@code Stock: 12/40 points} when there is less than the job needs. Salvaging once counted parts still on their way as
 * in stock (#11071).
 *
 * <p>The line does not wrap and the task cell is narrow, so it names the count unit once and puts one thing in its
 * brackets: when parts arrive on different days, or some are due and some ordered, it gives only how many are coming,
 * and {@link #describeDetails()} gives the rest for the task's tooltip.</p>
 *
 * @param inStock   the spares here now and ready to use
 * @param needed    how many the job needs, or {@code 0} when it needs none, as when salvaging
 * @param inventory the spares on their way and on order, when they arrive, and the unit they are counted in, such as
 *                  shots
 */
public record StockCount(int inStock, int needed, PartInventory inventory) {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    /**
     * @return the stock line: how many are in stock against what the job needs, and what is on its way or on order
     */
    public String describe() {
        String countUnit = inventory.getCountModifier();
        String shelf;
        if (inStock == 0) {
            shelf = messageSurroundedBySpanWithColor(getNegativeColor(),
                  getFormattedTextAt(RESOURCE_BUNDLE, "StockCount.none", countUnit));
        } else if (inStock < needed) {
            shelf = messageSurroundedBySpanWithColor(getNegativeColor(),
                  getFormattedTextAt(RESOURCE_BUNDLE, "StockCount.short", inStock, needed, countUnit));
        } else {
            shelf = messageSurroundedBySpanWithColor(getPositiveColor(),
                  getFormattedTextAt(RESOURCE_BUNDLE, "StockCount.inStock", inStock, countUnit));
        }
        String coming = describeComingBriefly();
        if (coming.isEmpty()) {
            return shelf;
        }
        return shelf + messageSurroundedBySpanWithColor(getWarningColor(),
              getFormattedTextAt(RESOURCE_BUNDLE, "StockCount.coming", coming));
    }

    // One thing, without the count unit, which the shelf part already names
    private String describeComingBriefly() {
        int arriving = inventory.getTransit();
        int ordered = inventory.getOrdered();
        if (isLeavingOutDetails()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "StockCount.brief.coming", arriving + ordered);
        } else if (arriving > 0) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "StockCount.brief.arriving", arriving,
                  inventory.getDaysToFirstArrival());
        } else if (ordered > 0) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "StockCount.brief.onOrder", ordered);
        }
        return "";
    }

    // Lots arriving on different days, or some arriving and some ordered, are too much for the line
    private boolean isLeavingOutDetails() {
        boolean isArrivingTogether = inventory.getDaysToFirstArrival() == inventory.getDaysToLastArrival();
        return (inventory.getTransit() > 0) && ((inventory.getOrdered() > 0) || !isArrivingTogether);
    }

    /**
     * Gives what {@link #describe()} leaves out, for the task's tooltip: how many are arriving and when the first
     * arrives, and how many are on order, each on its own line.
     *
     * @return the details, as lines joined by {@code <br>}, or {@code null} when the stock line already says it all
     */
    public @Nullable String describeDetails() {
        if (!isLeavingOutDetails()) {
            return null;
        }
        String countUnit = inventory.getCountModifier();
        List<String> lines = new ArrayList<>();
        // Lots arriving on different days give the soonest, since that is when the job could start
        boolean isArrivingTogether = inventory.getDaysToFirstArrival() == inventory.getDaysToLastArrival();
        String arrivingKey = isArrivingTogether ? "StockCount.arriving" : "StockCount.arrivingFirst";
        lines.add(getFormattedTextAt(RESOURCE_BUNDLE, arrivingKey, inventory.getTransit(), countUnit,
              inventory.getDaysToFirstArrival()));
        if (inventory.getOrdered() > 0) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "StockCount.onOrder", inventory.getOrdered(), countUnit));
        }
        return String.join("<br>", lines);
    }
}
