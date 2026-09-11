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
package mekhq.campaign.mission.contract.contractGeneration;

import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import megamek.common.compute.Compute;
import megamek.common.icons.Camouflage;
import mekhq.campaign.Campaign;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.contract.contractData.EnemyData;
import mekhq.campaign.mission.contract.contractGeneration.targetFinder.EnemySelectionProfile;
import mekhq.campaign.mission.utilities.RandomFactionCamouflage;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RandomFactionGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Exercises {@link ChaosContractDeterminationEnemy#generateEnemyForFaction(Campaign, Faction, LocalDate)} &mdash; the
 * method that wraps a fixed enemy faction into {@link EnemyData}, including the mercenary-sponsor substitution used
 * when the enemy fields hired guns. Camouflage lookups are stubbed so no universe fixtures are required.
 */
class ChaosContractDeterminationEnemyTest {

    private static final int YEAR = 3050;
    private static final LocalDate DATE = LocalDate.of(YEAR, 1, 1);

    /**
     * A campaign whose personnel generator yields no one, so {@code generateOpposingCommander} short-circuits to
     * {@code null}. The commander is orthogonal to the faction-code and camouflage resolution under test here.
     */
    private static Campaign campaignWithoutPersonnel() {
        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        when(campaign.getPlayerForce().getHumanResources().newPerson(any(), any(), anyString(), any()))
              .thenReturn(null);
        return campaign;
    }

    private static Faction enemyFaction(boolean usesMercenaries) {
        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn("DC");
        when(faction.getFullName(YEAR)).thenReturn("Draconis Combine");
        when(faction.isUsesMercenaries(YEAR)).thenReturn(usesMercenaries);
        return faction;
    }

    @Test
    void enemyFightingItsOwnWarKeepsItsFactionCodeAndHasNoSponsor() {
        Camouflage camouflage = mock(Camouflage.class);

        try (MockedStatic<RandomFactionCamouflage> camo = mockStatic(RandomFactionCamouflage.class)) {
            camo.when(() -> RandomFactionCamouflage.pickRandomCamouflage(anyInt(), anyString()))
                  .thenReturn(camouflage);

            EnemyData enemy = ChaosContractDeterminationEnemy.generateEnemyForFaction(campaignWithoutPersonnel(),
                  enemyFaction(false),
                  DATE);

            assertEquals("DC", enemy.factionCode());
            assertNull(enemy.sponsorFactionCode(), "an enemy fighting its own war has no sponsor");
            assertEquals("Draconis Combine", enemy.displayName());
            assertSame(camouflage, enemy.camouflage());
        }
    }

    @Test
    void enemyThatFieldsMercenariesIsDisguisedAsMercWithTheRealFactionAsSponsor() {
        Camouflage camouflage = mock(Camouflage.class);

        try (MockedStatic<RandomFactionCamouflage> camo = mockStatic(RandomFactionCamouflage.class);
              MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            camo.when(() -> RandomFactionCamouflage.pickRandomCamouflage(anyInt(), anyString()))
                  .thenReturn(camouflage);
            // MERCENARY_ENEMY_CHANCE roll of 0 means mercenaries were hired.
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(0);

            EnemyData enemy = ChaosContractDeterminationEnemy.generateEnemyForFaction(campaignWithoutPersonnel(),
                  enemyFaction(true),
                  DATE);

            assertEquals(MERCENARY_FACTION_CODE, enemy.factionCode(), "the visible combatant is the mercenaries");
            assertEquals("DC", enemy.sponsorFactionCode(), "the real faction is recorded as the hidden sponsor");
        }
    }

    @Test
    void enemyThatAllowsMercenariesButDidNotHireThemKeepsItsFactionCode() {
        Camouflage camouflage = mock(Camouflage.class);

        try (MockedStatic<RandomFactionCamouflage> camo = mockStatic(RandomFactionCamouflage.class);
              MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            camo.when(() -> RandomFactionCamouflage.pickRandomCamouflage(anyInt(), anyString()))
                  .thenReturn(camouflage);
            // Any non-zero roll means no mercenaries were hired this time.
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(1);

            EnemyData enemy = ChaosContractDeterminationEnemy.generateEnemyForFaction(campaignWithoutPersonnel(),
                  enemyFaction(true),
                  DATE);

            assertEquals("DC", enemy.factionCode());
            assertNull(enemy.sponsorFactionCode());
        }
    }

    @Test
    void covertContractDrawsTheEnemyUnderTheCovertProfileRatherThanTheObjectiveProfile() {
        // A profile that is deliberately NOT COVERT, so a pass-through of the objective's own profile would be visible.
        ContractObjectiveType objectiveType = ContractObjectiveType.PIRATE_HUNTING;
        assertNotEquals(EnemySelectionProfile.COVERT, objectiveType.getEnemySelectionProfile(),
              "test fixture must use an objective whose own profile is not COVERT");

        assertProfilePassedToRandomEnemy(objectiveType, true, EnemySelectionProfile.COVERT);
    }

    @Test
    void nonCovertContractDrawsTheEnemyUnderTheObjectiveOwnProfile() {
        ContractObjectiveType objectiveType = ContractObjectiveType.PIRATE_HUNTING;

        assertProfilePassedToRandomEnemy(objectiveType, false, objectiveType.getEnemySelectionProfile());
    }

    /**
     * Runs {@code generateEnemyFactionForObjective} with the given covert flag and verifies that the
     * {@link EnemySelectionProfile} handed to {@link RandomFactionGenerator#getRandomEnemy} is the one expected. The
     * generator singleton and camouflage lookup are stubbed so no universe fixtures are required.
     */
    private static void assertProfilePassedToRandomEnemy(ContractObjectiveType objectiveType, boolean isCovert,
          EnemySelectionProfile expectedProfile) {
        ILocation location = mock(ILocation.class);
        Faction employer = mock(Faction.class);
        Faction drawnEnemy = enemyFaction(false);

        RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
        when(generator.getRandomEnemy(any(), any(), any(), any())).thenReturn(drawnEnemy);

        try (MockedStatic<RandomFactionGenerator> factionGenerator = mockStatic(RandomFactionGenerator.class);
              MockedStatic<RandomFactionCamouflage> camo = mockStatic(RandomFactionCamouflage.class)) {
            factionGenerator.when(RandomFactionGenerator::getInstance).thenReturn(generator);
            camo.when(() -> RandomFactionCamouflage.pickRandomCamouflage(anyInt(), anyString()))
                  .thenReturn(mock(Camouflage.class));

            ChaosContractDeterminationEnemy.generateEnemyFactionForObjective(campaignWithoutPersonnel(), location, DATE,
                  employer, objectiveType, isCovert);

            verify(generator).getRandomEnemy(eq(location), eq(DATE), eq(employer), eq(expectedProfile));
        }
    }
}
