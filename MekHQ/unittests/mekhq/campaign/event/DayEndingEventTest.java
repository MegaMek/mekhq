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
package mekhq.campaign.event;

import mekhq.campaign.Campaign;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * This class is responsible for unit testing the cancellation of DayEndingEvent.
 */
class DayEndingEventTest {
    /**
     * Unit test to verify if a DayEndingEvent can be canceled.
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