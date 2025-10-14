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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import megamek.common.universe.FactionTag;
import megamek.common.universe.Factions2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.DOMException;

public class FactionsIntegrationTest {
    private static Factions2 testFactions2;

    @BeforeAll
    public static void setUp() {
        testFactions2 = new Factions2("testresources/data/universe/factions");
        Factions.setInstance(Factions.loadDefault(true));
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
}
