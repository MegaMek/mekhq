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
 */
package mekhq.campaign.universe;

public enum Atmosphere {
    NONE("None"),
    TAINTEDPOISON("Tainted (Poisonous)"),
    TAINTEDCAUSTIC("Tainted (Caustic)"),
    TAINTEDFLAME("Tainted (Flammable)"),
    TOXICPOISON("Toxic (Poisonous)"),
    TOXICCAUSTIC("Toxic (Caustic)"),
    TOXICFLAME("Toxic (Flammable)"),
    BREATHABLE("Breathable");

    public final String name;

    Atmosphere(String name) {
        this.name = name;
    }

    @Override
    public String toString() { return name; }

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isTaintedPoison() {
        return this == TAINTEDPOISON;
    }

    public boolean isTaintedCaustic() {
        return this == TAINTEDCAUSTIC;
    }

    public boolean isTaintedFlame() {
        return this == TAINTEDFLAME;
    }

    public boolean isToxicPoison() {
        return this == TOXICPOISON;
    }

    public boolean isToxicCaustic() {
        return this == TOXICCAUSTIC;
    }

    public boolean isToxicFlame() {
        return this == TOXICFLAME;
    }

    public boolean isBreathable() {
        return this == BREATHABLE;
    }

    public boolean isTainted() {
        return isTaintedPoison() || isTaintedCaustic() || isTaintedFlame();
    }

    public boolean isToxic() {
        return isToxicPoison() || isToxicCaustic() || isToxicFlame();
    }
}
