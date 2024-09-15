package mekhq.gui.dialog.nagDialogs;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InsufficientAstechsNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    private InsufficientAstechsNagDialog dialog;

    /**
     * Sets up the necessary dependencies and configurations before running the test methods.
     * Runs once before all tests
     */
    @BeforeAll
    static void setup() {
        try {
            Systems.setInstance(Systems.loadDefault());
        } catch (Exception exception) {
            MMLogger.create(InsufficientAstechsNagDialogTest.class).error("", exception);
        }
    }

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        campaign = mock(Campaign.class);
        dialog = new InsufficientAstechsNagDialog(null, campaign);
    }

    // In the following tests,
    // Different combinations of unit states to set up desired behaviors in mock objects
    // Then the checkNag() method of InsufficientAstechsNagDialog class is called,
    // and its response is checked against expected behavior

    @Test
    void noAstechsNeeded() {
        when(campaign.getAstechNeed()).thenReturn(0);
        assertFalse(dialog.checkNag());
    }

    @Test
    void oneAstechNeeded() {
        when(campaign.getAstechNeed()).thenReturn(1);
        assertTrue(dialog.checkNag());
    }

    @Test
    void negativeAstechsNeeded() {
        when(campaign.getAstechNeed()).thenReturn(-1);
        assertFalse(dialog.checkNag());
    }
}