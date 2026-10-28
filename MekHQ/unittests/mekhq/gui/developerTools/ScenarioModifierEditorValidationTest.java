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
package mekhq.gui.developerTools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Covers the number checks the scenario modifier editor runs before saving, so a typo is reported instead of being
 * silently saved as blank. Blank is valid: it means "no value".
 */
class ScenarioModifierEditorValidationTest {

    @ParameterizedTest
    @ValueSource(strings = { "", "  ", "0", "3", "-2", " 7 " })
    void acceptsBlankOrWholeNumbers(String text) {
        assertTrue(ScenarioModifierEditorDialog.isValidInteger(text));
    }

    @ParameterizedTest
    @ValueSource(strings = { "3x", "1.5", "abc", "--1", "99999999999" })
    void rejectsNonWholeNumbers(String text) {
        assertFalse(ScenarioModifierEditorDialog.isValidInteger(text));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "1", "0.25", "-1.5", " 2.0 " })
    void acceptsBlankOrDecimals(String text) {
        assertTrue(ScenarioModifierEditorDialog.isValidDecimal(text));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1.2.3", "abc", "NaN", "Infinity" })
    void rejectsNonFiniteOrMalformedDecimals(String text) {
        assertFalse(ScenarioModifierEditorDialog.isValidDecimal(text));
    }
}
