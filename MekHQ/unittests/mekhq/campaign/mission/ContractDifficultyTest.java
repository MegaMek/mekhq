/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import megamek.common.enums.SkillLevel;
import megamek.common.units.Entity;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class ContractDifficultyTest {

    private static Stream<Arguments> provideContractDifficultyParameters() {
        return Stream.of(Arguments.of(500.0, 0.0, true, 10),
              Arguments.of(500.0, 0.0, false, 10),
              Arguments.of(500.0, 500.0, true, 5),
              Arguments.of(500.0, 500.0, false, 5),
              Arguments.of(500.0, 2000.0, true, 1),
              Arguments.of(500.0, 2000.0, false, 1),
              Arguments.of(500.0, 525.0, true, 5),
              Arguments.of(500.0, 525.0, false, 5),
              Arguments.of(500.0, 350.0, true, 7),
              Arguments.of(500.0, 350.0, false, 7),
              Arguments.of(0.0, 0.0, true, -99),
              Arguments.of(0.0, 0.0, false, -99));
    }

    @ParameterizedTest
    @MethodSource("provideContractDifficultyParameters")
    public void calculateContractDifficultySameSkillMatchesExpectedRating(double enemyBV, double playerBV,
          boolean useGenericBattleValue, int expectedResult) {
        AbstractMission mission = new AbstractMission();
        List<Entity> playerCombatUnits = new ArrayList<>();

        try (MockedStatic<ContractDifficulty> mockedDifficulty = mockStatic(ContractDifficulty.class,
              CALLS_REAL_METHODS)) {
            mockedDifficulty.when(() -> ContractDifficulty.modifySkillLevelBasedOnFaction(anyString(),
                  any(SkillLevel.class))).thenReturn(SkillLevel.REGULAR);
            mockedDifficulty.when(() -> ContractDifficulty.estimateMekStrength(anyInt(), anyBoolean(), anyString(),
                  anyInt())).thenReturn(enemyBV);
            mockedDifficulty.when(() -> ContractDifficulty.estimatePlayerPower(anyList(), anyBoolean()))
                  .thenReturn(playerBV);

            int difficulty = ContractDifficulty.calculateContractDifficulty(mission, 3025, useGenericBattleValue,
                  playerCombatUnits);

            assertEquals(expectedResult, difficulty);
        }
    }
}
