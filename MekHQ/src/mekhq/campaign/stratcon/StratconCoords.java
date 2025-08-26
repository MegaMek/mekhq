/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.stratcon;

import java.util.Objects;

import megamek.common.board.Coords;

/**
 * Coordinates used in the StratCon map system. Derived from the MegaMek coordinates to make use of the hex math already
 * implemented there
 *
 * @author NickAragua
 */
public class StratconCoords extends Coords {
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

    /**
     * Returns a tabletop bt-like coordinate string
     */
    public String toBTString() {
        return String.format("%02d%02d", getX(), getY());
    }
}
