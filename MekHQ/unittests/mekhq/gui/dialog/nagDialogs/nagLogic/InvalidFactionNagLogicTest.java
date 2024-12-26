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
package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import mekhq.gui.dialog.nagDialogs.InvalidFactionNagDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static mekhq.gui.dialog.nagDialogs.nagLogic.InvalidFactionNagLogic.isFactionInvalid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link InvalidFactionNagDialog} class.
 * <p>
 * It tests the different combinations of unit states and verifies the behavior of the
 * {@code isFactionInvalid()}, {@code lyranAllianceSpecialHandler()}, and
 * {@code federatedSunsSpecialHandler()} methods.
 */
class InvalidFactionNagLogicTest {
    // Mock objects for the tests
    private Campaign campaign;
    private Faction faction;
    private LocalDate dateValid;
    private LocalDate dateInvalid;

    /**
     * Sets up the necessary dependencies and configurations before running the test methods.
     * Runs once before all tests
     */
    @BeforeEach
    public void setup() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        faction = mock(Faction.class);

        dateValid = LocalDate.of(3151, 1, 1);
        dateInvalid = LocalDate.of(1936, 1, 1);

        // When the Campaign mock calls 'getFaction()' return the mocked faction
        when(campaign.getFaction()).thenReturn(faction);
    }

    // In the following tests, the beginning of the isFactionInvalid() method is called, and its
    // response is checked against expected behavior

    @Test
    public void validDate() {
        when(faction.validIn(dateValid)).thenReturn(true);
        when(campaign.getLocalDate()).thenReturn(dateValid);

        assertFalse(isFactionInvalid(campaign));
    }

    @Test
    public void invalidDate() {
        when(faction.validIn(dateInvalid)).thenReturn(false);
        when(campaign.getLocalDate()).thenReturn(dateInvalid);

        assertTrue(isFactionInvalid(campaign));
    }
}
