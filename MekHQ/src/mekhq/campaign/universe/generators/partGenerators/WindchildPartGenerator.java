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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe.generators.partGenerators;

import java.util.List;

import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.Part;
import mekhq.campaign.universe.enums.PartGenerationMethod;

/**
 * 1 Part for every 3, rounded normally. This means you get 1 part for 2-4 in the input array, plus another for every
 * interval above that.
 *
 * @author Justin "Windchild" Bowen
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
