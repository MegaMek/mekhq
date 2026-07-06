package mekhq.campaign.mission.newContract.contractGeneration;

import jakarta.annotation.Nullable;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.newContract.MissionLocationProfile;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RandomFactionGenerator;

public class DetermineContractLocation {
    public static @Nullable String generateContractLocation(Faction employer, Faction enemy,
          ILocation location, AtBContractType firstObjective) {
        String employerCode = employer.getShortName();
        String enemyCode = enemy.getShortName();

        MissionLocationProfile profile = MissionLocationProfile.fromContractType(firstObjective);
        return RandomFactionGenerator.getInstance().getMissionTarget(employerCode, enemyCode, location, profile);
    }
}
