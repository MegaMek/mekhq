/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import megamek.common.universe.FactionTag;
import megamek.common.universe.Factions2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.DOMException;

public class FactionsIntegrationTest {
    private static Factions2 testFactions2;

    @BeforeAll
    public static void setUp() {
        testFactions2 = new Factions2("testresources/data/universe/factions");
        // The instance has to be published, otherwise these tests run against whichever faction data a
        // previously executed test class happened to load into the singleton.
        Factions2.setInstance(testFactions2);
        Factions.setInstance(Factions.loadDefault(true));
    }

    /**
     * Clears the faction singletons this class published, so that a later test class in the same JVM does not silently
     * inherit the small test faction set. Clearing rather than restoring a captured instance is deliberate: there is no
     * way to read the singletons without creating them, so capturing in {@link #setUp()} would force the full
     * production faction data to load just to have something to put back. A null instance simply means "not yet
     * loaded", which is the state the next caller expects to handle.
     */
    @AfterAll
    public static void tearDown() {
        Factions.setInstance(null);
        Factions2.setInstance(null);
        testFactions2 = null;
    }

    @Test
    public void loadDefaultTest() throws DOMException {
        Factions factions = Factions.loadDefault(true);

        assertNotNull(factions);

        List<Faction> choosableFactions = factions.getChoosableFactions();
        assertNotNull(choosableFactions);
        assertTrue(choosableFactions.contains(factions.getFaction("MERC")));
        assertTrue(choosableFactions.contains(factions.getFaction("FS")));

        for (final Faction faction : choosableFactions) {
            assertNotNull(faction,
                  String.format("Missing faction %s in choosable faction list", faction.getShortName()));
        }

        Faction capellans = factions.getFaction("CC");
        assertNotNull(capellans);
        assertFalse(capellans.isClan());
        assertEquals("Sian", capellans.getStartingPlanet(LocalDate.of(3025, 1, 1)));
        assertTrue(capellans.is(FactionTag.IS));
        assertTrue(capellans.is(FactionTag.MAJOR));

        Faction comStar = factions.getFaction("CS");
        assertNotNull(comStar);
        assertTrue(comStar.isComStar());
        assertEquals("Terra", comStar.getStartingPlanet(LocalDate.of(3025, 1, 1)));
        assertEquals("Tukayyid", comStar.getStartingPlanet(LocalDate.of(3067, 1, 1)));
        assertTrue(comStar.is(FactionTag.IS));
        assertTrue(comStar.is(FactionTag.INACTIVE));
        assertTrue(comStar.is(FactionTag.MAJOR));

        Faction ghostBear = factions.getFaction("CGB");
        assertNotNull(ghostBear);
        assertTrue(ghostBear.isClan());
        assertEquals("Arcadia (Clan)", ghostBear.getStartingPlanet(LocalDate.of(3025, 1, 1)));
        assertEquals("Alshain", ghostBear.getStartingPlanet(LocalDate.of(3067, 1, 1)));
        assertTrue(ghostBear.is(FactionTag.CLAN));
        assertTrue(ghostBear.is(FactionTag.MAJOR));
    }

    /**
     * A faction consolidation retires a faction code and keeps it on the surviving faction as an alias, so universe
     * data and campaign saves still referring to the retired code must keep resolving. Regression coverage for the
     * live case where {@code CEI} (retired into {@code CGS}) resolved to the placeholder faction instead, which made
     * the faction diplomacy loader drop every Escorpion Imperio containment entry.
     */
    @Test
    public void getFactionResolvesRetiredCodeThroughAlias() {
        Factions factions = Factions.getInstance();

        Faction byCurrentKey = factions.getFaction("CC");
        Faction byRetiredAlias = factions.getFaction("CAPCON");

        // Pin down the current key first. Comparing the two lookups alone would also pass if the test faction data
        // failed to load and both calls returned the placeholder faction.
        assertEquals("CC", byCurrentKey.getShortName(), "Test faction data did not load");
        assertNotEquals(Faction.DEFAULT_CODE, byRetiredAlias.getShortName(),
              "A retired faction code must not fall back to the placeholder faction");
        assertSame(byCurrentKey, byRetiredAlias,
              "A retired faction code kept as an alias must resolve to the surviving faction entry itself");
    }

    /**
     * The alias fallback must not turn genuinely unknown codes into real factions - they still have to come back as
     * the placeholder faction so callers can detect and report bad data.
     */
    @Test
    public void getFactionReturnsPlaceholderForUnknownCode() {
        Factions factions = Factions.getInstance();

        Faction unknown = factions.getFaction("NOT_A_REAL_FACTION_CODE");

        assertEquals(Faction.DEFAULT_CODE, unknown.getShortName(),
              "An unrecognised faction code must resolve to the placeholder faction");
    }
}
