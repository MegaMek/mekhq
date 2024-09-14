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

import java.util.List;

import static mekhq.gui.dialog.nagDialogs.UnmaintainedUnitsNagDialog.checkHanger;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the UnmaintainedUnitsNagDialog class.
 * It tests the different combinations of unit states and verifies the behavior of the checkHanger() method.
 */
class UnmaintainedUnitsNagDialogTest {
    // Mock objects for the tests
    Campaign campaign;
    Hangar hangar;
    Unit mockUnit1, mockUnit2;

    /**
     * Sets up the necessary dependencies and configurations before running the test methods.
     * Runs once before all tests
     */
    @BeforeAll
    static void setup() {
        try {
            Systems.setInstance(Systems.loadDefault());
        } catch (Exception ex) {
            MMLogger.create(UnmaintainedUnitsNagDialogTest.class).error("", ex);
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
        mockUnit1 = mock(Unit.class);
        mockUnit2 = mock(Unit.class);

        // When the Campaign mock calls 'getHangar()' return the 'hangar' mock
        when(campaign.getHangar()).thenReturn(hangar);
    }

    /**
     * Initializes the units by setting their maintenance status and salvage status.
     *
     * @param unit1Unmaintained A boolean indicating whether the first unit is unmaintained.
     * @param unit1Salvage A boolean indicating whether the first unit is salvage.
     * @param unit2Unmaintained A boolean indicating whether the second unit is unmaintained.
     * @param unit2Salvage A boolean indicating whether the second unit is salvage.
     */
    private void initializeUnits(boolean unit1Unmaintained, boolean unit1Salvage, boolean unit2Unmaintained, boolean unit2Salvage) {
        when(mockUnit1.isUnmaintained()).thenReturn(unit1Unmaintained);
        when(mockUnit1.isSalvage()).thenReturn(unit1Salvage);

        when(mockUnit2.isUnmaintained()).thenReturn(unit2Unmaintained);
        when(mockUnit2.isSalvage()).thenReturn(unit2Salvage);

        List<Unit> units = List.of(mockUnit1, mockUnit2);
        when(hangar.getUnits()).thenReturn(units);
    }

    // In the following tests,
    // Different combinations of unit states to set up desired behaviors in mock objects
    // Then the checkHanger() method of UnmaintainedUnitsNagDialog class is called,
    // and its response is checked against expected behavior

    @Test
    void unmaintainedUnitExistsUnit1() {
        initializeUnits(true, false, false, false);
        assertTrue(checkHanger(campaign));
    }

    @Test
    void unmaintainedUnitExistsUnit2() {
        initializeUnits(false, false, true, false);
        assertTrue(checkHanger(campaign));
    }

    @Test
    void unmaintainedUnitExistsButSalvageUnit1() {
        initializeUnits(true, true, true, false);
        assertTrue(checkHanger(campaign));
    }

    @Test
    void unmaintainedUnitExistsButSalvageUnit2() {
        initializeUnits(true, false, true, true);
        assertTrue(checkHanger(campaign));
    }

    @Test
    void unmaintainedUnitExistsButSalvageMixed() {
        initializeUnits(false, true, true, false);
        assertTrue(checkHanger(campaign));
    }

    @Test
    void noUoUnmaintainedUnitExistsUnit1() {
        initializeUnits(false, false, false, false);
        assertFalse(checkHanger(campaign));
    }

    @Test
    void noUnmaintainedUnitExistsUnit2() {
        initializeUnits(false, false, false, false);
        assertFalse(checkHanger(campaign));
    }

    @Test
    void noUnmaintainedUnitExistsButSalvageUnit1() {
        initializeUnits(false, true, false, false);
        assertFalse(checkHanger(campaign));
    }

    @Test
    void noUnmaintainedUnitExistsButSalvageUnit2() {
        initializeUnits(false, false, false, true);
        assertFalse(checkHanger(campaign));
    }

    @Test
    void noUnmaintainedUnitExistsButSalvageMixed() {
        initializeUnits(false, true, false, false);
        assertFalse(checkHanger(campaign));
    }
}
