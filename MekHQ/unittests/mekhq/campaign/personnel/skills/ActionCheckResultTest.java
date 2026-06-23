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

package mekhq.campaign.personnel.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ActionCheckResultTest {

    @Test
    void testRecord() {
        ActionCheckResult result = new ActionCheckResult(8, 2, true, "Success");
        assertEquals(8, result.roll());
        assertEquals(2, result.marginOfSuccess());
        assertTrue(result.usedEdge());
        assertEquals("Success", result.resultsText());
    }

    @ParameterizedTest
    @CsvSource({ "-2, false", "-1, false", "0, true", "1, true", "12, true" })
    void testIsSuccess(int marginOfSuccess, boolean expectedResult) {
        ActionCheckResult result = new ActionCheckResult(10, marginOfSuccess, false, "");
        assertEquals(expectedResult, result.isSuccess());
    }

    @ParameterizedTest
    @CsvSource({ "-2, false", "-1, false", "0, true", "1, true", "12, true" })
    void testIsSuccessStatic(int marginOfSuccess, boolean expectedResult) {
        assertEquals(expectedResult, ActionCheckResult.isSuccess(marginOfSuccess));
    }

}
