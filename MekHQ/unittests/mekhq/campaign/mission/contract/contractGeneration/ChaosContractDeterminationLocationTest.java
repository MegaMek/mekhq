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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.universe.RandomFactionGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests {@link ChaosContractDeterminationLocation#determineContractLocation}. The one branch worth pinning is the
 * attacker/defender assignment: when the player attacks, the employer is the attacker and the enemy the defender; when
 * the player defends, the roles swap. Getting that backwards would draw targets from the wrong faction's border.
 */
class ChaosContractDeterminationLocationTest {

    private static final String EMPLOYER = "EMP";
    private static final String ENEMY = "ENE";

    @Test
    void whenThePlayerAttacksTheEmployerIsTheAttacker() {
        ILocation location = mock(ILocation.class);
        RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
        when(generator.getMissionTarget(eq(EMPLOYER), eq(ENEMY), any(), any())).thenReturn("attacker-target");

        try (MockedStatic<RandomFactionGenerator> rfg = mockStatic(RandomFactionGenerator.class)) {
            rfg.when(RandomFactionGenerator::getInstance).thenReturn(generator);

            String target = ChaosContractDeterminationLocation.determineContractLocation(
                  ContractObjectiveType.GARRISON_DUTY, true, EMPLOYER, ENEMY, location);

            assertEquals("attacker-target", target, "employer should be passed as the attacker faction");
        }
    }

    @Test
    void whenThePlayerDefendsTheEnemyIsTheAttacker() {
        ILocation location = mock(ILocation.class);
        RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
        when(generator.getMissionTarget(eq(ENEMY), eq(EMPLOYER), any(), any())).thenReturn("defender-target");

        try (MockedStatic<RandomFactionGenerator> rfg = mockStatic(RandomFactionGenerator.class)) {
            rfg.when(RandomFactionGenerator::getInstance).thenReturn(generator);

            String target = ChaosContractDeterminationLocation.determineContractLocation(
                  ContractObjectiveType.GARRISON_DUTY, false, EMPLOYER, ENEMY, location);

            assertEquals("defender-target", target, "with the player defending, the enemy is the attacker faction");
        }
    }
}
