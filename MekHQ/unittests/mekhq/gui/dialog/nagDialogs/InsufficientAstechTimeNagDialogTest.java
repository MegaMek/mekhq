/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog.nagDialogs;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static mekhq.gui.dialog.nagDialogs.InsufficientAstechTimeNagDialog.checkAstechTimeDeficit;
import static mekhq.gui.dialog.nagDialogs.InsufficientAstechTimeNagDialog.getAstechTimeDeficit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class contains test cases for the {@link InsufficientAstechTimeNagDialog} class.
 * It tests the different combinations of Astech requirements and verifies the behavior of the
 * {@code isContractEnded()} method.
 */
class InsufficientAstechTimeNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    private Hangar hangar;
    private Unit unit1, unit2;
    private int possibleAstechMinutes;
    private int possibleAstechOvertimeMinutes;

    /**
     * Sets up the necessary dependencies and configurations before running the test methods.
     * Runs once before all tests
     */
    @BeforeAll
    static void setup() {
        try {
            Systems.setInstance(Systems.loadDefault());
        } catch (Exception exception) {
            MMLogger.create(InsufficientAstechTimeNagDialogTest.class).error("", exception);
        }
    }

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        hangar = mock(Hangar.class);
        unit1 = mock(Unit.class);
        unit2 = mock(Unit.class);
        possibleAstechMinutes = 2880; // This is the equivalent of one full team
        possibleAstechOvertimeMinutes = 1440; // As is this

        // Stub the getHangar() method to return the hangar mock
        when(campaign.getHangar()).thenReturn(hangar);
    }

    /**
     * Initializes a unit with the given parameters.
     *
     * @param unit            the unit to be initialized
     * @param isUnmaintained  a boolean indicating if the unit is unmaintained
     * @param isPresent       a boolean indicating if the unit is present
     * @param isSelfCrewed    a boolean indicating if the unit is self-crewed
     * @param maintenanceTime the maintenance time for the unit
     */
    private void initiateUnit(Unit unit, boolean isUnmaintained, boolean isPresent, boolean isSelfCrewed, int maintenanceTime) {
        when(unit.isUnmaintained()).thenReturn(isUnmaintained);
        when(unit.isPresent()).thenReturn(isPresent);
        when(unit.isSelfCrewed()).thenReturn(isSelfCrewed);
        when(unit.getMaintenanceTime()).thenReturn(maintenanceTime);
    }

    /**
     * Processes units and Astech time based on the given parameters.
     *
     * @param isOvertimeAllowed {@code true} if overtime is allowed, {@code false} otherwise
     */
    private void processUnitsAndAstechTime(boolean isOvertimeAllowed) {
        // Prepare a stream of the unit mocks
        Stream<Unit> unitStream = Stream.of(unit1, unit2);

        // Stub the getUnitsStream() method to return the stream of unit mocks
        when(hangar.getUnitsStream()).thenReturn(unitStream);

        // Calculate possible Astech Minutes
        when(campaign.getPossibleAstechPoolMinutes()).thenReturn(possibleAstechMinutes);

        // Calculate overtime minutes
        when(campaign.isOvertimeAllowed()).thenReturn(isOvertimeAllowed);
        when(campaign.getPossibleAstechPoolOvertime()).thenReturn(possibleAstechOvertimeMinutes);
    }

    // In the following tests,
    // Different combinations of Unit states to set up desired behaviors in mock objects
    // Then the getAstechTimeDeficit() method of InsufficientAstechTimeNagDialog class is called,
    // and its response is checked against expected behavior

    @Test
    void testAstechTimeDeficitCalculationNoOvertime() {
        // Initiate Units
        initiateUnit(unit1, false, true, false, 60);
        initiateUnit(unit2, false, true, false, 60);

        // Stream Units and process Astech Time
        processUnitsAndAstechTime(false);

        // Assert results equals expected value
        assertEquals(-4, getAstechTimeDeficit(campaign));
    }

    @Test
    void testAstechTimeDeficitCalculationWithOvertime() {
        // Initiate Units
        initiateUnit(unit1, false, true, false, 60);
        initiateUnit(unit2, false, true, false, 60);

        // Stream Units and process Astech Time
        processUnitsAndAstechTime(true);

        // Assert results equals expected value
        assertEquals(-7, getAstechTimeDeficit(campaign));
    }

    @Test
    void testInsufficientAstechTimeDeficitCalculationNoOvertime() {
        // Initiate Units
        initiateUnit(unit1, false, true, false, 6000);
        initiateUnit(unit2, false, true, false, 6000);

        // Stream Units and process Astech Time
        processUnitsAndAstechTime(false);

        // Assert results equals expected value
        assertEquals(144, getAstechTimeDeficit(campaign));
    }

    @Test
    void testInsufficientAstechTimeDeficitCalculationWithInsufficientOvertime() {
        // Initiate Units
        initiateUnit(unit1, false, true, false, 6000);
        initiateUnit(unit2, false, true, false, 6000);

        // Stream Units and process Astech Time
        processUnitsAndAstechTime(true);

        // Assert results equals expected value
        assertEquals(141, getAstechTimeDeficit(campaign));
    }

    @Test
    void testInsufficientAstechTimeDeficitCalculationWithSufficientOvertime() {
        // Initiate Units
        initiateUnit(unit1, false, true, false, 300);
        initiateUnit(unit2, false, true, false, 300);

        // Stream Units and process Astech Time
        processUnitsAndAstechTime(true);

        // Assert results equals expected value
        assertEquals(-1, getAstechTimeDeficit(campaign));
    }

    @Test
    void testAstechTimeDeficitCalculationOneUnitUnmaintained() {
        // Initiate Units
        initiateUnit(unit1, true, true, false, 60);
        initiateUnit(unit2, false, true, false, 60);

        // Stream Units and process Astech Time
        processUnitsAndAstechTime(false);

        // Assert results equals expected value
        assertEquals(-5, getAstechTimeDeficit(campaign));
    }

    @Test
    void testAstechTimeDeficitCalculationTwoUnitsUnmaintained() {
        // Initiate Units
        initiateUnit(unit1, true, true, false, 60);
        initiateUnit(unit2, true, true, false, 60);

        // Stream Units and process Astech Time
        processUnitsAndAstechTime(false);

        // Assert results equals expected value
        assertEquals(-6, getAstechTimeDeficit(campaign));
    }

    @Test
    void testAstechTimeDeficitCalculationOneUnitAbsent() {
        // Initiate Units
        initiateUnit(unit1, false, false, false, 60);
        initiateUnit(unit2, false, true, false, 60);

        // Stream Units and process Astech Time
        processUnitsAndAstechTime(false);

        // Assert results equals expected value
        assertEquals(-5, getAstechTimeDeficit(campaign));
    }

    @Test
    void testAstechTimeDeficitCalculationTwoUnitsAbsent() {
        // Initiate Units
        initiateUnit(unit1, false, false, false, 60);
        initiateUnit(unit2, false, false, false, 60);

        // Stream Units and process Astech Time
        processUnitsAndAstechTime(false);

        // Assert results equals expected value
        assertEquals(-6, getAstechTimeDeficit(campaign));
    }

    @Test
    void testAstechTimeDeficitCalculationOneUnitSelfCrewed() {
        // Initiate Units
        initiateUnit(unit1, false, true, true, 60);
        initiateUnit(unit2, false, true, false, 60);

        // Stream Units and process Astech Time
        processUnitsAndAstechTime(false);

        // Assert results equals expected value
        assertEquals(-5, getAstechTimeDeficit(campaign));
    }

    @Test
    void testAstechTimeDeficitCalculationTwoUnitsSelfCrewed() {
        // Initiate Units
        initiateUnit(unit1, false, true, true, 60);
        initiateUnit(unit2, false, true, true, 60);

        // Stream Units and process Astech Time
        processUnitsAndAstechTime(false);

        // Assert results equals expected value
        assertEquals(-6, getAstechTimeDeficit(campaign));
    }

    // In the following tests,
    // Different combinations of Unit states to set up desired behaviors in mock objects
    // Then the checkAstechTimeDeficit() method of InsufficientAstechTimeNagDialog class is called,
    // and its response is checked against expected behavior

    @Test
    void testAstechTimeDeficitCheckNegativeDeficit() {
        // Initiate Units
        initiateUnit(unit1, false, true, false, 60);
        initiateUnit(unit2, false, true, false, 60);

        // Stream Units and process Astech Time
        processUnitsAndAstechTime(false);

        // Assert results equals expected value
        assertFalse(checkAstechTimeDeficit(campaign));
    }

    @Test
    void testAstechTimeDeficitCheckPositiveDeficit() {
        // Initiate Units
        initiateUnit(unit1, false, true, false, 6000);
        initiateUnit(unit2, false, true, false, 6000);

        // Stream Units and process Astech Time
        processUnitsAndAstechTime(false);

        // Assert results equals expected value
        assertTrue(checkAstechTimeDeficit(campaign));
    }

    @Test
    void testAstechTimeDeficitCheckZeroDeficit() {
        // Initiate Units
        initiateUnit(unit1, false, true, false, 240);
        initiateUnit(unit2, false, true, false, 240);

        // Stream Units and process Astech Time
        processUnitsAndAstechTime(false);

        // Assert results equals expected value
        assertFalse(checkAstechTimeDeficit(campaign));
    }
}