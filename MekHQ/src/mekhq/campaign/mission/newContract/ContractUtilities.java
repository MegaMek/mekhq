package mekhq.campaign.mission.newContract;

import static java.lang.Math.ceil;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

public class ContractUtilities {
    public static int getTravelDays(Campaign campaign, AbstractContract abstractContract,
          CurrentLocation currentLocation,
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
          CurrentLocation currentLocation) {
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
