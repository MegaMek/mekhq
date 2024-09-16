package mekhq.gui.dialog.nagDialogs;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static mekhq.gui.dialog.nagDialogs.PregnantCombatantNagDialog.isPregnantCombatant;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link PregnantCombatantNagDialog} class.
 * It contains tests for various scenarios related to the {@code isPregnantCombatant} method
 */
class PregnantCombatantNagDialogTest {
    // Mock objects for the tests
    private Campaign campaign;
    private Mission mission;
    private Person personNotPregnant;
    private Person personPregnant;
    private Unit unit;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        mission = mock(Mission.class);
        personNotPregnant = mock(Person.class);
        personPregnant = mock(Person.class);
        unit = mock(Unit.class);

        // Stubs
        when(personNotPregnant.isPregnant()).thenReturn(false);
        when(personPregnant.isPregnant()).thenReturn(true);
    }

    // In the following tests the isPregnantCombatant() method is called, and its response is
    // checked against expected behavior

    @Test
    void noActiveMission() {
        when(campaign.getActiveMissions(false)).thenReturn(new ArrayList<>());
        assertFalse(isPregnantCombatant(campaign));
    }

    @Test
    void activeMissionsNoPregnancy() {
        when(campaign.getActiveMissions(false)).thenReturn(List.of(mission));
        when(campaign.getActivePersonnel()).thenReturn(List.of(personNotPregnant));

        assertFalse(isPregnantCombatant(campaign));
    }

    @Test
    void activeMissionsPregnancyNoUnit() {
        when(campaign.getActiveMissions(false)).thenReturn(List.of(mission));
        when(campaign.getActivePersonnel()).thenReturn(List.of(personNotPregnant, personPregnant));

        when(personPregnant.getUnit()).thenReturn(null);

        assertFalse(isPregnantCombatant(campaign));
    }

    @Test
    void activeMissionsPregnancyNoForce() {
        when(campaign.getActiveMissions(false)).thenReturn(List.of(mission));
        when(campaign.getActivePersonnel()).thenReturn(List.of(personNotPregnant, personPregnant));

        when(personPregnant.getUnit()).thenReturn(unit);
        when(unit.getForceId()).thenReturn(Force.FORCE_NONE);

        assertFalse(isPregnantCombatant(campaign));
    }

    @Test
    void activeMissionsPregnancyYesUnitYesForce() {
        when(campaign.getActiveMissions(false)).thenReturn(List.of(mission));
        when(campaign.getActivePersonnel()).thenReturn(List.of(personNotPregnant, personPregnant));

        when(personPregnant.getUnit()).thenReturn(unit);
        when(unit.getForceId()).thenReturn(1);

        assertTrue(isPregnantCombatant(campaign));
    }
}