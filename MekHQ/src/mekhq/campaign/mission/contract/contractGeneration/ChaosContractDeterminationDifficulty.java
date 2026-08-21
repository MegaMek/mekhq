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
package mekhq.campaign.mission.contract.contractGeneration;

import static java.lang.Math.round;
import static megamek.client.ratgenerator.ModelRecord.NETWORK_NONE;
import static megamek.client.ratgenerator.UnitTable.findTable;
import static megamek.common.enums.SkillLevel.ELITE;
import static megamek.common.enums.SkillLevel.parseFromInteger;
import static megamek.common.units.UnitType.MEK;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.UnitTable;
import megamek.common.enums.SkillLevel;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.universe.Factions;

/**
 * Estimates a difficulty rating for an {@link AbstractContract}.
 *
 * <p>The rating compares the estimated combat strength of the opposing force (an average-'Mek BV estimate drawn from
 * the enemy faction's RAT tables, scaled by their skill) against the player's participating combat units. A value of
 * {@code 5} represents roughly even forces; every 20% difference in relative strength shifts the rating by one point,
 * producing values from {@code 1} (easiest) to {@code 10} (hardest).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ChaosContractDeterminationDifficulty {
    /** Generic BV is used so this scale matches the one AtB contracts are rated on. */
    private static final boolean USE_GENERIC_BV = true;
    public static final int UNKNOWN_DIFFICULTY = -99;
    /** The 1-10 band the rating is reported on; the skulls in the contract market render the same scale. */
    public static final int MINIMUM_DIFFICULTY = 1;
    public static final int MAXIMUM_DIFFICULTY = 10;

    /**
     * Calculates the difficulty rating for a contract.
     *
     * @param campaign the active campaign, supplying the player's combat units
     * @param contract the contract being rated
     *
     * @return a difficulty rating from {@code 1} to {@code 10}, or {@link #UNKNOWN_DIFFICULTY} if the enemy strength
     *       cannot be estimated
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int calculateContractDifficulty(Campaign campaign, AbstractContract contract) {
        String enemyCode = contract.getEnemyFactionCode();

        SkillLevel opposingSkill = modifySkillLevelBasedOnFaction(enemyCode, contract.getEnemyForceSkill());

        // Public entry point, so the contract need not be one this class just generated - a legacy-converted one can
        // arrive with no schedule, and without a start year there is no era to size the enemy against.
        final LocalDate startDate = contract.getStartDate();
        if (startDate == null) {
            return UNKNOWN_DIFFICULTY;
        }

        double enemyPower = estimateMekStrength(startDate.getYear(),
              enemyCode,
              contract.getEnemyEquipmentRating());

        if (enemyPower == 0) {
            return UNKNOWN_DIFFICULTY;
        }

        enemyPower = round(enemyPower * getSkillMultiplier(opposingSkill));

        double playerPower = estimatePlayerPower(campaign.getAllCombatEntities());

        // The rating is a ratio, so with no fielded combat units there is no denominator to scale against. Say so
        // outright: fighting anything with nothing is as hard as the scale goes.
        if (playerPower == 0) {
            return MAXIMUM_DIFFICULTY;
        }

        double difference = enemyPower - playerPower;
        double percentDifference = (difference / playerPower) * 100;

        int difficulty = (int) round(Math.abs(percentDifference) / 20);
        difficulty = (percentDifference < 0) ? 5 - difficulty : 5 + difficulty;

        return Math.clamp(difficulty, MINIMUM_DIFFICULTY, MAXIMUM_DIFFICULTY);
    }

    /**
     * Adjusts a faction's effective skill level to account for special training characteristics. ComStar Special
     * Operations ({@code SOC}) forces are treated as {@link SkillLevel#ELITE}, and Clan factions are treated as one
     * skill level higher than their nominal rating.
     *
     * @author Illiani
     * @since 0.51.01
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
     * Estimates the average combat power of the player's participating combat units.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static double estimatePlayerPower(List<Entity> units) {
        if (units.isEmpty()) {
            return 0;
        }

        int totalBV = 0;
        int totalGBV = 0;

        for (Entity unit : units) {
            totalBV += unit.calculateBattleValue();
            totalGBV += unit.getGenericBattleValue();
        }

        return averageBattleValue(totalBV, totalGBV, units.size());
    }

    /**
     * Returns the skill BV multiplier for the given skill level.
     *
     * @author Illiani
     * @since 0.51.01
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
     * @author Illiani
     * @since 0.51.01
     */
    static double estimateMekStrength(int gameYear, String factionCode, int quality) {
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

        return averageBattleValue(totalBV, totalGBV, totalWeight);
    }

    /**
     * Computes an average (generic) battle value.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static double averageBattleValue(int totalBV, int totalGBV, int divisor) {
        if (divisor == 0) {
            return 0;
        }

        return USE_GENERIC_BV ? (double) totalGBV / divisor : (double) totalBV / divisor;
    }
}
