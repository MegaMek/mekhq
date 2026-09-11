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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link PlanetarySystem#getPopulationFactions(LocalDate)} &mdash; the tenure-weighted set of factions a native
 * of a system could have been born under, including the resolution of the {@code DIS} ("Disputed") marker into the two
 * sides fighting over a contested world.
 */
class PlanetarySystemPopulationFactionsTest {

    /** Must match {@code PlanetarySystem.POPULATION_WINDOW_YEARS}. */
    private static final int WINDOW_YEARS = 40;

    private static final LocalDate WHEN = LocalDate.of(3000, 1, 1);

    @AfterEach
    void tearDown() {
        Factions.setInstance(null);
    }

    @Test
    @DisplayName("An owner holding the world across the whole window weighs the full window length")
    void continuousOwnerFillsWindow() {
        Faction federatedSuns = registerFactions("FS").get("FS");
        // Acquired long before the window opened; only the in-window portion (40 years) is credited.
        PlanetarySystem system = systemWith(planet(factionEvent(LocalDate.of(2800, 1, 1), "FS")));

        assertEquals(Map.of(federatedSuns, WINDOW_YEARS), system.getPopulationFactions(WHEN));
    }

    @Test
    @DisplayName("Two owners inside the window are each weighted by how long they held the world")
    void ownersWeightedByTenure() {
        Map<String, Faction> factions = registerFactions("FS", "DC");
        // FS holds the window's first 25 years (2960-2985); DC the last 15 (2985-3000).
        PlanetarySystem system = systemWith(planet(
              factionEvent(LocalDate.of(2900, 1, 1), "FS"),
              factionEvent(LocalDate.of(2985, 1, 1), "DC")));

        assertEquals(Map.of(factions.get("FS"), 25, factions.get("DC"), 15),
              system.getPopulationFactions(WHEN));
    }

    @Test
    @DisplayName("An owner that left before the window opened is not part of the current population")
    void ownerDepartedBeforeWindowExcluded() {
        Map<String, Faction> factions = registerFactions("FS", "DC");
        // FS ruled 2900-2950, entirely before the 2960 window start; DC has held it since.
        PlanetarySystem system = systemWith(planet(
              factionEvent(LocalDate.of(2900, 1, 1), "FS"),
              factionEvent(LocalDate.of(2950, 1, 1), "DC")));

        assertEquals(Map.of(factions.get("DC"), WINDOW_YEARS), system.getPopulationFactions(WHEN));
    }

    @Test
    @DisplayName("A contested world splits the dispute's span between the pre-dispute and eventual owner")
    void disputedWorldSplitsBetweenContestants() {
        Map<String, Faction> factions = registerFactions("FS", "DC", "DIS");
        // FS holds 2960-2980, the world is disputed 2980-3010, and DC takes it in 3010 (after the evaluation date).
        // In-window: FS reigns 20 years, then a 20-year dispute is split 10/10 between FS (before) and DC (eventual).
        PlanetarySystem system = systemWith(planet(
              factionEvent(LocalDate.of(2900, 1, 1), "FS"),
              factionEvent(LocalDate.of(2980, 1, 1), "DIS"),
              factionEvent(LocalDate.of(3010, 1, 1), "DC")));

        Map<Faction, Integer> result = system.getPopulationFactions(WHEN);

        assertEquals(Map.of(factions.get("FS"), 30, factions.get("DC"), 10), result);
        assertFalse(result.containsKey(factions.get("DIS")),
              "the Disputed pseudo-faction must never be returned as a birth origin");
    }

    @Test
    @DisplayName("Co-owners of a world each receive the full span rather than a split")
    void coOwnersEachGetFullSpan() {
        Map<String, Faction> factions = registerFactions("FS", "LA");
        // A jointly held world (unlike a dispute) credits both owners the full tenure.
        PlanetarySystem system = systemWith(planet(factionEvent(LocalDate.of(2900, 1, 1), "FS", "LA")));

        assertEquals(Map.of(factions.get("FS"), WINDOW_YEARS, factions.get("LA"), WINDOW_YEARS),
              system.getPopulationFactions(WHEN));
    }

    @Test
    @DisplayName("A faction that ruled only briefly still weighs at least one")
    void briefTenureFlooredToOne() {
        Map<String, Faction> factions = registerFactions("FS", "DC");
        // DC seized the world barely two months before the evaluation date — a fraction of a year, floored to 1.
        PlanetarySystem system = systemWith(planet(
              factionEvent(LocalDate.of(2900, 1, 1), "FS"),
              factionEvent(LocalDate.of(2999, 11, 1), "DC")));

        assertEquals(Map.of(factions.get("FS"), WINDOW_YEARS, factions.get("DC"), 1),
              system.getPopulationFactions(WHEN));
    }

    @Test
    @DisplayName("Abandoned (ABN) is dropped when a real owner is also present")
    void abandonedDroppedAlongsideRealOwner() {
        Map<String, Faction> factions = registerFactions("FS", "ABN");
        // One inhabited world (FS) and one abandoned world in the same system.
        PlanetarySystem system = systemWith(
              planet(factionEvent(LocalDate.of(2900, 1, 1), "FS")),
              planet(factionEvent(LocalDate.of(2900, 1, 1), "ABN")));

        assertEquals(Map.of(factions.get("FS"), WINDOW_YEARS), system.getPopulationFactions(WHEN));
    }

    @Test
    @DisplayName("Tenure accumulates across a faction's worlds in the system")
    void tenureSumsAcrossPlanets() {
        Faction federatedSuns = registerFactions("FS").get("FS");
        // FS has held two worlds in this system for the whole window; person-years accumulate across both.
        PlanetarySystem system = systemWith(
              planet(factionEvent(LocalDate.of(2900, 1, 1), "FS")),
              planet(factionEvent(LocalDate.of(2900, 1, 1), "FS")));

        assertEquals(Map.of(federatedSuns, WINDOW_YEARS * 2), system.getPopulationFactions(WHEN));
    }

    @Test
    @DisplayName("An uninhabited world with no ownership history yields no population factions")
    void planetWithoutEventsYieldsEmpty() {
        registerFactions("FS");
        PlanetarySystem system = systemWith(new Planet("Barren"));

        assertTrue(system.getPopulationFactions(WHEN).isEmpty());
    }

    // --- helpers ---------------------------------------------------------------------------------------------------

    private static Map<String, Faction> registerFactions(String... codes) {
        java.util.Map<String, Faction> byCode = new java.util.HashMap<>();
        for (String code : codes) {
            Faction faction = mock(Faction.class);
            when(faction.getShortName()).thenReturn(code);
            byCode.put(code, faction);
        }
        Factions registry = mock(Factions.class);
        when(registry.getFaction(anyString())).thenAnswer(invocation -> byCode.get(invocation.getArgument(0)));
        Factions.setInstance(registry);
        return byCode;
    }

    private static Planet.PlanetaryEvent factionEvent(LocalDate date, String... factionCodes) {
        Planet.PlanetaryEvent event = new Planet.PlanetaryEvent();
        event.date = date;
        event.faction = SourceableValue.of(List.of(factionCodes));
        return event;
    }

    private static Planet planet(Planet.PlanetaryEvent... events) {
        Planet planet = new Planet("Test World");
        for (Planet.PlanetaryEvent event : events) {
            planet.putEvent(event);
        }
        return planet;
    }

    private static PlanetarySystem systemWith(Planet... planets) {
        PlanetarySystem system = new PlanetarySystem("Test System");
        TreeMap<Integer, Planet> planetMap = new TreeMap<>();
        int position = 1;
        for (Planet planet : planets) {
            planetMap.put(position++, planet);
        }
        try {
            Field field = PlanetarySystem.class.getDeclaredField("planets");
            field.setAccessible(true);
            field.set(system, planetMap);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not inject planets into the test system", e);
        }
        return system;
    }
}
