package mekhq.gui.dialog.nagDialogs;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static mekhq.gui.dialog.nagDialogs.PrisonersNagDialog.hasPrisoners;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link PrisonersNagDialog} class.
 * It contains tests for various scenarios related to the {@code hasPrisoners} method
 */
class PrisonersNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
    }

    // In the following tests the hasPrisoners() method is called, and its response is checked
    // against expected behavior

    @Test
    void activeContract() {
        when(campaign.hasActiveContract()).thenReturn(true);

        assertFalse(hasPrisoners(campaign));
    }

    @Test
    void noActiveContractNoPrisoners() {
        when(campaign.hasActiveContract()).thenReturn(false);
        when(campaign.getCurrentPrisoners()).thenReturn(new ArrayList<>());

        assertFalse(hasPrisoners(campaign));
    }

    @Test
    void noActiveContractPrisoners() {
        Person prisoner = mock(Person.class);

        when(campaign.hasActiveContract()).thenReturn(false);
        when(campaign.getCurrentPrisoners()).thenReturn(List.of(prisoner));

        assertTrue(hasPrisoners(campaign));
    }
}