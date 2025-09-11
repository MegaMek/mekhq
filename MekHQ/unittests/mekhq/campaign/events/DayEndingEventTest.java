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
package mekhq.campaign.events;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import mekhq.campaign.Campaign;
import org.junit.jupiter.api.Test;

/**
 * This class is responsible for unit testing the cancellation of {@link DayEndingEvent}.
 */
class DayEndingEventTest {
    /**
     * Unit test to verify if a {@link DayEndingEvent} can be canceled.
     */
    @Test
    void checkDayEndingEventCancellable() {
        // Creates a mock instance of the Campaign class.
        Campaign mockCampaign = mock(Campaign.class);

        // Creates a new DayEndingEvent associated with the mock Campaign.
        DayEndingEvent dayEndingEvent = new DayEndingEvent(mockCampaign);

        // Asserts that the isCancellable() method of the created DayEndingEvent returns true.
        // If it does not, this test will fail.
        assertTrue(dayEndingEvent.isCancellable());
    }
}
