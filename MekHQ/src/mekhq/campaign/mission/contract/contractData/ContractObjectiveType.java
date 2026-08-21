/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.contract.contractData;


import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.logging.MMLogger;
import mekhq.campaign.mission.contract.contractGeneration.ChaosObjectiveType;
import mekhq.campaign.mission.contract.contractGeneration.targetFinder.EnemySelectionProfile;
import mekhq.campaign.mission.contract.contractGeneration.targetFinder.MissionLocationProfile;
import mekhq.campaign.mission.utilities.CombatRole;

public enum ContractObjectiveType {
    ASSASSINATION("ContractObjectiveType.ASSASSINATION.text", "ContractObjectiveType.ASSASSINATION.toolTipText",
          ChaosObjectiveType.GUERILLA_OPERATION,
          1.9,
          EnemySelectionProfile.COVERT,
          MissionLocationProfile.DEEP_RAID,
          CombatRole.MANEUVER),
    CADRE_DUTY("ContractObjectiveType.CADRE_DUTY.text", "ContractObjectiveType.CADRE_DUTY.toolTipText",
          ChaosObjectiveType.CADRE_DUTY,
          0.8,
          EnemySelectionProfile.RAIDERS,
          MissionLocationProfile.REAR_AREA,
          CombatRole.CADRE),
    DIVERSIONARY_RAID("ContractObjectiveType.DIVERSIONARY_RAID.text",
          "ContractObjectiveType.DIVERSIONARY_RAID.toolTipText",
          ChaosObjectiveType.RAID,
          1.8,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEEP_RAID,
          CombatRole.MANEUVER),
    ESPIONAGE("ContractObjectiveType.ESPIONAGE.text", "ContractObjectiveType.ESPIONAGE.toolTipText",
          ChaosObjectiveType.EXPEDITION,
          2.4,
          EnemySelectionProfile.COVERT,
          MissionLocationProfile.HIGH_VALUE,
          CombatRole.PATROL),
    EXTRACTION_RAID("ContractObjectiveType.EXTRACTION_RAID.text", "ContractObjectiveType.EXTRACTION_RAID.toolTipText",
          ChaosObjectiveType.RAID,
          1.6,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEEP_RAID,
          CombatRole.MANEUVER),
    GARRISON_DUTY("ContractObjectiveType.GARRISON_DUTY.text", "ContractObjectiveType.GARRISON_DUTY.toolTipText",
          ChaosObjectiveType.GARRISON,
          1.0,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEFAULT,
          CombatRole.FRONTLINE),
    GUERRILLA_WARFARE("ContractObjectiveType.GUERRILLA_WARFARE.text",
          "ContractObjectiveType.GUERRILLA_WARFARE.toolTipText",
          ChaosObjectiveType.GUERILLA_OPERATION,
          2.1,
          EnemySelectionProfile.OCCUPYING_POWER,
          MissionLocationProfile.OCCUPIED_TERRITORY,
          CombatRole.MANEUVER),
    MOLE_HUNTING("ContractObjectiveType.MOLE_HUNTING.text", "ContractObjectiveType.MOLE_HUNTING.toolTipText",
          ChaosObjectiveType.EXPEDITION,
          1.2,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEFAULT,
          CombatRole.PATROL),
    OBJECTIVE_RAID("ContractObjectiveType.OBJECTIVE_RAID.text", "ContractObjectiveType.OBJECTIVE_RAID.toolTipText",
          ChaosObjectiveType.RAID,
          1.6,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEEP_RAID,
          CombatRole.MANEUVER),
    OBSERVATION_RAID("ContractObjectiveType.OBSERVATION_RAID.text",
          "ContractObjectiveType.OBSERVATION_RAID.toolTipText",
          ChaosObjectiveType.RAID,
          1.6,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEEP_RAID,
          CombatRole.PATROL),
    PIRATE_HUNTING("ContractObjectiveType.PIRATE_HUNTING.text", "ContractObjectiveType.PIRATE_HUNTING.toolTipText",
          ChaosObjectiveType.PIRATE_HUNT,
          1.0,
          EnemySelectionProfile.PIRATES,
          MissionLocationProfile.DEFAULT,
          CombatRole.PATROL),
    PLANETARY_ASSAULT("ContractObjectiveType.PLANETARY_ASSAULT.text",
          "ContractObjectiveType.PLANETARY_ASSAULT.toolTipText",
          ChaosObjectiveType.INVASION,
          1.5,
          EnemySelectionProfile.AT_WAR,
          MissionLocationProfile.INVASION,
          CombatRole.FRONTLINE),
    RECON_RAID("ContractObjectiveType.RECON_RAID.text", "ContractObjectiveType.RECON_RAID.toolTipText",
          ChaosObjectiveType.RAID,
          1.6,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEEP_RAID,
          CombatRole.MANEUVER),
    RELIEF_DUTY("ContractObjectiveType.RELIEF_DUTY.text", "ContractObjectiveType.RELIEF_DUTY.toolTipText",
          ChaosObjectiveType.INVASION,
          1.4,
          EnemySelectionProfile.AT_WAR,
          MissionLocationProfile.DEFAULT,
          CombatRole.MANEUVER),
    RETAINER("ContractObjectiveType.RETAINER.text", "ContractObjectiveType.RETAINER.toolTipText",
          ChaosObjectiveType.GARRISON,
          1.3,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.REAR_AREA,
          CombatRole.FRONTLINE),
    RIOT_DUTY("ContractObjectiveType.RIOT_DUTY.text", "ContractObjectiveType.RIOT_DUTY.toolTipText",
          ChaosObjectiveType.GARRISON,
          1.0,
          EnemySelectionProfile.REBELS,
          MissionLocationProfile.INTERIOR_POPULATED,
          CombatRole.MANEUVER),
    SABOTAGE("ContractObjectiveType.SABOTAGE.text", "ContractObjectiveType.SABOTAGE.toolTipText",
          ChaosObjectiveType.EXPEDITION,
          2.4,
          EnemySelectionProfile.COVERT,
          MissionLocationProfile.HIGH_VALUE,
          CombatRole.MANEUVER),
    SECURITY_DUTY("ContractObjectiveType.SECURITY_DUTY.text", "ContractObjectiveType.SECURITY_DUTY.toolTipText",
          ChaosObjectiveType.GARRISON,
          1.2,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.INTERIOR_POPULATED,
          CombatRole.FRONTLINE),
    TERRORISM("ContractObjectiveType.TERRORISM.text", "ContractObjectiveType.TERRORISM.toolTipText",
          ChaosObjectiveType.EXPEDITION,
          1.9,
          EnemySelectionProfile.COVERT,
          MissionLocationProfile.HIGH_VALUE,
          CombatRole.MANEUVER),
    UNDEFINED("ContractObjectiveType.UNDEFINED.text", "ContractObjectiveType.UNDEFINED.toolTipText",
          ChaosObjectiveType.RAID,
          1.0,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEFAULT,
          CombatRole.FRONTLINE);
    // endregion Enum Declarations

    // region Variable Declarations
    private final static String RESOURCE_BUNDLE = "mekhq.resources.Mission";

    private final String name;
    private final String toolTipText;
    private final double operationsTempoMultiplier;
    private final ChaosObjectiveType chaosObjectiveType;
    private final EnemySelectionProfile enemySelectionProfile;
    private final MissionLocationProfile missionLocationProfile;
    private final CombatRole requiredCombatRole;
    // endregion Variable Declarations

    // region Constructors
    ContractObjectiveType(final String name, final String toolTipText, final ChaosObjectiveType chaosObjectiveType,
          final double operationsTempoMultiplier, final EnemySelectionProfile enemySelectionProfile,
          final MissionLocationProfile missionLocationProfile, final CombatRole requiredCombatRole) {
        this.name = getTextAt(RESOURCE_BUNDLE, name);
        this.toolTipText = getTextAt(RESOURCE_BUNDLE, toolTipText);
        this.operationsTempoMultiplier = operationsTempoMultiplier;
        this.chaosObjectiveType = chaosObjectiveType;
        this.enemySelectionProfile = enemySelectionProfile;
        this.missionLocationProfile = missionLocationProfile;
        this.requiredCombatRole = requiredCombatRole;
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    public ChaosObjectiveType getChaosObjectiveType() {
        return chaosObjectiveType;
    }

    public double getOperationsTempoMultiplier() {
        return operationsTempoMultiplier;
    }

    public EnemySelectionProfile getEnemySelectionProfile() {
        return enemySelectionProfile;
    }

    public MissionLocationProfile getMissionLocationProfile() {
        return missionLocationProfile;
    }

    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isGarrisonDuty() {
        return this == GARRISON_DUTY;
    }

    public boolean isCadreDuty() {
        return this == CADRE_DUTY;
    }

    public boolean isSecurityDuty() {
        return this == SECURITY_DUTY;
    }

    public boolean isRiotDuty() {
        return this == RIOT_DUTY;
    }

    public boolean isPlanetaryAssault() {
        return this == PLANETARY_ASSAULT;
    }

    public boolean isReliefDuty() {
        return this == RELIEF_DUTY;
    }

    public boolean isGuerrillaWarfare() {
        return this == GUERRILLA_WARFARE;
    }

    public boolean isPirateHunting() {
        return this == PIRATE_HUNTING;
    }

    public boolean isDiversionaryRaid() {
        return this == DIVERSIONARY_RAID;
    }

    public boolean isObjectiveRaid() {
        return this == OBJECTIVE_RAID;
    }

    public boolean isReconRaid() {
        return this == RECON_RAID;
    }

    public boolean isExtractionRaid() {
        return this == EXTRACTION_RAID;
    }

    public boolean isAssassination() {
        return this == ASSASSINATION;
    }

    public boolean isEspionage() {
        return this == ESPIONAGE;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean isMoleHunting() {
        return this == MOLE_HUNTING;
    }

    public boolean isRetainer() {
        return this == RETAINER;
    }

    public boolean isSabotage() {
        return this == SABOTAGE;
    }

    public boolean isTerrorism() {
        return this == TERRORISM;
    }

    public boolean isObservationRaid() {
        return this == OBSERVATION_RAID;
    }

    public boolean isGarrisonType() {
        return isGarrisonDuty() || isCadreDuty() || isSecurityDuty() || isRiotDuty() || isRetainer();
    }

    public boolean isRaidType() {
        return isDiversionaryRaid() ||
                     isObjectiveRaid() ||
                     isReconRaid() ||
                     isExtractionRaid() ||
                     isObservationRaid() ||
                     isAssassination();
    }

    public boolean isGuerrillaType() {
        return isGuerrillaWarfare() || isTerrorism() || isSabotage() || isEspionage();
    }
    // endregion Boolean Comparison Methods

    /**
     * Determines the parts availability level for the current contract type.
     *
     * <p>The returned value is a modifier added to the target number of procurement checks, so a higher value makes
     * parts harder to acquire and a lower (negative) value makes them easier. In other words, higher is worse. The
     * modifier is only applied when StratCon and "restrict parts by mission" are both enabled.</p>
     *
     * @return the parts availability modifier for the current contract type, where higher values mean worse
     *       availability
     */
    public int calculatePartsAvailabilityLevel() {
        return chaosObjectiveType.getProcurementTargetNumberModifier();
    }

    /**
     * @return the {@link CombatRole} required for the current contract type.
     */
    public CombatRole getRequiredCombatRole() {
        return requiredCombatRole;
    }

    // region File I/O

    /**
     * @param text containing the ContractObjectiveType
     *
     * @return the saved ContractObjectiveType
     */
    public static ContractObjectiveType parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return GARRISON_DUTY;
                case 1:
                    return CADRE_DUTY;
                case 2:
                    return SECURITY_DUTY;
                case 3:
                    return RIOT_DUTY;
                case 4:
                    return PLANETARY_ASSAULT;
                case 5:
                    return RELIEF_DUTY;
                case 6:
                    return GUERRILLA_WARFARE;
                case 7:
                    return PIRATE_HUNTING;
                case 8:
                    return DIVERSIONARY_RAID;
                case 9:
                    return OBJECTIVE_RAID;
                case 10:
                    return RECON_RAID;
                case 11:
                    return EXTRACTION_RAID;
                case 12:
                    return ASSASSINATION;
                case 13:
                    return ESPIONAGE;
                case 14:
                    return MOLE_HUNTING;
                case 15:
                    return OBSERVATION_RAID;
                case 16:
                    return RETAINER;
                case 17:
                    return SABOTAGE;
                case 18:
                    return TERRORISM;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        MMLogger.create(ContractObjectiveType.class)
              .error("Failed to parse text {} into an ContractObjectiveType, returning GARRISON_DUTY.", text);

        return GARRISON_DUTY;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
