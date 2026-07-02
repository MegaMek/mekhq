package mekhq.campaign.mission.mission.contractGeneration;

import static java.lang.Math.ceil;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;

import java.time.LocalDate;
import java.util.Collection;

import mekhq.campaign.Hangar;
import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Money;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.TransportCostCalculations;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

public class ContractObjectiveTransportPay {
    public static Money calculateObjectiveTransportPay(LocalDate today, Hangar hangar,
          final Collection<Part> spareParts, final Collection<Person> allPersonnel, ILocation currentLocation,
          JumpPath jumpPath, boolean isOverridingCommandCircuitRequirements, boolean isGM,
          FactionStandings factionStandings, String employerCode) {
        boolean isUseCommandCircuit = FactionStandingUtilities.isUseCommandCircuit(
              isOverridingCommandCircuitRequirements,
              isGM,
              factionStandings,
              employerCode);

        int duration = (int) ceil(jumpPath.getTotalTime(today, currentLocation.getTransitTime(),
              isUseCommandCircuit));

        TransportCostCalculations costCalculation = new TransportCostCalculations(hangar,
              spareParts,
              allPersonnel,
              EXP_REGULAR);
        return costCalculation.calculateJumpCostForEntireJourney(duration, jumpPath.getJumps());
    }
}
