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

import megamek.common.units.Mek;
import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.MekCockpit;
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.MekLifeSupport;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MekSensor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.MASC;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.enums.PartGenerationMethod;

/**
 * The Rules for this Generator: 1) Remove all non-'Mek Units 2) Start with Triple Parts 3) Remove all Engines 3) All
 * Heat Sinks are capped at 30 per type 4) All 'Mek Heads [Sensors, Life Support] are capped at 2 per weight/type 5) All
 * Gyros are capped at 1 per weight/type 6) MASC is capped at 1 per type 7) Any other parts are capped at 6.
 *
 * @author Justin "Windchild" Bowen
 */
public class MishraPartGenerator extends MultiplePartGenerator {
    //region Constructors
    public MishraPartGenerator() {
        super(PartGenerationMethod.MISHRA, 3);
    }
    //endregion Constructors

    @Override
    public List<Part> generate(final List<Unit> units, final boolean includeArmour,
          final boolean includeAmmunition) {
        units.removeIf(unit -> !(unit.getEntity() instanceof Mek));
        return super.generate(units, includeArmour, includeAmmunition);
    }

    @Override
    public Warehouse generateWarehouse(final List<Part> inputParts) {
        final Warehouse warehouse = super.generateWarehouse(inputParts);
        warehouse.getParts().removeIf(part -> part instanceof EnginePart);
        warehouse.forEachPart(part -> {
            if (part instanceof HeatSink) {
                part.setQuantity(Math.min(part.getQuantity(), 30));
            } else if ((part instanceof MekCockpit) || (part instanceof MekLifeSupport)
                             || (part instanceof MekSensor)
                             || ((part instanceof MekLocation) && ((MekLocation) part).getLoc() == Mek.LOC_HEAD)) {
                part.setQuantity(Math.min(part.getQuantity(), 2));
            } else if ((part instanceof MekGyro) || (part instanceof MASC)) {
                part.setQuantity(Math.min(part.getQuantity(), 1));
            } else {
                part.setQuantity(Math.min(part.getQuantity(), 6));
            }
        });
        return warehouse;
    }
}
