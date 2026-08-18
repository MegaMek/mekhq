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
package mekhq.campaign.mission.newContract.utilities;

import static java.lang.Math.ceil;
import static java.lang.Math.max;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.annotation.Nullable;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.mission.newContract.AbstractContract;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

public class ContractUtilities {
    /**
     * Whether a force at {@code location} has arrived at where {@code contract} is fought.
     *
     * <p>Arrival is judged on the system first, since that is what the location model has always tracked, and then on
     * the world - but only when both worlds are known. A location from a save predating planet tracking knows only its
     * system, and {@link AbstractLocation#getPlanet()} would report the system's primary world; comparing that against
     * a contract targeting some other world would say "not arrived" forever.</p>
     *
     * @param location the force's location
     * @param contract the contract whose target is being tested against
     *
     * @return {@code true} when the force is in the contract's target system, out of transit, and - where known - at
     *       its target world
     */
    public static boolean hasArrivedAtContractLocation(AbstractLocation location, AbstractContract contract) {
        if (!Objects.equals(location.getCurrentSystem(), contract.getTargetSystem()) || !location.isOnPlanet()) {
            return false;
        }

        Planet knownPlanet = location.getCurrentPlanetDirect();
        Planet targetPlanet = contract.getTargetPlanet();
        return (knownPlanet == null) || (targetPlanet == null) || Objects.equals(knownPlanet, targetPlanet);
    }

    public static int getTravelDays(Campaign campaign, AbstractContract abstractContract,
          AbstractLocation currentLocation, boolean isOverridingCommandCircuitRequirements,
          FactionStandings factionStandings) {
        boolean isGM = campaign.isGM();
        boolean isUseCommandCircuit = FactionStandingUtilities.isUseCommandCircuit(
              isOverridingCommandCircuitRequirements,
              isGM,
              factionStandings,
              abstractContract.getEmployerFactionCode());

        JumpPath jumpPath = getJumpPath(campaign, abstractContract, currentLocation);

        if (jumpPath != null) {
            LocalDate currentDate = campaign.getLocalDate();
            double transitTime = currentLocation.getTransitTime();
            return (int) ceil(jumpPath.getTotalTime(currentDate, transitTime, isUseCommandCircuit));
        }

        return 0;
    }

    public static @Nullable JumpPath getJumpPath(Campaign campaign, AbstractContract abstractContract,
          AbstractLocation currentLocation) {
        // if we don't have a cached jump path, or if the jump path's starting/ending point no longer match the
        // campaign's current location or contract's destination
        JumpPath cachedJumpPath = abstractContract.getCachedJumpPathDirect();
        PlanetarySystem targetSystem = abstractContract.getTargetSystem();
        PlanetarySystem currentSystem = currentLocation.getCurrentSystem();
        if (targetSystem == null) {
            return refreshJumpPath(campaign, abstractContract, currentSystem, null, null);
        }

        Planet targetPlanet = abstractContract.getTargetPlanet();

        if (cachedJumpPath == null ||
                  cachedJumpPath.isEmpty() ||
                  !Objects.equals(cachedJumpPath.getFirstSystem(), currentSystem) ||
                  !Objects.equals(cachedJumpPath.getTargetPlanet(), targetPlanet)) {
            return refreshJumpPath(campaign, abstractContract, currentSystem, targetSystem, targetPlanet);
        }

        return cachedJumpPath;
    }

    private static JumpPath refreshJumpPath(Campaign campaign, AbstractContract abstractContract,
          PlanetarySystem currentSystem, PlanetarySystem targetSystem, @Nullable Planet targetPlanet) {
        JumpPath jumpPath = campaign.calculateJumpPath(currentSystem, targetSystem);

        if (jumpPath != null) {
            jumpPath.setTargetPlanet(targetPlanet);
        }

        abstractContract.setCachedJumpPath(jumpPath);

        return jumpPath;
    }

    /**
     * @param contract an active AtBContract
     *
     * @return the current deployment deficit for the contract
     */
    public static int getDeploymentDeficit(Campaign campaign, AbstractContract contract) {
        LocalDate currentDate = campaign.getLocalDate();
        if (!contract.isActiveOn(currentDate) || currentDate.equals(contract.getStartDate())) {
            // Do not check for deficits if the contract has not started, or
            // it is the first day of the contract, as players won't have
            // had time to assign forces to the contract yet
            return 0;
        }

        int total = -contract.getRequiredCombatElements();
        int role = -max(1, contract.getRequiredCombatElements() / 2);

        final CombatRole requiredLanceRole = contract.getObjectiveType().getRequiredCombatRole();
        for (CombatTeam combatTeam : campaign.getPlayerForce().getCombatTeamsMap().values()) {
            CombatRole combatRole = combatTeam.getRole();

            if (!combatRole.isReserve() && !combatRole.isAuxiliary()) {
                if (Objects.equals(combatTeam.getMissionId(), contract.getId())) {
                    if (!combatRole.isTraining()) {
                        if (!combatRole.isCadre() || contract.getObjectiveType().isCadreDuty()) {
                            total += combatTeam.getSize(campaign);
                        }
                    }
                }

                if (combatRole == requiredLanceRole) {
                    role += combatTeam.getSize(campaign);
                }
            }
        }

        if (total >= 0 && role >= 0) {
            return 0;
        }
        return Math.abs(Math.min(total, role));
    }
}
