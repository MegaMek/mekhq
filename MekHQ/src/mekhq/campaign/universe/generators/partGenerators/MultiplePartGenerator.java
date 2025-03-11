/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.campaign.universe.generators.partGenerators;

import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.Part;
import mekhq.campaign.universe.enums.PartGenerationMethod;

import java.util.List;

/**
 * @author Justin "Windchild" Bowen
 */
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
