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
package mekhq.campaign.mission.newContract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.newContract.MissionLocationProfile;
import mekhq.campaign.universe.RandomFactionGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class ContractDeterminationLocationTest {
    private static final String EMPLOYER = "EMPLOYER";
    private static final String ENEMY = "ENEMY";
    private static final AtBContractType OBJECTIVE_TYPE = AtBContractType.PLANETARY_ASSAULT;

    @Test
    public void testPlayerAttacker_EmployerIsAttackerEnemyIsDefender() {
        ILocation location = mock(ILocation.class);
        RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
        when(generator.getMissionTarget(any(), any(), any(), any())).thenReturn("TARGET");

        try (MockedStatic<RandomFactionGenerator> generatorStatic = mockStatic(RandomFactionGenerator.class)) {
            generatorStatic.when(RandomFactionGenerator::getInstance).thenReturn(generator);

            String result = ContractDeterminationLocation.determineContractLocation(OBJECTIVE_TYPE, true,
                  EMPLOYER, ENEMY, location);

            assertEquals("TARGET", result);

            ArgumentCaptor<String> attacker = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> defender = ArgumentCaptor.forClass(String.class);
            verify(generator).getMissionTarget(attacker.capture(), defender.capture(), eq(location),
                  eq(OBJECTIVE_TYPE.getMissionLocationProfile()));

            assertEquals(EMPLOYER, attacker.getValue());
            assertEquals(ENEMY, defender.getValue());
        }
    }

    @Test
    public void testPlayerDefender_EnemyIsAttackerEmployerIsDefender() {
        ILocation location = mock(ILocation.class);
        RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
        when(generator.getMissionTarget(any(), any(), any(), any())).thenReturn("TARGET");

        try (MockedStatic<RandomFactionGenerator> generatorStatic = mockStatic(RandomFactionGenerator.class)) {
            generatorStatic.when(RandomFactionGenerator::getInstance).thenReturn(generator);

            String result = ContractDeterminationLocation.determineContractLocation(OBJECTIVE_TYPE, false,
                  EMPLOYER, ENEMY, location);

            assertEquals("TARGET", result);

            ArgumentCaptor<String> attacker = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> defender = ArgumentCaptor.forClass(String.class);
            verify(generator).getMissionTarget(attacker.capture(), defender.capture(), eq(location),
                  eq(OBJECTIVE_TYPE.getMissionLocationProfile()));

            assertEquals(ENEMY, attacker.getValue());
            assertEquals(EMPLOYER, defender.getValue());
        }
    }

    @Test
    public void testPassesMissionLocationProfileFromObjectiveType() {
        ILocation location = mock(ILocation.class);
        RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
        when(generator.getMissionTarget(any(), any(), any(), any())).thenReturn("TARGET");

        try (MockedStatic<RandomFactionGenerator> generatorStatic = mockStatic(RandomFactionGenerator.class)) {
            generatorStatic.when(RandomFactionGenerator::getInstance).thenReturn(generator);

            ContractDeterminationLocation.determineContractLocation(AtBContractType.RIOT_DUTY, true,
                  EMPLOYER, ENEMY, location);

            ArgumentCaptor<MissionLocationProfile> profile = ArgumentCaptor.forClass(MissionLocationProfile.class);
            verify(generator).getMissionTarget(any(), any(), eq(location), profile.capture());

            assertEquals(AtBContractType.RIOT_DUTY.getMissionLocationProfile(), profile.getValue());
        }
    }

    @Test
    public void testReturnsNullWhenGeneratorReturnsNull() {
        ILocation location = mock(ILocation.class);
        RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
        when(generator.getMissionTarget(any(), any(), any(), any())).thenReturn(null);

        try (MockedStatic<RandomFactionGenerator> generatorStatic = mockStatic(RandomFactionGenerator.class)) {
            generatorStatic.when(RandomFactionGenerator::getInstance).thenReturn(generator);

            String result = ContractDeterminationLocation.determineContractLocation(OBJECTIVE_TYPE, true,
                  EMPLOYER, ENEMY, location);

            assertNull(result);
        }
    }
}
