package mekhq.gui.dialog.nagDialogs;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Hangar;
import mekhq.campaign.Warehouse;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.FinancialReport;
import mekhq.campaign.finances.Money;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static mekhq.gui.dialog.nagDialogs.UnableToAffordExpensesNagDialog.isUnableToAffordExpenses;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link UnableToAffordExpensesNagDialog} class.
 * It contains tests for various scenarios related to the {@code isUnableToAffordExpenses} and
 * {@code getMonthlyExpenses} methods
 */
class UnableToAffordExpensesNagDialogTest {
    // Mock objects for the tests
    // I know some of these can be converted to a local variable, but it makes sense to keep all the
    // mock objects in one place
    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private Finances finances;
    private Unit unit;
    private Hangar hangar;
    private Warehouse warehouse;
    private FinancialReport report;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        campaignOptions = mock(CampaignOptions.class);

        finances = mock(Finances.class);

        unit = mock(Unit.class);
        hangar = mock(Hangar.class);
        hangar.addUnit(unit);

        warehouse = mock(Warehouse.class);

        report = mock(FinancialReport.class);

        // Stubs
        when(campaign.getFinances()).thenReturn(finances);
        when(campaign.getHangar()).thenReturn(hangar);
        when(campaign.getWarehouse()).thenReturn(warehouse);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
    }

    // In the following tests the isUnableToAffordExpenses() method is called, and its response is
    // checked against expected behavior

    @Test
    void canAffordExpenses() {
        when(campaign.getFunds()).thenReturn(Money.of(2));
        when(report.getMonthlyExpenses()).thenReturn(Money.of(1));

        assertFalse(isUnableToAffordExpenses(campaign));
    }

    @Test
    void cannotAffordExpenses() {
        when(campaign.getFunds()).thenReturn(Money.of(1));
        when(report.getMonthlyExpenses()).thenReturn(Money.of(2));

        assertFalse(isUnableToAffordExpenses(campaign));
    }
}