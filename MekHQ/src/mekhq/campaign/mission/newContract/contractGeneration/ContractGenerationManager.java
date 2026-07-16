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
package mekhq.campaign.mission.newContract.contractGeneration;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;
import megamek.logging.MMLogger;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.camOpsReputation.ForceReputationController;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.newContract.AbstractContractManager;
import mekhq.campaign.mission.newContract.AbstractContractObjective;
import mekhq.campaign.mission.newContract.NormalContractManager;
import mekhq.campaign.mission.newContract.NormalContractObjective;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.ConnectionsLevel;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;
import org.jspecify.annotations.Nullable;

/**
 * Orchestrates end-to-end generation of a new contract, tying together the individual determination stages: employer,
 * objective type, enemy, location, negotiators, negotiated terms, and pay.
 */
public class ContractGenerationManager {
    private static final MMLogger LOGGER = MMLogger.create(ContractGenerationManager.class);

    private ContractGenerationManager() {}

    /**
     * Generates a new contract for the campaign, running each determination stage in turn (employer, objective type,
     * enemy, location, negotiators, terms, and pay).
     *
     * @param campaign                the current campaign
     * @param forceReputationFactor   the force's reputation factor, applied to employer selection and pay
     * @param currentLocation         the campaign's current location
     * @param playerNegotiator        the player's negotiator
     * @param formations              the campaign's formations
     * @param localHangar             the campaign's localHangar
     * @param temporaryAsTechPoolSize the temporary astech pool size, factored into pay
     * @param temporaryMedicPool      the temporary medic pool size, factored into pay
     * @param temporaryCrewMap        temporary crew counts by role, factored into pay
     * @param campaignType            the campaign type, which drives employer, objective, and negotiator determination
     */
    public static void generateContract(Campaign campaign, double forceReputationFactor,
          AbstractLocation currentLocation, Person playerNegotiator, List<Formation> formations,
          LocalHangar localHangar,
          int temporaryAsTechPoolSize, int temporaryMedicPool, Map<PersonnelRole, Integer> temporaryCrewMap,
          CampaignTypeForContractDetermination campaignType) {
        AbstractContractManager contractManager = new NormalContractManager();

        // Generate Employer, Contract Type, Enemy, & Location
        LocalDate currentDate = campaign.getLocalDate();
        PlanetarySystem currentSystem = currentLocation.getCurrentSystem();
        HiringHallLevel hiringHallLevel = currentSystem == null ?
                                                HiringHallLevel.NONE :
                                                currentSystem.getHiringHallLevel(currentDate);

        generateEmployerContractTypeEnemyAndLocation(campaign,
              forceReputationFactor,
              currentLocation,
              playerNegotiator,
              currentDate,
              contractManager,
              hiringHallLevel,
              campaignType);

        // Employer Modifier Data
        Faction employerFaction = contractManager.getEmployerFaction();
        EmployerModifierData employerModifierData = new EmployerModifierData();
        int currentYear = currentDate.getYear();
        ForceReputationController reputation = campaign.getReputation();
        int reputationRating = reputation.getReputationRating();
        EmployerNegotiationsModifier.getNegotiationsModifier(employerFaction, currentYear, employerModifierData);
        UnitReputationNegotiationsModifier.getNegotiationsModifier(reputationRating, employerModifierData);

        if (contractManager.isHighRisk()) {
            SpecialNegotiationsModifier.applyHighRisk(employerModifierData);
        }

        if (contractManager.isCovert()) {
            SpecialNegotiationsModifier.applyCovert(employerModifierData);
        }

        contractManager.setEmployerModifierData(employerModifierData);

        // Contract Pay Data
        Faction campaignFaction = campaign.getFaction();
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        ContractPayData contractPayData = ContractDeterminationPay.generateContractPay(contractManager,
              formations,
              localHangar,
              campaignOptions,
              currentDate,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              temporaryCrewMap,
              campaignFaction,
              forceReputationFactor);
        contractManager.setContractPayData(contractPayData);

        // Initial Negotiations
        Person employerNegotiator = EmployerNegotiator.generateNegotiator(campaign,
              campaignType,
              employerFaction,
              hiringHallLevel);
        NegotiationsData negotiationsData = ContractDeterminationNegotiations.determineInitialNegotiationTerms(
              employerModifierData,
              campaign,
              playerNegotiator,
              employerNegotiator);
        contractManager.setNegotiationsData(negotiationsData);
    }

    private static void generateEmployerContractTypeEnemyAndLocation(Campaign campaign, double forceReputationFactor,
          AbstractLocation currentLocation, Person negotiator, LocalDate currentDate,
          AbstractContractManager parentContractManager, HiringHallLevel hiringHallLevel,
          CampaignTypeForContractDetermination campaignType) {
        // Pick employer
        EmployerFactionSelection employerFactionSelectionData = generateEmployerFaction(campaign,
              forceReputationFactor,
              currentLocation,
              negotiator,
              currentDate,
              hiringHallLevel,
              parentContractManager);

        // For pirate campaigns the faction selected above is really the victim, not the true employer; it is still
        // used below to determine the contract's objectives.
        Faction selectedFaction = employerFactionSelectionData.employerFaction();
        if (selectedFaction == null) {
            return;
        }

        // Pick contract objectives
        List<AtBContractType> objectives = generateContractObjectives(campaign,
              negotiator,
              hiringHallLevel,
              employerFactionSelectionData,
              parentContractManager);
        if (objectives.isEmpty()) {
            LOGGER.error("Contract generated with no objectives. No contract generated.");
            return;
        }
        AtBContractType firstObjective = objectives.getFirst();

        // CamOps pg 39 rev 5th printing: pirate factions pick target, not employer. So the random employer we've
        // chosen becomes the contract's enemy, pirate (Bandit Caste for a Clan campaign) becomes the 'employer.'
        Faction employerFaction;
        Faction enemyFaction;
        if (campaignType == CampaignTypeForContractDetermination.PIRATE) {
            employerFaction = getPirateEmployerFaction(campaign);
            enemyFaction = selectedFaction;
            parentContractManager.setEmployerFactionCode(employerFaction.getShortName());
            assignEnemyFactionToObjectives(parentContractManager, enemyFaction);
        } else {
            employerFaction = selectedFaction;
            enemyFaction = generateEnemyFactionForContractObjectives(currentLocation,
                  currentDate,
                  employerFactionSelectionData,
                  firstObjective,
                  parentContractManager);
        }

        // Pick location. The employer is the attacker and the enemy the defender; for pirates this means a landless
        // pirate attacker striking into the victim's (defender's) territory, which MissionTargetFinder handles.
        String targetSystemId = generateContractLocation(currentLocation,
              firstObjective,
              employerFaction,
              enemyFaction,
              parentContractManager);
        if (targetSystemId == null) {
            LOGGER.error("Somehow failed to generate a contract without a viable location. This shouldn't be possible.");
            return;
        }
    }

    /**
     * Resolves the pirate faction that acts as the employer for a pirate campaign's contract: the Bandit Caste for a
     * Clan campaign, or the generic pirate faction otherwise.
     */
    private static Faction getPirateEmployerFaction(Campaign campaign) {
        String pirateFactionCode = campaign.isClanCampaign() ?
                                         Faction.BANDIT_CASTE_FACTION_CODE :
                                         Faction.PIRATE_FACTION_CODE;
        return Factions.getInstance().getFaction(pirateFactionCode);
    }

    private static @Nullable String generateContractLocation(AbstractLocation currentLocation,
          AtBContractType firstObjective, Faction employerFaction, Faction enemyFaction,
          AbstractContractManager parentContractManager) {
        if (employerFaction == null) {
            LOGGER.error("Null employer passed into generateContractLocation. This should have been vetted already");
            return null;
        }

        // TODO change from short name to faction object
        String targetSystemId = ContractDeterminationLocation.determineContractLocation(firstObjective,
              true,
              employerFaction.getShortName(),
              enemyFaction.getShortName(),
              currentLocation);
        if (targetSystemId == null) {
            LOGGER.error("Could not find target system for contract generation. No contract generated.");
            return null;
        }

        parentContractManager.setTargetSystemId(targetSystemId);

        return targetSystemId;
    }

    private static Faction generateEnemyFactionForContractObjectives(AbstractLocation currentLocation,
          LocalDate currentDate, EmployerFactionSelection employerFactionSelectionData, AtBContractType firstObjective,
          AbstractContractManager parentContractManager) {
        // Only the first objective is used to determine the initial enemy, as this is used to influence contract
        // location.
        Faction enemyFaction = ContractDeterminationObjectiveEnemy.generateEnemyFactionForObjective(currentLocation,
              currentDate, employerFactionSelectionData.employerFaction(), firstObjective);
        assignEnemyFactionToObjectives(parentContractManager, enemyFaction);

        return enemyFaction;
    }

    /**
     * Stamps the given enemy faction's code onto every objective already registered with the contract manager.
     */
    private static void assignEnemyFactionToObjectives(AbstractContractManager parentContractManager,
          Faction enemyFaction) {
        String enemyFactionCode = enemyFaction.getShortName();
        for (AbstractContractObjective contractObjective : parentContractManager.getContractAllObjectivesCopy()) {
            contractObjective.setEnemyFactionCode(enemyFactionCode);
        }
    }

    private static List<AtBContractType> generateContractObjectives(Campaign campaign, Person negotiator,
          HiringHallLevel hiringHallLevel, EmployerFactionSelection employerFactionSelectionData,
          AbstractContractManager contractManager) {
        Faction employerFaction = employerFactionSelectionData.employerFaction();
        if (employerFaction == null) {
            LOGGER.error("Null employer passed into generateContractObjectives. This should have been vetted already");
            return List.of();
        }

        ContractDeterminationObjectiveType contractDeterminationObjectiveType = new ContractDeterminationObjectiveType(
              campaign,
              hiringHallLevel,
              negotiator,
              employerFaction,
              employerFactionSelectionData.globalEmployerTableValue(),
              employerFactionSelectionData.independentEmployerTableValue());

        List<AtBContractType> objectives = contractDeterminationObjectiveType.getObjectiveTypes();
        for (AtBContractType objective : objectives) {
            AbstractContractObjective contractObjective = new NormalContractObjective();
            contractObjective.setObjectiveType(objective);
            contractManager.addContractObjective(contractObjective);
        }

        // isCovert and isHighRisk are set here and apply to the whole contract even if only true for one objective
        boolean isCovert = contractDeterminationObjectiveType.isCovert();
        if (isCovert) {
            contractManager.setCovert(true);
        }

        boolean isHighRisk = contractDeterminationObjectiveType.isHighRisk();
        if (isHighRisk) {
            contractManager.setHighRisk(true);
        }

        return objectives;
    }

    private static @Nonnull EmployerFactionSelection generateEmployerFaction(Campaign campaign,
          double forceReputationFactor, AbstractLocation currentLocation, Person negotiator, LocalDate currentDate,
          HiringHallLevel hiringHallLevel, AbstractContractManager parentContractManager) {
        Faction campaignFaction = campaign.getFaction();
        int adjustedConnectionsLevel = negotiator.getAdjustedConnections(false);
        ConnectionsLevel connectionsLevel = ConnectionsLevel.parseConnectionsLevelFromInt(adjustedConnectionsLevel);
        int connectionsEquipLevel = connectionsLevel.getEquipLevel();

        EmployerFactionSelection employerFactionSelectionData =
              ContractDeterminationEmployer.getEmployerFactionSelectionData(currentLocation,
                    connectionsEquipLevel,
                    campaignFaction,
                    currentDate,
                    hiringHallLevel,
                    forceReputationFactor);
        Faction employerFaction = employerFactionSelectionData.employerFaction();
        if (employerFaction == null) {
            LOGGER.error("Could not find employer for contract generation. No contract generated.");
            return employerFactionSelectionData;
        }

        parentContractManager.setEmployerFactionCode(employerFaction.getShortName());
        return employerFactionSelectionData;
    }
}
