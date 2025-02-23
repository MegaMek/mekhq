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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

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
