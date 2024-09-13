/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
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
