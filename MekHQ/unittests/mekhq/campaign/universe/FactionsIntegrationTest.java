/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

import mekhq.campaign.universe.Faction.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FactionsIntegrationTest {
    @Test
    public void loadDefaultTest()
            throws DOMException, SAXException, IOException, ParserConfigurationException {
        Factions factions = Factions.loadDefault();

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
        assertTrue(capellans.is(Tag.IS));
        assertTrue(capellans.is(Tag.MAJOR));

        Faction comStar = factions.getFaction("CS");
        assertNotNull(comStar);
        assertTrue(comStar.isComStar());
        assertEquals("Terra", comStar.getStartingPlanet(LocalDate.of(3025, 1, 1)));
        assertEquals("Tukayyid", comStar.getStartingPlanet(LocalDate.of(3067, 1, 1)));
        assertTrue(comStar.is(Tag.IS));
        assertTrue(comStar.is(Tag.INACTIVE));
        assertTrue(comStar.is(Tag.MAJOR));

        Faction ghostBear = factions.getFaction("CGB");
        assertNotNull(ghostBear);
        assertTrue(ghostBear.isClan());
        assertEquals("Arcadia (Clan)", ghostBear.getStartingPlanet(LocalDate.of(3025, 1, 1)));
        assertEquals("Alshain", ghostBear.getStartingPlanet(LocalDate.of(3067, 1, 1)));
        assertTrue(ghostBear.is(Tag.CLAN));
        assertTrue(ghostBear.is(Tag.MAJOR));
    }
}
