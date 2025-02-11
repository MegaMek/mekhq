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

public enum LifeForm {
    NONE("None"), MICROBE("Microbes"), PLANT("Plants"), INSECT("Insects"), FISH("Fish"),
    AMPHIBIAN("Amphibians"), REPTILE("Reptiles"), BIRD("Birds"), MAMMAL("Mammals");

    // For old life form data
    public static LifeForm parseLifeForm(String val) {
        switch (val.toLowerCase()) {
            case "microbe":
            case "microbes":
                return MICROBE;
            case "plant":
            case "plants":
                return PLANT;
            case "fish":
                return FISH;
            case "amphibian":
            case "amphibians":
                return AMPHIBIAN;
            case "reptile":
            case "reptiles":
                return REPTILE;
            case "bird":
            case "birds":
                return BIRD;
            case "mammal":
            case "mammals":
                return MAMMAL;
            case "insect":
            case "insects":
                return INSECT;
            default: return NONE;
        }
    }

    public final String name;

    private LifeForm(String name) {
        this.name = name;
    }

    /** Deserializer for Jackson loading of this enum **/
    public static class LifeFormDeserializer extends StdDeserializer<LifeForm> {

        public LifeFormDeserializer() {
            this(null);
        }

        public LifeFormDeserializer(final Class<?> vc) {
            super(vc);
        }

        @Override
        public LifeForm deserialize(final JsonParser jsonParser, final DeserializationContext context) {
            try {
                return LifeForm.parseLifeForm(jsonParser.getText());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
