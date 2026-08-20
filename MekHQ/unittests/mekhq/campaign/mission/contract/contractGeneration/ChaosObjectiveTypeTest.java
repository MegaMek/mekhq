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

import static java.lang.Math.max;
import static java.lang.Math.round;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ChaosObjectiveTypeTest {

    /**
     * The intended force-commitment weight per objective. Positive values commit better forces to strategically weighty
     * objectives (invasion, garrison); negative values reflect the light, irregular, or expendable forces fielded for
     * probes, hunts, and raids. Kept as an explicit table so any change to a value &mdash; or any newly added objective
     * &mdash; is a deliberate, reviewed edit.
     */
    private static final Map<ChaosObjectiveType, Integer> EXPECTED_FORCE_COMMITMENT =
          new EnumMap<>(ChaosObjectiveType.class);

    static {
        EXPECTED_FORCE_COMMITMENT.put(ChaosObjectiveType.EXPEDITION, -1);
        EXPECTED_FORCE_COMMITMENT.put(ChaosObjectiveType.PIRATE_HUNT, -1);
        EXPECTED_FORCE_COMMITMENT.put(ChaosObjectiveType.GUERILLA_OPERATION, -1);
        EXPECTED_FORCE_COMMITMENT.put(ChaosObjectiveType.GARRISON, 1);
        EXPECTED_FORCE_COMMITMENT.put(ChaosObjectiveType.CADRE_DUTY, -2);
        EXPECTED_FORCE_COMMITMENT.put(ChaosObjectiveType.RAID, 0);
        EXPECTED_FORCE_COMMITMENT.put(ChaosObjectiveType.INVASION, 3);
        EXPECTED_FORCE_COMMITMENT.put(ChaosObjectiveType.PIRATE_RAID, -2);
    }

    @ParameterizedTest
    @EnumSource(ChaosObjectiveType.class)
    void forceCommitmentModifierMatchesExpectedValue(final ChaosObjectiveType objectiveType) {
        Integer expected = EXPECTED_FORCE_COMMITMENT.get(objectiveType);
        assertEquals(expected, objectiveType.getForceCommitmentModifier(),
              "Unexpected force-commitment modifier for " + objectiveType
                    + " (a new objective must be given a value in both the enum and this test's table)");
    }

    @Test
    void everyObjectiveHasAnExpectedForceCommitmentValue() {
        assertEquals(ChaosObjectiveType.values().length, EXPECTED_FORCE_COMMITMENT.size(),
              "Every ChaosObjectiveType must have an expected force-commitment value in this test's table");
    }

    // --- contract length ---

    @ParameterizedTest
    @EnumSource(ChaosObjectiveType.class)
    void fixedLengthEqualsTheDeclaredMonthsLength(final ChaosObjectiveType objectiveType) {
        assertEquals(objectiveType.getMonthsLength(), objectiveType.calculateLength(false),
              "with variable length disabled the contract runs for exactly the declared duration");
    }

    @ParameterizedTest
    @EnumSource(ChaosObjectiveType.class)
    void variableLengthStaysWithinTheDocumentedBand(final ChaosObjectiveType objectiveType) {
        // Mirrors calculateVariableLength: base of 75% plus 0..(variance-1) months of jitter at 50%, floored at 1.
        int months = objectiveType.getMonthsLength();
        int base = (int) round(months * 0.75);
        int variance = (int) round(months * 0.5);
        int lowerBound = max(1, base);
        int upperBound = (variance > 0) ? base + variance - 1 : months;

        for (int iteration = 0; iteration < 500; iteration++) {
            int length = objectiveType.calculateLength(true);
            assertTrue(length >= lowerBound && length <= upperBound,
                  objectiveType + " variable length " + length + " fell outside [" + lowerBound + ", " + upperBound
                        + "]");
        }
    }

    // --- special rules ---

    @Test
    void usesSpecialRuleReflectsMembership() {
        assertTrue(ChaosObjectiveType.PIRATE_HUNT.usesSpecialRule(
                    ChaosObjectiveSpecialRules.END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS),
              "pirate hunts end after two consecutive quiet tracks");
        assertFalse(ChaosObjectiveType.PIRATE_HUNT.usesSpecialRule(ChaosObjectiveSpecialRules.USE_PIRATE_LOOTING),
              "a pirate hunt does not itself loot");
        assertTrue(ChaosObjectiveType.PIRATE_RAID.usesSpecialRule(ChaosObjectiveSpecialRules.USE_PIRATE_LOOTING),
              "a pirate raid loots");
    }

    @Test
    void objectiveWithoutSpecialRulesHasNone() {
        assertTrue(ChaosObjectiveType.EXPEDITION.getSpecialRules().isEmpty(),
              "an expedition carries no special rules");
        assertFalse(ChaosObjectiveType.EXPEDITION.usesSpecialRule(ChaosObjectiveSpecialRules.SIMULATED_DAMAGE));
    }
}
