/*
 * PlanetaryType.java
 *
 * Copyright (c) 2019-2025 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe.enums;

public enum PlanetaryType {
    ASTEROID_BELT("Asteroid Belt"),
    DWARF_TERRESTRIAL("Dwarf Terrestrial"),
    TERRESTRIAL ("Terrestrial"),
    GIANT_TERRESTRIAL("Giant Terrestrial"),
    ICE_GIANT("Ice Giant"),
    GAS_GIANT("Gas Giant");

    public final String name;

    private PlanetaryType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
