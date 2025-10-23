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

import static java.lang.Boolean.parseBoolean;
import static megamek.codeUtilities.MathUtility.parseDouble;
import static megamek.codeUtilities.MathUtility.parseInt;
import static mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick.ALL;
import static mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick.SUPPORT;

import java.util.EnumMap;

import megamek.Version;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.Utilities;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.autoResolve.AutoResolveMethod;
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
            case "checkMaintenance" -> campaignOptions.setCheckMaintenance(parseBoolean(nodeContents));
            case "maintenanceCycleDays" -> campaignOptions.setMaintenanceCycleDays(parseInt(nodeContents));
            case "maintenanceBonus" -> campaignOptions.setMaintenanceBonus(parseInt(nodeContents));
            case "useQualityMaintenance" -> campaignOptions.setUseQualityMaintenance(parseBoolean(nodeContents));
            case "reverseQualityNames" -> campaignOptions.setReverseQualityNames(parseBoolean(nodeContents));
            case "useRandomUnitQualities" -> campaignOptions.setUseRandomUnitQualities(parseBoolean(nodeContents));
            case "usePlanetaryModifiers" -> campaignOptions.setUsePlanetaryModifiers(parseBoolean(nodeContents));
            case "useUnofficialMaintenance" -> campaignOptions.setUseUnofficialMaintenance(parseBoolean(
                  nodeContents));
            case "logMaintenance" -> campaignOptions.setLogMaintenance(parseBoolean(nodeContents));
            case "defaultMaintenanceTime" -> campaignOptions.setDefaultMaintenanceTime(parseInt(nodeContents));
            case "mrmsUseRepair" -> campaignOptions.setMRMSUseRepair(parseBoolean(nodeContents));
            case "mrmsUseSalvage" -> campaignOptions.setMRMSUseSalvage(parseBoolean(nodeContents));
            case "mrmsUseExtraTime" -> campaignOptions.setMRMSUseExtraTime(parseBoolean(nodeContents));
            case "mrmsUseRushJob" -> campaignOptions.setMRMSUseRushJob(parseBoolean(nodeContents));
            case "mrmsAllowCarryover" -> campaignOptions.setMRMSAllowCarryover(parseBoolean(nodeContents));
            case "mrmsOptimizeToCompleteToday" -> campaignOptions.setMRMSOptimizeToCompleteToday(parseBoolean(
                  nodeContents));
            case "mrmsScrapImpossible" -> campaignOptions.setMRMSScrapImpossible(parseBoolean(nodeContents));
            case "mrmsUseAssignedTechsFirst" -> campaignOptions.setMRMSUseAssignedTechsFirst(parseBoolean(
                  nodeContents));
            case "mrmsReplacePod" -> campaignOptions.setMRMSReplacePod(parseBoolean(nodeContents));
            case "mrmsOptions" -> campaignOptions.setMRMSOptions(MRMSOption.parseListFromXML(childNode, version));
            case "useFactionForNames" -> campaignOptions.setUseOriginFactionForNames(parseBoolean(nodeContents));
            case "useEraMods" -> campaignOptions.setEraMods(parseBoolean(nodeContents));
            case "assignedTechFirst" -> campaignOptions.setAssignedTechFirst(parseBoolean(nodeContents));
            case "resetToFirstTech" -> campaignOptions.setResetToFirstTech(parseBoolean(nodeContents));
            case "techsUseAdministration" -> campaignOptions.setTechsUseAdministration(parseBoolean(nodeContents));
            case "useUsefulAsTechs" -> campaignOptions.setIsUseUsefulAsTechs(parseBoolean(nodeContents));
            case "useQuirks" -> campaignOptions.setQuirks(parseBoolean(nodeContents));
            case "xpCostMultiplier" -> campaignOptions.setXpCostMultiplier(parseDouble(nodeContents));
            case "scenarioXP" -> campaignOptions.setScenarioXP(parseInt(nodeContents));
            case "killsForXP" -> campaignOptions.setKillsForXP(parseInt(nodeContents));
            case "killXPAward" -> campaignOptions.setKillXPAward(parseInt(nodeContents));
            case "nTasksXP" -> campaignOptions.setNTasksXP(parseInt(nodeContents));
            case "tasksXP" -> campaignOptions.setTaskXP(parseInt(nodeContents));
            case "successXP" -> campaignOptions.setSuccessXP(parseInt(nodeContents));
            case "mistakeXP" -> campaignOptions.setMistakeXP(parseInt(nodeContents));
            case "vocationalXP" -> campaignOptions.setVocationalXP(parseInt(nodeContents));
            case "vocationalXPTargetNumber" -> campaignOptions.setVocationalXPTargetNumber(parseInt(nodeContents));
            case "vocationalXPCheckFrequency" -> campaignOptions.setVocationalXPCheckFrequency(parseInt(
                  nodeContents));
            case "contractNegotiationXP" -> campaignOptions.setContractNegotiationXP(parseInt(nodeContents));
            case "adminWeeklyXP" -> campaignOptions.setAdminXP(parseInt(nodeContents));
            case "adminXPPeriod" -> campaignOptions.setAdminXPPeriod(parseInt(nodeContents));
            case "missionXpFail" -> campaignOptions.setMissionXpFail(parseInt(nodeContents));
            case "missionXpSuccess" -> campaignOptions.setMissionXpSuccess(parseInt(nodeContents));
            case "missionXpOutstandingSuccess" -> campaignOptions.setMissionXpOutstandingSuccess(parseInt(
                  nodeContents));
            case "edgeCost" -> campaignOptions.setEdgeCost(parseInt(nodeContents));
            case "waitingPeriod" -> campaignOptions.setWaitingPeriod(parseInt(nodeContents));
            case "acquisitionSkill" -> campaignOptions.setAcquisitionSkill(nodeContents);
            case "useFunctionalAppraisal" -> campaignOptions.setUseFunctionalAppraisal(parseBoolean(nodeContents));
            case "unitTransitTime" -> campaignOptions.setUnitTransitTime(parseInt(nodeContents));
            case "clanAcquisitionPenalty" -> campaignOptions.setClanAcquisitionPenalty(parseInt(nodeContents));
            case "isAcquisitionPenalty" -> campaignOptions.setIsAcquisitionPenalty(parseInt(nodeContents));
            case "usePlanetaryAcquisition" -> campaignOptions.setPlanetaryAcquisition(parseBoolean(nodeContents));
            case "planetAcquisitionFactionLimit" ->
                  campaignOptions.setPlanetAcquisitionFactionLimit(PlanetaryAcquisitionFactionLimit.parseFromString(
                        nodeContents));
            case "planetAcquisitionNoClanCrossover" ->
                  campaignOptions.setDisallowPlanetAcquisitionClanCrossover(parseBoolean(
                        nodeContents));
            case "noClanPartsFromIS" -> campaignOptions.setDisallowClanPartsFromIS(parseBoolean(nodeContents));
            case "penaltyClanPartsFromIS" -> campaignOptions.setPenaltyClanPartsFromIS(parseInt(nodeContents));
            case "planetAcquisitionVerbose" -> campaignOptions.setPlanetAcquisitionVerboseReporting(parseBoolean(
                  nodeContents));
            case "maxJumpsPlanetaryAcquisition" -> campaignOptions.setMaxJumpsPlanetaryAcquisition(parseInt(
                  nodeContents));
            case "planetTechAcquisitionBonus" -> {
                EnumMap<PlanetarySophistication, Integer> acquisitionBonuses = campaignOptions.getAllPlanetTechAcquisitionBonuses();

                String[] values = nodeContents.split(",");
                if (values.length == 6) {
                    // < 0.50.07 compatibility handler
                    acquisitionBonuses.put(PlanetarySophistication.A, parseInt(values[0]));
                    acquisitionBonuses.put(PlanetarySophistication.B, parseInt(values[1]));
                    acquisitionBonuses.put(PlanetarySophistication.C, parseInt(values[2]));
                    acquisitionBonuses.put(PlanetarySophistication.D, parseInt(values[3]));
                    acquisitionBonuses.put(PlanetarySophistication.F, parseInt(values[5]));
                } else if (values.length == PlanetarySophistication.values().length) {
                    // >= 0.50.07 compatibility handler
                    for (int i = 0; i < values.length; i++) {
                        acquisitionBonuses.put(PlanetarySophistication.fromIndex(i), parseInt(values[i]));
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
                    acquisitionBonuses.put(PlanetaryRating.A, parseInt(values[0]));
                    acquisitionBonuses.put(PlanetaryRating.B, parseInt(values[1]));
                    acquisitionBonuses.put(PlanetaryRating.C, parseInt(values[2]));
                    acquisitionBonuses.put(PlanetaryRating.D, parseInt(values[3]));
                    acquisitionBonuses.put(PlanetaryRating.F, parseInt(values[5]));
                } else if (values.length == PlanetaryRating.values().length) {
                    // >= 0.50.07 compatibility handler
                    for (int i = 0; i < values.length; i++) {
                        acquisitionBonuses.put(PlanetaryRating.fromIndex(i), parseInt(values[i]));
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
                    acquisitionBonuses.put(PlanetaryRating.A, parseInt(values[0]));
                    acquisitionBonuses.put(PlanetaryRating.B, parseInt(values[1]));
                    acquisitionBonuses.put(PlanetaryRating.C, parseInt(values[2]));
                    acquisitionBonuses.put(PlanetaryRating.D, parseInt(values[3]));
                    acquisitionBonuses.put(PlanetaryRating.F, parseInt(values[5]));
                } else if (values.length == PlanetaryRating.values().length) {
                    // >= 0.50.07 compatibility handler
                    for (int i = 0; i < values.length; i++) {
                        acquisitionBonuses.put(PlanetaryRating.fromIndex(i), parseInt(values[i]));
                    }
                } else {
                    LOGGER.error("Invalid number of values for planetOutputAcquisitionBonus: {}", values.length);
                }
            }
            case "equipmentContractPercent" -> campaignOptions.setEquipmentContractPercent(parseDouble(
                  nodeContents));
            case "dropShipContractPercent" -> campaignOptions.setDropShipContractPercent(parseDouble(nodeContents));
            case "jumpShipContractPercent" -> campaignOptions.setJumpShipContractPercent(parseDouble(nodeContents));
            case "warShipContractPercent" -> campaignOptions.setWarShipContractPercent(parseDouble(nodeContents));
            case "equipmentContractBase" -> campaignOptions.setEquipmentContractBase(parseBoolean(nodeContents));
            case "equipmentContractSaleValue" -> campaignOptions.setEquipmentContractSaleValue(parseBoolean(
                  nodeContents));
            case "blcSaleValue" -> campaignOptions.setBLCSaleValue(parseBoolean(nodeContents));
            case "overageRepaymentInFinalPayment" -> campaignOptions.setOverageRepaymentInFinalPayment(parseBoolean(
                  nodeContents));
            case "acquisitionSupportStaffOnly" -> campaignOptions.setAcquisitionPersonnelCategory(parseBoolean(
                  nodeContents) ? SUPPORT : ALL);
            case "acquisitionPersonnelCategory" ->
                  campaignOptions.setAcquisitionPersonnelCategory(ProcurementPersonnelPick.fromString(
                        nodeContents));
            case "limitByYear" -> campaignOptions.setLimitByYear(parseBoolean(nodeContents));
            case "disallowExtinctStuff" -> campaignOptions.setDisallowExtinctStuff(parseBoolean(nodeContents));
            case "allowClanPurchases" -> campaignOptions.setAllowClanPurchases(parseBoolean(nodeContents));
            case "allowISPurchases" -> campaignOptions.setAllowISPurchases(parseBoolean(nodeContents));
            case "allowCanonOnly" -> campaignOptions.setAllowCanonOnly(parseBoolean(nodeContents));
            case "allowCanonRefitOnly" -> campaignOptions.setAllowCanonRefitOnly(parseBoolean(nodeContents));
            case "useAmmoByType" -> campaignOptions.setUseAmmoByType(parseBoolean(nodeContents));
            case "variableTechLevel" -> campaignOptions.setVariableTechLevel(parseBoolean(nodeContents));
            case "factionIntroDate" -> campaignOptions.setIsUseFactionIntroDate(parseBoolean(nodeContents));
            case "techLevel" -> campaignOptions.setTechLevel(parseInt(nodeContents));
            case "unitRatingMethod", "dragoonsRatingMethod" ->
                  campaignOptions.setUnitRatingMethod(UnitRatingMethod.parseFromString(
                        nodeContents));
            case "manualUnitRatingModifier" -> campaignOptions.setManualUnitRatingModifier(parseInt(nodeContents));
            case "clampReputationPayMultiplier" -> campaignOptions.setClampReputationPayMultiplier(parseBoolean(
                  nodeContents));
            case "reduceReputationPerformanceModifier" ->
                  campaignOptions.setReduceReputationPerformanceModifier(parseBoolean(
                        nodeContents));
            case "reputationPerformanceModifierCutOff" ->
                  campaignOptions.setReputationPerformanceModifierCutOff(parseBoolean(
                        nodeContents));
            case "usePortraitForType" -> {
                String[] values = nodeContents.split(",");
                for (int i = 0; i < values.length; i++) {
                    campaignOptions.setUsePortraitForRole(i, parseBoolean(values[i].trim()));
                }
            }
            case "assignPortraitOnRoleChange" -> campaignOptions.setAssignPortraitOnRoleChange(parseBoolean(
                  nodeContents));
            case "allowDuplicatePortraits" -> campaignOptions.setAllowDuplicatePortraits(parseBoolean(
                  nodeContents));
            case "destroyByMargin" -> campaignOptions.setDestroyByMargin(parseBoolean(nodeContents));
            case "destroyMargin" -> campaignOptions.setDestroyMargin(parseInt(nodeContents));
            case "destroyPartTarget" -> campaignOptions.setDestroyPartTarget(parseInt(nodeContents));
            case "useAeroSystemHits" -> campaignOptions.setUseAeroSystemHits(parseBoolean(nodeContents));
            case "maxAcquisitions" -> campaignOptions.setMaxAcquisitions(parseInt(nodeContents));
            case "autoLogisticsHeatSink" -> campaignOptions.setAutoLogisticsHeatSink(parseInt(nodeContents));
            case "autoLogisticsMekHead" -> campaignOptions.setAutoLogisticsMekHead(parseInt(nodeContents));
            case "autoLogisticsMekLocation" -> campaignOptions.setAutoLogisticsMekLocation(parseInt(
                  nodeContents));
            case "autoLogisticsNonRepairableLocation" -> campaignOptions.setAutoLogisticsNonRepairableLocation(parseInt(
                  nodeContents));
            case "autoLogisticsArmor" -> campaignOptions.setAutoLogisticsArmor(parseInt(nodeContents));
            case "autoLogisticsAmmunition" -> campaignOptions.setAutoLogisticsAmmunition(parseInt(
                  nodeContents));
            case "autoLogisticsActuators" -> campaignOptions.setAutoLogisticsActuators(parseInt(nodeContents));
            case "autoLogisticsJumpJets" -> campaignOptions.setAutoLogisticsJumpJets(parseInt(nodeContents));
            case "autoLogisticsEngines" -> campaignOptions.setAutoLogisticsEngines(parseInt(nodeContents));
            case "autoLogisticsWeapons" -> campaignOptions.setAutoLogisticsWeapons(parseInt(nodeContents));
            case "autoLogisticsOther" -> campaignOptions.setAutoLogisticsOther(parseInt(nodeContents));
            case "useTactics" -> campaignOptions.setUseTactics(parseBoolean(nodeContents));
            case "useInitiativeBonus" -> campaignOptions.setUseInitiativeBonus(parseBoolean(nodeContents));
            case "useToughness" -> campaignOptions.setUseToughness(parseBoolean(nodeContents));
            case "useRandomToughness" -> campaignOptions.setUseRandomToughness(parseBoolean(nodeContents));
            case "useArtillery" -> campaignOptions.setUseArtillery(parseBoolean(nodeContents));
            case "useAbilities" -> campaignOptions.setUseAbilities(parseBoolean(nodeContents));
            case "useCommanderAbilitiesOnly" -> campaignOptions.setUseCommanderAbilitiesOnly(parseBoolean(
                  nodeContents));
            case "useEdge" -> campaignOptions.setUseEdge(parseBoolean(nodeContents));
            case "useSupportEdge" -> campaignOptions.setUseSupportEdge(parseBoolean(nodeContents));
            case "useImplants" -> campaignOptions.setUseImplants(parseBoolean(nodeContents));
            case "alternativeQualityAveraging" -> campaignOptions.setAlternativeQualityAveraging(parseBoolean(
                  nodeContents));
            case "useAgeEffects" -> campaignOptions.setUseAgeEffects(parseBoolean(nodeContents));
            case "useTransfers" -> campaignOptions.setUseTransfers(parseBoolean(nodeContents));
            case "useExtendedTOEForceName" -> campaignOptions.setUseExtendedTOEForceName(parseBoolean(
                  nodeContents));
            case "personnelLogSkillGain" -> campaignOptions.setPersonnelLogSkillGain(parseBoolean(nodeContents));
            case "personnelLogAbilityGain" -> campaignOptions.setPersonnelLogAbilityGain(parseBoolean(
                  nodeContents));
            case "personnelLogEdgeGain" -> campaignOptions.setPersonnelLogEdgeGain(parseBoolean(nodeContents));
            case "displayPersonnelLog" -> campaignOptions.setDisplayPersonnelLog(parseBoolean(nodeContents));
            case "displayScenarioLog" -> campaignOptions.setDisplayScenarioLog(parseBoolean(nodeContents));
            case "displayKillRecord" -> campaignOptions.setDisplayKillRecord(parseBoolean(nodeContents));
            case "displayMedicalRecord" -> campaignOptions.setDisplayMedicalRecord(parseBoolean(nodeContents));
            case "displayPatientRecord" -> campaignOptions.setDisplayPatientRecord(parseBoolean(nodeContents));
            case "displayAssignmentRecord" -> campaignOptions.setDisplayAssignmentRecord(parseBoolean(
                  nodeContents));
            case "displayPerformanceRecord" -> campaignOptions.setDisplayPerformanceRecord(parseBoolean(
                  nodeContents));
            case "rewardComingOfAgeAbilities" -> campaignOptions.setRewardComingOfAgeAbilities(parseBoolean(
                  nodeContents));
            case "rewardComingOfAgeRPSkills" -> campaignOptions.setRewardComingOfAgeRPSkills(parseBoolean(
                  nodeContents));
            case "useTimeInService" -> campaignOptions.setUseTimeInService(parseBoolean(nodeContents));
            case "timeInServiceDisplayFormat" ->
                  campaignOptions.setTimeInServiceDisplayFormat(TimeInDisplayFormat.valueOf(
                        nodeContents));
            case "useTimeInRank" -> campaignOptions.setUseTimeInRank(parseBoolean(nodeContents));
            case "timeInRankDisplayFormat" -> campaignOptions.setTimeInRankDisplayFormat(TimeInDisplayFormat.valueOf(
                  nodeContents));
            case "trackTotalEarnings" -> campaignOptions.setTrackTotalEarnings(parseBoolean(nodeContents));
            case "trackTotalXPEarnings" -> campaignOptions.setTrackTotalXPEarnings(parseBoolean(nodeContents));
            case "showOriginFaction" -> campaignOptions.setShowOriginFaction(parseBoolean(nodeContents));
            case "adminsHaveNegotiation" -> campaignOptions.setAdminsHaveNegotiation(parseBoolean(nodeContents));
            case "adminExperienceLevelIncludeNegotiation" ->
                  campaignOptions.setAdminExperienceLevelIncludeNegotiation(parseBoolean(
                        nodeContents));
            case "useAdvancedMedical" -> campaignOptions.setUseAdvancedMedical(parseBoolean(nodeContents));
            case "healWaitingPeriod" -> campaignOptions.setHealingWaitingPeriod(parseInt(nodeContents));
            case "naturalHealingWaitingPeriod" -> campaignOptions.setNaturalHealingWaitingPeriod(parseInt(
                  nodeContents));
            case "minimumHitsForVehicles" -> campaignOptions.setMinimumHitsForVehicles(parseInt(nodeContents));
            case "useRandomHitsForVehicles" -> campaignOptions.setUseRandomHitsForVehicles(parseBoolean(
                  nodeContents));
            case "tougherHealing" -> campaignOptions.setTougherHealing(parseBoolean(nodeContents));
            case "maximumPatients" -> campaignOptions.setMaximumPatients(parseInt(nodeContents));
            case "doctorsUseAdministration" -> campaignOptions.setDoctorsUseAdministration(parseBoolean(
                  nodeContents));
            case "useUsefulMedics" -> campaignOptions.setIsUseUsefulMedics(parseBoolean(nodeContents));
            case "useMASHTheatres" -> campaignOptions.setIsUseMASHTheatres(parseBoolean(nodeContents));
            case "mashTheatreCapacity" -> campaignOptions.setMASHTheatreCapacity(parseInt(nodeContents));
            case "prisonerCaptureStyle" -> campaignOptions.setPrisonerCaptureStyle(PrisonerCaptureStyle.fromString(
                  nodeContents));
            case "useFunctionalEscapeArtist" -> campaignOptions.setUseFunctionalEscapeArtist(parseBoolean(
                  nodeContents));
            case "useRandomDependentAddition" -> campaignOptions.setUseRandomDependentAddition(parseBoolean(
                  nodeContents));
            case "useRandomDependentRemoval" -> campaignOptions.setUseRandomDependentRemoval(parseBoolean(
                  nodeContents));
            case "dependentProfessionDieSize" -> campaignOptions.setDependentProfessionDieSize(parseInt(
                  nodeContents, 4));
            case "civilianProfessionDieSize" -> campaignOptions.setCivilianProfessionDieSize(parseInt(
                  nodeContents, 2));
            case "usePersonnelRemoval" -> campaignOptions.setUsePersonnelRemoval(parseBoolean(nodeContents));
            case "useRemovalExemptCemetery" -> campaignOptions.setUseRemovalExemptCemetery(parseBoolean(
                  nodeContents));
            case "useRemovalExemptRetirees" -> campaignOptions.setUseRemovalExemptRetirees(parseBoolean(
                  nodeContents));
            case "disableSecondaryRoleSalary" -> campaignOptions.setDisableSecondaryRoleSalary(parseBoolean(
                  nodeContents));
            case "salaryAntiMekMultiplier" -> campaignOptions.setSalaryAntiMekMultiplier(parseDouble(nodeContents));
            case "salarySpecialistInfantryMultiplier" ->
                  campaignOptions.setSalarySpecialistInfantryMultiplier(parseDouble(
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
                                parseDouble(wn3.getTextContent().trim()));
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
            case "enableAutoAwards" -> campaignOptions.setEnableAutoAwards(parseBoolean(nodeContents));
            case "issuePosthumousAwards" -> campaignOptions.setIssuePosthumousAwards(parseBoolean(nodeContents));
            case "issueBestAwardOnly" -> campaignOptions.setIssueBestAwardOnly(parseBoolean(nodeContents));
            case "ignoreStandardSet" -> campaignOptions.setIgnoreStandardSet(parseBoolean(nodeContents));
            case "awardTierSize" -> campaignOptions.setAwardTierSize(parseInt(nodeContents));
            case "enableContractAwards" -> campaignOptions.setEnableContractAwards(parseBoolean(nodeContents));
            case "enableFactionHunterAwards" -> campaignOptions.setEnableFactionHunterAwards(parseBoolean(
                  nodeContents));
            case "enableInjuryAwards" -> campaignOptions.setEnableInjuryAwards(parseBoolean(nodeContents));
            case "enableIndividualKillAwards" -> campaignOptions.setEnableIndividualKillAwards(parseBoolean(
                  nodeContents));
            case "enableFormationKillAwards" -> campaignOptions.setEnableFormationKillAwards(parseBoolean(
                  nodeContents));
            case "enableRankAwards" -> campaignOptions.setEnableRankAwards(parseBoolean(nodeContents));
            case "enableScenarioAwards" -> campaignOptions.setEnableScenarioAwards(parseBoolean(nodeContents));
            case "enableSkillAwards" -> campaignOptions.setEnableSkillAwards(parseBoolean(nodeContents));
            case "enableTheatreOfWarAwards" -> campaignOptions.setEnableTheatreOfWarAwards(parseBoolean(
                  nodeContents));
            case "enableTimeAwards" -> campaignOptions.setEnableTimeAwards(parseBoolean(nodeContents));
            case "enableTrainingAwards" -> campaignOptions.setEnableTrainingAwards(parseBoolean(nodeContents));
            case "enableMiscAwards" -> campaignOptions.setEnableMiscAwards(parseBoolean(nodeContents));
            case "awardSetFilterList" -> campaignOptions.setAwardSetFilterList(nodeContents);
            case "useDylansRandomXP" -> campaignOptions.setUseDylansRandomXP(parseBoolean(nodeContents));
            case "nonBinaryDiceSize" -> campaignOptions.setNonBinaryDiceSize(parseInt(nodeContents));
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
            case "useRandomPersonalities" -> campaignOptions.setUseRandomPersonalities(parseBoolean(nodeContents));
            case "useRandomPersonalityReputation" -> campaignOptions.setUseRandomPersonalityReputation(parseBoolean(
                  nodeContents));
            case "useReasoningXpMultiplier" -> campaignOptions.setUseReasoningXpMultiplier(parseBoolean(
                  nodeContents));
            case "useSimulatedRelationships" -> campaignOptions.setUseSimulatedRelationships(parseBoolean(
                  nodeContents));
            case "familyDisplayLevel" ->
                  campaignOptions.setFamilyDisplayLevel(FamilialRelationshipDisplayLevel.parseFromString(
                        nodeContents));
            case "announceBirthdays" -> campaignOptions.setAnnounceBirthdays(parseBoolean(nodeContents));
            case "announceRecruitmentAnniversaries" -> campaignOptions.setAnnounceRecruitmentAnniversaries(parseBoolean(
                  nodeContents));
            case "announceOfficersOnly" -> campaignOptions.setAnnounceOfficersOnly(parseBoolean(nodeContents));
            case "announceChildBirthdays" -> campaignOptions.setAnnounceChildBirthdays(parseBoolean(nodeContents));
            case "showLifeEventDialogBirths" -> campaignOptions.setShowLifeEventDialogBirths(parseBoolean(
                  nodeContents));
            case "showLifeEventDialogComingOfAge" -> campaignOptions.setShowLifeEventDialogComingOfAge(parseBoolean(
                  nodeContents));
            case "showLifeEventDialogCelebrations" -> campaignOptions.setShowLifeEventDialogCelebrations(parseBoolean(
                  nodeContents));
            case "useManualMarriages" -> campaignOptions.setUseManualMarriages(parseBoolean(nodeContents));
            case "useClanPersonnelMarriages" -> campaignOptions.setUseClanPersonnelMarriages(parseBoolean(
                  nodeContents));
            case "usePrisonerMarriages" -> campaignOptions.setUsePrisonerMarriages(parseBoolean(nodeContents));
            case "checkMutualAncestorsDepth" -> campaignOptions.setCheckMutualAncestorsDepth(parseInt(
                  nodeContents));
            case "noInterestInMarriageDiceSize" -> campaignOptions.setNoInterestInMarriageDiceSize(parseInt(
                  nodeContents));
            case "logMarriageNameChanges" -> campaignOptions.setLogMarriageNameChanges(parseBoolean(nodeContents));
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
                                parseInt(wn3.getTextContent().trim()));
                }
            }
            case "randomMarriageMethod" -> campaignOptions.setRandomMarriageMethod(RandomMarriageMethod.fromString(
                  nodeContents));
            case "useRandomClanPersonnelMarriages" -> campaignOptions.setUseRandomClanPersonnelMarriages(parseBoolean(
                  nodeContents));
            case "useRandomPrisonerMarriages" -> campaignOptions.setUseRandomPrisonerMarriages(parseBoolean(
                  nodeContents));
            case "randomMarriageAgeRange" -> campaignOptions.setRandomMarriageAgeRange(parseInt(nodeContents));
            case "randomMarriageDiceSize" -> campaignOptions.setRandomMarriageDiceSize(parseInt(nodeContents));
            case "randomSameSexMarriageDiceSize" -> campaignOptions.setRandomSameSexMarriageDiceSize(parseInt(
                  nodeContents));
            case "randomNewDependentMarriage" -> campaignOptions.setRandomNewDependentMarriage(parseInt(
                  nodeContents));
            case "useManualDivorce" -> campaignOptions.setUseManualDivorce(parseBoolean(nodeContents));
            case "useClanPersonnelDivorce" -> campaignOptions.setUseClanPersonnelDivorce(parseBoolean(
                  nodeContents));
            case "usePrisonerDivorce" -> campaignOptions.setUsePrisonerDivorce(parseBoolean(nodeContents));
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
                                parseInt(wn3.getTextContent().trim()));
                }
            }
            case "randomDivorceMethod" -> campaignOptions.setRandomDivorceMethod(RandomDivorceMethod.fromString(
                  nodeContents));
            case "useRandomOppositeSexDivorce" -> campaignOptions.setUseRandomOppositeSexDivorce(parseBoolean(
                  nodeContents));
            case "useRandomSameSexDivorce" -> campaignOptions.setUseRandomSameSexDivorce(parseBoolean(
                  nodeContents));
            case "useRandomClanPersonnelDivorce" -> campaignOptions.setUseRandomClanPersonnelDivorce(parseBoolean(
                  nodeContents));
            case "useRandomPrisonerDivorce" -> campaignOptions.setUseRandomPrisonerDivorce(parseBoolean(
                  nodeContents));
            case "randomDivorceDiceSize" -> campaignOptions.setRandomDivorceDiceSize(parseInt(nodeContents));
            case "useManualProcreation" -> campaignOptions.setUseManualProcreation(parseBoolean(nodeContents));
            case "useClanPersonnelProcreation" -> campaignOptions.setUseClanPersonnelProcreation(parseBoolean(
                  nodeContents));
            case "usePrisonerProcreation" -> campaignOptions.setUsePrisonerProcreation(parseBoolean(nodeContents));
            case "multiplePregnancyOccurrences" -> campaignOptions.setMultiplePregnancyOccurrences(parseInt(
                  nodeContents));
            case "babySurnameStyle" ->
                  campaignOptions.setBabySurnameStyle(BabySurnameStyle.parseFromString(nodeContents));
            case "assignNonPrisonerBabiesFounderTag" ->
                  campaignOptions.setAssignNonPrisonerBabiesFounderTag(parseBoolean(
                        nodeContents));
            case "assignChildrenOfFoundersFounderTag" ->
                  campaignOptions.setAssignChildrenOfFoundersFounderTag(parseBoolean(
                        nodeContents));
            case "useMaternityLeave" -> campaignOptions.setUseMaternityLeave(parseBoolean(nodeContents));
            case "determineFatherAtBirth" -> campaignOptions.setDetermineFatherAtBirth(parseBoolean(nodeContents));
            case "displayTrueDueDate" -> campaignOptions.setDisplayTrueDueDate(parseBoolean(nodeContents));
            case "noInterestInChildrenDiceSize" -> campaignOptions.setNoInterestInChildrenDiceSize(parseInt(
                  nodeContents));
            case "logProcreation" -> campaignOptions.setLogProcreation(parseBoolean(nodeContents));
            case "randomProcreationMethod" ->
                  campaignOptions.setRandomProcreationMethod(RandomProcreationMethod.fromString(
                        nodeContents));
            case "useRelationshiplessRandomProcreation" ->
                  campaignOptions.setUseRelationshiplessRandomProcreation(parseBoolean(
                        nodeContents));
            case "useRandomClanPersonnelProcreation" ->
                  campaignOptions.setUseRandomClanPersonnelProcreation(parseBoolean(
                        nodeContents));
            case "useRandomPrisonerProcreation" -> campaignOptions.setUseRandomPrisonerProcreation(parseBoolean(
                  nodeContents));
            case "randomProcreationRelationshipDiceSize" ->
                  campaignOptions.setRandomProcreationRelationshipDiceSize(parseInt(
                        nodeContents));
            case "randomProcreationRelationshiplessDiceSize" ->
                  campaignOptions.setRandomProcreationRelationshiplessDiceSize(parseInt(
                        nodeContents));
            case "useEducationModule" -> campaignOptions.setUseEducationModule(parseBoolean(nodeContents));
            case "curriculumXpRate" -> campaignOptions.setCurriculumXpRate(parseInt(nodeContents));
            case "maximumJumpCount" -> campaignOptions.setMaximumJumpCount(parseInt(nodeContents));
            case "useReeducationCamps" -> campaignOptions.setUseReeducationCamps(parseBoolean(nodeContents));
            case "enableLocalAcademies" -> campaignOptions.setEnableLocalAcademies(parseBoolean(nodeContents));
            case "enablePrestigiousAcademies" -> campaignOptions.setEnablePrestigiousAcademies(parseBoolean(
                  nodeContents));
            case "enableUnitEducation" -> campaignOptions.setEnableUnitEducation(parseBoolean(nodeContents));
            case "enableOverrideRequirements" -> campaignOptions.setEnableOverrideRequirements(parseBoolean(
                  nodeContents));
            case "enableShowIneligibleAcademies" -> campaignOptions.setEnableShowIneligibleAcademies(parseBoolean(
                  nodeContents));
            case "entranceExamBaseTargetNumber" -> campaignOptions.setEntranceExamBaseTargetNumber(parseInt(
                  nodeContents));
            case "facultyXpRate" -> campaignOptions.setFacultyXpRate(parseDouble(nodeContents));
            case "enableBonuses" -> campaignOptions.setEnableBonuses(parseBoolean(nodeContents));
            case "adultDropoutChance" -> campaignOptions.setAdultDropoutChance(parseInt(nodeContents));
            case "childrenDropoutChance" -> campaignOptions.setChildrenDropoutChance(parseInt(nodeContents));
            case "allAges" -> campaignOptions.setAllAges(parseBoolean(nodeContents));
            case "militaryAcademyAccidents" -> campaignOptions.setMilitaryAcademyAccidents(parseInt(nodeContents));
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
                                    parseBoolean(wn3.getTextContent().trim()));
                    } catch (Exception ignored) {

                    }
                }
            }
            case "useRandomDeathSuicideCause" -> campaignOptions.setUseRandomDeathSuicideCause(parseBoolean(
                  nodeContents));
            case "randomDeathMultiplier" -> campaignOptions.setRandomDeathMultiplier(parseDouble(nodeContents));
            case "useRandomRetirement" -> campaignOptions.setUseRandomRetirement(parseBoolean(nodeContents));
            case "turnoverBaseTn" -> campaignOptions.setTurnoverFixedTargetNumber(parseInt(nodeContents));
            case "turnoverFrequency" -> campaignOptions.setTurnoverFrequency(TurnoverFrequency.valueOf(nodeContents));
            case "trackOriginalUnit" -> campaignOptions.setTrackOriginalUnit(parseBoolean(nodeContents));
            case "aeroRecruitsHaveUnits" -> campaignOptions.setAeroRecruitsHaveUnits(parseBoolean(nodeContents));
            case "useContractCompletionRandomRetirement" ->
                  campaignOptions.setUseContractCompletionRandomRetirement(parseBoolean(
                        nodeContents));
            case "useRandomFounderTurnover" -> campaignOptions.setUseRandomFounderTurnover(parseBoolean(
                  nodeContents));
            case "useFounderRetirement" -> campaignOptions.setUseFounderRetirement(parseBoolean(nodeContents));
            case "useSubContractSoldiers" -> campaignOptions.setUseSubContractSoldiers(parseBoolean(nodeContents));
            case "serviceContractDuration" -> campaignOptions.setServiceContractDuration(parseInt(nodeContents));
            case "serviceContractModifier" -> campaignOptions.setServiceContractModifier(parseInt(nodeContents));
            case "payBonusDefault" -> campaignOptions.setPayBonusDefault(parseBoolean(nodeContents));
            case "payBonusDefaultThreshold" -> campaignOptions.setPayBonusDefaultThreshold(parseInt(nodeContents));
            case "useCustomRetirementModifiers" -> campaignOptions.setUseCustomRetirementModifiers(parseBoolean(
                  nodeContents));
            case "useFatigueModifiers" -> campaignOptions.setUseFatigueModifiers(parseBoolean(nodeContents));
            case "useSkillModifiers" -> campaignOptions.setUseSkillModifiers(parseBoolean(nodeContents));
            case "useAgeModifiers" -> campaignOptions.setUseAgeModifiers(parseBoolean(nodeContents));
            case "useUnitRatingModifiers" -> campaignOptions.setUseUnitRatingModifiers(parseBoolean(nodeContents));
            case "useFactionModifiers" -> campaignOptions.setUseFactionModifiers(parseBoolean(nodeContents));
            case "useMissionStatusModifiers" -> campaignOptions.setUseMissionStatusModifiers(parseBoolean(
                  nodeContents));
            case "useHostileTerritoryModifiers" -> campaignOptions.setUseHostileTerritoryModifiers(parseBoolean(
                  nodeContents));
            case "useFamilyModifiers" -> campaignOptions.setUseFamilyModifiers(parseBoolean(nodeContents));
            case "useLoyaltyModifiers" -> campaignOptions.setUseLoyaltyModifiers(parseBoolean(nodeContents));
            case "useHideLoyalty" -> campaignOptions.setUseHideLoyalty(parseBoolean(nodeContents));
            case "payoutRateOfficer" -> campaignOptions.setPayoutRateOfficer(parseInt(nodeContents));
            case "payoutRateEnlisted" -> campaignOptions.setPayoutRateEnlisted(parseInt(nodeContents));
            case "payoutRetirementMultiplier" -> campaignOptions.setPayoutRetirementMultiplier(parseInt(
                  nodeContents));
            case "usePayoutServiceBonus" -> campaignOptions.setUsePayoutServiceBonus(parseBoolean(nodeContents));
            case "payoutServiceBonusRate" -> campaignOptions.setPayoutServiceBonusRate(parseInt(nodeContents));
            // 'useAdministrativeStrain' is <50.07
            case "UseHRStrain", "useAdministrativeStrain" -> campaignOptions.setUseHRStrain(parseBoolean(nodeContents));
            // 'administrativeStrain' is <50.07
            case "hrStrain", "administrativeStrain" -> campaignOptions.setHRCapacity(parseInt(nodeContents));
            case "useManagementSkill" -> campaignOptions.setUseManagementSkill(parseBoolean(nodeContents));
            case "useCommanderLeadershipOnly" -> campaignOptions.setUseCommanderLeadershipOnly(parseBoolean(
                  nodeContents));
            case "managementSkillPenalty" -> campaignOptions.setManagementSkillPenalty(parseInt(nodeContents));
            case "useFatigue" -> campaignOptions.setUseFatigue(parseBoolean(nodeContents));
            case "fatigueRate" -> campaignOptions.setFatigueRate(parseInt(nodeContents));
            case "useInjuryFatigue" -> campaignOptions.setUseInjuryFatigue(parseBoolean(nodeContents));
            case "fieldKitchenCapacity" -> campaignOptions.setFieldKitchenCapacity(parseInt(nodeContents));
            case "fieldKitchenIgnoreNonCombatants" -> campaignOptions.setFieldKitchenIgnoreNonCombatants(parseBoolean(
                  nodeContents));
            case "fatigueLeaveThreshold" -> campaignOptions.setFatigueLeaveThreshold(parseInt(nodeContents));
            case "payForParts" -> campaignOptions.setPayForParts(parseBoolean(nodeContents));
            case "payForRepairs" -> campaignOptions.setPayForRepairs(parseBoolean(nodeContents));
            case "payForUnits" -> campaignOptions.setPayForUnits(parseBoolean(nodeContents));
            case "payForSalaries" -> campaignOptions.setPayForSalaries(parseBoolean(nodeContents));
            case "payForOverhead" -> campaignOptions.setPayForOverhead(parseBoolean(nodeContents));
            case "payForMaintain" -> campaignOptions.setPayForMaintain(parseBoolean(nodeContents));
            case "payForTransport" -> campaignOptions.setPayForTransport(parseBoolean(nodeContents));
            case "sellUnits" -> campaignOptions.setSellUnits(parseBoolean(nodeContents));
            case "sellParts" -> campaignOptions.setSellParts(parseBoolean(nodeContents));
            case "payForRecruitment" -> campaignOptions.setPayForRecruitment(parseBoolean(nodeContents));
            case "payForFood" -> campaignOptions.setPayForFood(parseBoolean(nodeContents));
            case "payForHousing" -> campaignOptions.setPayForHousing(parseBoolean(nodeContents));
            case "useLoanLimits" -> campaignOptions.setLoanLimits(parseBoolean(nodeContents));
            case "usePercentageMaint" -> campaignOptions.setUsePercentageMaintenance(parseBoolean(nodeContents));
            case "infantryDontCount" -> campaignOptions.setUseInfantryDontCount(parseBoolean(nodeContents));
            case "usePeacetimeCost" -> campaignOptions.setUsePeacetimeCost(parseBoolean(nodeContents));
            case "useExtendedPartsModifier" -> campaignOptions.setUseExtendedPartsModifier(parseBoolean(
                  nodeContents));
            case "showPeacetimeCost" -> campaignOptions.setShowPeacetimeCost(parseBoolean(nodeContents));
            case "newFinancialYearFinancesToCSVExport" ->
                  campaignOptions.setNewFinancialYearFinancesToCSVExport(parseBoolean(
                        nodeContents));
            case "financialYearDuration" ->
                  campaignOptions.setFinancialYearDuration(FinancialYearDuration.parseFromString(
                        nodeContents));
            case "simulateGrayMonday" -> campaignOptions.setSimulateGrayMonday(parseBoolean(nodeContents));
            case "allowMonthlyReinvestment" -> campaignOptions.setAllowMonthlyReinvestment(parseBoolean(
                  nodeContents));
            case "displayAllAttributes" -> campaignOptions.setDisplayAllAttributes(parseBoolean(
                  nodeContents));
            case "allowMonthlyConnections" -> campaignOptions.setAllowMonthlyConnections(parseBoolean(
                  nodeContents));
            case "commonPartPriceMultiplier" -> campaignOptions.setCommonPartPriceMultiplier(parseDouble(
                  nodeContents));
            case "innerSphereUnitPriceMultiplier" -> campaignOptions.setInnerSphereUnitPriceMultiplier(parseDouble(
                  nodeContents));
            case "innerSpherePartPriceMultiplier" -> campaignOptions.setInnerSpherePartPriceMultiplier(parseDouble(
                  nodeContents));
            case "clanUnitPriceMultiplier" -> campaignOptions.setClanUnitPriceMultiplier(parseDouble(nodeContents));
            case "clanPartPriceMultiplier" -> campaignOptions.setClanPartPriceMultiplier(parseDouble(nodeContents));
            case "mixedTechUnitPriceMultiplier" -> campaignOptions.setMixedTechUnitPriceMultiplier(parseDouble(
                  nodeContents));
            case "usedPartPriceMultipliers" -> {
                final String[] values = nodeContents.split(",");
                for (int i = 0; i < values.length; i++) {
                    try {
                        campaignOptions.getUsedPartPriceMultipliers()[i] = parseDouble(values[i]);
                    } catch (Exception ignored) {

                    }
                }
            }
            case "damagedPartsValueMultiplier" -> campaignOptions.setDamagedPartsValueMultiplier(parseDouble(
                  nodeContents));
            case "unrepairablePartsValueMultiplier" -> campaignOptions.setUnrepairablePartsValueMultiplier(parseDouble(
                  nodeContents));
            case "cancelledOrderRefundMultiplier" -> campaignOptions.setCancelledOrderRefundMultiplier(parseDouble(
                  nodeContents));
            case "useTaxes" -> campaignOptions.setUseTaxes(parseBoolean(nodeContents));
            case "taxesPercentage" -> campaignOptions.setTaxesPercentage(parseInt(nodeContents));
            case "useShareSystem" -> campaignOptions.setUseShareSystem(parseBoolean(nodeContents));
            case "sharesForAll" -> campaignOptions.setSharesForAll(parseBoolean(nodeContents));
            case "personnelMarketStyle" -> campaignOptions.setPersonnelMarketStyle(PersonnelMarketStyle.fromString(
                  nodeContents));
            case "personnelMarketName" -> {
                String marketName = nodeContents;
                // Backwards compatibility with saves from before these rules moved to CamOps
                if (marketName.equals("Strat Ops")) {
                    marketName = "Campaign Ops";
                }
                campaignOptions.setPersonnelMarketName(marketName);
            }
            case "personnelMarketReportRefresh" -> campaignOptions.setPersonnelMarketReportRefresh(parseBoolean(
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
                                parseInt(wn3.getTextContent().trim()));
                }
            }
            case "personnelMarketDylansWeight" -> campaignOptions.setPersonnelMarketDylansWeight(parseDouble(
                  nodeContents));
            case "usePersonnelHireHiringHallOnly" -> campaignOptions.setUsePersonnelHireHiringHallOnly(parseBoolean(
                  nodeContents));
            case "unitMarketMethod" -> campaignOptions.setUnitMarketMethod(UnitMarketMethod.valueOf(nodeContents));
            case "unitMarketRegionalMekVariations" -> campaignOptions.setUnitMarketRegionalMekVariations(parseBoolean(
                  nodeContents));
            case "unitMarketArtilleryUnitChance", "unitMarketSpecialUnitChance" ->
                  campaignOptions.setUnitMarketArtilleryUnitChance(parseInt(nodeContents));
            case "unitMarketRarityModifier" -> campaignOptions.setUnitMarketRarityModifier(parseInt(nodeContents));
            case "instantUnitMarketDelivery" -> campaignOptions.setInstantUnitMarketDelivery(parseBoolean(
                  nodeContents));
            case "mothballUnitMarketDeliveries" -> campaignOptions.setMothballUnitMarketDeliveries(parseBoolean(
                  nodeContents));
            case "unitMarketReportRefresh" -> campaignOptions.setUnitMarketReportRefresh(parseBoolean(
                  nodeContents));
            case "contractMarketMethod" -> campaignOptions.setContractMarketMethod(ContractMarketMethod.valueOf(
                  nodeContents));
            case "contractSearchRadius" -> campaignOptions.setContractSearchRadius(parseInt(nodeContents));
            case "variableContractLength" -> campaignOptions.setVariableContractLength(parseBoolean(nodeContents));
            case "useDynamicDifficulty" -> campaignOptions.setUseDynamicDifficulty(parseBoolean(nodeContents));
            case "contractMarketReportRefresh" -> campaignOptions.setContractMarketReportRefresh(parseBoolean(
                  nodeContents));
            case "contractMaxSalvagePercentage" -> campaignOptions.setContractMaxSalvagePercentage(parseInt(
                  nodeContents));
            case "dropShipBonusPercentage" -> campaignOptions.setDropShipBonusPercentage(parseInt(nodeContents));
            case "skillLevel" -> campaignOptions.setSkillLevel(SkillLevel.parseFromString(nodeContents));
            case "autoResolveMethod" -> campaignOptions.setAutoResolveMethod(AutoResolveMethod.valueOf(nodeContents));
            case "autoResolveVictoryChanceEnabled" -> campaignOptions.setAutoResolveVictoryChanceEnabled(parseBoolean(
                  nodeContents));
            case "autoResolveNumberOfScenarios" -> campaignOptions.setAutoResolveNumberOfScenarios(parseInt(
                  nodeContents));
            case "autoResolveUseExperimentalPacarGui" ->
                  campaignOptions.setAutoResolveExperimentalPacarGuiEnabled(parseBoolean(
                        nodeContents));
            case "strategicViewTheme" -> campaignOptions.setStrategicViewTheme(nodeContents);
            case "phenotypeProbabilities" -> {
                String[] values = nodeContents.split(",");
                for (int i = 0; i < values.length; i++) {
                    campaignOptions.setPhenotypeProbability(i, parseInt(values[i]));
                }
            }
            case "useAtB" -> campaignOptions.setUseAtB(parseBoolean(nodeContents));
            case "useStratCon" -> campaignOptions.setUseStratCon(parseBoolean(nodeContents));
            case "useAdvancedScouting" -> campaignOptions.setUseAdvancedScouting(parseBoolean(nodeContents));
            case "useAero" -> campaignOptions.setUseAero(parseBoolean(nodeContents));
            case "useVehicles" -> campaignOptions.setUseVehicles(parseBoolean(nodeContents));
            case "clanVehicles" -> campaignOptions.setClanVehicles(parseBoolean(nodeContents));
            case "useGenericBattleValue" -> campaignOptions.setUseGenericBattleValue(parseBoolean(nodeContents));
            case "useVerboseBidding" -> campaignOptions.setUseVerboseBidding(parseBoolean(nodeContents));
            case "doubleVehicles" -> campaignOptions.setDoubleVehicles(parseBoolean(nodeContents));
            case "adjustPlayerVehicles" -> campaignOptions.setAdjustPlayerVehicles(parseBoolean(nodeContents));
            case "opForLanceTypeMeks" -> campaignOptions.setOpForLanceTypeMeks(parseInt(nodeContents));
            case "opForLanceTypeMixed" -> campaignOptions.setOpForLanceTypeMixed(parseInt(nodeContents));
            case "opForLanceTypeVehicles" -> campaignOptions.setOpForLanceTypeVehicles(parseInt(nodeContents));
            case "opForUsesVTOLs" -> campaignOptions.setOpForUsesVTOLs(parseBoolean(nodeContents));
            case "useDropShips" -> campaignOptions.setUseDropShips(parseBoolean(nodeContents));
            case "mercSizeLimited" -> campaignOptions.setMercSizeLimited(parseBoolean(nodeContents));
            case "regionalMekVariations" -> campaignOptions.setRegionalMekVariations(parseBoolean(nodeContents));
            case "attachedPlayerCamouflage" -> campaignOptions.setAttachedPlayerCamouflage(parseBoolean(
                  nodeContents));
            case "playerControlsAttachedUnits" -> campaignOptions.setPlayerControlsAttachedUnits(parseBoolean(
                  nodeContents));
            case "atbBattleChance" -> {
                String[] values = nodeContents.split(",");
                for (int i = 0; i < values.length; i++) {
                    try {
                        campaignOptions.setAtBBattleChance(i, parseInt(values[i]));
                    } catch (Exception ignored) {
                        // Badly coded, but this is to migrate devs and their games as the swap was done before a
                        // release and is thus better to handle this way than through a more code complex method
                        campaignOptions.setAtBBattleChance(i, (int) Math.round(parseDouble(values[i])));
                    }
                }
            }
            case "generateChases" -> campaignOptions.setGenerateChases(parseBoolean(nodeContents));
            case "useWeatherConditions" -> campaignOptions.setUseWeatherConditions(parseBoolean(nodeContents));
            case "useLightConditions" -> campaignOptions.setUseLightConditions(parseBoolean(nodeContents));
            case "usePlanetaryConditions" -> campaignOptions.setUsePlanetaryConditions(parseBoolean(nodeContents));
            case "restrictPartsByMission" -> campaignOptions.setRestrictPartsByMission(parseBoolean(nodeContents));
            case "allowOpForLocalUnits" -> campaignOptions.setAllowOpForLocalUnits(parseBoolean(nodeContents));
            case "allowOpForAeros" -> campaignOptions.setAllowOpForAerospace(parseBoolean(nodeContents));
            case "opForAeroChance" -> campaignOptions.setOpForAeroChance(parseInt(nodeContents));
            case "opForLocalUnitChance" -> campaignOptions.setOpForLocalUnitChance(parseInt(nodeContents));
            case "fixedMapChance" -> campaignOptions.setFixedMapChance(parseInt(nodeContents));
            case "spaUpgradeIntensity" -> campaignOptions.setSpaUpgradeIntensity(parseInt(nodeContents));
            case "scenarioModMax" -> campaignOptions.setScenarioModMax(parseInt(nodeContents));
            case "scenarioModChance" -> campaignOptions.setScenarioModChance(parseInt(nodeContents));
            case "scenarioModBV" -> campaignOptions.setScenarioModBV(parseInt(nodeContents));
            case "autoConfigMunitions" -> campaignOptions.setAutoConfigMunitions(parseBoolean(nodeContents));
            case "autoGenerateOpForCallsigns", "autoGenerateOpForCallSigns" ->
                  campaignOptions.setAutoGenerateOpForCallSigns(parseBoolean(
                        nodeContents));
            case "minimumCallsignSkillLevel" -> campaignOptions.setMinimumCallsignSkillLevel(SkillLevel.parseFromString(
                  nodeContents));
            case "trackFactionStanding" -> campaignOptions.setTrackFactionStanding(parseBoolean(nodeContents));
            case "trackClimateRegardChanges" ->
                  campaignOptions.setTrackClimateRegardChanges(parseBoolean(nodeContents));
            case "useFactionStandingNegotiation" -> campaignOptions.setUseFactionStandingNegotiation(parseBoolean(
                  nodeContents));
            case "useFactionStandingResupply" -> campaignOptions.setUseFactionStandingResupply(parseBoolean(
                  nodeContents));
            case "useFactionStandingCommandCircuit" -> campaignOptions.setUseFactionStandingCommandCircuit(parseBoolean(
                  nodeContents));
            case "useFactionStandingOutlawed" -> campaignOptions.setUseFactionStandingOutlawed(parseBoolean(
                  nodeContents));
            case "useFactionStandingBatchallRestrictions" ->
                  campaignOptions.setUseFactionStandingBatchallRestrictions(parseBoolean(
                        nodeContents));
            case "useFactionStandingRecruitment" -> campaignOptions.setUseFactionStandingRecruitment(parseBoolean(
                  nodeContents));
            case "useFactionStandingBarracksCosts" -> campaignOptions.setUseFactionStandingBarracksCosts(parseBoolean(
                  nodeContents));
            case "useFactionStandingUnitMarket" -> campaignOptions.setUseFactionStandingUnitMarket(parseBoolean(
                  nodeContents));
            case "useFactionStandingContractPay" -> campaignOptions.setUseFactionStandingContractPay(parseBoolean(
                  nodeContents));
            case "useFactionStandingSupportPoints" -> campaignOptions.setUseFactionStandingSupportPoints(parseBoolean(
                  nodeContents));
            case "factionStandingGainMultiplier" -> campaignOptions.setRegardMultiplier(parseDouble(
                  nodeContents, 1.0));
            default -> LOGGER.warn("Potentially unexpected entry in campaign options: {}", nodeName);
        }
    }
}
