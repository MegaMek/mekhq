/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

package mekhq.campaign.mission.utilities;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.force.FormationType.STANDARD;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.annotation.Nullable;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

public class ContractUtilities {
    /**
     * The portion of combat teams we expect to be performing combat actions. This is one in 'x' where 'x' is the value
     * set here.
     */
    static final double BASE_VARIANCE_FACTOR = 0.7;

    /**
     * Calculates the number of lances used for this contract, based on [campaign].
     *
     * @param campaign       The campaign to reference.
     * @param isCadreDuty    {@code true} if {@link CombatRole#CADRE} should be considered a combat role
     * @param bypassVariance a flag indicating whether variance adjustments should be bypassed
     * @param varianceFactor the degree of variance to apply to required combat elements
     *
     * @return The number of lances required.
     */
    public static int calculateBaseNumberOfRequiredLances(Campaign campaign, boolean isCadreDuty,
          boolean bypassVariance, double varianceFactor) {
        int combatForceCount = 0;
        for (CombatTeam combatTeam : campaign.getPlayerForce().getCombatTeamsAsList(campaign)) {
            if (0 >= combatTeam.getSize(campaign)) { // Don't count empty combat teams (or warship-only)
                continue;
            }

            Formation formation = combatTeam.getFormation(campaign);
            if (formation == null) {
                continue;
            }

            CombatRole roleInMemory = formation.getCombatRoleInMemory();
            boolean hasCombatRole = roleInMemory.isCombatRole() || (isCadreDuty && roleInMemory.isCadre());
            if (formation.isFormationType(STANDARD) && hasCombatRole) {
                combatForceCount++;
            }
        }

        if (bypassVariance) {
            return max(combatForceCount, 1);
        } else {
            return (int) ceil(max(combatForceCount * varianceFactor, 1));
        }
    }

    /**
     * Calculates the number of units required for this contract, based on [campaign].
     *
     * @param campaign The campaign to reference.
     *
     * @return The number of combat units present.
     */
    public static int calculateBaseNumberOfUnitsRequiredInCombatTeams(Campaign campaign) {
        return max(getEffectiveNumUnits(campaign), 1);
    }

    /**
     * Calculates the effective number of units available in the given campaign based on unit types and roles.
     *
     * <p>
     * This method iterates through all combat teams in the specified campaign, ignoring combat teams with the auxiliary
     * role. For each valid combat team, it retrieves the associated force and evaluates all units within that force.
     * The unit contribution to the total is determined based on its type. See {@link CombatTeam#getSize(Campaign)}
     *
     * <p>
     * Units that aren’t associated with a valid combat team or can’t be fetched due to missing data are ignored. The
     * final result is returned as an integer by flooring the calculated total.
     * </p>
     *
     * @param campaign the campaign containing the combat teams and units to evaluate
     *
     * @return the effective number of units as an integer
     */
    public static int getEffectiveNumUnits(Campaign campaign) {
        double numUnits = 0;
        for (CombatTeam combatTeam : campaign.getPlayerForce().getCombatTeamsAsList(campaign)) {
            Formation formation = combatTeam.getFormation(campaign);

            if (formation == null) {
                continue;
            }

            if (!formation.isFormationType(STANDARD)) {
                continue;
            }

            numUnits += combatTeam.getSize(campaign);
        }

        return (int) floor(numUnits);
    }

    /**
     * Calculates the variance factor based on the given roll value and a fixed formation size divisor.
     *
     * <p>
     * The variance factor is determined by applying a multiplier to the fixed formation size divisor. The multiplier
     * varies based on the roll value:
     * <ul>
     *   <li><b>Roll 2:</b> Multiplier is 0.575.</li>
     *   <li><b>Roll 3:</b> Multiplier is 0.6.</li>
     *   <li><b>Roll 4:</b> Multiplier is 0.625</li>
     *   <li><b>Roll 5:</b> Multiplier is 0.65.</li>
     *   <li><b>Roll 6:</b> Multiplier is 0.675.</li>
     *   <li><b>Roll 7:</b> Multiplier is 0.7.</li>
     *   <li><b>Roll 8:</b> Multiplier is 0.725.</li>
     *   <li><b>Roll 9:</b> Multiplier is 0.75.</li>
     *   <li><b>Roll 10:</b> Multiplier is 0.775.</li>
     *   <li><b>Roll 11:</b> Multiplier is 0.8.</li>
     *   <li><b>Roll 12:</b> Multiplier is 0.825.</li>
     * </ul>
     *
     * @return the calculated variance factor as a double
     */
    public static double calculateVarianceFactor() {
        int roll = d6(2);
        return switch (roll) {
            case 2 -> BASE_VARIANCE_FACTOR - 0.125;
            case 3 -> BASE_VARIANCE_FACTOR - 0.1;
            case 4 -> BASE_VARIANCE_FACTOR - 0.075;
            case 5 -> BASE_VARIANCE_FACTOR - 0.05;
            case 6 -> BASE_VARIANCE_FACTOR - 0.025;
            case 8 -> BASE_VARIANCE_FACTOR + 0.025;
            case 9 -> BASE_VARIANCE_FACTOR + 0.05;
            case 10 -> BASE_VARIANCE_FACTOR + 0.075;
            case 11 -> BASE_VARIANCE_FACTOR + 0.1;
            case 12 -> BASE_VARIANCE_FACTOR + 0.125;
            default -> BASE_VARIANCE_FACTOR; // 0.7
        };
    }

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


    public static String getEnemyDisplayNameIncludingFaction(Faction enemyFaction, String enemyDisplayName,
          int gameYear) {
        String factionFullName = enemyFaction.getFullName(gameYear);
        if (!factionFullName.equals(enemyDisplayName)) {
            return enemyDisplayName + " (" + factionFullName + ")";
        }

        return enemyDisplayName;
    }
}
