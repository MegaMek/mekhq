/*
 * CargoReport.java
 *
 * Copyright (c) 2013 - Jay Lawson <jaylawson39 at yahoo.com>. All Rights Reserved.
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
package mekhq.campaign.report;

import mekhq.campaign.Campaign;
import mekhq.campaign.unit.CargoStatistics;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.campaign.unit.Unit;

/**
 * @author Jay Lawson
 */
public class CargoReport extends AbstractReport {
    //region Constructors
    public CargoReport(final Campaign campaign) {
        super(campaign);
    }
    //endregion Constructors

    public String getCargoDetails() {
        final CargoStatistics cargoStats = getCampaign().getCargoStatistics();
        final HangarStatistics hangarStats = getCampaign().getHangarStatistics();

        final double ccc = cargoStats.getTotalCombinedCargoCapacity();
        final double gcc = cargoStats.getTotalCargoCapacity();
        final double icc = cargoStats.getTotalInsulatedCargoCapacity();
        final double lcc = cargoStats.getTotalLiquidCargoCapacity();
        final double scc = cargoStats.getTotalLivestockCargoCapacity();
        final double rcc = cargoStats.getTotalRefrigeratedCargoCapacity();
        final double tonnage = cargoStats.getCargoTonnage(false);
        final double mothballedTonnage = cargoStats.getCargoTonnage(false, true);
        final int mothballedUnits = hangarStats.getNumberOfUnitsByType(Unit.ETYPE_MOTHBALLED);
        final double combined = tonnage + mothballedTonnage;
        final double transported = Math.min(combined, ccc);
        final double overage = combined - transported;

        return resources.getString("CargoReport.Cargo.text")
                + String.format(resources.getString("CargoReport.TotalCapacity.text"), ccc)
                + String.format(resources.getString("CargoReport.GeneralCapacity.text"), gcc)
                + String.format(resources.getString("CargoReport.InsulatedCapacity.text"), icc)
                + String.format(resources.getString("CargoReport.LiquidCapacity.text"), lcc)
                + String.format(resources.getString("CargoReport.LivestockCapacity.text"), scc)
                + String.format(resources.getString("CargoReport.RefrigeratedCapacity.text"), rcc)
                + String.format(resources.getString("CargoReport.CargoTransported.text"), tonnage)
                + String.format(resources.getString("CargoReport.MothballedCargo.text"), mothballedUnits, mothballedTonnage)
                + String.format(resources.getString("CargoReport.CargoTransportedVersusCapacity.text"), transported, ccc)
                + String.format(resources.getString("CargoReport.UntransportedOverage.text"), overage);
    }
}
