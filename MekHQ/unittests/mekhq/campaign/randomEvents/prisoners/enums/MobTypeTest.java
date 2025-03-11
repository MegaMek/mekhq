/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
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
