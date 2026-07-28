package mekhq.campaign.mission.newContract.contractGeneration;

import mekhq.campaign.mission.newContract.targetFinder.EnemySelectionProfile;
import mekhq.campaign.mission.newContract.targetFinder.MissionLocationProfile;

public enum ChaosObjectiveType {
    EXPEDITION(EnemySelectionProfile.COVERT, MissionLocationProfile.HIGH_VALUE),
    PIRATE_HUNT(EnemySelectionProfile.PIRATES, MissionLocationProfile.DEFAULT),
    GUERILLA_OPERATION(EnemySelectionProfile.OCCUPYING_POWER, MissionLocationProfile.OCCUPIED_TERRITORY),
    GARRISON(EnemySelectionProfile.DEFAULT, MissionLocationProfile.DEFAULT),
    CADRE_DUTY(EnemySelectionProfile.RAIDERS, MissionLocationProfile.REAR_AREA),
    RAID(EnemySelectionProfile.DEFAULT, MissionLocationProfile.DEEP_RAID),
    INVASION(EnemySelectionProfile.AT_WAR, MissionLocationProfile.INVASION),
    PIRATE_RAID(EnemySelectionProfile.DEFAULT, MissionLocationProfile.DEEP_RAID);

    private final EnemySelectionProfile enemySelectionProfile;
    private final MissionLocationProfile missionLocationProfile;

    ChaosObjectiveType(final EnemySelectionProfile enemySelectionProfile,
          final MissionLocationProfile missionLocationProfile) {
        this.enemySelectionProfile = enemySelectionProfile;
        this.missionLocationProfile = missionLocationProfile;
    }

    public EnemySelectionProfile getEnemySelectionProfile() {
        return enemySelectionProfile;
    }

    public MissionLocationProfile getMissionLocationProfile() {
        return missionLocationProfile;
    }
}
