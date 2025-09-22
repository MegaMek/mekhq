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
    BREATHABLE("Breathable");

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
        return this == TAINTED_POISON;
    }

    public boolean isTaintedCaustic() {
        return this == TAINTED_CAUSTIC;
    }

    public boolean isTaintedFlame() {
        return this == TAINTED_FLAME;
    }

    public boolean isToxicPoison() {
        return this == TOXIC_POISON;
    }

    public boolean isToxicCaustic() {
        return this == TOXIC_CAUSTIC;
    }

    public boolean isToxicFlame() {
        return this == TOXIC_FLAME;
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
