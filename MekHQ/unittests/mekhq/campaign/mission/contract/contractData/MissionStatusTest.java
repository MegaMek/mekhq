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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests {@link MissionStatus} - {@code parseFromString} (enum names plus legacy numeric codes, with the ACTIVE
 * fallback), and the predicate set. The two composite predicates carry the real logic: {@code isOverallSuccess} counts
 * a partial as a win, and {@code isCompleted} means "no longer running" for every terminal status.
 */
class MissionStatusTest {

    @ParameterizedTest
    @EnumSource(MissionStatus.class)
    void parsesEveryEnumNameBackToItself(final MissionStatus status) {
        assertSame(status, MissionStatus.parseFromString(status.name()));
    }

    @Test
    void parsesLegacyNumericCodes() {
        assertSame(MissionStatus.ACTIVE, MissionStatus.parseFromString("0"));
        assertSame(MissionStatus.SUCCESS, MissionStatus.parseFromString("1"));
        assertSame(MissionStatus.PARTIAL, MissionStatus.parseFromString("2"));
        assertSame(MissionStatus.FAILED, MissionStatus.parseFromString("3"));
        assertSame(MissionStatus.BREACH, MissionStatus.parseFromString("4"));
    }

    @Test
    void parsingAnUnknownValueFallsBackToActive() {
        assertSame(MissionStatus.ACTIVE, MissionStatus.parseFromString("5"),
              "an out-of-range numeric code must not throw - it falls back to ACTIVE");
        assertSame(MissionStatus.ACTIVE, MissionStatus.parseFromString("-1"));
        assertSame(MissionStatus.ACTIVE, MissionStatus.parseFromString("not-a-status"));
        assertSame(MissionStatus.ACTIVE, MissionStatus.parseFromString(""));
    }

    // region predicates

    @ParameterizedTest
    @EnumSource(MissionStatus.class)
    void exactlyOneBaseStatusPredicateIsTrueForEachConstant(final MissionStatus status) {
        int trueCount = (status.isActive() ? 1 : 0)
                              + (status.isSuccess() ? 1 : 0)
                              + (status.isPartialSuccess() ? 1 : 0)
                              + (status.isFailed() ? 1 : 0)
                              + (status.isBreach() ? 1 : 0);
        assertEquals(1, trueCount, status + " must satisfy exactly one of the base status predicates");
    }

    @Test
    void overallSuccessCountsSuccessAndPartialOnly() {
        assertTrue(MissionStatus.SUCCESS.isOverallSuccess());
        assertTrue(MissionStatus.PARTIAL.isOverallSuccess(), "a partial success still counts as an overall win");

        assertFalse(MissionStatus.ACTIVE.isOverallSuccess());
        assertFalse(MissionStatus.FAILED.isOverallSuccess());
        assertFalse(MissionStatus.BREACH.isOverallSuccess());
    }

    @Test
    void completedIsEveryTerminalStatusButNotActive() {
        assertFalse(MissionStatus.ACTIVE.isCompleted(), "an active mission is not completed");

        assertTrue(MissionStatus.SUCCESS.isCompleted());
        assertTrue(MissionStatus.PARTIAL.isCompleted());
        assertTrue(MissionStatus.FAILED.isCompleted());
        assertTrue(MissionStatus.BREACH.isCompleted());
    }
}
