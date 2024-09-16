package mekhq.gui.dialog.nagDialogs;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.finances.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static mekhq.gui.dialog.nagDialogs.UnableToAffordJumpNagDialog.getNextJumpCost;
import static mekhq.gui.dialog.nagDialogs.UnableToAffordJumpNagDialog.isUnableToAffordNextJump;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link UnableToAffordJumpNagDialog} class.
 * It contains tests for various scenarios related to the {@code isUnableToAffordNextJump} method
 */
class UnableToAffordJumpNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    private CampaignOptions options;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        options = mock(CampaignOptions.class);

        // Stubs
        when(campaign.getCampaignOptions()).thenReturn(options);
    }

    // In the following tests the canAffordNextJump() method is called, and its response is checked
    // against expected behavior

    @Test
    void canAffordNextJump() {
        when(campaign.getFunds()).thenReturn(Money.of(5));
        when(getNextJumpCost(campaign)).thenReturn(Money.of(1));

        assertFalse(isUnableToAffordNextJump(campaign));
    }

    @Test
    void cannotAffordNextJump() {
        when(campaign.getFunds()).thenReturn(Money.of(1));
        when(getNextJumpCost(campaign)).thenReturn(Money.of(5));

        assertTrue(isUnableToAffordNextJump(campaign));
    }
}