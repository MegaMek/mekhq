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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog.nagDialogs.nagLogic;

import static mekhq.gui.dialog.nagDialogs.nagLogic.InvalidFactionNagLogic.isFactionInvalid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import mekhq.gui.dialog.nagDialogs.InvalidFactionNagDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class is a test class for the {@link InvalidFactionNagDialog} class.
 * <p>
 * It tests the different combinations of unit states and verifies the behavior of the {@code isFactionInvalid()},
 * {@code lyranAllianceSpecialHandler()}, and {@code federatedSunsSpecialHandler()} methods.
 */
class InvalidFactionNagLogicTest {
    // Mock objects for the tests
    private Campaign campaign;
    private Faction faction;
    private LocalDate dateValid;
    private LocalDate dateInvalid;

    /**
     * Sets up the necessary dependencies and configurations before running the test methods. Runs once before all
     * tests
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

        assertFalse(isFactionInvalid(faction, dateValid));
    }

    @Test
    public void invalidDate() {
        when(faction.validIn(dateInvalid)).thenReturn(false);
        when(campaign.getLocalDate()).thenReturn(dateInvalid);

        assertTrue(isFactionInvalid(faction, dateInvalid));
    }
}
