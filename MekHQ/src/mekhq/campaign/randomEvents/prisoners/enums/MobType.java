/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
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
