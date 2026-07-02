/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

package mekhq.campaign.unit;

import java.util.Collection;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.Part;

/**
 * Provides methods to gather statistics on cargo in a campaign.
 */
public record CargoStatistics(Campaign campaign) {

    public Hangar getHangar() {
        return campaign().getHangar();
    }

    public double getTotalInsulatedCargoCapacity() {
        return getHangar().getUnitsStream()
                     .mapToDouble(Unit::getInsulatedCargoCapacity)
                     .sum();
    }

    public double getTotalRefrigeratedCargoCapacity() {
        return getHangar().getUnitsStream()
                     .mapToDouble(Unit::getRefrigeratedCargoCapacity)
                     .sum();
    }

    public double getTotalLivestockCargoCapacity() {
        return getHangar().getUnitsStream()
                     .mapToDouble(Unit::getLivestockCargoCapacity)
                     .sum();
    }

    public double getTotalLiquidCargoCapacity() {
        return getHangar().getUnitsStream()
                     .mapToDouble(Unit::getLiquidCargoCapacity)
                     .sum();
    }

    public double getTotalCargoCapacity() {
        return getTotalCargoCapacity(getHangar());
    }

    public static double getTotalCargoCapacity(Hangar hangar) {
        // The use of the convoy cargo capacity here is deliberate, we should not be factoring in temporary cargo
        // capacity such as roof racks and lift hoists. Otherwise, every unit added to the campaign roster will
        // increase total cargo capacity exponentially. Cargo units will become redundant, because why would you
        // bother with a cargo unit when every BattleMek adds capacity equal to its weight? - Illiani, December 3rd 2025
        return hangar.getUnitsStream()
                     .mapToDouble(Unit::getCargoCapacityForConvoy)
                     .sum();
    }

    // Liquid not included
    public double getTotalCombinedCargoCapacity() {
        return getTotalCargoCapacity() + getTotalLivestockCargoCapacity()
                     + getTotalInsulatedCargoCapacity() + getTotalRefrigeratedCargoCapacity();
    }

    public double getCargoTonnage(Campaign campaign, boolean inTransit) {
        Collection<Part> parts = campaign.getAllParts();
        Collection<Part> spareParts = Warehouse.getSpareParts(parts);
        return getCargoTonnage(campaign.getAllUnits(), spareParts, inTransit, false);
    }

    public double getCargoTonnage(Campaign campaign, final boolean inTransit,
          final boolean mothballed) {
        Collection<Part> parts = campaign.getAllParts();
        Collection<Part> spareParts = Warehouse.getSpareParts(parts);
        return getCargoTonnage(campaign.getAllUnits(), spareParts, inTransit, mothballed);
    }

    // FIXME: This whole method needs re-worked once Dropship Assignments are in
    public static double getCargoTonnage(final Collection<Unit> hangarContents, final Collection<Part> spareParts,
          final boolean inTransit, final boolean mothballed) {
        double cargoTonnage = 0;
        double mothballedTonnage = 0;

        // if we're in transit or the part is present and has a meaningful tonnage, accumulate it
        // not sure what the "in transit" flag is for, but I'm leaving it to retain current behavior
        for (Part part : spareParts) {
            if ((inTransit || part.isPresent()) && !Double.isNaN(part.getTonnage())) {
                cargoTonnage += part.getQuantity() * part.getTonnage();
            }
        }

        // place units in bays
        // Remaining units go into cargo
        for (Unit unit : hangarContents) {
            if (!inTransit && !unit.isPresent()) {
                continue;
            }
            Entity en = unit.getEntity();
            if (unit.isMothballed()) {
                mothballedTonnage += en.getWeight();
            }
        }
        if (mothballed) {
            return mothballedTonnage;
        }
        return cargoTonnage;
    }
}
