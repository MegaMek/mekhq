/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.universe.enums.EraFlag;
import org.apache.logging.log4j.LogManager;

import java.util.ResourceBundle;

public enum AtBContractType {
    //region Enum Declarations
    GARRISON_DUTY("AtBContractType.GARRISON_DUTY.text", "AtBContractType.GARRISON_DUTY.toolTipText", 18, 1.0),
    CADRE_DUTY("AtBContractType.CADRE_DUTY.text", "AtBContractType.CADRE_DUTY.toolTipText", 12, 0.8),
    SECURITY_DUTY("AtBContractType.SECURITY_DUTY.text", "AtBContractType.SECURITY_DUTY.toolTipText", 6, 1.2),
    RIOT_DUTY("AtBContractType.RIOT_DUTY.text", "AtBContractType.RIOT_DUTY.toolTipText", 4, 1.0),
    PLANETARY_ASSAULT("AtBContractType.PLANETARY_ASSAULT.text", "AtBContractType.PLANETARY_ASSAULT.toolTipText", 9, 1.5),
    RELIEF_DUTY("AtBContractType.RELIEF_DUTY.text", "AtBContractType.RELIEF_DUTY.toolTipText", 9, 1.4),
    GUERRILLA_WARFARE("AtBContractType.GUERRILLA_WARFARE.text", "AtBContractType.GUERRILLA_WARFARE.toolTipText", 24, 2.1),
    PIRATE_HUNTING("AtBContractType.PIRATE_HUNTING.text", "AtBContractType.PIRATE_HUNTING.toolTipText", 6, 1.0),
    DIVERSIONARY_RAID("AtBContractType.DIVERSIONARY_RAID.text", "AtBContractType.DIVERSIONARY_RAID.toolTipText", 3, 1.8),
    OBJECTIVE_RAID("AtBContractType.OBJECTIVE_RAID.text", "AtBContractType.OBJECTIVE_RAID.toolTipText", 3, 1.6),
    RECON_RAID("AtBContractType.RECON_RAID.text", "AtBContractType.RECON_RAID.toolTipText", 3, 1.6),
    EXTRACTION_RAID("AtBContractType.EXTRACTION_RAID.text", "AtBContractType.EXTRACTION_RAID.toolTipText", 3, 1.6);
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final int constantLength;
    private final double paymentMultiplier;
    //endregion Variable Declarations

    //region Constructors
    AtBContractType(final String name, final String toolTipText, final int constantLength,
                    final double paymentMultiplier) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Mission",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.constantLength = constantLength;
        this.paymentMultiplier = paymentMultiplier;
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    public int getConstantLength() {
        return constantLength;
    }

    public double getPaymentMultiplier() {
        return paymentMultiplier;
    }
    //endregion Getters

    //region Boolean Comparison Methods
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
    //endregion Boolean Comparison Methods

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
     * AtB Rules apply an additional -1 from 2950 to 3040, which is superseded by MekHQ's era
     * variation code
     */
    public int calculatePartsAvailabilityLevel() {
        switch (this) {
            case GUERRILLA_WARFARE:
                return 0;
            case DIVERSIONARY_RAID:
            case OBJECTIVE_RAID:
            case RECON_RAID:
            case EXTRACTION_RAID:
                return 1;
            case PLANETARY_ASSAULT:
            case RELIEF_DUTY:
                return 2;
            case PIRATE_HUNTING:
                return 3;
            default:
                return 4;
        }
    }

    public AtBLanceRole getRequiredLanceRole() {
        switch (this) {
            case CADRE_DUTY:
                return AtBLanceRole.TRAINING;
            case GARRISON_DUTY:
            case SECURITY_DUTY:
            case RIOT_DUTY:
                return AtBLanceRole.DEFENCE;
            case GUERRILLA_WARFARE:
            case PIRATE_HUNTING:
            case PLANETARY_ASSAULT:
            case RELIEF_DUTY:
                return AtBLanceRole.FIGHTING;
            case DIVERSIONARY_RAID:
            case EXTRACTION_RAID:
            case OBJECTIVE_RAID:
            case RECON_RAID:
                return AtBLanceRole.SCOUTING;
            default:
                return AtBLanceRole.UNASSIGNED;
        }
    }

    public int getFatigue() {
        switch (this) {
            case GARRISON_DUTY:
            case SECURITY_DUTY:
            case CADRE_DUTY:
                return -1;
            case RIOT_DUTY:
            case PIRATE_HUNTING:
                return 1;
            case DIVERSIONARY_RAID:
            case EXTRACTION_RAID:
            case RECON_RAID:
            case RELIEF_DUTY:
            case OBJECTIVE_RAID:
                return 2;
            case GUERRILLA_WARFARE:
            case PLANETARY_ASSAULT:
                return 3;
            default:
                return 0;
        }
    }
    public int generateEventType() {
        final int roll = Compute.randomInt(20) + 1;

        switch (this) {
            case DIVERSIONARY_RAID:
            case OBJECTIVE_RAID:
            case RECON_RAID:
            case EXTRACTION_RAID:
                if (roll < 10) {
                    return AtBContract.EVT_BONUSROLL;
                } else if (roll < 14) {
                    return AtBContract.EVT_SPECIAL_SCENARIO;
                } else if (roll < 16) {
                    return AtBContract.EVT_BETRAYAL;
                } else if (roll < 17) {
                    return AtBContract.EVT_TREACHERY;
                } else if (roll < 18) {
                    return AtBContract.EVT_LOGISTICSFAILURE;
                } else if (roll < 19) {
                    return AtBContract.EVT_REINFORCEMENTS;
                } else if (roll < 20) {
                    return AtBContract.EVT_SPECIALEVENTS;
                } else {
                    return AtBContract.EVT_BIGBATTLE;
                }
            case GARRISON_DUTY:
                if (roll < 8) {
                    return AtBContract.EVT_BONUSROLL;
                } else if (roll < 12) {
                    return AtBContract.EVT_SPECIAL_SCENARIO;
                } else if (roll < 13) {
                    return AtBContract.EVT_CIVILDISTURBANCE;
                } else if (roll < 14) {
                    return AtBContract.EVT_SPORADICUPRISINGS;
                } else if (roll < 15) {
                    return AtBContract.EVT_REBELLION;
                } else if (roll < 16) {
                    return AtBContract.EVT_BETRAYAL;
                } else if (roll < 17) {
                    return AtBContract.EVT_TREACHERY;
                } else if (roll < 18) {
                    return AtBContract.EVT_LOGISTICSFAILURE;
                } else if (roll < 19) {
                    return AtBContract.EVT_REINFORCEMENTS;
                } else if (roll < 20) {
                    return AtBContract.EVT_SPECIALEVENTS;
                } else {
                    return AtBContract.EVT_BIGBATTLE;
                }
            case RIOT_DUTY:
                if (roll < 8) {
                    return AtBContract.EVT_BONUSROLL;
                } else if (roll < 11) {
                    return AtBContract.EVT_SPECIAL_SCENARIO;
                } else if (roll < 12) {
                    return AtBContract.EVT_CIVILDISTURBANCE;
                } else if (roll < 13) {
                    return AtBContract.EVT_SPORADICUPRISINGS;
                } else if (roll < 15) {
                    return AtBContract.EVT_REBELLION;
                } else if (roll < 16) {
                    return AtBContract.EVT_BETRAYAL;
                } else if (roll < 17) {
                    return AtBContract.EVT_TREACHERY;
                } else if (roll < 18) {
                    return AtBContract.EVT_LOGISTICSFAILURE;
                } else if (roll < 19) {
                    return AtBContract.EVT_REINFORCEMENTS;
                } else if (roll < 20) {
                    return AtBContract.EVT_SPECIALEVENTS;
                } else {
                    return AtBContract.EVT_BIGBATTLE;
                }
            case PIRATE_HUNTING:
                if (roll < 10) {
                    return AtBContract.EVT_BONUSROLL;
                } else if (roll < 14) {
                    return AtBContract.EVT_SPECIAL_SCENARIO;
                } else if (roll < 15) {
                    return AtBContract.EVT_CIVILDISTURBANCE;
                } else if (roll < 16) {
                    return AtBContract.EVT_BETRAYAL;
                } else if (roll < 17) {
                    return AtBContract.EVT_TREACHERY;
                } else if (roll < 18) {
                    return AtBContract.EVT_LOGISTICSFAILURE;
                } else if (roll < 19) {
                    return AtBContract.EVT_REINFORCEMENTS;
                } else if (roll < 20) {
                    return AtBContract.EVT_SPECIALEVENTS;
                } else {
                    return AtBContract.EVT_BIGBATTLE;
                }
            default:
                if (roll < 10) {
                    return AtBContract.EVT_BONUSROLL;
                } else if (roll < 15) {
                    return AtBContract.EVT_SPECIAL_SCENARIO;
                } else if (roll < 16) {
                    return AtBContract.EVT_BETRAYAL;
                } else if (roll < 17) {
                    return AtBContract.EVT_TREACHERY;
                } else if (roll < 18) {
                    return AtBContract.EVT_LOGISTICSFAILURE;
                } else if (roll < 19) {
                    return AtBContract.EVT_REINFORCEMENTS;
                } else if (roll < 20) {
                    return AtBContract.EVT_SPECIALEVENTS;
                } else {
                    return AtBContract.EVT_BIGBATTLE;
                }
        }
    }

    public int generateSpecialScenarioType(final Campaign campaign) {
        // Our roll is era-based. If it is pre-spaceflight, early spaceflight, or Age of War there
        // cannot be Star League Caches as the Star League hasn't formed
        final int roll = Compute.randomInt(campaign.getEra().hasFlag(EraFlag.PRE_SPACEFLIGHT,
                EraFlag.EARLY_SPACEFLIGHT, EraFlag.AGE_OF_WAR) ? 12 : 20) + 1;
        switch (this) {
            case DIVERSIONARY_RAID:
            case OBJECTIVE_RAID:
            case RECON_RAID:
            case EXTRACTION_RAID:
                if (roll <= 1) {
                    return AtBScenario.OFFICERDUEL;
                } else if (roll <= 2) {
                    return AtBScenario.ACEDUEL;
                } else if (roll <= 6) {
                    return AtBScenario.AMBUSH;
                } else if (roll <= 7) {
                    return AtBScenario.CIVILIANHELP;
                } else if (roll <= 8) {
                    return AtBScenario.ALLIEDTRAITORS;
                } else if (roll <= 12) {
                    return AtBScenario.PRISONBREAK;
                } else if (roll <= 16) {
                    return AtBScenario.STARLEAGUECACHE1;
                } else {
                    return AtBScenario.STARLEAGUECACHE2;
                }
            case GARRISON_DUTY:
                if (roll <= 2) {
                    return AtBScenario.OFFICERDUEL;
                } else if (roll <= 4) {
                    return AtBScenario.ACEDUEL;
                } else if (roll <= 6) {
                    return AtBScenario.AMBUSH;
                } else if (roll <= 10) {
                    return AtBScenario.CIVILIANHELP;
                } else if (roll <= 12) {
                    return AtBScenario.ALLIEDTRAITORS;
                } else if (roll <= 16) {
                    return AtBScenario.STARLEAGUECACHE1;
                } else {
                    return AtBScenario.STARLEAGUECACHE2;
                }
            case RIOT_DUTY:
                if (roll <= 1) {
                    return AtBScenario.OFFICERDUEL;
                } else if (roll <= 3) {
                    return AtBScenario.ACEDUEL;
                } else if (roll <= 7) {
                    return AtBScenario.AMBUSH;
                } else if (roll <= 8) {
                    return AtBScenario.CIVILIANHELP;
                } else if (roll <= 12) {
                    return AtBScenario.ALLIEDTRAITORS;
                } else if (roll <= 16) {
                    return AtBScenario.STARLEAGUECACHE1;
                } else {
                    return AtBScenario.STARLEAGUECACHE2;
                }
            case PIRATE_HUNTING:
                if (roll <= 1) {
                    return AtBScenario.OFFICERDUEL;
                } else if (roll <= 4) {
                    return AtBScenario.ACEDUEL;
                } else if (roll <= 7) {
                    return AtBScenario.AMBUSH;
                } else if (roll <= 11) {
                    return AtBScenario.CIVILIANHELP;
                } else if (roll <= 12) {
                    return AtBScenario.ALLIEDTRAITORS;
                } else if (roll <= 16) {
                    return AtBScenario.STARLEAGUECACHE1;
                } else {
                    return AtBScenario.STARLEAGUECACHE2;
                }
            default:
                if (roll <= 2) {
                    return AtBScenario.OFFICERDUEL;
                } else if (roll <= 4) {
                    return AtBScenario.ACEDUEL;
                } else if (roll <= 6) {
                    return AtBScenario.AMBUSH;
                } else if (roll <= 8) {
                    return AtBScenario.CIVILIANHELP;
                } else if (roll <= 10) {
                    return AtBScenario.ALLIEDTRAITORS;
                } else if (roll <= 12) {
                    return AtBScenario.PRISONBREAK;
                } else if (roll <= 16) {
                    return AtBScenario.STARLEAGUECACHE1;
                } else {
                    return AtBScenario.STARLEAGUECACHE2;
                }
        }
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

    //region File I/O
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

        LogManager.getLogger().error("Failed to parse text " + text + " into an AtBContractType, returning GARRISON_DUTY.");

        return GARRISON_DUTY;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
