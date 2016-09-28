/*
 * Copyright (C) 2016 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
package mekhq.campaign.material;

import java.util.Objects;

/** A class representing some amount (in tons) of a specific material */
public final class MaterialStorage {
    private Material material;
    private double amount;
    
    public MaterialStorage(Material material, double amount) {
        this.material = Objects.requireNonNull(material);
        if(amount < 0.0) {
            throw new IllegalArgumentException("Amount has to be more than or equals to zero");
        }
        this.amount = amount;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public double getAmount() {
        return amount;
    }
    
    /** @return How much the amount actually changed */
    public double changeAmount(double diff) {
        double oldAmount = amount;
        amount += diff;
        if(amount < 0.0) {
            amount = 0.0;
        }
        return amount - oldAmount;
    }
}
