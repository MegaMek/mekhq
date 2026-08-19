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
import mekhq.campaign.campaignOptions.CampaignOption;
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
    boolean allowMonthlyReinvestment;
    boolean allowMonthlyConnections;
    boolean useBetterExtraIncome;
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
        useLoanLimits = options.get(CampaignOption.USE_LOAN_LIMITS);
        usePercentageMaintenance = options.get(CampaignOption.USE_PERCENTAGE_MAINTENANCE);
        useExtendedPartsModifier = options.get(CampaignOption.USE_EXTENDED_PARTS_MODIFIER);
        usePeacetimeCost = options.get(CampaignOption.USE_PEACETIME_COST);
        showPeacetimeCost = options.get(CampaignOption.SHOW_PEACETIME_COST);
        financialYearDuration = options.get(CampaignOption.FINANCIAL_YEAR_DURATION);
        newFinancialYearFinancesToCSVExport = options.get(CampaignOption.NEW_FINANCIAL_YEAR_FINANCES_TO_CSV_EXPORT);
        simulateGrayMonday = options.get(CampaignOption.SIMULATE_GRAY_MONDAY);
        payForParts = options.get(CampaignOption.PAY_FOR_PARTS);
        payForRepairs = options.get(CampaignOption.PAY_FOR_REPAIRS);
        payForUnits = options.get(CampaignOption.PAY_FOR_UNITS);
        payForSalaries = options.get(CampaignOption.PAY_FOR_SALARIES);
        payForOverhead = options.get(CampaignOption.PAY_FOR_OVERHEAD);
        payForMaintain = options.get(CampaignOption.PAY_FOR_MAINTAIN);
        payForTransport = options.get(CampaignOption.PAY_FOR_TRANSPORT);
        payForRecruitment = options.get(CampaignOption.PAY_FOR_RECRUITMENT);
        payForFood = options.get(CampaignOption.PAY_FOR_FOOD);
        payForHousing = options.get(CampaignOption.PAY_FOR_HOUSING);
        sellUnits = options.get(CampaignOption.SELL_UNITS);
        sellParts = options.get(CampaignOption.SELL_PARTS);
        allowMonthlyReinvestment = options.get(CampaignOption.ALLOW_MONTHLY_REINVESTMENT);
        allowMonthlyConnections = options.get(CampaignOption.ALLOW_MONTHLY_CONNECTIONS);
        useBetterExtraIncome = options.get(CampaignOption.USE_BETTER_EXTRA_INCOME);
        useTaxes = options.get(CampaignOption.USE_TAXES);
        taxesPercentage = options.get(CampaignOption.TAXES_PERCENTAGE);
        useShareSystem = options.get(CampaignOption.USE_SHARE_SYSTEM);
        sharesForAll = options.get(CampaignOption.SHARES_FOR_ALL);
        rentedFacilitiesCostHospitalBeds = options.get(CampaignOption.RENTED_FACILITIES_COST_HOSPITAL_BEDS);
        rentedFacilitiesCostKitchens = options.get(CampaignOption.RENTED_FACILITIES_COST_KITCHENS);
        rentedFacilitiesCostHoldingCells = options.get(CampaignOption.RENTED_FACILITIES_COST_HOLDING_CELLS);
        rentedFacilitiesCostRepairBays = options.get(CampaignOption.RENTED_FACILITIES_COST_REPAIR_BAYS);
        commonPartPriceMultiplier = options.get(CampaignOption.COMMON_PART_PRICE_MULTIPLIER);
        innerSphereUnitPriceMultiplier = options.get(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER);
        innerSpherePartPriceMultiplier = options.get(CampaignOption.INNER_SPHERE_PART_PRICE_MULTIPLIER);
        clanUnitPriceMultiplier = options.get(CampaignOption.CLAN_UNIT_PRICE_MULTIPLIER);
        clanPartPriceMultiplier = options.get(CampaignOption.CLAN_PART_PRICE_MULTIPLIER);
        mixedTechUnitPriceMultiplier = options.get(CampaignOption.MIXED_TECH_UNIT_PRICE_MULTIPLIER);
        usedPartPriceMultipliers = Arrays.copyOf(options.get(CampaignOption.USED_PART_PRICE_MULTIPLIERS),
              options.get(CampaignOption.USED_PART_PRICE_MULTIPLIERS).length);
        damagedPartsValueMultiplier = options.get(CampaignOption.DAMAGED_PARTS_VALUE_MULTIPLIER);
        unrepairablePartsValueMultiplier = options.get(CampaignOption.UNREPAIRABLE_PARTS_VALUE_MULTIPLIER);
        cancelledOrderRefundMultiplier = options.get(CampaignOption.CANCELLED_ORDER_REFUND_MULTIPLIER);
    }

    void applyTo(@Nonnull CampaignOptions options) {
        options.set(CampaignOption.USE_LOAN_LIMITS, useLoanLimits);
        options.set(CampaignOption.USE_PERCENTAGE_MAINTENANCE, usePercentageMaintenance);
        options.set(CampaignOption.USE_EXTENDED_PARTS_MODIFIER, useExtendedPartsModifier);
        options.set(CampaignOption.USE_PEACETIME_COST, usePeacetimeCost);
        options.set(CampaignOption.SHOW_PEACETIME_COST, showPeacetimeCost);
        options.set(CampaignOption.FINANCIAL_YEAR_DURATION, financialYearDuration);
        options.set(CampaignOption.NEW_FINANCIAL_YEAR_FINANCES_TO_CSV_EXPORT, newFinancialYearFinancesToCSVExport);
        options.set(CampaignOption.SIMULATE_GRAY_MONDAY, simulateGrayMonday);
        options.set(CampaignOption.PAY_FOR_PARTS, payForParts);
        options.set(CampaignOption.PAY_FOR_REPAIRS, payForRepairs);
        options.set(CampaignOption.PAY_FOR_UNITS, payForUnits);
        options.set(CampaignOption.PAY_FOR_SALARIES, payForSalaries);
        options.set(CampaignOption.PAY_FOR_OVERHEAD, payForOverhead);
        options.set(CampaignOption.PAY_FOR_MAINTAIN, payForMaintain);
        options.set(CampaignOption.PAY_FOR_TRANSPORT, payForTransport);
        options.set(CampaignOption.PAY_FOR_RECRUITMENT, payForRecruitment);
        options.set(CampaignOption.PAY_FOR_FOOD, payForFood);
        options.set(CampaignOption.PAY_FOR_HOUSING, payForHousing);
        options.set(CampaignOption.SELL_UNITS, sellUnits);
        options.set(CampaignOption.SELL_PARTS, sellParts);
        options.set(CampaignOption.ALLOW_MONTHLY_REINVESTMENT, allowMonthlyReinvestment);
        options.set(CampaignOption.ALLOW_MONTHLY_CONNECTIONS, allowMonthlyConnections);
        options.set(CampaignOption.USE_BETTER_EXTRA_INCOME, useBetterExtraIncome);
        options.set(CampaignOption.USE_TAXES, useTaxes);
        options.set(CampaignOption.TAXES_PERCENTAGE, taxesPercentage);
        options.set(CampaignOption.USE_SHARE_SYSTEM, useShareSystem);
        options.set(CampaignOption.SHARES_FOR_ALL, sharesForAll);
        options.set(CampaignOption.RENTED_FACILITIES_COST_HOSPITAL_BEDS, rentedFacilitiesCostHospitalBeds);
        options.set(CampaignOption.RENTED_FACILITIES_COST_KITCHENS, rentedFacilitiesCostKitchens);
        options.set(CampaignOption.RENTED_FACILITIES_COST_HOLDING_CELLS, rentedFacilitiesCostHoldingCells);
        options.set(CampaignOption.RENTED_FACILITIES_COST_REPAIR_BAYS, rentedFacilitiesCostRepairBays);
        options.set(CampaignOption.COMMON_PART_PRICE_MULTIPLIER, commonPartPriceMultiplier);
        options.set(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER, innerSphereUnitPriceMultiplier);
        options.set(CampaignOption.INNER_SPHERE_PART_PRICE_MULTIPLIER, innerSpherePartPriceMultiplier);
        options.set(CampaignOption.CLAN_UNIT_PRICE_MULTIPLIER, clanUnitPriceMultiplier);
        options.set(CampaignOption.CLAN_PART_PRICE_MULTIPLIER, clanPartPriceMultiplier);
        options.set(CampaignOption.MIXED_TECH_UNIT_PRICE_MULTIPLIER, mixedTechUnitPriceMultiplier);
        for (int i = 0; i < Math.min(options.get(CampaignOption.USED_PART_PRICE_MULTIPLIERS).length,
              usedPartPriceMultipliers.length); i++) {
            options.get(CampaignOption.USED_PART_PRICE_MULTIPLIERS)[i] = usedPartPriceMultipliers[i];
        }
        options.set(CampaignOption.DAMAGED_PARTS_VALUE_MULTIPLIER, damagedPartsValueMultiplier);
        options.set(CampaignOption.UNREPAIRABLE_PARTS_VALUE_MULTIPLIER, unrepairablePartsValueMultiplier);
        options.set(CampaignOption.CANCELLED_ORDER_REFUND_MULTIPLIER, cancelledOrderRefundMultiplier);
    }
}
