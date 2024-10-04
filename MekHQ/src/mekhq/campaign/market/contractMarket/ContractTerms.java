/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.contractMarket;

import megamek.codeUtilities.MathUtility;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.universe.Faction;

import java.time.LocalDate;

/**
 * Structure that resolves and stores modifiers and contract terms as shown in the Master Contract
 * Terms Table on page 42 of CamOps (4th printing).
 */
public class ContractTerms {
    private double operationsTempoMultiplier;
    private int baseLength;
    private double employmentMultiplier;
    private int commandModifier;
    private int salvageModifier;
    private int supportModifier;
    private int transportModifier;

    public ContractTerms(AtBContractType mission, Faction employer, int reputationFactor, LocalDate date) {
        operationsTempoMultiplier = mission.getOperationsTempoMultiplier();
        baseLength = mission.getConstantLength();
        addMissionTypeModifiers(mission);
        addEmployerModifiers(employer, date);
        addUnitReputationModifiers(reputationFactor);
    }

    public double getOperationsTempoMultiplier() {
        return operationsTempoMultiplier;
    }

    public int getBaseLength() {
        return baseLength;
    }

    public double getEmploymentMultiplier() {
        return employmentMultiplier;
    }

    public ContractCommandRights getCommandRights(int roll) {
        roll += commandModifier;
        ContractCommandRights commandRights;
        if (roll > 2 && roll < 8) {
            commandRights = ContractCommandRights.HOUSE;
        } else if (roll < 12) {
            commandRights = ContractCommandRights.LIAISON;
        } else {
            commandRights = ContractCommandRights.INDEPENDENT;
        }
        return commandRights;
    }

    public boolean isSalvageExchange(int roll) {
        roll += salvageModifier;
        return roll == 2 || roll == 3;
    }

    public int getSalvagePercentage(int roll) {
        roll += salvageModifier;
        return switch (MathUtility.clamp(roll, 3, 13)) {
            case 3 -> 0;
            case 4 -> 10;
            case 5 -> 20;
            case 6 -> 30;
            case 7 -> 40;
            case 8 -> 50;
            case 9 -> 60;
            case 10 -> 70;
            case 11 -> 80;
            case 12 -> 90;
            default -> 100;
        };
    }

    public int getSupportPercentage(int roll) {
        roll += supportModifier;
        return switch(MathUtility.clamp(roll, 2, 13)) {
            case 2 -> 0;
            case 3, 9 -> 20;
            case 4, 10 -> 40;
            case 5, 11 -> 60;
            case 6, 12 -> 80;
            case 8 -> 10;
            default -> 100;
        };
    }

    public boolean isStraightSupport(int roll) {
        roll += supportModifier;
        return roll > 2 && roll < 8;
    }

    public boolean isBattleLossComp(int roll) {
        roll += supportModifier;
        return roll > 7;
    }

    public int getTransportTerms(int roll) {
        roll += transportModifier;
        return switch (MathUtility.clamp(roll, 1, 10)) {
            case 1 -> 0;
            case 2 -> 20;
            case 3 -> 25;
            case 4 -> 30;
            case 5 -> 35;
            case 6 -> 45;
            case 7 -> 50;
            case 8 -> 55;
            case 9 -> 60;
            default -> 100;
        };
    }

    private void addMissionTypeModifiers(AtBContractType mission) {
        switch (mission) {
            case CADRE_DUTY -> supportModifier += 1;
            case DIVERSIONARY_RAID -> {
                salvageModifier += 2;
                supportModifier += 2;
                transportModifier += 1;
            }
            case EXTRACTION_RAID -> {
                commandModifier += -1;
                salvageModifier += -1;
                supportModifier += 2;
                transportModifier += 1;
            }
            case GARRISON_DUTY -> {
                commandModifier += 1;
                supportModifier += 1;
            }
            case GUERRILLA_WARFARE -> {
                commandModifier += -2;
                salvageModifier += 3;
                supportModifier += -2;
                transportModifier += -1;
            }
            case OBJECTIVE_RAID -> {
                commandModifier += -1;
                supportModifier += 1;
                transportModifier += 2;
            }
            case PIRATE_HUNTING -> {
                commandModifier += 2;
                salvageModifier += 2;
                supportModifier += -1;
                transportModifier += -1;
            }
            case PLANETARY_ASSAULT -> {
                commandModifier += -2;
                supportModifier += 2;
                transportModifier += 3;
            }
            case RECON_RAID -> {
                commandModifier += -1;
                salvageModifier += -2;
                supportModifier += 1;
                transportModifier += -1;
            }
            case RELIEF_DUTY -> {
                commandModifier += -1;
                salvageModifier += 1;
                supportModifier += 1;
                transportModifier += 1;
            }
            case RIOT_DUTY -> {
                commandModifier += -2;
                salvageModifier += 1;
                supportModifier += 2;
            }
            case SECURITY_DUTY -> {
                commandModifier += -3;
                supportModifier += 2;
                transportModifier += 1;
            }
        }
    }

    private void addEmployerModifiers(Faction employer, LocalDate date) {
        employmentMultiplier = 1.0;
        if (employer.isSuperPower()) {
            employmentMultiplier += 0.3;
            supportModifier += 1;
            transportModifier += 2;
        } else if (employer.isMajorPower()) {
            employmentMultiplier += 0.2;
            salvageModifier += -1;
            transportModifier += 1;
        } else if (employer.isMinorPower()) {
            employmentMultiplier += 0.1;
            salvageModifier += -2;
        } else if (employer.isCorporation() || employer.isMercenary()) {
            employmentMultiplier += 0.1;
            commandModifier += -1;
            salvageModifier += 2;
            supportModifier += 1;
            transportModifier += 1;
        } else if (employer.isIndependent() || employer.isPlanetaryGovt()) {
            salvageModifier += -1;
            supportModifier += -1;
        }
        if (employer.isStingy()) {
            employmentMultiplier += -0.2;
            salvageModifier += -1;
            supportModifier += -1;
            transportModifier += -1;
        } else if (employer.isGenerous()) {
            employmentMultiplier += 0.2;
            salvageModifier += 1;
            supportModifier += 2;
            transportModifier += 1;
        }
        if (employer.isControlling()) {
            commandModifier += -2;
            salvageModifier += -1;
        } else if (employer.isLenient()) {
            commandModifier += 1;
            salvageModifier += 1;
        }
        if (date.getYear() < 2781 || date.getYear() > 3062) {
            salvageModifier += -2;
        }
    }

    private void addUnitReputationModifiers(int reputationFactor) {
        switch(MathUtility.clamp(reputationFactor, 0, 10)) {
            case 0:
                commandModifier += -2;
                salvageModifier += -1;
                supportModifier += -1;
                transportModifier += -3;
                break;
            case 1:
                commandModifier += -1;
                salvageModifier += -1;
                supportModifier += -1;
                transportModifier += -2;
                break;
            case 2:
                commandModifier += -1;
                transportModifier += -2;
                break;
            case 3:
                commandModifier += -1;
                transportModifier += -1;
                break;
            case 4:
                transportModifier += -1;
                break;
            case 6:
            case 7:
                commandModifier += 1;
                salvageModifier += 1;
                break;
            case 8:
                commandModifier += 1;
                salvageModifier += 1;
                supportModifier += 1;
                break;
            case 9:
                commandModifier += 2;
                salvageModifier += 2;
                supportModifier += 1;
                transportModifier += 1;
                break;
            default:
                commandModifier += 3;
                salvageModifier += 2;
                supportModifier += 2;
                transportModifier += 2;
                break;

        }
    }
}