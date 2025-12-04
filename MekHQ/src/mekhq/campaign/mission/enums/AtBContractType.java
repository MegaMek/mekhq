/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

import static java.lang.Math.round;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.mission.enums.AtBEventType.*;

import java.util.ResourceBundle;

import megamek.common.compute.Compute;
import megamek.common.eras.EraFlag;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBScenario;

public enum AtBContractType {
    // NEVER SORT THESE ENUM ENTRIES. IT WILL BREAK ATB CONTRACT GENERATION.
    GARRISON_DUTY("AtBContractType.GARRISON_DUTY.text", "AtBContractType.GARRISON_DUTY.toolTipText", 18, 1.0),
    CADRE_DUTY("AtBContractType.CADRE_DUTY.text", "AtBContractType.CADRE_DUTY.toolTipText", 12, 0.8),
    SECURITY_DUTY("AtBContractType.SECURITY_DUTY.text", "AtBContractType.SECURITY_DUTY.toolTipText", 6, 1.2),
    RIOT_DUTY("AtBContractType.RIOT_DUTY.text", "AtBContractType.RIOT_DUTY.toolTipText", 4, 1.0),
    PLANETARY_ASSAULT("AtBContractType.PLANETARY_ASSAULT.text", "AtBContractType.PLANETARY_ASSAULT.toolTipText", 9,
          1.5),
    RELIEF_DUTY("AtBContractType.RELIEF_DUTY.text", "AtBContractType.RELIEF_DUTY.toolTipText", 9, 1.4),
    GUERRILLA_WARFARE("AtBContractType.GUERRILLA_WARFARE.text", "AtBContractType.GUERRILLA_WARFARE.toolTipText", 24,
          2.1),
    PIRATE_HUNTING("AtBContractType.PIRATE_HUNTING.text", "AtBContractType.PIRATE_HUNTING.toolTipText", 6, 1.0),
    DIVERSIONARY_RAID("AtBContractType.DIVERSIONARY_RAID.text", "AtBContractType.DIVERSIONARY_RAID.toolTipText", 3,
          1.8),
    OBJECTIVE_RAID("AtBContractType.OBJECTIVE_RAID.text", "AtBContractType.OBJECTIVE_RAID.toolTipText", 3, 1.6),
    RECON_RAID("AtBContractType.RECON_RAID.text", "AtBContractType.RECON_RAID.toolTipText", 3, 1.6),
    EXTRACTION_RAID("AtBContractType.EXTRACTION_RAID.text", "AtBContractType.EXTRACTION_RAID.toolTipText", 3, 1.6),
    ASSASSINATION("AtBContractType.ASSASSINATION.text", "AtBContractType.ASSASSINATION.toolTipText", 3, 1.9),
    ESPIONAGE("AtBContractType.ESPIONAGE.text", "AtBContractType.ESPIONAGE.toolTipText", 12, 2.4),
    MOLE_HUNTING("AtBContractType.MOLE_HUNTING.text", "AtBContractType.MOLE_HUNTING.toolTipText", 6, 1.2),
    OBSERVATION_RAID("AtBContractType.OBSERVATION_RAID.text", "AtBContractType.OBSERVATION_RAID.toolTipText", 3, 1.6),
    RETAINER("AtBContractType.RETAINER.text", "AtBContractType.RETAINER.toolTipText", 12, 1.3),
    SABOTAGE("AtBContractType.SABOTAGE.text", "AtBContractType.SABOTAGE.toolTipText", 24, 2.4),
    TERRORISM("AtBContractType.TERRORISM.text", "AtBContractType.TERRORISM.toolTipText", 3, 1.9);
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final int constantLength;
    private final double operationsTempoMultiplier;
    // endregion Variable Declarations

    // region Constructors
    AtBContractType(final String name, final String toolTipText, final int constantLength,
          final double operationsTempoMultiplier) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Mission",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.constantLength = constantLength;
        this.operationsTempoMultiplier = operationsTempoMultiplier;
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    public int getConstantLength() {
        return constantLength;
    }

    public double getOperationsTempoMultiplier() {
        return operationsTempoMultiplier;
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
     * Calculates the length of the contract in months.
     *
     * <p>If variable contract lengths are enabled, the length is calculated with randomization around the base
     * contract type's standard duration. Otherwise, the constant length defined by the contract type is used.</p>
     *
     * @param useVariableContractLengths whether to use variable length calculation
     *
     * @return the calculated contract length in months
     */
    public int calculateLength(final boolean useVariableContractLengths) {
        return useVariableContractLengths ? calculateVariableLength() : getConstantLength();
    }

    /**
     * Calculates a variable contract length with randomization.
     *
     * <p>The length is calculated as 75% of the constant length plus a random variance of up to 50% of the constant
     * length. For example, a contract type with a constant length of 12 months would have a base of 9 months plus 0-6
     * months variance, resulting in a range of 9-15 months.</p>
     *
     * @return the calculated variable contract length in months
     */
    private int calculateVariableLength() {
        int baseLength = (int) round(constantLength * 0.75);
        int variance = (int) round(constantLength * 0.5);

        if (variance > 0) {
            return baseLength + randomInt(variance);
        } else {
            // If we can't determine variance return the constantLength
            return constantLength;
        }
    }

    /**
     * Determines the availability level of parts and units based on the type of operation being conducted.
     *
     * <p>The availability level is represented as an integer and varies depending on the specific
     * mission type. Higher values indicate worse availability, while lower values signify more restricted access to
     * parts.
     *
     * @return an integer representing the availability level of parts for the current mission type.
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
            case CADRE_DUTY -> CombatRole.CADRE;
            case GARRISON_DUTY, SECURITY_DUTY, RIOT_DUTY, SABOTAGE, TERRORISM, RETAINER, ASSASSINATION ->
                  CombatRole.MANEUVER;
            case GUERRILLA_WARFARE, PIRATE_HUNTING, PLANETARY_ASSAULT, RELIEF_DUTY -> CombatRole.FRONTLINE;
            case DIVERSIONARY_RAID, EXTRACTION_RAID, OBJECTIVE_RAID, RECON_RAID, OBSERVATION_RAID, MOLE_HUNTING,
                 ESPIONAGE -> CombatRole.PATROL;
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
