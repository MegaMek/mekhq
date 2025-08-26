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

import megamek.common.units.Entity;
import megamek.common.units.FighterSquadron;
import megamek.common.equipment.GunEmplacement;
import megamek.common.units.Jumpship;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.parts.Part;

/**
 * Provides methods to gather statistics on cargo in a campaign.
 */
public class CargoStatistics {
    private Campaign campaign;

    public CargoStatistics(Campaign campaign) {
        this.campaign = campaign;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public Hangar getHangar() {
        return getCampaign().getHangar();
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
        return getHangar().getUnitsStream()
                     .mapToDouble(Unit::getCargoCapacity)
                     .sum();
    }

    // Liquid not included
    public double getTotalCombinedCargoCapacity() {
        return getTotalCargoCapacity() + getTotalLivestockCargoCapacity()
                     + getTotalInsulatedCargoCapacity() + getTotalRefrigeratedCargoCapacity();
    }

    public double getCargoTonnage(boolean inTransit) {
        return getCargoTonnage(inTransit, false);
    }

    @SuppressWarnings("unused") // FIXME: This whole method needs re-worked once Dropship Assignments are in
    public double getCargoTonnage(final boolean inTransit, final boolean mothballed) {
        HangarStatistics stats = getCampaign().getHangarStatistics();

        double cargoTonnage = 0;
        double mothballedTonnage = 0;

        // if we're in transit or the part is present and has a meaningful tonnage, accumulate it
        // not sure what the "in transit" flag is for, but I'm leaving it to retain current behavior
        for (Part part : getCampaign().getWarehouse().getSpareParts()) {
            if ((inTransit || part.isPresent()) && !Double.isNaN(part.getTonnage())) {
                cargoTonnage += part.getQuantity() * part.getTonnage();
            }
        }

        // place units in bays
        // FIXME: This has been temporarily disabled. It really needs DropShip assignments done to fix it correctly.
        // Remaining units go into cargo
        for (Unit unit : getHangar().getUnits()) {
            if (!inTransit && !unit.isPresent()) {
                continue;
            }
            Entity en = unit.getEntity();
            if (unit.isMothballed()) {
                mothballedTonnage += en.getWeight();
                continue;
            }
            if (en instanceof GunEmplacement || en instanceof FighterSquadron || en instanceof Jumpship) {
                continue;
            }
            // cargoTonnage += en.getWeight();
        }
        if (mothballed) {
            return mothballedTonnage;
        }
        return cargoTonnage;
    }
}
