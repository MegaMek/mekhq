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
package mekhq.campaign.universe;

public enum Atmosphere {
    NONE("None"),
    TAINTED_POISON("Tainted (Poisonous)"),
    TAINTED_CAUSTIC("Tainted (Caustic)"),
    TAINTED_FLAME("Tainted (Flammable)"),
    TOXIC_POISON("Toxic (Poisonous)"),
    TOXIC_CAUSTIC("Toxic (Caustic)"),
    TOXIC_FLAME("Toxic (Flammable)"),
    BREATHABLE("Breathable"),
    // Pre <50.07 enums. Removing these will break player customized systems
    @Deprecated(since = "50.07", forRemoval = false)
    TAINTEDPOISON("Tainted (Poisonous)"),
    @Deprecated(since = "50.07", forRemoval = false)
    TAINTEDCAUSTIC("Tainted (Caustic)"),
    @Deprecated(since = "50.07", forRemoval = false)
    TAINTEDFLAME("Tainted (Flammable)"),
    @Deprecated(since = "50.07", forRemoval = false)
    TOXICPOISON("Toxic (Poisonous)"),
    @Deprecated(since = "50.07", forRemoval = false)
    TOXICCAUSTIC("Toxic (Caustic)"),
    @Deprecated(since = "50.07", forRemoval = false)
    TOXICFLAME("Toxic (Flammable)");

    public final String name;

    Atmosphere(String name) {
        this.name = name;
    }

    @Override
    public String toString() {return name;}

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isTaintedPoison() {
        return this == TAINTED_POISON || this == TAINTEDPOISON;
    }

    public boolean isTaintedCaustic() {
        return this == TAINTED_CAUSTIC || this == TAINTEDCAUSTIC;
    }

    public boolean isTaintedFlame() {
        return this == TAINTED_FLAME || this == TAINTEDFLAME;
    }

    public boolean isToxicPoison() {
        return this == TOXIC_POISON || this == TOXICPOISON;
    }

    public boolean isToxicCaustic() {
        return this == TOXIC_CAUSTIC || this == TOXICCAUSTIC;
    }

    public boolean isToxicFlame() {
        return this == TOXIC_FLAME || this == TOXICFLAME;
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
