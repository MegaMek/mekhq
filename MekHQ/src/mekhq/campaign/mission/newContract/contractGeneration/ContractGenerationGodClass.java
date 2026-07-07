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
import mekhq.campaign.Hangar;
import mekhq.campaign.camOpsReputation.ReputationController;
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
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;
import org.jspecify.annotations.Nullable;

public class ContractGenerationGodClass {
    private static final MMLogger LOGGER = MMLogger.create(ContractGenerationGodClass.class);

    public ContractGenerationGodClass() {
    }

    public void generateContract(Campaign campaign, double forceReputationFactor, AbstractLocation currentLocation,
          Person negotiator, List<Formation> formations, Hangar hangar, int temporaryAsTechPoolSize,
          int temporaryMedicPool, Map<PersonnelRole, Integer> temporaryCrewMap) {
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
              negotiator,
              currentDate,
              contractManager,
              hiringHallLevel);

        // Employer Modifier Data
        Faction employerFaction = contractManager.getEmployerFaction();
        EmployerModifierData employerModifierData = new EmployerModifierData();
        int currentYear = currentDate.getYear();
        ReputationController reputation = campaign.getReputation();
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
        ContractPayData contractPayData = DetermineContractPay.generateContractPay(contractManager,
              formations,
              hangar,
              campaignOptions,
              currentDate,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              temporaryCrewMap,
              campaignFaction,
              forceReputationFactor);
        contractManager.setContractPayData(contractPayData);
    }

    private static void generateEmployerContractTypeEnemyAndLocation(Campaign campaign, double forceReputationFactor,
          AbstractLocation currentLocation, Person negotiator, LocalDate currentDate,
          AbstractContractManager parentContractManager, HiringHallLevel hiringHallLevel) {
        // Pick employer
        EmployerFactionSelection employerFactionSelectionData = generateEmployerFaction(campaign,
              forceReputationFactor,
              currentLocation,
              negotiator,
              currentDate,
              hiringHallLevel,
              parentContractManager);

        Faction employerFaction = employerFactionSelectionData.employerFaction();
        if (employerFaction == null) {
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

        // Pick enemy
        Faction enemyFaction = generateEnemyFactionForContractObjectives(currentLocation,
              currentDate,
              employerFactionSelectionData,
              firstObjective,
              parentContractManager);

        // Pick location
        String targetSystemId = generateContractLocation(currentLocation,
              firstObjective,
              employerFactionSelectionData,
              enemyFaction,
              parentContractManager);
        if (targetSystemId == null) {
            LOGGER.error("Somehow failed to generate a contract without a viable location. This shouldn't be possible.");
            return;
        }
    }

    private static @Nullable String generateContractLocation(AbstractLocation currentLocation,
          AtBContractType firstObjective, EmployerFactionSelection employerFactionSelectionData, Faction enemyFaction,
          AbstractContractManager parentContractManager) {
        Faction employerFaction = employerFactionSelectionData.employerFaction();
        if (employerFaction == null) {
            LOGGER.error("Null employer passed into generateContractLocation. This should have been vetted already");
            return null;
        }

        // TODO change from short name to faction object
        String targetSystemId = ContractLocationDetermination.determineContractLocation(firstObjective,
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
        Faction enemyFaction = ObjectiveEnemyDetermination.generateEnemyFactionForObjective(currentLocation,
              currentDate, employerFactionSelectionData.employerFaction(), firstObjective);
        String enemyFactionCode = enemyFaction.getShortName();
        for (AbstractContractObjective contractObjective : parentContractManager.getContractAllObjectivesCopy()) {
            contractObjective.setEnemyFactionCode(enemyFactionCode);
        }

        return enemyFaction;
    }

    private static List<AtBContractType> generateContractObjectives(Campaign campaign, Person negotiator,
          HiringHallLevel hiringHallLevel, EmployerFactionSelection employerFactionSelectionData,
          AbstractContractManager contractManager) {
        Faction employerFaction = employerFactionSelectionData.employerFaction();
        if (employerFaction == null) {
            LOGGER.error("Null employer passed into generateContractObjectives. This should have been vetted already");
            return List.of();
        }

        MissionObjectiveTypeDetermination missionObjectiveTypeDetermination = new MissionObjectiveTypeDetermination(
              campaign,
              hiringHallLevel,
              negotiator,
              employerFaction,
              employerFactionSelectionData.globalEmployerTableValue(),
              employerFactionSelectionData.independentEmployerTableValue());

        List<AtBContractType> objectives = missionObjectiveTypeDetermination.getObjectiveTypes();
        for (AtBContractType objective : objectives) {
            AbstractContractObjective contractObjective = new NormalContractObjective();
            contractObjective.setObjectiveType(objective);
            contractManager.addContractObjective(contractObjective);
        }

        boolean isCovert = missionObjectiveTypeDetermination.isCovert();
        contractManager.setCovert(isCovert);

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
              ContractEmployerDetermination.getEmployerFactionSelectionData(currentLocation,
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
