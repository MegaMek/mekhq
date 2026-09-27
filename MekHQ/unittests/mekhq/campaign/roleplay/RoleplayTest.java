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
package mekhq.campaign.roleplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

class RoleplayTest {
    @Test
    void defaultsToChaosFive() {
        assertEquals(5, new Roleplay().getChaosFactor());
    }

    @Test
    void chaosFactorStaysWithinRange() {
        Roleplay roleplay = new Roleplay();
        for (int i = 0; i < 20; i++) {
            roleplay.increaseChaosFactor();
        }
        assertEquals(FateChart.MAXIMUM_CHAOS_FACTOR, roleplay.getChaosFactor());

        for (int i = 0; i < 20; i++) {
            roleplay.decreaseChaosFactor();
        }
        assertEquals(FateChart.MINIMUM_CHAOS_FACTOR, roleplay.getChaosFactor());

        roleplay.setChaosFactor(42);
        assertEquals(FateChart.MAXIMUM_CHAOS_FACTOR, roleplay.getChaosFactor());
    }

    @Test
    void chaosFactorSurvivesSaveAndLoad() throws Exception {
        Roleplay original = new Roleplay();
        original.setChaosFactor(8);

        StringWriter stringWriter = new StringWriter();
        try (PrintWriter writer = new PrintWriter(stringWriter)) {
            original.writeToXML(writer, 0);
        }

        Roleplay loaded = Roleplay.generateInstanceFromXML(parse(stringWriter.toString()));
        assertEquals(8, loaded.getChaosFactor());
    }

    @Test
    void malformedChaosFactorFallsBackToDefault() throws Exception {
        Roleplay loaded = Roleplay.generateInstanceFromXML(
              parse("<roleplay><chaosFactor>banana</chaosFactor></roleplay>"));
        assertEquals(Roleplay.DEFAULT_CHAOS_FACTOR, loaded.getChaosFactor());
    }

    @Test
    void addCharacterIgnoresBlanksAndDuplicates() {
        Roleplay roleplay = new Roleplay();
        assertTrue(roleplay.addCharacter("  Natasha Kerensky "));
        assertFalse(roleplay.addCharacter("Natasha Kerensky"));
        assertFalse(roleplay.addCharacter("   "));
        assertFalse(roleplay.addCharacter(null));
        assertEquals(List.of("Natasha Kerensky"), roleplay.getCharacters());
    }

    @Test
    void pickRandomCharacterDrawsFromTheList() {
        Roleplay roleplay = new Roleplay();
        assertNull(roleplay.pickRandomCharacter());

        roleplay.addCharacter("Grayson Carlyle");
        roleplay.addCharacter("Lori Kalmar");
        for (int i = 0; i < 100; i++) {
            assertTrue(roleplay.getCharacters().contains(roleplay.pickRandomCharacter()));
        }
    }

    @Test
    void charactersSurviveSaveAndLoadInOrder() throws Exception {
        Roleplay original = new Roleplay();
        original.addCharacter("Zeta <Ace> & Co, Ltd");
        original.addCharacter("Alpha");

        StringWriter stringWriter = new StringWriter();
        try (PrintWriter writer = new PrintWriter(stringWriter)) {
            original.writeToXML(writer, 0);
        }

        Roleplay loaded = Roleplay.generateInstanceFromXML(parse(stringWriter.toString()));
        assertEquals(List.of("Zeta <Ace> & Co, Ltd", "Alpha"), loaded.getCharacters());
    }

    @Test
    void onlyNpcFocusesInvolveNpcs() {
        for (RandomEventFocus focus : RandomEventFocus.values()) {
            boolean expected = focus == RandomEventFocus.NPC_ACTION
                                     || focus == RandomEventFocus.NPC_NEGATIVE
                                     || focus == RandomEventFocus.NPC_POSITIVE;
            assertEquals(expected, focus.involvesNPC(), focus.name());
        }
    }

    @Test
    void journalAndOracleLogSurviveSaveAndLoad() throws Exception {
        Roleplay original = new Roleplay();
        original.getJournal().add(new JournalEntry(java.time.LocalDate.of(3025, 1, 2),
              "Landed on Helm.\nThe <locals> & their militia are wary."));
        original.getJournal().add(new JournalEntry(java.time.LocalDate.of(3025, 1, 5), ""));
        original.logOracle(java.time.LocalDate.of(3025, 1, 3), "Fate Chart: Likely at chaos 5. Rolled 12: Yes.", 500);

        StringWriter stringWriter = new StringWriter();
        try (PrintWriter writer = new PrintWriter(stringWriter)) {
            original.writeToXML(writer, 0);
        }
        Roleplay loaded = Roleplay.generateInstanceFromXML(parse(stringWriter.toString()));

        assertEquals(2, loaded.getJournal().size());
        assertEquals(java.time.LocalDate.of(3025, 1, 2), loaded.getJournal().get(0).getDate());
        assertEquals("Landed on Helm.\nThe <locals> & their militia are wary.", loaded.getJournal().get(0).getText());
        assertEquals("", loaded.getJournal().get(1).getText());
        assertEquals(1, loaded.getOracleLog().size());
        assertEquals("Fate Chart: Likely at chaos 5. Rolled 12: Yes.", loaded.getOracleLog().get(0).getText());
    }

    @Test
    void oracleLogDropsOldestEntriesPastTheMaximum() {
        Roleplay roleplay = new Roleplay();
        java.time.LocalDate date = java.time.LocalDate.of(3025, 1, 1);
        for (int i = 1; i <= 5; i++) {
            roleplay.logOracle(date, "result " + i, 3);
        }
        assertEquals(List.of("result 3", "result 4", "result 5"),
              roleplay.getOracleLog().stream().map(JournalEntry::getText).toList());

        // Lowering the maximum trims the backlog on the next log.
        roleplay.logOracle(date, "result 6", 1);
        assertEquals(List.of("result 6"), roleplay.getOracleLog().stream().map(JournalEntry::getText).toList());

        // Nonsensical maximums still keep the newest result.
        roleplay.logOracle(date, "result 7", 0);
        assertEquals(List.of("result 7"), roleplay.getOracleLog().stream().map(JournalEntry::getText).toList());
    }

    private static Node parse(final String xml) throws Exception {
        return DocumentBuilderFactory.newInstance()
                     .newDocumentBuilder()
                     .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                     .getDocumentElement();
    }
}
