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
import mekhq.utilities.ReportingUtilities;
import org.junit.jupiter.api.Test;

class AppraisalTest {
    @Test
    void testGetAppraisalCostMultiplier_withPositiveMargin() {
        double multiplier = Appraisal.getAppraisalCostMultiplier(4);
        assertEquals(0.9, multiplier);
    }

    @Test
    void testGetAppraisalCostMultiplier_withNegativeMargin() {
        double multiplier = Appraisal.getAppraisalCostMultiplier(-2);
        assertEquals(1.05, multiplier);
    }

    @Test
    void testGetAppraisalCostMultiplier_withZeroMargin() {
        double multiplier = Appraisal.getAppraisalCostMultiplier(0);
        assertEquals(1.0, multiplier);
    }

    @Test
    void testGetAppraisalCostMultiplier_withAutomaticSuccess() {
        double multiplier = Appraisal.getAppraisalCostMultiplier(100);
        assertEquals(0.85, multiplier);
    }

    @Test
    void testGetAppraisalCostMultiplier_withAutomaticFailure() {
        double multiplier = Appraisal.getAppraisalCostMultiplier(-100);
        assertEquals(1.15, multiplier);
    }

    @Test
    void testGetAppraisalReport_Almost() {
        assertEquals("They got a <span color='warning'><b>Below Average</b></span> deal. " +
                           "Margin of Failure 1, price increased to <b>105%</b>.",
              Appraisal.getAppraisalReport(1.05, MarginOfSuccess.ALMOST)
                    .replace(ReportingUtilities.getWarningColor(), "warning"));
    }

    @Test
    void testGetAppraisalReport_Extraordinary() {
        assertEquals("They got an <span color='positive'><b>Extraordinary</b></span> deal! " +
                           "Margin of Success 3, price reduced to <b>80%</b>.",
              Appraisal.getAppraisalReport(0.8, MarginOfSuccess.EXTRAORDINARY)
                    .replace(ReportingUtilities.getPositiveColor(), "positive"));
    }

}
