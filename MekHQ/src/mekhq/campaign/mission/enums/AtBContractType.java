/*
 * Copyright (c) 2021-2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission.enums;

import megamek.common.Compute;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.universe.enums.EraFlag;

import java.util.ResourceBundle;

import static mekhq.campaign.mission.enums.AtBEventType.*;

public enum AtBContractType {
    // TODO: Missing Camops Mission Types: ASSASSINATION, ESPIONAGE, MOLE_HUNTING, OBSERVATION_RAID,
    //  RETAINER, SABOTAGE, TERRORISM, HIGH_RISK
    // region Enum Declarations
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
    EXTRACTION_RAID("AtBContractType.EXTRACTION_RAID.text", "AtBContractType.EXTRACTION_RAID.toolTipText", 3, 1.6);
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

    public boolean isGarrisonType() {
        return isGarrisonDuty() || isCadreDuty() || isSecurityDuty() || isRiotDuty();
    }

    public boolean isRaidType() {
        return isDiversionaryRaid() || isObjectiveRaid() || isReconRaid() || isExtractionRaid();
    }
    // endregion Boolean Comparison Methods

    public int calculateLength(final boolean variable, final AtBContract contract) {
        return variable ? calculateVariableLength(contract) : getConstantLength();
    }

    private int calculateVariableLength(final AtBContract contract) {
        switch (this) {
            case CADRE_DUTY:
            case SECURITY_DUTY:
                return 4;
            case GARRISON_DUTY:
                return 9 + Compute.d6(3);
            case DIVERSIONARY_RAID:
            case RECON_RAID:
                return 1;
            case EXTRACTION_RAID:
                return 1 + contract.getEnemySkill().ordinal();
            case OBJECTIVE_RAID:
            case PIRATE_HUNTING:
                return 3 + Compute.randomInt(3);
            case PLANETARY_ASSAULT:
            case RELIEF_DUTY:
                return 4 + Compute.randomInt(3);
            case GUERRILLA_WARFARE:
            case RIOT_DUTY:
                return 6;
            default:
                return 12;
        }
    }

    /**
     * Determines the availability level of parts and units based on the type of operation being
     * conducted.
     *
     * <p>The availability level is represented as an integer and varies depending on the specific
     * mission type. Higher values indicate worse availability, while lower values signify more
     * restricted access to parts.
     *
     * @return an integer representing the availability level of parts for the current mission type.
     */
    public int calculatePartsAvailabilityLevel() {
        return switch (this) {
            case GUERRILLA_WARFARE -> 2;
            case DIVERSIONARY_RAID, OBJECTIVE_RAID, RECON_RAID, EXTRACTION_RAID -> 1;
            case PLANETARY_ASSAULT, RELIEF_DUTY -> 0;
            case PIRATE_HUNTING -> -1;
            default -> -2;
        };
    }

    /**
     * Determines the required combat role for the current contract type.
     *
     * <p>Each contract type specifies a primary {@link CombatRole} that defines
     * the focus of the contract. For example, some contracts may require a patrol role,
     * while others require maneuver or frontline support.</p>
     *
     * @return the {@link CombatRole} required for the current contract type.
     */
    public CombatRole getRequiredCombatRole() {
        return switch (this) {
            case CADRE_DUTY -> CombatRole.TRAINING;
            case GARRISON_DUTY, SECURITY_DUTY, RIOT_DUTY -> CombatRole.MANEUVER;
            case GUERRILLA_WARFARE, PIRATE_HUNTING, PLANETARY_ASSAULT, RELIEF_DUTY -> CombatRole.FRONTLINE;
            case DIVERSIONARY_RAID, EXTRACTION_RAID, OBJECTIVE_RAID, RECON_RAID -> CombatRole.PATROL;
        };
    }

    /**
     * Generates an event type for the campaign based on the current contract type.
     *
     * <p>This method calculates a random event, with probabilities defined by
     * the type of contract. The result is used to trigger specific in-game scenarios or effects.</p>
     *
     * <p>If StratCon is enabled the event is instead generated by the
     * {@link #generateStratConEvent()} method.</p>
     *
     * @param campaign the {@link Campaign} instance for which the event is being generated.
     * @return an AtBEvent enum representing the event type.
     */
    public AtBEventType generateEventType(Campaign campaign) {
        if (campaign.getCampaignOptions().isUseStratCon()) {
            return generateStratConEvent();
        }

        final int roll = Compute.randomInt(20) + 1;

        switch (this) {
            case DIVERSIONARY_RAID:
            case OBJECTIVE_RAID:
            case RECON_RAID:
            case EXTRACTION_RAID:
                switch (roll) {
                    case 21, 20 -> { return BIGBATTLE; }
                    case 19 -> { return SPECIALEVENTS; }
                    case 18 -> { return REINFORCEMENTS; }
                    case 17 -> { return LOGISTICSFAILURE; }
                    case 16 -> { return TREACHERY; }
                    case 15, 14 -> { return BETRAYAL; }
                    case 13, 12, 11, 10 -> { return SPECIAL_SCENARIO; }
                    default -> { return BONUSROLL; }
                }
            case GARRISON_DUTY:
                switch (roll) {
                    case 21, 20 -> { return BIGBATTLE; }
                    case 19 -> { return SPECIALEVENTS; }
                    case 18 -> { return REINFORCEMENTS; }
                    case 17 -> { return LOGISTICSFAILURE; }
                    case 16 -> { return TREACHERY; }
                    case 15 -> { return BETRAYAL; }
                    case 14 -> { return REBELLION; }
                    case 13 -> { return SPORADICUPRISINGS; }
                    case 12 -> { return CIVILDISTURBANCE; }
                    case 11, 10, 9, 8 -> { return SPECIAL_SCENARIO; }
                    default -> { return BONUSROLL; }
                }
            case RIOT_DUTY:
                switch (roll) {
                    case 21, 20 -> { return BIGBATTLE; }
                    case 19 -> { return SPECIALEVENTS; }
                    case 18 -> { return REINFORCEMENTS; }
                    case 17 -> { return LOGISTICSFAILURE; }
                    case 16 -> { return TREACHERY; }
                    case 15 -> { return BETRAYAL; }
                    case 14, 13 -> { return REBELLION; }
                    case 12 -> { return SPORADICUPRISINGS; }
                    case 11 -> { return CIVILDISTURBANCE; }
                    case 10, 9, 8 -> { return SPECIAL_SCENARIO; }
                    default -> { return BONUSROLL; }
                }
            case PIRATE_HUNTING:
                switch (roll) {
                    case 21, 20 -> { return BIGBATTLE; }
                    case 19 -> { return SPECIALEVENTS; }
                    case 18 -> { return REINFORCEMENTS; }
                    case 17 -> { return LOGISTICSFAILURE; }
                    case 16 -> { return TREACHERY; }
                    case 15 -> { return BETRAYAL; }
                    case 14 -> { return CIVILDISTURBANCE; }
                    case 13, 12, 11, 10 -> { return SPECIAL_SCENARIO; }
                    default -> { return BONUSROLL; }
                }
            default:
                switch (roll) {
                    case 21, 20 -> { return BIGBATTLE; }
                    case 19 -> { return SPECIALEVENTS; }
                    case 18 -> { return REINFORCEMENTS; }
                    case 17 -> { return LOGISTICSFAILURE; }
                    case 16 -> { return TREACHERY; }
                    case 15 -> { return BETRAYAL; }
                    case 14, 13, 12, 11, 10 -> { return SPECIAL_SCENARIO; }
                    default -> { return BONUSROLL; }
                }
        }
    }

    /**
     * Generates an event type based on the current contract type.
     *
     * <p>This method is similar to {@link #generateEventType(Campaign)} but is specifically
     * tailored for StratCon-enabled campaigns. It uses a die roll to determine the
     * resulting event, with probabilities varying by contract type.</p>
     *
     * @return an integer representing the event type.
     */
    public AtBEventType generateStratConEvent() {
        final int roll = Compute.randomInt(20) + 1;

        switch (this) {
            case DIVERSIONARY_RAID, OBJECTIVE_RAID, RECON_RAID, EXTRACTION_RAID -> {
                return switch (roll) {
                    case 21, 20, 19 -> SPECIALEVENTS;
                    case 18 -> REINFORCEMENTS;
                    case 17 -> LOGISTICSFAILURE;
                    case 16 -> TREACHERY;
                    case 15, 14 -> BETRAYAL;
                    default -> BONUSROLL;
                };
            }
            case GARRISON_DUTY -> {
                return switch (roll) {
                    case 21, 20, 19 -> SPECIALEVENTS;
                    case 18 -> REINFORCEMENTS;
                    case 17 -> LOGISTICSFAILURE;
                    case 16 -> TREACHERY;
                    case 15 -> BETRAYAL;
                    case 14 -> REBELLION;
                    case 13 -> SPORADICUPRISINGS;
                    case 12 -> CIVILDISTURBANCE;
                    default -> BONUSROLL;
                };
            }
            case RIOT_DUTY -> {
                return switch (roll) {
                    case 21, 20, 19 -> SPECIALEVENTS;
                    case 18 -> REINFORCEMENTS;
                    case 17 -> LOGISTICSFAILURE;
                    case 16 -> TREACHERY;
                    case 15 -> BETRAYAL;
                    case 14, 13 -> REBELLION;
                    case 12 -> SPORADICUPRISINGS;
                    case 11 -> CIVILDISTURBANCE;
                    default -> BONUSROLL;
                };
            }
            case PIRATE_HUNTING -> {
                return switch (roll) {
                    case 21, 20, 19 -> SPECIALEVENTS;
                    case 18 -> REINFORCEMENTS;
                    case 17 -> LOGISTICSFAILURE;
                    case 16 -> TREACHERY;
                    case 15 -> BETRAYAL;
                    case 14 -> CIVILDISTURBANCE;
                    default -> BONUSROLL;
                };
            }
            default -> {
                return switch (roll) {
                    case 21, 20, 19 -> SPECIALEVENTS;
                    case 18 -> REINFORCEMENTS;
                    case 17 -> LOGISTICSFAILURE;
                    case 16 -> TREACHERY;
                    case 15 -> BETRAYAL;
                    default -> BONUSROLL;
                };
            }
        }
    }

    public int generateSpecialScenarioType(final Campaign campaign) {
        // Our roll is era-based. If it is pre-spaceflight, early spaceflight, or Age of
        // War there
        // cannot be Star League Caches as the Star League hasn't formed
        final int roll = Compute.randomInt(campaign.getEra().hasFlag(EraFlag.PRE_SPACEFLIGHT,
                EraFlag.EARLY_SPACEFLIGHT, EraFlag.AGE_OF_WAR) ? 12 : 20) + 1;
        return switch (this) {
            case DIVERSIONARY_RAID, OBJECTIVE_RAID, RECON_RAID, EXTRACTION_RAID -> {
                if (roll <= 1) {
                    yield AtBScenario.OFFICERDUEL;
                } else if (roll == 2) {
                    yield AtBScenario.ACEDUEL;
                } else if (roll <= 6) {
                    yield AtBScenario.AMBUSH;
                } else if (roll == 7) {
                    yield AtBScenario.CIVILIANHELP;
                } else if (roll == 8) {
                    yield AtBScenario.ALLIEDTRAITORS;
                } else if (roll <= 12) {
                    yield AtBScenario.PRISONBREAK;
                } else if (roll <= 16) {
                    yield AtBScenario.STARLEAGUECACHE1;
                } else {
                    yield AtBScenario.STARLEAGUECACHE2;
                }
            }
            case GARRISON_DUTY -> {
                if (roll <= 2) {
                    yield AtBScenario.OFFICERDUEL;
                } else if (roll <= 4) {
                    yield AtBScenario.ACEDUEL;
                } else if (roll <= 6) {
                    yield AtBScenario.AMBUSH;
                } else if (roll <= 10) {
                    yield AtBScenario.CIVILIANHELP;
                } else if (roll <= 12) {
                    yield AtBScenario.ALLIEDTRAITORS;
                } else if (roll <= 16) {
                    yield AtBScenario.STARLEAGUECACHE1;
                } else {
                    yield AtBScenario.STARLEAGUECACHE2;
                }
            }
            case RIOT_DUTY -> {
                if (roll <= 1) {
                    yield AtBScenario.OFFICERDUEL;
                } else if (roll <= 3) {
                    yield AtBScenario.ACEDUEL;
                } else if (roll <= 7) {
                    yield AtBScenario.AMBUSH;
                } else if (roll == 8) {
                    yield AtBScenario.CIVILIANHELP;
                } else if (roll <= 12) {
                    yield AtBScenario.ALLIEDTRAITORS;
                } else if (roll <= 16) {
                    yield AtBScenario.STARLEAGUECACHE1;
                } else {
                    yield AtBScenario.STARLEAGUECACHE2;
                }
            }
            case PIRATE_HUNTING -> {
                if (roll <= 1) {
                    yield AtBScenario.OFFICERDUEL;
                } else if (roll <= 4) {
                    yield AtBScenario.ACEDUEL;
                } else if (roll <= 7) {
                    yield AtBScenario.AMBUSH;
                } else if (roll <= 11) {
                    yield AtBScenario.CIVILIANHELP;
                } else if (roll == 12) {
                    yield AtBScenario.ALLIEDTRAITORS;
                } else if (roll <= 16) {
                    yield AtBScenario.STARLEAGUECACHE1;
                } else {
                    yield AtBScenario.STARLEAGUECACHE2;
                }
            }
            default -> {
                if (roll <= 2) {
                    yield AtBScenario.OFFICERDUEL;
                } else if (roll <= 4) {
                    yield AtBScenario.ACEDUEL;
                } else if (roll <= 6) {
                    yield AtBScenario.AMBUSH;
                } else if (roll <= 8) {
                    yield AtBScenario.CIVILIANHELP;
                } else if (roll <= 10) {
                    yield AtBScenario.ALLIEDTRAITORS;
                } else if (roll <= 12) {
                    yield AtBScenario.PRISONBREAK;
                } else if (roll <= 16) {
                    yield AtBScenario.STARLEAGUECACHE1;
                } else {
                    yield AtBScenario.STARLEAGUECACHE2;
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
                    return AtBScenario.ALLYRESCUE;
                } else if (roll <= 2) {
                    return AtBScenario.CONVOYRESCUE;
                } else if (roll <= 5) {
                    return AtBScenario.CONVOYATTACK;
                } else {
                    return AtBScenario.PIRATEFREEFORALL;
                }
            case GARRISON_DUTY:
                if (roll <= 2) {
                    return AtBScenario.ALLYRESCUE;
                } else if (roll <= 3) {
                    return AtBScenario.CIVILIANRIOT;
                } else if (roll <= 5) {
                    return AtBScenario.CONVOYRESCUE;
                } else {
                    return AtBScenario.PIRATEFREEFORALL;
                }
            case RIOT_DUTY:
                if (roll <= 1) {
                    return AtBScenario.ALLYRESCUE;
                } else if (roll <= 4) {
                    return AtBScenario.CIVILIANRIOT;
                } else if (roll <= 5) {
                    return AtBScenario.CONVOYRESCUE;
                } else {
                    return AtBScenario.PIRATEFREEFORALL;
                }
            case PIRATE_HUNTING:
                if (roll <= 1) {
                    return AtBScenario.ALLYRESCUE;
                } else if (roll <= 3) {
                    return AtBScenario.CONVOYRESCUE;
                } else if (roll <= 4) {
                    return AtBScenario.CONVOYATTACK;
                } else {
                    return AtBScenario.PIRATEFREEFORALL;
                }
            default:
                if (roll <= 2) {
                    return AtBScenario.ALLYRESCUE;
                } else if (roll <= 3) {
                    return AtBScenario.CIVILIANRIOT;
                } else if (roll <= 4) {
                    return AtBScenario.CONVOYRESCUE;
                } else if (roll <= 5) {
                    return AtBScenario.CONVOYATTACK;
                } else {
                    return AtBScenario.PIRATEFREEFORALL;
                }
        }
    }

    // region File I/O
    /**
     * @param text containing the AtBContractType
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
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        MMLogger.create(AtBContractType.class)
                .error("Failed to parse text " + text + " into an AtBContractType, returning GARRISON_DUTY.");

        return GARRISON_DUTY;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
