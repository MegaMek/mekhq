package mekhq.gui.dialog.nagDialogs;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static mekhq.gui.dialog.nagDialogs.InvalidFactionNagDialog.federatedSunsSpecialHandler;
import static mekhq.gui.dialog.nagDialogs.InvalidFactionNagDialog.isFactionInvalid;
import static mekhq.gui.dialog.nagDialogs.InvalidFactionNagDialog.lyranAllianceSpecialHandler;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link InvalidFactionNagDialog} class.
 * <p>
 * It tests the different combinations of unit states and verifies the behavior of the
 * {@code isFactionInvalid()}, {@code lyranAllianceSpecialHandler()}, and
 * {@code federatedSunsSpecialHandler()} methods.
 */
class InvalidFactionNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    private Faction faction;
    private LocalDate dateValid;
    private LocalDate dateInvalid;

    /**
     * Sets up the necessary dependencies and configurations before running the test methods.
     * Runs once before all tests
     */
    @BeforeEach
    public void setup() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        faction = mock(Faction.class);

        dateValid = LocalDate.of(3151, 1, 1);
        dateInvalid = LocalDate.of(1936, 1, 1);

        // When the Campaign mock calls 'getFaction()' return the mocked faction
        when(campaign.getFaction()).thenReturn(faction);
    }

    // In the following tests, the beginning of the isFactionInvalid() method is called, and its
    // response is checked against expected behavior

    @Test
    public void validDate() {
        when(faction.validIn(dateValid)).thenReturn(true);
        when(campaign.getLocalDate()).thenReturn(dateValid);

        assertFalse(isFactionInvalid(campaign));
    }

    @Test
    public void invalidDate() {
        when(faction.validIn(dateInvalid)).thenReturn(false);
        when(campaign.getLocalDate()).thenReturn(dateInvalid);

        assertTrue(isFactionInvalid(campaign));
    }

    // In the following tests, the lyranAllianceSpecialHandler() method is called, and its response
    // is checked against expected behavior

    @Test
    public void lyranAllianceSpecialHandlerInvalidDate() {
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3067, 4, 19));
        assertTrue(lyranAllianceSpecialHandler(campaign));
    }

    @Test
    public void lyranAllianceSpecialHandlerBeforeActiveDate() {
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3040, 1, 17));
        assertFalse(lyranAllianceSpecialHandler(campaign));
    }

    @Test
    public void lyranAllianceSpecialHandlerAfterInactiveDate() {
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3067, 4, 21));
        assertFalse(lyranAllianceSpecialHandler(campaign));
    }

    // In the following tests, the federatedSunsSpecialHandler() method is called, and its response
    // is checked against expected behavior

    @Test
    public void federatedSunsSpecialHandlerInvalidDate() {
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3057, 9, 17));

        assertTrue(federatedSunsSpecialHandler(campaign));
    }

    @Test
    public void federatedSunsSpecialHandlerBeforeActiveDate() {
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3040, 1, 17));

        assertFalse(federatedSunsSpecialHandler(campaign));
    }

    @Test
    public void federatedSunsSpecialHandlerAfterInactiveDate() {
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3057, 9, 19));

        assertFalse(federatedSunsSpecialHandler(campaign));
    }
}