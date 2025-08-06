/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package asserts;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Assertions;

/**
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 */
public class BigDecimalAssert {
    public static void assertEquals(BigDecimal expected, Object actual, int scale) {
        assertNotNull(actual);

        assertInstanceOf(BigDecimal.class, actual, "actual: " + actual.getClass().getName());
        BigDecimal scaledExpected = expected.setScale(scale, RoundingMode.FLOOR);
        BigDecimal scaledActual = ((BigDecimal) actual).setScale(scale, RoundingMode.FLOOR);
        Assertions.assertEquals(0, scaledExpected.compareTo(scaledActual),
              "\n\texpected: " + scaledExpected + "\n\tactual: " + scaledActual);
    }
}
