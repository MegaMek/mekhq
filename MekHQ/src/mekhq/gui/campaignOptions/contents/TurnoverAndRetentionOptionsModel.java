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

import jakarta.annotation.Nonnull;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.TurnoverFrequency;

class TurnoverAndRetentionOptionsModel {
    boolean useRandomRetirement;
    int turnoverFixedTargetNumber;
    TurnoverFrequency turnoverFrequency;
    boolean useContractCompletionRandomRetirement;
    boolean useRandomFounderTurnover;
    boolean trackOriginalUnit;
    boolean aeroRecruitsHaveUnits;
    boolean useSubContractSoldiers;
    int serviceContractDuration;
    int serviceContractModifier;
    boolean payBonusDefault;
    int payBonusDefaultThreshold;
    boolean includeCivilians;
    boolean useCustomRetirementModifiers;
    boolean useFatigueModifiers;
    boolean useSkillModifiers;
    boolean useAgeModifiers;
    boolean useUnitRatingModifiers;
    boolean useFactionModifiers;
    boolean useMissionStatusModifiers;
    boolean useHostileTerritoryModifiers;
    boolean useFamilyModifiers;
    boolean useLoyaltyModifiers;
    boolean useHideLoyalty;
    int payoutRateOfficer;
    int payoutRateEnlisted;
    int payoutRetirementMultiplier;
    boolean usePayoutServiceBonus;
    int payoutServiceBonusRate;
    boolean useHRStrain;
    int hrCapacity;
    boolean useManagementSkill;
    boolean useCommanderLeadershipOnly;
    int managementSkillPenalty;
    boolean useFatigue;
    int fatigueRate;
    boolean useInjuryFatigue;
    int fieldKitchenCapacity;
    boolean fieldKitchenIgnoreNonCombatants;
    int fatigueUndeploymentThreshold;
    int fatigueLeaveThreshold;

    TurnoverAndRetentionOptionsModel(@Nonnull CampaignOptions options) {
        useRandomRetirement = options.isUseRandomRetirement();
        turnoverFixedTargetNumber = options.getTurnoverFixedTargetNumber();
        turnoverFrequency = options.getTurnoverFrequency();
        useContractCompletionRandomRetirement = options.isUseContractCompletionRandomRetirement();
        useRandomFounderTurnover = options.isUseRandomFounderTurnover();
        trackOriginalUnit = options.isTrackOriginalUnit();
        aeroRecruitsHaveUnits = options.isAeroRecruitsHaveUnits();
        useSubContractSoldiers = options.isUseSubContractSoldiers();
        serviceContractDuration = options.getServiceContractDuration();
        serviceContractModifier = options.getServiceContractModifier();
        payBonusDefault = options.isPayBonusDefault();
        payBonusDefaultThreshold = options.getPayBonusDefaultThreshold();
        includeCivilians = options.isIncludeCivilians();
        useCustomRetirementModifiers = options.isUseCustomRetirementModifiers();
        useFatigueModifiers = options.isUseFatigueModifiers();
        useSkillModifiers = options.isUseSkillModifiers();
        useAgeModifiers = options.isUseAgeModifiers();
        useUnitRatingModifiers = options.isUseUnitRatingModifiers();
        useFactionModifiers = options.isUseFactionModifiers();
        useMissionStatusModifiers = options.isUseMissionStatusModifiers();
        useHostileTerritoryModifiers = options.isUseHostileTerritoryModifiers();
        useFamilyModifiers = options.isUseFamilyModifiers();
        useLoyaltyModifiers = options.isUseLoyaltyModifiers();
        useHideLoyalty = options.isUseHideLoyalty();
        payoutRateOfficer = options.getPayoutRateOfficer();
        payoutRateEnlisted = options.getPayoutRateEnlisted();
        payoutRetirementMultiplier = options.getPayoutRetirementMultiplier();
        usePayoutServiceBonus = options.isUsePayoutServiceBonus();
        payoutServiceBonusRate = options.getPayoutServiceBonusRate();
        useHRStrain = options.isUseHRStrain();
        hrCapacity = options.getHRCapacity();
        useManagementSkill = options.isUseManagementSkill();
        useCommanderLeadershipOnly = options.isUseCommanderLeadershipOnly();
        managementSkillPenalty = options.getManagementSkillPenalty();
        useFatigue = options.isUseFatigue();
        fatigueRate = options.getFatigueRate();
        useInjuryFatigue = options.isUseInjuryFatigue();
        fieldKitchenCapacity = options.getFieldKitchenCapacity();
        fieldKitchenIgnoreNonCombatants = options.isUseFieldKitchenIgnoreNonCombatants();
        fatigueUndeploymentThreshold = options.getFatigueUndeploymentThreshold();
        fatigueLeaveThreshold = options.getFatigueLeaveThreshold();
    }

    void applyTo(@Nonnull CampaignOptions options) {
        options.setUseRandomRetirement(useRandomRetirement);
        options.setTurnoverFixedTargetNumber(turnoverFixedTargetNumber);
        options.setTurnoverFrequency(turnoverFrequency);
        options.setUseContractCompletionRandomRetirement(useContractCompletionRandomRetirement);
        options.setUseRandomFounderTurnover(useRandomFounderTurnover);
        options.setTrackOriginalUnit(trackOriginalUnit);
        options.setAeroRecruitsHaveUnits(aeroRecruitsHaveUnits);
        options.setUseSubContractSoldiers(useSubContractSoldiers);
        options.setServiceContractDuration(serviceContractDuration);
        options.setServiceContractModifier(serviceContractModifier);
        options.setPayBonusDefault(payBonusDefault);
        options.setPayBonusDefaultThreshold(payBonusDefaultThreshold);
        options.setIncludeCivilians(includeCivilians);
        options.setUseCustomRetirementModifiers(useCustomRetirementModifiers);
        options.setUseFatigueModifiers(useFatigueModifiers);
        options.setUseSkillModifiers(useSkillModifiers);
        options.setUseAgeModifiers(useAgeModifiers);
        options.setUseUnitRatingModifiers(useUnitRatingModifiers);
        options.setUseFactionModifiers(useFactionModifiers);
        options.setUseMissionStatusModifiers(useMissionStatusModifiers);
        options.setUseHostileTerritoryModifiers(useHostileTerritoryModifiers);
        options.setUseFamilyModifiers(useFamilyModifiers);
        options.setUseLoyaltyModifiers(useLoyaltyModifiers);
        options.setUseHideLoyalty(useHideLoyalty);
        options.setPayoutRateOfficer(payoutRateOfficer);
        options.setPayoutRateEnlisted(payoutRateEnlisted);
        options.setPayoutRetirementMultiplier(payoutRetirementMultiplier);
        options.setUsePayoutServiceBonus(usePayoutServiceBonus);
        options.setPayoutServiceBonusRate(payoutServiceBonusRate);
        options.setUseHRStrain(useHRStrain);
        options.setHRCapacity(hrCapacity);
        options.setUseManagementSkill(useManagementSkill);
        options.setUseCommanderLeadershipOnly(useCommanderLeadershipOnly);
        options.setManagementSkillPenalty(managementSkillPenalty);
        options.setUseFatigue(useFatigue);
        options.setFatigueRate(fatigueRate);
        options.setUseInjuryFatigue(useInjuryFatigue);
        options.setFieldKitchenCapacity(fieldKitchenCapacity);
        options.setFieldKitchenIgnoreNonCombatants(fieldKitchenIgnoreNonCombatants);
        options.setFatigueUndeploymentThreshold(fatigueUndeploymentThreshold);
        options.setFatigueLeaveThreshold(fatigueLeaveThreshold);
    }
}
