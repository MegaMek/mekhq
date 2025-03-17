package mekhq.gui.dialog.nagDialogs.nagLogic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test class verifies the functionality of the `hasAdminStrain` method
 * in the `AdminStrainNagLogic` class. The method checks if the given
 * admin strain value is positive.
 */
public class AdminStrainNagLogicTest {
    @Test
    void testHasAdminStrainWithPositiveValue() {
        int adminStrain = 10;
        boolean result = AdminStrainNagLogic.hasAdminStrain(adminStrain);
        assertTrue(result, "Admin strain should be positive and return true.");
    }

    @Test
    void testHasAdminStrainWithZeroValue() {
        int adminStrain = 0;
        boolean result = AdminStrainNagLogic.hasAdminStrain(adminStrain);
        assertFalse(result, "Admin strain of zero should return false.");
    }

    @Test
    void testHasAdminStrainWithNegativeValue() {
        int adminStrain = -5;
        boolean result = AdminStrainNagLogic.hasAdminStrain(adminStrain);
        assertFalse(result, "Admin strain should not be negative; it should return false.");
    }
}
