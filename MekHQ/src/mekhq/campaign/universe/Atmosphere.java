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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

public enum Atmosphere {
    NONE("None"), 
    TAINTEDPOISON("Tainted (Poisonous)"), TAINTEDCAUSTIC("Tainted (Caustic)"), TAINTEDFLAME("Tainted (Flammable)"),
    TOXICPOISON("Toxic (Poisonous)"), TOXICCAUSTIC("Toxic (Caustic)"), TOXICFLAME("Toxic (Flammable)"),
    BREATHABLE("Breathable");
    
    // For old life form data
    public static Atmosphere parseAtmosphere(String val) {
        switch(val.toLowerCase()) {
            case "tainted (poisonous)": return TAINTEDPOISON;
            case "tainted (caustic)": return TAINTEDCAUSTIC;
            case "tainted (flammable)": return TAINTEDFLAME;
            case "toxic (poisonous)": return TOXICPOISON;
            case "toxic (caustic)": return TOXICCAUSTIC;
            case "toxic (flammable)": return TOXICFLAME;
            case "breathable": return BREATHABLE;
            default: return NONE;
        }
    }
    
    public final String name;
    
    private Atmosphere(String name) {
        this.name = name;
    }
}