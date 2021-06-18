/*
 * Copyright (c) 2019 The Megamek Team. All rights reserved.
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

package mekhq.campaign.stratcon;

import java.util.Objects;

import megamek.common.Coords;

/**
 * Coordinates used in the StratCon map system.
 * Derived from the MegaMek coordinates to make use of the hex math already implemented there
 * @author NickAragua
 */
public class StratconCoords extends Coords {
    private static final long serialVersionUID = 2660132431077309812L;

    /**
     * Create a set of StratCon coordinates at x, y
     */
    public StratconCoords(int x, int y) {
        super(x, y);
    }
    
    /**
     * Create a default set of StratCon coordinates at 0, 0
     */
    public StratconCoords() {
        super(0, 0);
    }

    /**
     * Get a set of StratCon coords translated in the given direction from these coordinates.
     */
    public StratconCoords translate(int direction) {
        Coords coords = translated(direction);
        int y = coords.getY();
        
        if (isXOdd() && (coords.getX() != getX())) {
            y--;
        } else if (!isXOdd() && (coords.getX() != getX())) {
            y++;
        }
        
        return new StratconCoords(coords.getX(), y);
    }
    
    /**
     * Get the hash code for these coords.
     * 
     * @return The <code>int</code> hash code for these coords.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY());
    }
    
    /**
     * Coords are equal if their x and y components are equal
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if ((object == null) || (getClass() != object.getClass())) {
            return false;
        }
        
        StratconCoords other = (StratconCoords) object;
        return (other.getX() == this.getX()) && (other.getY() == this.getY());
    }
    
    @Override
    public String toString() {
        return String.format("%s, %s", getX(), getY());
    }
}
