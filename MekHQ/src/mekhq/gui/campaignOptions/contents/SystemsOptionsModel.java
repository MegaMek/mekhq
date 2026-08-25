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
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;

class SystemsOptionsModel {
    int manualUnitRatingModifier;
    boolean resetCriminalRecord;
    boolean useChaosReputation;
    boolean campaignLevelChaosReputation;
    int chaosReputationCap;
    boolean chaosDebtPenaltiesStack;
    boolean chaosNoPartialSuccessReputation;
    boolean chaosPersonalityAffectsReputation;
    boolean chaosNewRecruitsHaveReputation;
    boolean requireSupportForceTransportation;
    boolean clampReputationPayMultiplier;
    boolean reduceReputationPerformanceModifier;
    boolean reputationPerformanceModifierCutOff;
    boolean trackFactionStanding;
    boolean trackClimateRegardChanges;
    double regardMultiplier;
    boolean useFactionStandingNegotiation;
    boolean useFactionStandingResupply;
    boolean useFactionStandingCommandCircuit;
    boolean useFactionStandingOutlawed;
    boolean useFactionStandingBatchallRestrictions;
    boolean useFactionStandingRecruitment;
    boolean useFactionStandingBarracksCosts;
    boolean useFactionStandingUnitMarket;
    boolean useFactionStandingContractPay;
    boolean useFactionStandingSupportPoints;

    SystemsOptionsModel(@Nonnull CampaignOptions options) {
        manualUnitRatingModifier = options.get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER);
        resetCriminalRecord = false;
        useChaosReputation = options.get(CampaignOption.USE_CHAOS_REPUTATION);
        campaignLevelChaosReputation = options.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION);
        chaosReputationCap = options.get(CampaignOption.CHAOS_REPUTATION_CAP);
        chaosDebtPenaltiesStack = options.get(CampaignOption.CHAOS_DEBT_PENALTIES_STACK);
        chaosNoPartialSuccessReputation = options.get(CampaignOption.CHAOS_NO_PARTIAL_SUCCESS_REPUTATION);
        chaosPersonalityAffectsReputation = options.get(CampaignOption.CHAOS_PERSONALITY_AFFECTS_REPUTATION);
        chaosNewRecruitsHaveReputation = options.get(CampaignOption.CHAOS_NEW_RECRUITS_HAVE_REPUTATION);
        requireSupportForceTransportation = options.get(CampaignOption.REQUIRE_SUPPORT_FORCE_TRANSPORTATION);
        clampReputationPayMultiplier = options.get(CampaignOption.CLAMP_REPUTATION_PAY_MULTIPLIER);
        reduceReputationPerformanceModifier = options.get(CampaignOption.REDUCE_REPUTATION_PERFORMANCE_MODIFIER);
        reputationPerformanceModifierCutOff = options.get(CampaignOption.REPUTATION_PERFORMANCE_MODIFIER_CUT_OFF);
        trackFactionStanding = options.get(CampaignOption.TRACK_FACTION_STANDING);
        trackClimateRegardChanges = options.get(CampaignOption.TRACK_CLIMATE_REGARD_CHANGES);
        regardMultiplier = options.get(CampaignOption.REGARD_MULTIPLIER);
        useFactionStandingNegotiation = options.get(CampaignOption.USE_FACTION_STANDING_NEGOTIATION);
        useFactionStandingResupply = options.get(CampaignOption.USE_FACTION_STANDING_RESUPPLY);
        useFactionStandingCommandCircuit = options.get(CampaignOption.USE_FACTION_STANDING_COMMAND_CIRCUIT);
        useFactionStandingOutlawed = options.get(CampaignOption.USE_FACTION_STANDING_OUTLAWED);
        useFactionStandingBatchallRestrictions = options.get(CampaignOption.USE_FACTION_STANDING_BATCHALL_RESTRICTIONS);
        useFactionStandingRecruitment = options.get(CampaignOption.USE_FACTION_STANDING_RECRUITMENT);
        useFactionStandingBarracksCosts = options.get(CampaignOption.USE_FACTION_STANDING_BARRACKS_COSTS);
        useFactionStandingUnitMarket = options.get(CampaignOption.USE_FACTION_STANDING_UNIT_MARKET);
        useFactionStandingContractPay = options.get(CampaignOption.USE_FACTION_STANDING_CONTRACT_PAY);
        useFactionStandingSupportPoints = options.get(CampaignOption.USE_FACTION_STANDING_SUPPORT_POINTS);
    }

    void applyTo(@Nonnull CampaignOptions options) {
        options.set(CampaignOption.MANUAL_UNIT_RATING_MODIFIER, manualUnitRatingModifier);
        options.set(CampaignOption.USE_CHAOS_REPUTATION, useChaosReputation);
        options.set(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION, campaignLevelChaosReputation);
        options.set(CampaignOption.CHAOS_REPUTATION_CAP, chaosReputationCap);
        options.set(CampaignOption.CHAOS_DEBT_PENALTIES_STACK, chaosDebtPenaltiesStack);
        options.set(CampaignOption.CHAOS_NO_PARTIAL_SUCCESS_REPUTATION, chaosNoPartialSuccessReputation);
        options.set(CampaignOption.CHAOS_PERSONALITY_AFFECTS_REPUTATION, chaosPersonalityAffectsReputation);
        options.set(CampaignOption.CHAOS_NEW_RECRUITS_HAVE_REPUTATION, chaosNewRecruitsHaveReputation);
        options.set(CampaignOption.REQUIRE_SUPPORT_FORCE_TRANSPORTATION, requireSupportForceTransportation);
        options.set(CampaignOption.CLAMP_REPUTATION_PAY_MULTIPLIER, clampReputationPayMultiplier);
        options.set(CampaignOption.REDUCE_REPUTATION_PERFORMANCE_MODIFIER, reduceReputationPerformanceModifier);
        options.set(CampaignOption.REPUTATION_PERFORMANCE_MODIFIER_CUT_OFF, reputationPerformanceModifierCutOff);
        options.set(CampaignOption.TRACK_FACTION_STANDING, trackFactionStanding);
        options.set(CampaignOption.TRACK_CLIMATE_REGARD_CHANGES, trackClimateRegardChanges);
        options.set(CampaignOption.REGARD_MULTIPLIER, regardMultiplier);
        options.set(CampaignOption.USE_FACTION_STANDING_NEGOTIATION, useFactionStandingNegotiation);
        options.set(CampaignOption.USE_FACTION_STANDING_RESUPPLY, useFactionStandingResupply);
        options.set(CampaignOption.USE_FACTION_STANDING_COMMAND_CIRCUIT, useFactionStandingCommandCircuit);
        options.set(CampaignOption.USE_FACTION_STANDING_OUTLAWED, useFactionStandingOutlawed);
        options.set(CampaignOption.USE_FACTION_STANDING_BATCHALL_RESTRICTIONS, useFactionStandingBatchallRestrictions);
        options.set(CampaignOption.USE_FACTION_STANDING_RECRUITMENT, useFactionStandingRecruitment);
        options.set(CampaignOption.USE_FACTION_STANDING_BARRACKS_COSTS, useFactionStandingBarracksCosts);
        options.set(CampaignOption.USE_FACTION_STANDING_UNIT_MARKET, useFactionStandingUnitMarket);
        options.set(CampaignOption.USE_FACTION_STANDING_CONTRACT_PAY, useFactionStandingContractPay);
        options.set(CampaignOption.USE_FACTION_STANDING_SUPPORT_POINTS, useFactionStandingSupportPoints);
    }
}
