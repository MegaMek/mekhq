/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.campaignOptions;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import megamek.common.enums.SkillLevel;
import mekhq.Utilities;
import mekhq.campaign.personnel.enums.AgeGroup;
import mekhq.campaign.personnel.enums.MergingSurnameStyle;
import mekhq.campaign.personnel.enums.SplittingSurnameStyle;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.service.mrms.MRMSOption;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author natit
 */
public class CampaignOptionsMarshaller {
    public static void writeCampaignOptionsToXML(final CampaignOptions campaignOptions, final PrintWriter pw,
          int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "campaignOptions");
        // region General Tab
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "manualUnitRatingModifier",
              campaignOptions.getManualUnitRatingModifier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clampReputationPayMultiplier",
              campaignOptions.isClampReputationPayMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "reduceReputationPerformanceModifier",
              campaignOptions.isReduceReputationPerformanceModifier());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "reputationPerformanceModifierCutOff",
              campaignOptions.isReputationPerformanceModifierCutOff());
        // endregion General Tab

        // region Repair and Maintenance Tab
        // region Maintenance
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "logMaintenance", campaignOptions.isLogMaintenance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "defaultMaintenanceTime",
              campaignOptions.getDefaultMaintenanceTime());
        // endregion Maintenance

        // region Mass Repair / Mass Salvage
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsUseRepair", campaignOptions.isMRMSUseRepair());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsUseSalvage", campaignOptions.isMRMSUseSalvage());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsUseExtraTime", campaignOptions.isMRMSUseExtraTime());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsUseRushJob", campaignOptions.isMRMSUseRushJob());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsAllowCarryover", campaignOptions.isMRMSAllowCarryover());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "mrmsOptimizeToCompleteToday",
              campaignOptions.isMRMSOptimizeToCompleteToday());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsScrapImpossible", campaignOptions.isMRMSScrapImpossible());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "mrmsUseAssignedTechsFirst",
              campaignOptions.isMRMSUseAssignedTechsFirst());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsReplacePod", campaignOptions.isMRMSReplacePod());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "mrmsOptions");
        for (final MRMSOption mrmsOption : campaignOptions.getMRMSOptions()) {
            mrmsOption.writeToXML(pw, indent);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "mrmsOptions");
        // endregion Mass Repair / Mass Salvage
        // endregion Repair and Maintenance Tab

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useFactionForNames", campaignOptions.isUseOriginFactionForNames());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useEraMods", campaignOptions.isUseEraMods());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignedTechFirst", campaignOptions.isAssignedTechFirst());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "resetToFirstTech", campaignOptions.isResetToFirstTech());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "techsUseAdministration",
              campaignOptions.isTechsUseAdministration());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useUsefulAsTechs",
              campaignOptions.isUseUsefulAsTechs());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useQuirks", campaignOptions.isUseQuirks());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "xpCostMultiplier", campaignOptions.getXpCostMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "scenarioXP", campaignOptions.getScenarioXP());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "killsForXP", campaignOptions.getKillsForXP());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "killXPAward", campaignOptions.getKillXPAward());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "nTasksXP", campaignOptions.getNTasksXP());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "tasksXP", campaignOptions.getTaskXP());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mistakeXP", campaignOptions.getMistakeXP());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "successXP", campaignOptions.getSuccessXP());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "vocationalXP", campaignOptions.getVocationalXP());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "vocationalXPTargetNumber",
              campaignOptions.getVocationalXPTargetNumber());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "vocationalXPCheckFrequency",
              campaignOptions.getVocationalXPCheckFrequency());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "contractNegotiationXP",
              campaignOptions.getContractNegotiationXP());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "adminWeeklyXP", campaignOptions.getAdminXP());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "adminXPPeriod", campaignOptions.getAdminXPPeriod());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "missionXpFail", campaignOptions.getMissionXpFail());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "missionXpSuccess", campaignOptions.getMissionXpSuccess());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "missionXpOutstandingSuccess",
              campaignOptions.getMissionXpOutstandingSuccess());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "edgeCost", campaignOptions.getEdgeCost());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "limitByYear", campaignOptions.isLimitByYear());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "disallowExtinctStuff", campaignOptions.isDisallowExtinctStuff());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowClanPurchases", campaignOptions.isAllowClanPurchases());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowISPurchases", campaignOptions.isAllowISPurchases());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowCanonOnly", campaignOptions.isAllowCanonOnly());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowCanonRefitOnly", campaignOptions.isAllowCanonRefitOnly());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "variableTechLevel", campaignOptions.isVariableTechLevel());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "factionIntroDate", campaignOptions.isFactionIntroDate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAmmoByType", campaignOptions.isUseAmmoByType());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "waitingPeriod", campaignOptions.getWaitingPeriod());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "acquisitionSkill", campaignOptions.getAcquisitionSkill());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useFunctionalAppraisal",
              campaignOptions.isUseFunctionalAppraisal());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "acquisitionPersonnelCategory",
              campaignOptions.getAcquisitionPersonnelCategory().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "techLevel", campaignOptions.getTechLevel());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitTransitTime", campaignOptions.getUnitTransitTime());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePlanetaryAcquisition",
              campaignOptions.isUsePlanetaryAcquisition());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "planetAcquisitionFactionLimit",
              campaignOptions.getPlanetAcquisitionFactionLimit().name());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "planetAcquisitionNoClanCrossover",
              campaignOptions.isPlanetAcquisitionNoClanCrossover());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "noClanPartsFromIS", campaignOptions.isNoClanPartsFromIS());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "penaltyClanPartsFromIS",
              campaignOptions.getPenaltyClanPartsFromIS());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "planetAcquisitionVerbose",
              campaignOptions.isPlanetAcquisitionVerbose());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maxJumpsPlanetaryAcquisition",
              campaignOptions.getMaxJumpsPlanetaryAcquisition());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentContractPercent",
              campaignOptions.getEquipmentContractPercent());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "dropShipContractPercent",
              campaignOptions.getDropShipContractPercent());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "jumpShipContractPercent",
              campaignOptions.getJumpShipContractPercent());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "warShipContractPercent",
              campaignOptions.getWarShipContractPercent());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentContractBase",
              campaignOptions.isEquipmentContractBase());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentContractSaleValue",
              campaignOptions.isEquipmentContractSaleValue());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "blcSaleValue", campaignOptions.isBLCSaleValue());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "overageRepaymentInFinalPayment",
              campaignOptions.isOverageRepaymentInFinalPayment());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clanAcquisitionPenalty",
              campaignOptions.getClanAcquisitionPenalty());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "isAcquisitionPenalty", campaignOptions.getIsAcquisitionPenalty());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "destroyByMargin", campaignOptions.isDestroyByMargin());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "destroyMargin", campaignOptions.getDestroyMargin());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "destroyPartTarget", campaignOptions.getDestroyPartTarget());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAeroSystemHits", campaignOptions.isUseAeroSystemHits());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maintenanceCycleDays", campaignOptions.getMaintenanceCycleDays());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maintenanceBonus", campaignOptions.getMaintenanceBonus());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useQualityMaintenance", campaignOptions.isUseQualityMaintenance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "reverseQualityNames", campaignOptions.isReverseQualityNames());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomUnitQualities",
              campaignOptions.isUseRandomUnitQualities());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePlanetaryModifiers", campaignOptions.isUsePlanetaryModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useUnofficialMaintenance",
              campaignOptions.isUseUnofficialMaintenance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "checkMaintenance", campaignOptions.isCheckMaintenance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maxAcquisitions", campaignOptions.getMaxAcquisitions());

        // autoLogistics
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoLogisticsHeatSink",
              campaignOptions.getAutoLogisticsHeatSink());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoLogisticsMekHead", campaignOptions.getAutoLogisticsMekHead());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "autoLogisticsMekLocation",
              campaignOptions.getAutoLogisticsMekLocation());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "autoLogisticsNonRepairableLocation",
              campaignOptions.getAutoLogisticsNonRepairableLocation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoLogisticsArmor", campaignOptions.getAutoLogisticsArmor());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "autoLogisticsAmmunition",
              campaignOptions.getAutoLogisticsAmmunition());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "autoLogisticsActuators",
              campaignOptions.getAutoLogisticsActuators());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "autoLogisticsJumpJets",
              campaignOptions.getAutoLogisticsJumpJets());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoLogisticsEngines", campaignOptions.getAutoLogisticsEngines());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoLogisticsWeapons", campaignOptions.getAutoLogisticsWeapons());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoLogisticsOther", campaignOptions.getAutoLogisticsOther());

        // region Personnel Tab
        // region General Personnel
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useTactics", campaignOptions.isUseTactics());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useInitiativeBonus", campaignOptions.isUseInitiativeBonus());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useToughness", campaignOptions.isUseToughness());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomToughness", campaignOptions.isUseRandomToughness());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useArtillery", campaignOptions.isUseArtillery());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAbilities", campaignOptions.isUseAbilities());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useCommanderAbilitiesOnly",
              campaignOptions.isUseCommanderAbilitiesOnly());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useEdge", campaignOptions.isUseEdge());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useSupportEdge", campaignOptions.isUseSupportEdge());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useImplants", campaignOptions.isUseImplants());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "alternativeQualityAveraging",
              campaignOptions.isAlternativeQualityAveraging());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAgeEffects", campaignOptions.isUseAgeEffects());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useTransfers", campaignOptions.isUseTransfers());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useExtendedTOEForceName",
              campaignOptions.isUseExtendedTOEForceName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personnelLogSkillGain", campaignOptions.isPersonnelLogSkillGain());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "personnelLogAbilityGain",
              campaignOptions.isPersonnelLogAbilityGain());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personnelLogEdgeGain", campaignOptions.isPersonnelLogEdgeGain());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "displayPersonnelLog", campaignOptions.isDisplayPersonnelLog());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "displayScenarioLog", campaignOptions.isDisplayScenarioLog());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "displayKillRecord", campaignOptions.isDisplayKillRecord());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "displayMedicalRecord", campaignOptions.isDisplayMedicalRecord());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "displayPatientRecord", campaignOptions.isDisplayPatientRecord());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "displayAssignmentRecord",
              campaignOptions.isDisplayAssignmentRecord());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "displayPerformanceRecord",
              campaignOptions.isDisplayPerformanceRecord());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "rewardComingOfAgeAbilities",
              campaignOptions.isRewardComingOfAgeAbilities());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "rewardComingOfAgeRPSkills",
              campaignOptions.isRewardComingOfAgeRPSkills());
        // endregion General Personnel

        // region Expanded Personnel Information
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useTimeInService", campaignOptions.isUseTimeInService());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "timeInServiceDisplayFormat",
              campaignOptions.getTimeInServiceDisplayFormat().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useTimeInRank", campaignOptions.isUseTimeInRank());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "timeInRankDisplayFormat",
              campaignOptions.getTimeInRankDisplayFormat().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "trackTotalEarnings", campaignOptions.isTrackTotalEarnings());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "trackTotalXPEarnings", campaignOptions.isTrackTotalXPEarnings());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "showOriginFaction", campaignOptions.isShowOriginFaction());
        // endregion Expanded Personnel Information

        // region Admin
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "adminsHaveNegotiation", campaignOptions.isAdminsHaveNegotiation());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "adminExperienceLevelIncludeNegotiation",
              campaignOptions.isAdminExperienceLevelIncludeNegotiation());
        // endregion Admin

        // region Medical
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAdvancedMedical", campaignOptions.isUseAdvancedMedical());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "healWaitingPeriod", campaignOptions.getHealingWaitingPeriod());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "naturalHealingWaitingPeriod",
              campaignOptions.getNaturalHealingWaitingPeriod());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "minimumHitsForVehicles",
              campaignOptions.getMinimumHitsForVehicles());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomHitsForVehicles",
              campaignOptions.isUseRandomHitsForVehicles());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "tougherHealing", campaignOptions.isTougherHealing());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maximumPatients", campaignOptions.getMaximumPatients());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "doctorsUseAdministration",
              campaignOptions.isDoctorsUseAdministration());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useUsefulMedics", campaignOptions.isUseUsefulMedics());
        // endregion Medical

        // region Prisoners
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "prisonerCaptureStyle",
              campaignOptions.getPrisonerCaptureStyle().name());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useFunctionalEscapeArtist",
              campaignOptions.isUseFunctionalEscapeArtist());
        // endregion Prisoners

        // region Dependent
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomDependentAddition",
              campaignOptions.isUseRandomDependentAddition());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomDependentRemoval",
              campaignOptions.isUseRandomDependentRemoval());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "dependentProfessionDieSize",
              campaignOptions.getDependentProfessionDieSize());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "civilianProfessionDieSize",
              campaignOptions.getCivilianProfessionDieSize());
        // endregion Dependent

        // region Personnel Removal
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePersonnelRemoval", campaignOptions.isUsePersonnelRemoval());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRemovalExemptCemetery",
              campaignOptions.isUseRemovalExemptCemetery());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRemovalExemptRetirees",
              campaignOptions.isUseRemovalExemptRetirees());
        // endregion Personnel Removal

        // region Salary
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "disableSecondaryRoleSalary",
              campaignOptions.isDisableSecondaryRoleSalary());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "salaryAntiMekMultiplier",
              campaignOptions.getSalaryAntiMekMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "salarySpecialistInfantryMultiplier",
              campaignOptions.getSalarySpecialistInfantryMultiplier());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "salaryXPMultipliers");
        for (final Entry<SkillLevel, Double> entry : campaignOptions.getSalaryXPMultipliers().entrySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, entry.getKey().name(), entry.getValue());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "salaryXPMultipliers");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salaryTypeBase",
              Utilities.printMoneyArray(campaignOptions.getRoleBaseSalaries()));
        // endregion Salary

        // region Awards
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "awardBonusStyle", campaignOptions.getAwardBonusStyle().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableAutoAwards", campaignOptions.isEnableAutoAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "issuePosthumousAwards", campaignOptions.isIssuePosthumousAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "issueBestAwardOnly", campaignOptions.isIssueBestAwardOnly());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "ignoreStandardSet", campaignOptions.isIgnoreStandardSet());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "awardTierSize", campaignOptions.getAwardTierSize());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableContractAwards", campaignOptions.isEnableContractAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "enableFactionHunterAwards",
              campaignOptions.isEnableFactionHunterAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableInjuryAwards", campaignOptions.isEnableInjuryAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "enableIndividualKillAwards",
              campaignOptions.isEnableIndividualKillAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "enableFormationKillAwards",
              campaignOptions.isEnableFormationKillAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableRankAwards", campaignOptions.isEnableRankAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableScenarioAwards", campaignOptions.isEnableScenarioAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableSkillAwards", campaignOptions.isEnableSkillAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "enableTheatreOfWarAwards",
              campaignOptions.isEnableTheatreOfWarAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableTimeAwards", campaignOptions.isEnableTimeAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableTrainingAwards", campaignOptions.isEnableTrainingAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableMiscAwards", campaignOptions.isEnableMiscAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "awardSetFilterList", campaignOptions.getAwardSetFilterList());
        // endregion Awards
        // endregion Personnel Tab

        // region Life Paths Tab
        // region Personnel Randomization
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useDylansRandomXP", campaignOptions.isUseDylansRandomXP());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "nonBinaryDiceSize", campaignOptions.getNonBinaryDiceSize());
        // endregion Personnel Randomization

        // region Random Histories
        campaignOptions.getRandomOriginOptions().writeToXML(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomPersonalities",
              campaignOptions.isUseRandomPersonalities());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomPersonalityReputation",
              campaignOptions.isUseRandomPersonalityReputation());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useReasoningXpMultiplier",
              campaignOptions.isUseReasoningXpMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useSimulatedRelationships",
              campaignOptions.isUseSimulatedRelationships());
        // endregion Random Histories

        // region Retirement
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomRetirement", campaignOptions.isUseRandomRetirement());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "turnoverBaseTn", campaignOptions.getTurnoverFixedTargetNumber());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "turnoverFrequency", campaignOptions.getTurnoverFrequency().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "aeroRecruitsHaveUnits", campaignOptions.isAeroRecruitsHaveUnits());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "trackOriginalUnit", campaignOptions.isTrackOriginalUnit());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useContractCompletionRandomRetirement",
              campaignOptions.isUseContractCompletionRandomRetirement());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomFounderTurnover",
              campaignOptions.isUseRandomFounderTurnover());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useFounderRetirement", campaignOptions.isUseFounderRetirement());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useSubContractSoldiers",
              campaignOptions.isUseSubContractSoldiers());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "serviceContractDuration",
              campaignOptions.getServiceContractDuration());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "serviceContractModifier",
              campaignOptions.getServiceContractModifier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payBonusDefault", campaignOptions.isPayBonusDefault());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "payBonusDefaultThreshold",
              campaignOptions.getPayBonusDefaultThreshold());

        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useCustomRetirementModifiers",
              campaignOptions.isUseCustomRetirementModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useFatigueModifiers", campaignOptions.isUseFatigueModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useSkillModifiers", campaignOptions.isUseSkillModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAgeModifiers", campaignOptions.isUseAgeModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useUnitRatingModifiers",
              campaignOptions.isUseUnitRatingModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useFactionModifiers", campaignOptions.isUseFactionModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useMissionStatusModifiers",
              campaignOptions.isUseMissionStatusModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useHostileTerritoryModifiers",
              campaignOptions.isUseHostileTerritoryModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useFamilyModifiers", campaignOptions.isUseFamilyModifiers());

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useLoyaltyModifiers", campaignOptions.isUseLoyaltyModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useHideLoyalty", campaignOptions.isUseHideLoyalty());

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payoutRateOfficer", campaignOptions.getPayoutRateOfficer());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payoutRateEnlisted", campaignOptions.getPayoutRateEnlisted());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "payoutRetirementMultiplier",
              campaignOptions.getPayoutRetirementMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePayoutServiceBonus", campaignOptions.isUsePayoutServiceBonus());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "payoutServiceBonusRate",
              campaignOptions.getPayoutServiceBonusRate());

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "UseHRStrain", campaignOptions.isUseHRStrain());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "hrStrain", campaignOptions.getHRCapacity());

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useManagementSkill", campaignOptions.isUseManagementSkill());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useCommanderLeadershipOnly",
              campaignOptions.isUseCommanderLeadershipOnly());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "managementSkillPenalty",
              campaignOptions.getManagementSkillPenalty());

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useFatigue", campaignOptions.isUseFatigue());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fatigueRate", campaignOptions.getFatigueRate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useInjuryFatigue", campaignOptions.isUseInjuryFatigue());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fieldKitchenCapacity", campaignOptions.getFieldKitchenCapacity());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "fieldKitchenIgnoreNonCombatants",
              campaignOptions.isUseFieldKitchenIgnoreNonCombatants());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "fatigueUndeploymentThreshold",
              campaignOptions.getFatigueUndeploymentThreshold());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "fatigueLeaveThreshold",
              campaignOptions.getFatigueLeaveThreshold());
        // endregion Retirement

        // region Family
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "familyDisplayLevel",
              campaignOptions.getFamilyDisplayLevel().name());
        // endregion Family

        // region Announcements
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "announceBirthdays", campaignOptions.isAnnounceBirthdays());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "announceRecruitmentAnniversaries",
              campaignOptions.isAnnounceRecruitmentAnniversaries());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "announceOfficersOnly", campaignOptions.isAnnounceOfficersOnly());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "announceChildBirthdays",
              campaignOptions.isAnnounceChildBirthdays());
        // endregion Announcements

        // region Life Events
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "showLifeEventDialogBirths",
              campaignOptions.isShowLifeEventDialogBirths());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "showLifeEventDialogComingOfAge",
              campaignOptions.isShowLifeEventDialogComingOfAge());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "showLifeEventDialogCelebrations",
              campaignOptions.isShowLifeEventDialogCelebrations());
        // endregion Life Events

        // region Marriage
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useManualMarriages", campaignOptions.isUseManualMarriages());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useClanPersonnelMarriages",
              campaignOptions.isUseClanPersonnelMarriages());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePrisonerMarriages", campaignOptions.isUsePrisonerMarriages());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "checkMutualAncestorsDepth",
              campaignOptions.getCheckMutualAncestorsDepth());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "noInterestInMarriageDiceSize",
              campaignOptions.getNoInterestInMarriageDiceSize());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "logMarriageNameChanges",
              campaignOptions.isLogMarriageNameChanges());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "marriageSurnameWeights");
        for (final Entry<MergingSurnameStyle, Integer> entry : campaignOptions.getMarriageSurnameWeights().entrySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, entry.getKey().name(), entry.getValue());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "marriageSurnameWeights");
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "randomMarriageMethod",
              campaignOptions.getRandomMarriageMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomClanPersonnelMarriages",
              campaignOptions.isUseRandomClanPersonnelMarriages());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomPrisonerMarriages",
              campaignOptions.isUseRandomPrisonerMarriages());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "randomMarriageAgeRange",
              campaignOptions.getRandomMarriageAgeRange());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "randomMarriageDiceSize",
              campaignOptions.getRandomMarriageDiceSize());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "randomSameSexMarriageDiceSize",
              campaignOptions.getRandomSameSexMarriageDiceSize());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "randomNewDependentMarriage",
              campaignOptions.getRandomNewDependentMarriage());
        // endregion Marriage

        // region Divorce
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useManualDivorce", campaignOptions.isUseManualDivorce());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useClanPersonnelDivorce",
              campaignOptions.isUseClanPersonnelDivorce());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePrisonerDivorce", campaignOptions.isUsePrisonerDivorce());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "divorceSurnameWeights");
        for (final Entry<SplittingSurnameStyle, Integer> entry : campaignOptions.getDivorceSurnameWeights()
                                                                       .entrySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, entry.getKey().name(), entry.getValue());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "divorceSurnameWeights");
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "randomDivorceMethod",
              campaignOptions.getRandomDivorceMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomOppositeSexDivorce",
              campaignOptions.isUseRandomOppositeSexDivorce());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomSameSexDivorce",
              campaignOptions.isUseRandomSameSexDivorce());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomClanPersonnelDivorce",
              campaignOptions.isUseRandomClanPersonnelDivorce());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomPrisonerDivorce",
              campaignOptions.isUseRandomPrisonerDivorce());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "randomDivorceDiceSize",
              campaignOptions.getRandomDivorceDiceSize());
        // endregion Divorce

        // region Procreation
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useManualProcreation", campaignOptions.isUseManualProcreation());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useClanPersonnelProcreation",
              campaignOptions.isUseClanPersonnelProcreation());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "usePrisonerProcreation",
              campaignOptions.isUsePrisonerProcreation());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "multiplePregnancyOccurrences",
              campaignOptions.getMultiplePregnancyOccurrences());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "babySurnameStyle", campaignOptions.getBabySurnameStyle().name());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "assignNonPrisonerBabiesFounderTag",
              campaignOptions.isAssignNonPrisonerBabiesFounderTag());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "assignChildrenOfFoundersFounderTag",
              campaignOptions.isAssignChildrenOfFoundersFounderTag());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useMaternityLeave", campaignOptions.isUseMaternityLeave());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "determineFatherAtBirth",
              campaignOptions.isDetermineFatherAtBirth());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "displayTrueDueDate", campaignOptions.isDisplayTrueDueDate());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "noInterestInChildrenDiceSize",
              campaignOptions.getNoInterestInChildrenDiceSize());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "logProcreation", campaignOptions.isLogProcreation());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "randomProcreationMethod",
              campaignOptions.getRandomProcreationMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRelationshiplessRandomProcreation",
              campaignOptions.isUseRelationshiplessRandomProcreation());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomClanPersonnelProcreation",
              campaignOptions.isUseRandomClanPersonnelProcreation());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomPrisonerProcreation",
              campaignOptions.isUseRandomPrisonerProcreation());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "randomProcreationRelationshipDiceSize",
              campaignOptions.getRandomProcreationRelationshipDiceSize());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "randomProcreationRelationshiplessDiceSize",
              campaignOptions.getRandomProcreationRelationshiplessDiceSize());
        // endregion Procreation

        // region Education
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useEducationModule", campaignOptions.isUseEducationModule());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "curriculumXpRate", campaignOptions.getCurriculumXpRate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maximumJumpCount", campaignOptions.getMaximumJumpCount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useReeducationCamps", campaignOptions.isUseReeducationCamps());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableLocalAcademies", campaignOptions.isEnableLocalAcademies());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "enablePrestigiousAcademies",
              campaignOptions.isEnablePrestigiousAcademies());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableUnitEducation", campaignOptions.isEnableUnitEducation());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "enableOverrideRequirements",
              campaignOptions.isEnableOverrideRequirements());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "enableShowIneligibleAcademies",
              campaignOptions.isEnableShowIneligibleAcademies());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "entranceExamBaseTargetNumber",
              campaignOptions.getEntranceExamBaseTargetNumber());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "facultyXpRate", campaignOptions.getFacultyXpRate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableBonuses", campaignOptions.isEnableBonuses());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "adultDropoutChance", campaignOptions.getAdultDropoutChance());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "childrenDropoutChance",
              campaignOptions.getChildrenDropoutChance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allAges", campaignOptions.isAllAges());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "militaryAcademyAccidents",
              campaignOptions.getMilitaryAcademyAccidents());
        // endregion Education

        // region Death
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "enabledRandomDeathAgeGroups");
        for (final Entry<AgeGroup, Boolean> entry : campaignOptions.getEnabledRandomDeathAgeGroups().entrySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, entry.getKey().name(), entry.getValue());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "enabledRandomDeathAgeGroups");
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useRandomDeathSuicideCause",
              campaignOptions.isUseRandomDeathSuicideCause());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "randomDeathMultiplier",
              campaignOptions.getRandomDeathMultiplier());
        // endregion Death
        // endregion Life Paths Tab

        // region Finances Tab
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForParts", campaignOptions.isPayForParts());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForRepairs", campaignOptions.isPayForRepairs());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForUnits", campaignOptions.isPayForUnits());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForSalaries", campaignOptions.isPayForSalaries());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForOverhead", campaignOptions.isPayForOverhead());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForMaintain", campaignOptions.isPayForMaintain());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForTransport", campaignOptions.isPayForTransport());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sellUnits", campaignOptions.isSellUnits());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sellParts", campaignOptions.isSellParts());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForRecruitment", campaignOptions.isPayForRecruitment());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForFood", campaignOptions.isPayForFood());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForHousing", campaignOptions.isPayForHousing());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useLoanLimits", campaignOptions.isUseLoanLimits());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePercentageMaint", campaignOptions.isUsePercentageMaintenance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "infantryDontCount", campaignOptions.isInfantryDontCount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePeacetimeCost", campaignOptions.isUsePeacetimeCost());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useExtendedPartsModifier",
              campaignOptions.isUseExtendedPartsModifier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "showPeacetimeCost", campaignOptions.isShowPeacetimeCost());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "financialYearDuration",
              campaignOptions.getFinancialYearDuration().name());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "newFinancialYearFinancesToCSVExport",
              campaignOptions.isNewFinancialYearFinancesToCSVExport());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "simulateGrayMonday", campaignOptions.isSimulateGrayMonday());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "allowMonthlyReinvestment",
              campaignOptions.isAllowMonthlyReinvestment());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "displayAllAttributes", campaignOptions.isDisplayAllAttributes());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowMonthlyConnections",
              campaignOptions.isAllowMonthlyConnections());

        // region Price Multipliers
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "commonPartPriceMultiplier",
              campaignOptions.getCommonPartPriceMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "innerSphereUnitPriceMultiplier",
              campaignOptions.getInnerSphereUnitPriceMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "innerSpherePartPriceMultiplier",
              campaignOptions.getInnerSpherePartPriceMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "clanUnitPriceMultiplier",
              campaignOptions.getClanUnitPriceMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "clanPartPriceMultiplier",
              campaignOptions.getClanPartPriceMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "mixedTechUnitPriceMultiplier",
              campaignOptions.getMixedTechUnitPriceMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "usedPartPriceMultipliers",
              campaignOptions.getUsedPartPriceMultipliers());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "damagedPartsValueMultiplier",
              campaignOptions.getDamagedPartsValueMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "unrepairablePartsValueMultiplier",
              campaignOptions.getUnrepairablePartsValueMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "cancelledOrderRefundMultiplier",
              campaignOptions.getCancelledOrderRefundMultiplier());

        // Shares
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useShareSystem", campaignOptions.isUseShareSystem());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sharesForAll", campaignOptions.isSharesForAll());
        // endregion Price Multipliers

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useTaxes", campaignOptions.isUseTaxes());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "taxesPercentage", campaignOptions.getTaxesPercentage());
        // region Taxes
        // endregion Taxes
        // endregion Finances Tab

        // region Markets Tab
        // region Personnel Market
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "personnelMarketStyle",
              campaignOptions.getPersonnelMarketStyle().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personnelMarketName", campaignOptions.getPersonnelMarketName());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "personnelMarketReportRefresh",
              campaignOptions.isPersonnelMarketReportRefresh());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "personnelMarketRandomRemovalTargets");
        for (final Entry<SkillLevel, Integer> entry : campaignOptions.getPersonnelMarketRandomRemovalTargets()
                                                            .entrySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, entry.getKey().name(), entry.getValue());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "personnelMarketRandomRemovalTargets");
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "personnelMarketDylansWeight",
              campaignOptions.getPersonnelMarketDylansWeight());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "usePersonnelHireHiringHallOnly",
              campaignOptions.isUsePersonnelHireHiringHallOnly());
        // endregion Personnel Market

        // region Unit Market
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitMarketMethod", campaignOptions.getUnitMarketMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "unitMarketRegionalMekVariations",
              campaignOptions.isUnitMarketRegionalMekVariations());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "unitMarketArtilleryUnitChance",
              campaignOptions.getUnitMarketArtilleryUnitChance());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "unitMarketRarityModifier",
              campaignOptions.getUnitMarketRarityModifier());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "instantUnitMarketDelivery",
              campaignOptions.isInstantUnitMarketDelivery());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "mothballUnitMarketDeliveries",
              campaignOptions.isMothballUnitMarketDeliveries());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "unitMarketReportRefresh",
              campaignOptions.isUnitMarketReportRefresh());
        // endregion Unit Market

        // region Contract Market
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "contractMarketMethod",
              campaignOptions.getContractMarketMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "contractSearchRadius", campaignOptions.getContractSearchRadius());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "variableContractLength",
              campaignOptions.isVariableContractLength());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useDynamicDifficulty", campaignOptions.isUseDynamicDifficulty());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "contractMarketReportRefresh",
              campaignOptions.isContractMarketReportRefresh());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "contractMaxSalvagePercentage",
              campaignOptions.getContractMaxSalvagePercentage());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "dropShipBonusPercentage",
              campaignOptions.getDropShipBonusPercentage());
        // endregion Contract Market
        // endregion Markets Tab

        // region AtB Tab
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "skillLevel", campaignOptions.getSkillLevel().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoResolveMethod", campaignOptions.getAutoResolveMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "autoResolveVictoryChanceEnabled",
              campaignOptions.isAutoResolveVictoryChanceEnabled());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "autoResolveNumberOfScenarios",
              campaignOptions.getAutoResolveNumberOfScenarios());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "autoResolveUseExperimentalPacarGui",
              campaignOptions.isAutoResolveExperimentalPacarGuiEnabled());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "strategicViewTheme",
              campaignOptions.getStrategicViewTheme().getName());
        // endregion AtB Tab

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "phenotypeProbabilities",
              campaignOptions.getPhenotypeProbabilities());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAtB", campaignOptions.isUseAtB());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useStratCon", campaignOptions.isUseStratCon());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAdvancedScouting", campaignOptions.isUseAdvancedScouting());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAero", campaignOptions.isUseAero());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useVehicles", campaignOptions.isUseVehicles());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clanVehicles", campaignOptions.isClanVehicles());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useGenericBattleValue", campaignOptions.isUseGenericBattleValue());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useVerboseBidding", campaignOptions.isUseVerboseBidding());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "doubleVehicles", campaignOptions.isDoubleVehicles());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "adjustPlayerVehicles", campaignOptions.isAdjustPlayerVehicles());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "opForLanceTypeMeks", campaignOptions.getOpForLanceTypeMeks());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "opForLanceTypeMixed", campaignOptions.getOpForLanceTypeMixed());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "opForLanceTypeVehicles",
              campaignOptions.getOpForLanceTypeVehicles());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "opForUsesVTOLs", campaignOptions.isOpForUsesVTOLs());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useDropShips", campaignOptions.isUseDropShips());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mercSizeLimited", campaignOptions.isMercSizeLimited());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "regionalMekVariations", campaignOptions.isRegionalMekVariations());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "attachedPlayerCamouflage",
              campaignOptions.isAttachedPlayerCamouflage());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "playerControlsAttachedUnits",
              campaignOptions.isPlayerControlsAttachedUnits());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "atbBattleChance", campaignOptions.getAllAtBBattleChances());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "generateChases", campaignOptions.isGenerateChases());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useWeatherConditions", campaignOptions.isUseWeatherConditions());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useLightConditions", campaignOptions.isUseLightConditions());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePlanetaryConditions",
              campaignOptions.isUsePlanetaryConditions());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "restrictPartsByMission",
              campaignOptions.isRestrictPartsByMission());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignPortraitOnRoleChange",
              campaignOptions.isAssignPortraitOnRoleChange());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowDuplicatePortraits",
              campaignOptions.isAllowDuplicatePortraits());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowOpForAeros", campaignOptions.isAllowOpForAerospace());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowOpForLocalUnits", campaignOptions.isAllowOpForLocalUnits());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "opForAeroChance", campaignOptions.getOpForAeroChance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "opForLocalUnitChance", campaignOptions.getOpForLocalUnitChance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fixedMapChance", campaignOptions.getFixedMapChance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "spaUpgradeIntensity", campaignOptions.getSpaUpgradeIntensity());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "scenarioModMax", campaignOptions.getScenarioModMax());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "scenarioModChance", campaignOptions.getScenarioModChance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "scenarioModBV", campaignOptions.getScenarioModBV());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoConfigMunitions", campaignOptions.isAutoConfigMunitions());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoGenerateOpForCallSigns",
              campaignOptions.isAutoGenerateOpForCallSigns());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "minimumCallsignSkillLevel",
              campaignOptions.getMinimumCallsignSkillLevel().name());

        String planetTechAcquisitionBonusString = Arrays.stream(PlanetarySophistication.values())
                                                        .map(sophistication -> campaignOptions.getAllPlanetTechAcquisitionBonuses()
                                                                                     .getOrDefault(sophistication, 0)
                                                                                     .toString())
                                                        .collect(Collectors.joining(","));
        String planetIndustryAcquisitionBonusString = Arrays.stream(PlanetaryRating.values())
                                                            .map(rating -> campaignOptions.getAllPlanetIndustryAcquisitionBonuses()
                                                                                 .getOrDefault(rating, 0)
                                                                                 .toString())
                                                            .collect(Collectors.joining(","));
        String planetOutputAcquisitionBonusString = Arrays.stream(PlanetaryRating.values())
                                                          .map(rating -> campaignOptions.getAllPlanetOutputAcquisitionBonuses()
                                                                               .getOrDefault(rating, 0)
                                                                               .toString())
                                                          .collect(Collectors.joining(","));
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "planetTechAcquisitionBonus", planetTechAcquisitionBonusString);
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "planetIndustryAcquisitionBonus",
              planetIndustryAcquisitionBonusString);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "planetOutputAcquisitionBonus", planetOutputAcquisitionBonusString);

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePortraitForType", campaignOptions.isUsePortraitForRoles());

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "trackFactionStanding", campaignOptions.isTrackFactionStanding());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "trackClimateRegardChanges",
              campaignOptions.isTrackClimateRegardChanges());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useFactionStandingNegotiation",
              campaignOptions.isUseFactionStandingNegotiation());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useFactionStandingResupply",
              campaignOptions.isUseFactionStandingResupply());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useFactionStandingCommandCircuit",
              campaignOptions.isUseFactionStandingCommandCircuit());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useFactionStandingOutlawed",
              campaignOptions.isUseFactionStandingOutlawed());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useFactionStandingBatchallRestrictions",
              campaignOptions.isUseFactionStandingBatchallRestrictions());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useFactionStandingRecruitment",
              campaignOptions.isUseFactionStandingRecruitment());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useFactionStandingBarracksCosts",
              campaignOptions.isUseFactionStandingBarracksCosts());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useFactionStandingUnitMarket",
              campaignOptions.isUseFactionStandingUnitMarket());
        MHQXMLUtility.writeSimpleXMLTag(pw,
              indent,
              "useFactionStandingContractPay",
              campaignOptions.isUseFactionStandingContractPay());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useFactionStandingSupportPoints",
              campaignOptions.isUseFactionStandingSupportPoints());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "factionStandingGainMultiplier",
              campaignOptions.getRegardMultiplier());

        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "campaignOptions");
    }
}
