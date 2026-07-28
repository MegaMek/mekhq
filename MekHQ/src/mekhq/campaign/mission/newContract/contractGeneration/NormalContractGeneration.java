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

import static java.lang.Math.ceil;

import java.time.LocalDate;
import java.util.Collection;

import jakarta.annotation.Nullable;
import megamek.codeUtilities.ObjectUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.newContract.ChaosContract;
import mekhq.campaign.mission.newContract.contractData.ContractObjectiveData;
import mekhq.campaign.mission.newContract.contractData.ContractScheduleData;
import mekhq.campaign.mission.newContract.contractData.EmployerData;
import mekhq.campaign.mission.newContract.contractData.EnemyData;
import mekhq.campaign.mission.newContract.contractData.SystemsTargetData;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import org.jspecify.annotations.NonNull;

public class NormalContractGeneration extends AbstractContractGeneration {
    public static @Nullable ChaosContract createChaosContract(Campaign campaign, CampaignOptions campaignOptions,
          LocalDate currentDate, ILocation currentLocation, int contractGenerationModifier,
          ContractSearchType searchType, FactionStandings factionStandings,
          boolean overridingCommandCircuitRequirements, boolean isGM) {
        final ChaosContract contract = new ChaosContract();

        // Step 1: Employer
        EmployerData employerData = pickEmployer(campaign, currentDate, currentLocation, searchType, contract);
        if (employerData == null) {
            return null;
        }

        // Step 2: Type
        ContractObjectiveData objectiveData = pickObjective(contractGenerationModifier, contract);

        // Step 3: Enemy
        EnemyData enemyData = pickEnemy(currentDate, currentLocation, employerData, objectiveData, contract);

        // Step 4: Location
        PlanetarySystem targetSystem = pickTargetLocation(currentLocation,
              objectiveData,
              employerData,
              enemyData,
              contract);

        // Step 5: Length
        boolean useVariableContractLength = campaignOptions.get(CampaignOption.VARIABLE_CONTRACT_LENGTH);
        ChaosObjectiveType objectiveType = determineSchedule(campaign,
              useVariableContractLength,
              currentDate,
              currentLocation.getCurrentSystem(),
              factionStandings,
              overridingCommandCircuitRequirements,
              isGM,
              targetSystem,
              employerData.factionCode(),
              objectiveData.playerObjectiveType().getChaosObjectiveType(),
              contract);

        // Step 6: Initial Terms
        setContractTerms(objectiveType, employerData.type(), contract);

        // Step 7: Track Count & Intensity
        setTrackCount(objectiveType, contract);

        // Step 8: Return the Contract
        return contract;
    }

    private static void setTrackCount(ChaosObjectiveType objectiveType, ChaosContract contract) {
        int trackCount = ChaosContractDetermineIntensity.determineTrackCount(objectiveType);
        contract.setTrackCount(trackCount);
    }

    private static void setContractTerms(ChaosObjectiveType objectiveType, ChaosEmployerType employerType,
          ChaosContract contract) {
        ContractTermsData initialContractTerms = ChaosContractDetermineTerms.determineInitialTerms(objectiveType,
              employerType);
        contract.setContractTerms(initialContractTerms);
    }

    private static @NonNull ChaosObjectiveType determineSchedule(Campaign campaign, boolean useVariableContractLength,
          LocalDate currentDate, PlanetarySystem currentSystem, FactionStandings factionStandings,
          boolean overridingCommandCircuitRequirements, boolean isGM, PlanetarySystem targetSystem,
          String employerFactionCode, ChaosObjectiveType objectiveType, ChaosContract contract) {
        // Jump Path
        JumpPath jumpPath = campaign.calculateJumpPath(currentSystem, targetSystem);
        contract.setCachedJumpPath(jumpPath);

        // Journey Duration
        boolean isUseCommandCircuit = FactionStandingUtilities.isUseCommandCircuit(overridingCommandCircuitRequirements,
              isGM, factionStandings, employerFactionCode);
        int journeyTimeInDays = (int) ceil(jumpPath.getTotalTime(currentDate, 0, isUseCommandCircuit));

        // Contract schedule
        LocalDate startDate = currentDate.plusDays(journeyTimeInDays);

        int monthsLength = objectiveType.calculateLength(useVariableContractLength);
        LocalDate endDate = startDate.plusMonths(monthsLength);

        ContractScheduleData contractScheduleData = new ContractScheduleData(startDate, endDate, monthsLength);
        contract.setScheduleData(contractScheduleData);

        return objectiveType;
    }

    private static @NonNull EnemyData pickEnemy(LocalDate currentDate, ILocation currentLocation,
          EmployerData employerData, ContractObjectiveData objectiveData, ChaosContract contract) {
        EnemyData enemyData = ChaosContractDeterminationEnemy.generateEnemyFactionForObjective(currentLocation,
              currentDate, employerData.getFaction(), objectiveData.playerObjectiveType());
        contract.setEnemyData(enemyData);
        return enemyData;
    }

    private static @NonNull PlanetarySystem pickTargetLocation(ILocation currentLocation,
          ContractObjectiveData objectiveData, EmployerData employerData, EnemyData enemyData, ChaosContract contract) {
        String targetSystemId = ChaosContractDeterminationLocation.determineContractLocation(objectiveData.playerObjectiveType(),
              true,
              employerData.factionCode(),
              enemyData.factionCode(),
              currentLocation);
        PlanetarySystem targetSystem = Systems.getInstance().getSystemById(targetSystemId);
        Collection<Planet> candidatePlanets = targetSystem.getPlanets();
        // TODO use same planetary picker profile as System picker
        Planet targetPlanet = ObjectUtility.getRandomItem(candidatePlanets);
        SystemsTargetData systemsTargetData = new SystemsTargetData(targetSystemId, targetPlanet.getId());
        contract.setSystemsTargetData(systemsTargetData);
        return targetSystem;
    }

    private static @NonNull ContractObjectiveData pickObjective(int contractGenerationModifier,
          ChaosContract contract) {
        ContractObjectiveData objectiveData = ChaosContractObjectiveDetermination.determineContractObjectiveType(
              contractGenerationModifier);
        contract.setObjectiveData(objectiveData);
        return objectiveData;
    }

    private static @org.jspecify.annotations.Nullable EmployerData pickEmployer(Campaign campaign,
          LocalDate currentDate, ILocation currentLocation, ContractSearchType searchType, ChaosContract contract) {
        EmployerData employerData = ChaosContractEmployerDetermination.getEmployerGenerationData(currentDate,
              currentLocation,
              campaign,
              searchType);
        if (employerData == null) {
            // No employer means no contract
            return null;
        }

        contract.setEmployerData(employerData);
        return employerData;
    }
}
