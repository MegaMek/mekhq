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
     * Calculates a difficulty rating for a mission by comparing the estimated combat strength of the opposing force
     * against the player's participating units.
     *
     * <p>A value of {@code 5} represents roughly even forces. Every 20% difference in relative strength shifts the
     * rating by one point, producing values in the range {@code 1} (easiest) to {@code 10} (hardest). If the enemy
     * force strength cannot be estimated, {@code -99} is returned.</p>
     *
     * @param mission           the mission being evaluated
     * @param gameYear          the current campaign year
     * @param useGenericBV      whether to use generic BV values instead of unit-specific BV
     * @param playerCombatUnits the player's participating combat units
     *
     * @return a difficulty rating from {@code 1} to {@code 10}, or {@code -99} if enemy strength estimation fails
     */
    public static int calculateContractDifficulty(AbstractMission mission, int gameYear,
          boolean useGenericBV, List<Entity> playerCombatUnits) {

        final int ERROR = -99;

        SkillLevel opposingSkill = modifySkillLevelBasedOnFaction(
              mission.getEnemyCode(), mission.getEnemySkill());

        double enemyPower = estimateMekStrength(
              gameYear,
              useGenericBV,
              mission.getEnemyCode(),
              mission.getEnemyQuality());

        if (enemyPower == 0) {
            return ERROR;
        }

        enemyPower = round(enemyPower * getSkillMultiplier(opposingSkill));

        double playerPower = estimatePlayerPower(playerCombatUnits, useGenericBV);

        double difference = enemyPower - playerPower;
        double percentDifference =
              (playerPower != 0 ? difference / playerPower : difference) * 100;

        int difficulty = (int) round(Math.abs(percentDifference) / 20);
        difficulty = (percentDifference < 0) ? 5 - difficulty : 5 + difficulty;

        return Math.clamp(difficulty, 1, 10);
    }

    /**
     * Adjusts a faction's effective skill level to account for special training characteristics.
     *
     * <p>ComStar Special Operations ({@code SOC}) forces are always treated as {@link SkillLevel#ELITE}. Clan
     * factions are treated as one skill level higher than their nominal rating.</p>
     *
     * @param factionCode the faction code
     * @param skillLevel  the base skill level
     *
     * @return the effective skill level used for BV calculations
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

    /**
     * Estimates the average combat power of a group of units.
     *
     * @param units        the units to evaluate
     * @param useGenericBV whether to use generic BV values
     *
     * @return the average BV per unit, or {@code 0} if no units are supplied
     */
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

        return averageBattleValue(totalBV, totalGBV, units.size(), useGenericBV);
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
     * Estimates the average combat power of non-salvage 'Meks available to a faction at a given quality level.
     *
     * @param gameYear     the campaign year
     * @param useGenericBV whether to use generic BV values
     * @param factionCode  the faction to evaluate
     * @param quality      the RAT quality level
     *
     * @return the weighted average BV per unit, or {@code 0} if the estimate cannot be generated
     */
    static double estimateMekStrength(int gameYear, boolean useGenericBV,
          String factionCode, int quality) {

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

        int totalBV = 0;
        int totalGBV = 0;
        int totalWeight = 0;

        for (int i = 0; i < unitTable.getNumEntries(); i++) {
            int battleValue = unitTable.getBV(i);

            // Salvage entries have no associated unit data.
            if (battleValue == 0) {
                continue;
            }

            int weight = unitTable.getEntryWeight(i);

            totalBV += battleValue * weight;
            totalGBV += unitTable.getMekSummary(i).getGenericBattleValue() * weight;
            totalWeight += weight;
        }

        if (totalWeight == 0) {
            return ERROR;
        }

        return averageBattleValue(totalBV, totalGBV, totalWeight, useGenericBV);
    }

    /**
     * Computes an average battle value.
     *
     * @param totalBV      summed BV values
     * @param totalGBV     summed generic BV values
     * @param divisor      number of units or total weight
     * @param useGenericBV whether generic BV values should be used
     *
     * @return the average battle value, or {@code 0} if {@code divisor} is zero
     */
    private static double averageBattleValue(int totalBV, int totalGBV, int divisor, boolean useGenericBV) {
        if (divisor == 0) {
            return 0;
        }

        return useGenericBV ? (double) totalGBV / divisor : (double) totalBV / divisor;
    }
}
