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

import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.DEFAULT_TEMPORARY_CAPACITY;

import jakarta.annotation.Nonnull;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.AwardBonus;
import mekhq.campaign.personnel.enums.EdgeRefreshPeriod;
import mekhq.campaign.personnel.enums.TimeInDisplayFormat;
import mekhq.campaign.personnel.familiarity.Familiarity;
import mekhq.campaign.randomEvents.prisoners.PrisonerCaptureStyle;

class PersonnelOptionsModel {
    boolean useTactics;
    boolean useInitiativeBonus;
    boolean useSensibleTactics;
    boolean useToughness;
    boolean useRandomToughness;
    boolean useArtillery;
    boolean useAbilities;
    boolean onlyCommandersMatterVehicles;
    boolean onlyCommandersMatterInfantry;
    boolean onlyCommandersMatterBattleArmor;
    boolean useEdge;
    int maximumEdge;
    boolean useTwistOfFateSurvival;
    boolean useFoundersHavePlotArmor;
    EdgeRefreshPeriod edgeRefreshPeriod;
    int edgeRefreshCost;
    boolean useImplants;
    boolean alternativeQualityAveraging;
    boolean adminsHaveNegotiation;
    boolean adminExperienceLevelIncludeNegotiation;
    boolean usePersonnelRemoval;
    boolean useRemovalExemptCemetery;
    boolean useRemovalExemptRetirees;
    boolean useBlobInfantry;
    boolean useBlobBattleArmor;
    boolean useBlobVehicleCrewGround;
    boolean useBlobVehicleCrewVTOL;
    boolean useBlobVehicleCrewNaval;
    boolean useBlobVesselPilot;
    boolean useBlobVesselGunner;
    boolean useBlobVesselCrew;
    boolean useTransfers;
    boolean useExtendedTOEForceName;
    boolean personnelLogSkillGain;
    boolean personnelLogAbilityGain;
    boolean personnelLogEdgeGain;
    boolean useTimeInService;
    TimeInDisplayFormat timeInServiceDisplayFormat;
    boolean useTimeInRank;
    TimeInDisplayFormat timeInRankDisplayFormat;
    boolean trackTotalEarnings;
    boolean trackTotalXPEarnings;
    boolean showOriginFaction;
    AwardBonus awardBonusStyle;
    boolean useReplaceEdgeAwards;
    int awardTierSize;
    boolean enableAutoAwards;
    boolean issuePosthumousAwards;
    boolean issueBestAwardOnly;
    boolean ignoreStandardSet;
    boolean enableContractAwards;
    boolean enableFactionHunterAwards;
    boolean enableInjuryAwards;
    boolean enableIndividualKillAwards;
    boolean enableFormationKillAwards;
    boolean enableRankAwards;
    boolean enableScenarioAwards;
    boolean enableSkillAwards;
    boolean enableTheatreOfWarAwards;
    boolean enableTimeAwards;
    boolean enableTrainingAwards;
    boolean enableMiscAwards;
    String awardSetFilterList;
    boolean useAdvancedMedical;
    int healingWaitingPeriod;
    int naturalHealingWaitingPeriod;
    int minimumHitsForVehicles;
    boolean useRandomHitsForVehicles;
    boolean tougherHealing;
    boolean useAlternativeAdvancedMedical;
    boolean useAlternativeAdvancedMedicalFewerPermanentInjuries;
    double alternativeAdvancedMedicalHealingTimeMultiplier;
    boolean useRandomDiseases;
    int maximumPatients;
    boolean doctorsUseAdministration;
    boolean useUsefulMedics;
    boolean useMASHTheatres;
    int mashTheatreCapacity;
    PrisonerCaptureStyle prisonerCaptureStyle;
    boolean useFunctionalEscapeArtist;
    boolean resetTemporaryPrisonerCapacity;
    boolean useRandomDependentAddition;
    boolean useRandomDependentRemoval;
    int dependentProfessionDieSize;
    int civilianProfessionDieSize;
    Familiarity chassisFamiliarity;
    int chassisFamiliaritySpeed;

    PersonnelOptionsModel(@Nonnull CampaignOptions options) {
        useTactics = options.get(CampaignOption.USE_TACTICS);
        useInitiativeBonus = options.get(CampaignOption.USE_INITIATIVE_BONUS);
        useSensibleTactics = options.get(CampaignOption.USE_SENSIBLE_TACTICS);
        useToughness = options.get(CampaignOption.USE_TOUGHNESS);
        useRandomToughness = options.get(CampaignOption.USE_RANDOM_TOUGHNESS);
        useArtillery = options.get(CampaignOption.USE_ARTILLERY);
        useAbilities = options.get(CampaignOption.USE_ABILITIES);
        onlyCommandersMatterVehicles = options.get(CampaignOption.ONLY_COMMANDERS_MATTER_VEHICLES);
        onlyCommandersMatterInfantry = options.get(CampaignOption.ONLY_COMMANDERS_MATTER_INFANTRY);
        onlyCommandersMatterBattleArmor = options.get(CampaignOption.ONLY_COMMANDERS_MATTER_BATTLE_ARMOR);
        useEdge = options.get(CampaignOption.USE_EDGE);
        maximumEdge = options.get(CampaignOption.MAXIMUM_EDGE);
        useTwistOfFateSurvival = options.get(CampaignOption.USE_TWIST_OF_FATE_SURVIVAL);
        useFoundersHavePlotArmor = options.get(CampaignOption.USE_FOUNDER_PLOT_ARMOR);
        edgeRefreshPeriod = options.get(CampaignOption.EDGE_REFRESH_PERIOD);
        edgeRefreshCost = options.get(CampaignOption.EDGE_REFRESH_COST);
        useImplants = options.get(CampaignOption.USE_IMPLANTS);
        alternativeQualityAveraging = options.get(CampaignOption.ALTERNATIVE_QUALITY_AVERAGING);
        adminsHaveNegotiation = options.get(CampaignOption.ADMINS_HAVE_NEGOTIATION);
        adminExperienceLevelIncludeNegotiation = options.get(CampaignOption.ADMIN_EXPERIENCE_LEVEL_INCLUDE_NEGOTIATION);
        usePersonnelRemoval = options.get(CampaignOption.USE_PERSONNEL_REMOVAL);
        useRemovalExemptCemetery = options.get(CampaignOption.USE_REMOVAL_EXEMPT_CEMETERY);
        useRemovalExemptRetirees = options.get(CampaignOption.USE_REMOVAL_EXEMPT_RETIREES);
        useBlobInfantry = options.get(CampaignOption.USE_BLOB_INFANTRY);
        useBlobBattleArmor = options.get(CampaignOption.USE_BLOB_BATTLE_ARMOR);
        useBlobVehicleCrewGround = options.get(CampaignOption.USE_BLOB_VEHICLE_CREW_GROUND);
        useBlobVehicleCrewVTOL = options.get(CampaignOption.USE_BLOB_VEHICLE_CREW_VTOL);
        useBlobVehicleCrewNaval = options.get(CampaignOption.USE_BLOB_VEHICLE_CREW_NAVAL);
        useBlobVesselPilot = options.get(CampaignOption.USE_BLOB_VESSEL_PILOT);
        useBlobVesselGunner = options.get(CampaignOption.USE_BLOB_VESSEL_GUNNER);
        useBlobVesselCrew = options.get(CampaignOption.USE_BLOB_VESSEL_CREW);
        useTransfers = options.get(CampaignOption.USE_TRANSFERS);
        useExtendedTOEForceName = options.get(CampaignOption.USE_EXTENDED_TOE_FORCE_NAME);
        personnelLogSkillGain = options.get(CampaignOption.PERSONNEL_LOG_SKILL_GAIN);
        personnelLogAbilityGain = options.get(CampaignOption.PERSONNEL_LOG_ABILITY_GAIN);
        personnelLogEdgeGain = options.get(CampaignOption.PERSONNEL_LOG_EDGE_GAIN);
        useTimeInService = options.get(CampaignOption.USE_TIME_IN_SERVICE);
        timeInServiceDisplayFormat = options.get(CampaignOption.TIME_IN_SERVICE_DISPLAY_FORMAT);
        useTimeInRank = options.get(CampaignOption.USE_TIME_IN_RANK);
        timeInRankDisplayFormat = options.get(CampaignOption.TIME_IN_RANK_DISPLAY_FORMAT);
        trackTotalEarnings = options.get(CampaignOption.TRACK_TOTAL_EARNINGS);
        trackTotalXPEarnings = options.get(CampaignOption.TRACK_TOTAL_XP_EARNINGS);
        showOriginFaction = options.get(CampaignOption.SHOW_ORIGIN_FACTION);
        awardBonusStyle = options.get(CampaignOption.AWARD_BONUS_STYLE);
        useReplaceEdgeAwards = options.get(CampaignOption.USE_REPLACE_EDGE_AWARDS);
        awardTierSize = options.get(CampaignOption.AWARD_TIER_SIZE);
        enableAutoAwards = options.get(CampaignOption.ENABLE_AUTO_AWARDS);
        issuePosthumousAwards = options.get(CampaignOption.ISSUE_POSTHUMOUS_AWARDS);
        issueBestAwardOnly = options.get(CampaignOption.ISSUE_BEST_AWARD_ONLY);
        ignoreStandardSet = options.get(CampaignOption.IGNORE_STANDARD_SET);
        enableContractAwards = options.get(CampaignOption.ENABLE_CONTRACT_AWARDS);
        enableFactionHunterAwards = options.get(CampaignOption.ENABLE_FACTION_HUNTER_AWARDS);
        enableInjuryAwards = options.get(CampaignOption.ENABLE_INJURY_AWARDS);
        enableIndividualKillAwards = options.get(CampaignOption.ENABLE_INDIVIDUAL_KILL_AWARDS);
        enableFormationKillAwards = options.get(CampaignOption.ENABLE_FORMATION_KILL_AWARDS);
        enableRankAwards = options.get(CampaignOption.ENABLE_RANK_AWARDS);
        enableScenarioAwards = options.get(CampaignOption.ENABLE_SCENARIO_AWARDS);
        enableSkillAwards = options.get(CampaignOption.ENABLE_SKILL_AWARDS);
        enableTheatreOfWarAwards = options.get(CampaignOption.ENABLE_THEATRE_OF_WAR_AWARDS);
        enableTimeAwards = options.get(CampaignOption.ENABLE_TIME_AWARDS);
        enableTrainingAwards = options.get(CampaignOption.ENABLE_TRAINING_AWARDS);
        enableMiscAwards = options.get(CampaignOption.ENABLE_MISC_AWARDS);
        awardSetFilterList = options.get(CampaignOption.AWARD_SET_FILTER_LIST);
        useAdvancedMedical = options.get(CampaignOption.USE_ADVANCED_MEDICAL);
        healingWaitingPeriod = options.get(CampaignOption.HEAL_WAITING_PERIOD);
        naturalHealingWaitingPeriod = options.get(CampaignOption.NATURAL_HEALING_WAITING_PERIOD);
        minimumHitsForVehicles = options.get(CampaignOption.MINIMUM_HITS_FOR_VEHICLES);
        useRandomHitsForVehicles = options.get(CampaignOption.USE_RANDOM_HITS_FOR_VEHICLES);
        tougherHealing = options.get(CampaignOption.TOUGHER_HEALING);
        useAlternativeAdvancedMedical = options.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL);
        useAlternativeAdvancedMedicalFewerPermanentInjuries = options.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL_FEWER_PERMANENT_INJURIES);
        alternativeAdvancedMedicalHealingTimeMultiplier = options.get(CampaignOption.ALTERNATIVE_ADVANCED_MEDICAL_HEALING_TIME_MULTIPLIER);
        useRandomDiseases = options.get(CampaignOption.USE_RANDOM_DISEASES);
        maximumPatients = options.get(CampaignOption.MAXIMUM_PATIENTS);
        doctorsUseAdministration = options.get(CampaignOption.DOCTORS_USE_ADMINISTRATION);
        useUsefulMedics = options.get(CampaignOption.USE_USEFUL_MEDICS);
        useMASHTheatres = options.get(CampaignOption.USE_MASH_THEATRES);
        mashTheatreCapacity = options.get(CampaignOption.MASH_THEATRE_CAPACITY);
        prisonerCaptureStyle = options.get(CampaignOption.PRISONER_CAPTURE_STYLE);
        useFunctionalEscapeArtist = options.get(CampaignOption.USE_FUNCTIONAL_ESCAPE_ARTIST);
        resetTemporaryPrisonerCapacity = false;
        useRandomDependentAddition = options.get(CampaignOption.USE_RANDOM_DEPENDENT_ADDITION);
        useRandomDependentRemoval = options.get(CampaignOption.USE_RANDOM_DEPENDENT_REMOVAL);
        dependentProfessionDieSize = options.get(CampaignOption.DEPENDENT_PROFESSION_DIE_SIZE);
        civilianProfessionDieSize = options.get(CampaignOption.CIVILIAN_PROFESSION_DIE_SIZE);
        chassisFamiliarity = options.get(CampaignOption.CHASSIS_FAMILIARITY_MODE);
        chassisFamiliaritySpeed = options.get(CampaignOption.CHASSIS_FAMILIARITY_SPEED);
    }

    void applyTo(@Nonnull Campaign campaign, @Nonnull CampaignOptions options) {
        options.set(CampaignOption.USE_TACTICS, useTactics);
        options.set(CampaignOption.USE_INITIATIVE_BONUS, useInitiativeBonus);
        options.set(CampaignOption.USE_SENSIBLE_TACTICS, useSensibleTactics);
        options.set(CampaignOption.USE_TOUGHNESS, useToughness);
        options.set(CampaignOption.USE_RANDOM_TOUGHNESS, useRandomToughness);
        options.set(CampaignOption.USE_ARTILLERY, useArtillery);
        options.set(CampaignOption.USE_ABILITIES, useAbilities);
        options.set(CampaignOption.ONLY_COMMANDERS_MATTER_VEHICLES, onlyCommandersMatterVehicles);
        options.set(CampaignOption.ONLY_COMMANDERS_MATTER_INFANTRY, onlyCommandersMatterInfantry);
        options.set(CampaignOption.ONLY_COMMANDERS_MATTER_BATTLE_ARMOR, onlyCommandersMatterBattleArmor);
        options.set(CampaignOption.USE_EDGE, useEdge);
        options.set(CampaignOption.MAXIMUM_EDGE, maximumEdge);
        options.set(CampaignOption.USE_TWIST_OF_FATE_SURVIVAL, useTwistOfFateSurvival);
        options.set(CampaignOption.USE_FOUNDER_PLOT_ARMOR, useFoundersHavePlotArmor);
        options.set(CampaignOption.EDGE_REFRESH_PERIOD, edgeRefreshPeriod);
        options.set(CampaignOption.EDGE_REFRESH_COST, edgeRefreshCost);
        options.set(CampaignOption.USE_IMPLANTS, useImplants);
        options.set(CampaignOption.ALTERNATIVE_QUALITY_AVERAGING, alternativeQualityAveraging);
        options.set(CampaignOption.ADMINS_HAVE_NEGOTIATION, adminsHaveNegotiation);
        options.set(CampaignOption.ADMIN_EXPERIENCE_LEVEL_INCLUDE_NEGOTIATION, adminExperienceLevelIncludeNegotiation);
        options.set(CampaignOption.USE_PERSONNEL_REMOVAL, usePersonnelRemoval);
        options.set(CampaignOption.USE_REMOVAL_EXEMPT_CEMETERY, useRemovalExemptCemetery);
        options.set(CampaignOption.USE_REMOVAL_EXEMPT_RETIREES, useRemovalExemptRetirees);
        options.set(CampaignOption.USE_BLOB_INFANTRY, useBlobInfantry);
        options.set(CampaignOption.USE_BLOB_BATTLE_ARMOR, useBlobBattleArmor);
        options.set(CampaignOption.USE_BLOB_VEHICLE_CREW_GROUND, useBlobVehicleCrewGround);
        options.set(CampaignOption.USE_BLOB_VEHICLE_CREW_VTOL, useBlobVehicleCrewVTOL);
        options.set(CampaignOption.USE_BLOB_VEHICLE_CREW_NAVAL, useBlobVehicleCrewNaval);
        options.set(CampaignOption.USE_BLOB_VESSEL_PILOT, useBlobVesselPilot);
        options.set(CampaignOption.USE_BLOB_VESSEL_GUNNER, useBlobVesselGunner);
        options.set(CampaignOption.USE_BLOB_VESSEL_CREW, useBlobVesselCrew);
        options.set(CampaignOption.USE_TRANSFERS, useTransfers);
        options.set(CampaignOption.USE_EXTENDED_TOE_FORCE_NAME, useExtendedTOEForceName);
        options.set(CampaignOption.PERSONNEL_LOG_SKILL_GAIN, personnelLogSkillGain);
        options.set(CampaignOption.PERSONNEL_LOG_ABILITY_GAIN, personnelLogAbilityGain);
        options.set(CampaignOption.PERSONNEL_LOG_EDGE_GAIN, personnelLogEdgeGain);
        options.set(CampaignOption.USE_TIME_IN_SERVICE, useTimeInService);
        options.set(CampaignOption.TIME_IN_SERVICE_DISPLAY_FORMAT, timeInServiceDisplayFormat);
        options.set(CampaignOption.USE_TIME_IN_RANK, useTimeInRank);
        options.set(CampaignOption.TIME_IN_RANK_DISPLAY_FORMAT, timeInRankDisplayFormat);
        options.set(CampaignOption.TRACK_TOTAL_EARNINGS, trackTotalEarnings);
        options.set(CampaignOption.TRACK_TOTAL_XP_EARNINGS, trackTotalXPEarnings);
        options.set(CampaignOption.SHOW_ORIGIN_FACTION, showOriginFaction);
        options.set(CampaignOption.AWARD_BONUS_STYLE, awardBonusStyle);
        options.set(CampaignOption.AWARD_TIER_SIZE, awardTierSize);
        options.set(CampaignOption.USE_REPLACE_EDGE_AWARDS, useReplaceEdgeAwards);
        options.set(CampaignOption.ENABLE_AUTO_AWARDS, enableAutoAwards);
        options.set(CampaignOption.ISSUE_POSTHUMOUS_AWARDS, issuePosthumousAwards);
        options.set(CampaignOption.ISSUE_BEST_AWARD_ONLY, issueBestAwardOnly);
        options.set(CampaignOption.IGNORE_STANDARD_SET, ignoreStandardSet);
        options.set(CampaignOption.ENABLE_CONTRACT_AWARDS, enableContractAwards);
        options.set(CampaignOption.ENABLE_FACTION_HUNTER_AWARDS, enableFactionHunterAwards);
        options.set(CampaignOption.ENABLE_INJURY_AWARDS, enableInjuryAwards);
        options.set(CampaignOption.ENABLE_INDIVIDUAL_KILL_AWARDS, enableIndividualKillAwards);
        options.set(CampaignOption.ENABLE_FORMATION_KILL_AWARDS, enableFormationKillAwards);
        options.set(CampaignOption.ENABLE_RANK_AWARDS, enableRankAwards);
        options.set(CampaignOption.ENABLE_SCENARIO_AWARDS, enableScenarioAwards);
        options.set(CampaignOption.ENABLE_SKILL_AWARDS, enableSkillAwards);
        options.set(CampaignOption.ENABLE_THEATRE_OF_WAR_AWARDS, enableTheatreOfWarAwards);
        options.set(CampaignOption.ENABLE_TIME_AWARDS, enableTimeAwards);
        options.set(CampaignOption.ENABLE_TRAINING_AWARDS, enableTrainingAwards);
        options.set(CampaignOption.ENABLE_MISC_AWARDS, enableMiscAwards);
        options.set(CampaignOption.AWARD_SET_FILTER_LIST, awardSetFilterList);
        options.set(CampaignOption.USE_ADVANCED_MEDICAL, useAdvancedMedical);
        options.set(CampaignOption.HEAL_WAITING_PERIOD, healingWaitingPeriod);
        options.set(CampaignOption.NATURAL_HEALING_WAITING_PERIOD, naturalHealingWaitingPeriod);
        options.set(CampaignOption.MINIMUM_HITS_FOR_VEHICLES, minimumHitsForVehicles);
        options.set(CampaignOption.USE_RANDOM_HITS_FOR_VEHICLES, useRandomHitsForVehicles);
        options.set(CampaignOption.TOUGHER_HEALING, tougherHealing);
        options.set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, useAlternativeAdvancedMedical);
        options.set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL_FEWER_PERMANENT_INJURIES,
              useAlternativeAdvancedMedicalFewerPermanentInjuries);
        options.set(CampaignOption.ALTERNATIVE_ADVANCED_MEDICAL_HEALING_TIME_MULTIPLIER, alternativeAdvancedMedicalHealingTimeMultiplier);
        options.set(CampaignOption.USE_RANDOM_DISEASES, useRandomDiseases);
        options.set(CampaignOption.MAXIMUM_PATIENTS, maximumPatients);
        options.set(CampaignOption.DOCTORS_USE_ADMINISTRATION, doctorsUseAdministration);
        options.set(CampaignOption.USE_USEFUL_MEDICS, useUsefulMedics);
        options.set(CampaignOption.USE_MASH_THEATRES, useMASHTheatres);
        options.set(CampaignOption.MASH_THEATRE_CAPACITY, mashTheatreCapacity);
        options.set(CampaignOption.PRISONER_CAPTURE_STYLE, prisonerCaptureStyle);
        options.set(CampaignOption.USE_FUNCTIONAL_ESCAPE_ARTIST, useFunctionalEscapeArtist);
        if (resetTemporaryPrisonerCapacity) {
            campaign.getPlayerForce().setTemporaryPrisonerCapacity(DEFAULT_TEMPORARY_CAPACITY);
        }
        options.set(CampaignOption.USE_RANDOM_DEPENDENT_ADDITION, useRandomDependentAddition);
        options.set(CampaignOption.USE_RANDOM_DEPENDENT_REMOVAL, useRandomDependentRemoval);
        options.set(CampaignOption.DEPENDENT_PROFESSION_DIE_SIZE, dependentProfessionDieSize);
        options.set(CampaignOption.CIVILIAN_PROFESSION_DIE_SIZE, civilianProfessionDieSize);
        options.set(CampaignOption.CHASSIS_FAMILIARITY_MODE, chassisFamiliarity);
        options.set(CampaignOption.CHASSIS_FAMILIARITY_SPEED, chassisFamiliaritySpeed);
    }
}
