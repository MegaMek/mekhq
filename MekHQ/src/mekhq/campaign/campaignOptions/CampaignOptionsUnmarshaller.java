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

import static mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick.ALL;
import static mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick.SUPPORT;

import java.util.EnumMap;

import megamek.Version;
import megamek.codeUtilities.MathUtility;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.Utilities;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.autoresolve.AutoResolveMethod;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerCaptureStyle;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick;
import mekhq.service.mrms.MRMSOption;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CampaignOptionsUnmarshaller {
    private static final MMLogger LOGGER = MMLogger.create(CampaignOptionsUnmarshaller.class);

    public static CampaignOptions generateCampaignOptionsFromXml(Node parentNod, Version version) {
        LOGGER.info("Loading Campaign Options from Version {} XML...", version);

        parentNod.normalize();
        CampaignOptions campaignOptions = new CampaignOptions();
        NodeList childNodes = parentNod.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);

            // If it's not an element node, we ignore it.
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = childNode.getNodeName();
            String nodeContents = childNode.getTextContent().trim();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{}\n\t{}", nodeName, nodeContents);
            }

            try {
                parseNodeName(version, nodeName, campaignOptions, nodeContents, childNode);
            } catch (Exception ex) {
                LOGGER.error(ex, "Exception parsing campaign option node: {}", nodeName);
            }
        }

        LOGGER.debug("Load Campaign Options Complete!");
        return campaignOptions;
    }

    private static void parseNodeName(Version version, String nodeName, CampaignOptions campaignOptions,
          String nodeContents, Node childNode) {
        switch (nodeName) {
            case "checkMaintenance" -> campaignOptions.setCheckMaintenance(Boolean.parseBoolean(nodeContents));
            case "maintenanceCycleDays" -> campaignOptions.setMaintenanceCycleDays(Integer.parseInt(nodeContents));
            case "maintenanceBonus" -> campaignOptions.setMaintenanceBonus(Integer.parseInt(nodeContents));
            case "useQualityMaintenance" ->
                  campaignOptions.setUseQualityMaintenance(Boolean.parseBoolean(nodeContents));
            case "reverseQualityNames" -> campaignOptions.setReverseQualityNames(Boolean.parseBoolean(nodeContents));
            case "useRandomUnitQualities" ->
                  campaignOptions.setUseRandomUnitQualities(Boolean.parseBoolean(nodeContents));
            case "usePlanetaryModifiers" ->
                  campaignOptions.setUsePlanetaryModifiers(Boolean.parseBoolean(nodeContents));
            case "useUnofficialMaintenance" -> campaignOptions.setUseUnofficialMaintenance(Boolean.parseBoolean(
                  nodeContents));
            case "logMaintenance" -> campaignOptions.setLogMaintenance(Boolean.parseBoolean(nodeContents));
            case "defaultMaintenanceTime" -> campaignOptions.setDefaultMaintenanceTime(Integer.parseInt(nodeContents));
            case "mrmsUseRepair" -> campaignOptions.setMRMSUseRepair(Boolean.parseBoolean(nodeContents));
            case "mrmsUseSalvage" -> campaignOptions.setMRMSUseSalvage(Boolean.parseBoolean(nodeContents));
            case "mrmsUseExtraTime" -> campaignOptions.setMRMSUseExtraTime(Boolean.parseBoolean(nodeContents));
            case "mrmsUseRushJob" -> campaignOptions.setMRMSUseRushJob(Boolean.parseBoolean(nodeContents));
            case "mrmsAllowCarryover" -> campaignOptions.setMRMSAllowCarryover(Boolean.parseBoolean(nodeContents));
            case "mrmsOptimizeToCompleteToday" -> campaignOptions.setMRMSOptimizeToCompleteToday(Boolean.parseBoolean(
                  nodeContents));
            case "mrmsScrapImpossible" -> campaignOptions.setMRMSScrapImpossible(Boolean.parseBoolean(nodeContents));
            case "mrmsUseAssignedTechsFirst" -> campaignOptions.setMRMSUseAssignedTechsFirst(Boolean.parseBoolean(
                  nodeContents));
            case "mrmsReplacePod" -> campaignOptions.setMRMSReplacePod(Boolean.parseBoolean(nodeContents));
            case "mrmsOptions" -> campaignOptions.setMRMSOptions(MRMSOption.parseListFromXML(childNode, version));
            case "useFactionForNames" ->
                  campaignOptions.setUseOriginFactionForNames(Boolean.parseBoolean(nodeContents));
            case "useEraMods" -> campaignOptions.setEraMods(Boolean.parseBoolean(nodeContents));
            case "assignedTechFirst" -> campaignOptions.setAssignedTechFirst(Boolean.parseBoolean(nodeContents));
            case "resetToFirstTech" -> campaignOptions.setResetToFirstTech(Boolean.parseBoolean(nodeContents));
            case "techsUseAdministration" ->
                  campaignOptions.setTechsUseAdministration(Boolean.parseBoolean(nodeContents));
            case "useQuirks" -> campaignOptions.setQuirks(Boolean.parseBoolean(nodeContents));
            case "xpCostMultiplier" -> campaignOptions.setXpCostMultiplier(Double.parseDouble(nodeContents));
            case "scenarioXP" -> campaignOptions.setScenarioXP(Integer.parseInt(nodeContents));
            case "killsForXP" -> campaignOptions.setKillsForXP(Integer.parseInt(nodeContents));
            case "killXPAward" -> campaignOptions.setKillXPAward(Integer.parseInt(nodeContents));
            case "nTasksXP" -> campaignOptions.setNTasksXP(Integer.parseInt(nodeContents));
            case "tasksXP" -> campaignOptions.setTaskXP(Integer.parseInt(nodeContents));
            case "successXP" -> campaignOptions.setSuccessXP(Integer.parseInt(nodeContents));
            case "mistakeXP" -> campaignOptions.setMistakeXP(Integer.parseInt(nodeContents));
            case "vocationalXP" -> campaignOptions.setVocationalXP(Integer.parseInt(nodeContents));
            case "vocationalXPTargetNumber" ->
                  campaignOptions.setVocationalXPTargetNumber(Integer.parseInt(nodeContents));
            case "vocationalXPCheckFrequency" -> campaignOptions.setVocationalXPCheckFrequency(Integer.parseInt(
                  nodeContents));
            case "contractNegotiationXP" -> campaignOptions.setContractNegotiationXP(Integer.parseInt(nodeContents));
            case "adminWeeklyXP" -> campaignOptions.setAdminXP(Integer.parseInt(nodeContents));
            case "adminXPPeriod" -> campaignOptions.setAdminXPPeriod(Integer.parseInt(nodeContents));
            case "missionXpFail" -> campaignOptions.setMissionXpFail(Integer.parseInt(nodeContents));
            case "missionXpSuccess" -> campaignOptions.setMissionXpSuccess(Integer.parseInt(nodeContents));
            case "missionXpOutstandingSuccess" -> campaignOptions.setMissionXpOutstandingSuccess(Integer.parseInt(
                  nodeContents));
            case "edgeCost" -> campaignOptions.setEdgeCost(Integer.parseInt(nodeContents));
            case "waitingPeriod" -> campaignOptions.setWaitingPeriod(Integer.parseInt(nodeContents));
            case "acquisitionSkill" -> campaignOptions.setAcquisitionSkill(nodeContents);
            case "unitTransitTime" -> campaignOptions.setUnitTransitTime(Integer.parseInt(nodeContents));
            case "clanAcquisitionPenalty" -> campaignOptions.setClanAcquisitionPenalty(Integer.parseInt(nodeContents));
            case "isAcquisitionPenalty" -> campaignOptions.setIsAcquisitionPenalty(Integer.parseInt(nodeContents));
            case "usePlanetaryAcquisition" ->
                  campaignOptions.setPlanetaryAcquisition(Boolean.parseBoolean(nodeContents));
            case "planetAcquisitionFactionLimit" ->
                  campaignOptions.setPlanetAcquisitionFactionLimit(PlanetaryAcquisitionFactionLimit.parseFromString(
                        nodeContents));
            case "planetAcquisitionNoClanCrossover" ->
                  campaignOptions.setDisallowPlanetAcquisitionClanCrossover(Boolean.parseBoolean(
                        nodeContents));
            case "noClanPartsFromIS" -> campaignOptions.setDisallowClanPartsFromIS(Boolean.parseBoolean(nodeContents));
            case "penaltyClanPartsFromIS" -> campaignOptions.setPenaltyClanPartsFromIS(Integer.parseInt(nodeContents));
            case "planetAcquisitionVerbose" ->
                  campaignOptions.setPlanetAcquisitionVerboseReporting(Boolean.parseBoolean(
                        nodeContents));
            case "maxJumpsPlanetaryAcquisition" -> campaignOptions.setMaxJumpsPlanetaryAcquisition(Integer.parseInt(
                  nodeContents));
            case "planetTechAcquisitionBonus" -> {
                EnumMap<PlanetarySophistication, Integer> acquisitionBonuses = campaignOptions.getAllPlanetTechAcquisitionBonuses();

                String[] values = nodeContents.split(",");
                if (values.length == 6) {
                    // < 0.50.07 compatibility handler
                    acquisitionBonuses.put(PlanetarySophistication.A, Integer.parseInt(values[0]));
                    acquisitionBonuses.put(PlanetarySophistication.B, Integer.parseInt(values[1]));
                    acquisitionBonuses.put(PlanetarySophistication.C, Integer.parseInt(values[2]));
                    acquisitionBonuses.put(PlanetarySophistication.D, Integer.parseInt(values[3]));
                    acquisitionBonuses.put(PlanetarySophistication.F, Integer.parseInt(values[5]));
                } else if (values.length == PlanetarySophistication.values().length) {
                    // >= 0.50.07 compatibility handler
                    for (int i = 0; i < values.length; i++) {
                        acquisitionBonuses.put(PlanetarySophistication.fromIndex(i), Integer.parseInt(values[i]));
                    }
                } else {
                    LOGGER.error("Invalid number of values for planetTechAcquisitionBonus: {}", values.length);
                }
            }
            case "planetIndustryAcquisitionBonus" -> {
                EnumMap<PlanetaryRating, Integer> acquisitionBonuses = campaignOptions.getAllPlanetIndustryAcquisitionBonuses();

                String[] values = nodeContents.split(",");
                if (values.length == 6) {
                    // < 0.50.07 compatibility handler
                    acquisitionBonuses.put(PlanetaryRating.A, Integer.parseInt(values[0]));
                    acquisitionBonuses.put(PlanetaryRating.B, Integer.parseInt(values[1]));
                    acquisitionBonuses.put(PlanetaryRating.C, Integer.parseInt(values[2]));
                    acquisitionBonuses.put(PlanetaryRating.D, Integer.parseInt(values[3]));
                    acquisitionBonuses.put(PlanetaryRating.F, Integer.parseInt(values[5]));
                } else if (values.length == PlanetaryRating.values().length) {
                    // >= 0.50.07 compatibility handler
                    for (int i = 0; i < values.length; i++) {
                        acquisitionBonuses.put(PlanetaryRating.fromIndex(i), Integer.parseInt(values[i]));
                    }
                } else {
                    LOGGER.error("Invalid number of values for planetIndustryAcquisitionBonus: {}", values.length);
                }
            }
            case "planetOutputAcquisitionBonus" -> {
                EnumMap<PlanetaryRating, Integer> acquisitionBonuses = campaignOptions.getAllPlanetOutputAcquisitionBonuses();

                String[] values = nodeContents.split(",");
                if (values.length == 6) {
                    // < 0.50.07 compatibility handler
                    acquisitionBonuses.put(PlanetaryRating.A, Integer.parseInt(values[0]));
                    acquisitionBonuses.put(PlanetaryRating.B, Integer.parseInt(values[1]));
                    acquisitionBonuses.put(PlanetaryRating.C, Integer.parseInt(values[2]));
                    acquisitionBonuses.put(PlanetaryRating.D, Integer.parseInt(values[3]));
                    acquisitionBonuses.put(PlanetaryRating.F, Integer.parseInt(values[5]));
                } else if (values.length == PlanetaryRating.values().length) {
                    // >= 0.50.07 compatibility handler
                    for (int i = 0; i < values.length; i++) {
                        acquisitionBonuses.put(PlanetaryRating.fromIndex(i), Integer.parseInt(values[i]));
                    }
                } else {
                    LOGGER.error("Invalid number of values for planetOutputAcquisitionBonus: {}", values.length);
                }
            }
            case "equipmentContractPercent" -> campaignOptions.setEquipmentContractPercent(Double.parseDouble(
                  nodeContents));
            case "dropShipContractPercent" ->
                  campaignOptions.setDropShipContractPercent(Double.parseDouble(nodeContents));
            case "jumpShipContractPercent" ->
                  campaignOptions.setJumpShipContractPercent(Double.parseDouble(nodeContents));
            case "warShipContractPercent" ->
                  campaignOptions.setWarShipContractPercent(Double.parseDouble(nodeContents));
            case "equipmentContractBase" ->
                  campaignOptions.setEquipmentContractBase(Boolean.parseBoolean(nodeContents));
            case "equipmentContractSaleValue" -> campaignOptions.setEquipmentContractSaleValue(Boolean.parseBoolean(
                  nodeContents));
            case "blcSaleValue" -> campaignOptions.setBLCSaleValue(Boolean.parseBoolean(nodeContents));
            case "overageRepaymentInFinalPayment" ->
                  campaignOptions.setOverageRepaymentInFinalPayment(Boolean.parseBoolean(
                        nodeContents));
            case "acquisitionSupportStaffOnly" -> campaignOptions.setAcquisitionPersonnelCategory(Boolean.parseBoolean(
                  nodeContents) ? SUPPORT : ALL);
            case "acquisitionPersonnelCategory" ->
                  campaignOptions.setAcquisitionPersonnelCategory(ProcurementPersonnelPick.fromString(
                        nodeContents));
            case "limitByYear" -> campaignOptions.setLimitByYear(Boolean.parseBoolean(nodeContents));
            case "disallowExtinctStuff" -> campaignOptions.setDisallowExtinctStuff(Boolean.parseBoolean(nodeContents));
            case "allowClanPurchases" -> campaignOptions.setAllowClanPurchases(Boolean.parseBoolean(nodeContents));
            case "allowISPurchases" -> campaignOptions.setAllowISPurchases(Boolean.parseBoolean(nodeContents));
            case "allowCanonOnly" -> campaignOptions.setAllowCanonOnly(Boolean.parseBoolean(nodeContents));
            case "allowCanonRefitOnly" -> campaignOptions.setAllowCanonRefitOnly(Boolean.parseBoolean(nodeContents));
            case "useAmmoByType" -> campaignOptions.setUseAmmoByType(Boolean.parseBoolean(nodeContents));
            case "variableTechLevel" -> campaignOptions.setVariableTechLevel(Boolean.parseBoolean(nodeContents));
            case "factionIntroDate" -> campaignOptions.setIsUseFactionIntroDate(Boolean.parseBoolean(nodeContents));
            case "techLevel" -> campaignOptions.setTechLevel(Integer.parseInt(nodeContents));
            case "unitRatingMethod", "dragoonsRatingMethod" ->
                  campaignOptions.setUnitRatingMethod(UnitRatingMethod.parseFromString(
                        nodeContents));
            case "manualUnitRatingModifier" ->
                  campaignOptions.setManualUnitRatingModifier(Integer.parseInt(nodeContents));
            case "clampReputationPayMultiplier" -> campaignOptions.setClampReputationPayMultiplier(Boolean.parseBoolean(
                  nodeContents));
            case "reduceReputationPerformanceModifier" ->
                  campaignOptions.setReduceReputationPerformanceModifier(Boolean.parseBoolean(
                        nodeContents));
            case "reputationPerformanceModifierCutOff" ->
                  campaignOptions.setReputationPerformanceModifierCutOff(Boolean.parseBoolean(
                        nodeContents));
            case "usePortraitForType" -> {
                String[] values = nodeContents.split(",");
                for (int i = 0; i < values.length; i++) {
                    campaignOptions.setUsePortraitForRole(i, Boolean.parseBoolean(values[i].trim()));
                }
            }
            case "assignPortraitOnRoleChange" -> campaignOptions.setAssignPortraitOnRoleChange(Boolean.parseBoolean(
                  nodeContents));
            case "allowDuplicatePortraits" -> campaignOptions.setAllowDuplicatePortraits(Boolean.parseBoolean(
                  nodeContents));
            case "destroyByMargin" -> campaignOptions.setDestroyByMargin(Boolean.parseBoolean(nodeContents));
            case "destroyMargin" -> campaignOptions.setDestroyMargin(Integer.parseInt(nodeContents));
            case "destroyPartTarget" -> campaignOptions.setDestroyPartTarget(Integer.parseInt(nodeContents));
            case "useAeroSystemHits" -> campaignOptions.setUseAeroSystemHits(Boolean.parseBoolean(nodeContents));
            case "maxAcquisitions" -> campaignOptions.setMaxAcquisitions(Integer.parseInt(nodeContents));
            case "autoLogisticsHeatSink" ->
                  campaignOptions.setAutoLogisticsHeatSink(MathUtility.parseInt(nodeContents));
            case "autoLogisticsMekHead" -> campaignOptions.setAutoLogisticsMekHead(MathUtility.parseInt(nodeContents));
            case "autoLogisticsMekLocation" -> campaignOptions.setAutoLogisticsMekLocation(MathUtility.parseInt(
                  nodeContents));
            case "autoLogisticsNonRepairableLocation" ->
                  campaignOptions.setAutoLogisticsNonRepairableLocation(MathUtility.parseInt(
                        nodeContents));
            case "autoLogisticsArmor" -> campaignOptions.setAutoLogisticsArmor(MathUtility.parseInt(nodeContents));
            case "autoLogisticsAmmunition" -> campaignOptions.setAutoLogisticsAmmunition(MathUtility.parseInt(
                  nodeContents));
            case "autoLogisticsActuators" ->
                  campaignOptions.setAutoLogisticsActuators(MathUtility.parseInt(nodeContents));
            case "autoLogisticsJumpJets" ->
                  campaignOptions.setAutoLogisticsJumpJets(MathUtility.parseInt(nodeContents));
            case "autoLogisticsEngines" -> campaignOptions.setAutoLogisticsEngines(MathUtility.parseInt(nodeContents));
            case "autoLogisticsWeapons" -> campaignOptions.setAutoLogisticsWeapons(MathUtility.parseInt(nodeContents));
            case "autoLogisticsOther" -> campaignOptions.setAutoLogisticsOther(MathUtility.parseInt(nodeContents));
            case "useTactics" -> campaignOptions.setUseTactics(Boolean.parseBoolean(nodeContents));
            case "useInitiativeBonus" -> campaignOptions.setUseInitiativeBonus(Boolean.parseBoolean(nodeContents));
            case "useToughness" -> campaignOptions.setUseToughness(Boolean.parseBoolean(nodeContents));
            case "useRandomToughness" -> campaignOptions.setUseRandomToughness(Boolean.parseBoolean(nodeContents));
            case "useArtillery" -> campaignOptions.setUseArtillery(Boolean.parseBoolean(nodeContents));
            case "useAbilities" -> campaignOptions.setUseAbilities(Boolean.parseBoolean(nodeContents));
            case "useCommanderAbilitiesOnly" -> campaignOptions.setUseCommanderAbilitiesOnly(Boolean.parseBoolean(
                  nodeContents));
            case "useEdge" -> campaignOptions.setUseEdge(Boolean.parseBoolean(nodeContents));
            case "useSupportEdge" -> campaignOptions.setUseSupportEdge(Boolean.parseBoolean(nodeContents));
            case "useImplants" -> campaignOptions.setUseImplants(Boolean.parseBoolean(nodeContents));
            case "alternativeQualityAveraging" -> campaignOptions.setAlternativeQualityAveraging(Boolean.parseBoolean(
                  nodeContents));
            case "useAgeEffects" -> campaignOptions.setUseAgeEffects(Boolean.parseBoolean(nodeContents));
            case "useTransfers" -> campaignOptions.setUseTransfers(Boolean.parseBoolean(nodeContents));
            case "useExtendedTOEForceName" -> campaignOptions.setUseExtendedTOEForceName(Boolean.parseBoolean(
                  nodeContents));
            case "personnelLogSkillGain" ->
                  campaignOptions.setPersonnelLogSkillGain(Boolean.parseBoolean(nodeContents));
            case "personnelLogAbilityGain" -> campaignOptions.setPersonnelLogAbilityGain(Boolean.parseBoolean(
                  nodeContents));
            case "personnelLogEdgeGain" -> campaignOptions.setPersonnelLogEdgeGain(Boolean.parseBoolean(nodeContents));
            case "displayPersonnelLog" -> campaignOptions.setDisplayPersonnelLog(Boolean.parseBoolean(nodeContents));
            case "displayScenarioLog" -> campaignOptions.setDisplayScenarioLog(Boolean.parseBoolean(nodeContents));
            case "displayKillRecord" -> campaignOptions.setDisplayKillRecord(Boolean.parseBoolean(nodeContents));
            case "displayMedicalRecord" -> campaignOptions.setDisplayMedicalRecord(Boolean.parseBoolean(nodeContents));
            case "displayAssignmentRecord" -> campaignOptions.setDisplayAssignmentRecord(Boolean.parseBoolean(
                  nodeContents));
            case "displayPerformanceRecord" -> campaignOptions.setDisplayPerformanceRecord(Boolean.parseBoolean(
                  nodeContents));
            case "rewardComingOfAgeAbilities" -> campaignOptions.setRewardComingOfAgeAbilities(Boolean.parseBoolean(
                  nodeContents));
            case "rewardComingOfAgeRPSkills" -> campaignOptions.setRewardComingOfAgeRPSkills(Boolean.parseBoolean(
                  nodeContents));
            case "useTimeInService" -> campaignOptions.setUseTimeInService(Boolean.parseBoolean(nodeContents));
            case "timeInServiceDisplayFormat" ->
                  campaignOptions.setTimeInServiceDisplayFormat(TimeInDisplayFormat.valueOf(
                        nodeContents));
            case "useTimeInRank" -> campaignOptions.setUseTimeInRank(Boolean.parseBoolean(nodeContents));
            case "timeInRankDisplayFormat" -> campaignOptions.setTimeInRankDisplayFormat(TimeInDisplayFormat.valueOf(
                  nodeContents));
            case "trackTotalEarnings" -> campaignOptions.setTrackTotalEarnings(Boolean.parseBoolean(nodeContents));
            case "trackTotalXPEarnings" -> campaignOptions.setTrackTotalXPEarnings(Boolean.parseBoolean(nodeContents));
            case "showOriginFaction" -> campaignOptions.setShowOriginFaction(Boolean.parseBoolean(nodeContents));
            case "adminsHaveNegotiation" ->
                  campaignOptions.setAdminsHaveNegotiation(Boolean.parseBoolean(nodeContents));
            case "adminExperienceLevelIncludeNegotiation" ->
                  campaignOptions.setAdminExperienceLevelIncludeNegotiation(Boolean.parseBoolean(
                        nodeContents));
            case "useAdvancedMedical" -> campaignOptions.setUseAdvancedMedical(Boolean.parseBoolean(nodeContents));
            case "healWaitingPeriod" -> campaignOptions.setHealingWaitingPeriod(Integer.parseInt(nodeContents));
            case "naturalHealingWaitingPeriod" -> campaignOptions.setNaturalHealingWaitingPeriod(Integer.parseInt(
                  nodeContents));
            case "minimumHitsForVehicles" -> campaignOptions.setMinimumHitsForVehicles(Integer.parseInt(nodeContents));
            case "useRandomHitsForVehicles" -> campaignOptions.setUseRandomHitsForVehicles(Boolean.parseBoolean(
                  nodeContents));
            case "tougherHealing" -> campaignOptions.setTougherHealing(Boolean.parseBoolean(nodeContents));
            case "maximumPatients" -> campaignOptions.setMaximumPatients(Integer.parseInt(nodeContents));
            case "doctorsUseAdministration" -> campaignOptions.setDoctorsUseAdministration(Boolean.parseBoolean(
                  nodeContents));
            case "prisonerCaptureStyle" -> campaignOptions.setPrisonerCaptureStyle(PrisonerCaptureStyle.fromString(
                  nodeContents));
            case "useRandomDependentAddition" -> campaignOptions.setUseRandomDependentAddition(Boolean.parseBoolean(
                  nodeContents));
            case "useRandomDependentRemoval" -> campaignOptions.setUseRandomDependentRemoval(Boolean.parseBoolean(
                  nodeContents));
            case "dependentProfessionDieSize" -> campaignOptions.setDependentProfessionDieSize(MathUtility.parseInt(
                  nodeContents, 4));
            case "civilianProfessionDieSize" -> campaignOptions.setCivilianProfessionDieSize(MathUtility.parseInt(
                  nodeContents, 2));
            case "usePersonnelRemoval" -> campaignOptions.setUsePersonnelRemoval(Boolean.parseBoolean(nodeContents));
            case "useRemovalExemptCemetery" -> campaignOptions.setUseRemovalExemptCemetery(Boolean.parseBoolean(
                  nodeContents));
            case "useRemovalExemptRetirees" -> campaignOptions.setUseRemovalExemptRetirees(Boolean.parseBoolean(
                  nodeContents));
            case "disableSecondaryRoleSalary" -> campaignOptions.setDisableSecondaryRoleSalary(Boolean.parseBoolean(
                  nodeContents));
            case "salaryAntiMekMultiplier" ->
                  campaignOptions.setSalaryAntiMekMultiplier(Double.parseDouble(nodeContents));
            case "salarySpecialistInfantryMultiplier" ->
                  campaignOptions.setSalarySpecialistInfantryMultiplier(Double.parseDouble(
                        nodeContents));
            case "salaryXPMultipliers" -> {
                if (!childNode.hasChildNodes()) {
                    return;
                }
                final NodeList nl2 = childNode.getChildNodes();
                for (int j = 0; j < nl2.getLength(); j++) {
                    final Node wn3 = nl2.item(j);
                    if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    campaignOptions.getSalaryXPMultipliers()
                          .put(SkillLevel.valueOf(wn3.getNodeName().trim()),
                                Double.parseDouble(wn3.getTextContent().trim()));
                }
            }
            case "salaryTypeBase" -> {
                Money[] defaultSalaries = campaignOptions.getRoleBaseSalaries();
                Money[] newSalaries = Utilities.readMoneyArray(childNode);

                Money[] mergedSalaries = new Money[PersonnelRole.values().length];
                for (int i = 0; i < mergedSalaries.length; i++) {
                    try {
                        mergedSalaries[i] = (newSalaries[i] != null) ? newSalaries[i] : defaultSalaries[i];
                    } catch (Exception e) {
                        // This will happen if we ever add a new profession, as it will exceed the entries in
                        // the child node
                        if (defaultSalaries != null) {
                            mergedSalaries[i] = defaultSalaries[i];
                        }
                    }
                }

                campaignOptions.setRoleBaseSalaries(mergedSalaries);
            }
            case "awardBonusStyle" -> campaignOptions.setAwardBonusStyle(AwardBonus.valueOf(nodeContents));
            case "enableAutoAwards" -> campaignOptions.setEnableAutoAwards(Boolean.parseBoolean(nodeContents));
            case "issuePosthumousAwards" ->
                  campaignOptions.setIssuePosthumousAwards(Boolean.parseBoolean(nodeContents));
            case "issueBestAwardOnly" -> campaignOptions.setIssueBestAwardOnly(Boolean.parseBoolean(nodeContents));
            case "ignoreStandardSet" -> campaignOptions.setIgnoreStandardSet(Boolean.parseBoolean(nodeContents));
            case "awardTierSize" -> campaignOptions.setAwardTierSize(Integer.parseInt(nodeContents));
            case "enableContractAwards" -> campaignOptions.setEnableContractAwards(Boolean.parseBoolean(nodeContents));
            case "enableFactionHunterAwards" -> campaignOptions.setEnableFactionHunterAwards(Boolean.parseBoolean(
                  nodeContents));
            case "enableInjuryAwards" -> campaignOptions.setEnableInjuryAwards(Boolean.parseBoolean(nodeContents));
            case "enableIndividualKillAwards" -> campaignOptions.setEnableIndividualKillAwards(Boolean.parseBoolean(
                  nodeContents));
            case "enableFormationKillAwards" -> campaignOptions.setEnableFormationKillAwards(Boolean.parseBoolean(
                  nodeContents));
            case "enableRankAwards" -> campaignOptions.setEnableRankAwards(Boolean.parseBoolean(nodeContents));
            case "enableScenarioAwards" -> campaignOptions.setEnableScenarioAwards(Boolean.parseBoolean(nodeContents));
            case "enableSkillAwards" -> campaignOptions.setEnableSkillAwards(Boolean.parseBoolean(nodeContents));
            case "enableTheatreOfWarAwards" -> campaignOptions.setEnableTheatreOfWarAwards(Boolean.parseBoolean(
                  nodeContents));
            case "enableTimeAwards" -> campaignOptions.setEnableTimeAwards(Boolean.parseBoolean(nodeContents));
            case "enableTrainingAwards" -> campaignOptions.setEnableTrainingAwards(Boolean.parseBoolean(nodeContents));
            case "enableMiscAwards" -> campaignOptions.setEnableMiscAwards(Boolean.parseBoolean(nodeContents));
            case "awardSetFilterList" -> campaignOptions.setAwardSetFilterList(nodeContents);
            case "useDylansRandomXP" -> campaignOptions.setUseDylansRandomXP(Boolean.parseBoolean(nodeContents));
            case "nonBinaryDiceSize" -> campaignOptions.setNonBinaryDiceSize(Integer.parseInt(nodeContents));
            case "randomOriginOptions" -> {
                if (!childNode.hasChildNodes()) {
                    return;
                }
                final RandomOriginOptions randomOriginOptions = RandomOriginOptions.parseFromXML(childNode.getChildNodes(),
                      true);
                if (randomOriginOptions == null) {
                    return;
                }
                campaignOptions.setRandomOriginOptions(randomOriginOptions);
            }
            case "useRandomPersonalities" ->
                  campaignOptions.setUseRandomPersonalities(Boolean.parseBoolean(nodeContents));
            case "useRandomPersonalityReputation" ->
                  campaignOptions.setUseRandomPersonalityReputation(Boolean.parseBoolean(
                        nodeContents));
            case "useReasoningXpMultiplier" -> campaignOptions.setUseReasoningXpMultiplier(Boolean.parseBoolean(
                  nodeContents));
            case "useSimulatedRelationships" -> campaignOptions.setUseSimulatedRelationships(Boolean.parseBoolean(
                  nodeContents));
            case "familyDisplayLevel" ->
                  campaignOptions.setFamilyDisplayLevel(FamilialRelationshipDisplayLevel.parseFromString(
                        nodeContents));
            case "announceBirthdays" -> campaignOptions.setAnnounceBirthdays(Boolean.parseBoolean(nodeContents));
            case "announceRecruitmentAnniversaries" ->
                  campaignOptions.setAnnounceRecruitmentAnniversaries(Boolean.parseBoolean(
                        nodeContents));
            case "announceOfficersOnly" -> campaignOptions.setAnnounceOfficersOnly(Boolean.parseBoolean(nodeContents));
            case "announceChildBirthdays" ->
                  campaignOptions.setAnnounceChildBirthdays(Boolean.parseBoolean(nodeContents));
            case "showLifeEventDialogBirths" -> campaignOptions.setShowLifeEventDialogBirths(Boolean.parseBoolean(
                  nodeContents));
            case "showLifeEventDialogComingOfAge" ->
                  campaignOptions.setShowLifeEventDialogComingOfAge(Boolean.parseBoolean(
                        nodeContents));
            case "showLifeEventDialogCelebrations" ->
                  campaignOptions.setShowLifeEventDialogCelebrations(Boolean.parseBoolean(
                        nodeContents));
            case "useManualMarriages" -> campaignOptions.setUseManualMarriages(Boolean.parseBoolean(nodeContents));
            case "useClanPersonnelMarriages" -> campaignOptions.setUseClanPersonnelMarriages(Boolean.parseBoolean(
                  nodeContents));
            case "usePrisonerMarriages" -> campaignOptions.setUsePrisonerMarriages(Boolean.parseBoolean(nodeContents));
            case "checkMutualAncestorsDepth" -> campaignOptions.setCheckMutualAncestorsDepth(Integer.parseInt(
                  nodeContents));
            case "noInterestInMarriageDiceSize" -> campaignOptions.setNoInterestInMarriageDiceSize(Integer.parseInt(
                  nodeContents));
            case "logMarriageNameChanges" ->
                  campaignOptions.setLogMarriageNameChanges(Boolean.parseBoolean(nodeContents));
            case "marriageSurnameWeights" -> {
                if (!childNode.hasChildNodes()) {
                    return;
                }
                final NodeList nl2 = childNode.getChildNodes();
                for (int j = 0; j < nl2.getLength(); j++) {
                    final Node wn3 = nl2.item(j);
                    if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    campaignOptions.getMarriageSurnameWeights()
                          .put(MergingSurnameStyle.parseFromString(wn3.getNodeName().trim()),
                                Integer.parseInt(wn3.getTextContent().trim()));
                }
            }
            case "randomMarriageMethod" -> campaignOptions.setRandomMarriageMethod(RandomMarriageMethod.fromString(
                  nodeContents));
            case "useRandomClanPersonnelMarriages" ->
                  campaignOptions.setUseRandomClanPersonnelMarriages(Boolean.parseBoolean(
                        nodeContents));
            case "useRandomPrisonerMarriages" -> campaignOptions.setUseRandomPrisonerMarriages(Boolean.parseBoolean(
                  nodeContents));
            case "randomMarriageAgeRange" -> campaignOptions.setRandomMarriageAgeRange(Integer.parseInt(nodeContents));
            case "randomMarriageDiceSize" -> campaignOptions.setRandomMarriageDiceSize(Integer.parseInt(nodeContents));
            case "randomSameSexMarriageDiceSize" -> campaignOptions.setRandomSameSexMarriageDiceSize(Integer.parseInt(
                  nodeContents));
            case "randomNewDependentMarriage" -> campaignOptions.setRandomNewDependentMarriage(Integer.parseInt(
                  nodeContents));
            case "useManualDivorce" -> campaignOptions.setUseManualDivorce(Boolean.parseBoolean(nodeContents));
            case "useClanPersonnelDivorce" -> campaignOptions.setUseClanPersonnelDivorce(Boolean.parseBoolean(
                  nodeContents));
            case "usePrisonerDivorce" -> campaignOptions.setUsePrisonerDivorce(Boolean.parseBoolean(nodeContents));
            case "divorceSurnameWeights" -> {
                if (!childNode.hasChildNodes()) {
                    return;
                }
                final NodeList nl2 = childNode.getChildNodes();
                for (int j = 0; j < nl2.getLength(); j++) {
                    final Node wn3 = nl2.item(j);
                    if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    campaignOptions.getDivorceSurnameWeights()
                          .put(SplittingSurnameStyle.valueOf(wn3.getNodeName().trim()),
                                Integer.parseInt(wn3.getTextContent().trim()));
                }
            }
            case "randomDivorceMethod" -> campaignOptions.setRandomDivorceMethod(RandomDivorceMethod.fromString(
                  nodeContents));
            case "useRandomOppositeSexDivorce" -> campaignOptions.setUseRandomOppositeSexDivorce(Boolean.parseBoolean(
                  nodeContents));
            case "useRandomSameSexDivorce" -> campaignOptions.setUseRandomSameSexDivorce(Boolean.parseBoolean(
                  nodeContents));
            case "useRandomClanPersonnelDivorce" ->
                  campaignOptions.setUseRandomClanPersonnelDivorce(Boolean.parseBoolean(
                        nodeContents));
            case "useRandomPrisonerDivorce" -> campaignOptions.setUseRandomPrisonerDivorce(Boolean.parseBoolean(
                  nodeContents));
            case "randomDivorceDiceSize" -> campaignOptions.setRandomDivorceDiceSize(Integer.parseInt(nodeContents));
            case "useManualProcreation" -> campaignOptions.setUseManualProcreation(Boolean.parseBoolean(nodeContents));
            case "useClanPersonnelProcreation" -> campaignOptions.setUseClanPersonnelProcreation(Boolean.parseBoolean(
                  nodeContents));
            case "usePrisonerProcreation" ->
                  campaignOptions.setUsePrisonerProcreation(Boolean.parseBoolean(nodeContents));
            case "multiplePregnancyOccurrences" -> campaignOptions.setMultiplePregnancyOccurrences(Integer.parseInt(
                  nodeContents));
            case "babySurnameStyle" ->
                  campaignOptions.setBabySurnameStyle(BabySurnameStyle.parseFromString(nodeContents));
            case "assignNonPrisonerBabiesFounderTag" ->
                  campaignOptions.setAssignNonPrisonerBabiesFounderTag(Boolean.parseBoolean(
                        nodeContents));
            case "assignChildrenOfFoundersFounderTag" ->
                  campaignOptions.setAssignChildrenOfFoundersFounderTag(Boolean.parseBoolean(
                        nodeContents));
            case "useMaternityLeave" -> campaignOptions.setUseMaternityLeave(Boolean.parseBoolean(nodeContents));
            case "determineFatherAtBirth" ->
                  campaignOptions.setDetermineFatherAtBirth(Boolean.parseBoolean(nodeContents));
            case "displayTrueDueDate" -> campaignOptions.setDisplayTrueDueDate(Boolean.parseBoolean(nodeContents));
            case "noInterestInChildrenDiceSize" -> campaignOptions.setNoInterestInChildrenDiceSize(Integer.parseInt(
                  nodeContents));
            case "logProcreation" -> campaignOptions.setLogProcreation(Boolean.parseBoolean(nodeContents));
            case "randomProcreationMethod" ->
                  campaignOptions.setRandomProcreationMethod(RandomProcreationMethod.fromString(
                        nodeContents));
            case "useRelationshiplessRandomProcreation" ->
                  campaignOptions.setUseRelationshiplessRandomProcreation(Boolean.parseBoolean(
                        nodeContents));
            case "useRandomClanPersonnelProcreation" ->
                  campaignOptions.setUseRandomClanPersonnelProcreation(Boolean.parseBoolean(
                        nodeContents));
            case "useRandomPrisonerProcreation" -> campaignOptions.setUseRandomPrisonerProcreation(Boolean.parseBoolean(
                  nodeContents));
            case "randomProcreationRelationshipDiceSize" ->
                  campaignOptions.setRandomProcreationRelationshipDiceSize(Integer.parseInt(
                        nodeContents));
            case "randomProcreationRelationshiplessDiceSize" ->
                  campaignOptions.setRandomProcreationRelationshiplessDiceSize(Integer.parseInt(
                        nodeContents));
            case "useEducationModule" -> campaignOptions.setUseEducationModule(Boolean.parseBoolean(nodeContents));
            case "curriculumXpRate" -> campaignOptions.setCurriculumXpRate(Integer.parseInt(nodeContents));
            case "maximumJumpCount" -> campaignOptions.setMaximumJumpCount(Integer.parseInt(nodeContents));
            case "useReeducationCamps" -> campaignOptions.setUseReeducationCamps(Boolean.parseBoolean(nodeContents));
            case "enableLocalAcademies" -> campaignOptions.setEnableLocalAcademies(Boolean.parseBoolean(nodeContents));
            case "enablePrestigiousAcademies" -> campaignOptions.setEnablePrestigiousAcademies(Boolean.parseBoolean(
                  nodeContents));
            case "enableUnitEducation" -> campaignOptions.setEnableUnitEducation(Boolean.parseBoolean(nodeContents));
            case "enableOverrideRequirements" -> campaignOptions.setEnableOverrideRequirements(Boolean.parseBoolean(
                  nodeContents));
            case "enableShowIneligibleAcademies" ->
                  campaignOptions.setEnableShowIneligibleAcademies(Boolean.parseBoolean(
                        nodeContents));
            case "entranceExamBaseTargetNumber" -> campaignOptions.setEntranceExamBaseTargetNumber(Integer.parseInt(
                  nodeContents));
            case "facultyXpRate" -> campaignOptions.setFacultyXpRate(Double.parseDouble(nodeContents));
            case "enableBonuses" -> campaignOptions.setEnableBonuses(Boolean.parseBoolean(nodeContents));
            case "adultDropoutChance" -> campaignOptions.setAdultDropoutChance(Integer.parseInt(nodeContents));
            case "childrenDropoutChance" -> campaignOptions.setChildrenDropoutChance(Integer.parseInt(nodeContents));
            case "allAges" -> campaignOptions.setAllAges(Boolean.parseBoolean(nodeContents));
            case "militaryAcademyAccidents" ->
                  campaignOptions.setMilitaryAcademyAccidents(Integer.parseInt(nodeContents));
            case "enabledRandomDeathAgeGroups" -> {
                if (!childNode.hasChildNodes()) {
                    return;
                }
                final NodeList nl2 = childNode.getChildNodes();
                for (int i = 0; i < nl2.getLength(); i++) {
                    final Node wn3 = nl2.item(i);
                    try {
                        campaignOptions.getEnabledRandomDeathAgeGroups()
                              .put(AgeGroup.valueOf(wn3.getNodeName()),
                                    Boolean.parseBoolean(wn3.getTextContent().trim()));
                    } catch (Exception ignored) {

                    }
                }
            }
            case "useRandomDeathSuicideCause" -> campaignOptions.setUseRandomDeathSuicideCause(Boolean.parseBoolean(
                  nodeContents));
            case "randomDeathMultiplier" -> campaignOptions.setRandomDeathMultiplier(Double.parseDouble(nodeContents));
            case "useRandomRetirement" -> campaignOptions.setUseRandomRetirement(Boolean.parseBoolean(nodeContents));
            case "turnoverBaseTn" -> campaignOptions.setTurnoverFixedTargetNumber(Integer.parseInt(nodeContents));
            case "turnoverFrequency" -> campaignOptions.setTurnoverFrequency(TurnoverFrequency.valueOf(nodeContents));
            case "trackOriginalUnit" -> campaignOptions.setTrackOriginalUnit(Boolean.parseBoolean(nodeContents));
            case "aeroRecruitsHaveUnits" ->
                  campaignOptions.setAeroRecruitsHaveUnits(Boolean.parseBoolean(nodeContents));
            case "useContractCompletionRandomRetirement" ->
                  campaignOptions.setUseContractCompletionRandomRetirement(Boolean.parseBoolean(
                        nodeContents));
            case "useRandomFounderTurnover" -> campaignOptions.setUseRandomFounderTurnover(Boolean.parseBoolean(
                  nodeContents));
            case "useFounderRetirement" -> campaignOptions.setUseFounderRetirement(Boolean.parseBoolean(nodeContents));
            case "useSubContractSoldiers" ->
                  campaignOptions.setUseSubContractSoldiers(Boolean.parseBoolean(nodeContents));
            case "serviceContractDuration" ->
                  campaignOptions.setServiceContractDuration(Integer.parseInt(nodeContents));
            case "serviceContractModifier" ->
                  campaignOptions.setServiceContractModifier(Integer.parseInt(nodeContents));
            case "payBonusDefault" -> campaignOptions.setPayBonusDefault(Boolean.parseBoolean(nodeContents));
            case "payBonusDefaultThreshold" ->
                  campaignOptions.setPayBonusDefaultThreshold(Integer.parseInt(nodeContents));
            case "useCustomRetirementModifiers" -> campaignOptions.setUseCustomRetirementModifiers(Boolean.parseBoolean(
                  nodeContents));
            case "useFatigueModifiers" -> campaignOptions.setUseFatigueModifiers(Boolean.parseBoolean(nodeContents));
            case "useSkillModifiers" -> campaignOptions.setUseSkillModifiers(Boolean.parseBoolean(nodeContents));
            case "useAgeModifiers" -> campaignOptions.setUseAgeModifiers(Boolean.parseBoolean(nodeContents));
            case "useUnitRatingModifiers" ->
                  campaignOptions.setUseUnitRatingModifiers(Boolean.parseBoolean(nodeContents));
            case "useFactionModifiers" -> campaignOptions.setUseFactionModifiers(Boolean.parseBoolean(nodeContents));
            case "useMissionStatusModifiers" -> campaignOptions.setUseMissionStatusModifiers(Boolean.parseBoolean(
                  nodeContents));
            case "useHostileTerritoryModifiers" -> campaignOptions.setUseHostileTerritoryModifiers(Boolean.parseBoolean(
                  nodeContents));
            case "useFamilyModifiers" -> campaignOptions.setUseFamilyModifiers(Boolean.parseBoolean(nodeContents));
            case "useLoyaltyModifiers" -> campaignOptions.setUseLoyaltyModifiers(Boolean.parseBoolean(nodeContents));
            case "useHideLoyalty" -> campaignOptions.setUseHideLoyalty(Boolean.parseBoolean(nodeContents));
            case "payoutRateOfficer" -> campaignOptions.setPayoutRateOfficer(Integer.parseInt(nodeContents));
            case "payoutRateEnlisted" -> campaignOptions.setPayoutRateEnlisted(Integer.parseInt(nodeContents));
            case "payoutRetirementMultiplier" -> campaignOptions.setPayoutRetirementMultiplier(Integer.parseInt(
                  nodeContents));
            case "usePayoutServiceBonus" ->
                  campaignOptions.setUsePayoutServiceBonus(Boolean.parseBoolean(nodeContents));
            case "payoutServiceBonusRate" -> campaignOptions.setPayoutServiceBonusRate(Integer.parseInt(nodeContents));
            // 'useAdministrativeStrain' is <50.07
            case "UseHRStrain", "useAdministrativeStrain" ->
                  campaignOptions.setUseHRStrain(Boolean.parseBoolean(nodeContents));
            // 'administrativeStrain' is <50.07
            case "hrStrain", "administrativeStrain" ->
                  campaignOptions.setHRCapacity(MathUtility.parseInt(nodeContents));
            case "useManagementSkill" -> campaignOptions.setUseManagementSkill(Boolean.parseBoolean(nodeContents));
            case "useCommanderLeadershipOnly" -> campaignOptions.setUseCommanderLeadershipOnly(Boolean.parseBoolean(
                  nodeContents));
            case "managementSkillPenalty" -> campaignOptions.setManagementSkillPenalty(Integer.parseInt(nodeContents));
            case "useFatigue" -> campaignOptions.setUseFatigue(Boolean.parseBoolean(nodeContents));
            case "fatigueRate" -> campaignOptions.setFatigueRate(Integer.parseInt(nodeContents));
            case "useInjuryFatigue" -> campaignOptions.setUseInjuryFatigue(Boolean.parseBoolean(nodeContents));
            case "fieldKitchenCapacity" -> campaignOptions.setFieldKitchenCapacity(Integer.parseInt(nodeContents));
            case "fieldKitchenIgnoreNonCombatants" ->
                  campaignOptions.setFieldKitchenIgnoreNonCombatants(Boolean.parseBoolean(
                        nodeContents));
            case "fatigueLeaveThreshold" -> campaignOptions.setFatigueLeaveThreshold(Integer.parseInt(nodeContents));
            case "payForParts" -> campaignOptions.setPayForParts(Boolean.parseBoolean(nodeContents));
            case "payForRepairs" -> campaignOptions.setPayForRepairs(Boolean.parseBoolean(nodeContents));
            case "payForUnits" -> campaignOptions.setPayForUnits(Boolean.parseBoolean(nodeContents));
            case "payForSalaries" -> campaignOptions.setPayForSalaries(Boolean.parseBoolean(nodeContents));
            case "payForOverhead" -> campaignOptions.setPayForOverhead(Boolean.parseBoolean(nodeContents));
            case "payForMaintain" -> campaignOptions.setPayForMaintain(Boolean.parseBoolean(nodeContents));
            case "payForTransport" -> campaignOptions.setPayForTransport(Boolean.parseBoolean(nodeContents));
            case "sellUnits" -> campaignOptions.setSellUnits(Boolean.parseBoolean(nodeContents));
            case "sellParts" -> campaignOptions.setSellParts(Boolean.parseBoolean(nodeContents));
            case "payForRecruitment" -> campaignOptions.setPayForRecruitment(Boolean.parseBoolean(nodeContents));
            case "payForFood" -> campaignOptions.setPayForFood(Boolean.parseBoolean(nodeContents));
            case "payForHousing" -> campaignOptions.setPayForHousing(Boolean.parseBoolean(nodeContents));
            case "useLoanLimits" -> campaignOptions.setLoanLimits(Boolean.parseBoolean(nodeContents));
            case "usePercentageMaint" -> campaignOptions.setUsePercentageMaint(Boolean.parseBoolean(nodeContents));
            case "infantryDontCount" -> campaignOptions.setUseInfantryDontCount(Boolean.parseBoolean(nodeContents));
            case "usePeacetimeCost" -> campaignOptions.setUsePeacetimeCost(Boolean.parseBoolean(nodeContents));
            case "useExtendedPartsModifier" -> campaignOptions.setUseExtendedPartsModifier(Boolean.parseBoolean(
                  nodeContents));
            case "showPeacetimeCost" -> campaignOptions.setShowPeacetimeCost(Boolean.parseBoolean(nodeContents));
            case "newFinancialYearFinancesToCSVExport" ->
                  campaignOptions.setNewFinancialYearFinancesToCSVExport(Boolean.parseBoolean(
                        nodeContents));
            case "financialYearDuration" ->
                  campaignOptions.setFinancialYearDuration(FinancialYearDuration.parseFromString(
                        nodeContents));
            case "simulateGrayMonday" -> campaignOptions.setSimulateGrayMonday(Boolean.parseBoolean(nodeContents));
            case "allowMonthlyReinvestment" -> campaignOptions.setAllowMonthlyReinvestment(Boolean.parseBoolean(
                  nodeContents));
            case "allowMonthlyConnections" -> campaignOptions.setAllowMonthlyConnections(Boolean.parseBoolean(
                  nodeContents));
            case "commonPartPriceMultiplier" -> campaignOptions.setCommonPartPriceMultiplier(Double.parseDouble(
                  nodeContents));
            case "innerSphereUnitPriceMultiplier" ->
                  campaignOptions.setInnerSphereUnitPriceMultiplier(Double.parseDouble(
                        nodeContents));
            case "innerSpherePartPriceMultiplier" ->
                  campaignOptions.setInnerSpherePartPriceMultiplier(Double.parseDouble(
                        nodeContents));
            case "clanUnitPriceMultiplier" ->
                  campaignOptions.setClanUnitPriceMultiplier(Double.parseDouble(nodeContents));
            case "clanPartPriceMultiplier" ->
                  campaignOptions.setClanPartPriceMultiplier(Double.parseDouble(nodeContents));
            case "mixedTechUnitPriceMultiplier" -> campaignOptions.setMixedTechUnitPriceMultiplier(Double.parseDouble(
                  nodeContents));
            case "usedPartPriceMultipliers" -> {
                final String[] values = nodeContents.split(",");
                for (int i = 0; i < values.length; i++) {
                    try {
                        campaignOptions.getUsedPartPriceMultipliers()[i] = Double.parseDouble(values[i]);
                    } catch (Exception ignored) {

                    }
                }
            }
            case "damagedPartsValueMultiplier" -> campaignOptions.setDamagedPartsValueMultiplier(Double.parseDouble(
                  nodeContents));
            case "unrepairablePartsValueMultiplier" ->
                  campaignOptions.setUnrepairablePartsValueMultiplier(Double.parseDouble(
                        nodeContents));
            case "cancelledOrderRefundMultiplier" ->
                  campaignOptions.setCancelledOrderRefundMultiplier(Double.parseDouble(
                        nodeContents));
            case "useTaxes" -> campaignOptions.setUseTaxes(Boolean.parseBoolean(nodeContents));
            case "taxesPercentage" -> campaignOptions.setTaxesPercentage(Integer.parseInt(nodeContents));
            case "useShareSystem" -> campaignOptions.setUseShareSystem(Boolean.parseBoolean(nodeContents));
            case "sharesForAll" -> campaignOptions.setSharesForAll(Boolean.parseBoolean(nodeContents));
            case "personnelMarketStyle" -> campaignOptions.setPersonnelMarketStyle(PersonnelMarketStyle.fromString(
                  nodeContents));
            case "personnelMarketName" -> {
                String marketName = nodeContents;
                // Backwards compatibility with saves from before these rules moved to Camops
                if (marketName.equals("Strat Ops")) {
                    marketName = "Campaign Ops";
                }
                campaignOptions.setPersonnelMarketName(marketName);
            }
            case "personnelMarketReportRefresh" -> campaignOptions.setPersonnelMarketReportRefresh(Boolean.parseBoolean(
                  nodeContents));
            case "personnelMarketRandomRemovalTargets" -> {
                if (!childNode.hasChildNodes()) {
                    return;
                }
                final NodeList nl2 = childNode.getChildNodes();
                for (int j = 0; j < nl2.getLength(); j++) {
                    final Node wn3 = nl2.item(j);
                    if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    campaignOptions.getPersonnelMarketRandomRemovalTargets()
                          .put(SkillLevel.valueOf(wn3.getNodeName().trim()),
                                Integer.parseInt(wn3.getTextContent().trim()));
                }
            }
            case "personnelMarketDylansWeight" -> campaignOptions.setPersonnelMarketDylansWeight(Double.parseDouble(
                  nodeContents));
            case "usePersonnelHireHiringHallOnly" ->
                  campaignOptions.setUsePersonnelHireHiringHallOnly(Boolean.parseBoolean(
                        nodeContents));
            case "unitMarketMethod" -> campaignOptions.setUnitMarketMethod(UnitMarketMethod.valueOf(nodeContents));
            case "unitMarketRegionalMekVariations" ->
                  campaignOptions.setUnitMarketRegionalMekVariations(Boolean.parseBoolean(
                        nodeContents));
            case "unitMarketSpecialUnitChance" -> campaignOptions.setUnitMarketSpecialUnitChance(Integer.parseInt(
                  nodeContents));
            case "unitMarketRarityModifier" ->
                  campaignOptions.setUnitMarketRarityModifier(Integer.parseInt(nodeContents));
            case "instantUnitMarketDelivery" -> campaignOptions.setInstantUnitMarketDelivery(Boolean.parseBoolean(
                  nodeContents));
            case "mothballUnitMarketDeliveries" -> campaignOptions.setMothballUnitMarketDeliveries(Boolean.parseBoolean(
                  nodeContents));
            case "unitMarketReportRefresh" -> campaignOptions.setUnitMarketReportRefresh(Boolean.parseBoolean(
                  nodeContents));
            case "contractMarketMethod" -> campaignOptions.setContractMarketMethod(ContractMarketMethod.valueOf(
                  nodeContents));
            case "contractSearchRadius" -> campaignOptions.setContractSearchRadius(Integer.parseInt(nodeContents));
            case "variableContractLength" ->
                  campaignOptions.setVariableContractLength(Boolean.parseBoolean(nodeContents));
            case "useDynamicDifficulty" -> campaignOptions.setUseDynamicDifficulty(Boolean.parseBoolean(nodeContents));
            case "contractMarketReportRefresh" -> campaignOptions.setContractMarketReportRefresh(Boolean.parseBoolean(
                  nodeContents));
            case "contractMaxSalvagePercentage" -> campaignOptions.setContractMaxSalvagePercentage(Integer.parseInt(
                  nodeContents));
            case "dropShipBonusPercentage" ->
                  campaignOptions.setDropShipBonusPercentage(Integer.parseInt(nodeContents));
            case "useStaticRATs" -> campaignOptions.setUseStaticRATs(Boolean.parseBoolean(nodeContents));
            case "rats" -> campaignOptions.setRATs(MHQXMLUtility.unEscape(nodeContents).split(","));
            case "ignoreRATEra" -> campaignOptions.setIgnoreRATEra(Boolean.parseBoolean(nodeContents));
            case "skillLevel" -> campaignOptions.setSkillLevel(SkillLevel.parseFromString(nodeContents));
            case "autoResolveMethod" -> campaignOptions.setAutoResolveMethod(AutoResolveMethod.valueOf(nodeContents));
            case "autoResolveVictoryChanceEnabled" ->
                  campaignOptions.setAutoResolveVictoryChanceEnabled(Boolean.parseBoolean(
                        nodeContents));
            case "autoResolveNumberOfScenarios" -> campaignOptions.setAutoResolveNumberOfScenarios(Integer.parseInt(
                  nodeContents));
            case "autoResolveUseExperimentalPacarGui" ->
                  campaignOptions.setAutoResolveExperimentalPacarGuiEnabled(Boolean.parseBoolean(
                        nodeContents));
            case "strategicViewTheme" -> campaignOptions.setStrategicViewTheme(nodeContents);
            case "phenotypeProbabilities" -> {
                String[] values = nodeContents.split(",");
                for (int i = 0; i < values.length; i++) {
                    campaignOptions.setPhenotypeProbability(i, Integer.parseInt(values[i]));
                }
            }
            case "useAtB" -> campaignOptions.setUseAtB(Boolean.parseBoolean(nodeContents));
            case "useStratCon" -> campaignOptions.setUseStratCon(Boolean.parseBoolean(nodeContents));
            case "useAero" -> campaignOptions.setUseAero(Boolean.parseBoolean(nodeContents));
            case "useVehicles" -> campaignOptions.setUseVehicles(Boolean.parseBoolean(nodeContents));
            case "clanVehicles" -> campaignOptions.setClanVehicles(Boolean.parseBoolean(nodeContents));
            case "useGenericBattleValue" ->
                  campaignOptions.setUseGenericBattleValue(Boolean.parseBoolean(nodeContents));
            case "useVerboseBidding" -> campaignOptions.setUseVerboseBidding(Boolean.parseBoolean(nodeContents));
            case "doubleVehicles" -> campaignOptions.setDoubleVehicles(Boolean.parseBoolean(nodeContents));
            case "adjustPlayerVehicles" -> campaignOptions.setAdjustPlayerVehicles(Boolean.parseBoolean(nodeContents));
            case "opForLanceTypeMeks" -> campaignOptions.setOpForLanceTypeMeks(Integer.parseInt(nodeContents));
            case "opForLanceTypeMixed" -> campaignOptions.setOpForLanceTypeMixed(Integer.parseInt(nodeContents));
            case "opForLanceTypeVehicles" -> campaignOptions.setOpForLanceTypeVehicles(Integer.parseInt(nodeContents));
            case "opForUsesVTOLs" -> campaignOptions.setOpForUsesVTOLs(Boolean.parseBoolean(nodeContents));
            case "useDropShips" -> campaignOptions.setUseDropShips(Boolean.parseBoolean(nodeContents));
            case "mercSizeLimited" -> campaignOptions.setMercSizeLimited(Boolean.parseBoolean(nodeContents));
            case "regionalMekVariations" ->
                  campaignOptions.setRegionalMekVariations(Boolean.parseBoolean(nodeContents));
            case "attachedPlayerCamouflage" -> campaignOptions.setAttachedPlayerCamouflage(Boolean.parseBoolean(
                  nodeContents));
            case "playerControlsAttachedUnits" -> campaignOptions.setPlayerControlsAttachedUnits(Boolean.parseBoolean(
                  nodeContents));
            case "atbBattleChance" -> {
                String[] values = nodeContents.split(",");
                for (int i = 0; i < values.length; i++) {
                    try {
                        campaignOptions.setAtBBattleChance(i, Integer.parseInt(values[i]));
                    } catch (Exception ignored) {
                        // Badly coded, but this is to migrate devs and their games as the swap was done before a
                        // release and is thus better to handle this way than through a more code complex method
                        campaignOptions.setAtBBattleChance(i, (int) Math.round(Double.parseDouble(values[i])));
                    }
                }
            }
            case "generateChases" -> campaignOptions.setGenerateChases(Boolean.parseBoolean(nodeContents));
            case "useWeatherConditions" -> campaignOptions.setUseWeatherConditions(Boolean.parseBoolean(nodeContents));
            case "useLightConditions" -> campaignOptions.setUseLightConditions(Boolean.parseBoolean(nodeContents));
            case "usePlanetaryConditions" ->
                  campaignOptions.setUsePlanetaryConditions(Boolean.parseBoolean(nodeContents));
            case "restrictPartsByMission" ->
                  campaignOptions.setRestrictPartsByMission(Boolean.parseBoolean(nodeContents));
            case "allowOpForLocalUnits" -> campaignOptions.setAllowOpForLocalUnits(Boolean.parseBoolean(nodeContents));
            case "allowOpForAeros" -> campaignOptions.setAllowOpForAeros(Boolean.parseBoolean(nodeContents));
            case "opForAeroChance" -> campaignOptions.setOpForAeroChance(Integer.parseInt(nodeContents));
            case "opForLocalUnitChance" -> campaignOptions.setOpForLocalUnitChance(Integer.parseInt(nodeContents));
            case "fixedMapChance" -> campaignOptions.setFixedMapChance(Integer.parseInt(nodeContents));
            case "spaUpgradeIntensity" -> campaignOptions.setSpaUpgradeIntensity(Integer.parseInt(nodeContents));
            case "scenarioModMax" -> campaignOptions.setScenarioModMax(Integer.parseInt(nodeContents));
            case "scenarioModChance" -> campaignOptions.setScenarioModChance(Integer.parseInt(nodeContents));
            case "scenarioModBV" -> campaignOptions.setScenarioModBV(Integer.parseInt(nodeContents));
            case "autoConfigMunitions" -> campaignOptions.setAutoConfigMunitions(Boolean.parseBoolean(nodeContents));
            case "autoGenerateOpForCallsigns" -> campaignOptions.setAutoGenerateOpForCallsigns(Boolean.parseBoolean(
                  nodeContents));
            case "minimumCallsignSkillLevel" -> campaignOptions.setMinimumCallsignSkillLevel(SkillLevel.parseFromString(
                  nodeContents));
            case "trackFactionStanding" -> campaignOptions.setTrackFactionStanding(Boolean.parseBoolean(nodeContents));
            case "useFactionStandingNegotiation" ->
                  campaignOptions.setUseFactionStandingNegotiation(Boolean.parseBoolean(
                        nodeContents));
            case "useFactionStandingResupply" -> campaignOptions.setUseFactionStandingResupply(Boolean.parseBoolean(
                  nodeContents));
            case "useFactionStandingCommandCircuit" ->
                  campaignOptions.setUseFactionStandingCommandCircuit(Boolean.parseBoolean(
                        nodeContents));
            case "useFactionStandingOutlawed" -> campaignOptions.setUseFactionStandingOutlawed(Boolean.parseBoolean(
                  nodeContents));
            case "useFactionStandingBatchallRestrictions" ->
                  campaignOptions.setUseFactionStandingBatchallRestrictions(Boolean.parseBoolean(
                        nodeContents));
            case "useFactionStandingRecruitment" ->
                  campaignOptions.setUseFactionStandingRecruitment(Boolean.parseBoolean(
                        nodeContents));
            case "useFactionStandingBarracksCosts" ->
                  campaignOptions.setUseFactionStandingBarracksCosts(Boolean.parseBoolean(
                        nodeContents));
            case "useFactionStandingUnitMarket" -> campaignOptions.setUseFactionStandingUnitMarket(Boolean.parseBoolean(
                  nodeContents));
            case "useFactionStandingContractPay" ->
                  campaignOptions.setUseFactionStandingContractPay(Boolean.parseBoolean(
                        nodeContents));
            case "useFactionStandingSupportPoints" ->
                  campaignOptions.setUseFactionStandingSupportPoints(Boolean.parseBoolean(
                        nodeContents));
            case "factionStandingGainMultiplier" -> campaignOptions.setRegardMultiplier(MathUtility.parseDouble(
                  nodeContents, 1.0));
            default -> throw new IllegalStateException("Potentially unexpected entry in campaign options: " + nodeName);
        }
    }
}
