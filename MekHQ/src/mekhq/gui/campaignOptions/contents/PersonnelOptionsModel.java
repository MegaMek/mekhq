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
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.AwardBonus;
import mekhq.campaign.personnel.enums.EdgeRefreshPeriod;
import mekhq.campaign.personnel.enums.TimeInDisplayFormat;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerCaptureStyle;

class PersonnelOptionsModel {
    boolean useTactics;
    boolean useInitiativeBonus;
    boolean useToughness;
    boolean useRandomToughness;
    boolean useArtillery;
    boolean useAbilities;
    boolean onlyCommandersMatterVehicles;
    boolean onlyCommandersMatterInfantry;
    boolean onlyCommandersMatterBattleArmor;
    boolean useEdge;
    boolean useSupportEdge;
    EdgeRefreshPeriod edgeRefreshPeriod;
    int edgeRefreshCost;
    boolean useImplants;
    boolean alternativeQualityAveraging;
    boolean usePersonnelRemoval;
    boolean useRemovalExemptCemetery;
    boolean useRemovalExemptRetirees;
    boolean adminsHaveNegotiation;
    boolean adminExperienceLevelIncludeNegotiation;
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
    boolean displayPersonnelLog;
    boolean displayScenarioLog;
    boolean displayKillRecord;
    boolean displayMedicalRecord;
    boolean displayPatientRecord;
    boolean displayAssignmentRecord;
    boolean displayPerformanceRecord;
    boolean useTimeInService;
    TimeInDisplayFormat timeInServiceDisplayFormat;
    boolean useTimeInRank;
    TimeInDisplayFormat timeInRankDisplayFormat;
    boolean trackTotalEarnings;
    boolean trackTotalXPEarnings;
    boolean showOriginFaction;
    AwardBonus awardBonusStyle;
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
    boolean useKinderAlternativeAdvancedMedical;
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

    PersonnelOptionsModel(@Nonnull CampaignOptions options) {
        useTactics = options.isUseTactics();
        useInitiativeBonus = options.isUseInitiativeBonus();
        useToughness = options.isUseToughness();
        useRandomToughness = options.isUseRandomToughness();
        useArtillery = options.isUseArtillery();
        useAbilities = options.isUseAbilities();
        onlyCommandersMatterVehicles = options.isOnlyCommandersMatterVehicles();
        onlyCommandersMatterInfantry = options.isOnlyCommandersMatterInfantry();
        onlyCommandersMatterBattleArmor = options.isOnlyCommandersMatterBattleArmor();
        useEdge = options.isUseEdge();
        useSupportEdge = options.isUseSupportEdge();
        edgeRefreshPeriod = options.getEdgeRefreshPeriod();
        edgeRefreshCost = options.getEdgeRefreshCost();
        useImplants = options.isUseImplants();
        alternativeQualityAveraging = options.isAlternativeQualityAveraging();
        usePersonnelRemoval = options.isUsePersonnelRemoval();
        useRemovalExemptCemetery = options.isUseRemovalExemptCemetery();
        useRemovalExemptRetirees = options.isUseRemovalExemptRetirees();
        adminsHaveNegotiation = options.isAdminsHaveNegotiation();
        adminExperienceLevelIncludeNegotiation = options.isAdminExperienceLevelIncludeNegotiation();
        useBlobInfantry = options.isUseBlobInfantry();
        useBlobBattleArmor = options.isUseBlobBattleArmor();
        useBlobVehicleCrewGround = options.isUseBlobVehicleCrewGround();
        useBlobVehicleCrewVTOL = options.isUseBlobVehicleCrewVTOL();
        useBlobVehicleCrewNaval = options.isUseBlobVehicleCrewNaval();
        useBlobVesselPilot = options.isUseBlobVesselPilot();
        useBlobVesselGunner = options.isUseBlobVesselGunner();
        useBlobVesselCrew = options.isUseBlobVesselCrew();
        useTransfers = options.isUseTransfers();
        useExtendedTOEForceName = options.isUseExtendedTOEForceName();
        personnelLogSkillGain = options.isPersonnelLogSkillGain();
        personnelLogAbilityGain = options.isPersonnelLogAbilityGain();
        personnelLogEdgeGain = options.isPersonnelLogEdgeGain();
        displayPersonnelLog = options.isDisplayPersonnelLog();
        displayScenarioLog = options.isDisplayScenarioLog();
        displayKillRecord = options.isDisplayKillRecord();
        displayMedicalRecord = options.isDisplayMedicalRecord();
        displayPatientRecord = options.isDisplayPatientRecord();
        displayAssignmentRecord = options.isDisplayAssignmentRecord();
        displayPerformanceRecord = options.isDisplayPerformanceRecord();
        useTimeInService = options.isUseTimeInService();
        timeInServiceDisplayFormat = options.getTimeInServiceDisplayFormat();
        useTimeInRank = options.isUseTimeInRank();
        timeInRankDisplayFormat = options.getTimeInRankDisplayFormat();
        trackTotalEarnings = options.isTrackTotalEarnings();
        trackTotalXPEarnings = options.isTrackTotalXPEarnings();
        showOriginFaction = options.isShowOriginFaction();
        awardBonusStyle = options.getAwardBonusStyle();
        awardTierSize = options.getAwardTierSize();
        enableAutoAwards = options.isEnableAutoAwards();
        issuePosthumousAwards = options.isIssuePosthumousAwards();
        issueBestAwardOnly = options.isIssueBestAwardOnly();
        ignoreStandardSet = options.isIgnoreStandardSet();
        enableContractAwards = options.isEnableContractAwards();
        enableFactionHunterAwards = options.isEnableFactionHunterAwards();
        enableInjuryAwards = options.isEnableInjuryAwards();
        enableIndividualKillAwards = options.isEnableIndividualKillAwards();
        enableFormationKillAwards = options.isEnableFormationKillAwards();
        enableRankAwards = options.isEnableRankAwards();
        enableScenarioAwards = options.isEnableScenarioAwards();
        enableSkillAwards = options.isEnableSkillAwards();
        enableTheatreOfWarAwards = options.isEnableTheatreOfWarAwards();
        enableTimeAwards = options.isEnableTimeAwards();
        enableTrainingAwards = options.isEnableTrainingAwards();
        enableMiscAwards = options.isEnableMiscAwards();
        awardSetFilterList = options.getAwardSetFilterList();
        useAdvancedMedical = options.isUseAdvancedMedicalDirect();
        healingWaitingPeriod = options.getHealingWaitingPeriod();
        naturalHealingWaitingPeriod = options.getNaturalHealingWaitingPeriod();
        minimumHitsForVehicles = options.getMinimumHitsForVehicles();
        useRandomHitsForVehicles = options.isUseRandomHitsForVehicles();
        tougherHealing = options.isTougherHealing();
        useAlternativeAdvancedMedical = options.isUseAlternativeAdvancedMedical();
        useKinderAlternativeAdvancedMedical = options.isUseKinderAlternativeAdvancedMedical();
        useRandomDiseases = options.isUseRandomDiseases();
        maximumPatients = options.getMaximumPatients();
        doctorsUseAdministration = options.isDoctorsUseAdministration();
        useUsefulMedics = options.isUseUsefulMedics();
        useMASHTheatres = options.isUseMASHTheatres();
        mashTheatreCapacity = options.getMASHTheatreCapacity();
        prisonerCaptureStyle = options.getPrisonerCaptureStyle();
        useFunctionalEscapeArtist = options.isUseFunctionalEscapeArtist();
        resetTemporaryPrisonerCapacity = false;
        useRandomDependentAddition = options.isUseRandomDependentAddition();
        useRandomDependentRemoval = options.isUseRandomDependentRemoval();
        dependentProfessionDieSize = options.getDependentProfessionDieSize();
        civilianProfessionDieSize = options.getCivilianProfessionDieSize();
    }

    void applyTo(@Nonnull Campaign campaign, @Nonnull CampaignOptions options) {
        options.setUseTactics(useTactics);
        options.setUseInitiativeBonus(useInitiativeBonus);
        options.setUseToughness(useToughness);
        options.setUseRandomToughness(useRandomToughness);
        options.setUseArtillery(useArtillery);
        options.setUseAbilities(useAbilities);
        options.setOnlyCommandersMatterVehicles(onlyCommandersMatterVehicles);
        options.setOnlyCommandersMatterInfantry(onlyCommandersMatterInfantry);
        options.setOnlyCommandersMatterBattleArmor(onlyCommandersMatterBattleArmor);
        options.setUseEdge(useEdge);
        options.setUseSupportEdge(useSupportEdge);
        options.setEdgeRefreshPeriod(edgeRefreshPeriod);
        options.setEdgeRefreshCost(edgeRefreshCost);
        options.setUseImplants(useImplants);
        options.setAlternativeQualityAveraging(alternativeQualityAveraging);
        options.setUsePersonnelRemoval(usePersonnelRemoval);
        options.setUseRemovalExemptCemetery(useRemovalExemptCemetery);
        options.setUseRemovalExemptRetirees(useRemovalExemptRetirees);
        options.setAdminsHaveNegotiation(adminsHaveNegotiation);
        options.setAdminExperienceLevelIncludeNegotiation(adminExperienceLevelIncludeNegotiation);
        options.setUseBlobInfantry(useBlobInfantry);
        options.setUseBlobBattleArmor(useBlobBattleArmor);
        options.setUseBlobVehicleCrewGround(useBlobVehicleCrewGround);
        options.setUseBlobVehicleCrewVTOL(useBlobVehicleCrewVTOL);
        options.setUseBlobVehicleCrewNaval(useBlobVehicleCrewNaval);
        options.setUseBlobVesselPilot(useBlobVesselPilot);
        options.setUseBlobVesselGunner(useBlobVesselGunner);
        options.setUseBlobVesselCrew(useBlobVesselCrew);
        options.setUseTransfers(useTransfers);
        options.setUseExtendedTOEForceName(useExtendedTOEForceName);
        options.setPersonnelLogSkillGain(personnelLogSkillGain);
        options.setPersonnelLogAbilityGain(personnelLogAbilityGain);
        options.setPersonnelLogEdgeGain(personnelLogEdgeGain);
        options.setDisplayPersonnelLog(displayPersonnelLog);
        options.setDisplayScenarioLog(displayScenarioLog);
        options.setDisplayKillRecord(displayKillRecord);
        options.setDisplayMedicalRecord(displayMedicalRecord);
        options.setDisplayPatientRecord(displayPatientRecord);
        options.setDisplayAssignmentRecord(displayAssignmentRecord);
        options.setDisplayPerformanceRecord(displayPerformanceRecord);
        options.setUseTimeInService(useTimeInService);
        options.setTimeInServiceDisplayFormat(timeInServiceDisplayFormat);
        options.setUseTimeInRank(useTimeInRank);
        options.setTimeInRankDisplayFormat(timeInRankDisplayFormat);
        options.setTrackTotalEarnings(trackTotalEarnings);
        options.setTrackTotalXPEarnings(trackTotalXPEarnings);
        options.setShowOriginFaction(showOriginFaction);
        options.setAwardBonusStyle(awardBonusStyle);
        options.setAwardTierSize(awardTierSize);
        options.setEnableAutoAwards(enableAutoAwards);
        options.setIssuePosthumousAwards(issuePosthumousAwards);
        options.setIssueBestAwardOnly(issueBestAwardOnly);
        options.setIgnoreStandardSet(ignoreStandardSet);
        options.setEnableContractAwards(enableContractAwards);
        options.setEnableFactionHunterAwards(enableFactionHunterAwards);
        options.setEnableInjuryAwards(enableInjuryAwards);
        options.setEnableIndividualKillAwards(enableIndividualKillAwards);
        options.setEnableFormationKillAwards(enableFormationKillAwards);
        options.setEnableRankAwards(enableRankAwards);
        options.setEnableScenarioAwards(enableScenarioAwards);
        options.setEnableSkillAwards(enableSkillAwards);
        options.setEnableTheatreOfWarAwards(enableTheatreOfWarAwards);
        options.setEnableTimeAwards(enableTimeAwards);
        options.setEnableTrainingAwards(enableTrainingAwards);
        options.setEnableMiscAwards(enableMiscAwards);
        options.setAwardSetFilterList(awardSetFilterList);
        options.setUseAdvancedMedical(useAdvancedMedical);
        options.setHealingWaitingPeriod(healingWaitingPeriod);
        options.setNaturalHealingWaitingPeriod(naturalHealingWaitingPeriod);
        options.setMinimumHitsForVehicles(minimumHitsForVehicles);
        options.setUseRandomHitsForVehicles(useRandomHitsForVehicles);
        options.setTougherHealing(tougherHealing);
        options.setUseAlternativeAdvancedMedical(useAlternativeAdvancedMedical);
        options.setUseKinderAlternativeAdvancedMedical(useKinderAlternativeAdvancedMedical);
        options.setUseRandomDiseases(useRandomDiseases);
        options.setMaximumPatients(maximumPatients);
        options.setDoctorsUseAdministration(doctorsUseAdministration);
        options.setIsUseUsefulMedics(useUsefulMedics);
        options.setIsUseMASHTheatres(useMASHTheatres);
        options.setMASHTheatreCapacity(mashTheatreCapacity);
        options.setPrisonerCaptureStyle(prisonerCaptureStyle);
        options.setUseFunctionalEscapeArtist(useFunctionalEscapeArtist);
        if (resetTemporaryPrisonerCapacity) {
            campaign.setTemporaryPrisonerCapacity(DEFAULT_TEMPORARY_CAPACITY);
        }
        options.setUseRandomDependentAddition(useRandomDependentAddition);
        options.setUseRandomDependentRemoval(useRandomDependentRemoval);
        options.setDependentProfessionDieSize(dependentProfessionDieSize);
        options.setCivilianProfessionDieSize(civilianProfessionDieSize);
    }
}
