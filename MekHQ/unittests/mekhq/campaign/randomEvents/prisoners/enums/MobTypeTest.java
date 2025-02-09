package mekhq.campaign.randomEvents.prisoners.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A test class for validating the behavior and properties of the {@code MobType} enumeration.
 *
 * <p>This class contains unit tests to ensure that the ranges defined by each {@code MobType}
 * are consistent and correctly implemented. It checks whether the minimum value of each
 * subsequent {@code MobType} is one greater than the maximum value of the previous {@code MobType}.</p>
 *
 * <p>These tests aim to validate that the {@code MobType} enumerations are properly sequential and
 * follow the expected logical configuration.</p>
 */
class MobTypeTest {
    @Test
    void testToStringSmall() {
        int maximum = 0;
        for (MobType mobType : MobType.values()) {
            int minimum = mobType.getMinimum();

            assertEquals(maximum + 1, minimum);

            maximum = mobType.getMaximum();
        }
    }
}
