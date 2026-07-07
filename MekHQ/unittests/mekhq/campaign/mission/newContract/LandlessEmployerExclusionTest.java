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
package mekhq.campaign.mission.newContract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RandomFactionGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class LandlessEmployerExclusionTest {

    @Test
    void shouldReturnTrueWhenEmployerIsLandlessAndNotAttacker() {
        Faction mockedFaction = mock(Faction.class);
        when(mockedFaction.getShortName()).thenReturn("TEST");
        LocalDate currentDate = LocalDate.of(3100, 1, 1);

        try (MockedStatic<RandomFactionGenerator> mocked = mockStatic(RandomFactionGenerator.class)) {
            RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
            mocked.when(RandomFactionGenerator::getInstance).thenReturn(generator);
            when(generator.hasAnyTerritory(mockedFaction, currentDate)).thenReturn(false);

            assertTrue(LandlessEmployerExclusion.shouldRejectDefensiveObjectives(mockedFaction, false, currentDate));
        }
    }

    @Test
    void shouldReturnFalseWhenEmployerIsNotLandlessAndNotAttacker() {
        Faction mockedFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3100, 1, 1);

        try (MockedStatic<RandomFactionGenerator> mocked = mockStatic(RandomFactionGenerator.class)) {
            RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
            mocked.when(RandomFactionGenerator::getInstance).thenReturn(generator);
            when(generator.hasAnyTerritory(mockedFaction, currentDate)).thenReturn(true);

            assertFalse(LandlessEmployerExclusion.shouldRejectDefensiveObjectives(mockedFaction, false, currentDate));
        }
    }

    @Test
    void shouldReturnFalseWhenPlayerIsAttackerRegardlessOfLandlessStatus() {
        Faction mockedFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3100, 1, 1);

        try (MockedStatic<RandomFactionGenerator> mocked = mockStatic(RandomFactionGenerator.class)) {
            RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
            mocked.when(RandomFactionGenerator::getInstance).thenReturn(generator);
            when(generator.hasAnyTerritory(mockedFaction, currentDate)).thenReturn(false);

            assertFalse(LandlessEmployerExclusion.shouldRejectDefensiveObjectives(mockedFaction, true, currentDate));
        }
    }

    @Test
    void shouldReturnFalseWhenEmployerIsNullRegardlessOfPlayerRole() {
        assertFalse(LandlessEmployerExclusion.shouldRejectDefensiveObjectives(null, false, LocalDate.of(3100, 1, 1)));
    }
}
