package mekhq.campaign.mission.mission.contractGeneration;

import jakarta.annotation.Nullable;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RandomFactionGenerator;

public class DetermineContractLocation {
    public static @Nullable String generateContractLocation(Faction employer, Faction enemy) {
        String employerCode = employer.getShortName();
        String enemyCode = enemy.getShortName();
        return RandomFactionGenerator.getInstance().getMissionTarget(employerCode, enemyCode);
    }
}
