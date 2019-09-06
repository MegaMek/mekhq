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

public enum LifeForm {
    NONE("None"), MICROBE("Microbes"), PLANT("Plants"), INSECT("Insects"), FISH("Fish"),
    AMPH("Amphibians"), REPTILE("Reptiles"), BIRD("Birds"), MAMMAL("Mammals");
    
    // For old life form data
    public static LifeForm parseLifeForm(String val) {
        switch(val.toLowerCase()) {
            case "microbes": return MICROBE;
            case "plants": return PLANT;
            case "fish": return FISH;
            case "amphibians": return AMPH;
            case "reptiles": return REPTILE;
            case "birds": return BIRD;
            case "mammals": return MAMMAL;
            case "insects": return INSECT;
            default: return NONE;
        }
    }
    
    public final String name;
    
    private LifeForm(String name) {
        this.name = name;
    }
}