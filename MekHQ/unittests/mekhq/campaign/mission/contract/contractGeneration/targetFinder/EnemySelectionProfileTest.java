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
package mekhq.campaign.mission.contract.contractGeneration.targetFinder;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.location.ILocation;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RandomFactionGenerator;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link EnemySelectionProfile#PIRATE_VICTIM} profile: a pirate raid's victim is a legitimate regional power
 * (drawn as an employer would be), falling back to the standard enemy pool when no plausible victim is in range.
 *
 * @author Illiani
 * @since 0.51.01
 */
class EnemySelectionProfileTest {
    private static final LocalDate DATE = LocalDate.of(3025, 1, 1);

    @Test
    void pirateVictimDrawsALegitimateRegionalTarget() {
        RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
        ILocation location = mock(ILocation.class);
        Faction employer = mock(Faction.class);
        Faction victim = mock(Faction.class);
        when(generator.getRandomEmployerFaction(location, DATE, false)).thenReturn(victim);

        Faction selected = EnemySelectionProfile.PIRATE_VICTIM.selectEnemy(generator, location, DATE, employer);

        assertSame(victim, selected, "the victim is the regional power the band preys on");
    }

    @Test
    void pirateVictimFallsBackToTheStandardPoolWhenNoRegionalTarget() {
        RandomFactionGenerator generator = mock(RandomFactionGenerator.class);
        ILocation location = mock(ILocation.class);
        Faction employer = mock(Faction.class);
        Faction fallback = mock(Faction.class);
        when(generator.getRandomEmployerFaction(location, DATE, false)).thenReturn(null);
        when(generator.getRandomEnemy(false, location, DATE, employer)).thenReturn(fallback);

        Faction selected = EnemySelectionProfile.PIRATE_VICTIM.selectEnemy(generator, location, DATE, employer);

        assertSame(fallback, selected, "with no plausible victim, generation falls back to the standard enemy pool");
    }
}
