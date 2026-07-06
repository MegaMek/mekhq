package mekhq.campaign.mission.newContract.contractGeneration;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.campaign.universe.Faction;

public record EmployerFactionSelection(
      @Nonnull Faction employerFaction,
      @Nonnull GlobalEmployerTableValue globalEmployerTableValue,
      @Nullable IndependentEmployerTableValue independentEmployerTableValue
) {
}
