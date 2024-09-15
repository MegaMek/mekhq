package mekhq.gui.dialog.nagDialogs;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static mekhq.gui.dialog.nagDialogs.EndContractNagDialog.isContractEnded;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the EndContractNagDialog class.
 * It contains test methods for various scenarios related to contract expiration.
 */
public class EndContractNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    private LocalDate today;
    private Contract contract1, contract2;

    /**
     * Sets up the necessary dependencies and configurations before running the test methods.
     * Runs once before all tests
     */
    @BeforeAll
    static void setup() {
        try {
            Systems.setInstance(Systems.loadDefault());
        } catch (Exception exception) {
            MMLogger.create(EndContractNagDialogTest.class).error("", exception);
        }
    }

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        today = LocalDate.now();
        contract1 = mock(Contract.class);
        contract2 = mock(Contract.class);

        // When the Campaign mock calls 'getLocalDate()' return today's date
        when(campaign.getLocalDate()).thenReturn(today);
    }

    // In the following tests,
    // Different combinations of contract end date states to set up desired behaviors in mock objects
    // Then the isContractEnded() method of EndContractNagDialog class is called,
    // and its response is checked against expected behavior

    @Test
    void noActiveContracts() {
        when(campaign.getActiveContracts()).thenReturn(new ArrayList<>());
        assertFalse(isContractEnded(campaign));
    }

    @Test
    void oneActiveContractEndsTomorrow() {
        when(campaign.getActiveContracts()).thenReturn(List.of(contract1));
        when(contract1.getEndingDate()).thenReturn(today.plusDays(1));
        assertFalse(isContractEnded(campaign));
    }

    @Test
    void oneActiveContractEndsToday() {
        when(campaign.getActiveContracts()).thenReturn(List.of(contract1));
        when(contract1.getEndingDate()).thenReturn(today);
        assertTrue(isContractEnded(campaign));
    }

    @Test
    void twoActiveContractsOneEndsTomorrowOneEndsToday() {
        when(campaign.getActiveContracts()).thenReturn(List.of(contract1, contract2));
        when(contract1.getEndingDate()).thenReturn(today.plusDays(1));
        when(contract2.getEndingDate()).thenReturn(today);
        assertTrue(isContractEnded(campaign));
    }

    @Test
    void twoActiveContractsBothEndToday() {
        when(campaign.getActiveContracts()).thenReturn(List.of(contract1, contract2));
        when(contract1.getEndingDate()).thenReturn(today);
        assertTrue(isContractEnded(campaign));
    }
}