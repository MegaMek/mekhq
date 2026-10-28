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
 * surgery also clears the injuries it treats and leaves its marker injury, which is a hit of its own unless it records
 * a prosthetic or implant - removing a limb, for instance, leaves the patient with a severed limb. Once every surgery
 * has been rolled, a patient whose total injury severity has reached {@link megamek.common.units.Crew#DEATH} dies of
 * medical complications.</p>
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
     * @param hitsCleared   the hits of the injuries a success removes
     * @param markerHits    the hits a success adds through its marker injury, from
     *                      {@link #getMarkerHits(ProstheticType)}
     * @param successChance the chance of the surgery succeeding, from 0 to 1
     */
    public record SurgeryOdds(int hitsCleared, int markerHits, double successChance) {
        /**
         * A surgery whose marker injury adds no hits, as for one fitting a prosthetic or implant.
         *
         * @param hitsCleared   the hits of the injuries a success removes
         * @param successChance the chance of the surgery succeeding, from 0 to 1
         */
        public SurgeryOdds(int hitsCleared, double successChance) {
            this(hitsCleared, 0, successChance);
        }

        /** @return how many more hits a success leaves the patient with than a failure, negative if it helps */
        int successChange() {
            return markerHits - hitsCleared;
        }

        /** @return the most hits this surgery can add, whether it passes or fails */
        int mostHitsAdded() {
            return RECOVERY_INJURY_HITS + Math.max(0, successChange());
        }
    }

    /**
     * The risk of a batch of surgeries.
     *
     * @param deathChance         the chance the patient dies, from 0 to 1
     * @param bestCaseHits        the fewest hits any outcome of the batch leaves the patient with
     * @param worstCaseHits       the most hits any outcome of the batch leaves the patient with
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
     * @param type the surgery
     *
     * @return the hits a successful surgery's marker injury adds: Alternate Advanced Medical counts it as one hit,
     *       unless it records a prosthetic, an implant or a flaw
     */
    public static int getMarkerHits(ProstheticType type) {
        InjurySubType subType = type.getInjuryType().getSubType();
        return (subType.isPermanentModification() || subType.isFlaw()) ? 0 : 1;
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
                next.merge(hits + surgery.successChange(), outcome.getValue() * surgery.successChance(), Double::sum);
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

        int allFailedHits = startingHits + surgeries.size() * RECOVERY_INJURY_HITS;
        int allSucceededHits = allFailedHits;
        int bestCaseHits = allFailedHits;
        int worstCaseHits = allFailedHits;
        for (SurgeryOdds surgery : surgeries) {
            allSucceededHits += surgery.successChange();
            bestCaseHits += Math.min(0, surgery.successChange());
            worstCaseHits += Math.max(0, surgery.successChange());
        }

        // Starting from every surgery passing, the deadliest failures are the surgeries whose success helps the most
        List<Integer> failureCostsMostFirst = surgeries.stream()
                                                    .map(surgery -> -surgery.successChange())
                                                    .sorted((first, second) -> second - first)
                                                    .toList();
        Integer fewestFatalFailures = null;
        int hits = allSucceededHits;
        for (int failures = 0; failures <= failureCostsMostFirst.size(); failures++) {
            if (failures > 0) {
                hits += failureCostsMostFirst.get(failures - 1);
            }
            if (hits >= DEATH) {
                fewestFatalFailures = failures;
                break;
            }
        }

        return new Risk(deathChance, bestCaseHits, worstCaseHits, fewestFatalFailures);
    }

    /**
     * @param startingHits the patient's total injury severity before surgery
     * @param surgeries    the surgeries to choose from, in the order they should be kept
     *
     * @return how many of the surgeries, taken in order, can be performed together without any outcome killing the
     *       patient
     */
    public static int getSafeSurgeryCount(int startingHits, List<SurgeryOdds> surgeries) {
        int worstCaseHits = startingHits;
        int safeCount = 0;
        for (SurgeryOdds surgery : surgeries) {
            worstCaseHits += surgery.mostHitsAdded();
            if (worstCaseHits >= DEATH) {
                break;
            }
            safeCount++;
        }
        return safeCount;
    }
}
