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
import java.util.Collection;

import jakarta.annotation.Nullable;
import megamek.codeUtilities.ObjectUtility;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.newContract.ChaosContract;
import mekhq.campaign.mission.newContract.contractData.ContractObjectiveData;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;

public class NormalContractGeneration extends AbstractContractGeneration {
    public static @Nullable ChaosContract createChaosContract(CampaignOptions campaignOptions, LocalDate currentDate,
          ILocation currentLocation,
          boolean isMercenarySearch, int contractGenerationModifier) {
        final ChaosContract contract = new ChaosContract();

        // Step 1: Employer
        EmployerGenerationData employerGenerationData =
              ChaosContractEmployerDetermination.getEmployerGenerationData(currentDate,
                    currentLocation,
                    isMercenarySearch);
        if (employerGenerationData.faction() == null) {
            // No employer means no contract
            return null;
        }

        ChaosEmployerType employerType = employerGenerationData.type();

        // Step 2: Type
        ContractObjectiveData objectiveData = ChaosContractObjectiveDetermination.determineContractObjectiveType(
              contractGenerationModifier);
        ChaosObjectiveType objectiveType = objectiveData.playerObjectiveType().getChaosObjectiveType();

        // Step 3: Enemy
        Faction enemyFaction = ChaosContractDeterminationEnemy.generateEnemyFactionForObjective(currentLocation,
              currentDate, employerGenerationData.faction(), objectiveData.playerObjectiveType());

        // Step 4: Location
        String targetSystemId = ChaosContractDeterminationLocation.determineContractLocation(objectiveData.playerObjectiveType(),
              true,
              employerGenerationData.faction().getShortName(),
              enemyFaction.getShortName(),
              currentLocation);
        PlanetarySystem targetSystem = Systems.getInstance().getSystemById(targetSystemId);
        Collection<Planet> candidatePlanets = targetSystem.getPlanets();
        // TODO use same planetary picker profile as System picker
        Planet targetPlanet = ObjectUtility.getRandomItem(candidatePlanets);

        // Step 5: Length
        boolean useVariableContractLength = campaignOptions.get(CampaignOption.VARIABLE_CONTRACT_LENGTH);
        int monthsLength = objectiveType.calculateLength(useVariableContractLength);

        // Step 6: Initial Terms
        ContractTermsData initialContractTerms = ChaosContractDetermineTerms.determineInitialTerms(objectiveType,
              employerType);

        // Step 7: Track Count & Intensity
        int trackCount = ChaosContractDetermineIntensity.determineTrackCount(objectiveType);

        // Step 8: Return
        return contract;
    }
}
