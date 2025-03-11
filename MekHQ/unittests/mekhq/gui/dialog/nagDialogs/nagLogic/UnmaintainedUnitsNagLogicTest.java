/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.nagDialogs.UnmaintainedUnitsNagDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mekhq.gui.dialog.nagDialogs.nagLogic.UnmaintainedUnitsNagLogic.campaignHasUnmaintainedUnits;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link UnmaintainedUnitsNagDialog} class.
 * It tests the different combinations of unit states and verifies the behavior of the {@code checkHanger()} method.
 */
class UnmaintainedUnitsNagLogicTest {
    // Mock objects for the tests
    private Campaign campaign;
    private Hangar hangar;
    private Unit mockUnit1, mockUnit2;

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
    private void initializeUnits(boolean unit1Unmaintained, boolean unit1Salvage,
                                 boolean unit2Unmaintained, boolean unit2Salvage) {
        when(mockUnit1.isUnmaintained()).thenReturn(unit1Unmaintained);
        when(mockUnit1.isSalvage()).thenReturn(unit1Salvage);

        when(mockUnit2.isUnmaintained()).thenReturn(unit2Unmaintained);
        when(mockUnit2.isSalvage()).thenReturn(unit2Salvage);

        List<Unit> units = List.of(mockUnit1, mockUnit2);
        when(hangar.getUnits()).thenReturn(units);
    }

    // In the following tests the checkHanger() method is called, and its response is checked
    // against expected behavior

    @Test
    void unmaintainedUnitExistsUnit1() {
        initializeUnits(true, false, false, false);
        assertTrue(campaignHasUnmaintainedUnits(campaign));
    }

    @Test
    void unmaintainedUnitExistsUnit2() {
        initializeUnits(false, false, true, false);
        assertTrue(campaignHasUnmaintainedUnits(campaign));
    }

    @Test
    void unmaintainedUnitExistsButSalvageUnit1() {
        initializeUnits(true, true, true, false);
        assertTrue(campaignHasUnmaintainedUnits(campaign));
    }

    @Test
    void unmaintainedUnitExistsButSalvageUnit2() {
        initializeUnits(true, false, true, true);
        assertTrue(campaignHasUnmaintainedUnits(campaign));
    }

    @Test
    void unmaintainedUnitExistsButSalvageMixed() {
        initializeUnits(false, true, true, false);
        assertTrue(campaignHasUnmaintainedUnits(campaign));
    }

    @Test
    void noUnmaintainedUnitExistsNoSalvage() {
        initializeUnits(false, false, false, false);
        assertFalse(campaignHasUnmaintainedUnits(campaign));
    }

    @Test
    void noUnmaintainedUnitExistsAllSalvage() {
        initializeUnits(false, true, false, true);
        assertFalse(campaignHasUnmaintainedUnits(campaign));
    }

    @Test
    void noUnmaintainedUnitExistsButSalvageUnit1() {
        initializeUnits(false, true, false, false);
        assertFalse(campaignHasUnmaintainedUnits(campaign));
    }

    @Test
    void noUnmaintainedUnitExistsButSalvageUnit2() {
        initializeUnits(false, false, false, true);
        assertFalse(campaignHasUnmaintainedUnits(campaign));
    }

    @Test
    void noUnmaintainedUnitExistsButSalvageMixed() {
        initializeUnits(false, true, false, false);
        assertFalse(campaignHasUnmaintainedUnits(campaign));
    }
}
