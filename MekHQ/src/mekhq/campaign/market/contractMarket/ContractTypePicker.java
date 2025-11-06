package mekhq.campaign.market.contractMarket;

import static megamek.common.compute.Compute.d6;

import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.universe.Faction;

public class ContractTypePicker {
    public static AtBContractType findMissionType(Faction employer) {
        // I took some liberties here with what faction rolls on what table, as the options were fairly limited. The 
        // below gives a much better variance among the factions.
        if (employer.isClan()) {
            return clanTable();
        } else if (employer.isPirate()) {
            return pirateTable();
        } else if (employer.isMajorPower()) {
            return innerSphereTable();
        } else if (employer.isCorporation() || employer.isRebel() || employer.isComStarOrWoB()) {
            return corporationTable();
        } else {
            return independentTable();
        }
    }

    private static AtBContractType clanTable() {
        int roll = d6(2);
        while (roll < 4 || roll > 11) {
            roll = d6(2);
        }

        return switch (roll) {
            case 4 -> AtBContractType.PIRATE_HUNTING;
            case 5 -> AtBContractType.PLANETARY_ASSAULT;
            case 6, 7 -> AtBContractType.OBJECTIVE_RAID;
            case 8 -> AtBContractType.EXTRACTION_RAID;
            case 9 -> AtBContractType.RECON_RAID;
            case 10 -> AtBContractType.GARRISON_DUTY;
            case 11 -> AtBContractType.CADRE_DUTY;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    private static AtBContractType innerSphereTable() {
        int roll = d6(2);
        return switch (roll) {
            case 2 -> covertTable();
            case 3, 12 -> specialTable();
            case 4 -> AtBContractType.PIRATE_HUNTING;
            case 5 -> AtBContractType.PLANETARY_ASSAULT;
            case 6, 7 -> AtBContractType.OBJECTIVE_RAID;
            case 8 -> AtBContractType.EXTRACTION_RAID;
            case 9 -> AtBContractType.RECON_RAID;
            case 10 -> AtBContractType.GARRISON_DUTY;
            case 11 -> AtBContractType.CADRE_DUTY;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    private static AtBContractType independentTable() {
        int roll = d6(2);
        return switch (roll) {
            case 2 -> covertTable();
            case 3, 12 -> specialTable();
            case 4 -> AtBContractType.PLANETARY_ASSAULT;
            case 5, 9 -> AtBContractType.OBJECTIVE_RAID;
            case 6 -> AtBContractType.EXTRACTION_RAID;
            case 7 -> AtBContractType.PIRATE_HUNTING;
            case 8 -> AtBContractType.SECURITY_DUTY;
            case 10 -> AtBContractType.GARRISON_DUTY;
            case 11 -> AtBContractType.CADRE_DUTY;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    private static AtBContractType corporationTable() {
        int roll = d6(2);
        return switch (roll) {
            case 2 -> covertTable();
            case 3, 4 -> AtBContractType.GUERRILLA_WARFARE;
            case 5, 8 -> AtBContractType.RECON_RAID;
            case 6 -> AtBContractType.EXTRACTION_RAID;
            case 7 -> AtBContractType.RETAINER;
            case 9 -> AtBContractType.RELIEF_DUTY;
            case 10 -> AtBContractType.DIVERSIONARY_RAID;
            case 11 -> AtBContractType.RIOT_DUTY;
            case 12 -> AtBContractType.CADRE_DUTY;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    private static AtBContractType pirateTable() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 3, 4, 5 -> AtBContractType.RECON_RAID;
            case 6, 7, 8, 9, 10, 11, 12 -> AtBContractType.OBJECTIVE_RAID;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    private static AtBContractType covertTable() {
        int roll = d6(2);
        return switch (roll) {
            case 2 -> AtBContractType.TERRORISM;
            case 3, 4 -> AtBContractType.ASSASSINATION;
            case 5 -> AtBContractType.ESPIONAGE;
            case 6 -> AtBContractType.SABOTAGE;
            case 7 -> AtBContractType.GUERRILLA_WARFARE;
            case 8 -> AtBContractType.RECON_RAID;
            case 9 -> AtBContractType.DIVERSIONARY_RAID;
            case 10 -> AtBContractType.OBSERVATION_RAID;
            case 11 -> AtBContractType.MOLE_HUNTING;
            case 12 -> AtBContractType.SECURITY_DUTY;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    private static AtBContractType specialTable() {
        int roll = d6(2);
        return switch (roll) {
            case 2 -> covertTable();
            case 3, 4 -> AtBContractType.GUERRILLA_WARFARE;
            case 5, 8 -> AtBContractType.RECON_RAID;
            case 6 -> AtBContractType.EXTRACTION_RAID;
            case 7 -> AtBContractType.RETAINER;
            case 9 -> AtBContractType.RELIEF_DUTY;
            case 10 -> AtBContractType.DIVERSIONARY_RAID;
            case 11 -> AtBContractType.RIOT_DUTY;
            case 12 -> AtBContractType.CADRE_DUTY;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }
}
