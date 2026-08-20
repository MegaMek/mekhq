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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link SystemsTargetData}, which stores a contract's destination as ids into the universe data.
 *
 * <p>Both ids are optional - a contract need not name a world within its system, and a converted legacy contract may
 * name no system at all - and the universe the ids point into may not contain them (a save from a different data set,
 * or a world that does not exist at the queried date). Every accessor has to cope: an absent or unresolvable id yields
 * {@code null}, and the display accessors render a dash rather than "null".</p>
 */
class SystemsTargetDataTest {
    private static final LocalDate DATE = LocalDate.of(3051, 1, 1);

    @AfterEach
    void tearDown() {
        Systems.setInstance(null);
    }

    /** Installs a universe in which only {@code systemId} exists, holding only {@code planetId}. */
    private static PlanetarySystem installUniverse(String systemId, String systemName, String planetId,
          String planetName) {
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getName(any())).thenReturn(systemName);

        if (planetId != null) {
            Planet planet = mock(Planet.class);
            when(planet.getName(any())).thenReturn(planetName);
            when(system.getPlanetById(planetId)).thenReturn(planet);
        }

        Systems systems = mock(Systems.class);
        when(systems.getSystemById(systemId)).thenReturn(system);
        Systems.setInstance(systems);
        return system;
    }

    @Test
    void aFullyResolvedTargetReportsItsSystemAndPlanet() {
        PlanetarySystem system = installUniverse("Galatea", "Galatea", "Galatea 3", "Galatea III");
        SystemsTargetData target = new SystemsTargetData("Galatea", "Galatea 3");

        assertSame(system, target.getSystem());
        assertEquals("Galatea", target.getSystemName(DATE));
        assertEquals("Galatea III", target.getPlanetName(DATE));
    }

    @Test
    void aContractNamingNoWorldResolvesItsSystemButNoPlanet() {
        installUniverse("Galatea", "Galatea", null, null);
        SystemsTargetData target = new SystemsTargetData("Galatea", null);

        assertEquals("Galatea", target.getSystemName(DATE));
        assertNull(target.getPlanet(), "a contract may target a system without naming a world inside it");
        assertEquals("-", target.getPlanetName(DATE));
    }

    @Test
    void aContractNamingNoSystemResolvesNothing() {
        installUniverse("Galatea", "Galatea", "Galatea 3", "Galatea III");
        SystemsTargetData target = new SystemsTargetData(null, null);

        assertNull(target.getSystem(), "a converted legacy contract may name no system at all");
        assertEquals("-", target.getSystemName(DATE));
        assertNull(target.getPlanet());
        assertEquals("-", target.getPlanetName(DATE));
    }

    @Test
    void aSystemIdTheUniverseDoesNotContainResolvesToNothing() {
        installUniverse("Galatea", "Galatea", null, null);
        SystemsTargetData target = new SystemsTargetData("NoSuchSystem", "NoSuchPlanet");

        assertNull(target.getSystem());
        assertEquals("-", target.getSystemName(DATE));
        assertNull(target.getPlanet(), "an unresolvable system cannot yield a planet either");
        assertEquals("-", target.getPlanetName(DATE));
    }

    @Test
    void aPlanetIdTheSystemDoesNotContainResolvesToNothing() {
        installUniverse("Galatea", "Galatea", "Galatea 3", "Galatea III");
        SystemsTargetData target = new SystemsTargetData("Galatea", "NoSuchPlanet");

        assertEquals("Galatea", target.getSystemName(DATE), "the system still resolves");
        assertNull(target.getPlanet());
        assertEquals("-", target.getPlanetName(DATE));
    }
}
