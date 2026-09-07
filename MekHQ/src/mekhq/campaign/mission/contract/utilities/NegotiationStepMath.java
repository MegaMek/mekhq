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
package mekhq.campaign.mission.contract.utilities;

import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.CHAOS_CONTRACT_MAXIMUM_STEP_VALUE;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.CHAOS_CONTRACT_MINIMUM_STEP_VALUE;

import java.util.List;
import java.util.Objects;

import mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable;

/**
 * Pure arithmetic behind the contract negotiation table: crossing the plateaus on the {@link ChaosContractStepsTable}
 * when a term is lowered, and the caps on how much may be sacrificed. Kept free of any GUI so it can be reasoned about
 * and tested in isolation; {@code ContractNegotiationDialog} delegates to it.
 *
 * <p>Lowering a term does not move one raw step - it drops straight to the next step whose value actually changes,
 * counting every raw step crossed. Two global caps bound sacrifice across the whole negotiation: at most a set number
 * of distinct terms, and at most a set number of raw steps in total over them.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class NegotiationStepMath {
    private NegotiationStepMath() {
    }

    /** The five negotiable contract terms, each a column on the {@link ChaosContractStepsTable}. */
    public enum Term {
        BASE_PAY, SUPPORT, TRANSPORT, SALVAGE, COMMAND_RIGHTS
    }

    /**
     * The book's raw value column for a term at a given step, used to detect plateaus (adjacent steps that share a
     * value). Two composite terms carry two columns each: Support pairs its straight-support and battlefield-loss
     * multipliers; Salvage pairs its exchange flag with its multiplier.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Object rawValue(Term term, ChaosContractStepsTable step) {
        return switch (term) {
            case COMMAND_RIGHTS -> step.getContractCommandRights();
            case BASE_PAY -> step.getBasePayMultiplier();
            case SUPPORT -> List.of(step.getStraightSupportMultiplier(), step.getBattlefieldLossMultiplier());
            case TRANSPORT -> step.getTransportMultiplier();
            case SALVAGE -> List.of(step.isExchangeSalvage(), step.getSalvageMultiplier());
        };
    }

    /**
     * The nearest step below {@code fromStep} whose value for this term differs from the value at {@code fromStep}, or
     * {@code -1} when every lower step shares the same value (the term already sits at its lowest distinct value).
     * Comparing the per-term column means plateaus - runs of steps that leave a term's value unchanged, such as Command
     * Rights holding HOUSE across steps 4-7 - are crossed in a single move.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int nextLowerDifferentStep(Term term, int fromStep) {
        Object currentValue = rawValue(term, ChaosContractStepsTable.fromStepValue(fromStep));
        for (int step = fromStep - 1; step >= CHAOS_CONTRACT_MINIMUM_STEP_VALUE; step--) {
            if (!Objects.equals(currentValue, rawValue(term, ChaosContractStepsTable.fromStepValue(step)))) {
                return step;
            }
        }
        return -1;
    }

    /**
     * The nearest step above {@code fromStep} whose value for this term differs from the value at {@code fromStep}, or
     * {@code -1} when every higher step shares the same value (the term already sits at its highest distinct value).
     * The upward mirror of {@link #nextLowerDifferentStep}: it moves a term to the next better value in a single
     * meaningful step, crossing any plateau in one move. Used by active negotiation to improve a term.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int nextHigherDifferentStep(Term term, int fromStep) {
        Object currentValue = rawValue(term, ChaosContractStepsTable.fromStepValue(fromStep));
        for (int step = fromStep + 1; step <= CHAOS_CONTRACT_MAXIMUM_STEP_VALUE; step++) {
            if (!Objects.equals(currentValue, rawValue(term, ChaosContractStepsTable.fromStepValue(step)))) {
                return step;
            }
        }
        return -1;
    }

    /**
     * The step a sacrificed term rises to when a single raise undoes one lower: the sacrifice boundary directly above
     * {@code currentStep} on the deterministic path lowered down from {@code originalStep}.
     *
     * <p>Lowering always lands on value boundaries (it skips whole plateaus), so the boundaries a term passes through
     * as it is lowered from its baseline are fixed - {@code originalStep}, then {@code nextLowerDifferentStep} of that,
     * and so on. Restoring walks that same path and returns the boundary just above where the term now sits, i.e. the
     * exact step the most recent lower started from. One raise therefore mirrors one lower, refunding precisely that
     * lower's banked steps. Crucially it never returns a mid-plateau step that shares the baseline's value: every step
     * on the path below the baseline has a value distinct from the baseline's, so a restored term can never sit below
     * its baseline while showing no concession yet still holding spendable bank.</p>
     *
     * <p>Returns {@code originalStep} when the term is not below its baseline.</p>
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int restoreStep(Term term, int originalStep, int currentStep) {
        int previous = originalStep;
        int step = originalStep;
        while (step > currentStep) {
            previous = step;
            step = nextLowerDifferentStep(term, step);
            if (step < CHAOS_CONTRACT_MINIMUM_STEP_VALUE) {
                break;
            }
        }
        return previous;
    }

    /**
     * Total raw steps currently sacrificed (lowered below baseline) summed over every term. The two arrays are parallel
     * - same length, same term at each index - holding each term's baseline step and its current step.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int totalStepsSacrificed(int[] originalSteps, int[] currentSteps) {
        int total = 0;
        for (int i = 0; i < originalSteps.length; i++) {
            total += Math.max(0, originalSteps[i] - currentSteps[i]);
        }
        return total;
    }

    /**
     * How many distinct terms are currently sacrificed (sitting below their baseline).
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int distinctTermsSacrificed(int[] originalSteps, int[] currentSteps) {
        int count = 0;
        for (int i = 0; i < originalSteps.length; i++) {
            if (currentSteps[i] < originalSteps[i]) {
                count++;
            }
        }
        return count;
    }

    /**
     * Whether {@code gap} further raw steps may be sacrificed from a term, honoring both global caps.
     *
     * @param gap                     raw steps this move would sacrifice; a non-positive gap is never allowed
     * @param termAlreadySacrificed   whether this term is already below its baseline (so it counts against the
     *                                distinct-term cap already, and adding to it opens no new term)
     * @param distinctTermsSacrificed how many distinct terms are already sacrificed
     * @param totalStepsSacrificed    how many raw steps are already sacrificed in total
     * @param maxTerms                the distinct-term cap
     * @param maxSteps                the total-steps cap
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean sacrificeAllowed(int gap, boolean termAlreadySacrificed, int distinctTermsSacrificed,
          int totalStepsSacrificed, int maxTerms, int maxSteps) {
        if (gap <= 0) {
            return false;
        }
        if (!termAlreadySacrificed && distinctTermsSacrificed >= maxTerms) {
            return false;
        }
        return totalStepsSacrificed + gap <= maxSteps;
    }
}
