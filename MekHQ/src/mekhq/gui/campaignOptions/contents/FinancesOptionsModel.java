/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.contents;

import java.util.Arrays;

import jakarta.annotation.Nonnull;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.enums.FinancialYearDuration;

class FinancesOptionsModel {
    boolean useLoanLimits;
    boolean usePercentageMaintenance;
    boolean useExtendedPartsModifier;
    boolean usePeacetimeCost;
    boolean showPeacetimeCost;
    FinancialYearDuration financialYearDuration;
    boolean newFinancialYearFinancesToCSVExport;
    boolean simulateGrayMonday;
    boolean payForParts;
    boolean payForRepairs;
    boolean payForUnits;
    boolean payForSalaries;
    boolean payForOverhead;
    boolean payForMaintain;
    boolean payForTransport;
    boolean payForRecruitment;
    boolean payForFood;
    boolean payForHousing;
    boolean sellUnits;
    boolean sellParts;
    boolean useTaxes;
    int taxesPercentage;
    boolean useShareSystem;
    boolean sharesForAll;
    int rentedFacilitiesCostHospitalBeds;
    int rentedFacilitiesCostKitchens;
    int rentedFacilitiesCostHoldingCells;
    int rentedFacilitiesCostRepairBays;
    double commonPartPriceMultiplier;
    double innerSphereUnitPriceMultiplier;
    double innerSpherePartPriceMultiplier;
    double clanUnitPriceMultiplier;
    double clanPartPriceMultiplier;
    double mixedTechUnitPriceMultiplier;
    double[] usedPartPriceMultipliers;
    double damagedPartsValueMultiplier;
    double unrepairablePartsValueMultiplier;
    double cancelledOrderRefundMultiplier;

    FinancesOptionsModel(@Nonnull CampaignOptions options) {
        useLoanLimits = options.isUseLoanLimits();
        usePercentageMaintenance = options.isUsePercentageMaintenance();
        useExtendedPartsModifier = options.isUseExtendedPartsModifier();
        usePeacetimeCost = options.isUsePeacetimeCost();
        showPeacetimeCost = options.isShowPeacetimeCost();
        financialYearDuration = options.getFinancialYearDuration();
        newFinancialYearFinancesToCSVExport = options.isNewFinancialYearFinancesToCSVExport();
        simulateGrayMonday = options.isSimulateGrayMonday();
        payForParts = options.isPayForParts();
        payForRepairs = options.isPayForRepairs();
        payForUnits = options.isPayForUnits();
        payForSalaries = options.isPayForSalaries();
        payForOverhead = options.isPayForOverhead();
        payForMaintain = options.isPayForMaintain();
        payForTransport = options.isPayForTransport();
        payForRecruitment = options.isPayForRecruitment();
        payForFood = options.isPayForFood();
        payForHousing = options.isPayForHousing();
        sellUnits = options.isSellUnits();
        sellParts = options.isSellParts();
        useTaxes = options.isUseTaxes();
        taxesPercentage = options.getTaxesPercentage();
        useShareSystem = options.isUseShareSystem();
        sharesForAll = options.isSharesForAll();
        rentedFacilitiesCostHospitalBeds = options.getRentedFacilitiesCostHospitalBeds();
        rentedFacilitiesCostKitchens = options.getRentedFacilitiesCostKitchens();
        rentedFacilitiesCostHoldingCells = options.getRentedFacilitiesCostHoldingCells();
        rentedFacilitiesCostRepairBays = options.getRentedFacilitiesCostRepairBays();
        commonPartPriceMultiplier = options.getCommonPartPriceMultiplier();
        innerSphereUnitPriceMultiplier = options.getInnerSphereUnitPriceMultiplier();
        innerSpherePartPriceMultiplier = options.getInnerSpherePartPriceMultiplier();
        clanUnitPriceMultiplier = options.getClanUnitPriceMultiplier();
        clanPartPriceMultiplier = options.getClanPartPriceMultiplier();
        mixedTechUnitPriceMultiplier = options.getMixedTechUnitPriceMultiplier();
        usedPartPriceMultipliers = Arrays.copyOf(options.getUsedPartPriceMultipliers(),
              options.getUsedPartPriceMultipliers().length);
        damagedPartsValueMultiplier = options.getDamagedPartsValueMultiplier();
        unrepairablePartsValueMultiplier = options.getUnrepairablePartsValueMultiplier();
        cancelledOrderRefundMultiplier = options.getCancelledOrderRefundMultiplier();
    }

    void applyTo(@Nonnull CampaignOptions options) {
        options.setLoanLimits(useLoanLimits);
        options.setUsePercentageMaintenance(usePercentageMaintenance);
        options.setUseExtendedPartsModifier(useExtendedPartsModifier);
        options.setUsePeacetimeCost(usePeacetimeCost);
        options.setShowPeacetimeCost(showPeacetimeCost);
        options.setFinancialYearDuration(financialYearDuration);
        options.setNewFinancialYearFinancesToCSVExport(newFinancialYearFinancesToCSVExport);
        options.setSimulateGrayMonday(simulateGrayMonday);
        options.setPayForParts(payForParts);
        options.setPayForRepairs(payForRepairs);
        options.setPayForUnits(payForUnits);
        options.setPayForSalaries(payForSalaries);
        options.setPayForOverhead(payForOverhead);
        options.setPayForMaintain(payForMaintain);
        options.setPayForTransport(payForTransport);
        options.setPayForRecruitment(payForRecruitment);
        options.setPayForFood(payForFood);
        options.setPayForHousing(payForHousing);
        options.setSellUnits(sellUnits);
        options.setSellParts(sellParts);
        options.setUseTaxes(useTaxes);
        options.setTaxesPercentage(taxesPercentage);
        options.setUseShareSystem(useShareSystem);
        options.setSharesForAll(sharesForAll);
        options.setRentedFacilitiesCostHospitalBeds(rentedFacilitiesCostHospitalBeds);
        options.setRentedFacilitiesCostKitchens(rentedFacilitiesCostKitchens);
        options.setRentedFacilitiesCostHoldingCells(rentedFacilitiesCostHoldingCells);
        options.setRentedFacilitiesCostRepairBays(rentedFacilitiesCostRepairBays);
        options.setCommonPartPriceMultiplier(commonPartPriceMultiplier);
        options.setInnerSphereUnitPriceMultiplier(innerSphereUnitPriceMultiplier);
        options.setInnerSpherePartPriceMultiplier(innerSpherePartPriceMultiplier);
        options.setClanUnitPriceMultiplier(clanUnitPriceMultiplier);
        options.setClanPartPriceMultiplier(clanPartPriceMultiplier);
        options.setMixedTechUnitPriceMultiplier(mixedTechUnitPriceMultiplier);
        for (int i = 0; i < Math.min(options.getUsedPartPriceMultipliers().length,
              usedPartPriceMultipliers.length); i++) {
            options.getUsedPartPriceMultipliers()[i] = usedPartPriceMultipliers[i];
        }
        options.setDamagedPartsValueMultiplier(damagedPartsValueMultiplier);
        options.setUnrepairablePartsValueMultiplier(unrepairablePartsValueMultiplier);
        options.setCancelledOrderRefundMultiplier(cancelledOrderRefundMultiplier);
    }
}
