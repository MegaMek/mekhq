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

import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;

class SystemsOptionsModel {
    int manualUnitRatingModifier;
    boolean resetCriminalRecord;
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
    boolean useAttributes;
    boolean randomizeAttributes;
    boolean displayAllAttributes;
    boolean useAgeEffects;
    boolean randomizeTraits;
    boolean allowMonthlyReinvestment;
    boolean allowMonthlyConnections;
    boolean useBetterExtraIncome;
    boolean useSmallArmsOnly;

    SystemsOptionsModel(CampaignOptions options, RandomSkillPreferences skillPreferences) {
        manualUnitRatingModifier = options.getManualUnitRatingModifier();
        resetCriminalRecord = false;
        requireSupportForceTransportation = options.isRequireSupportForceTransportation();
        clampReputationPayMultiplier = options.isClampReputationPayMultiplier();
        reduceReputationPerformanceModifier = options.isReduceReputationPerformanceModifier();
        reputationPerformanceModifierCutOff = options.isReputationPerformanceModifierCutOff();
        trackFactionStanding = options.isTrackFactionStanding();
        trackClimateRegardChanges = options.isTrackClimateRegardChanges();
        regardMultiplier = options.getRegardMultiplier();
        useFactionStandingNegotiation = options.isUseFactionStandingNegotiation();
        useFactionStandingResupply = options.isUseFactionStandingResupply();
        useFactionStandingCommandCircuit = options.isUseFactionStandingCommandCircuit();
        useFactionStandingOutlawed = options.isUseFactionStandingOutlawed();
        useFactionStandingBatchallRestrictions = options.isUseFactionStandingBatchallRestrictions();
        useFactionStandingRecruitment = options.isUseFactionStandingRecruitment();
        useFactionStandingBarracksCosts = options.isUseFactionStandingBarracksCosts();
        useFactionStandingUnitMarket = options.isUseFactionStandingUnitMarket();
        useFactionStandingContractPay = options.isUseFactionStandingContractPay();
        useFactionStandingSupportPoints = options.isUseFactionStandingSupportPoints();
        useAttributes = skillPreferences.isUseAttributes();
        randomizeAttributes = skillPreferences.isRandomizeAttributes();
        displayAllAttributes = options.isDisplayAllAttributes();
        useAgeEffects = options.isUseAgeEffects();
        randomizeTraits = skillPreferences.isRandomizeTraits();
        allowMonthlyReinvestment = options.isAllowMonthlyReinvestment();
        allowMonthlyConnections = options.isAllowMonthlyConnections();
        useBetterExtraIncome = options.isUseBetterExtraIncome();
        useSmallArmsOnly = options.isUseSmallArmsOnly();
    }

    void applyTo(CampaignOptions options, RandomSkillPreferences skillPreferences) {
        options.setManualUnitRatingModifier(manualUnitRatingModifier);
        options.setRequireSupportForceTransportation(requireSupportForceTransportation);
        options.setClampReputationPayMultiplier(clampReputationPayMultiplier);
        options.setReduceReputationPerformanceModifier(reduceReputationPerformanceModifier);
        options.setReputationPerformanceModifierCutOff(reputationPerformanceModifierCutOff);
        options.setTrackFactionStanding(trackFactionStanding);
        options.setTrackClimateRegardChanges(trackClimateRegardChanges);
        options.setRegardMultiplier(regardMultiplier);
        options.setUseFactionStandingNegotiation(useFactionStandingNegotiation);
        options.setUseFactionStandingResupply(useFactionStandingResupply);
        options.setUseFactionStandingCommandCircuit(useFactionStandingCommandCircuit);
        options.setUseFactionStandingOutlawed(useFactionStandingOutlawed);
        options.setUseFactionStandingBatchallRestrictions(useFactionStandingBatchallRestrictions);
        options.setUseFactionStandingRecruitment(useFactionStandingRecruitment);
        options.setUseFactionStandingBarracksCosts(useFactionStandingBarracksCosts);
        options.setUseFactionStandingUnitMarket(useFactionStandingUnitMarket);
        options.setUseFactionStandingContractPay(useFactionStandingContractPay);
        options.setUseFactionStandingSupportPoints(useFactionStandingSupportPoints);
        skillPreferences.setUseAttributes(useAttributes);
        skillPreferences.setRandomizeAttributes(randomizeAttributes);
        options.setDisplayAllAttributes(displayAllAttributes);
        options.setUseAgeEffects(useAgeEffects);
        skillPreferences.setRandomizeTraits(randomizeTraits);
        options.setAllowMonthlyReinvestment(allowMonthlyReinvestment);
        options.setAllowMonthlyConnections(allowMonthlyConnections);
        options.setUseBetterExtraIncome(useBetterExtraIncome);
        options.setUseSmallArmsOnly(useSmallArmsOnly);
    }
}