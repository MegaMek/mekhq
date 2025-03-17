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
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.nagDialogs.PregnantCombatantNagDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static mekhq.campaign.force.Force.FORCE_NONE;
import static mekhq.campaign.force.Force.FORCE_ORIGIN;
import static mekhq.gui.dialog.nagDialogs.nagLogic.PregnantCombatantNagLogic.hasActivePregnantCombatant;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link PregnantCombatantNagDialog} class.
 * It contains tests for various scenarios related to the {@code isPregnantCombatant} method
 */
class PregnantCombatantNagLogicTest {
    // Mock objects for the tests
    private Campaign campaign;
    private Mission mission;
    private Person personNotPregnant;
    private Person personPregnant;
    private Unit unit;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        personNotPregnant = new Person(campaign);
        personPregnant = new Person(campaign);
        personPregnant.setDueDate(LocalDate.of(3151, 1, 1));
        unit = mock(Unit.class);
    }

    // In the following tests the isPregnantCombatant() method is called, and its response is
    // checked against expected behavior

    @Test
    void noActiveMission() {
        assertFalse(hasActivePregnantCombatant(false, List.of(personPregnant)));
    }

    @Test
    void activeMissionsNoPregnancy() {
        assertFalse(hasActivePregnantCombatant(true, List.of(personNotPregnant)));
    }

    @Test
    void activeMissionsPregnancyNoUnit() {
        assertFalse(hasActivePregnantCombatant(true, List.of(personPregnant)));
    }

    @Test
    void activeMissionsPregnancyYesUnitNoForce() {
        personPregnant.setUnit(unit);
        when(unit.getForceId()).thenReturn(FORCE_NONE);

        assertFalse(hasActivePregnantCombatant(true, List.of(personPregnant)));
    }

    @Test
    void activeMissionsPregnancyYesUnitYesForce() {
        personPregnant.setUnit(unit);
        when(unit.getForceId()).thenReturn(FORCE_ORIGIN);

        assertTrue(hasActivePregnantCombatant(true, List.of(personPregnant)));
    }
}
