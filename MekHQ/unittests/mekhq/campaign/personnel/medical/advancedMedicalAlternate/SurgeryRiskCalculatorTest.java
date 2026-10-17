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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mekhq.campaign.personnel.medical.advancedMedicalAlternate.SurgeryRiskCalculator.Risk;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.SurgeryRiskCalculator.SurgeryOdds;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class SurgeryRiskCalculatorTest {
    private static final double TOLERANCE = 1e-9;
    /** The chance of rolling 7 or more on 2d6. */
    private static final double SEVEN_PLUS = 21 / 36.0;

    @Test
    @DisplayName("No surgeries leave the patient where they are, with no risk")
    public void noSurgeries() {
        Risk risk = SurgeryRiskCalculator.calculate(3, List.of());

        assertEquals(0.0, risk.deathChance(), TOLERANCE);
        assertEquals(3, risk.bestCaseHits());
        assertEquals(3, risk.worstCaseHits());
        assertNull(risk.fewestFatalFailures());
        assertFalse(risk.isLethal());
    }

    @Test
    @DisplayName("A plan that can't reach the fatal count has no risk, whatever the odds")
    public void safePlan() {
        Risk risk = SurgeryRiskCalculator.calculate(DEATH - 2, List.of(new SurgeryOdds(1, 0.1)));

        assertEquals(0.0, risk.deathChance(), TOLERANCE);
        assertEquals(DEATH - 2, risk.bestCaseHits());
        assertEquals(DEATH - 1, risk.worstCaseHits());
        assertNull(risk.fewestFatalFailures());
        assertFalse(risk.isLethal());
    }

    @Test
    @DisplayName("One injury from death, a single failure is fatal")
    public void oneFailureFromDeath() {
        Risk risk = SurgeryRiskCalculator.calculate(DEATH - 1, List.of(new SurgeryOdds(1, 0.75)));

        assertEquals(0.25, risk.deathChance(), TOLERANCE);
        assertEquals(DEATH - 1, risk.bestCaseHits());
        assertEquals(DEATH, risk.worstCaseHits());
        assertEquals(1, risk.fewestFatalFailures());
        assertTrue(risk.isLethal());
    }

    @Test
    @DisplayName("Surgeries that treat nothing can kill the patient even when every one succeeds")
    public void fatalEvenOnSuccess() {
        Risk risk = SurgeryRiskCalculator.calculate(DEATH - 1, List.of(new SurgeryOdds(0, 1.0),
              new SurgeryOdds(0, 1.0)));

        assertEquals(1.0, risk.deathChance(), TOLERANCE);
        assertEquals(DEATH + 1, risk.bestCaseHits());
        assertEquals(0, risk.fewestFatalFailures());
    }

    @Test
    @DisplayName("Three surgeries on four injuries: death needs the arm and one other surgery to fail")
    public void mixedPlan() {
        // An arm replacement clearing two injuries, then two surgeries clearing one each
        List<SurgeryOdds> surgeries = List.of(new SurgeryOdds(2, SEVEN_PLUS), new SurgeryOdds(1, SEVEN_PLUS),
              new SurgeryOdds(1, SEVEN_PLUS));
        Risk risk = SurgeryRiskCalculator.calculate(4, surgeries);

        double failure = 1 - SEVEN_PLUS;
        double expected = failure * (1 - SEVEN_PLUS * SEVEN_PLUS);
        assertEquals(expected, risk.deathChance(), TOLERANCE);
        assertEquals(3, risk.bestCaseHits());
        assertEquals(7, risk.worstCaseHits());
        assertEquals(2, risk.fewestFatalFailures());
        assertTrue(risk.isLethal());
    }

    @Test
    @DisplayName("The death chance of a mixed plan stays between 0 and 1")
    public void deathChanceIsAProbability() {
        List<SurgeryOdds> surgeries = List.of(new SurgeryOdds(3, 0.4), new SurgeryOdds(0, 0.9),
              new SurgeryOdds(1, 0.2), new SurgeryOdds(2, 0.6));
        Risk risk = SurgeryRiskCalculator.calculate(2, surgeries);

        assertTrue(risk.deathChance() >= 0.0);
        assertTrue(risk.deathChance() <= 1.0);
    }

    @ParameterizedTest(name = "{0} hits allows {1} safe surgeries")
    @CsvSource({ "0, 5", "3, 2", "4, 1", "5, 0", "6, 0", "9, 0" })
    @DisplayName("Safe surgery count keeps the worst case below the fatal count")
    public void safeSurgeryCount(int startingHits, int expected) {
        assertEquals(expected, SurgeryRiskCalculator.getSafeSurgeryCount(startingHits));
    }
}
