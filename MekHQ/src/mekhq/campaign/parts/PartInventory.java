/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.parts;

/**
 * Describes the inventory details of a part.
 */
public class PartInventory {
    private int supply;
    private int transit;
    private int ordered;
    private String countModifier = "";

    /**
     * Gets the count of a part on hand.
     * 
     * @return part count on hand.
     */
    public int getSupply() {
        return this.supply;
    }

    /**
     * Sets the count of a part on hand.
     * 
     * @param count count of a part on hand.
     */
    public void setSupply(int count) {
        this.supply = count;
    }

    /**
     * Formats the count of a supply on hand as a String.
     * 
     * @return the count of a part's supply on hand as a String.
     */
    public String supplyAsString() {
        return this.supply + this.countModifier;
    }

    /**
     * Gets the count in transit of a part.
     * 
     * @return the count of a part in transit.
     */
    public int getTransit() {
        return this.transit;
    }

    /**
     * Sets the count in transit of a part.
     * 
     * @param count count in transit of a part.
     */
    public void setTransit(int count) {
        this.transit = count;
    }

    /**
     * Formats the count in transit of a part as a String.
     * 
     * @return the count in transit of a part as a String.
     */
    public String transitAsString() {
        return this.transit + this.countModifier;
    }

    /**
     * Gets the count ordered of a part.
     * 
     * @return count ordered of a part.
     */
    public int getOrdered() {
        return this.ordered;
    }

    /**
     * Sets the count ordered of a part.
     * 
     * @param count count ordered of a part.
     */
    public void setOrdered(int count) {
        this.ordered = count;
    }

    /**
     * Formats the count ordered of a part as a String.
     * 
     * @return count ordered of a part as a String.
     */
    public String orderedAsString() {
        return this.ordered + this.countModifier;
    }

    /**
     * Gets the modifier to display next to a count when formatted as a String.
     * 
     * @return modifier displayed next to a count when formatted as a String.
     */
    public String getCountModifier() {
        return this.countModifier;
    }

    /**
     * Sets the modifier to display next to a count when formatted as a String.
     * 
     * @param countModifier modifier to display next to a count when formatted as a
     *                      String.
     */
    public void setCountModifier(String countModifier) {
        if (countModifier != null && !countModifier.isBlank()) {
            this.countModifier = " " + countModifier;
        }
    }

    /**
     * Gets the transit and ordered counts formatted as a String.
     * 
     * @return A String like, <code>&quot;XXX in transit, YYY on order&quot;</code>,
     *         describing
     *         the transit and ordered counts.
     * @see #transitAsString()
     * @see #orderedAsString()
     */
    public String getTransitOrderedDetails() {
        return transitAsString() + " in transit, " + orderedAsString() + " on order";
    }
}
