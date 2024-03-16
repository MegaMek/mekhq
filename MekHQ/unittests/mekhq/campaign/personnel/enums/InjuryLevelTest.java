/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InjuryLevelTest {
    //region Variable Declarations
    private static final InjuryLevel[] levels = InjuryLevel.values();
    //endregion Variable Declarations

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final InjuryLevel level : levels) {
            if (level == InjuryLevel.NONE) {
                assertTrue(level.isNone());
            } else {
                assertFalse(level.isNone());
            }
        }
    }

    @Test
    public void testIsChronic() {
        for (final InjuryLevel level : levels) {
            if (level == InjuryLevel.CHRONIC) {
                assertTrue(level.isChronic());
            } else {
                assertFalse(level.isChronic());
            }
        }
    }

    @Test
    public void testIsMinor() {
        for (final InjuryLevel level : levels) {
            if (level == InjuryLevel.MINOR) {
                assertTrue(level.isMinor());
            } else {
                assertFalse(level.isMinor());
            }
        }
    }

    @Test
    public void testIsMajor() {
        for (final InjuryLevel level : levels) {
            if (level == InjuryLevel.MAJOR) {
                assertTrue(level.isMajor());
            } else {
                assertFalse(level.isMajor());
            }
        }
    }

    @Test
    public void testIsDeadly() {
        for (final InjuryLevel level : levels) {
            if (level == InjuryLevel.DEADLY) {
                assertTrue(level.isDeadly());
            } else {
                assertFalse(level.isDeadly());
            }
        }
    }

    @Test
    public void testIsMajorOrDeadly() {
        for (final InjuryLevel level : levels) {
            if ((level == InjuryLevel.MAJOR) || (level == InjuryLevel.DEADLY)) {
                assertTrue(level.isMajorOrDeadly());
            } else {
                assertFalse(level.isMajorOrDeadly());
            }
        }
    }
    //endregion Boolean Comparison Methods
}
