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
import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.enums.ContractMoraleLevel;
import mekhq.campaign.mission.newContract.AbstractContract;
import mekhq.campaign.mission.newContract.ChaosContract;
import mekhq.campaign.mission.newContract.contractData.ContractObjectiveData;
import mekhq.campaign.mission.newContract.contractData.ContractScheduleData;
import mekhq.campaign.mission.newContract.contractData.EmployerData;
import mekhq.campaign.mission.newContract.contractData.EnemyData;
import mekhq.campaign.mission.newContract.contractData.MoraleData;
import mekhq.campaign.mission.newContract.contractData.RentedFacilitiesData;
import mekhq.campaign.mission.newContract.contractData.SystemsTargetData;
import mekhq.campaign.mission.newContract.contractGeneration.targetFinder.ChaosContractPayDetermination;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.reputation.chaosReputation.ChaosReputation;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

public class AbstractContractGeneration {
    private static final MMLogger LOGGER = MMLogger.create(AbstractContractGeneration.class);

    public static @Nullable AbstractContract createContract(Campaign campaign, CampaignOptions campaignOptions,
          LocalDate currentDate, Detachment detachment, int contractGenerationModifier, ContractSearchType searchType,
          FactionStandings factionStandings, boolean overridingCommandCircuitRequirements, boolean isGM) {
        final ChaosContract contract = new ChaosContract();

        // Step 1: Employer
        AbstractLocation currentLocation = detachment.getCurrentLocation();
        EmployerData employerData = pickEmployer(campaign, currentDate, currentLocation, searchType, contract);
        if (employerData == null) {
            return null;
        }

        // Step 2: Type
        ContractObjectiveData objectiveData = pickObjective(contractGenerationModifier, contract);
        ChaosObjectiveType chaosObjectiveType = objectiveData.playerObjectiveType().getChaosObjectiveType();
        boolean isDefensiveObjective = !objectiveData.playerObjectiveType().getChaosObjectiveType().isAttacker();

        // Step 3: Scale & Intensity
        setTrackCount(campaign.getPlayerForce(), detachment.getHangar(), chaosObjectiveType, contract);

        // Step 3: Enemy (situated against the employer's territorial anchor faction, so a landless flavor employer -
        // rebels, a mercenary command, a corporation - still yields a sensible nearby belligerent)
        EnemyData enemyData = pickEnemy(currentDate, currentLocation, employerData, objectiveData, contract);

        // Step 4: Location
        SystemsTargetData systemsTargetData = pickTargetLocation(currentLocation,
              currentDate,
              objectiveData,
              employerData,
              enemyData,
              contract,
              isDefensiveObjective,
              employerData.type().isCurrentSystemEmployer());
        if (systemsTargetData == null) {
            // No valid target planet means nowhere to situate the contract.
            return null;
        }

        // Step 5: Force Ratings (skill and equipment of both sides)
        PlayerForce playerForce = campaign.getPlayerForce();
        PlanetarySystem targetSystem = Systems.getInstance().getSystemById(systemsTargetData.systemId());
        Planet targetPlanet = targetSystem == null ? null : targetSystem.getPlanetById(systemsTargetData.planetId());
        setForceRatings(playerForce,
              campaignOptions,
              currentDate,
              detachment,
              targetPlanet,
              employerData,
              enemyData,
              objectiveData,
              contract);

        // Step 6: Length
        boolean useVariableContractLength = campaignOptions.get(CampaignOption.VARIABLE_CONTRACT_LENGTH);
        determineSchedule(campaign,
              useVariableContractLength,
              currentDate,
              currentLocation.getCurrentSystem(),
              targetPlanet,
              factionStandings,
              overridingCommandCircuitRequirements,
              isGM,
              targetSystem,
              employerData.factionCode(),
              chaosObjectiveType,
              contract);

        // Step 7: Initial Terms
        setContractTerms(chaosObjectiveType, employerData.type(), contract);

        // Step 9: Final Tasks
        performFinalTasks(campaign, currentDate, contract, currentLocation);

        // Step 10: Difficulty estimate (cached; enemy force and skill are finalized by this point)
        contract.setCachedContractDifficulty(
              ChaosContractDeterminationDifficulty.calculateContractDifficulty(campaign, contract));

        // Step 11: Return the Contract
        return contract;
    }

    private static void performFinalTasks(Campaign campaign, LocalDate currentDate, ChaosContract contract,
          AbstractLocation currentLocation) {
        // Basic Information
        UUID contractId = UUID.randomUUID();
        contract.setContractId(contractId);
        contract.setMissionStatus(null);

        // Contract Details
        String contractName = contract.getStartDate().toString() +
                                    " - " +
                                    contract.getObjectiveType().name() +
                                    ": " +
                                    contract.getEmployerDisplayName() +
                                    " vs. " +
                                    contract.getEnemyDisplayName();
        contract.setContractName(contractName);

        String description = "Placeholder description";
        contract.setDescription(description);

        // Morale
        MoraleData moraleData = new MoraleData(ContractMoraleLevel.STALEMATE);
        contract.setMoraleData(moraleData);

        // Rented Facilities
        RentedFacilitiesData rentedFacilitiesData = new RentedFacilitiesData(0, 0, 0);
        contract.setRentedFacilitiesData(rentedFacilitiesData);

        // StratCon
        StratConCampaignState stratConCampaignState = new StratConCampaignState();
        contract.setStratConCampaignState(stratConCampaignState);

        // Pay
        ChaosContractPayDetermination.determineContractPayForChaosContract(campaign, currentDate, contract,
              currentLocation);
    }

    private static void setTrackCount(PlayerForce playerForce, LocalHangar detachmentHangar,
          ChaosObjectiveType objectiveType, ChaosContract contract) {
        int scale = ChaosContractDeterminationScale.generateScaleForDetachment(playerForce,
              detachmentHangar,
              contract.getObjectiveType().isCadreDuty());
        contract.setScale(scale);

        int trackCount = ChaosContractDetermineIntensity.determineTrackCount(objectiveType);
        contract.setTrackCount(trackCount);
    }

    /**
     * Rates the skill and equipment both sides commit to the contract, then rebuilds the employer and enemy data with
     * those ratings in place of the placeholder defaults.
     *
     * <p>The rating is grounded in the galaxy: it synthesizes the contract's {@link ContractImportance} from who is
     * hiring, what for, and the strategic value of the world being fought over, then hands that &mdash; alongside each
     * side's faction, role, and the era &mdash; to {@link ChaosEmployerForceRating}. When the campaign has opted into
     * dynamic difficulty, the ratings are additionally scaled toward the player's own skill.</p>
     *
     * @param campaignOptions the campaign options
     * @param currentDate     the current date, for era context and reading the target world's data
     * @param targetPlanet    the target planet
     * @param employerData    the employer data to re-rate
     * @param enemyData       the enemy data to re-rate
     * @param objectiveData   the contract's objectives, for the player objective's strategic scope
     * @param contract        the contract being built
     */
    private static void setForceRatings(PlayerForce playerForce, CampaignOptions campaignOptions, LocalDate currentDate,
          Detachment detachment, Planet targetPlanet, EmployerData employerData, EnemyData enemyData,
          ContractObjectiveData objectiveData, ChaosContract contract) {
        int planetStrategicValue = (targetPlanet == null) ?
                                         0 :
                                         ChaosPlanetStrategicValue.calculate(targetPlanet, currentDate);

        ChaosObjectiveType objectiveType = objectiveData.playerObjectiveType().getChaosObjectiveType();
        ContractImportance importance = ContractImportance.from(employerData.type(), objectiveType,
              planetStrategicValue);

        boolean isPlayerAttacker = objectiveType.isAttacker();
        int year = currentDate.getYear();
        boolean scaleToPlayer = campaignOptions.get(CampaignOption.USE_DYNAMIC_DIFFICULTY);

        boolean isClanForce = playerForce.isClanForce();
        SkillLevel playerAverageSkill = getAverageSkill(playerForce,
              isClanForce,
              campaignOptions,
              currentDate,
              detachment);

        ChaosEmployerForceRating.ForceRating employerRating = ChaosEmployerForceRating.determine(employerData.getFaction(),
              isPlayerAttacker,
              true,
              year,
              importance,
              scaleToPlayer,
              playerAverageSkill);

        ChaosEmployerForceRating.ForceRating enemyRating = ChaosEmployerForceRating.determine(enemyData.getFaction(),
              !isPlayerAttacker,
              false,
              year,
              importance,
              scaleToPlayer,
              playerAverageSkill);

        EmployerData updatedEmployerData = new EmployerData(employerData,
              employerRating.forceSkill(),
              employerRating.equipmentRating());
        contract.setEmployerData(updatedEmployerData);

        EnemyData updatedEnemyData = new EnemyData(enemyData, enemyRating.forceSkill(), enemyRating.equipmentRating());
        contract.setEnemyData(updatedEnemyData);
    }

    private static SkillLevel getAverageSkill(PlayerForce playerForce, boolean isClanForce,
          CampaignOptions campaignOptions, LocalDate currentDate, Detachment detachment) {
        SkillLevel averageSkill;
        boolean isUseChaosReputation = campaignOptions.get(CampaignOption.USE_CHAOS_REPUTATION);

        if (isUseChaosReputation) {
            List<Person> personnel = detachment.getPersonnel().values().stream()
                                           .filter(person -> !person.getStatus().isDepartedUnit())
                                           .toList();
            averageSkill = ChaosReputation.getAverageSkillLevel(campaignOptions,
                  isClanForce,
                  currentDate,
                  personnel);
        } else {
            averageSkill = playerForce.getCamOpsReputation().getAverageSkillLevel();
        }

        return averageSkill;
    }

    private static void setContractTerms(ChaosObjectiveType objectiveType, ChaosEmployerType employerType,
          ChaosContract contract) {
        ContractTermsData initialContractTerms = ChaosContractDetermineTerms.determineInitialTerms(objectiveType,
              employerType);
        contract.setContractTerms(initialContractTerms);
    }

    private static void determineSchedule(Campaign campaign, boolean useVariableContractLength, LocalDate currentDate,
          PlanetarySystem currentSystem, Planet targetPlanet, FactionStandings factionStandings,
          boolean overridingCommandCircuitRequirements, boolean isGM, PlanetarySystem targetSystem,
          String employerFactionCode, ChaosObjectiveType objectiveType, ChaosContract contract) {
        // Jump Path, terminating at the specific target planet so both the journey time below and the actual travel
        // end at the world the contract is fought over rather than the system's primary world.
        JumpPath jumpPath = campaign.calculateJumpPath(currentSystem, targetSystem);
        jumpPath.setTargetPlanet(targetPlanet);
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
    }

    private static @Nonnull EnemyData pickEnemy(LocalDate currentDate, ILocation currentLocation,
          EmployerData employerData, ContractObjectiveData objectiveData, ChaosContract contract) {
        EnemyData enemyData;
        if (employerData.type() == ChaosEmployerType.CIVILIAN_ORGANIZATION_REBELS) {
            // A rebellion is fought against the rebels' own ruling power, not a randomly drawn belligerent, so the
            // enemy is fixed to the employer's territorial anchor (the local government the rebels rise against).
            enemyData = ChaosContractDeterminationEnemy.generateEnemyForFaction(employerData.getAnchorFaction(),
                  currentDate);
        } else {
            enemyData = ChaosContractDeterminationEnemy.generateEnemyFactionForObjective(currentLocation,
                  currentDate, employerData.getAnchorFaction(), objectiveData.playerObjectiveType());
        }
        contract.setEnemyData(enemyData);
        return enemyData;
    }

    private static @Nullable SystemsTargetData pickTargetLocation(ILocation currentLocation, LocalDate currentDate,
          ContractObjectiveData objectiveData, EmployerData employerData, EnemyData enemyData,
          ChaosContract contract, boolean isDefensiveObjective, boolean currentSystemEmployer) {
        // A rebellion happens where the rebels are - here, against the local ruling power - so it is always situated in
        // the current system rather than drawn from the employer/enemy border.
        boolean isRebellion = employerData.type() == ChaosEmployerType.CIVILIAN_ORGANIZATION_REBELS;
        boolean shouldAlwaysPickCurrentSystem = (isDefensiveObjective && currentSystemEmployer) || isRebellion;
        String currentSystemId = currentLocation.getCurrentSystem().getId();

        String targetSystemId = shouldAlwaysPickCurrentSystem ?
                                      currentSystemId :
                                      ChaosContractDeterminationLocation.determineContractLocation(objectiveData.playerObjectiveType(),
                                            true,
                                            employerData.anchorFactionCode(),
                                            enemyData.factionCode(),
                                            currentLocation);
        PlanetarySystem targetSystem = Systems.getInstance().getSystemById(targetSystemId);
        if (targetSystem == null) {
            LOGGER.warn("Target system {} could not be resolved. Contract generation failed.", targetSystemId);
            return null;
        }

        Collection<Planet> candidatePlanets = targetSystem.getPlanets();
        // Weight the pick toward (or, for pirate hunts, away from) strategically valuable worlds, the same way the
        // target system itself is chosen.
        Planet targetPlanet = ChaosPlanetSelector.selectTargetPlanet(candidatePlanets,
              objectiveData.playerObjectiveType().getChaosObjectiveType(),
              currentDate);
        if (targetPlanet == null) {
            LOGGER.warn("Target system {} has no planets to situate a contract on. Contract generation failed.",
                  targetSystemId);
            return null;
        }

        SystemsTargetData systemsTargetData = new SystemsTargetData(targetSystemId, targetPlanet.getId());
        contract.setSystemsTargetData(systemsTargetData);
        return systemsTargetData;
    }

    private static @Nonnull ContractObjectiveData pickObjective(int contractGenerationModifier,
          ChaosContract contract) {
        ContractObjectiveData objectiveData = ChaosContractObjectiveDetermination.determineContractObjectiveType(
              contractGenerationModifier);
        contract.setObjectiveData(objectiveData);
        return objectiveData;
    }

    private static @Nullable EmployerData pickEmployer(Campaign campaign, LocalDate currentDate,
          ILocation currentLocation, ContractSearchType searchType, ChaosContract contract) {
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
