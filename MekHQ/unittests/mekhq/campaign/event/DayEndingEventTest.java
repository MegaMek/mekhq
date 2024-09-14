package mekhq.campaign.event;

import mekhq.campaign.Campaign;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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