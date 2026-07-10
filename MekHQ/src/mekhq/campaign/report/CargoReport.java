/*
 * Copyright (c) 2013 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
                     +
                     String.format(resources.getString("CargoReport.TotalCapacity.text"), ccc)
                     +
                     String.format(resources.getString("CargoReport.GeneralCapacity.text"), gcc)
                     +
                     String.format(resources.getString("CargoReport.InsulatedCapacity.text"), icc)
                     +
                     String.format(resources.getString("CargoReport.LiquidCapacity.text"), lcc)
                     +
                     String.format(resources.getString("CargoReport.LivestockCapacity.text"), scc)
                     +
                     String.format(resources.getString("CargoReport.RefrigeratedCapacity.text"), rcc)
                     +
                     String.format(resources.getString("CargoReport.CargoTransported.text"), tonnage)
                     +
                     String.format(resources.getString("CargoReport.MothballedCargo.text"),
                           mothballedUnits,
                           mothballedTonnage)
                     +
                     String.format(resources.getString("CargoReport.CargoTransportedVersusCapacity.text"),
                           transported,
                           ccc)
                     +
                     String.format(resources.getString("CargoReport.UntransportedOverage.text"), overage);
    }
}
