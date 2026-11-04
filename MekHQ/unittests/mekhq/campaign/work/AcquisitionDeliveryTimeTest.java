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
package mekhq.campaign.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;
import mekhq.utilities.ReportingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AcquisitionDeliveryTimeTest {
    @ParameterizedTest
    @CsvSource({ "0, 0", "1, 10", "3, 30", "5, 50", "9, 50", "-2, 0" })
    void testGetDeliveryReductionPercent(int marginOfSuccess, int expectedPercent) {
        assertEquals(expectedPercent, AcquisitionDeliveryTime.getDeliveryReductionPercent(marginOfSuccess));
    }

    @ParameterizedTest
    @CsvSource({ "60, 0, 60", "60, 1, 54", "60, 3, 42", "60, 5, 30", "60, 12, 30", "3, 5, 2", "2, 5, 1", "1, 5, 1",
                 "0, 5, 0" })
    void testApplyMarginOfSuccess(int transitDays, int marginOfSuccess, int expectedDays) {
        assertEquals(expectedDays, AcquisitionDeliveryTime.applyMarginOfSuccess(transitDays, marginOfSuccess));
    }

    @Test
    void testGetDeliveryMarginOfSuccess_success() {
        SkillCheck skillCheck = mockSkillCheck(new TargetRoll(7, "Base"));
        ActionCheckResult result = mockResult(3);

        assertEquals(3, AcquisitionDeliveryTime.getDeliveryMarginOfSuccess(skillCheck, result));
    }

    @Test
    void testGetDeliveryMarginOfSuccess_cappedAtMaximum() {
        SkillCheck skillCheck = mockSkillCheck(new TargetRoll(2, "Base"));
        ActionCheckResult result = mockResult(10);

        assertEquals(AcquisitionDeliveryTime.MARGIN_OF_SUCCESS_MAX,
              AcquisitionDeliveryTime.getDeliveryMarginOfSuccess(skillCheck, result));
    }

    @Test
    void testGetDeliveryMarginOfSuccess_failure() {
        SkillCheck skillCheck = mockSkillCheck(new TargetRoll(10, "Base"));
        ActionCheckResult result = mockResult(-2);

        assertEquals(0, AcquisitionDeliveryTime.getDeliveryMarginOfSuccess(skillCheck, result));
    }

    @Test
    void testGetDeliveryMarginOfSuccess_automaticSuccess() {
        SkillCheck skillCheck = mockSkillCheck(new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "Automatic Success"));
        ActionCheckResult result = mockResult(MarginOfSuccess.SPECTACULAR.getLowerBound());

        assertEquals(0, AcquisitionDeliveryTime.getDeliveryMarginOfSuccess(skillCheck, result));
    }

    @Test
    void testGetDeliveryReport_noReduction() {
        assertEquals("", AcquisitionDeliveryTime.getDeliveryReport(0));
    }

    @Test
    void testGetDeliveryReport_good() {
        assertEquals("Delivery <span color='positive'><b>expedited by 30%</b></span> (Margin of Success 3).",
              AcquisitionDeliveryTime.getDeliveryReport(3)
                    .replace(ReportingUtilities.getPositiveColor(), "positive"));
    }

    private static SkillCheck mockSkillCheck(TargetRoll targetRoll) {
        SkillCheck skillCheck = mock(SkillCheck.class);
        when(skillCheck.getTargetNumber()).thenReturn(targetRoll);
        return skillCheck;
    }

    private static ActionCheckResult mockResult(int marginOfSuccess) {
        ActionCheckResult result = mock(ActionCheckResult.class);
        when(result.getMarginOfSuccess()).thenReturn(marginOfSuccess);
        when(result.isSuccess()).thenReturn(ActionCheckResult.isSuccess(marginOfSuccess));
        return result;
    }
}
