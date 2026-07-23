/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.gui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MapDimensionInput}, the validator/parser that guards the scenario template editor's free-text map
 * dimension fields against the unguarded {@code Integer.parseInt} crash they previously caused on save.
 */
class MapDimensionInputTest {

    @Test
    void validInputsReportNoErrors() {
        assertTrue(MapDimensionInput.validate("35", "45", "5", "5").isEmpty());
    }

    @Test
    void surroundingWhitespaceIsTolerated() {
        assertTrue(MapDimensionInput.validate("  35 ", "45\t", " 5", "5 ").isEmpty());
    }

    @Test
    void zeroIsAllowed() {
        assertTrue(MapDimensionInput.validate("0", "0", "0", "0").isEmpty());
    }

    @Test
    void blankInputIsReportedWithFieldName() {
        String errors = MapDimensionInput.validate("", "45", "5", "5");
        assertTrue(errors.contains("Base Width"), () -> "Expected Base Width error, got: " + errors);
    }

    @Test
    void nonNumericInputIsReported() {
        String errors = MapDimensionInput.validate("wide", "45", "5", "5");
        assertTrue(errors.contains("Base Width"), () -> "Expected Base Width error, got: " + errors);
    }

    @Test
    void negativeInputIsReported() {
        String errors = MapDimensionInput.validate("35", "-1", "5", "5");
        assertTrue(errors.contains("Base Height"), () -> "Expected Base Height error, got: " + errors);
    }

    @Test
    void nullInputIsReportedRatherThanThrowing() {
        String errors = MapDimensionInput.validate("35", "45", null, "5");
        assertTrue(errors.contains("Scaled Width Increment"), () -> "Expected increment error, got: " + errors);
    }

    @Test
    void everyInvalidFieldIsReportedOnItsOwnLine() {
        String errors = MapDimensionInput.validate("x", "y", "z", "w");
        assertEquals(4, errors.split("\n").length, () -> "Expected four problems, got: " + errors);
    }
}
