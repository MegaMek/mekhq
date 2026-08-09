/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.contractMarket;

import static megamek.common.compute.Compute.d6;

import mekhq.campaign.mission.enums.ContractObjectiveType;
import mekhq.campaign.universe.Faction;

/**
 * Handles the selection of contract types based on employer faction and negotiation results.
 *
 * <p>This class implements the Against the Bot (AtB) contract type selection system, using different tables for
 * different faction types (Clans, Inner Sphere, Pirates, etc.). The selection process uses modified dice rolls that
 * take into account the unit's connections with the employer and negotiation success.</p>
 *
 * <p>This uses the tables found in CamOps pg 40</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class ContractTypePicker {
    /**
     * Determines the contract type based on the employer faction and negotiation results.
     *
     * <p>The method selects the appropriate contract type table based on the employer's faction type (Clan, Pirate,
     * Major Power, Corporation, or Independent) and rolls on that table with modifiers from connections and negotiation
     * success.</p>
     *
     * @param employer          the faction offering the contract
     * @param commanderModifier Modifiers applied based on the commander's connections and negotiation success
     *
     * @return the selected contract type
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static ContractObjectiveType findMissionType(Faction employer, int commanderModifier) {
        // I took some liberties here with what faction rolls on what table, as the options were fairly limited. The
        // below gives a much better variance among the factions.
        if (employer.isClan()) {
            return clanTable(commanderModifier);
        } else if (employer.isPirate()) {
            return pirateTable(commanderModifier);
        } else if (employer.isMajorPower()) {
            return innerSphereTable(commanderModifier);
        } else if (employer.isCorporation() || employer.isRebel() || employer.isComStarOrWoB()) {
            return corporationTable(commanderModifier);
        } else {
            return independentTable(commanderModifier);
        }
    }

    /**
     * Generates a modified 2d6 roll that gravitates towards extreme results.
     *
     * <p>This method applies the modifier differently based on the initial roll: if the roll is below 7, the
     * modifier is subtracted (favoring lower results); if above 7, the modifier is added (favoring higher results); if
     * exactly 7, no modifier is applied. This pushes results toward the extremes where more interesting contract types
     * are typically found.</p>
     *
     * @param modifier the total modifier to apply
     *
     * @return the modified dice roll result, clamped to 2 -> 12 (inclusive)
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int getRoll(int modifier) {
        int initialRoll = d6(2);

        // Ostensibly, the player is meant to pick whether they want to raise or lower the result. However, bugging the
        // player every time we roll a contract would get annoying real quick, so we're going to gravitate towards
        // the extremes - where the interesting contract types are.
        int result;

        if (initialRoll < 7) {
            result = initialRoll - modifier;
        } else if (initialRoll > 7) {
            result = initialRoll + modifier;
        } else {
            result = initialRoll;
        }

        return Math.clamp(result, 2, 12);
    }

    /**
     * Rolls on the Clan contract type table.
     *
     * <p>Clan contracts focus on direct military operations with limited variety. Valid rolls are restricted to 4-11
     * to match the Clan contract offerings.</p>
     *
     * @param modifier the total modifier from connections and negotiation success
     *
     * @return the selected contract type from the Clan table
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static ContractObjectiveType clanTable(int modifier) {
        int roll = getRoll(modifier);
        while (roll < 4 || roll > 11) {
            roll = getRoll(modifier);
        }

        return switch (roll) {
            case 4 -> ContractObjectiveType.PIRATE_HUNTING;
            case 5 -> ContractObjectiveType.PLANETARY_ASSAULT;
            case 6, 7 -> ContractObjectiveType.OBJECTIVE_RAID;
            case 8 -> ContractObjectiveType.EXTRACTION_RAID;
            case 9 -> ContractObjectiveType.RECON_RAID;
            case 10 -> ContractObjectiveType.GARRISON_DUTY;
            case 11 -> ContractObjectiveType.CADRE_DUTY;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    /**
     * Rolls on the Inner Sphere major power contract type table.
     *
     * <p>This table offers a wide variety of contract types, including special operations (on rolls of 3 or 12) and
     * covert operations (on rolls of 2).</p>
     *
     * @param modifier the total modifier from connections and negotiation success
     *
     * @return the selected contract type from the Inner Sphere table
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static ContractObjectiveType innerSphereTable(int modifier) {
        int roll = getRoll(modifier);
        return switch (roll) {
            case 2 -> covertTable(modifier);
            case 3, 12 -> specialTable(modifier);
            case 4 -> ContractObjectiveType.PIRATE_HUNTING;
            case 5 -> ContractObjectiveType.PLANETARY_ASSAULT;
            case 6, 7 -> ContractObjectiveType.OBJECTIVE_RAID;
            case 8 -> ContractObjectiveType.EXTRACTION_RAID;
            case 9 -> ContractObjectiveType.RECON_RAID;
            case 10 -> ContractObjectiveType.GARRISON_DUTY;
            case 11 -> ContractObjectiveType.CADRE_DUTY;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    /**
     * Rolls on the independent world contract type table.
     *
     * <p>Independent worlds offer a mix of defensive and offensive contracts, with emphasis on security and garrison
     * duties. Can also offer special and covert operations.</p>
     *
     * @param modifier the total modifier from connections and negotiation success
     *
     * @return the selected contract type from the independent table
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static ContractObjectiveType independentTable(int modifier) {
        int roll = getRoll(modifier);
        return switch (roll) {
            case 2 -> covertTable(modifier);
            case 3, 12 -> specialTable(modifier);
            case 4 -> ContractObjectiveType.PLANETARY_ASSAULT;
            case 5, 9 -> ContractObjectiveType.OBJECTIVE_RAID;
            case 6 -> ContractObjectiveType.EXTRACTION_RAID;
            case 7 -> ContractObjectiveType.PIRATE_HUNTING;
            case 8 -> ContractObjectiveType.SECURITY_DUTY;
            case 10 -> ContractObjectiveType.GARRISON_DUTY;
            case 11 -> ContractObjectiveType.CADRE_DUTY;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    /**
     * Rolls on the corporation, rebel, or ComStar/Word of Blake contract type table.
     *
     * <p>These employers tend to offer more specialized missions including guerrilla warfare, riot suppression, and
     * covert operations.</p>
     *
     * @param modifier the total modifier from connections and negotiation success
     *
     * @return the selected contract type from the corporation table
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static ContractObjectiveType corporationTable(int modifier) {
        int roll = getRoll(modifier);
        return switch (roll) {
            case 2 -> covertTable(modifier);
            case 3, 4 -> ContractObjectiveType.GUERRILLA_WARFARE;
            case 5, 8 -> ContractObjectiveType.RECON_RAID;
            case 6 -> ContractObjectiveType.EXTRACTION_RAID;
            case 7 -> ContractObjectiveType.RETAINER;
            case 9 -> ContractObjectiveType.RELIEF_DUTY;
            case 10 -> ContractObjectiveType.DIVERSIONARY_RAID;
            case 11 -> ContractObjectiveType.RIOT_DUTY;
            case 12 -> ContractObjectiveType.CADRE_DUTY;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    /**
     * Rolls on the pirate contract type table.
     *
     * <p>Pirate employers only offer raid-type contracts, heavily weighted towards objective raids.</p>
     *
     * @param modifier the total modifier from connections and negotiation success
     *
     * @return the selected contract type from the pirate table
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static ContractObjectiveType pirateTable(int modifier) {
        int roll = getRoll(modifier);
        return switch (roll) {
            case 2, 3, 4, 5 -> ContractObjectiveType.RECON_RAID;
            case 6, 7, 8, 9, 10, 11, 12 -> ContractObjectiveType.OBJECTIVE_RAID;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    /**
     * Rolls on the covert operations contract type table.
     *
     * <p>This sub-table is accessed from other tables and offers black ops missions including assassination,
     * sabotage, espionage, and terrorism.</p>
     *
     * @param modifier the total modifier from connections and negotiation success
     *
     * @return the selected covert contract type
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static ContractObjectiveType covertTable(int modifier) {
        int roll = getRoll(modifier);
        return switch (roll) {
            case 2 -> ContractObjectiveType.TERRORISM;
            case 3, 4 -> ContractObjectiveType.ASSASSINATION;
            case 5 -> ContractObjectiveType.ESPIONAGE;
            case 6 -> ContractObjectiveType.SABOTAGE;
            case 7 -> ContractObjectiveType.GUERRILLA_WARFARE;
            case 8 -> ContractObjectiveType.RECON_RAID;
            case 9 -> ContractObjectiveType.DIVERSIONARY_RAID;
            case 10 -> ContractObjectiveType.OBSERVATION_RAID;
            case 11 -> ContractObjectiveType.MOLE_HUNTING;
            case 12 -> ContractObjectiveType.SECURITY_DUTY;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    /**
     * Rolls on the special operations contract type table.
     *
     * <p>This sub-table is accessed from other tables and offers specialized missions including retainer contracts,
     * relief duty, and riot suppression.</p>
     *
     * @param modifier the total modifier from connections and negotiation success
     *
     * @return the selected special contract type
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static ContractObjectiveType specialTable(int modifier) {
        int roll = getRoll(modifier);
        return switch (roll) {
            case 2 -> covertTable(modifier);
            case 3, 4 -> ContractObjectiveType.GUERRILLA_WARFARE;
            case 5, 8 -> ContractObjectiveType.RECON_RAID;
            case 6 -> ContractObjectiveType.EXTRACTION_RAID;
            case 7 -> ContractObjectiveType.RETAINER;
            case 9 -> ContractObjectiveType.RELIEF_DUTY;
            case 10 -> ContractObjectiveType.DIVERSIONARY_RAID;
            case 11 -> ContractObjectiveType.RIOT_DUTY;
            case 12 -> ContractObjectiveType.CADRE_DUTY;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }
}
