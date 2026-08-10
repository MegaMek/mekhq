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
package mekhq.campaign.mission.newContract;

import static java.lang.Math.ceil;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.annotation.Nullable;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

public class ContractUtilities {
    public static int getTravelDays(Campaign campaign, AbstractContract abstractContract,
          AbstractLocation currentLocation,
          boolean isGM, boolean isOverridingCommandCircuitRequirements, FactionStandings factionStandings,
          String employerFactionCode) {
        boolean isUseCommandCircuit = FactionStandingUtilities.isUseCommandCircuit(
              isOverridingCommandCircuitRequirements,
              isGM,
              factionStandings,
              employerFactionCode);

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
}
