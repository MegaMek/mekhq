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
package mekhq.campaign.mission.contract.contractData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ContractScheduleData} - when a contract runs.
 *
 * <p>Two behaviors carry weight here. {@link ContractScheduleData#isActiveOn} treats a missing endpoint as unbounded
 * on that side, which is how a contract with no settled schedule stays visible rather than vanishing. And the
 * {@code withStartDate}/{@code withEndDate} withers <em>recompute</em> the length from the resulting dates, which is
 * what makes an extension work - pushing the end date back lengthens the contract rather than leaving a stale figure
 * behind.</p>
 */
class ContractScheduleDataTest {
    private static final LocalDate START = LocalDate.of(3051, 1, 10);
    private static final LocalDate END = LocalDate.of(3051, 7, 10);

    private static ContractScheduleData sixMonthContract() {
        return new ContractScheduleData(START, END, 6);
    }

    // region isActiveOn

    @Test
    void aContractIsActiveOnBothOfItsEndpoints() {
        ContractScheduleData schedule = sixMonthContract();

        assertTrue(schedule.isActiveOn(START), "a contract counts from the day it starts");
        assertTrue(schedule.isActiveOn(END), "a contract counts through the day it ends");
    }

    @Test
    void aContractIsActiveBetweenItsEndpoints() {
        assertTrue(sixMonthContract().isActiveOn(LocalDate.of(3051, 4, 1)));
    }

    @Test
    void aContractIsInactiveOutsideItsEndpoints() {
        ContractScheduleData schedule = sixMonthContract();

        assertFalse(schedule.isActiveOn(START.minusDays(1)));
        assertFalse(schedule.isActiveOn(END.plusDays(1)));
    }

    @Test
    void anAbsentStartDateCountsAsAlreadyBegun() {
        ContractScheduleData schedule = new ContractScheduleData(null, END, 6);

        assertTrue(schedule.isActiveOn(LocalDate.of(3000, 1, 1)), "no start date means unbounded on that side");
        assertFalse(schedule.isActiveOn(END.plusDays(1)), "the end date still bounds it");
    }

    @Test
    void anAbsentEndDateCountsAsNotYetOver() {
        ContractScheduleData schedule = new ContractScheduleData(START, null, 6);

        assertTrue(schedule.isActiveOn(LocalDate.of(9999, 1, 1)), "no end date means unbounded on that side");
        assertFalse(schedule.isActiveOn(START.minusDays(1)), "the start date still bounds it");
    }

    @Test
    void aScheduleWithNeitherEndpointIsActiveOnAnyDay() {
        assertTrue(new ContractScheduleData(null, null, 0).isActiveOn(LocalDate.of(3051, 4, 1)));
    }

    // endregion isActiveOn

    // region withers

    @Test
    void aWitherLeavesTheOtherEndpointUntouched() {
        ContractScheduleData reEnded = sixMonthContract().withEndDate(END.plusMonths(3));

        assertEquals(START, reEnded.startDate(), "changing the end date must not move the start date");
    }

    @Test
    void pushingTheEndDateBackLengthensTheContract() {
        ContractScheduleData extended = sixMonthContract().withEndDate(END.plusMonths(3));

        assertEquals(START, extended.startDate(), "an extension does not move the start date");
        assertEquals(END.plusMonths(3), extended.endDate());
        assertEquals(9, extended.lengthInMonths(), "the length is recomputed, which is what makes an extension work");
    }

    @Test
    void pullingTheEndDateForwardShortensTheContract() {
        ContractScheduleData shortened = sixMonthContract().withEndDate(END.minusMonths(2));

        assertEquals(4, shortened.lengthInMonths());
    }

    @Test
    void movingOnlyTheStartDateAlsoChangesTheLength() {
        ContractScheduleData moved = sixMonthContract().withStartDate(START.plusMonths(2));

        assertEquals(4, moved.lengthInMonths(),
              "the length always describes the span the contract runs for, so a caller meaning to shift the whole "
                      + "contract must move both dates");
    }

    @Test
    void shiftingBothDatesLeavesTheLengthUnchanged() {
        ContractScheduleData shifted = sixMonthContract().withStartDate(START.plusYears(1))
                                               .withEndDate(END.plusYears(1));

        assertEquals(6, shifted.lengthInMonths(), "shifting the whole contract must not resize it");
    }

    @Test
    void lengthIsLeftAloneWhenEitherResultingDateIsAbsent() {
        ContractScheduleData openEnded = new ContractScheduleData(START, null, 6).withStartDate(START.plusMonths(2));

        assertEquals(6, openEnded.lengthInMonths(), "there is no span to measure, so the agreed length stands");
        assertNull(openEnded.endDate());
    }

    // endregion withers
}
