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
package mekhq.campaign.personnel.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;

import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;
import org.junit.jupiter.api.Test;

class AppraisalTest {
    @Test
    void testGetAppraisalCostMultiplier_withPositiveMargin() {
        double multiplier = Appraisal.getAppraisalCostMultiplier(4);
        assertEquals(0.8, multiplier);
    }

    @Test
    void testGetAppraisalCostMultiplier_withNegativeMargin() {
        double multiplier = Appraisal.getAppraisalCostMultiplier(-2);
        assertEquals(1.1, multiplier);
    }

    @Test
    void testGetAppraisalCostMultiplier_withZeroMargin() {
        double multiplier = Appraisal.getAppraisalCostMultiplier(0);
        assertEquals(1.0, multiplier);
    }

    @Test
    void testGetMarginOfSuccess_withPositiveMultiplier() {
        MarginOfSuccess marginOfSuccess = Appraisal.getMarginOfSuccess(0.8);
        assertEquals(MarginOfSuccess.SPECTACULAR, marginOfSuccess);
    }

    @Test
    void testGetMarginOfSuccess_withNegativeMultiplier() {
        MarginOfSuccess marginOfSuccess = Appraisal.getMarginOfSuccess(1.1);
        assertEquals(MarginOfSuccess.BAD, marginOfSuccess);
    }

    @Test
    void testGetMarginOfSuccess_withZeroMultiplier() {
        MarginOfSuccess marginOfSuccess = Appraisal.getMarginOfSuccess(1.0);
        assertEquals(MarginOfSuccess.BARELY_MADE_IT, marginOfSuccess);
    }
}
