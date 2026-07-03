package mekhq.campaign.mission.mission.contractGeneration;

import mekhq.campaign.universe.Faction;

public record EmployerFactionSelection(Faction employerFaction, GlobalEmployerTableValue globalEmployerTableValue,
      IndependentEmployerTableValue independentEmployerTableValue) {
}
