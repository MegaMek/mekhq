package mekhq.gui.dialog.nagDialogs;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static mekhq.gui.dialog.nagDialogs.NoCommanderNagDialog.isCommanderMissing;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link NoCommanderNagDialog} class.
 * It contains test methods for various scenarios related to commander assignment.
 */
class NoCommanderNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    private Person commander;
    private Person commanderNull;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        commander = mock(Person.class);
        commanderNull = null;
    }

    // In the following tests the isCommanderMissing() method is called, and its response is checked
    // against expected behavior

    @Test
    void commanderPresent() {
        when(campaign.getFlaggedCommander()).thenReturn(commander);
        assertFalse(isCommanderMissing(campaign));
    }

    @Test
    void commanderMissing() {
        when(campaign.getFlaggedCommander()).thenReturn(commanderNull);
        assertTrue(isCommanderMissing(campaign));
    }
}