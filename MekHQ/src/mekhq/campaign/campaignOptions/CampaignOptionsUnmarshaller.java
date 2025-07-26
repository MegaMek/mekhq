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
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.autoresolve.AutoResolveMethod;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerCaptureStyle;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick;
import mekhq.service.mrms.MRMSOption;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author natit
 */
public class CampaignOptionsUnmarshaller {
    private static final MMLogger LOGGER = MMLogger.create(CampaignOptionsUnmarshaller.class);

    public static CampaignOptions generateCampaignOptionsFromXml(Node parentNod, Version version) {
        LOGGER.info("Loading Campaign Options from Version {} XML...", version);

        parentNod.normalize();
        CampaignOptions campaignOptions = new CampaignOptions();
        NodeList childNodes = parentNod.getChildNodes();

        // Okay, let's iterate through the children, eh?
        for (int node = 0; node < childNodes.getLength(); node++) {
            Node childNode = childNodes.item(node);
            String nodeName = childNode.getNodeName();

            // If it's not an element node, we ignore it.
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeContents = childNode.getTextContent().trim();

            LOGGER.debug("{}\n\t{}", nodeName, nodeContents);
            try {
                // region Repair and Maintenance Tab
                if (nodeName.equalsIgnoreCase("checkMaintenance")) {
                    campaignOptions.setCheckMaintenance(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("maintenanceCycleDays")) {
                    campaignOptions.setMaintenanceCycleDays(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("maintenanceBonus")) {
                    campaignOptions.setMaintenanceBonus(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useQualityMaintenance")) {
                    campaignOptions.setUseQualityMaintenance(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("reverseQualityNames")) {
                    campaignOptions.setReverseQualityNames(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomUnitQualities")) {
                    campaignOptions.setUseRandomUnitQualities(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("usePlanetaryModifiers")) {
                    campaignOptions.setUsePlanetaryModifiers(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useUnofficialMaintenance")) {
                    campaignOptions.setUseUnofficialMaintenance(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("logMaintenance")) {
                    campaignOptions.setLogMaintenance(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("defaultMaintenanceTime")) {
                    campaignOptions.setDefaultMaintenanceTime(Integer.parseInt(nodeContents));

                    // region Mass Repair / Mass Salvage
                } else if (nodeName.equalsIgnoreCase("mrmsUseRepair")) {
                    campaignOptions.setMRMSUseRepair(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("mrmsUseSalvage")) {
                    campaignOptions.setMRMSUseSalvage(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("mrmsUseExtraTime")) {
                    campaignOptions.setMRMSUseExtraTime(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("mrmsUseRushJob")) {
                    campaignOptions.setMRMSUseRushJob(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("mrmsAllowCarryover")) {
                    campaignOptions.setMRMSAllowCarryover(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("mrmsOptimizeToCompleteToday")) {
                    campaignOptions.setMRMSOptimizeToCompleteToday(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("mrmsScrapImpossible")) {
                    campaignOptions.setMRMSScrapImpossible(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("mrmsUseAssignedTechsFirst")) {
                    campaignOptions.setMRMSUseAssignedTechsFirst(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("mrmsReplacePod")) {
                    campaignOptions.setMRMSReplacePod(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("mrmsOptions")) {
                    campaignOptions.setMRMSOptions(MRMSOption.parseListFromXML(childNode, version));
                    // endregion Mass Repair / Mass Salvage
                    // endregion Repair and Maintenance Tab

                } else if (nodeName.equalsIgnoreCase("useFactionForNames")) {
                    campaignOptions.setUseOriginFactionForNames(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useEraMods")) {
                    campaignOptions.setEraMods(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("assignedTechFirst")) {
                    campaignOptions.setAssignedTechFirst(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("resetToFirstTech")) {
                    campaignOptions.setResetToFirstTech(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("techsUseAdministration")) {
                    campaignOptions.setTechsUseAdministration(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useQuirks")) {
                    campaignOptions.setQuirks(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("xpCostMultiplier")) {
                    campaignOptions.setXpCostMultiplier(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("scenarioXP")) {
                    campaignOptions.setScenarioXP(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("killsForXP")) {
                    campaignOptions.setKillsForXP(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("killXPAward")) {
                    campaignOptions.setKillXPAward(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("nTasksXP")) {
                    campaignOptions.setNTasksXP(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("tasksXP")) {
                    campaignOptions.setTaskXP(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("successXP")) {
                    campaignOptions.setSuccessXP(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("mistakeXP")) {
                    campaignOptions.setMistakeXP(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("vocationalXP")) {
                    campaignOptions.setVocationalXP(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("vocationalXPTargetNumber")) {
                    campaignOptions.setVocationalXPTargetNumber(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("vocationalXPCheckFrequency")) {
                    campaignOptions.setVocationalXPCheckFrequency(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("contractNegotiationXP")) {
                    campaignOptions.setContractNegotiationXP(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("adminWeeklyXP")) {
                    campaignOptions.setAdminXP(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("adminXPPeriod")) {
                    campaignOptions.setAdminXPPeriod(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("missionXpFail")) {
                    campaignOptions.setMissionXpFail(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("missionXpSuccess")) {
                    campaignOptions.setMissionXpSuccess(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("missionXpOutstandingSuccess")) {
                    campaignOptions.setMissionXpOutstandingSuccess(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("edgeCost")) {
                    campaignOptions.setEdgeCost(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("waitingPeriod")) {
                    campaignOptions.setWaitingPeriod(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("acquisitionSkill")) {
                    campaignOptions.setAcquisitionSkill(nodeContents);
                } else if (nodeName.equalsIgnoreCase("unitTransitTime")) {
                    campaignOptions.setUnitTransitTime(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("clanAcquisitionPenalty")) {
                    campaignOptions.setClanAcquisitionPenalty(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("isAcquisitionPenalty")) {
                    campaignOptions.setIsAcquisitionPenalty(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("usePlanetaryAcquisition")) {
                    campaignOptions.setPlanetaryAcquisition(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("planetAcquisitionFactionLimit")) {
                    campaignOptions.setPlanetAcquisitionFactionLimit(PlanetaryAcquisitionFactionLimit.parseFromString(
                          nodeContents));
                } else if (nodeName.equalsIgnoreCase("planetAcquisitionNoClanCrossover")) {
                    campaignOptions.setDisallowPlanetAcquisitionClanCrossover(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("noClanPartsFromIS")) {
                    campaignOptions.setDisallowClanPartsFromIS(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("penaltyClanPartsFromIS")) {
                    campaignOptions.setPenaltyClanPartsFromIS(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("planetAcquisitionVerbose")) {
                    campaignOptions.setPlanetAcquisitionVerboseReporting(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("maxJumpsPlanetaryAcquisition")) {
                    campaignOptions.setMaxJumpsPlanetaryAcquisition(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("planetTechAcquisitionBonus")) {
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
                } else if (nodeName.equalsIgnoreCase("planetIndustryAcquisitionBonus")) {
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
                } else if (nodeName.equalsIgnoreCase("planetOutputAcquisitionBonus")) {
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
                } else if (nodeName.equalsIgnoreCase("equipmentContractPercent")) {
                    campaignOptions.setEquipmentContractPercent(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("dropShipContractPercent")) {
                    campaignOptions.setDropShipContractPercent(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("jumpShipContractPercent")) {
                    campaignOptions.setJumpShipContractPercent(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("warShipContractPercent")) {
                    campaignOptions.setWarShipContractPercent(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("equipmentContractBase")) {
                    campaignOptions.setEquipmentContractBase(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("equipmentContractSaleValue")) {
                    campaignOptions.setEquipmentContractSaleValue(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("blcSaleValue")) {
                    campaignOptions.setBLCSaleValue(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("overageRepaymentInFinalPayment")) {
                    campaignOptions.setOverageRepaymentInFinalPayment(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("acquisitionSupportStaffOnly")) {
                    campaignOptions.setAcquisitionPersonnelCategory(Boolean.parseBoolean(nodeContents) ? SUPPORT : ALL);
                } else if (nodeName.equalsIgnoreCase("acquisitionPersonnelCategory")) {
                    campaignOptions.setAcquisitionPersonnelCategory(ProcurementPersonnelPick.fromString(nodeContents));
                } else if (nodeName.equalsIgnoreCase("limitByYear")) {
                    campaignOptions.setLimitByYear(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("disallowExtinctStuff")) {
                    campaignOptions.setDisallowExtinctStuff(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("allowClanPurchases")) {
                    campaignOptions.setAllowClanPurchases(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("allowISPurchases")) {
                    campaignOptions.setAllowISPurchases(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("allowCanonOnly")) {
                    campaignOptions.setAllowCanonOnly(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("allowCanonRefitOnly")) {
                    campaignOptions.setAllowCanonRefitOnly(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useAmmoByType")) {
                    campaignOptions.setUseAmmoByType(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("variableTechLevel")) {
                    campaignOptions.setVariableTechLevel(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("factionIntroDate")) {
                    campaignOptions.setIsUseFactionIntroDate(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("techLevel")) {
                    campaignOptions.setTechLevel(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("unitRatingMethod") ||
                                 nodeName.equalsIgnoreCase("dragoonsRatingMethod")) {
                    campaignOptions.setUnitRatingMethod(UnitRatingMethod.parseFromString(nodeContents));
                } else if (nodeName.equalsIgnoreCase("manualUnitRatingModifier")) {
                    campaignOptions.setManualUnitRatingModifier(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("clampReputationPayMultiplier")) {
                    campaignOptions.setClampReputationPayMultiplier(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("reduceReputationPerformanceModifier")) {
                    campaignOptions.setReduceReputationPerformanceModifier(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("reputationPerformanceModifierCutOff")) {
                    campaignOptions.setReputationPerformanceModifierCutOff(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("usePortraitForType")) {
                    String[] values = nodeContents.split(",");
                    for (int i = 0; i < values.length; i++) {
                        campaignOptions.setUsePortraitForRole(i, Boolean.parseBoolean(values[i].trim()));
                    }
                } else if (nodeName.equalsIgnoreCase("assignPortraitOnRoleChange")) {
                    campaignOptions.setAssignPortraitOnRoleChange(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("allowDuplicatePortraits")) {
                    campaignOptions.setAllowDuplicatePortraits(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("destroyByMargin")) {
                    campaignOptions.setDestroyByMargin(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("destroyMargin")) {
                    campaignOptions.setDestroyMargin(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("destroyPartTarget")) {
                    campaignOptions.setDestroyPartTarget(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useAeroSystemHits")) {
                    campaignOptions.setUseAeroSystemHits(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("maxAcquisitions")) {
                    campaignOptions.setMaxAcquisitions(Integer.parseInt(nodeContents));

                    // autoLogistics
                } else if (nodeName.equalsIgnoreCase("autoLogisticsHeatSink")) {
                    campaignOptions.setAutoLogisticsHeatSink(MathUtility.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoLogisticsMekHead")) {
                    campaignOptions.setAutoLogisticsMekHead(MathUtility.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoLogisticsMekLocation")) {
                    campaignOptions.setAutoLogisticsMekLocation(MathUtility.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoLogisticsNonRepairableLocation")) {
                    campaignOptions.setAutoLogisticsNonRepairableLocation(MathUtility.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoLogisticsArmor")) {
                    campaignOptions.setAutoLogisticsArmor(MathUtility.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoLogisticsAmmunition")) {
                    campaignOptions.setAutoLogisticsAmmunition(MathUtility.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoLogisticsActuators")) {
                    campaignOptions.setAutoLogisticsActuators(MathUtility.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoLogisticsJumpJets")) {
                    campaignOptions.setAutoLogisticsJumpJets(MathUtility.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoLogisticsEngines")) {
                    campaignOptions.setAutoLogisticsEngines(MathUtility.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoLogisticsWeapons")) {
                    campaignOptions.setAutoLogisticsWeapons(MathUtility.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoLogisticsOther")) {
                    campaignOptions.setAutoLogisticsOther(MathUtility.parseInt(nodeContents));

                    // region Personnel Tab
                    // region General Personnel
                } else if (nodeName.equalsIgnoreCase("useTactics")) {
                    campaignOptions.setUseTactics(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useInitiativeBonus")) {
                    campaignOptions.setUseInitiativeBonus(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useToughness")) {
                    campaignOptions.setUseToughness(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomToughness")) {
                    campaignOptions.setUseRandomToughness(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useArtillery")) {
                    campaignOptions.setUseArtillery(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useAbilities")) {
                    campaignOptions.setUseAbilities(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useCommanderAbilitiesOnly")) {
                    campaignOptions.setUseCommanderAbilitiesOnly(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useEdge")) {
                    campaignOptions.setUseEdge(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useSupportEdge")) {
                    campaignOptions.setUseSupportEdge(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useImplants")) {
                    campaignOptions.setUseImplants(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("alternativeQualityAveraging")) {
                    campaignOptions.setAlternativeQualityAveraging(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useAgeEffects")) {
                    campaignOptions.setUseAgeEffects(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useTransfers")) {
                    campaignOptions.setUseTransfers(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useExtendedTOEForceName")) {
                    campaignOptions.setUseExtendedTOEForceName(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("personnelLogSkillGain")) {
                    campaignOptions.setPersonnelLogSkillGain(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("personnelLogAbilityGain")) {
                    campaignOptions.setPersonnelLogAbilityGain(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("personnelLogEdgeGain")) {
                    campaignOptions.setPersonnelLogEdgeGain(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("displayPersonnelLog")) {
                    campaignOptions.setDisplayPersonnelLog(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("displayScenarioLog")) {
                    campaignOptions.setDisplayScenarioLog(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("displayKillRecord")) {
                    campaignOptions.setDisplayKillRecord(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("displayMedicalRecord")) {
                    campaignOptions.setDisplayMedicalRecord(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("displayAssignmentRecord")) {
                    campaignOptions.setDisplayAssignmentRecord(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("displayPerformanceRecord")) {
                    campaignOptions.setDisplayPerformanceRecord(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("rewardComingOfAgeAbilities")) {
                    campaignOptions.setRewardComingOfAgeAbilities(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("rewardComingOfAgeRPSkills")) {
                    campaignOptions.setRewardComingOfAgeRPSkills(Boolean.parseBoolean(nodeContents));
                    // endregion General Personnel

                    // region Expanded Personnel Information
                } else if (nodeName.equalsIgnoreCase("useTimeInService")) {
                    campaignOptions.setUseTimeInService(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("timeInServiceDisplayFormat")) {
                    campaignOptions.setTimeInServiceDisplayFormat(TimeInDisplayFormat.valueOf(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useTimeInRank")) {
                    campaignOptions.setUseTimeInRank(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("timeInRankDisplayFormat")) {
                    campaignOptions.setTimeInRankDisplayFormat(TimeInDisplayFormat.valueOf(nodeContents));
                } else if (nodeName.equalsIgnoreCase("trackTotalEarnings")) {
                    campaignOptions.setTrackTotalEarnings(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("trackTotalXPEarnings")) {
                    campaignOptions.setTrackTotalXPEarnings(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("showOriginFaction")) {
                    campaignOptions.setShowOriginFaction(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("adminsHaveNegotiation")) {
                    campaignOptions.setAdminsHaveNegotiation(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("adminExperienceLevelIncludeNegotiation")) {
                    campaignOptions.setAdminExperienceLevelIncludeNegotiation(Boolean.parseBoolean(nodeContents));
                    // endregion Expanded Personnel Information

                    // region Medical
                } else if (nodeName.equalsIgnoreCase("useAdvancedMedical")) {
                    campaignOptions.setUseAdvancedMedical(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("healWaitingPeriod")) {
                    campaignOptions.setHealingWaitingPeriod(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("naturalHealingWaitingPeriod")) {
                    campaignOptions.setNaturalHealingWaitingPeriod(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("minimumHitsForVehicles")) {
                    campaignOptions.setMinimumHitsForVehicles(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomHitsForVehicles")) {
                    campaignOptions.setUseRandomHitsForVehicles(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("tougherHealing")) {
                    campaignOptions.setTougherHealing(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("maximumPatients")) {
                    campaignOptions.setMaximumPatients(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("doctorsUseAdministration")) {
                    campaignOptions.setDoctorsUseAdministration(Boolean.parseBoolean(nodeContents));
                    // endregion Medical

                    // region Prisoners
                } else if (nodeName.equalsIgnoreCase("prisonerCaptureStyle")) {
                    campaignOptions.setPrisonerCaptureStyle(PrisonerCaptureStyle.fromString(nodeContents));
                    // endregion Prisoners

                    // region Dependent
                } else if (nodeName.equalsIgnoreCase("useRandomDependentAddition")) {
                    campaignOptions.setUseRandomDependentAddition(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomDependentRemoval")) {
                    campaignOptions.setUseRandomDependentRemoval(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("dependentProfessionDieSize")) {
                    campaignOptions.setDependentProfessionDieSize(MathUtility.parseInt(nodeContents, 4));
                } else if (nodeName.equalsIgnoreCase("civilianProfessionDieSize")) {
                    campaignOptions.setCivilianProfessionDieSize(MathUtility.parseInt(nodeContents, 2));
                    // endregion Dependent

                    // region Personnel Removal
                } else if (nodeName.equalsIgnoreCase("usePersonnelRemoval")) {
                    campaignOptions.setUsePersonnelRemoval(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRemovalExemptCemetery")) {
                    campaignOptions.setUseRemovalExemptCemetery(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRemovalExemptRetirees")) {
                    campaignOptions.setUseRemovalExemptRetirees(Boolean.parseBoolean(nodeContents));
                    // endregion Personnel Removal

                    // region Salary
                } else if (nodeName.equalsIgnoreCase("disableSecondaryRoleSalary")) {
                    campaignOptions.setDisableSecondaryRoleSalary(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("salaryAntiMekMultiplier")) {
                    campaignOptions.setSalaryAntiMekMultiplier(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("salarySpecialistInfantryMultiplier")) {
                    campaignOptions.setSalarySpecialistInfantryMultiplier(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("salaryXPMultipliers")) {
                    if (!childNode.hasChildNodes()) {
                        continue;
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
                } else if (nodeName.equalsIgnoreCase("salaryTypeBase")) {
                    Money[] defaultSalaries = campaignOptions.getRoleBaseSalaries();
                    Money[] newSalaries = Utilities.readMoneyArray(childNode);

                    Money[] mergedSalaries = new Money[PersonnelRole.values().length];
                    for (int i = 0; i < mergedSalaries.length; i++) {
                        try {
                            mergedSalaries[i] = (newSalaries[i] != null) ? newSalaries[i] : defaultSalaries[i];
                        } catch (Exception e) {
                            // This will happen if we ever add a new profession, as it will exceed the entries in
                            // the child node
                            mergedSalaries[i] = defaultSalaries[i];
                        }
                    }

                    campaignOptions.setRoleBaseSalaries(mergedSalaries);
                    // endregion Salary

                    // region Awards
                } else if (nodeName.equalsIgnoreCase("awardBonusStyle")) {
                    campaignOptions.setAwardBonusStyle(AwardBonus.valueOf(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableAutoAwards")) {
                    campaignOptions.setEnableAutoAwards(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("issuePosthumousAwards")) {
                    campaignOptions.setIssuePosthumousAwards(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("issueBestAwardOnly")) {
                    campaignOptions.setIssueBestAwardOnly(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("ignoreStandardSet")) {
                    campaignOptions.setIgnoreStandardSet(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("awardTierSize")) {
                    campaignOptions.setAwardTierSize(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableContractAwards")) {
                    campaignOptions.setEnableContractAwards(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableFactionHunterAwards")) {
                    campaignOptions.setEnableFactionHunterAwards(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableInjuryAwards")) {
                    campaignOptions.setEnableInjuryAwards(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableIndividualKillAwards")) {
                    campaignOptions.setEnableIndividualKillAwards(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableFormationKillAwards")) {
                    campaignOptions.setEnableFormationKillAwards(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableRankAwards")) {
                    campaignOptions.setEnableRankAwards(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableScenarioAwards")) {
                    campaignOptions.setEnableScenarioAwards(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableSkillAwards")) {
                    campaignOptions.setEnableSkillAwards(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableTheatreOfWarAwards")) {
                    campaignOptions.setEnableTheatreOfWarAwards(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableTimeAwards")) {
                    campaignOptions.setEnableTimeAwards(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableTrainingAwards")) {
                    campaignOptions.setEnableTrainingAwards(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableMiscAwards")) {
                    campaignOptions.setEnableMiscAwards(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("awardSetFilterList")) {
                    campaignOptions.setAwardSetFilterList(nodeContents);
                    // endregion Awards
                    // endregion Personnel Tab

                    // region Life Paths Tab
                    // region Personnel Randomization
                } else if (nodeName.equalsIgnoreCase("useDylansRandomXP")) {
                    campaignOptions.setUseDylansRandomXP(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("nonBinaryDiceSize")) {
                    campaignOptions.setNonBinaryDiceSize(Integer.parseInt(nodeContents));
                    // endregion Personnel Randomization

                    // region Random Histories
                } else if (nodeName.equalsIgnoreCase("randomOriginOptions")) {
                    if (!childNode.hasChildNodes()) {
                        continue;
                    }
                    final RandomOriginOptions randomOriginOptions = RandomOriginOptions.parseFromXML(childNode.getChildNodes(),
                          true);
                    if (randomOriginOptions == null) {
                        continue;
                    }
                    campaignOptions.setRandomOriginOptions(randomOriginOptions);
                } else if (nodeName.equalsIgnoreCase("useRandomPersonalities")) {
                    campaignOptions.setUseRandomPersonalities(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomPersonalityReputation")) {
                    campaignOptions.setUseRandomPersonalityReputation(Boolean.parseBoolean(nodeContents));
                } else if ((nodeName.equalsIgnoreCase("useReasoningXpMultiplier"))) {
                    campaignOptions.setUseReasoningXpMultiplier(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useSimulatedRelationships")) {
                    campaignOptions.setUseSimulatedRelationships(Boolean.parseBoolean(nodeContents));
                    // endregion Random Histories

                    // region Family
                } else if (nodeName.equalsIgnoreCase("familyDisplayLevel")) {
                    campaignOptions.setFamilyDisplayLevel(FamilialRelationshipDisplayLevel.parseFromString(nodeContents));
                    // endregion Family

                    // region anniversaries
                } else if (nodeName.equalsIgnoreCase("announceBirthdays")) {
                    campaignOptions.setAnnounceBirthdays(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("announceRecruitmentAnniversaries")) {
                    campaignOptions.setAnnounceRecruitmentAnniversaries(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("announceOfficersOnly")) {
                    campaignOptions.setAnnounceOfficersOnly(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("announceChildBirthdays")) {
                    campaignOptions.setAnnounceChildBirthdays(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("showLifeEventDialogBirths")) {
                    campaignOptions.setShowLifeEventDialogBirths(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("showLifeEventDialogComingOfAge")) {
                    campaignOptions.setShowLifeEventDialogComingOfAge(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("showLifeEventDialogCelebrations")) {
                    campaignOptions.setShowLifeEventDialogCelebrations(Boolean.parseBoolean(nodeContents));
                    // endregion anniversaries

                    // region Marriage
                } else if (nodeName.equalsIgnoreCase("useManualMarriages")) {
                    campaignOptions.setUseManualMarriages(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useClanPersonnelMarriages")) {
                    campaignOptions.setUseClanPersonnelMarriages(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("usePrisonerMarriages")) {
                    campaignOptions.setUsePrisonerMarriages(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("checkMutualAncestorsDepth")) {
                    campaignOptions.setCheckMutualAncestorsDepth(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("noInterestInMarriageDiceSize")) {
                    campaignOptions.setNoInterestInMarriageDiceSize(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("logMarriageNameChanges")) {
                    campaignOptions.setLogMarriageNameChanges(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("marriageSurnameWeights")) {
                    if (!childNode.hasChildNodes()) {
                        continue;
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
                } else if (nodeName.equalsIgnoreCase("randomMarriageMethod")) {
                    campaignOptions.setRandomMarriageMethod(RandomMarriageMethod.fromString(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomClanPersonnelMarriages")) {
                    campaignOptions.setUseRandomClanPersonnelMarriages(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomPrisonerMarriages")) {
                    campaignOptions.setUseRandomPrisonerMarriages(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("randomMarriageAgeRange")) {
                    campaignOptions.setRandomMarriageAgeRange(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("randomMarriageDiceSize")) {
                    campaignOptions.setRandomMarriageDiceSize(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("randomSameSexMarriageDiceSize")) {
                    campaignOptions.setRandomSameSexMarriageDiceSize(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomSameSexMarriages")) { // Legacy, pre-50.01
                    if (!Boolean.parseBoolean(nodeContents)) {
                        campaignOptions.setRandomSameSexMarriageDiceSize(0);
                    }
                } else if (nodeName.equalsIgnoreCase("randomNewDependentMarriage")) {
                    campaignOptions.setRandomNewDependentMarriage(Integer.parseInt(nodeContents));
                    // endregion Marriage

                    // region Divorce
                } else if (nodeName.equalsIgnoreCase("useManualDivorce")) {
                    campaignOptions.setUseManualDivorce(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useClanPersonnelDivorce")) {
                    campaignOptions.setUseClanPersonnelDivorce(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("usePrisonerDivorce")) {
                    campaignOptions.setUsePrisonerDivorce(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("divorceSurnameWeights")) {
                    if (!childNode.hasChildNodes()) {
                        continue;
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
                } else if (nodeName.equalsIgnoreCase("randomDivorceMethod")) {
                    campaignOptions.setRandomDivorceMethod(RandomDivorceMethod.fromString(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomOppositeSexDivorce")) {
                    campaignOptions.setUseRandomOppositeSexDivorce(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomSameSexDivorce")) {
                    campaignOptions.setUseRandomSameSexDivorce(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomClanPersonnelDivorce")) {
                    campaignOptions.setUseRandomClanPersonnelDivorce(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomPrisonerDivorce")) {
                    campaignOptions.setUseRandomPrisonerDivorce(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("randomDivorceDiceSize")) {
                    campaignOptions.setRandomDivorceDiceSize(Integer.parseInt(nodeContents));
                    // endregion Divorce

                    // region Procreation
                } else if (nodeName.equalsIgnoreCase("useManualProcreation")) {
                    campaignOptions.setUseManualProcreation(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useClanPersonnelProcreation")) {
                    campaignOptions.setUseClanPersonnelProcreation(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("usePrisonerProcreation")) {
                    campaignOptions.setUsePrisonerProcreation(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("multiplePregnancyOccurrences")) {
                    campaignOptions.setMultiplePregnancyOccurrences(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("babySurnameStyle")) {
                    campaignOptions.setBabySurnameStyle(BabySurnameStyle.parseFromString(nodeContents));
                } else if (nodeName.equalsIgnoreCase("assignNonPrisonerBabiesFounderTag")) {
                    campaignOptions.setAssignNonPrisonerBabiesFounderTag(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("assignChildrenOfFoundersFounderTag")) {
                    campaignOptions.setAssignChildrenOfFoundersFounderTag(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useMaternityLeave")) {
                    campaignOptions.setUseMaternityLeave(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("determineFatherAtBirth")) {
                    campaignOptions.setDetermineFatherAtBirth(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("displayTrueDueDate")) {
                    campaignOptions.setDisplayTrueDueDate(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("noInterestInChildrenDiceSize")) {
                    campaignOptions.setNoInterestInChildrenDiceSize(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("logProcreation")) {
                    campaignOptions.setLogProcreation(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("randomProcreationMethod")) {
                    campaignOptions.setRandomProcreationMethod(RandomProcreationMethod.fromString(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRelationshiplessRandomProcreation")) {
                    campaignOptions.setUseRelationshiplessRandomProcreation(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomClanPersonnelProcreation")) {
                    campaignOptions.setUseRandomClanPersonnelProcreation(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomPrisonerProcreation")) {
                    campaignOptions.setUseRandomPrisonerProcreation(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("randomProcreationRelationshipDiceSize")) {
                    campaignOptions.setRandomProcreationRelationshipDiceSize(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("randomProcreationRelationshiplessDiceSize")) {
                    campaignOptions.setRandomProcreationRelationshiplessDiceSize(Integer.parseInt(nodeContents));
                    // endregion Procreation

                    // region Education
                } else if (nodeName.equalsIgnoreCase("useEducationModule")) {
                    campaignOptions.setUseEducationModule(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("curriculumXpRate")) {
                    campaignOptions.setCurriculumXpRate(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("maximumJumpCount")) {
                    campaignOptions.setMaximumJumpCount(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useReeducationCamps")) {
                    campaignOptions.setUseReeducationCamps(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableLocalAcademies")) {
                    campaignOptions.setEnableLocalAcademies(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enablePrestigiousAcademies")) {
                    campaignOptions.setEnablePrestigiousAcademies(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableUnitEducation")) {
                    campaignOptions.setEnableUnitEducation(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableOverrideRequirements")) {
                    campaignOptions.setEnableOverrideRequirements(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableShowIneligibleAcademies")) {
                    campaignOptions.setEnableShowIneligibleAcademies(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("entranceExamBaseTargetNumber")) {
                    campaignOptions.setEntranceExamBaseTargetNumber(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("facultyXpRate")) {
                    campaignOptions.setFacultyXpRate(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("enableBonuses")) {
                    campaignOptions.setEnableBonuses(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("adultDropoutChance")) {
                    campaignOptions.setAdultDropoutChance(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("childrenDropoutChance")) {
                    campaignOptions.setChildrenDropoutChance(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("allAges")) {
                    campaignOptions.setAllAges(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("militaryAcademyAccidents")) {
                    campaignOptions.setMilitaryAcademyAccidents(Integer.parseInt(nodeContents));
                    // endregion Education
                } else if (nodeName.equalsIgnoreCase("enabledRandomDeathAgeGroups")) {
                    if (!childNode.hasChildNodes()) {
                        continue;
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
                } else if (nodeName.equalsIgnoreCase("useRandomDeathSuicideCause")) {
                    campaignOptions.setUseRandomDeathSuicideCause(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("randomDeathMultiplier")) {
                    campaignOptions.setRandomDeathMultiplier(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomRetirement")) {
                    campaignOptions.setUseRandomRetirement(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("turnoverBaseTn")) {
                    campaignOptions.setTurnoverFixedTargetNumber(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("turnoverFrequency")) {
                    campaignOptions.setTurnoverFrequency(TurnoverFrequency.valueOf(nodeContents));
                } else if (nodeName.equalsIgnoreCase("trackOriginalUnit")) {
                    campaignOptions.setTrackOriginalUnit(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("aeroRecruitsHaveUnits")) {
                    campaignOptions.setAeroRecruitsHaveUnits(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useContractCompletionRandomRetirement")) {
                    campaignOptions.setUseContractCompletionRandomRetirement(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomFounderTurnover")) {
                    campaignOptions.setUseRandomFounderTurnover(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFounderRetirement")) {
                    campaignOptions.setUseFounderRetirement(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useSubContractSoldiers")) {
                    campaignOptions.setUseSubContractSoldiers(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("serviceContractDuration")) {
                    campaignOptions.setServiceContractDuration(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("serviceContractModifier")) {
                    campaignOptions.setServiceContractModifier(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payBonusDefault")) {
                    campaignOptions.setPayBonusDefault(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payBonusDefaultThreshold")) {
                    campaignOptions.setPayBonusDefaultThreshold(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useCustomRetirementModifiers")) {
                    campaignOptions.setUseCustomRetirementModifiers(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFatigueModifiers")) {
                    campaignOptions.setUseFatigueModifiers(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useSkillModifiers")) {
                    campaignOptions.setUseSkillModifiers(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useAgeModifiers")) {
                    campaignOptions.setUseAgeModifiers(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useUnitRatingModifiers")) {
                    campaignOptions.setUseUnitRatingModifiers(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFactionModifiers")) {
                    campaignOptions.setUseFactionModifiers(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useMissionStatusModifiers")) {
                    campaignOptions.setUseMissionStatusModifiers(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useHostileTerritoryModifiers")) {
                    campaignOptions.setUseHostileTerritoryModifiers(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFamilyModifiers")) {
                    campaignOptions.setUseFamilyModifiers(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useLoyaltyModifiers")) {
                    campaignOptions.setUseLoyaltyModifiers(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useHideLoyalty")) {
                    campaignOptions.setUseHideLoyalty(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payoutRateOfficer")) {
                    campaignOptions.setPayoutRateOfficer(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payoutRateEnlisted")) {
                    campaignOptions.setPayoutRateEnlisted(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payoutRetirementMultiplier")) {
                    campaignOptions.setPayoutRetirementMultiplier(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("usePayoutServiceBonus")) {
                    campaignOptions.setUsePayoutServiceBonus(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payoutServiceBonusRate")) {
                    campaignOptions.setPayoutServiceBonusRate(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("UseHRStrain")) {
                    campaignOptions.setUseHRStrain(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("hrStrain")) {
                    campaignOptions.setHRCapacity(MathUtility.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("administrativeStrain")) { // Legacy <50.07
                    campaignOptions.setHRCapacity(MathUtility.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useManagementSkill")) {
                    campaignOptions.setUseManagementSkill(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useCommanderLeadershipOnly")) {
                    campaignOptions.setUseCommanderLeadershipOnly(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("managementSkillPenalty")) {
                    campaignOptions.setManagementSkillPenalty(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFatigue")) {
                    campaignOptions.setUseFatigue(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("fatigueRate")) {
                    campaignOptions.setFatigueRate(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useInjuryFatigue")) {
                    campaignOptions.setUseInjuryFatigue(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("fieldKitchenCapacity")) {
                    campaignOptions.setFieldKitchenCapacity(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("fieldKitchenIgnoreNonCombatants")) {
                    campaignOptions.setFieldKitchenIgnoreNonCombatants(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("fatigueLeaveThreshold")) {
                    campaignOptions.setFatigueLeaveThreshold(Integer.parseInt(nodeContents));
                    // endregion Turnover and Retention

                    // region Finances Tab
                } else if (nodeName.equalsIgnoreCase("payForParts")) {
                    campaignOptions.setPayForParts(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payForRepairs")) {
                    campaignOptions.setPayForRepairs(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payForUnits")) {
                    campaignOptions.setPayForUnits(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payForSalaries")) {
                    campaignOptions.setPayForSalaries(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payForOverhead")) {
                    campaignOptions.setPayForOverhead(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payForMaintain")) {
                    campaignOptions.setPayForMaintain(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payForTransport")) {
                    campaignOptions.setPayForTransport(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("sellUnits")) {
                    campaignOptions.setSellUnits(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("sellParts")) {
                    campaignOptions.setSellParts(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payForRecruitment")) {
                    campaignOptions.setPayForRecruitment(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payForFood")) {
                    campaignOptions.setPayForFood(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("payForHousing")) {
                    campaignOptions.setPayForHousing(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useLoanLimits")) {
                    campaignOptions.setLoanLimits(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("usePercentageMaint")) {
                    campaignOptions.setUsePercentageMaint(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("infantryDontCount")) {
                    campaignOptions.setUseInfantryDontCount(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("usePeacetimeCost")) {
                    campaignOptions.setUsePeacetimeCost(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useExtendedPartsModifier")) {
                    campaignOptions.setUseExtendedPartsModifier(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("showPeacetimeCost")) {
                    campaignOptions.setShowPeacetimeCost(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("newFinancialYearFinancesToCSVExport")) {
                    campaignOptions.setNewFinancialYearFinancesToCSVExport(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("financialYearDuration")) {
                    campaignOptions.setFinancialYearDuration(FinancialYearDuration.parseFromString(nodeContents));
                } else if (nodeName.equalsIgnoreCase("simulateGrayMonday")) {
                    campaignOptions.setSimulateGrayMonday(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("allowMonthlyReinvestment")) {
                    campaignOptions.setAllowMonthlyReinvestment(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("allowMonthlyConnections")) {
                    campaignOptions.setAllowMonthlyConnections(Boolean.parseBoolean(nodeContents));

                    // region Price Multipliers
                } else if (nodeName.equalsIgnoreCase("commonPartPriceMultiplier")) {
                    campaignOptions.setCommonPartPriceMultiplier(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("innerSphereUnitPriceMultiplier")) {
                    campaignOptions.setInnerSphereUnitPriceMultiplier(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("innerSpherePartPriceMultiplier")) {
                    campaignOptions.setInnerSpherePartPriceMultiplier(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("clanUnitPriceMultiplier")) {
                    campaignOptions.setClanUnitPriceMultiplier(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("clanPartPriceMultiplier")) {
                    campaignOptions.setClanPartPriceMultiplier(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("mixedTechUnitPriceMultiplier")) {
                    campaignOptions.setMixedTechUnitPriceMultiplier(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("usedPartPriceMultipliers")) {
                    final String[] values = nodeContents.split(",");
                    for (int i = 0; i < values.length; i++) {
                        try {
                            campaignOptions.getUsedPartPriceMultipliers()[i] = Double.parseDouble(values[i]);
                        } catch (Exception ignored) {

                        }
                    }
                } else if (nodeName.equalsIgnoreCase("damagedPartsValueMultiplier")) {
                    campaignOptions.setDamagedPartsValueMultiplier(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("unrepairablePartsValueMultiplier")) {
                    campaignOptions.setUnrepairablePartsValueMultiplier(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("cancelledOrderRefundMultiplier")) {
                    campaignOptions.setCancelledOrderRefundMultiplier(Double.parseDouble(nodeContents));
                    // endregion Price Multipliers

                    // region Taxes
                } else if (nodeName.equalsIgnoreCase("useTaxes")) {
                    campaignOptions.setUseTaxes(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("taxesPercentage")) {
                    campaignOptions.setTaxesPercentage(Integer.parseInt(nodeContents));
                    // endregion Taxes
                    // endregion Finances Tab

                    // Shares
                } else if (nodeName.equalsIgnoreCase("useShareSystem")) {
                    campaignOptions.setUseShareSystem(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("sharesForAll")) {
                    campaignOptions.setSharesForAll(Boolean.parseBoolean(nodeContents));
                    // endregion Price Multipliers
                    // endregion Finances Tab

                    // region Markets Tab
                    // region Personnel Market
                } else if (nodeName.equalsIgnoreCase("personnelMarketStyle")) {
                    campaignOptions.setPersonnelMarketStyle(PersonnelMarketStyle.fromString(nodeContents));
                } else if (nodeName.equalsIgnoreCase("personnelMarketName")) {
                    String marketName = nodeContents;
                    // Backwards compatibility with saves from before these rules moved to Camops
                    if (marketName.equals("Strat Ops")) {
                        marketName = "Campaign Ops";
                    }
                    campaignOptions.setPersonnelMarketName(marketName);
                } else if (nodeName.equalsIgnoreCase("personnelMarketReportRefresh")) {
                    campaignOptions.setPersonnelMarketReportRefresh(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("personnelMarketRandomRemovalTargets")) {
                    if (!childNode.hasChildNodes()) {
                        continue;
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
                } else if (nodeName.equalsIgnoreCase("personnelMarketDylansWeight")) {
                    campaignOptions.setPersonnelMarketDylansWeight(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("usePersonnelHireHiringHallOnly")) {
                    campaignOptions.setUsePersonnelHireHiringHallOnly(Boolean.parseBoolean(nodeContents));
                    // endregion Personnel Market

                    // region Unit Market
                } else if (nodeName.equalsIgnoreCase("unitMarketMethod")) {
                    campaignOptions.setUnitMarketMethod(UnitMarketMethod.valueOf(nodeContents));
                } else if (nodeName.equalsIgnoreCase("unitMarketRegionalMekVariations")) {
                    campaignOptions.setUnitMarketRegionalMekVariations(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("unitMarketSpecialUnitChance")) {
                    campaignOptions.setUnitMarketSpecialUnitChance(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("unitMarketRarityModifier")) {
                    campaignOptions.setUnitMarketRarityModifier(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("instantUnitMarketDelivery")) {
                    campaignOptions.setInstantUnitMarketDelivery(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("mothballUnitMarketDeliveries")) {
                    campaignOptions.setMothballUnitMarketDeliveries(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("unitMarketReportRefresh")) {
                    campaignOptions.setUnitMarketReportRefresh(Boolean.parseBoolean(nodeContents));
                    // endregion Unit Market

                    // region Contract Market
                } else if (nodeName.equalsIgnoreCase("contractMarketMethod")) {
                    campaignOptions.setContractMarketMethod(ContractMarketMethod.valueOf(nodeContents));
                } else if (nodeName.equalsIgnoreCase("contractSearchRadius")) {
                    campaignOptions.setContractSearchRadius(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("variableContractLength")) {
                    campaignOptions.setVariableContractLength(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useDynamicDifficulty")) {
                    campaignOptions.setUseDynamicDifficulty(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("contractMarketReportRefresh")) {
                    campaignOptions.setContractMarketReportRefresh(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("contractMaxSalvagePercentage")) {
                    campaignOptions.setContractMaxSalvagePercentage(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("dropShipBonusPercentage")) {
                    campaignOptions.setDropShipBonusPercentage(Integer.parseInt(nodeContents));
                    // endregion Contract Market
                    // endregion Markets Tab

                    // region RATs Tab
                } else if (nodeName.equals("useStaticRATs")) {
                    campaignOptions.setUseStaticRATs(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("rats")) {
                    campaignOptions.setRATs(MHQXMLUtility.unEscape(nodeContents).split(","));
                } else if (nodeName.equals("ignoreRATEra")) {
                    campaignOptions.setIgnoreRATEra(Boolean.parseBoolean(nodeContents));
                    // endregion RATs Tab

                    // region AtB Tab
                } else if (nodeName.equalsIgnoreCase("skillLevel")) {
                    campaignOptions.setSkillLevel(SkillLevel.parseFromString(nodeContents));
                    // region ACAR Tab
                } else if (nodeName.equalsIgnoreCase("autoResolveMethod")) {
                    campaignOptions.setAutoResolveMethod(AutoResolveMethod.valueOf(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoResolveVictoryChanceEnabled")) {
                    campaignOptions.setAutoResolveVictoryChanceEnabled(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoResolveNumberOfScenarios")) {
                    campaignOptions.setAutoResolveNumberOfScenarios(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoResolveUseExperimentalPacarGui")) {
                    campaignOptions.setAutoResolveExperimentalPacarGuiEnabled(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("strategicViewTheme")) {
                    campaignOptions.setStrategicViewTheme(nodeContents);
                    // endregion ACAR Tab
                    // endregion AtB Tab
                } else if (nodeName.equalsIgnoreCase("phenotypeProbabilities")) {
                    String[] values = nodeContents.split(",");
                    for (int i = 0; i < values.length; i++) {
                        campaignOptions.setPhenotypeProbability(i, Integer.parseInt(values[i]));
                    }
                } else if (nodeName.equalsIgnoreCase("useAtB")) {
                    campaignOptions.setUseAtB(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useStratCon")) {
                    campaignOptions.setUseStratCon(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useAero")) {
                    campaignOptions.setUseAero(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useVehicles")) {
                    campaignOptions.setUseVehicles(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("clanVehicles")) {
                    campaignOptions.setClanVehicles(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useGenericBattleValue")) {
                    campaignOptions.setUseGenericBattleValue(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useVerboseBidding")) {
                    campaignOptions.setUseVerboseBidding(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("doubleVehicles")) {
                    campaignOptions.setDoubleVehicles(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("adjustPlayerVehicles")) {
                    campaignOptions.setAdjustPlayerVehicles(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("opForLanceTypeMeks")) {
                    campaignOptions.setOpForLanceTypeMeks(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("opForLanceTypeMixed")) {
                    campaignOptions.setOpForLanceTypeMixed(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("opForLanceTypeVehicles")) {
                    campaignOptions.setOpForLanceTypeVehicles(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("opForUsesVTOLs")) {
                    campaignOptions.setOpForUsesVTOLs(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useDropShips")) {
                    campaignOptions.setUseDropShips(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("mercSizeLimited")) {
                    campaignOptions.setMercSizeLimited(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("regionalMekVariations")) {
                    campaignOptions.setRegionalMekVariations(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("attachedPlayerCamouflage")) {
                    campaignOptions.setAttachedPlayerCamouflage(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("playerControlsAttachedUnits")) {
                    campaignOptions.setPlayerControlsAttachedUnits(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("atbBattleChance")) {
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
                } else if (nodeName.equalsIgnoreCase("generateChases")) {
                    campaignOptions.setGenerateChases(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useWeatherConditions")) {
                    campaignOptions.setUseWeatherConditions(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useLightConditions")) {
                    campaignOptions.setUseLightConditions(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("usePlanetaryConditions")) {
                    campaignOptions.setUsePlanetaryConditions(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("restrictPartsByMission")) {
                    campaignOptions.setRestrictPartsByMission(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("allowOpForLocalUnits")) {
                    campaignOptions.setAllowOpForLocalUnits(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("allowOpForAeros")) {
                    campaignOptions.setAllowOpForAeros(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("opForAeroChance")) {
                    campaignOptions.setOpForAeroChance(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("opForLocalUnitChance")) {
                    campaignOptions.setOpForLocalUnitChance(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("fixedMapChance")) {
                    campaignOptions.setFixedMapChance(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("spaUpgradeIntensity")) {
                    campaignOptions.setSpaUpgradeIntensity(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("scenarioModMax")) {
                    campaignOptions.setScenarioModMax(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("scenarioModChance")) {
                    campaignOptions.setScenarioModChance(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("scenarioModBV")) {
                    campaignOptions.setScenarioModBV(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoConfigMunitions")) {
                    campaignOptions.setAutoConfigMunitions(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("autoGenerateOpForCallsigns")) {
                    campaignOptions.setAutoGenerateOpForCallsigns(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("minimumCallsignSkillLevel")) {
                    campaignOptions.setMinimumCallsignSkillLevel(SkillLevel.parseFromString(nodeContents));
                } else if (nodeName.equalsIgnoreCase("trackFactionStanding")) {
                    campaignOptions.setTrackFactionStanding(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFactionStandingNegotiation")) {
                    campaignOptions.setUseFactionStandingNegotiation(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFactionStandingResupply")) {
                    campaignOptions.setUseFactionStandingResupply(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFactionStandingCommandCircuit")) {
                    campaignOptions.setUseFactionStandingCommandCircuit(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFactionStandingOutlawed")) {
                    campaignOptions.setUseFactionStandingOutlawed(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFactionStandingBatchallRestrictions")) {
                    campaignOptions.setUseFactionStandingBatchallRestrictions(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFactionStandingRecruitment")) {
                    campaignOptions.setUseFactionStandingRecruitment(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFactionStandingBarracksCosts")) {
                    campaignOptions.setUseFactionStandingBarracksCosts(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFactionStandingUnitMarket")) {
                    campaignOptions.setUseFactionStandingUnitMarket(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFactionStandingContractPay")) {
                    campaignOptions.setUseFactionStandingContractPay(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useFactionStandingSupportPoints")) {
                    campaignOptions.setUseFactionStandingSupportPoints(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("factionStandingGainMultiplier")) {
                    campaignOptions.setRegardMultiplier(MathUtility.parseDouble(nodeContents, 1.0));

                    // region Legacy
                    // Removed in 0.49.*
                } else if (nodeName.equalsIgnoreCase("salaryXPMultiplier")) { // Legacy, 0.49.12 removal
                    String[] values = nodeContents.split(",");
                    for (int i = 0; i < values.length; i++) {
                        campaignOptions.getSalaryXPMultipliers()
                              .put(Skills.SKILL_LEVELS[i + 1], Double.parseDouble(values[i]));
                    }
                } else if (nodeName.equalsIgnoreCase("personnelMarketRandomEliteRemoval")) { // Legacy, 0.49.12
                    // removal
                    campaignOptions.getPersonnelMarketRandomRemovalTargets()
                          .put(SkillLevel.ELITE, Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("personnelMarketRandomVeteranRemoval")) { // Legacy,
                    // 0.49.12
                    // removal
                    campaignOptions.getPersonnelMarketRandomRemovalTargets()
                          .put(SkillLevel.VETERAN, Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("personnelMarketRandomRegularRemoval")) { // Legacy,
                    // 0.49.12
                    // removal
                    campaignOptions.getPersonnelMarketRandomRemovalTargets()
                          .put(SkillLevel.REGULAR, Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("personnelMarketRandomGreenRemoval")) { // Legacy, 0.49.12
                    // removal
                    campaignOptions.getPersonnelMarketRandomRemovalTargets()
                          .put(SkillLevel.GREEN, Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("personnelMarketRandomUltraGreenRemoval")) { // Legacy,
                    // 0.49.12
                    // removal
                    campaignOptions.getPersonnelMarketRandomRemovalTargets()
                          .put(SkillLevel.ULTRA_GREEN, Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("randomizeOrigin")) { // Legacy, 0.49.7 Removal
                    campaignOptions.getRandomOriginOptions().setRandomizeOrigin(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("randomizeDependentOrigin")) { // Legacy, 0.49.7 Removal
                    campaignOptions.getRandomOriginOptions()
                          .setRandomizeDependentOrigin(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("originSearchRadius")) { // Legacy, 0.49.7 Removal
                    campaignOptions.getRandomOriginOptions().setOriginSearchRadius(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("extraRandomOrigin")) { // Legacy, 0.49.7 Removal
                    campaignOptions.getRandomOriginOptions().setExtraRandomOrigin(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("originDistanceScale")) { // Legacy, 0.49.7 Removal
                    campaignOptions.getRandomOriginOptions().setOriginDistanceScale(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("dependentsNeverLeave")) { // Legacy - 0.49.7 Removal
                    campaignOptions.setUseRandomDependentRemoval(!Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("marriageAgeRange")) { // Legacy - 0.49.6 Removal
                    campaignOptions.setRandomMarriageAgeRange(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useRandomMarriages")) { // Legacy - 0.49.6 Removal
                    campaignOptions.setRandomMarriageMethod(Boolean.parseBoolean(nodeContents) ?
                                                                  RandomMarriageMethod.DICE_ROLL :
                                                                  RandomMarriageMethod.NONE);
                } else if (nodeName.equalsIgnoreCase("logMarriageNameChange")) { // Legacy - 0.49.6 Removal
                    campaignOptions.setLogMarriageNameChanges(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("randomMarriageSurnameWeights")) { // Legacy - 0.49.6
                    // Removal
                    final String[] values = nodeContents.split(",");
                    if (values.length == 13) {
                        final MergingSurnameStyle[] marriageSurnameStyles = MergingSurnameStyle.values();
                        for (int i = 0; i < values.length; i++) {
                            campaignOptions.getMarriageSurnameWeights()
                                  .put(marriageSurnameStyles[i], Integer.parseInt(values[i]));
                        }
                    } else if (values.length == 9) {
                        migrateMarriageSurnameWeights(campaignOptions, values);
                    } else {
                        LOGGER.error("Unknown length of randomMarriageSurnameWeights");
                    }
                } else if (nodeName.equalsIgnoreCase("logConception")) { // Legacy - 0.49.4 Removal
                    campaignOptions.setLogProcreation(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("staticRATs")) { // Legacy - 0.49.4 Removal
                    campaignOptions.setUseStaticRATs(true);
                } else if (nodeName.equalsIgnoreCase("ignoreRatEra")) { // Legacy - 0.49.4 Removal
                    campaignOptions.setIgnoreRATEra(true);
                } else if (nodeName.equalsIgnoreCase("clanPriceModifier")) { // Legacy - 0.49.3 Removal
                    final double value = Double.parseDouble(nodeContents);
                    campaignOptions.setClanUnitPriceMultiplier(value);
                    campaignOptions.setClanPartPriceMultiplier(value);
                } else if (nodeName.equalsIgnoreCase("usedPartsValueA")) { // Legacy - 0.49.3 Removal
                    campaignOptions.getUsedPartPriceMultipliers()[0] = Double.parseDouble(nodeContents);
                } else if (nodeName.equalsIgnoreCase("usedPartsValueB")) { // Legacy - 0.49.3 Removal
                    campaignOptions.getUsedPartPriceMultipliers()[1] = Double.parseDouble(nodeContents);
                } else if (nodeName.equalsIgnoreCase("usedPartsValueC")) { // Legacy - 0.49.3 Removal
                    campaignOptions.getUsedPartPriceMultipliers()[2] = Double.parseDouble(nodeContents);
                } else if (nodeName.equalsIgnoreCase("usedPartsValueD")) { // Legacy - 0.49.3 Removal
                    campaignOptions.getUsedPartPriceMultipliers()[3] = Double.parseDouble(nodeContents);
                } else if (nodeName.equalsIgnoreCase("usedPartsValueE")) { // Legacy - 0.49.3 Removal
                    campaignOptions.getUsedPartPriceMultipliers()[4] = Double.parseDouble(nodeContents);
                } else if (nodeName.equalsIgnoreCase("usedPartsValueF")) { // Legacy - 0.49.3 Removal
                    campaignOptions.getUsedPartPriceMultipliers()[5] = Double.parseDouble(nodeContents);
                } else if (nodeName.equalsIgnoreCase("damagedPartsValue")) { // Legacy - 0.49.3 Removal
                    campaignOptions.setDamagedPartsValueMultiplier(Double.parseDouble(nodeContents));
                } else if (nodeName.equalsIgnoreCase("canceledOrderReimbursement")) { // Legacy - 0.49.3
                    // Removal
                    campaignOptions.setCancelledOrderRefundMultiplier(Double.parseDouble(nodeContents));

                    // Removed in 0.47.*
                } else if (nodeName.equalsIgnoreCase("personnelMarketType")) { // Legacy
                    campaignOptions.setPersonnelMarketName(PersonnelMarket.getTypeName(Integer.parseInt(nodeContents)));
                } else if (nodeName.equalsIgnoreCase("intensity")) { // Legacy
                    double intensity = Double.parseDouble(nodeContents);

                    campaignOptions.setAtBBattleChance(CombatRole.MANEUVER.ordinal(),
                          (int) Math.round(((40.0 * intensity) / (40.0 * intensity + 60.0)) * 100.0 + 0.5));
                    campaignOptions.setAtBBattleChance(CombatRole.FRONTLINE.ordinal(),
                          (int) Math.round(((20.0 * intensity) / (20.0 * intensity + 80.0)) * 100.0 + 0.5));
                    campaignOptions.setAtBBattleChance(CombatRole.PATROL.ordinal(),
                          (int) Math.round(((60.0 * intensity) / (60.0 * intensity + 40.0)) * 100.0 + 0.5));
                    campaignOptions.setAtBBattleChance(CombatRole.TRAINING.ordinal(),
                          (int) Math.round(((10.0 * intensity) / (10.0 * intensity + 90.0)) * 100.0 + 0.5));
                } else if (nodeName.equalsIgnoreCase("personnelMarketType")) { // Legacy
                    campaignOptions.setPersonnelMarketName(PersonnelMarket.getTypeName(Integer.parseInt(nodeContents)));
                } else if (nodeName.equalsIgnoreCase("startGameDelay")) { // Legacy
                    MekHQ.getMHQOptions().setStartGameDelay(Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("historicalDailyLog")) { // Legacy
                    MekHQ.getMHQOptions().setHistoricalDailyLog(Boolean.parseBoolean(nodeContents));
                } else if (nodeName.equalsIgnoreCase("useUnitRating") // Legacy
                                 || nodeName.equalsIgnoreCase("useDragoonRating")) { // Legacy
                    if (!Boolean.parseBoolean(nodeContents)) {
                        campaignOptions.setUnitRatingMethod(UnitRatingMethod.NONE);
                    }
                } else if (nodeName.equalsIgnoreCase("probPhenoMW")) { // Legacy
                    campaignOptions.setPhenotypeProbability(Phenotype.MEKWARRIOR.ordinal(),
                          Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("probPhenoBA")) { // Legacy
                    campaignOptions.setPhenotypeProbability(Phenotype.ELEMENTAL.ordinal(),
                          Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("probPhenoAero")) { // Legacy
                    campaignOptions.setPhenotypeProbability(Phenotype.AEROSPACE.ordinal(),
                          Integer.parseInt(nodeContents));
                } else if (nodeName.equalsIgnoreCase("probPhenoVee")) { // Legacy
                    campaignOptions.setPhenotypeProbability(Phenotype.VEHICLE.ordinal(),
                          Integer.parseInt(nodeContents));
                }
            } catch (Exception ex) {
                LOGGER.error(ex, "Unknown Exception: generationCampaignOptionsFromXML");
            }
        }

        LOGGER.debug("Load Campaign Options Complete!");

        return campaignOptions;
    }

    /**
     * This is annoyingly required for the case of anyone having changed the surname weights. The code is not nice, but
     * will nicely handle the cases where anyone has made changes
     *
     * @param values the values to migrate
     */
    private static void migrateMarriageSurnameWeights(final CampaignOptions campaignOptions, final String... values) {
        int[] weights = new int[values.length];

        for (int i = 0; i < weights.length; i++) {
            try {
                weights[i] = Integer.parseInt(values[i]);
            } catch (Exception ex) {
                LOGGER.error(ex, "Unknown Exception: migrateMarriageSurnameWeights47");
                weights[i] = 0;
            }
        }

        // Now we need to test it to figure out the weights have changed. If not, we
        // will keep the
        // new default values. If they have, we save their changes and add the new
        // surname weights
        if ((weights[0] != campaignOptions.getMarriageSurnameWeights().get(MergingSurnameStyle.NO_CHANGE)) ||
                  (weights[1] != campaignOptions.getMarriageSurnameWeights().get(MergingSurnameStyle.YOURS) + 5) ||
                  (weights[2] != campaignOptions.getMarriageSurnameWeights().get(MergingSurnameStyle.SPOUSE) + 5) ||
                  (weights[3] !=
                         campaignOptions.getMarriageSurnameWeights().get(MergingSurnameStyle.HYPHEN_SPOUSE) + 5) ||
                  (weights[4] !=
                         campaignOptions.getMarriageSurnameWeights().get(MergingSurnameStyle.BOTH_HYPHEN_SPOUSE) + 5) ||
                  (weights[5] !=
                         campaignOptions.getMarriageSurnameWeights().get(MergingSurnameStyle.HYPHEN_YOURS) + 5) ||
                  (weights[6] !=
                         campaignOptions.getMarriageSurnameWeights().get(MergingSurnameStyle.BOTH_HYPHEN_YOURS) + 5) ||
                  (weights[7] != campaignOptions.getMarriageSurnameWeights().get(MergingSurnameStyle.MALE)) ||
                  (weights[8] != campaignOptions.getMarriageSurnameWeights().get(MergingSurnameStyle.FEMALE))) {
            campaignOptions.getMarriageSurnameWeights().put(MergingSurnameStyle.NO_CHANGE, weights[0]);
            campaignOptions.getMarriageSurnameWeights().put(MergingSurnameStyle.YOURS, weights[1]);
            campaignOptions.getMarriageSurnameWeights().put(MergingSurnameStyle.SPOUSE, weights[2]);
            // SPACE_YOURS is newly added
            // BOTH_SPACE_YOURS is newly added
            campaignOptions.getMarriageSurnameWeights().put(MergingSurnameStyle.HYPHEN_YOURS, weights[3]);
            campaignOptions.getMarriageSurnameWeights().put(MergingSurnameStyle.BOTH_HYPHEN_YOURS, weights[4]);
            // SPACE_SPOUSE is newly added
            // BOTH_SPACE_SPOUSE is newly added
            campaignOptions.getMarriageSurnameWeights().put(MergingSurnameStyle.HYPHEN_SPOUSE, weights[5]);
            campaignOptions.getMarriageSurnameWeights().put(MergingSurnameStyle.BOTH_HYPHEN_SPOUSE, weights[6]);
            campaignOptions.getMarriageSurnameWeights().put(MergingSurnameStyle.MALE, weights[7]);
            campaignOptions.getMarriageSurnameWeights().put(MergingSurnameStyle.FEMALE, weights[8]);
        }
    }
}
