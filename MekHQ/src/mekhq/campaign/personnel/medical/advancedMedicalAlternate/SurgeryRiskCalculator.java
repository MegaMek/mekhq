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
package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

import static megamek.common.units.Crew.DEATH;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;

/**
 * Works out how likely a patient is to die from a batch of advanced surgeries.
 *
 * <p>Every surgery, pass or fail, leaves a recovery injury worth {@link #RECOVERY_INJURY_HITS} hit. A successful
 * surgery also clears the injuries it treats. Once every surgery has been rolled, a patient whose total injury
 * severity has reached {@link megamek.common.units.Crew#DEATH} dies of medical complications.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class SurgeryRiskCalculator {
    /** Alternate Advanced Medical counts every non-permanent injury, including recovery injuries, as one hit. */
    public static final int RECOVERY_INJURY_HITS = 1;

    private SurgeryRiskCalculator() {}

    /**
     * One planned surgery, as far as the risk is concerned.
     *
     * @param hitsCleared    the hits of the injuries a success removes
     * @param successChance  the chance of the surgery succeeding, from 0 to 1
     */
    public record SurgeryOdds(int hitsCleared, double successChance) {}

    /**
     * The risk of a batch of surgeries.
     *
     * @param deathChance         the chance the patient dies, from 0 to 1
     * @param bestCaseHits        the patient's hits if every surgery succeeds
     * @param worstCaseHits       the patient's hits if every surgery fails
     * @param fewestFatalFailures the fewest failed surgeries that can kill the patient, or {@code null} if none can
     */
    public record Risk(double deathChance, int bestCaseHits, int worstCaseHits,
          @Nullable Integer fewestFatalFailures) {
        /** @return {@code true} if any outcome of the batch kills the patient */
        public boolean isLethal() {
            return worstCaseHits >= DEATH;
        }
    }

    /**
     * @param startingHits the patient's total injury severity before surgery
     * @param surgeries    the surgeries to be performed together
     *
     * @return the risk of performing them all at once
     */
    public static Risk calculate(int startingHits, List<SurgeryOdds> surgeries) {
        Map<Integer, Double> outcomes = new HashMap<>();
        outcomes.put(startingHits, 1.0);
        for (SurgeryOdds surgery : surgeries) {
            Map<Integer, Double> next = new HashMap<>();
            for (Map.Entry<Integer, Double> outcome : outcomes.entrySet()) {
                int hits = outcome.getKey() + RECOVERY_INJURY_HITS;
                next.merge(hits - surgery.hitsCleared(), outcome.getValue() * surgery.successChance(), Double::sum);
                next.merge(hits, outcome.getValue() * (1 - surgery.successChance()), Double::sum);
            }
            outcomes = next;
        }

        double deathChance = 0;
        for (Map.Entry<Integer, Double> outcome : outcomes.entrySet()) {
            if (outcome.getKey() >= DEATH) {
                deathChance += outcome.getValue();
            }
        }

        int worstCaseHits = startingHits + surgeries.size() * RECOVERY_INJURY_HITS;
        int bestCaseHits = worstCaseHits;
        for (SurgeryOdds surgery : surgeries) {
            bestCaseHits -= surgery.hitsCleared();
        }

        // The deadliest failures are the surgeries that would have cleared the most
        List<Integer> clearedMostFirst = surgeries.stream()
                                               .map(SurgeryOdds::hitsCleared)
                                               .sorted((first, second) -> second - first)
                                               .toList();
        Integer fewestFatalFailures = null;
        for (int failures = 0; failures <= clearedMostFirst.size(); failures++) {
            int stillCleared = clearedMostFirst.subList(failures, clearedMostFirst.size())
                                     .stream()
                                     .mapToInt(Integer::intValue)
                                     .sum();
            if (worstCaseHits - stillCleared >= DEATH) {
                fewestFatalFailures = failures;
                break;
            }
        }

        return new Risk(deathChance, bestCaseHits, worstCaseHits, fewestFatalFailures);
    }

    /**
     * @param startingHits the patient's total injury severity before surgery
     *
     * @return how many surgeries can be performed together without any combination of failures killing the patient
     */
    public static int getSafeSurgeryCount(int startingHits) {
        return Math.max(0, (DEATH - 1 - startingHits) / RECOVERY_INJURY_HITS);
    }
}
