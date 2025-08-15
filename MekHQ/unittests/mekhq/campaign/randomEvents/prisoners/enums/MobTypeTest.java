/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.randomEvents.prisoners.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * A test class for validating the behavior and properties of the {@code MobType} enumeration.
 *
 * <p>This class contains unit tests to ensure that the ranges defined by each {@code MobType}
 * are consistent and correctly implemented. It checks whether the minimum value of each subsequent {@code MobType} is
 * one greater than the maximum value of the previous {@code MobType}.</p>
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
