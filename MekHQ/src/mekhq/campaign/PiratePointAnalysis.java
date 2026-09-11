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
package mekhq.campaign;

import java.util.List;
import java.util.Objects;

import mekhq.campaign.universe.StarUtil;

/** Deterministic planning facts for comparing a standard jump point with an assumed pirate point. */
public final class PiratePointAnalysis {
    public static final int TWO_D6_OUTCOMES = 36;

    private static final double SECONDS_PER_HALF_DAY = 43_200.0;

    private PiratePointAnalysis() {
    }

    /** Neutral planning categories whose values are supplied explicitly by the user. */
    public enum ModifierCategory {
        POINT_GEOMETRY,
        NAVIGATION_DATA,
        VESSEL_CREW_CONDITION,
        OTHER_SITUATION
    }

    /** One optional signed target-number modifier. */
    public record Modifier(ModifierCategory category, int value, boolean enabled) {
        public Modifier {
            Objects.requireNonNull(category);
        }
    }

    /** Explicit physical and check assumptions used for one analysis. */
    public record Input(double standardDistanceKm, double pirateDistanceKm, double detectionRadiusKm,
                        double accelerationG, int baseTargetNumber, List<Modifier> modifiers) {
        public Input {
            requireNonNegativeFinite(standardDistanceKm, "standardDistanceKm");
            requireNonNegativeFinite(pirateDistanceKm, "pirateDistanceKm");
            requireNonNegativeFinite(detectionRadiusKm, "detectionRadiusKm");
            requirePositiveFinite(accelerationG, "accelerationG");
            modifiers = List.copyOf(modifiers);
        }
    }

    /** Physical facts for one endpoint approach. */
    public record ApproachFacts(double distanceKm, double transitDays, boolean emergenceInsideDetectionEnvelope,
                                double exposureDays) {
    }

    /** Exact target number and 2d6 success outcomes. */
    public record DifficultyFacts(int baseTargetNumber, int enabledModifierTotal, int targetNumber,
                                  int successfulOutcomes, int possibleOutcomes) {
        public double successProbability() {
            return successfulOutcomes / (double) possibleOutcomes;
        }
    }

    /** Immutable comparison facts. Transit change is pirate transit minus standard transit. */
    public record Facts(ApproachFacts standardPoint, ApproachFacts piratePoint, DifficultyFacts difficulty,
                        double transitSavingsDays, double transitAddedDays, double transitChangeDays) {
    }

    public static Facts analyze(Input input) {
        Objects.requireNonNull(input);
        ApproachFacts standardPoint = analyzeApproach(input.standardDistanceKm(), input.detectionRadiusKm(),
              input.accelerationG());
        ApproachFacts piratePoint = analyzeApproach(input.pirateDistanceKm(), input.detectionRadiusKm(),
              input.accelerationG());

        long modifierTotal = input.modifiers().stream()
                                   .filter(Modifier::enabled)
                                   .mapToLong(Modifier::value)
                                   .sum();
        int enabledModifierTotal = clampToInt(modifierTotal);
        int targetNumber = clampToInt((long) input.baseTargetNumber() + modifierTotal);
        DifficultyFacts difficulty = new DifficultyFacts(input.baseTargetNumber(), enabledModifierTotal,
              targetNumber, successful2d6Outcomes(targetNumber), TWO_D6_OUTCOMES);

        double transitChangeDays = piratePoint.transitDays() - standardPoint.transitDays();
        return new Facts(standardPoint, piratePoint, difficulty,
              Math.max(0.0, -transitChangeDays), Math.max(0.0, transitChangeDays), transitChangeDays);
    }

    private static ApproachFacts analyzeApproach(double distanceKm, double detectionRadiusKm,
          double accelerationG) {
        double transitDays = transitDays(distanceKm, accelerationG);
        return new ApproachFacts(distanceKm, transitDays, distanceKm <= detectionRadiusKm,
              exposureDays(distanceKm, detectionRadiusKm, transitDays));
    }

    /** Uses the same symmetric acceleration/deceleration convention as planetary endpoint transit. */
    public static double transitDays(double distanceKm, double accelerationG) {
        requireNonNegativeFinite(distanceKm, "distanceKm");
        requirePositiveFinite(accelerationG, "accelerationG");
        if (distanceKm == 0.0) {
            return 0.0;
        }
        double transitDays = Math.sqrt(distanceKm) * Math.sqrt(1000.0 / StarUtil.G)
                                   / (Math.sqrt(accelerationG) * SECONDS_PER_HALF_DAY);
        if (!Double.isFinite(transitDays)) {
            throw new IllegalArgumentException("endpoint transit must be finite");
        }
        return transitDays;
    }

    /**
     * Returns time inside a destination-centered detection radius along the symmetric endpoint trajectory.
     */
    static double exposureDays(double distanceKm, double detectionRadiusKm, double transitDays) {
        requireNonNegativeFinite(distanceKm, "distanceKm");
        requireNonNegativeFinite(detectionRadiusKm, "detectionRadiusKm");
        requireNonNegativeFinite(transitDays, "transitDays");
        if ((distanceKm == 0.0) || (detectionRadiusKm == 0.0)) {
            return 0.0;
        }
        if (detectionRadiusKm >= distanceKm) {
            return transitDays;
        }

        double radiusFraction = detectionRadiusKm / distanceKm;
        double exposureFraction = radiusFraction <= 0.5
              ? Math.sqrt(radiusFraction / 2.0)
              : 1.0 - Math.sqrt((1.0 - radiusFraction) / 2.0);
        return transitDays * exposureFraction;
    }

    /** Returns the exact number of successful ordered outcomes for a roll of 2d6. */
    public static int successful2d6Outcomes(int targetNumber) {
        if (targetNumber <= 2) {
            return TWO_D6_OUTCOMES;
        }
        if (targetNumber > 12) {
            return 0;
        }

        int successfulOutcomes = 0;
        for (int firstDie = 1; firstDie <= 6; firstDie++) {
            for (int secondDie = 1; secondDie <= 6; secondDie++) {
                if ((firstDie + secondDie) >= targetNumber) {
                    successfulOutcomes++;
                }
            }
        }
        return successfulOutcomes;
    }

    private static int clampToInt(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!(value > 0.0) || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }

    private static void requireNonNegativeFinite(double value, String name) {
        if ((value < 0.0) || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
