package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Unit#canSalvage(boolean)}, which decides whether a unit can take part in CamOps salvage
 * operations.
 *
 * @author Illiani
 * @since 0.51.01
 */
class UnitCanSalvageTest {
    private static Tank tank(boolean isTrailer) {
        Tank tank = mock(Tank.class);
        when(tank.isTrailer()).thenReturn(isTrailer);
        when(tank.canPerformGroundSalvageOperations()).thenReturn(true);
        // Deliberately true, to show the tow rule itself doesn't apply in space
        when(tank.canPerformSpaceSalvageOperations()).thenReturn(true);
        return tank;
    }

    private static Unit unit(Entity entity, double cargoCapacity, boolean isFullyCrewed) {
        Unit unit = spy(new Unit(entity, mockCampaign()));
        doReturn(cargoCapacity).when(unit).getCargoCapacityForSalvage();
        doReturn(isFullyCrewed).when(unit).isFullyCrewed();
        return unit;
    }

    @Test
    void towOnlyVehicleCanSalvageOnTheGround() {
        assertTrue(unit(tank(false), 0.0, true).canSalvage(false));
    }

    @Test
    void towOnlyVehicleCannotSalvageInSpace() {
        assertFalse(unit(tank(false), 0.0, true).canSalvage(true));
    }

    @Test
    void trailerWithoutCargoCannotSalvage() {
        assertFalse(unit(tank(true), 0.0, true).canSalvage(false));
    }

    @Test
    void trailerWithCargoCanSalvage() {
        assertTrue(unit(tank(true), 10.0, true).canSalvage(false));
    }

    @Test
    void vehicleThatIsNotFullyCrewedCannotSalvage() {
        assertFalse(unit(tank(false), 10.0, false).canSalvage(false));
    }

    @Test
    void mekWithWorkingHandsCanSalvageWithoutCargo() {
        Mek mek = mock(Mek.class);
        when(mek.canPerformGroundSalvageOperations()).thenReturn(true);

        assertTrue(unit(mek, 0.0, true).canSalvage(false));
    }

    @Test
    void mekWithoutWorkingHandsCannotSalvage() {
        Mek mek = mock(Mek.class);
        when(mek.canPerformGroundSalvageOperations()).thenReturn(false);

        assertFalse(unit(mek, 0.0, true).canSalvage(false));
    }

    @Test
    void dropshipWithCargoCanSalvageInSpace() {
        Dropship dropship = mock(Dropship.class);
        when(dropship.canPerformSpaceSalvageOperations()).thenReturn(true);

        assertTrue(unit(dropship, 100.0, true).canSalvage(true));
    }

    @Test
    void dropshipWithoutCargoOrTugCannotSalvageInSpace() {
        Dropship dropship = mock(Dropship.class);
        when(dropship.canPerformSpaceSalvageOperations()).thenReturn(true);

        assertFalse(unit(dropship, 0.0, true).canSalvage(true));
    }

    @Test
    void unitWithoutEntityCannotSalvage() {
        Unit unit = spy(new Unit());

        assertFalse(unit.canSalvage(false));
        assertFalse(unit.canSalvage(true));
    }
}
