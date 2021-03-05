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

public class TriplePartGenerator extends AbstractPartGenerator {
    //region Constructors
    public TriplePartGenerator() {
        this(PartGenerationMethod.TRIPLE);
    }

    protected TriplePartGenerator(final PartGenerationMethod method) {
        super(method);
    }
    //endregion Constructors

    @Override
    public List<Part> generate(final List<Part> inputParts) {
        final List<Part> parts = new ArrayList<>();
        inputParts.forEach(inputPart -> {
            final Part part = clonePart(inputPart);
            part.setQuantity(part.getQuantity() * 3);
            parts.add(part);
        });
        return parts;
    }
}
