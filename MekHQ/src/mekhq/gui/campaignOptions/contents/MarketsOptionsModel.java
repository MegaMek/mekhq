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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
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
    String personnelMarketName;
    boolean personnelMarketReportRefresh;
    boolean usePersonnelHireHiringHallOnly;
    double personnelMarketDylansWeight;
    Map<SkillLevel, Integer> personnelMarketRandomRemovalTargets;
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
    boolean contractMarketReportRefresh;
    int contractMaxSalvagePercentage;
    int dropShipBonusPercentage;
    int pityContracts;
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
        personnelMarketStyle = options.getPersonnelMarketStyle();
        personnelMarketName = options.getPersonnelMarketName();
        personnelMarketReportRefresh = options.isPersonnelMarketReportRefresh();
        usePersonnelHireHiringHallOnly = options.isUsePersonnelHireHiringHallOnly();
        personnelMarketDylansWeight = options.getPersonnelMarketDylansWeight();
        personnelMarketRandomRemovalTargets = new HashMap<>(options.getPersonnelMarketRandomRemovalTargets());
        unitMarketMethod = options.getUnitMarketMethod();
        unitMarketRegionalMekVariations = options.isRegionalMekVariations();
        unitMarketArtilleryUnitChance = options.getUnitMarketArtilleryUnitChance();
        unitMarketRarityModifier = options.getUnitMarketRarityModifier();
        instantUnitMarketDelivery = options.isInstantUnitMarketDelivery();
        mothballUnitMarketDeliveries = options.isMothballUnitMarketDeliveries();
        unitMarketReportRefresh = options.isUnitMarketReportRefresh();
        contractMarketMethod = options.getContractMarketMethod();
        contractSearchRadius = options.getContractSearchRadius();
        variableContractLength = options.isVariableContractLength();
        useTwoWayPay = options.isUseTwoWayPay();
        useCamOpsSalvage = options.isUseCamOpsSalvage();
        useRiskySalvage = options.isUseRiskySalvage();
        enableSalvageFlagByDefault = options.isEnableSalvageFlagByDefault();
        useDynamicDifficulty = options.isUseDynamicDifficulty();
        useBolsterContractSkill = options.isUseBolsterContractSkill();
        contractMarketReportRefresh = options.isContractMarketReportRefresh();
        contractMaxSalvagePercentage = options.getContractMaxSalvagePercentage();
        dropShipBonusPercentage = options.getDropShipBonusPercentage();
        pityContracts = options.getPityContracts();
        equipmentContractBase = options.isEquipmentContractBase();
        equipmentContractPercent = options.getEquipmentContractPercent();
        useAlternatePaymentMode = options.isUseAlternatePaymentMode();
        useDiminishingContractPay = options.isUseDiminishingContractPay();
        equipmentContractSaleValue = options.isEquipmentContractSaleValue();
        dropShipContractPercent = options.getDropShipContractPercent();
        jumpShipContractPercent = options.getJumpShipContractPercent();
        warShipContractPercent = options.getWarShipContractPercent();
        infantryDontCount = options.isInfantryDontCount();
        blcSaleValue = options.isBLCSaleValue();
        overageRepaymentInFinalPayment = options.isOverageRepaymentInFinalPayment();
    }

    void applyTo(@Nonnull Campaign campaign, @Nonnull CampaignOptions options) {
        if (personnelMarketStyle != null) {
            PersonnelMarketStyle originalPersonnelMarketStyle = options.getPersonnelMarketStyle();
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
            options.setPersonnelMarketStyle(personnelMarketStyle);
        }

        options.setPersonnelMarketName(personnelMarketName);
        if (Objects.equals(personnelMarketName, "Campaign Ops")) {
            campaign.getPersonnelMarket().setPaidRecruitment(false);
        }
        options.setPersonnelMarketDylansWeight(personnelMarketDylansWeight);
        options.setUsePersonnelHireHiringHallOnly(usePersonnelHireHiringHallOnly);
        options.setPersonnelMarketReportRefresh(personnelMarketReportRefresh);
        options.getPersonnelMarketRandomRemovalTargets().putAll(personnelMarketRandomRemovalTargets);
        options.setUnitMarketMethod(unitMarketMethod);
        options.setUnitMarketRegionalMekVariations(unitMarketRegionalMekVariations);
        options.setUnitMarketArtilleryUnitChance(unitMarketArtilleryUnitChance);
        options.setUnitMarketRarityModifier(unitMarketRarityModifier);
        options.setInstantUnitMarketDelivery(instantUnitMarketDelivery);
        options.setMothballUnitMarketDeliveries(mothballUnitMarketDeliveries);
        options.setUnitMarketReportRefresh(unitMarketReportRefresh);
        options.setContractMarketMethod(contractMarketMethod);
        options.setContractSearchRadius(contractSearchRadius);
        options.setVariableContractLength(variableContractLength);
        options.setUseTwoWayPay(useTwoWayPay);
        options.setUseCamOpsSalvage(useCamOpsSalvage);
        options.setUseRiskySalvage(useRiskySalvage);
        options.setEnableSalvageFlagByDefault(enableSalvageFlagByDefault);
        options.setUseDynamicDifficulty(useDynamicDifficulty);
        options.setUseBolsterContractSkill(useBolsterContractSkill);
        options.setContractMarketReportRefresh(contractMarketReportRefresh);
        options.setContractMaxSalvagePercentage(contractMaxSalvagePercentage);
        options.setDropShipBonusPercentage(dropShipBonusPercentage);
        options.setPityContracts(pityContracts);
        options.setEquipmentContractBase(equipmentContractBase);
        options.setEquipmentContractPercent(equipmentContractPercent);
        options.setDropShipContractPercent(dropShipContractPercent);
        options.setJumpShipContractPercent(jumpShipContractPercent);
        options.setWarShipContractPercent(warShipContractPercent);
        options.setUseAlternatePaymentMode(useAlternatePaymentMode);
        options.setUseDiminishingContractPay(useDiminishingContractPay);
        options.setEquipmentContractSaleValue(equipmentContractSaleValue);
        options.setBLCSaleValue(blcSaleValue);
        options.setUseInfantryDontCount(infantryDontCount);
        options.setOverageRepaymentInFinalPayment(overageRepaymentInFinalPayment);
    }
}
