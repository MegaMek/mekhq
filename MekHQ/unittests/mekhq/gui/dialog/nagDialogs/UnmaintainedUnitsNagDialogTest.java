package mekhq.gui.dialog.nagDialogs;

import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Systems;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UnmaintainedUnitsNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    private Hangar hangar;
    private Unit mockUnit1, mockUnit2;

    // System setup for all tests, runs once before all tests
    @BeforeAll
    public static void setup() {
        try {
            Systems.setInstance(Systems.loadDefault());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    // Test setup for each test, runs before each test
    @BeforeEach
    public void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        hangar = mock(Hangar.class);
        mockUnit1 = mock(Unit.class);
        mockUnit2 = mock(Unit.class);

        // When the Campaign mock calls 'getHangar()' return the 'hangar' mock
        when(campaign.getHangar()).thenReturn(hangar);
    }

    // In the following tests,
    // Different combinations of unit states to set up desired behaviors in mock objects
    // Then the checkHanger() method of UnmaintainedUnitsNagDialog class is called,
    // and its response is checked against expected behavior

    @Test
    public void unmaintainedUnitExists() {
        when(mockUnit1.isUnmaintained()).thenReturn(false);
        when(mockUnit1.isSalvage()).thenReturn(false);

        when(mockUnit2.isUnmaintained()).thenReturn(true);
        when(mockUnit2.isSalvage()).thenReturn(false);

        List<Unit> units = List.of(mockUnit1, mockUnit2);
        when(hangar.getUnits()).thenReturn(units);

        UnmaintainedUnitsNagDialog nagDialog = new UnmaintainedUnitsNagDialog(null, campaign);
        assertTrue(nagDialog.checkHanger());
    }

    @Test
    public void unmaintainedUnitExistsButSalvageUnit1() {
        when(mockUnit1.isUnmaintained()).thenReturn(false);
        when(mockUnit1.isSalvage()).thenReturn(true);

        when(mockUnit2.isUnmaintained()).thenReturn(true);
        when(mockUnit2.isSalvage()).thenReturn(false);

        List<Unit> units = List.of(mockUnit1, mockUnit2);
        when(hangar.getUnits()).thenReturn(units);

        UnmaintainedUnitsNagDialog nagDialog = new UnmaintainedUnitsNagDialog(null, campaign);
        assertTrue(nagDialog.checkHanger());
    }

    @Test
    public void unmaintainedUnitExistsButSalvageUnit2() {
        when(mockUnit1.isUnmaintained()).thenReturn(false);
        when(mockUnit1.isSalvage()).thenReturn(false);

        when(mockUnit2.isUnmaintained()).thenReturn(true);
        when(mockUnit2.isSalvage()).thenReturn(true);

        List<Unit> units = List.of(mockUnit1, mockUnit2);
        when(hangar.getUnits()).thenReturn(units);

        UnmaintainedUnitsNagDialog nagDialog = new UnmaintainedUnitsNagDialog(null, campaign);
        assertFalse(nagDialog.checkHanger());
    }

    @Test
    public void unmaintainedUnitExistsButSalvageMixed() {
        when(mockUnit1.isUnmaintained()).thenReturn(false);
        when(mockUnit1.isSalvage()).thenReturn(true);

        when(mockUnit2.isUnmaintained()).thenReturn(true);
        when(mockUnit2.isSalvage()).thenReturn(false);

        List<Unit> units = List.of(mockUnit1, mockUnit2);
        when(hangar.getUnits()).thenReturn(units);

        UnmaintainedUnitsNagDialog nagDialog = new UnmaintainedUnitsNagDialog(null, campaign);
        assertTrue(nagDialog.checkHanger());
    }

    @Test
    public void noUnmaintainedUnitExists() {
        when(mockUnit1.isUnmaintained()).thenReturn(false);
        when(mockUnit1.isSalvage()).thenReturn(false);

        when(mockUnit2.isUnmaintained()).thenReturn(false);
        when(mockUnit2.isSalvage()).thenReturn(false);

        List<Unit> units = List.of(mockUnit1, mockUnit2);
        when(hangar.getUnits()).thenReturn(units);

        UnmaintainedUnitsNagDialog nagDialog = new UnmaintainedUnitsNagDialog(null, campaign);
        assertFalse(nagDialog.checkHanger());
    }

    @Test
    public void noUnmaintainedUnitExistsButSalvageUnit1() {
        when(mockUnit1.isUnmaintained()).thenReturn(false);
        when(mockUnit1.isSalvage()).thenReturn(true);

        when(mockUnit2.isUnmaintained()).thenReturn(false);
        when(mockUnit2.isSalvage()).thenReturn(false);

        List<Unit> units = List.of(mockUnit1, mockUnit2);
        when(hangar.getUnits()).thenReturn(units);

        UnmaintainedUnitsNagDialog nagDialog = new UnmaintainedUnitsNagDialog(null, campaign);
        assertFalse(nagDialog.checkHanger());
    }

    @Test
    public void noUnmaintainedUnitExistsButSalvageUnit2() {
        when(mockUnit1.isUnmaintained()).thenReturn(false);
        when(mockUnit1.isSalvage()).thenReturn(false);

        when(mockUnit2.isUnmaintained()).thenReturn(false);
        when(mockUnit2.isSalvage()).thenReturn(true);

        List<Unit> units = List.of(mockUnit1, mockUnit2);
        when(hangar.getUnits()).thenReturn(units);

        UnmaintainedUnitsNagDialog nagDialog = new UnmaintainedUnitsNagDialog(null, campaign);
        assertFalse(nagDialog.checkHanger());
    }

    @Test
    public void noUnmaintainedUnitExistsButSalvageMixed() {
        when(mockUnit1.isUnmaintained()).thenReturn(false);
        when(mockUnit1.isSalvage()).thenReturn(true);

        when(mockUnit2.isUnmaintained()).thenReturn(false);
        when(mockUnit2.isSalvage()).thenReturn(false);

        List<Unit> units = List.of(mockUnit1, mockUnit2);
        when(hangar.getUnits()).thenReturn(units);

        UnmaintainedUnitsNagDialog nagDialog = new UnmaintainedUnitsNagDialog(null, campaign);
        assertFalse(nagDialog.checkHanger());
    }
}
