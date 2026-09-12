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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.PiratePointAnalysis.Facts;
import mekhq.campaign.PiratePointAnalysis.Input;
import mekhq.campaign.PiratePointAnalysis.Modifier;
import mekhq.campaign.PiratePointAnalysis.ModifierCategory;
import mekhq.campaign.universe.StarUtil;
import org.junit.jupiter.api.Test;

class PiratePointAnalysisTest {
    private static final double TOLERANCE = 1.0e-10;

    @Test
    void twoD6OddsAreExactAtBoundariesAndCommonTargets() {
        assertEquals(36, PiratePointAnalysis.successful2d6Outcomes(Integer.MIN_VALUE));
        assertEquals(36, PiratePointAnalysis.successful2d6Outcomes(2));
        assertEquals(35, PiratePointAnalysis.successful2d6Outcomes(3));
        assertEquals(21, PiratePointAnalysis.successful2d6Outcomes(7));
        assertEquals(15, PiratePointAnalysis.successful2d6Outcomes(8));
        assertEquals(1, PiratePointAnalysis.successful2d6Outcomes(12));
        assertEquals(0, PiratePointAnalysis.successful2d6Outcomes(13));
        assertEquals(0, PiratePointAnalysis.successful2d6Outcomes(Integer.MAX_VALUE));
    }

    @Test
    void onlyEnabledNeutralModifiersAffectAndExtremeTargetsRemainSafe() {
        Facts facts = PiratePointAnalysis.analyze(new Input(1000.0, 500.0, 100.0, 1.0, 8, List.of(
              new Modifier(ModifierCategory.POINT_GEOMETRY, 2, true),
              new Modifier(ModifierCategory.NAVIGATION_DATA, -4, false),
              new Modifier(ModifierCategory.VESSEL_CREW_CONDITION, -1, true),
              new Modifier(ModifierCategory.OTHER_SITUATION, 20, false))));

        assertEquals(1, facts.difficulty().enabledModifierTotal());
        assertEquals(9, facts.difficulty().targetNumber());
        assertEquals(10, facts.difficulty().successfulOutcomes());
        assertEquals(10.0 / 36.0, facts.difficulty().successProbability(), TOLERANCE);

        Facts extreme = PiratePointAnalysis.analyze(new Input(0.0, 0.0, 0.0, 1.0, Integer.MAX_VALUE,
              List.of(new Modifier(ModifierCategory.OTHER_SITUATION, Integer.MAX_VALUE, true))));
        assertEquals(Integer.MAX_VALUE, extreme.difficulty().targetNumber());
        assertEquals(0, extreme.difficulty().successfulOutcomes());
    }

    @Test
    void standardTransitUsesCanonicalPlanetaryKinematicsWhilePirateDistanceIsExplicit() {
        double standardDistanceKm = 1_000_000.0;
        double expectedStandardDays = Math.sqrt((standardDistanceKm * 1000.0) / StarUtil.G) / 43_200.0;
        Facts facts = analyze(standardDistanceKm, 250_000.0, 0.0);

        assertEquals(expectedStandardDays, facts.standardPoint().transitDays(), TOLERANCE);
        assertEquals(expectedStandardDays / 2.0, facts.piratePoint().transitDays(), TOLERANCE);
        assertEquals(expectedStandardDays / 2.0, facts.transitSavingsDays(), TOLERANCE);
        assertEquals(0.0, facts.transitAddedDays(), TOLERANCE);
        assertEquals(-expectedStandardDays / 2.0, facts.transitChangeDays(), TOLERANCE);
    }

    @Test
    void detectionExposureIsExactAtZeroHalfAndFullRangeBoundaries() {
        Facts zeroRange = analyze(1000.0, 1000.0, 0.0);
        Facts halfRange = analyze(1000.0, 1000.0, 500.0);
        Facts fullRange = analyze(1000.0, 1000.0, 1000.0);

        assertFalse(zeroRange.piratePoint().emergenceInsideDetectionEnvelope());
        assertEquals(0.0, zeroRange.piratePoint().exposureDays(), TOLERANCE);
        assertFalse(halfRange.piratePoint().emergenceInsideDetectionEnvelope());
        assertEquals(halfRange.piratePoint().transitDays() / 2.0,
              halfRange.piratePoint().exposureDays(), TOLERANCE);
        assertTrue(fullRange.piratePoint().emergenceInsideDetectionEnvelope());
        assertEquals(fullRange.piratePoint().transitDays(), fullRange.piratePoint().exposureDays(), TOLERANCE);

        Facts zeroDistance = analyze(0.0, 0.0, 0.0);
        assertTrue(zeroDistance.piratePoint().emergenceInsideDetectionEnvelope());
        assertEquals(0.0, zeroDistance.piratePoint().exposureDays(), TOLERANCE);
    }

    @Test
    void analysisInputDefensivelyCopiesModifierState() {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new Modifier(ModifierCategory.POINT_GEOMETRY, 1, true));
        Input input = new Input(1000.0, 500.0, 100.0, 1.0, 8, modifiers);
        modifiers.clear();

        assertEquals(1, input.modifiers().size());
        assertEquals(9, PiratePointAnalysis.analyze(input).difficulty().targetNumber());
    }

    private static Facts analyze(double standardDistanceKm, double pirateDistanceKm, double detectionRadiusKm) {
        return PiratePointAnalysis.analyze(new Input(standardDistanceKm, pirateDistanceKm, detectionRadiusKm,
              1.0, 8, List.of()));
    }
}
