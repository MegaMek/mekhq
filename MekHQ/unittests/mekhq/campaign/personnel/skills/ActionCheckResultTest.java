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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.utilities.ReportingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

class ActionCheckResultTest {

    @Test
    void testGetReport_EdgeNotUsed() {
        ActionCheckResult result = new ActionCheckResult(new ActionCheckRoll(3, List.of()), 1, false, "Success.");
        assertEquals(3, result.getRollResult());
        assertEquals(1, result.getMarginOfSuccess());
        assertFalse(result.hasUsedEdge());
        assertEquals("Success.", result.getReport(false));
        assertEquals("Success. <span color='positive'><i>It'll do...</i></span>",
              result.getReport(true).replace(ReportingUtilities.getPositiveColor(), "positive"));
    }

    @Test
    void testGetReport_EdgeUsed() {
        ActionCheckResult result = new ActionCheckResult(new ActionCheckRoll(8, List.of()), 2, true, "Success.");
        assertEquals(8, result.getRollResult());
        assertEquals(2, result.getMarginOfSuccess());
        assertTrue(result.hasUsedEdge());
        assertEquals("Success. Used a point of <b>Edge</b>.", result.getReport(false));
        assertEquals("Success. Used a point of <b>Edge</b>. <span color='positive'><i>Good.</i></span>",
              result.getReport(true).replace(ReportingUtilities.getPositiveColor(), "positive"));
    }

    @ParameterizedTest
    @CsvSource({ "-2, false", "-1, false", "0, true", "1, true", "12, true" })
    void testIsSuccess(int marginOfSuccess, boolean expectedResult) {
        ActionCheckResult result = new ActionCheckResult(new ActionCheckRoll(10, List.of()), marginOfSuccess, false, "");
        assertEquals(expectedResult, result.isSuccess());
    }

    @ParameterizedTest
    @CsvSource({ "-2, false", "-1, false", "0, true", "1, true", "12, true" })
    void testIsSuccessStatic(int marginOfSuccess, boolean expectedResult) {
        assertEquals(expectedResult, ActionCheckResult.isSuccess(marginOfSuccess));
    }

}
