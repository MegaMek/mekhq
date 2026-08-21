/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.market.personnelMarket.markets.PersonnelMarketCamOpsRevised;
import mekhq.campaign.market.personnelMarket.markets.PersonnelMarketCamOpsStrict;
import mekhq.campaign.market.personnelMarket.markets.PersonnelMarketMekHQ;

class MarketsOptionsModel {
    @Nullable PersonnelMarketStyle personnelMarketStyle;
    boolean personnelMarketReportRefresh;
    boolean usePersonnelHireHiringHallOnly;
    UnitMarketMethod unitMarketMethod;
    boolean unitMarketRegionalMekVariations;
    int unitMarketArtilleryUnitChance;
    int unitMarketRarityModifier;
    boolean instantUnitMarketDelivery;
    boolean mothballUnitMarketDeliveries;
    boolean unitMarketReportRefresh;
    ContractMarketMethod contractMarketMethod;
    int contractSearchRadius;
    boolean variableContractLength;
    boolean useTwoWayPay;
    boolean useCamOpsSalvage;
    boolean useRiskySalvage;
    boolean enableSalvageFlagByDefault;
    boolean useDynamicDifficulty;
    boolean useBolsterContractSkill;
    boolean useChaosScaleSupportPointConversion;
    boolean useContractFactionModifiers;
    boolean useIntelObfuscation;
    boolean contractMarketReportRefresh;
    int contractMaxSalvagePercentage;
    int dropShipBonusPercentage;
    int pityContracts;
    double contractBasePayMultiplier;
    double contractStraightSupportMultiplier;
    double contractBattlefieldLossMultiplier;
    double contractTransportMultiplier;
    double contractSalvageMultiplier;
    boolean useChaosSupportPointConversion;
    boolean useLegacyContractPay;
    boolean equipmentContractBase;
    double equipmentContractPercent;
    boolean useAlternatePaymentMode;
    boolean useDiminishingContractPay;
    boolean equipmentContractSaleValue;
    double dropShipContractPercent;
    double jumpShipContractPercent;
    double warShipContractPercent;
    boolean infantryDontCount;
    boolean blcSaleValue;
    boolean overageRepaymentInFinalPayment;

    MarketsOptionsModel(@Nonnull CampaignOptions options) {
        personnelMarketStyle = options.get(CampaignOption.PERSONNEL_MARKET_STYLE);
        personnelMarketReportRefresh = options.get(CampaignOption.PERSONNEL_MARKET_REPORT_REFRESH);
        usePersonnelHireHiringHallOnly = options.get(CampaignOption.USE_PERSONNEL_HIRE_HIRING_HALL_ONLY);
        unitMarketMethod = options.get(CampaignOption.UNIT_MARKET_METHOD);
        unitMarketRegionalMekVariations = options.get(CampaignOption.UNIT_MARKET_REGIONAL_MEK_VARIATIONS);
        unitMarketArtilleryUnitChance = options.get(CampaignOption.UNIT_MARKET_ARTILLERY_UNIT_CHANCE);
        unitMarketRarityModifier = options.get(CampaignOption.UNIT_MARKET_RARITY_MODIFIER);
        instantUnitMarketDelivery = options.get(CampaignOption.INSTANT_UNIT_MARKET_DELIVERY);
        mothballUnitMarketDeliveries = options.get(CampaignOption.MOTHBALL_UNIT_MARKET_DELIVERIES);
        unitMarketReportRefresh = options.get(CampaignOption.UNIT_MARKET_REPORT_REFRESH);
        contractMarketMethod = options.get(CampaignOption.CONTRACT_MARKET_METHOD);
        contractSearchRadius = options.get(CampaignOption.CONTRACT_SEARCH_RADIUS);
        variableContractLength = options.get(CampaignOption.VARIABLE_CONTRACT_LENGTH);
        useTwoWayPay = options.get(CampaignOption.IS_USE_TWO_WAY_PAY);
        useCamOpsSalvage = options.get(CampaignOption.IS_USE_CAM_OPS_SALVAGE);
        useRiskySalvage = options.get(CampaignOption.IS_USE_RISKY_SALVAGE);
        enableSalvageFlagByDefault = options.get(CampaignOption.IS_ENABLE_SALVAGE_FLAG_BY_DEFAULT);
        useDynamicDifficulty = options.get(CampaignOption.USE_DYNAMIC_DIFFICULTY);
        useBolsterContractSkill = options.get(CampaignOption.USE_BOLSTER_CONTRACT_SKILL);
        useChaosScaleSupportPointConversion = options.get(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION);
        useContractFactionModifiers = options.get(CampaignOption.USE_CONTRACT_FACTION_MODIFIERS);
        useIntelObfuscation = options.get(CampaignOption.USE_INTEL_OBFUSCATION);
        contractMarketReportRefresh = options.get(CampaignOption.CONTRACT_MARKET_REPORT_REFRESH);
        contractMaxSalvagePercentage = options.get(CampaignOption.CONTRACT_MAX_SALVAGE_PERCENTAGE);
        dropShipBonusPercentage = options.get(CampaignOption.DROP_SHIP_BONUS_PERCENTAGE);
        pityContracts = options.get(CampaignOption.PITY_CONTRACTS);
        contractBasePayMultiplier = options.get(CampaignOption.CONTRACT_BASE_PAY_MULTIPLIER);
        contractStraightSupportMultiplier = options.get(CampaignOption.CONTRACT_STRAIGHT_SUPPORT_MULTIPLIER);
        contractBattlefieldLossMultiplier = options.get(CampaignOption.CONTRACT_BATTLEFIELD_LOSS_MULTIPLIER);
        contractTransportMultiplier = options.get(CampaignOption.CONTRACT_TRANSPORT_MULTIPLIER);
        contractSalvageMultiplier = options.get(CampaignOption.CONTRACT_SALVAGE_MULTIPLIER);
        useChaosSupportPointConversion = options.get(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION);
        useLegacyContractPay = options.get(CampaignOption.USE_LEGACY_CONTRACT_PAY);
        equipmentContractBase = options.get(CampaignOption.EQUIPMENT_CONTRACT_BASE);
        equipmentContractPercent = options.get(CampaignOption.EQUIPMENT_CONTRACT_PERCENT);
        useAlternatePaymentMode = options.get(CampaignOption.USE_ALTERNATE_PAYMENT_MODE);
        useDiminishingContractPay = options.get(CampaignOption.USE_DIMINISHING_CONTRACT_PAY);
        equipmentContractSaleValue = options.get(CampaignOption.EQUIPMENT_CONTRACT_SALE_VALUE);
        dropShipContractPercent = options.get(CampaignOption.DROP_SHIP_CONTRACT_PERCENT);
        jumpShipContractPercent = options.get(CampaignOption.JUMP_SHIP_CONTRACT_PERCENT);
        warShipContractPercent = options.get(CampaignOption.WAR_SHIP_CONTRACT_PERCENT);
        infantryDontCount = options.get(CampaignOption.INFANTRY_DONT_COUNT);
        blcSaleValue = options.get(CampaignOption.BLC_SALE_VALUE);
        overageRepaymentInFinalPayment = options.get(CampaignOption.OVERAGE_REPAYMENT_IN_FINAL_PAYMENT);
    }

    void applyTo(@Nonnull Campaign campaign, @Nonnull CampaignOptions options) {
        if (personnelMarketStyle != null) {
            PersonnelMarketStyle originalPersonnelMarketStyle = options.get(CampaignOption.PERSONNEL_MARKET_STYLE);
            if (personnelMarketStyle != originalPersonnelMarketStyle) {
                NewPersonnelMarket replacementMarket = switch (personnelMarketStyle) {
                    case PERSONNEL_MARKET_DISABLED -> new NewPersonnelMarket();
                    case MEKHQ -> new PersonnelMarketMekHQ();
                    case CAMPAIGN_OPERATIONS_REVISED -> new PersonnelMarketCamOpsRevised();
                    case CAMPAIGN_OPERATIONS_STRICT -> new PersonnelMarketCamOpsStrict();
                };
                replacementMarket.setCampaign(campaign);
                campaign.setNewPersonnelMarket(replacementMarket);
            }
            options.set(CampaignOption.PERSONNEL_MARKET_STYLE, personnelMarketStyle);
        }

        options.set(CampaignOption.USE_PERSONNEL_HIRE_HIRING_HALL_ONLY, usePersonnelHireHiringHallOnly);
        options.set(CampaignOption.PERSONNEL_MARKET_REPORT_REFRESH, personnelMarketReportRefresh);
        options.set(CampaignOption.UNIT_MARKET_METHOD, unitMarketMethod);
        options.set(CampaignOption.UNIT_MARKET_REGIONAL_MEK_VARIATIONS, unitMarketRegionalMekVariations);
        options.set(CampaignOption.UNIT_MARKET_ARTILLERY_UNIT_CHANCE, unitMarketArtilleryUnitChance);
        options.set(CampaignOption.UNIT_MARKET_RARITY_MODIFIER, unitMarketRarityModifier);
        options.set(CampaignOption.INSTANT_UNIT_MARKET_DELIVERY, instantUnitMarketDelivery);
        options.set(CampaignOption.MOTHBALL_UNIT_MARKET_DELIVERIES, mothballUnitMarketDeliveries);
        options.set(CampaignOption.UNIT_MARKET_REPORT_REFRESH, unitMarketReportRefresh);
        options.set(CampaignOption.CONTRACT_MARKET_METHOD, contractMarketMethod);
        options.set(CampaignOption.CONTRACT_SEARCH_RADIUS, contractSearchRadius);
        options.set(CampaignOption.VARIABLE_CONTRACT_LENGTH, variableContractLength);
        options.set(CampaignOption.IS_USE_TWO_WAY_PAY, useTwoWayPay);
        options.set(CampaignOption.IS_USE_CAM_OPS_SALVAGE, useCamOpsSalvage);
        options.set(CampaignOption.IS_USE_RISKY_SALVAGE, useRiskySalvage);
        options.set(CampaignOption.IS_ENABLE_SALVAGE_FLAG_BY_DEFAULT, enableSalvageFlagByDefault);
        options.set(CampaignOption.USE_DYNAMIC_DIFFICULTY, useDynamicDifficulty);
        options.set(CampaignOption.USE_BOLSTER_CONTRACT_SKILL, useBolsterContractSkill);
        options.set(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION, useChaosScaleSupportPointConversion);
        options.set(CampaignOption.USE_CONTRACT_FACTION_MODIFIERS, useContractFactionModifiers);
        options.set(CampaignOption.USE_INTEL_OBFUSCATION, useIntelObfuscation);
        options.set(CampaignOption.CONTRACT_MARKET_REPORT_REFRESH, contractMarketReportRefresh);
        options.set(CampaignOption.CONTRACT_MAX_SALVAGE_PERCENTAGE, contractMaxSalvagePercentage);
        options.set(CampaignOption.DROP_SHIP_BONUS_PERCENTAGE, dropShipBonusPercentage);
        options.set(CampaignOption.PITY_CONTRACTS, pityContracts);
        options.set(CampaignOption.CONTRACT_BASE_PAY_MULTIPLIER, contractBasePayMultiplier);
        options.set(CampaignOption.CONTRACT_STRAIGHT_SUPPORT_MULTIPLIER, contractStraightSupportMultiplier);
        options.set(CampaignOption.CONTRACT_BATTLEFIELD_LOSS_MULTIPLIER, contractBattlefieldLossMultiplier);
        options.set(CampaignOption.CONTRACT_TRANSPORT_MULTIPLIER, contractTransportMultiplier);
        options.set(CampaignOption.CONTRACT_SALVAGE_MULTIPLIER, contractSalvageMultiplier);
        options.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, useChaosSupportPointConversion);
        options.set(CampaignOption.USE_LEGACY_CONTRACT_PAY, useLegacyContractPay);
        options.set(CampaignOption.EQUIPMENT_CONTRACT_BASE, equipmentContractBase);
        options.setEquipmentContractPercent(equipmentContractPercent);
        options.setDropShipContractPercent(dropShipContractPercent);
        options.setJumpShipContractPercent(jumpShipContractPercent);
        options.setWarShipContractPercent(warShipContractPercent);
        options.set(CampaignOption.USE_ALTERNATE_PAYMENT_MODE, useAlternatePaymentMode);
        options.set(CampaignOption.USE_DIMINISHING_CONTRACT_PAY, useDiminishingContractPay);
        options.set(CampaignOption.EQUIPMENT_CONTRACT_SALE_VALUE, equipmentContractSaleValue);
        options.set(CampaignOption.BLC_SALE_VALUE, blcSaleValue);
        options.set(CampaignOption.INFANTRY_DONT_COUNT, infantryDontCount);
        options.set(CampaignOption.OVERAGE_REPAYMENT_IN_FINAL_PAYMENT, overageRepaymentInFinalPayment);
    }
}
