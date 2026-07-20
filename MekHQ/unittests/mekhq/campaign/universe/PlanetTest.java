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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.campaign.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link Planet}'s event-sourced accessors, in particular the internal {@code CurrentEvents} cache's handling
 * of out-of-order date queries.
 */
public class PlanetTest {

    @AfterEach
    public void tearDown() {
        Factions.setInstance(null);
    }

    private static Faction createTestFaction(String shortName) {
        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn(shortName);
        return faction;
    }

    private static void mockFactionsRegistry(Faction... factions) {
        Factions mockedFactions = mock(Factions.class);
        when(mockedFactions.getFaction(anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            for (Faction faction : factions) {
                if (faction.getShortName().equals(code)) {
                    return faction;
                }
            }
            return null;
        });
        Factions.setInstance(mockedFactions);
    }

    private static Planet.PlanetaryEvent createFactionEvent(LocalDate date, String factionCode) {
        Planet.PlanetaryEvent event = new Planet.PlanetaryEvent();
        event.date = date;
        event.faction = SourceableValue.of(List.of(factionCode));
        return event;
    }

    /**
     * Regression test: {@code Planet.CurrentEvents} merges every event up to a queried date into a single shared,
     * mutable accumulator and never un-merges them. A query for a date <i>after</i> the campaign's actual current
     * date (e.g. looking up a contract's target system by its calculated start date, rather than the campaign's
     * current date) used to fast-forward that accumulator permanently, because the cache only reset when queried
     * with a date before the very first query ever made against it &mdash; not before the most recently resolved
     * one. A subsequent query for an earlier (or the original) date would then silently return the already-future
     * state instead of re-resolving for the date actually asked about.
     */
    @Test
    public void testGetFactionSetIsNotCorruptedByOutOfOrderQueries() {
        Faction earlyOwner = createTestFaction("AAA");
        Faction lateOwner = createTestFaction("BBB");
        mockFactionsRegistry(earlyOwner, lateOwner);

        LocalDate earlyDate = LocalDate.of(3049, 12, 1);
        LocalDate lateDate = LocalDate.of(3050, 4, 1);

        Planet planet = new Planet("Test System");
        planet.putEvent(createFactionEvent(earlyDate, "AAA"));
        planet.putEvent(createFactionEvent(lateDate, "BBB"));

        assertEquals(Set.of(earlyOwner), planet.getFactionSet(earlyDate),
              "Sanity check: the early date should resolve to the early owner");

        assertEquals(Set.of(lateOwner), planet.getFactionSet(lateDate),
              "Sanity check: a later query should fast-forward to the later owner");

        assertEquals(Set.of(earlyOwner), planet.getFactionSet(earlyDate),
              "A query for the early date again, after an intervening later-dated query, must still resolve to the "
                    + "early owner rather than the future state the prior query fast-forwarded past");
    }
}
