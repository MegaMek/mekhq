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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
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

    private static List<String> names(final List<OracleCharacter> characters) {
        return characters.stream().map(OracleCharacter::getName).toList();
    }

    @Test
    void addCharacterIgnoresBlanksAndDuplicates() {
        Roleplay roleplay = new Roleplay();
        assertNotNull(roleplay.addCharacter("  Natasha Kerensky "));
        assertNull(roleplay.addCharacter("Natasha Kerensky"));
        assertNull(roleplay.addCharacter("   "));
        assertNull(roleplay.addCharacter(null));
        assertEquals(List.of("Natasha Kerensky"), names(roleplay.getCharacters()));
    }

    @Test
    void pickRandomCharacterDrawsOnlyFromTheActiveCast() {
        Roleplay roleplay = new Roleplay();
        assertNull(roleplay.pickRandomCharacter());

        OracleCharacter grayson = roleplay.addCharacter("Grayson Carlyle");
        OracleCharacter lori = roleplay.addCharacter("Lori Kalmar");
        roleplay.removeCharacter(lori);
        for (int i = 0; i < 100; i++) {
            assertEquals(grayson, roleplay.pickRandomCharacter());
        }

        roleplay.removeCharacter(grayson);
        assertNull(roleplay.pickRandomCharacter());
    }

    @Test
    void removedCharactersKeepTheirPlaceInHistoryAndCanBeRestored() {
        Roleplay roleplay = new Roleplay();
        OracleCharacter ana = roleplay.addCharacter("Ana");
        roleplay.addCharacter("Bo");
        roleplay.removeCharacter(ana);

        assertEquals(List.of("Bo"), names(roleplay.getActiveCharacters()));
        assertEquals(ana, roleplay.getCharacter(ana.getId()), "a removed character can still be looked up");

        // Adding the same name restores the character rather than creating a new one.
        assertEquals(ana, roleplay.addCharacter("Ana"));
        assertTrue(ana.isActive());
        assertEquals(List.of("Bo", "Ana"), names(roleplay.getActiveCharacters()));
    }

    @Test
    void linkedCharactersFollowTheirPerson() {
        Roleplay roleplay = new Roleplay();
        UUID personId = UUID.randomUUID();
        OracleCharacter linked = roleplay.addLinkedCharacter(personId, "Natasha Kerensky");
        assertTrue(linked.isLinked());

        // Adding the same person again returns the same character, restoring it if removed.
        roleplay.removeCharacter(linked);
        assertEquals(linked, roleplay.addLinkedCharacter(personId, "Natasha Kerensky"));
        assertTrue(linked.isActive());

        roleplay.refreshLinkedNames(id -> id.equals(personId) ? "Natasha 'Black Widow' Kerensky" : null);
        assertEquals("Natasha 'Black Widow' Kerensky", linked.getName());

        // A person who has left the company keeps their last known name.
        roleplay.refreshLinkedNames(id -> null);
        assertEquals("Natasha 'Black Widow' Kerensky", linked.getName());
    }

    @Test
    void charactersCanBeRenamedAndReordered() {
        Roleplay roleplay = new Roleplay();
        OracleCharacter ana = roleplay.addCharacter("Ana");
        OracleCharacter bo = roleplay.addCharacter("Bo");
        OracleCharacter cy = roleplay.addCharacter("Cy");

        assertFalse(roleplay.renameCharacter(ana, "Bo"), "cannot take another cast member's name");
        assertFalse(roleplay.renameCharacter(ana, " "));
        assertTrue(roleplay.renameCharacter(ana, " Anastasia "));
        assertEquals("Anastasia", ana.getName());

        roleplay.moveCharacter(cy, 0);
        assertEquals(List.of(cy, ana, bo), roleplay.getActiveCharacters());
        roleplay.moveCharacter(cy, 99);
        assertEquals(List.of(ana, bo, cy), roleplay.getActiveCharacters());
    }

    @Test
    void charactersSurviveSaveAndLoadInOrder() throws Exception {
        Roleplay original = new Roleplay();
        OracleCharacter zeta = original.addCharacter("Zeta <Ace> & Co, Ltd");
        original.addCharacter("Alpha");
        OracleCharacter linked = original.addLinkedCharacter(UUID.randomUUID(), "Linked");
        original.removeCharacter(zeta);

        StringWriter stringWriter = new StringWriter();
        try (PrintWriter writer = new PrintWriter(stringWriter)) {
            original.writeToXML(writer, 0);
        }

        Roleplay loaded = Roleplay.generateInstanceFromXML(parse(stringWriter.toString()));
        assertEquals(names(original.getCharacters()), names(loaded.getCharacters()));
        assertEquals(zeta.getId(), loaded.getCharacters().get(0).getId(), "removed characters keep their place");
        assertFalse(loaded.getCharacter(zeta.getId()).isActive());
        assertEquals(linked.getPersonId(), loaded.getCharacter(linked.getId()).getPersonId());
    }

    @Test
    void charactersFromOlderSavesLoadByName() throws Exception {
        Roleplay loaded = Roleplay.generateInstanceFromXML(parse(
              "<roleplay><characters><character>Ana</character><character>Bo</character></characters>"
                    + "<journal><entry><date>3025-01-02</date><type>NOTE</type><text>x</text>"
                    + "<character>Bo</character><character>Gone</character></entry></journal></roleplay>"));

        assertEquals(List.of("Ana", "Bo"), names(loaded.getActiveCharacters()));
        JournalEntry note = loaded.getJournal().get(0);
        OracleCharacter bo = loaded.getActiveCharacters().get(1);
        assertTrue(note.getCharacters().contains(bo.getId()));

        // A tag naming someone no longer in the cast keeps its name through a removed character.
        assertEquals(2, note.getCharacters().size());
        OracleCharacter gone = loaded.getCharacters().stream().filter(c -> c.getName().equals("Gone")).findFirst()
                                     .orElseThrow();
        assertFalse(gone.isActive());
        assertTrue(note.getCharacters().contains(gone.getId()));
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
    void oracleLogDropsOldestEntriesPastTheMaximum() {
        Roleplay roleplay = new Roleplay();
        java.time.LocalDate date = java.time.LocalDate.of(3025, 1, 1);
        for (int i = 1; i <= 5; i++) {
            roleplay.logOracle(date, JournalEntryType.FATE_CHART, "result " + i, 3);
        }
        assertEquals(List.of("result 3", "result 4", "result 5"),
              roleplay.getOracleLog().stream().map(JournalEntry::getText).toList());

        // Lowering the maximum trims the backlog on the next log.
        roleplay.logOracle(date, JournalEntryType.FATE_CHART, "result 6", 1);
        assertEquals(List.of("result 6"), roleplay.getOracleLog().stream().map(JournalEntry::getText).toList());

        // Nonsensical maximums still keep the newest result.
        roleplay.logOracle(date, JournalEntryType.FATE_CHART, "result 7", 0);
        assertEquals(List.of("result 7"), roleplay.getOracleLog().stream().map(JournalEntry::getText).toList());
    }

    @Test
    void campaignEventsAreNeitherTrimmedNorCounted() {
        Roleplay roleplay = new Roleplay();
        java.time.LocalDate date = java.time.LocalDate.of(3025, 1, 1);
        roleplay.logOracle(date, JournalEntryType.CHRONICLE, "Contract accepted", 2);
        for (int i = 1; i <= 4; i++) {
            roleplay.logOracle(date, JournalEntryType.DICE, "roll " + i, 2);
        }
        assertEquals(List.of("Contract accepted", "roll 3", "roll 4"),
              roleplay.getOracleLog().stream().map(JournalEntry::getText).toList());
    }

    private static Node parse(final String xml) throws Exception {
        return DocumentBuilderFactory.newInstance()
                     .newDocumentBuilder()
                     .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                     .getDocumentElement();
    }

    @Test
    void notesSurviveSaveAndLoad() throws Exception {
        Roleplay original = new Roleplay();
        OracleCharacter kai = original.addCharacter("Kai");
        kai.setNotes("  Characters: Appearance: scarred\nCharacters: Motive: <revenge> & glory  ");
        original.addCharacter("Plain");

        StringWriter text = new StringWriter();
        try (PrintWriter writer = new PrintWriter(text)) {
            original.writeToXML(writer, 0);
        }
        Node node = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                          .parse(new ByteArrayInputStream(text.toString().getBytes(StandardCharsets.UTF_8)))
                          .getDocumentElement();
        Roleplay loaded = Roleplay.generateInstanceFromXML(node);

        assertEquals("Characters: Appearance: scarred\nCharacters: Motive: <revenge> & glory",
              loaded.getCharacters().get(0).getNotes());
        assertEquals("", loaded.getCharacters().get(1).getNotes());
    }
}
