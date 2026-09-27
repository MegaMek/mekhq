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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import mekhq.campaign.roleplay.JournalArchive.Archive;
import mekhq.campaign.roleplay.JournalArchive.ImportResult;
import mekhq.campaign.roleplay.JournalExporter.Format;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JournalArchiveTest {
    private static final LocalDate DAY_ONE = LocalDate.of(3025, 1, 1);
    private static final LocalDate DAY_TWO = LocalDate.of(3025, 1, 2);

    private static Roleplay campaignJournal() {
        Roleplay roleplay = new Roleplay();
        OracleCharacter ana = roleplay.addCharacter("Ana");
        PlotThread hunt = PlotThread.create("Hunt", PlotThreadLength.SHORT,
              RandomOracleGenerator.createForTesting(new EnumMap<>(OracleTable.class)));
        roleplay.getPlotThreads().add(hunt);
        JournalEntry asked = new JournalEntry(DAY_ONE, JournalEntryType.FATE_CHART,
              "Asked: Is it safe?\nRolled 12: Yes.");
        asked.setAnswer(new OracleAnswer("Is it safe?", FateChartOdds.values()[0], 5, 12,
              FateChartAnswer.values()[0]));
        roleplay.getOracleLog().add(asked.tagCharacter(ana));
        JournalEntry note = new JournalEntry(DAY_TWO, JournalEntryType.NOTE,
              "<html><body><p>We move <b>tonight</b> &amp; <i>quietly</i>.</p><ul><li>Ana</li></ul></body></html>");
        roleplay.getJournal().add(note.tagThread(hunt).tagCharacter(ana));
        return roleplay;
    }

    private static String export(final Roleplay roleplay) {
        return JournalExporter.export(roleplay.getTimeline(JournalFilter.ALL), Format.MARKDOWN, "Journal", null,
              roleplay::getPlotThreadName, roleplay::getCharacterName, LocalDate::toString);
    }

    @Test
    void markdownExportsReadBackExactly() {
        Roleplay original = campaignJournal();
        String document = export(original);
        // The data is hidden in a comment, after the readable text.
        assertTrue(document.contains(JournalArchive.START), document);
        assertTrue(document.indexOf("We move **tonight**") < document.indexOf(JournalArchive.START));

        Archive archive = JournalArchive.read(document, LocalDate::parse);
        assertTrue(archive.exact());
        assertEquals(2, archive.entries().size());
        JournalEntry asked = archive.entries().get(0);
        assertEquals(original.getOracleLog().get(0).getText(), asked.getText());
        assertEquals(original.getOracleLog().get(0).getAnswer(), asked.getAnswer());
        assertEquals(original.getJournal().get(0).getText(), archive.entries().get(1).getText());
        assertEquals("Hunt", archive.threads().values().iterator().next());
        assertEquals("Ana", archive.characters().values().iterator().next());
    }

    @Test
    void importingIntoTheSameCampaignSkipsWhatIsAlreadyThere() {
        Roleplay roleplay = campaignJournal();
        ImportResult result = JournalArchive.importInto(roleplay, JournalArchive.read(export(roleplay),
              LocalDate::parse), 500);
        assertEquals(new ImportResult(0, 0, 2, 0), result);
        assertEquals(1, roleplay.getJournal().size());
    }

    @Test
    void importingIntoAnotherCampaignMatchesTagsByName() {
        Archive archive = JournalArchive.read(export(campaignJournal()), LocalDate::parse);
        Roleplay other = new Roleplay();
        OracleCharacter otherAna = other.addCharacter("ana");
        PlotThread otherHunt = PlotThread.create("HUNT", PlotThreadLength.SHORT,
              RandomOracleGenerator.createForTesting(new EnumMap<>(OracleTable.class)));
        other.getPlotThreads().add(otherHunt);

        ImportResult result = JournalArchive.importInto(other, archive, 500);

        assertEquals(new ImportResult(1, 1, 0, 0), result);
        JournalEntry note = other.getJournal().get(0);
        assertEquals(List.of(otherHunt.getId()), List.copyOf(note.getThreads()));
        assertEquals(List.of(otherAna.getId()), List.copyOf(note.getCharacters()));
        assertEquals(1, other.getCharacters().size());
    }

    @Test
    void unknownCastAreKeptAsRemovedCharactersAndUnknownThreadsDropped() {
        Roleplay empty = new Roleplay();
        JournalArchive.importInto(empty, JournalArchive.read(export(campaignJournal()), LocalDate::parse), 500);

        assertEquals(1, empty.getCharacters().size());
        assertEquals("Ana", empty.getCharacters().get(0).getName());
        assertFalse(empty.getCharacters().get(0).isActive());
        assertTrue(empty.getJournal().get(0).getThreads().isEmpty());
    }

    @Test
    void importsBeyondTheLogLimitDropTheOldestRecords() {
        Roleplay source = new Roleplay();
        for (int day = 0; day < 5; day++) {
            source.getOracleLog().add(new JournalEntry(DAY_ONE.plusDays(day), JournalEntryType.DICE, "roll " + day));
        }
        Roleplay target = new Roleplay();
        ImportResult result = JournalArchive.importInto(target, JournalArchive.read(export(source),
              LocalDate::parse), 3);
        assertEquals(2, result.trimmed());
        assertEquals("roll 2", target.getOracleLog().get(0).getText());
    }

    @Test
    void oldExportsAreReadFromTheirText() {
        String document = export(campaignJournal());
        String old = document.substring(0, document.indexOf(JournalArchive.START)) + "\n";

        Archive archive = JournalArchive.read(old, LocalDate::parse);

        assertFalse(archive.exact());
        assertEquals(2, archive.entries().size());
        JournalEntry asked = archive.entries().get(0);
        assertEquals(JournalEntryType.FATE_CHART, asked.getType());
        assertEquals("Asked: Is it safe?\nRolled 12: Yes.", asked.getText());
        JournalEntry note = archive.entries().get(1);
        assertEquals(JournalEntryType.NOTE, note.getType());
        assertEquals(DAY_TWO, note.getDate());
        assertTrue(note.getText().contains("<b>tonight</b>"), note.getText());
        assertTrue(note.getText().contains("<i>quietly</i>"), note.getText());
        assertTrue(note.getText().contains("<li>Ana</li>"), note.getText());
        assertEquals(1, note.getThreads().size());
        assertEquals(1, note.getCharacters().size());
        assertTrue(archive.characters().containsValue("Ana"));

        // Tags still match by name on import.
        Roleplay other = new Roleplay();
        OracleCharacter ana = other.addCharacter("Ana");
        JournalArchive.importInto(other, archive, 500);
        assertTrue(other.getJournal().get(0).getCharacters().contains(ana.getId()));
    }

    @Test
    void anItalicFirstLineIsNotMistakenForTags() {
        String document = "# Journal\n\n## 3025-01-01 - Note\n\n*It rained all night.*\n\nWe waited.\n";
        Archive archive = JournalArchive.read(document, LocalDate::parse);
        assertEquals(1, archive.entries().size());
        assertTrue(archive.entries().get(0).getText().contains("<i>It rained all night.</i>"));
    }

    @Test
    void aDamagedDataBlockFallsBackToTheText() {
        String document = export(campaignJournal());
        int start = document.indexOf(JournalArchive.START) + JournalArchive.START.length() + 1;
        String damaged = document.substring(0, start) + "!!!" + document.substring(start + 3);
        Archive archive = JournalArchive.read(damaged, LocalDate::parse);
        assertFalse(archive.exact());
        assertEquals(2, archive.entries().size());
    }

    @Test
    void aFileWithNoEntriesImportsNothing() {
        assertTrue(JournalArchive.read("# Shopping list\n\n- Milk\n", LocalDate::parse).entries().isEmpty());
        assertNull(JournalArchive.decode("no data here"));
    }

    // region Rescue

    private static String saveWith(final Roleplay roleplay) {
        StringWriter text = new StringWriter();
        try (PrintWriter writer = new PrintWriter(text)) {
            roleplay.writeToXML(writer, 1);
        }
        // Everything around the journal is broken beyond reading.
        return "<?xml version=\"1.0\"?>\n<campaign>\n<units><unit id=\"x\"><broken></unit>\n" + text
                     + "<personnel></campaign";
    }

    @Test
    void theJournalIsReadFromASaveThatWontLoad() {
        Roleplay rescued = JournalRescue.read(saveWith(campaignJournal()));
        assertNotNull(rescued);
        assertEquals(1, rescued.getJournal().size());
        assertEquals(1, rescued.getOracleLog().size());
        assertEquals("Ana", rescued.getCharacters().get(0).getName());
    }

    @Test
    void aDamagedEntryCostsOnlyItself() {
        Roleplay roleplay = campaignJournal();
        roleplay.getOracleLog().add(new JournalEntry(DAY_TWO, JournalEntryType.DICE, "MARKER"));
        String save = saveWith(roleplay).replace("MARKER", "<broken>");
        Roleplay rescued = JournalRescue.read(save);
        assertNotNull(rescued);
        assertEquals(1, rescued.getJournal().size());
        assertEquals(1, rescued.getOracleLog().size());
    }

    @Test
    void rescueWritesAnImportableFileNextToACompressedSave(@TempDir final Path folder) throws Exception {
        File save = folder.resolve("Wolf's Dragoons.cpnx.gz").toFile();
        try (OutputStream out = new GZIPOutputStream(new FileOutputStream(save))) {
            out.write(saveWith(campaignJournal()).getBytes(StandardCharsets.UTF_8));
        }
        File first = JournalRescue.rescue(save, LocalDate::toString);
        File second = JournalRescue.rescue(save, LocalDate::toString);

        assertNotNull(first);
        assertEquals("Wolf's Dragoons - rescued journal.md", first.getName());
        assertEquals("Wolf's Dragoons - rescued journal 2.md", second.getName());
        Archive archive = JournalArchive.read(Files.readString(first.toPath()), LocalDate::parse);
        assertTrue(archive.exact());
        assertEquals(2, archive.entries().size());
    }

    @Test
    void aSaveWithoutAJournalRescuesNothing(@TempDir final Path folder) throws Exception {
        File save = folder.resolve("empty.cpnx").toFile();
        Files.writeString(save.toPath(), "<campaign><roleplay><chaosFactor>5</chaosFactor></roleplay></campaign>");
        assertNull(JournalRescue.rescue(save, LocalDate::toString));
        assertNull(JournalRescue.rescue(folder.resolve("missing.cpnx").toFile(), LocalDate::toString));
    }

    // endregion Rescue
}
