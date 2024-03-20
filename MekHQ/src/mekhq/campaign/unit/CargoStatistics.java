/*
 * CargoStatistics.java
 *
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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

package mekhq.campaign.unit;

import megamek.common.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;

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

        cargoTonnage += getCampaign().getWarehouse().streamSpareParts().filter(p -> inTransit || p.isPresent())
                            .mapToDouble(p -> p.getQuantity() * p.getTonnage())
                            .sum();

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
