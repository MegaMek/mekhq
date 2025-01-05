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
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.enums.PartGenerationMethod;
import mekhq.campaign.work.WorkTime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Justin "Windchild" Bowen
 */
public abstract class AbstractPartGenerator {
    //region Variable Declarations
    private final PartGenerationMethod method;
    //endregion Variable Declarations

    //region Constructors
    protected AbstractPartGenerator(final PartGenerationMethod method) {
        this.method = method;
    }
    //endregion Constructors

    //region Getters
    public PartGenerationMethod getMethod() {
        return method;
    }
    //endregion Getters

    /**
     * This generates based on the parts from a list of units, optionally excluding armour and
     * ammunition.
     *
     * @param units the list of units to generate parts based off of
     * @param includeArmour whether to include armour in the parts generated
     * @param includeAmmunition whether to include ammunition in the parts generated
     * @return the generated list of parts
     */
    public List<Part> generate(final List<Unit> units, final boolean includeArmour,
                               final boolean includeAmmunition) {
        final List<Part> parts = new ArrayList<>();
        units.forEach(unit -> unit.getParts().stream()
                .filter(part -> (includeArmour || !(part instanceof Armor))
                        && (includeAmmunition || !(part instanceof AmmoBin)))
                .forEach(parts::add));
        return generate(parts);
    }

    /**
     * @param inputParts a list of parts, which are not guaranteed to be unique, sorted, nor
     *                   unassigned. Implementors are required to clone the parts as required.
     * @return the list of generated parts
     */
    public List<Part> generate(final List<Part> inputParts) {
        final List<Part> parts = generateWarehouse(inputParts).getParts().stream()
                .filter(part -> part.getQuantity() > 0)
                .collect(Collectors.toList());
        parts.forEach(part -> part.setId(0));
        return parts;
    }

    /**
     * @param inputParts a list of parts, which are not guaranteed to be unique, sorted, nor
     *                   unassigned. Implementors are required to clone the parts as required.
     * @return a warehouse containing the generated parts
     */
    public abstract Warehouse generateWarehouse(List<Part> inputParts);

    /**
     * This creates a clone of the input part, with it not being omni-podded if it was originally.
     * @param inputPart the input part to clone
     * @return the cloned part
     */
    protected Part clonePart(final Part inputPart) {
        final Part part = inputPart.clone();
        part.setMode(WorkTime.NORMAL);
        part.setOmniPodded(false);
        return part;
    }
}
