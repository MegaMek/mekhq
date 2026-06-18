/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import static java.lang.Math.round;
import static megamek.client.ratgenerator.ModelRecord.NETWORK_NONE;
import static megamek.client.ratgenerator.UnitTable.findTable;
import static megamek.common.enums.SkillLevel.ELITE;
import static megamek.common.enums.SkillLevel.parseFromInteger;
import static megamek.common.units.UnitType.MEK;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.UnitTable;
import megamek.common.enums.SkillLevel;
import megamek.common.units.Entity;
import mekhq.campaign.universe.Factions;

public class ContractDifficulty {
    /**
     * Calculates the difficulty rating of a contract by comparing the estimated combat strength of the opposing force
     * to the combat strength of the player's participating units.
     *
     * <p>The method performs the following steps:</p>
     * <ol>
     *     <li>Determines the opposing force's effective skill level by applying any faction-based adjustments to
     *     their base {@link SkillLevel}.</li>
     *     <li>Computes a skill multiplier and applies it to the estimated enemy force power, derived from the game
     *     year, force quality, and whether generic BV values are used.</li>
     *     <li>Estimates the total combat power of the player's units based on their BV values and optionally using
     *     generic BV rules.</li>
     *     <li>Computes the percentage difference between enemy and player power.</li>
     *     <li>Maps that percentage difference into a difficulty scale ranging from {@code 1} (easiest) to {@code 10}
     *     (hardest), centered around {@code 5} as an even match.</li>
     * </ol>
     *
     * <p>A negative percentage difference indicates that the player is stronger than the opposing force; a positive
     * difference indicates the enemy is stronger. Each 20% shift away from parity increases (or decreases)
     * difficulty by one step.</p>
     *
     * <p>If enemy combat strength cannot be computed, the method returns {@code -99} to signal an error.</p>
     *
     * @param gameYear          the current in-game year used for estimating enemy technology and BV baselines
     * @param useGenericBV      whether generic BV values should be used instead of unit-specific BV calculations
     * @param playerCombatUnits the list of player {@link Entity} objects expected to participate in the contract
     *
     * @return a difficulty rating from {@code 1} to {@code 10}, where {@code 5} represents roughly even forces; or
     *       {@code -99} if the enemy power estimation fails
     */
    public static int calculateContractDifficulty(AbstractMission mission, int gameYear, boolean useGenericBV,
          List<Entity> playerCombatUnits) {
        final int ERROR = -99;

        // Estimate the power of the enemy forces
        String enemyCode = mission.getEnemyCode();
        SkillLevel enemySkill = mission.getEnemySkill();
        int enemyQuality = mission.getEnemyQuality();

        SkillLevel opposingSkill = modifySkillLevelBasedOnFaction(enemyCode, enemySkill);
        double enemySkillMultiplier = getSkillMultiplier(opposingSkill);
        double enemyPower = estimateMekStrength(gameYear, useGenericBV, enemyCode, enemyQuality);

        // If we cannot calculate enemy power, abort.
        if (enemyPower == 0) {
            return ERROR;
        }

        enemyPower = (int) round(enemyPower * enemySkillMultiplier);

        // Estimate player power
        double playerPower = estimatePlayerPower(playerCombatUnits, useGenericBV);

        // Calculate difficulty based on the percentage difference between the two forces.
        double difference = enemyPower - playerPower;
        // Divide by 0 protection
        double percentDifference = (playerPower != 0 ? (difference / playerPower) : difference) * 100;

        int mappedValue = (int) round(Math.abs(percentDifference) / 20);
        if (percentDifference < 0) {
            mappedValue = 5 - mappedValue;
        } else {
            mappedValue = 5 + mappedValue;
        }

        return Math.clamp(mappedValue, 1, 10);
    }

    /**
     * Modifies the skill level based on the faction code.
     *
     * @param factionCode the code of the faction
     * @param skillLevel  the original skill level
     *
     * @return the modified skill level
     */
    static SkillLevel modifySkillLevelBasedOnFaction(String factionCode, SkillLevel skillLevel) {
        if (Objects.equals(factionCode, "SOC")) {
            return ELITE;
        }

        if (Factions.getInstance().getFaction(factionCode).isClan()) {
            return parseFromInteger(skillLevel.ordinal() + 1);
        }

        return skillLevel;
    }

    static double estimatePlayerPower(List<Entity> units, boolean useGenericBV) {
        if (units.isEmpty()) {
            return 0;
        }

        int totalBV = 0;
        int totalGBV = 0;
        for (Entity unit : units) {
            totalBV += unit.calculateBattleValue();
            totalGBV += unit.getGenericBattleValue();
        }

        // Return an average per unit so this is comparable to the enemy strength estimate.
        if (useGenericBV) {
            return ((double) totalGBV) / units.size();
        } else {
            return ((double) totalBV) / units.size();
        }
    }

    /**
     * Returns the skill BV multiplier based on the given skill level.
     *
     * @param skillLevel the skill level to determine the multiplier
     *
     * @return the skill multiplier
     */
    private static double getSkillMultiplier(SkillLevel skillLevel) {
        return switch (skillLevel) {
            case NONE -> 0.68;
            case ULTRA_GREEN -> 0.77;
            case GREEN -> 0.86;
            case REGULAR -> 1.00;
            case VETERAN -> 1.32;
            case ELITE -> 1.68;
            case HEROIC -> 2.02;
            case LEGENDARY -> 2.31;
        };
    }

    /**
     * Estimates the relative strength for Mek units of a specific faction and quality. Excludes salvage.
     *
     * @param gameYear     the year of the current campaign
     * @param useGenericBV whether to use generic BV for strength calculations
     * @param factionCode  the code of the faction to estimate the average Mek strength for
     * @param quality      the quality of the Meks to calculate the average strength for
     *
     * @return the average battle value OR total BV2 divided by total GBV for Meks of the specified faction and quality
     *       OR 0 on error
     */
    static double estimateMekStrength(int gameYear, boolean useGenericBV, String factionCode, int quality) {
        final double ERROR = 0;

        RATGenerator ratGenerator = Factions.getInstance().getRATGenerator();
        FactionRecord faction = ratGenerator.getFaction(factionCode);

        if (faction == null) {
            return ERROR;
        }

        UnitTable unitTable;
        try {
            unitTable = findTable(faction,
                  MEK,
                  gameYear,
                  String.valueOf(quality),
                  new ArrayList<>(),
                  NETWORK_NONE,
                  new ArrayList<>(),
                  new ArrayList<>(),
                  new ArrayList<>(),
                  0,
                  faction);
        } catch (Exception ignored) {
            return ERROR;
        }

        // Otherwise, calculate the estimated power of the faction
        int entries = unitTable.getNumEntries();

        int totalBattleValue = 0;
        int totalGBV = 0;
        int rollingCount = 0;

        for (int i = 0; i < entries; i++) {
            int battleValue = unitTable.getBV(i); // 0 for salvage
            if (0 == battleValue) {
                // Removing this check will break things, see the other comments.
                continue;
            }

            // getMekSummary(int index) is NULL for salvage.
            int genericBattleValue = unitTable.getMekSummary(i).getGenericBattleValue();
            int weight = unitTable.getEntryWeight(i); // NOT 0 for salvage

            totalBattleValue += battleValue * weight;
            totalGBV += genericBattleValue * weight;
            rollingCount += weight;
            
            if (rollingCount == 0) {
                return ERROR;
            }
        }

        if (useGenericBV) {
            return ((double) totalGBV) / rollingCount;
        } else {
            return ((double) totalBattleValue) / rollingCount;
        }
    }
}
