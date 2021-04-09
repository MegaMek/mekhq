/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package mekhq.campaign.universe.generators.partGenerators;

import mekhq.campaign.parts.Part;
import mekhq.campaign.universe.enums.PartGenerationMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * The Rules for this Generator:
 * 1) Start with Triple, then cut it down.
 * Remove all Engines.
 * All Heat Sinks are capped at 30 per type.
 * All Mech Heads [Sensors, Life Support] are capped at 2 per weight/type.
 * All Gyros are capped at 1 per weight/type.
 * MASC is capped at 1 per type
 * Any other parts are capped at 6.
 */
public class MishraPartGenerator extends MultiplePartGenerator {
    //region Constructors
    public MishraPartGenerator() {
        super(PartGenerationMethod.MISHRA, 3);
    }
    //endregion Constructors

    @Override
    public List<Part> generate(final List<Part> inputParts) {
        return super.generate(inputParts);
    }
}
