/*
 * Copyright (C) 2016-2025 MegaMek team
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

    public final String name;

    private LifeForm(String name) {
        this.name = name;
    }

    @Override
    public String toString() { return name; }
}
