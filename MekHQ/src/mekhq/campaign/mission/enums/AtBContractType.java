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
package mekhq.campaign.mission.enums;

import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.mission.enums.AtBEventType.*;

import java.util.ResourceBundle;

import megamek.common.compute.Compute;
import megamek.common.eras.EraFlag;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.newContract.contractGeneration.ChaosObjectiveType;
import mekhq.campaign.mission.newContract.targetFinder.EnemySelectionProfile;
import mekhq.campaign.mission.newContract.targetFinder.MissionLocationProfile;

public enum AtBContractType {
    // NEVER SORT THESE ENUM ENTRIES. IT WILL BREAK ATB CONTRACT GENERATION.
    GARRISON_DUTY("AtBContractType.GARRISON_DUTY.text", "AtBContractType.GARRISON_DUTY.toolTipText",
          ChaosObjectiveType.GARRISON,
          1.0,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEFAULT),
    CADRE_DUTY("AtBContractType.CADRE_DUTY.text", "AtBContractType.CADRE_DUTY.toolTipText",
          ChaosObjectiveType.CADRE_DUTY,
          0.8,
          EnemySelectionProfile.RAIDERS,
          MissionLocationProfile.REAR_AREA),
    SECURITY_DUTY("AtBContractType.SECURITY_DUTY.text", "AtBContractType.SECURITY_DUTY.toolTipText",
          ChaosObjectiveType.GARRISON,
          1.2,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.INTERIOR_POPULATED),
    RIOT_DUTY("AtBContractType.RIOT_DUTY.text", "AtBContractType.RIOT_DUTY.toolTipText",
          ChaosObjectiveType.GARRISON,
          1.0,
          EnemySelectionProfile.REBELS,
          MissionLocationProfile.INTERIOR_POPULATED),
    PLANETARY_ASSAULT("AtBContractType.PLANETARY_ASSAULT.text", "AtBContractType.PLANETARY_ASSAULT.toolTipText",
          ChaosObjectiveType.INVASION,
          1.5,
          EnemySelectionProfile.AT_WAR,
          MissionLocationProfile.INVASION),
    RELIEF_DUTY("AtBContractType.RELIEF_DUTY.text", "AtBContractType.RELIEF_DUTY.toolTipText",
          ChaosObjectiveType.INVASION,
          1.4,
          EnemySelectionProfile.AT_WAR,
          MissionLocationProfile.DEFAULT),
    GUERRILLA_WARFARE("AtBContractType.GUERRILLA_WARFARE.text", "AtBContractType.GUERRILLA_WARFARE.toolTipText",
          ChaosObjectiveType.GUERILLA_OPERATION,
          2.1,
          EnemySelectionProfile.OCCUPYING_POWER,
          MissionLocationProfile.OCCUPIED_TERRITORY),
    PIRATE_HUNTING("AtBContractType.PIRATE_HUNTING.text", "AtBContractType.PIRATE_HUNTING.toolTipText",
          ChaosObjectiveType.PIRATE_HUNT,
          1.0,
          EnemySelectionProfile.PIRATES,
          MissionLocationProfile.DEFAULT),
    DIVERSIONARY_RAID("AtBContractType.DIVERSIONARY_RAID.text", "AtBContractType.DIVERSIONARY_RAID.toolTipText",
          ChaosObjectiveType.RAID,
          1.8,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEEP_RAID),
    OBJECTIVE_RAID("AtBContractType.OBJECTIVE_RAID.text", "AtBContractType.OBJECTIVE_RAID.toolTipText",
          ChaosObjectiveType.RAID,
          1.6,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEEP_RAID),
    RECON_RAID("AtBContractType.RECON_RAID.text", "AtBContractType.RECON_RAID.toolTipText",
          ChaosObjectiveType.RAID,
          1.6,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEEP_RAID),
    EXTRACTION_RAID("AtBContractType.EXTRACTION_RAID.text", "AtBContractType.EXTRACTION_RAID.toolTipText",
          ChaosObjectiveType.RAID,
          1.6,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEEP_RAID),
    ASSASSINATION("AtBContractType.ASSASSINATION.text", "AtBContractType.ASSASSINATION.toolTipText",
          ChaosObjectiveType.GUERILLA_OPERATION,
          1.9,
          EnemySelectionProfile.COVERT,
          MissionLocationProfile.DEEP_RAID),
    ESPIONAGE("AtBContractType.ESPIONAGE.text", "AtBContractType.ESPIONAGE.toolTipText",
          ChaosObjectiveType.EXPEDITION,
          2.4,
          EnemySelectionProfile.COVERT,
          MissionLocationProfile.HIGH_VALUE),
    MOLE_HUNTING("AtBContractType.MOLE_HUNTING.text", "AtBContractType.MOLE_HUNTING.toolTipText",
          ChaosObjectiveType.EXPEDITION,
          1.2,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEFAULT),
    OBSERVATION_RAID("AtBContractType.OBSERVATION_RAID.text", "AtBContractType.OBSERVATION_RAID.toolTipText",
          ChaosObjectiveType.RAID,
          1.6,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEEP_RAID),
    RETAINER("AtBContractType.RETAINER.text", "AtBContractType.RETAINER.toolTipText",
          ChaosObjectiveType.GARRISON,
          1.3,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.REAR_AREA),
    SABOTAGE("AtBContractType.SABOTAGE.text", "AtBContractType.SABOTAGE.toolTipText",
          ChaosObjectiveType.EXPEDITION,
          2.4,
          EnemySelectionProfile.COVERT,
          MissionLocationProfile.HIGH_VALUE),
    TERRORISM("AtBContractType.TERRORISM.text", "AtBContractType.TERRORISM.toolTipText",
          ChaosObjectiveType.EXPEDITION,
          1.9,
          EnemySelectionProfile.COVERT,
          MissionLocationProfile.HIGH_VALUE),
    UNDEFINED("AtBContractType.UNDEFINED.text", "AtBContractType.UNDEFINED.toolTipText",
          ChaosObjectiveType.RAID,
          1.0,
          EnemySelectionProfile.DEFAULT,
          MissionLocationProfile.DEFAULT);
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final double operationsTempoMultiplier;
    private final ChaosObjectiveType chaosObjectiveType;
    private final EnemySelectionProfile enemySelectionProfile;
    private final MissionLocationProfile missionLocationProfile;
    // endregion Variable Declarations

    // region Constructors
    AtBContractType(final String name, final String toolTipText, final ChaosObjectiveType chaosObjectiveType,
          final double operationsTempoMultiplier, final EnemySelectionProfile enemySelectionProfile,
          final MissionLocationProfile missionLocationProfile) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Mission",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.operationsTempoMultiplier = operationsTempoMultiplier;
        this.chaosObjectiveType = chaosObjectiveType;
        this.enemySelectionProfile = enemySelectionProfile;
        this.missionLocationProfile = missionLocationProfile;
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    public int getConstantLength() {
        return chaosObjectiveType.getMonthsLength();
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
     * <p>Values range from {@code 2} for deep EnemySelectionProfile.COVERT operations cut off behind enemy lines
     * (worst availability) down to {@code -2} (best availability). The {@code -2} value is the {@code default} branch,
     * applied to garrison-style postings and to any other contract type that lacks a more restrictive case.</p>
     *
     * @return the parts availability modifier for the current contract type, where higher values mean worse
     *       availability
     */
    public int calculatePartsAvailabilityLevel() {
        return switch (this) {
            case GUERRILLA_WARFARE, ESPIONAGE, SABOTAGE, TERRORISM -> 2;
            case DIVERSIONARY_RAID, OBJECTIVE_RAID, RECON_RAID, EXTRACTION_RAID, ASSASSINATION -> 1;
            case PLANETARY_ASSAULT, RELIEF_DUTY, MOLE_HUNTING -> 0;
            case PIRATE_HUNTING -> -1;
            default -> -2;
        };
    }

    /**
     * Determines the required combat role for the current contract type.
     *
     * <p>Each contract type specifies a primary {@link CombatRole} that defines
     * the focus of the contract. For example, some contracts may require a patrol role, while others require maneuver
     * or frontline support.</p>
     *
     * @return the {@link CombatRole} required for the current contract type.
     */
    public CombatRole getRequiredCombatRole() {
        return switch (this) {
            case GARRISON_DUTY, SECURITY_DUTY, RIOT_DUTY, RETAINER -> CombatRole.FRONTLINE;
            case CADRE_DUTY -> CombatRole.CADRE;
            case PLANETARY_ASSAULT, RELIEF_DUTY, GUERRILLA_WARFARE, DIVERSIONARY_RAID, OBJECTIVE_RAID,
                 EXTRACTION_RAID, ASSASSINATION, SABOTAGE, TERRORISM, UNDEFINED -> CombatRole.MANEUVER;
            case PIRATE_HUNTING, RECON_RAID, ESPIONAGE, MOLE_HUNTING, OBSERVATION_RAID -> CombatRole.PATROL;
        };
    }

    /**
     * Generates an event type for the campaign based on the current contract type.
     *
     * <p>This method calculates a random event, with probabilities defined by the type of contract. The result is
     * used to trigger specific in-game scenarios or effects.</p>
     *
     * <p>If StratCon is enabled the event is instead generated by the
     * {@link #generateStratConEvent()} method.</p>
     *
     * @param campaign the {@link Campaign} instance for which the event is being generated.
     *
     * @return an AtBEvent enum representing the event type.
     */
    public AtBEventType generateEventType(Campaign campaign) {
        if (campaign.getCampaignOptions().isUseStratCon()) {
            return generateStratConEvent();
        }

        final int roll = randomInt(20) + 1;

        switch (this) {
            case DIVERSIONARY_RAID:
            case OBJECTIVE_RAID:
            case RECON_RAID:
            case EXTRACTION_RAID:
                switch (roll) {
                    case 21, 20 -> {return BIG_BATTLE;}
                    case 19 -> {return SPECIAL_EVENTS;}
                    case 18 -> {return REINFORCEMENTS;}
                    case 17 -> {return LOGISTICS_FAILURE;}
                    case 16 -> {return TREACHERY;}
                    case 15, 14 -> {return BETRAYAL;}
                    case 13, 12, 11, 10 -> {return SPECIAL_SCENARIO;}
                    default -> {return BONUS_ROLL;}
                }
            case GARRISON_DUTY:
                switch (roll) {
                    case 21, 20 -> {return BIG_BATTLE;}
                    case 19 -> {return SPECIAL_EVENTS;}
                    case 18 -> {return REINFORCEMENTS;}
                    case 17 -> {return LOGISTICS_FAILURE;}
                    case 16 -> {return TREACHERY;}
                    case 15 -> {return BETRAYAL;}
                    case 14 -> {return REBELLION;}
                    case 13 -> {return SPORADIC_UPRISINGS;}
                    case 12 -> {return CIVIL_DISTURBANCE;}
                    case 11, 10, 9, 8 -> {return SPECIAL_SCENARIO;}
                    default -> {return BONUS_ROLL;}
                }
            case RIOT_DUTY:
                switch (roll) {
                    case 21, 20 -> {return BIG_BATTLE;}
                    case 19 -> {return SPECIAL_EVENTS;}
                    case 18 -> {return REINFORCEMENTS;}
                    case 17 -> {return LOGISTICS_FAILURE;}
                    case 16 -> {return TREACHERY;}
                    case 15 -> {return BETRAYAL;}
                    case 14, 13 -> {return REBELLION;}
                    case 12 -> {return SPORADIC_UPRISINGS;}
                    case 11 -> {return CIVIL_DISTURBANCE;}
                    case 10, 9, 8 -> {return SPECIAL_SCENARIO;}
                    default -> {return BONUS_ROLL;}
                }
            case PIRATE_HUNTING:
                switch (roll) {
                    case 21, 20 -> {return BIG_BATTLE;}
                    case 19 -> {return SPECIAL_EVENTS;}
                    case 18 -> {return REINFORCEMENTS;}
                    case 17 -> {return LOGISTICS_FAILURE;}
                    case 16 -> {return TREACHERY;}
                    case 15 -> {return BETRAYAL;}
                    case 14 -> {return CIVIL_DISTURBANCE;}
                    case 13, 12, 11, 10 -> {return SPECIAL_SCENARIO;}
                    default -> {return BONUS_ROLL;}
                }
            default:
                switch (roll) {
                    case 21, 20 -> {return BIG_BATTLE;}
                    case 19 -> {return SPECIAL_EVENTS;}
                    case 18 -> {return REINFORCEMENTS;}
                    case 17 -> {return LOGISTICS_FAILURE;}
                    case 16 -> {return TREACHERY;}
                    case 15 -> {return BETRAYAL;}
                    case 14, 13, 12, 11, 10 -> {return SPECIAL_SCENARIO;}
                    default -> {return BONUS_ROLL;}
                }
        }
    }

    /**
     * Generates an event type based on the current contract type.
     *
     * <p>This method is similar to {@link #generateEventType(Campaign)} but is specifically
     * tailored for StratCon-enabled campaigns. It uses a die roll to determine the resulting event, with probabilities
     * varying by contract type.</p>
     *
     * @return an integer representing the event type.
     */
    public AtBEventType generateStratConEvent() {
        final int roll = randomInt(20) + 1;

        switch (this) {
            case DIVERSIONARY_RAID, OBJECTIVE_RAID, RECON_RAID, EXTRACTION_RAID, OBSERVATION_RAID -> {
                return switch (roll) {
                    case 21, 20, 19 -> SPECIAL_EVENTS;
                    case 18 -> REINFORCEMENTS;
                    case 17 -> LOGISTICS_FAILURE;
                    case 16 -> TREACHERY;
                    case 15, 14 -> BETRAYAL;
                    default -> BONUS_ROLL;
                };
            }
            case GARRISON_DUTY, RETAINER -> {
                return switch (roll) {
                    case 21, 20, 19 -> SPECIAL_EVENTS;
                    case 18 -> REINFORCEMENTS;
                    case 17 -> LOGISTICS_FAILURE;
                    case 16 -> TREACHERY;
                    case 15 -> BETRAYAL;
                    case 14 -> REBELLION;
                    case 13 -> SPORADIC_UPRISINGS;
                    case 12 -> CIVIL_DISTURBANCE;
                    default -> BONUS_ROLL;
                };
            }
            case RIOT_DUTY -> {
                return switch (roll) {
                    case 21, 20, 19 -> SPECIAL_EVENTS;
                    case 18 -> REINFORCEMENTS;
                    case 17 -> LOGISTICS_FAILURE;
                    case 16 -> TREACHERY;
                    case 15 -> BETRAYAL;
                    case 14, 13 -> REBELLION;
                    case 12 -> SPORADIC_UPRISINGS;
                    case 11 -> CIVIL_DISTURBANCE;
                    default -> BONUS_ROLL;
                };
            }
            case PIRATE_HUNTING, MOLE_HUNTING, ASSASSINATION -> {
                return switch (roll) {
                    case 21, 20, 19 -> SPECIAL_EVENTS;
                    case 18 -> REINFORCEMENTS;
                    case 17 -> LOGISTICS_FAILURE;
                    case 16 -> TREACHERY;
                    case 15 -> BETRAYAL;
                    case 14 -> CIVIL_DISTURBANCE;
                    default -> BONUS_ROLL;
                };
            }
            default -> {
                return switch (roll) {
                    case 21, 20, 19 -> SPECIAL_EVENTS;
                    case 18 -> REINFORCEMENTS;
                    case 17 -> LOGISTICS_FAILURE;
                    case 16 -> TREACHERY;
                    case 15 -> BETRAYAL;
                    default -> BONUS_ROLL;
                };
            }
        }
    }

    public int generateSpecialScenarioType(final Campaign campaign) {
        // Our roll is era-based. If it is pre-spaceflight, early spaceflight, or Age of War there cannot be Star
        // League Caches as the Star League hasn't formed
        final int roll = randomInt(campaign.getEra().hasFlag(EraFlag.PRE_SPACEFLIGHT,
              EraFlag.EARLY_SPACEFLIGHT, EraFlag.AGE_OF_WAR) ? 12 : 20) + 1;
        return switch (this) {
            case DIVERSIONARY_RAID, OBJECTIVE_RAID, RECON_RAID, EXTRACTION_RAID -> {
                if (roll <= 1) {
                    yield AtBScenario.OFFICER_DUEL;
                } else if (roll == 2) {
                    yield AtBScenario.ACE_DUEL;
                } else if (roll <= 6) {
                    yield AtBScenario.AMBUSH;
                } else if (roll == 7) {
                    yield AtBScenario.CIVILIAN_HELP;
                } else if (roll == 8) {
                    yield AtBScenario.ALLIED_TRAITORS;
                } else if (roll <= 12) {
                    yield AtBScenario.PRISON_BREAK;
                } else if (roll <= 16) {
                    yield AtBScenario.STAR_LEAGUE_CACHE_1;
                } else {
                    yield AtBScenario.STAR_LEAGUE_CACHE_2;
                }
            }
            case GARRISON_DUTY -> {
                if (roll <= 2) {
                    yield AtBScenario.OFFICER_DUEL;
                } else if (roll <= 4) {
                    yield AtBScenario.ACE_DUEL;
                } else if (roll <= 6) {
                    yield AtBScenario.AMBUSH;
                } else if (roll <= 10) {
                    yield AtBScenario.CIVILIAN_HELP;
                } else if (roll <= 12) {
                    yield AtBScenario.ALLIED_TRAITORS;
                } else if (roll <= 16) {
                    yield AtBScenario.STAR_LEAGUE_CACHE_1;
                } else {
                    yield AtBScenario.STAR_LEAGUE_CACHE_2;
                }
            }
            case RIOT_DUTY -> {
                if (roll <= 1) {
                    yield AtBScenario.OFFICER_DUEL;
                } else if (roll <= 3) {
                    yield AtBScenario.ACE_DUEL;
                } else if (roll <= 7) {
                    yield AtBScenario.AMBUSH;
                } else if (roll == 8) {
                    yield AtBScenario.CIVILIAN_HELP;
                } else if (roll <= 12) {
                    yield AtBScenario.ALLIED_TRAITORS;
                } else if (roll <= 16) {
                    yield AtBScenario.STAR_LEAGUE_CACHE_1;
                } else {
                    yield AtBScenario.STAR_LEAGUE_CACHE_2;
                }
            }
            case PIRATE_HUNTING -> {
                if (roll <= 1) {
                    yield AtBScenario.OFFICER_DUEL;
                } else if (roll <= 4) {
                    yield AtBScenario.ACE_DUEL;
                } else if (roll <= 7) {
                    yield AtBScenario.AMBUSH;
                } else if (roll <= 11) {
                    yield AtBScenario.CIVILIAN_HELP;
                } else if (roll == 12) {
                    yield AtBScenario.ALLIED_TRAITORS;
                } else if (roll <= 16) {
                    yield AtBScenario.STAR_LEAGUE_CACHE_1;
                } else {
                    yield AtBScenario.STAR_LEAGUE_CACHE_2;
                }
            }
            default -> {
                if (roll <= 2) {
                    yield AtBScenario.OFFICER_DUEL;
                } else if (roll <= 4) {
                    yield AtBScenario.ACE_DUEL;
                } else if (roll <= 6) {
                    yield AtBScenario.AMBUSH;
                } else if (roll <= 8) {
                    yield AtBScenario.CIVILIAN_HELP;
                } else if (roll <= 10) {
                    yield AtBScenario.ALLIED_TRAITORS;
                } else if (roll <= 12) {
                    yield AtBScenario.PRISON_BREAK;
                } else if (roll <= 16) {
                    yield AtBScenario.STAR_LEAGUE_CACHE_1;
                } else {
                    yield AtBScenario.STAR_LEAGUE_CACHE_2;
                }
            }
        };
    }

    public int generateBigBattleType() {
        final int roll = Compute.d6();
        switch (this) {
            case DIVERSIONARY_RAID:
            case OBJECTIVE_RAID:
            case RECON_RAID:
            case EXTRACTION_RAID:
                if (roll <= 1) {
                    return AtBScenario.ALLY_RESCUE;
                } else if (roll == 2) {
                    return AtBScenario.CONVOY_RESCUE;
                } else if (roll <= 5) {
                    return AtBScenario.CONVOY_ATTACK;
                } else {
                    return AtBScenario.PIRATE_FREE_FOR_ALL;
                }
            case GARRISON_DUTY:
                if (roll <= 2) {
                    return AtBScenario.ALLY_RESCUE;
                } else if (roll == 3) {
                    return AtBScenario.CIVILIAN_RIOT;
                } else if (roll <= 5) {
                    return AtBScenario.CONVOY_RESCUE;
                } else {
                    return AtBScenario.PIRATE_FREE_FOR_ALL;
                }
            case RIOT_DUTY:
                if (roll <= 1) {
                    return AtBScenario.ALLY_RESCUE;
                } else if (roll <= 4) {
                    return AtBScenario.CIVILIAN_RIOT;
                } else if (roll == 5) {
                    return AtBScenario.CONVOY_RESCUE;
                } else {
                    return AtBScenario.PIRATE_FREE_FOR_ALL;
                }
            case PIRATE_HUNTING:
                if (roll <= 1) {
                    return AtBScenario.ALLY_RESCUE;
                } else if (roll <= 3) {
                    return AtBScenario.CONVOY_RESCUE;
                } else if (roll == 4) {
                    return AtBScenario.CONVOY_ATTACK;
                } else {
                    return AtBScenario.PIRATE_FREE_FOR_ALL;
                }
            default:
                if (roll <= 2) {
                    return AtBScenario.ALLY_RESCUE;
                } else if (roll == 3) {
                    return AtBScenario.CIVILIAN_RIOT;
                } else if (roll == 4) {
                    return AtBScenario.CONVOY_RESCUE;
                } else if (roll == 5) {
                    return AtBScenario.CONVOY_ATTACK;
                } else {
                    return AtBScenario.PIRATE_FREE_FOR_ALL;
                }
        }
    }

    // region File I/O

    /**
     * @param text containing the AtBContractType
     *
     * @return the saved AtBContractType
     */
    public static AtBContractType parseFromString(final String text) {
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

        MMLogger.create(AtBContractType.class)
              .error("Failed to parse text {} into an AtBContractType, returning GARRISON_DUTY.", text);

        return GARRISON_DUTY;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
