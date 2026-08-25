/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class ContractImportanceTest {

    // --- employer tier (0..3) ---

    @ParameterizedTest
    @CsvSource({ "ANY_SYSTEM_OWNER, 3", "LOCAL_SYSTEM_OWNER, 3", "ANY_PLANETARY_GOVERNMENT, 2",
                 "LOCAL_PLANETARY_GOVERNMENT, 2", "NOBLE, 2", "CORPORATION, 1", "MERCENARY_SUBCONTRACT, 1",
                 "CIVILIAN_ORGANIZATION_BUSINESS, 0", "CIVILIAN_ORGANIZATION_MILITIA, 0",
                 "CIVILIAN_ORGANIZATION_REBELS, 0" })
    void employerTierPointsByEmployer(final ChaosEmployerType employerType, final int expected) {
        assertEquals(expected, ContractImportance.employerTierPoints(employerType));
    }

    @ParameterizedTest
    @EnumSource(ChaosEmployerType.class)
    void employerTierPointsAlwaysInRange(final ChaosEmployerType employerType) {
        int points = ContractImportance.employerTierPoints(employerType);
        assertEquals(points,
              Math.clamp(points, 0, 3),
              "Employer tier points must stay within 0..3 for " + employerType);
    }

    // --- objective scope (-2..3 modifier shifted into 0..5) ---

    @ParameterizedTest
    @CsvSource({ "INVASION, 5", "GARRISON, 3", "RAID, 2", "EXPEDITION, 1", "PIRATE_HUNT, 1", "GUERILLA_OPERATION, 1",
                 "CADRE_DUTY, 0", "PIRATE_RAID, 0" })
    void objectivePointsByObjective(final ChaosObjectiveType objectiveType, final int expected) {
        assertEquals(expected, ContractImportance.objectivePoints(objectiveType));
    }

    // --- planet strategic-value buckets (0..MAX -> 0..5) ---

    @ParameterizedTest
    @CsvSource({ "0, 0", "3, 0", "4, 1", "7, 1", "8, 2", "11, 2", "12, 3", "15, 3", "16, 4", "19, 4", "20, 5",
                 "22, 5" })
    void planetValuePointsBuckets(final int strategicValue, final int expected) {
        assertEquals(expected, ContractImportance.planetValuePoints(strategicValue));
    }

    @Test
    void planetValuePointsCoversTheFullStrategicValueRange() {
        assertEquals(0, ContractImportance.planetValuePoints(0));
        assertEquals(5, ContractImportance.planetValuePoints(ChaosPlanetStrategicValue.MAX_STRATEGIC_VALUE));
    }

    // --- raw score -> tier ---

    @ParameterizedTest
    @CsvSource({ "0, MINOR", "3, MINOR", "4, STANDARD", "7, STANDARD", "8, STRATEGIC", "10, STRATEGIC", "11, CRITICAL",
                 "13, CRITICAL" })
    void fromScoreBuckets(final int score, final ContractImportance expected) {
        assertEquals(expected, ContractImportance.fromScore(score));
    }

    // --- force-quality modifier per tier ---

    @ParameterizedTest
    @CsvSource({ "MINOR, -1", "STANDARD, 0", "STRATEGIC, 1", "CRITICAL, 2" })
    void forceQualityModifierPerTier(final ContractImportance importance, final int expected) {
        assertEquals(expected, importance.getForceQualityModifier());
    }

    // --- end-to-end from(...) ---

    @Test
    void fromYieldsMinorForABusinessBackwaterExpedition() {
        // business tier 0 + expedition 1 + barren world 0 = 1 -> MINOR
        assertEquals(ContractImportance.MINOR,
              ContractImportance.from(ChaosEmployerType.CIVILIAN_ORGANIZATION_BUSINESS,
                    ChaosObjectiveType.EXPEDITION,
                    0));
    }

    @Test
    void fromYieldsCriticalForASystemOwnerInvasionOfAPrizeWorld() {
        // system owner 3 + invasion 5 + top-tier world 5 = 13 -> CRITICAL
        assertEquals(ContractImportance.CRITICAL,
              ContractImportance.from(ChaosEmployerType.ANY_SYSTEM_OWNER, ChaosObjectiveType.INVASION,
                    ChaosPlanetStrategicValue.MAX_STRATEGIC_VALUE));
    }

    @Test
    void fromYieldsStandardForAMidTierMix() {
        // corporation 1 + raid 2 + moderate world (value 9 -> 2) = 5 -> STANDARD
        assertEquals(ContractImportance.STANDARD,
              ContractImportance.from(ChaosEmployerType.CORPORATION, ChaosObjectiveType.RAID, 9));
    }
}
