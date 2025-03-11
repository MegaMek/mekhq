/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.market.contractMarket;

import megamek.codeUtilities.MathUtility;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.universe.Faction;

import java.time.LocalDate;

/**
 * Structure that resolves and stores modifiers and contract terms as shown in the Master Contract
 * Terms Table on page 42 and Supplemental Contract Terms Table on page 43 of CamOps (4th printing).
 */
public class ContractTerms {
    private double operationsTempoMultiplier;
    private int baseLength;
    private double employmentMultiplier;
    private int commandModifier;
    private int salvageModifier;
    private int supportModifier;
    private int transportModifier;

    public ContractTerms(AtBContractType mission, Faction employer, double reputationFactor, LocalDate date) {
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

    /**
     * Determines the command rights based on a roll and the current CamOps command modifier.
     *
     * @param roll the result of a 2d6 roll
     * @return the type of Command rights (Integrated, House, Liaison, or Independent) after
     *         all applicable modifiers
     */
    public ContractCommandRights getCommandRights(int roll) {
        roll += commandModifier;
        ContractCommandRights commandRights; // defaults to integrated
        if (roll > 2 && roll < 8) {
            commandRights = ContractCommandRights.HOUSE;
        } else if (roll < 12) {
            commandRights = ContractCommandRights.LIAISON;
        } else {
            commandRights = ContractCommandRights.INDEPENDENT;
        }
        return commandRights;
    }

    /**
     * Determines whether the salvage rights being offered are Exchange based on a 2d6 roll and
     * the current salvage modifier for the contract.
     *
     * @param roll The result of a 2d6 roll
     * @return boolean representing whether the salvage type is exchange
     */
    public boolean isSalvageExchange(int roll) {
        roll += salvageModifier;
        return roll == 2 || roll == 3;
    }

    /**
     * Determines the percentage of salvage being offered as part of the salvage terms of a
     * contract, based on the results of a 2d6 roll and the current salvage modifier for the
     * contract.
     *
     * @param roll The result of a 2d6 roll
     * @return the salvage percentage being offered
     */
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

    /**
     * Determines the support percentage being offered as part of the support terms of a
     * contract, based on the results of a 2d6 roll and the current support modifier for the
     * contract.
     *
     * @param roll The result of a 2d6 roll
     * @return the support percentage being offered
     */
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

    /**
     * Determines whether straight support is being offered based on a 2d6 roll and
     * the current support modifier for the contract.
     *
     * @param roll The result of a 2d6 roll
     * @return boolean representing whether the Straight support type is being offered
     */
    public boolean isStraightSupport(int roll) {
        roll += supportModifier;
        return roll > 2 && roll < 8;
    }

    /**
     * Determines whether Battle Loss Compensation is being offered based on a 2d6 roll and
     * the current support modifier for the contract.
     *
     * @param roll The result of a 2d6 roll
     * @return boolean representing whether the Battle Loss Compensation support type is
     *         being offered
     */
    public boolean isBattleLossComp(int roll) {
        roll += supportModifier;
        return roll > 7;
    }

    /**
     * Determines the transport cost percentage being offered as part of the transport terms of a
     * contract, based on the results of a 2d6 roll and the current transport modifier for the
     * contract.
     *
     * @param roll The result of a 2d6 roll
     * @return the transport percentage being offered
     */
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

    private void addUnitReputationModifiers(double reputationFactor) {
        int flooredReputationFactor = (int) Math.floor(reputationFactor);

        switch(MathUtility.clamp(flooredReputationFactor, 0, 10)) {
            case 0:
                commandModifier -= 2;
                salvageModifier -= 1;
                supportModifier -= 1;
                transportModifier -= 3;
                break;
            case 1:
                commandModifier -= 1;
                salvageModifier -= 1;
                supportModifier -= 1;
                transportModifier -= 2;
                break;
            case 2:
                commandModifier -= 1;
                transportModifier -= 2;
                break;
            case 3:
                commandModifier -= 1;
                transportModifier -= 1;
                break;
            case 4:
                transportModifier -= 1;
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
