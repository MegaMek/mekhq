/*
 * Copyright (C) 2019 MegaMek team
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
package mekhq.campaign.universe;

public enum Atmosphere {
    NONE("None"),
    TAINTEDPOISON("Tainted (Poisonous)"), TAINTEDCAUSTIC("Tainted (Caustic)"), TAINTEDFLAME("Tainted (Flammable)"),
    TOXICPOISON("Toxic (Poisonous)"), TOXICCAUSTIC("Toxic (Caustic)"), TOXICFLAME("Toxic (Flammable)"),
    BREATHABLE("Breathable");

    // For old life form data
    public static Atmosphere parseAtmosphere(String val) {
        return switch (val.toLowerCase()) {
            case "tainted (poisonous)" -> TAINTEDPOISON;
            case "tainted (caustic)" -> TAINTEDCAUSTIC;
            case "tainted (flammable)" -> TAINTEDFLAME;
            case "toxic (poisonous)" -> TOXICPOISON;
            case "toxic (caustic)" -> TOXICCAUSTIC;
            case "toxic (flammable)" -> TOXICFLAME;
            case "breathable" -> BREATHABLE;
            default -> NONE;
        };
    }

    public final String name;

    Atmosphere(String name) {
        this.name = name;
    }

    @SuppressWarnings(value = "unused")
    public boolean isNone() {
        return this == NONE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isTaintedPoison() {
        return this == TAINTEDPOISON;
    }

    @SuppressWarnings(value = "unused")
    public boolean isTaintedCaustic() {
        return this == TAINTEDCAUSTIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isTaintedFlame() {
        return this == TAINTEDFLAME;
    }

    @SuppressWarnings(value = "unused")
    public boolean isToxicPoison() {
        return this == TOXICPOISON;
    }

    @SuppressWarnings(value = "unused")
    public boolean isToxicCaustic() {
        return this == TOXICCAUSTIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isToxicFlame() {
        return this == TOXICFLAME;
    }

    @SuppressWarnings(value = "unused")
    public boolean isBreathable() {
        return this == BREATHABLE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isTainted() {
        return isTaintedPoison() || isTaintedCaustic() || isTaintedFlame();
    }

    @SuppressWarnings(value = "unused")
    public boolean isToxic() {
        return isToxicPoison() || isToxicCaustic() || isToxicFlame();
    }
}
