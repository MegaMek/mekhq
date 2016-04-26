/*
 * Copyright (C) 2016 MegaMek team
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

public enum Climate {
    // Temperature ranges from Interstellar Operations Beta
    ARCTIC(150, 267, "Arctic"), BOREAL(268, 277, "Boreal"), TEMPERATE(278, 287, "Temperate"),
    WARM(288, 297, "Warm"), TROPICAL(298, 307, "Tropical"), SUPERTROPICAL(308, 317, "Supertropical"),
    HELL(318, 500, "Hellish");
    
    // For old climate data
    public static Climate parseClimate(String val) {
        switch(val) {
            case "0": return ARCTIC;
            case "1": return BOREAL;
            case "2": case "3": return TEMPERATE;
            case "4": return WARM;
            case "5": return TROPICAL;
            default: return Climate.valueOf(val);
        }
    }
    
    public final int minTemp;
    public final int maxTemp;
    public final String climateName;
    
    private Climate(int minTemp, int maxTemp, String climateName) {
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.climateName = climateName;
    }
}