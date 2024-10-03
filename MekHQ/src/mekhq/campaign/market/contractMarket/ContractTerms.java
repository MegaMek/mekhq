package mekhq.campaign.market.contractMarket;

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
        if (roll < 4) {
            return 0;
        } else if (roll == 4) {
            return 10;
        } else if (roll == 5) {
            return 20;
        } else if (roll == 6) {
            return 30;
        } else if (roll == 7) {
            return 40;
        } else if (roll == 8) {
            return 50;
        } else if (roll == 9) {
            return 60;
        } else if (roll == 10) {
            return 70;
        } else if (roll == 11) {
            return 80;
        } else if (roll == 12) {
            return 90;
        } else {
            return 100;
        }
    }

    public int getSupportPercentage(int roll) {
        roll += supportModifier;
        int percentage = 0;
        if (roll == 3 || roll == 9) {
            percentage = 20;
        } else if (roll == 4 || roll == 10) {
            percentage = 40;
        } else if (roll == 5 || roll == 11) {
            percentage = 60;
        } else if (roll == 6 || roll == 12) {
            percentage = 80;
        } else if (roll == 7 || roll > 12) {
            percentage = 100;
        }
        return percentage;
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
        if (roll < 2) {
            return 0;
        } else if (roll == 2) {
            return 20;
        } else if (roll == 3) {
            return 25;
        } else if (roll == 4) {
            return 30;
        } else if (roll == 5) {
            return 35;
        } else if (roll == 6) {
            return 45;
        } else if (roll == 7) {
            return 50;
        } else if (roll == 8) {
            return 55;
        } else if (roll == 9) {
            return 60;
        } else {
            return 100;
        }
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
        }
        if (employer.isGenerous()) {
            employmentMultiplier += 0.2;
            salvageModifier += 1;
            supportModifier += 2;
            transportModifier += 1;
        }
        if (employer.isControlling()) {
            commandModifier += -2;
            salvageModifier += -1;
        }
        if (employer.isLenient()) {
            commandModifier += 1;
            salvageModifier += 1;
        }
        if (date.getYear() < 2781 || date.getYear() > 3062) {
            salvageModifier += -2;
        }
    }

    private void addUnitReputationModifiers(int reputationFactor) {
        if (reputationFactor <= 0) {
            commandModifier += -2;
            salvageModifier += -1;
            supportModifier += -1;
            transportModifier += -3;
        } else if (reputationFactor == 1) {
            commandModifier += -1;
            salvageModifier += -1;
            supportModifier += -1;
            transportModifier += -2;
        } else if (reputationFactor == 2) {
            commandModifier += -1;
            transportModifier += -2;
        } else if (reputationFactor == 3) {
            commandModifier += -1;
            transportModifier += -1;
        } else if (reputationFactor == 4) {
            transportModifier += -1;
        } else if (reputationFactor == 6 || reputationFactor == 7) {
            commandModifier += 1;
            salvageModifier += 1;
        } else if (reputationFactor == 8) {
            commandModifier += 1;
            salvageModifier += 1;
            supportModifier += 1;
        } else if (reputationFactor == 9) {
            commandModifier += 2;
            salvageModifier += 2;
            supportModifier += 1;
            transportModifier += 1;
        } else {
            commandModifier += 3;
            salvageModifier += 2;
            supportModifier += 2;
            transportModifier += 2;
        }
    }
}