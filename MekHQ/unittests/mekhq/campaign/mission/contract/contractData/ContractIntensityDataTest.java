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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ContractIntensityData}: the zeroed default, the single-field {@code with*} copies, the immutability of
 * the stored schedule, and the schedule helper methods.
 */
class ContractIntensityDataTest {

    @Test
    void defaultConstructorIsZeroedWithAnEmptySchedule() {
        ContractIntensityData data = new ContractIntensityData();

        assertEquals(0, data.scale());
        assertEquals(0, data.requiredVictoryPoints());
        assertEquals(0, data.trackCount());
        assertTrue(data.monthlyTrackCounts().isEmpty());
    }

    @Test
    void withMethodsEachReplaceExactlyOneField() {
        ContractIntensityData base = new ContractIntensityData(1, 2, 4, List.of(0, 1));

        assertEquals(new ContractIntensityData(9, 2, 4, List.of(0, 1)),
              base.withScale(9));
        assertEquals(new ContractIntensityData(1, 9, 4, List.of(0, 1)),
              base.withRequiredVictoryPoints(9));
        assertEquals(new ContractIntensityData(1, 2, 9, List.of(0, 1)),
              base.withTrackCount(9));
        assertEquals(new ContractIntensityData(1, 2, 4, List.of(5, 6, 7)),
              base.withMonthlyTrackCounts(List.of(5, 6, 7)));
    }

    @Test
    void scheduleIsDefensivelyCopiedFromTheConstructor() {
        List<Integer> source = new ArrayList<>(List.of(1, 2, 3));
        ContractIntensityData data = new ContractIntensityData(0, 0, 6, source);

        // Mutating the caller's list must not reach into the record.
        source.set(0, 99);
        assertEquals(List.of(1, 2, 3), data.monthlyTrackCounts());
    }

    @Test
    void storedScheduleIsUnmodifiable() {
        ContractIntensityData data = new ContractIntensityData(0, 0, 3, List.of(0, 1, 2));

        assertThrows(UnsupportedOperationException.class, () -> data.monthlyTrackCounts().add(3));
    }

    @Test
    void scheduleHelpersReportLengthPerMonthAndTotal() {
        ContractIntensityData data = new ContractIntensityData(0, 0, 3, List.of(0, 2, 1));

        assertEquals(3, data.scheduleLengthInMonths());
        assertEquals(0, data.tracksInMonth(0));
        assertEquals(2, data.tracksInMonth(1));
        assertEquals(1, data.tracksInMonth(2));
        assertEquals(3, data.totalScheduledTracks());
    }

    @Test
    void emptyScheduleFactoryZeroFillsTheGivenLength() {
        assertEquals(List.of(0, 0, 0, 0), ContractIntensityData.emptySchedule(4));
        assertTrue(ContractIntensityData.emptySchedule(0).isEmpty());
    }
}
