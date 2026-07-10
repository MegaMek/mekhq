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

import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.REBEL_FACTION_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDate;

import megamek.common.compute.Compute;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.newContract.EnemySelectionProfile;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class ContractDeterminationObjectiveEnemyTest {
    private static final LocalDate DATE = LocalDate.of(3025, 1, 1);
    private static final AtBContractType OBJECTIVE_TYPE = AtBContractType.PIRATE_HUNTING;

    @Test
    public void testGenerateEnemyFactionForObjective_ReturnsRandomEnemyWhenFound() {
        AbstractLocation location = mock(AbstractLocation.class);
        Faction employer = mock(Faction.class);
        Faction enemy = mock(Faction.class);

        RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
        when(generator.getRandomEnemy(any(), any(), any(), any())).thenReturn(enemy);

        try (MockedStatic<RandomFactionGenerator> generatorStatic = mockStatic(RandomFactionGenerator.class)) {
            generatorStatic.when(RandomFactionGenerator::getInstance).thenReturn(generator);

            Faction result = ContractDeterminationObjectiveEnemy.generateEnemyFactionForObjective(location, DATE,
                  employer, OBJECTIVE_TYPE);

            assertSame(enemy, result);
        }
    }

    @Test
    public void testGenerateEnemyFactionForObjective_PassesThroughArgumentsToGenerator() {
        AbstractLocation location = mock(AbstractLocation.class);
        Faction employer = mock(Faction.class);
        Faction enemy = mock(Faction.class);

        RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
        when(generator.getRandomEnemy(any(), any(), any(), any())).thenReturn(enemy);

        try (MockedStatic<RandomFactionGenerator> generatorStatic = mockStatic(RandomFactionGenerator.class)) {
            generatorStatic.when(RandomFactionGenerator::getInstance).thenReturn(generator);

            ContractDeterminationObjectiveEnemy.generateEnemyFactionForObjective(location, DATE, employer,
                  OBJECTIVE_TYPE);

            ArgumentCaptor<EnemySelectionProfile> profile = ArgumentCaptor.forClass(EnemySelectionProfile.class);
            verify(generator).getRandomEnemy(eq(location), eq(DATE), eq(employer), profile.capture());

            assertEquals(OBJECTIVE_TYPE.getEnemySelectionProfile(), profile.getValue());
        }
    }

    @Test
    public void testGenerateEnemyFactionForObjective_FallsBackToRebelsWhenNoEnemyFound() {
        AbstractLocation location = mock(AbstractLocation.class);
        Faction employer = mock(Faction.class);
        when(employer.getShortName()).thenReturn("EMPLOYER");
        Faction rebels = mock(Faction.class);

        RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
        when(generator.getRandomEnemy(any(), any(), any(), any())).thenReturn(null);

        Factions factions = mock(Factions.class);
        when(factions.getFaction(REBEL_FACTION_CODE)).thenReturn(rebels);

        try (MockedStatic<RandomFactionGenerator> generatorStatic = mockStatic(RandomFactionGenerator.class);
              MockedStatic<Factions> factionsStatic = mockStatic(Factions.class)) {
            generatorStatic.when(RandomFactionGenerator::getInstance).thenReturn(generator);
            factionsStatic.when(Factions::getInstance).thenReturn(factions);

            Faction result = ContractDeterminationObjectiveEnemy.generateEnemyFactionForObjective(location, DATE,
                  employer, OBJECTIVE_TYPE);

            assertSame(rebels, result);
        }
    }

    // ---- hasEmployedMercenaries (private; invoked reflectively) ------------------------------

    private static final int MERCENARY_ENEMY_CHANCE = 20;

    private static Faction invokeHasEmployedMercenaries(Faction employerFaction, LocalDate currentDate)
          throws Exception {
        Method method = ContractDeterminationObjectiveEnemy.class.getDeclaredMethod("hasEmployedMercenaries",
              Faction.class, LocalDate.class);
        method.setAccessible(true);
        return (Faction) method.invoke(null, employerFaction, currentDate);
    }

    @Test
    public void testHasEmployedMercenaries_EmployerDisallowsMercenaries_ReturnsNull() throws Exception {
        Faction employer = mock(Faction.class);
        when(employer.isUsesMercenaries(DATE.getYear())).thenReturn(false);

        // Neither the roll nor the faction lookup should be consulted; if they were, these statics would return
        // Mockito defaults and the result would still be null, so we mock them to prove they are not the source.
        try (MockedStatic<Compute> computeStatic = mockStatic(Compute.class);
              MockedStatic<Factions> factionsStatic = mockStatic(Factions.class)) {
            assertNull(invokeHasEmployedMercenaries(employer, DATE));
        }
    }

    @Test
    public void testHasEmployedMercenaries_AllowedAndRollZero_ReturnsMercenaryFaction() throws Exception {
        Faction employer = mock(Faction.class);
        when(employer.isUsesMercenaries(DATE.getYear())).thenReturn(true);
        Faction mercenaries = mock(Faction.class);

        Factions factions = mock(Factions.class);
        when(factions.getFaction(MERCENARY_FACTION_CODE)).thenReturn(mercenaries);

        try (MockedStatic<Compute> computeStatic = mockStatic(Compute.class);
              MockedStatic<Factions> factionsStatic = mockStatic(Factions.class)) {
            computeStatic.when(() -> Compute.randomInt(MERCENARY_ENEMY_CHANCE)).thenReturn(0);
            factionsStatic.when(Factions::getInstance).thenReturn(factions);

            assertSame(mercenaries, invokeHasEmployedMercenaries(employer, DATE));
        }
    }

    @Test
    public void testHasEmployedMercenaries_AllowedButRollNonZero_ReturnsNull() throws Exception {
        Faction employer = mock(Faction.class);
        when(employer.isUsesMercenaries(DATE.getYear())).thenReturn(true);

        try (MockedStatic<Compute> computeStatic = mockStatic(Compute.class);
              MockedStatic<Factions> factionsStatic = mockStatic(Factions.class)) {
            computeStatic.when(() -> Compute.randomInt(MERCENARY_ENEMY_CHANCE)).thenReturn(1);

            assertNull(invokeHasEmployedMercenaries(employer, DATE));
        }
    }
}
