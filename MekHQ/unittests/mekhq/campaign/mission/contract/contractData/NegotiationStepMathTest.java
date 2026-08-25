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
package mekhq.campaign.mission.contract.contractData;

import static mekhq.campaign.mission.contract.contractData.NegotiationStepMath.distinctTermsSacrificed;
import static mekhq.campaign.mission.contract.contractData.NegotiationStepMath.nextHigherDifferentStep;
import static mekhq.campaign.mission.contract.contractData.NegotiationStepMath.nextLowerDifferentStep;
import static mekhq.campaign.mission.contract.contractData.NegotiationStepMath.rawValue;
import static mekhq.campaign.mission.contract.contractData.NegotiationStepMath.restoreStep;
import static mekhq.campaign.mission.contract.contractData.NegotiationStepMath.sacrificeAllowed;
import static mekhq.campaign.mission.contract.contractData.NegotiationStepMath.totalStepsSacrificed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.mission.contract.contractData.NegotiationStepMath.Term;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Exercises {@link NegotiationStepMath}: the plateau-crossing lowering logic ({@code nextLowerDifferentStep}) and the
 * sacrifice caps ({@code sacrificeAllowed} and the two counters).
 */
class NegotiationStepMathTest {

    // The distinct-term and total-step caps the dialog uses, so the cap tests read against real limits.
    private static final int MAX_TERMS = 2;
    private static final int MAX_STEPS = 4;

    @ParameterizedTest
    @CsvSource({
          // Base Pay has no plateaus - every step is distinct, so lowering always drops exactly one step.
          "BASE_PAY, 17, 16", "BASE_PAY, 5, 4", "BASE_PAY, 2, 1", "BASE_PAY, 1, -1",
          // Command Rights: INTEGRATED(1-3), HOUSE(4-7), LIAISON(8-10), INDEPENDENT(11-17).
          "COMMAND_RIGHTS, 11, 10",  // INDEPENDENT -> LIAISON, one step
          "COMMAND_RIGHTS, 17, 10",  // INDEPENDENT (top) -> LIAISON, crossing the whole 11-17 band (gap 7)
          "COMMAND_RIGHTS, 8, 7",    // LIAISON -> HOUSE
          "COMMAND_RIGHTS, 7, 3",    // HOUSE -> INTEGRATED, crossing 4-7 (gap 4)
          "COMMAND_RIGHTS, 4, 3",    // HOUSE bottom -> INTEGRATED
          "COMMAND_RIGHTS, 3, -1",   // INTEGRATED is the floor band
          "COMMAND_RIGHTS, 1, -1",
          // Transport: 0.0(1-5), 0.25, 0.5, 0.75, then 1.0(9-17).
          "TRANSPORT, 9, 8",         // 1.0 -> 0.75
          "TRANSPORT, 6, 5",         // 0.25 -> 0.0
          "TRANSPORT, 5, -1",        // 0.0 spans 1-5 and is the floor
          "TRANSPORT, 1, -1",
          // Salvage pairs exchange flag with multiplier: percent-0(1), exchange-0.25(2-3), then percentages.
          "SALVAGE, 3, 1",           // exchange 0.25 (2-3) -> percent 0.0 at step 1 (gap 2)
          "SALVAGE, 2, 1",
          "SALVAGE, 13, 12",         // 1.0 -> 0.9 (13-17 all read 1.0)
          "SALVAGE, 1, -1",
          // Support pairs straight-support with battlefield-loss; (1.0,1.0) spans 15-17.
          "SUPPORT, 17, 14",         // (1.0,1.0) -> (1.0,0.75), crossing 15-17 (gap 3)
          "SUPPORT, 9, 8",           // (1.0,0.1) -> (1.0,0.0)
          "SUPPORT, 8, 7",           // (1.0,0.0) -> (0.9,0.0)
          "SUPPORT, 1, -1" })
    void nextLowerDifferentStepCrossesPlateaus(final Term term, final int fromStep, final int expected) {
        assertEquals(expected, nextLowerDifferentStep(term, fromStep));
    }

    @ParameterizedTest
    @EnumSource(Term.class)
    void nextLowerDifferentStepFromTheBottomStepIsAlwaysMinusOne(final Term term) {
        assertEquals(-1, nextLowerDifferentStep(term, 1),
              "no term has a lower distinct value than its step-1 value");
    }

    @ParameterizedTest
    @CsvSource({
          // Base Pay: every step distinct, so the next higher value is the next step (until the top).
          "BASE_PAY, 1, 2", "BASE_PAY, 16, 17", "BASE_PAY, 17, -1",
          // Command Rights bands crossed upward; INDEPENDENT (11-17) has nothing higher that differs.
          "COMMAND_RIGHTS, 1, 4", "COMMAND_RIGHTS, 3, 4", "COMMAND_RIGHTS, 7, 8", "COMMAND_RIGHTS, 10, 11",
          "COMMAND_RIGHTS, 11, -1", "COMMAND_RIGHTS, 15, -1",
          // Transport: 0.0(1-5) -> 0.25, and 1.0(9-17) is the top band.
          "TRANSPORT, 1, 6", "TRANSPORT, 5, 6", "TRANSPORT, 8, 9", "TRANSPORT, 9, -1",
          // Salvage: percent-0(1) -> exchange(2-3) -> percentages, 1.0(13-17) top band.
          "SALVAGE, 1, 2", "SALVAGE, 2, 4", "SALVAGE, 12, 13", "SALVAGE, 13, -1",
          // Support: (1.0,1.0) spans 15-17.
          "SUPPORT, 1, 2", "SUPPORT, 8, 9", "SUPPORT, 14, 15", "SUPPORT, 15, -1" })
    void nextHigherDifferentStepCrossesPlateaus(final Term term, final int fromStep, final int expected) {
        assertEquals(expected, nextHigherDifferentStep(term, fromStep));
    }

    @ParameterizedTest
    @EnumSource(Term.class)
    void nextHigherDifferentStepFromTheTopStepIsAlwaysMinusOne(final Term term) {
        assertEquals(-1, nextHigherDifferentStep(term, 17),
              "no term has a higher distinct value than its step-17 value");
    }

    @ParameterizedTest
    @CsvSource({
          // A single plateau-crossing lower is undone by a single raise straight back to the baseline: Command Rights
          // baseline 7 (top of HOUSE) sacrificed to 3 (INTEGRATED) restores directly to 7, never resting on step 4-6
          // (which share the baseline's HOUSE value and would leave spendable bank for no concession).
          "COMMAND_RIGHTS, 7, 3, 7",
          "COMMAND_RIGHTS, 11, 10, 11",  // one-step lower, one-step restore
          // Not below baseline: nothing to restore, returns the baseline itself.
          "COMMAND_RIGHTS, 7, 7, 7",
          "BASE_PAY, 8, 8, 8",
          // Base Pay has no plateaus, so each lower is one step and each restore is one step.
          "BASE_PAY, 8, 5, 6",   // lowered 8->7->6->5; one raise undoes the last, 5->6
          "BASE_PAY, 8, 7, 8",
          // Transport: distinct values 6-9, so stepping; one raise undoes one lower.
          "TRANSPORT, 9, 5, 6",  // 9->8->7->6->5 down; restore 5->6
          "TRANSPORT, 9, 8, 9",
          // Support was lowered twice: 17->14 (crossing the 15-17 plateau), then 14->13. One raise undoes only the
          // last lower (13->14), leaving the term on a genuine boundary whose value differs from the baseline's.
          "SUPPORT, 17, 13, 14",
          "SUPPORT, 17, 14, 17" })
    void restoreStepUndoesExactlyOneLower(final Term term, final int originalStep, final int currentStep,
          final int expected) {
        assertEquals(expected, restoreStep(term, originalStep, currentStep));
    }

    @Test
    void restoreStepNeverRestsOnAStepSharingTheBaselineValue() {
        // The anti-exploit property: from any sacrificed position, the step a raise restores to either is the baseline
        // itself or carries a value different from the baseline's - so a term can never sit below baseline showing no
        // concession while still holding spendable bank. Checked exhaustively over Command Rights, the widest bands.
        for (int baseline = 1; baseline <= 17; baseline++) {
            for (int current = 1; current < baseline; current++) {
                int target = restoreStep(Term.COMMAND_RIGHTS, baseline, current);
                if (target != baseline) {
                    assertNotEquals(rawValue(Term.COMMAND_RIGHTS, ChaosContractStepsTable.fromStepValue(baseline)),
                          rawValue(Term.COMMAND_RIGHTS, ChaosContractStepsTable.fromStepValue(target)),
                          "restore target " + target + " must not share baseline " + baseline + "'s value");
                }
            }
        }
    }

    @Test
    void totalAndDistinctCountOnlyStepsBelowBaseline() {
        // Two parallel terms: the first sacrificed by 3 steps, the second raised by 2 (must not count as sacrifice).
        int[] original = { 11, 5 };
        int[] current = { 8, 7 };
        assertEquals(3, totalStepsSacrificed(original, current));
        assertEquals(1, distinctTermsSacrificed(original, current));
    }

    @Test
    void sacrificeAllowedRefusesNonPositiveGaps() {
        assertFalse(sacrificeAllowed(0, true, 0, 0, MAX_TERMS, MAX_STEPS));
        assertFalse(sacrificeAllowed(-1, true, 0, 0, MAX_TERMS, MAX_STEPS));
    }

    @Test
    void sacrificeAllowedRefusesAThirdDistinctTerm() {
        // Two terms already sacrificed; opening a new one is refused even though only 2 of 4 steps are spent.
        assertFalse(sacrificeAllowed(1, false, MAX_TERMS, 2, MAX_TERMS, MAX_STEPS));
        // ...but adding to a term already sacrificed is fine while steps remain.
        assertTrue(sacrificeAllowed(1, true, MAX_TERMS, 2, MAX_TERMS, MAX_STEPS));
    }

    @Test
    void sacrificeAllowedRefusesCrossingTheTotalStepCap() {
        assertFalse(sacrificeAllowed(3, false, 1, 2, MAX_TERMS, MAX_STEPS), "2 spent + 3 more = 5 exceeds 4");
        assertTrue(sacrificeAllowed(2, false, 1, 2, MAX_TERMS, MAX_STEPS), "2 spent + 2 more = 4 is exactly the cap");
    }

    @Test
    void commandRightsHighInIndependentCannotBeSacrificed() {
        // A contract generated deep in the INDEPENDENT band: the nearest lower value (LIAISON at step 10) is 5 steps
        // away, over the 4-step cap, so the term cannot be lowered at all.
        int fromStep = 15;
        int target = nextLowerDifferentStep(Term.COMMAND_RIGHTS, fromStep);
        assertEquals(10, target);
        assertFalse(sacrificeAllowed(fromStep - target, false, 0, 0, MAX_TERMS, MAX_STEPS),
              "crossing 5 steps must be refused against a 4-step cap");
    }
}
