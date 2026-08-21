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
import mekhq.campaign.campaignOptions.CampaignOption;
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
        useRandomRetirement = options.get(CampaignOption.USE_RANDOM_RETIREMENT);
        turnoverFixedTargetNumber = options.get(CampaignOption.TURNOVER_FIXED_TARGET_NUMBER);
        turnoverFrequency = options.get(CampaignOption.TURNOVER_FREQUENCY);
        useContractCompletionRandomRetirement = options.get(CampaignOption.USE_CONTRACT_COMPLETION_RANDOM_RETIREMENT);
        useRandomFounderTurnover = options.get(CampaignOption.USE_RANDOM_FOUNDER_TURNOVER);
        trackOriginalUnit = options.get(CampaignOption.TRACK_ORIGINAL_UNIT);
        aeroRecruitsHaveUnits = options.get(CampaignOption.AERO_RECRUITS_HAVE_UNITS);
        useSubContractSoldiers = options.get(CampaignOption.USE_SUB_CONTRACT_SOLDIERS);
        serviceContractDuration = options.get(CampaignOption.SERVICE_CONTRACT_DURATION);
        serviceContractModifier = options.get(CampaignOption.SERVICE_CONTRACT_MODIFIER);
        payBonusDefault = options.get(CampaignOption.PAY_BONUS_DEFAULT);
        payBonusDefaultThreshold = options.get(CampaignOption.PAY_BONUS_DEFAULT_THRESHOLD);
        includeCivilians = options.get(CampaignOption.INCLUDE_CIVILIANS);
        useCustomRetirementModifiers = options.get(CampaignOption.USE_CUSTOM_RETIREMENT_MODIFIERS);
        useFatigueModifiers = options.get(CampaignOption.USE_FATIGUE_MODIFIERS);
        useSkillModifiers = options.get(CampaignOption.USE_SKILL_MODIFIERS);
        useAgeModifiers = options.get(CampaignOption.USE_AGE_MODIFIERS);
        useUnitRatingModifiers = options.get(CampaignOption.USE_UNIT_RATING_MODIFIERS);
        useFactionModifiers = options.get(CampaignOption.USE_FACTION_MODIFIERS);
        useMissionStatusModifiers = options.get(CampaignOption.USE_MISSION_STATUS_MODIFIERS);
        useHostileTerritoryModifiers = options.get(CampaignOption.USE_HOSTILE_TERRITORY_MODIFIERS);
        useFamilyModifiers = options.get(CampaignOption.USE_FAMILY_MODIFIERS);
        useLoyaltyModifiers = options.get(CampaignOption.USE_LOYALTY_MODIFIERS);
        useHideLoyalty = options.get(CampaignOption.USE_HIDE_LOYALTY);
        payoutRateOfficer = options.get(CampaignOption.PAYOUT_RATE_OFFICER);
        payoutRateEnlisted = options.get(CampaignOption.PAYOUT_RATE_ENLISTED);
        payoutRetirementMultiplier = options.get(CampaignOption.PAYOUT_RETIREMENT_MULTIPLIER);
        usePayoutServiceBonus = options.get(CampaignOption.USE_PAYOUT_SERVICE_BONUS);
        payoutServiceBonusRate = options.get(CampaignOption.PAYOUT_SERVICE_BONUS_RATE);
        useHRStrain = options.get(CampaignOption.USE_HR_STRAIN);
        hrCapacity = options.get(CampaignOption.HR_CAPACITY);
        useManagementSkill = options.get(CampaignOption.USE_MANAGEMENT_SKILL);
        useCommanderLeadershipOnly = options.get(CampaignOption.USE_COMMANDER_LEADERSHIP_ONLY);
        managementSkillPenalty = options.get(CampaignOption.MANAGEMENT_SKILL_PENALTY);
        useFatigue = options.get(CampaignOption.USE_FATIGUE);
        fatigueRate = options.get(CampaignOption.FATIGUE_RATE);
        useInjuryFatigue = options.get(CampaignOption.USE_INJURY_FATIGUE);
        fieldKitchenCapacity = options.get(CampaignOption.FIELD_KITCHEN_CAPACITY);
        fieldKitchenIgnoreNonCombatants = options.get(CampaignOption.FIELD_KITCHEN_IGNORE_NON_COMBATANTS);
        fatigueUndeploymentThreshold = options.get(CampaignOption.FATIGUE_UNDEPLOYMENT_THRESHOLD);
        fatigueLeaveThreshold = options.get(CampaignOption.FATIGUE_LEAVE_THRESHOLD);
    }

    void applyTo(@Nonnull CampaignOptions options) {
        options.set(CampaignOption.USE_RANDOM_RETIREMENT, useRandomRetirement);
        options.set(CampaignOption.TURNOVER_FIXED_TARGET_NUMBER, turnoverFixedTargetNumber);
        options.set(CampaignOption.TURNOVER_FREQUENCY, turnoverFrequency);
        options.set(CampaignOption.USE_CONTRACT_COMPLETION_RANDOM_RETIREMENT, useContractCompletionRandomRetirement);
        options.set(CampaignOption.USE_RANDOM_FOUNDER_TURNOVER, useRandomFounderTurnover);
        options.set(CampaignOption.TRACK_ORIGINAL_UNIT, trackOriginalUnit);
        options.set(CampaignOption.AERO_RECRUITS_HAVE_UNITS, aeroRecruitsHaveUnits);
        options.set(CampaignOption.USE_SUB_CONTRACT_SOLDIERS, useSubContractSoldiers);
        options.set(CampaignOption.SERVICE_CONTRACT_DURATION, serviceContractDuration);
        options.set(CampaignOption.SERVICE_CONTRACT_MODIFIER, serviceContractModifier);
        options.set(CampaignOption.PAY_BONUS_DEFAULT, payBonusDefault);
        options.set(CampaignOption.PAY_BONUS_DEFAULT_THRESHOLD, payBonusDefaultThreshold);
        options.set(CampaignOption.INCLUDE_CIVILIANS, includeCivilians);
        options.set(CampaignOption.USE_CUSTOM_RETIREMENT_MODIFIERS, useCustomRetirementModifiers);
        options.set(CampaignOption.USE_FATIGUE_MODIFIERS, useFatigueModifiers);
        options.set(CampaignOption.USE_SKILL_MODIFIERS, useSkillModifiers);
        options.set(CampaignOption.USE_AGE_MODIFIERS, useAgeModifiers);
        options.set(CampaignOption.USE_UNIT_RATING_MODIFIERS, useUnitRatingModifiers);
        options.set(CampaignOption.USE_FACTION_MODIFIERS, useFactionModifiers);
        options.set(CampaignOption.USE_MISSION_STATUS_MODIFIERS, useMissionStatusModifiers);
        options.set(CampaignOption.USE_HOSTILE_TERRITORY_MODIFIERS, useHostileTerritoryModifiers);
        options.set(CampaignOption.USE_FAMILY_MODIFIERS, useFamilyModifiers);
        options.set(CampaignOption.USE_LOYALTY_MODIFIERS, useLoyaltyModifiers);
        options.set(CampaignOption.USE_HIDE_LOYALTY, useHideLoyalty);
        options.set(CampaignOption.PAYOUT_RATE_OFFICER, payoutRateOfficer);
        options.set(CampaignOption.PAYOUT_RATE_ENLISTED, payoutRateEnlisted);
        options.set(CampaignOption.PAYOUT_RETIREMENT_MULTIPLIER, payoutRetirementMultiplier);
        options.set(CampaignOption.USE_PAYOUT_SERVICE_BONUS, usePayoutServiceBonus);
        options.set(CampaignOption.PAYOUT_SERVICE_BONUS_RATE, payoutServiceBonusRate);
        options.set(CampaignOption.USE_HR_STRAIN, useHRStrain);
        options.set(CampaignOption.HR_CAPACITY, hrCapacity);
        options.set(CampaignOption.USE_MANAGEMENT_SKILL, useManagementSkill);
        options.set(CampaignOption.USE_COMMANDER_LEADERSHIP_ONLY, useCommanderLeadershipOnly);
        options.set(CampaignOption.MANAGEMENT_SKILL_PENALTY, managementSkillPenalty);
        options.set(CampaignOption.USE_FATIGUE, useFatigue);
        options.set(CampaignOption.FATIGUE_RATE, fatigueRate);
        options.set(CampaignOption.USE_INJURY_FATIGUE, useInjuryFatigue);
        options.set(CampaignOption.FIELD_KITCHEN_CAPACITY, fieldKitchenCapacity);
        options.set(CampaignOption.FIELD_KITCHEN_IGNORE_NON_COMBATANTS, fieldKitchenIgnoreNonCombatants);
        options.set(CampaignOption.FATIGUE_UNDEPLOYMENT_THRESHOLD, fatigueUndeploymentThreshold);
        options.set(CampaignOption.FATIGUE_LEAVE_THRESHOLD, fatigueLeaveThreshold);
    }
}
