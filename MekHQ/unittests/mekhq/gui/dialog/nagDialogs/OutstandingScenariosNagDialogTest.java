package mekhq.gui.dialog.nagDialogs;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static mekhq.gui.dialog.nagDialogs.OutstandingScenariosNagDialog.checkForOutstandingScenarios;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutstandingScenariosNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    private AtBContract contract;
    private AtBScenario scenario1, scenario2;
    private LocalDate today;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        contract = mock(AtBContract.class);
        scenario1 = mock(AtBScenario.class);
        scenario2 = mock(AtBScenario.class);
        today = LocalDate.now();

        // When the Campaign mock calls 'getLocalDate()' return today's date
        when(campaign.getLocalDate()).thenReturn(today);
    }

    // In the following tests the checkForOutstandingScenarios() method is called, and its response
    // is checked against expected behavior

    @Test
    void noContracts() {
        when(campaign.getActiveAtBContracts(true)).thenReturn(new ArrayList<>());

        assertFalse(checkForOutstandingScenarios(campaign));
    }

    @Test
    void noScenarios() {
        when(campaign.getActiveAtBContracts(true)).thenReturn(List.of(contract));
        when(contract.getCurrentAtBScenarios()).thenReturn(new ArrayList<>());

        assertFalse(checkForOutstandingScenarios(campaign));
    }

    @Test
    void noOutstandingScenarios() {
        when(campaign.getActiveAtBContracts(true)).thenReturn(List.of(contract));
        when(contract.getCurrentAtBScenarios()).thenReturn(List.of(scenario1, scenario2));

        when(scenario1.getDate()).thenReturn(today.plusDays(1));
        when(scenario2.getDate()).thenReturn(today.plusDays(1));

        assertFalse(checkForOutstandingScenarios(campaign));
    }

    @Test
    void oneOutstandingScenarioFirst() {
        when(campaign.getActiveAtBContracts(true)).thenReturn(List.of(contract));
        when(contract.getCurrentAtBScenarios()).thenReturn(List.of(scenario1, scenario2));

        when(scenario1.getDate()).thenReturn(today);
        when(scenario2.getDate()).thenReturn(today.plusDays(1));

        assertTrue(checkForOutstandingScenarios(campaign));
    }

    @Test
    void oneOutstandingScenarioSecond() {
        when(campaign.getActiveAtBContracts(true)).thenReturn(List.of(contract));
        when(contract.getCurrentAtBScenarios()).thenReturn(List.of(scenario1, scenario2));

        when(scenario1.getDate()).thenReturn(today.plusDays(1));
        when(scenario2.getDate()).thenReturn(today);

        assertTrue(checkForOutstandingScenarios(campaign));
    }

    @Test
    void twoOutstandingScenarios() {
        when(campaign.getActiveAtBContracts(true)).thenReturn(List.of(contract));
        when(contract.getCurrentAtBScenarios()).thenReturn(List.of(scenario1, scenario2));

        when(scenario1.getDate()).thenReturn(today);
        when(scenario2.getDate()).thenReturn(today);

        assertTrue(checkForOutstandingScenarios(campaign));
    }
}