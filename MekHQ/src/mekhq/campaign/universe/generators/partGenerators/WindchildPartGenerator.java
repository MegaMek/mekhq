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

/**
 * 1 Part for every 3, rounded normally.
 * This means you get 1 part for 2-4 in the input array, plus another for every interval above that.
 */
public class WindchildPartGenerator extends AbstractPartGenerator {
    //region Constructors
    public WindchildPartGenerator() {
        super(PartGenerationMethod.WINDCHILD);
    }
    //endregion Constructors

    @Override
    public Warehouse generateWarehouse(final List<Part> inputParts) {
        final Warehouse warehouse = new Warehouse();
        inputParts.forEach(part -> warehouse.addPart(clonePart(part), true));
        warehouse.forEachPart(part -> part.setQuantity((int) Math.round(part.getQuantity() / 3.0)));
        return warehouse;
    }
}
