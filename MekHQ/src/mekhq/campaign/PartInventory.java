/*
 * PartInventiry.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;

/**
 *
 * @author natit
 * Keeps track of how many of a given part type are owned
 */
public class PartInventory {

    private int quantity;
    private Part part;

    public PartInventory (Part part, int quantity) {
        this.part = part;
        this.quantity = quantity;
    }

    public Part getPart() {
        return part;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getDescHTML() {
        String toReturn = "<html>";
        toReturn += part.getName();
        toReturn += "<font size='2'>";

        if (part instanceof Armor) {
            toReturn += "Quantity : " +  ((Armor)getPart()).getAmount() + "<br/>";
        }
        else {
            toReturn += "Quantity : " +  getQuantity() + "<br/>";
        }

        toReturn += "</font>";
        toReturn += "</html>";
        return toReturn;
    }

    @Override
    public boolean equals(Object obj) {
        return ((obj instanceof PartInventory)
                && (((PartInventory) obj).getPart().isSamePartTypeAndStatus(getPart()))
                );
    }

    public void addOnePart () {
        this.quantity ++;
    }
    
}
