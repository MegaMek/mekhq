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

import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.Part;
import mekhq.campaign.universe.enums.PartGenerationMethod;

import java.util.List;

public class MultiplePartGenerator extends AbstractPartGenerator {
    //region Variable Declarations
    private final int multiple;
    //endregion Variable Declarations

    //region Constructors
    public MultiplePartGenerator(final PartGenerationMethod method, final int multiple) {
        super(method);
        this.multiple = multiple;
    }
    //endregion Constructors

    //region Getters
    public int getMultiple() {
        return multiple;
    }
    //endregion Getters

    @Override
    public Warehouse generateWarehouse(final List<Part> inputParts) {
        final Warehouse warehouse = new Warehouse();
        inputParts.forEach(part -> warehouse.addPart(clonePart(part), true));
        warehouse.forEachPart(part -> part.setQuantity(part.getQuantity() * getMultiple()));
        return warehouse;
    }
}
