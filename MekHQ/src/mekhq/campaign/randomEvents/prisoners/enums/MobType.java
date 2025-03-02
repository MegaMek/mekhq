/*
 * Copyright (C) 2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community. BattleMech,
 * BattleTech, and MechWarrior are trademarks of The Topps Company, Inc.
 * The MegaMek organization is not affiliated with The Topps Company, Inc.
 * or Catalyst Game Labs.
 */
package mekhq.campaign.randomEvents.prisoners.enums;

public enum MobType {
    SMALL("Mob (Small)", 1, 5),
    MEDIUM("Mob (Medium)", 6, 10),
    LARGE("Mob (Large)", 11, 20),
    HUGE("Mob (Huge)", 21, 30);

    private final String name;
    private final int minimum;
    private final int maximum;

    /**
     * Constructor for MobType, which assigns attributes to each enum constant.
     *
     * @param name    the name of the mob
     * @param minimum the minimum value associated with the mob
     * @param maximum the maximum value associated with the mob
     */
    MobType(String name, int minimum, int maximum) {
        this.name = name;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    /**
     * Gets the name of this mob type.
     *
     * @return the name of the mob
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the minimum value associated with this mob type.
     *
     * @return the minimum value
     */
    public int getMinimum() {
        return minimum;
    }

    /**
     * Gets the maximum value associated with this mob type.
     *
     * @return the maximum value
     */
    public int getMaximum() {
        return maximum;
    }

    @Override
    public String toString() {
        return String.format("%s (Min: %d, Max: %d)", name, minimum, maximum);
    }
}
