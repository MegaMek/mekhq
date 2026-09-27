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
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.campaign.roleplay.JournalExporter.Format;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

class JournalTest {
    private static final LocalDate DAY_ONE = LocalDate.of(3025, 1, 1);
    private static final LocalDate DAY_TWO = LocalDate.of(3025, 1, 2);
    private static final LocalDate DAY_THREE = LocalDate.of(3025, 1, 3);

    @AfterEach
    void tearDown() {
        RandomOracleGenerator.resetForTesting();
    }

    private static PlotThread thread(final String name) {
        Map<OracleTable, WeightedIntMap<String>> pools = new EnumMap<>(OracleTable.class);
        for (OracleTable table : OracleTable.values()) {
            WeightedIntMap<String> pool = new WeightedIntMap<>();
            pool.add(1, "x");
            pools.put(table, pool);
        }
        return PlotThread.create(name, PlotThreadLength.SHORT, RandomOracleGenerator.createForTesting(pools));
    }

    private static JournalEntry note(final LocalDate date, final String html) {
        return new JournalEntry(date, JournalEntryType.NOTE, "<html><body>" + html + "</body></html>");
    }

    /** Round-trips markup through Swing's own HTML writer, as the journal's editor does. */
    private static String throughSwing(final String html) throws Exception {
        HTMLEditorKit kit = new HTMLEditorKit();
        Document document = kit.createDefaultDocument();
        kit.read(new java.io.StringReader(html), document, 0);
        StringWriter out = new StringWriter();
        kit.write(out, document, 0, document.getLength());
        return out.toString();
    }

    // region JournalText

    @Test
    void richTextFromSwingConvertsToPlainAndMarkdown() throws Exception {
        String html = throughSwing("<html><body><h3>Landfall</h3><p>We hit <b>Helm</b> at <i>dawn</i> &amp; "
                                         + "<u>held</u> the ridge.</p><ul><li>Two Locusts down</li>"
                                         + "<li>One tech hurt</li></ul></body></html>");

        String plain = JournalText.htmlToPlain(html);
        assertTrue(plain.contains("Landfall"), plain);
        assertTrue(plain.contains("We hit Helm at dawn & held the ridge."), plain);
        assertTrue(plain.contains("- Two Locusts down"), plain);
        assertTrue(plain.contains("- One tech hurt"), plain);
        assertFalse(plain.contains("<"), plain);

        String markdown = JournalText.htmlToMarkdown(html);
        assertTrue(markdown.contains("### Landfall"), markdown);
        assertTrue(markdown.contains("**Helm**"), markdown);
        assertTrue(markdown.contains("*dawn*"), markdown);
        assertTrue(markdown.contains("- Two Locusts down"), markdown);
    }

    @Test
    void plainTextSurvivesConversionToHtmlAndBack() {
        String original = "Line one <with> & symbols\nLine two";
        String html = JournalText.plainToHtml(original);
        assertTrue(JournalText.isHtml(html));
        assertEquals(original, JournalText.htmlToPlain(html));
    }

    // endregion JournalText

    // region Filters and timeline

    @Test
    void filterMatchesEachCriterion() {
        PlotThread hunt = thread("Hunt");
        OracleCharacter ana = new OracleCharacter("Ana", null);
        JournalEntry entry = note(DAY_TWO, "The <b>Ghost</b> strikes").tagThread(hunt).tagCharacter(ana);

        assertTrue(JournalFilter.ALL.matches(entry));
        assertTrue(new JournalFilter("ghost", null, null, null, null, null).matches(entry));
        assertFalse(new JournalFilter("<b>", null, null, null, null, null).matches(entry), "markup is not searched");
        assertTrue(new JournalFilter(null, DAY_TWO, DAY_TWO, null, null, null).matches(entry));
        assertFalse(new JournalFilter(null, DAY_THREE, null, null, null, null).matches(entry));
        assertFalse(new JournalFilter(null, null, DAY_ONE, null, null, null).matches(entry));
        assertTrue(new JournalFilter(null, null, null, Set.of(JournalEntryType.NOTE), null, null).matches(entry));
        assertFalse(new JournalFilter(null, null, null, Set.of(JournalEntryType.THREAD), null, null).matches(entry));
        assertTrue(new JournalFilter(null, null, null, null, hunt.getId(), null).matches(entry));
        assertFalse(new JournalFilter(null, null, null, null, UUID.randomUUID(), null).matches(entry));
        assertTrue(new JournalFilter(null, null, null, null, null, ana.getId()).matches(entry));
        assertFalse(new JournalFilter(null, null, null, null, null, UUID.randomUUID()).matches(entry));

        assertFalse(JournalFilter.ALL.isActive());
        assertFalse(new JournalFilter("  ", null, null, null, null, null).isActive());
        assertTrue(new JournalFilter(null, null, null, Set.of(JournalEntryType.NOTE), null, null).isActive());
        assertTrue(new JournalFilter(null, null, null,
              Set.of(JournalEntryType.FATE_CHART, JournalEntryType.RANDOM_EVENT), null, null).matches(
              new JournalEntry(DAY_ONE, JournalEntryType.RANDOM_EVENT, "x")), "a group of types matches any of them");
    }

    @Test
    void timelineMergesNotesAndOracleLogByDate() {
        Roleplay roleplay = new Roleplay();
        PlotThread hunt = thread("Hunt");
        roleplay.getPlotThreads().add(hunt);

        roleplay.getJournal().add(note(DAY_THREE, "note three").tagThread(hunt));
        roleplay.getJournal().add(note(DAY_ONE, "note one"));
        roleplay.logOracle(DAY_ONE, JournalEntryType.FATE_CHART, "oracle one", 500);
        roleplay.logOracle(DAY_TWO, JournalEntryType.THREAD, "oracle two", 500).tagThread(hunt);

        assertEquals(List.of("oracle one", "note one", "oracle two", "note three"),
              roleplay.getTimeline(JournalFilter.ALL).stream().map(JournalEntry::getPlainText).toList());

        // Filtering by a thread reads just that storyline, in order.
        assertEquals(List.of("oracle two", "note three"),
              roleplay.getTimeline(new JournalFilter(null, null, null, null, hunt.getId(), null)).stream()
                    .map(JournalEntry::getPlainText).toList());
    }

    @Test
    void renamingACharacterKeepsItsTags() {
        Roleplay roleplay = new Roleplay();
        OracleCharacter ana = roleplay.addCharacter("Ana");
        JournalEntry tagged = note(DAY_ONE, "x").tagCharacter(ana);
        roleplay.getJournal().add(tagged);

        assertTrue(roleplay.renameCharacter(ana, "Anastasia"));
        assertEquals(Set.of(ana.getId()), tagged.getCharacters());
        assertEquals("Anastasia", roleplay.getCharacterName(tagged.getCharacters().iterator().next()));
    }

    // endregion Filters and timeline

    // region Persistence

    @Test
    void entriesTypesAndTagsSurviveSaveAndLoad() throws Exception {
        Roleplay original = new Roleplay();
        PlotThread hunt = thread("Hunt");
        original.getPlotThreads().add(hunt);
        OracleCharacter ana = original.addCharacter("Ana & Co");
        original.getJournal().add(note(DAY_TWO, "<p>Landed on <b>Helm</b>.<br>The &lt;locals&gt; are wary.</p>")
                                        .tagThread(hunt).tagCharacter(ana));
        JournalEntry logged = original.logOracle(DAY_THREE, JournalEntryType.RANDOM_EVENT,
              "NPC Action\nCharacter: Ana & Co", 500).tagCharacter(ana);
        logged.setAnswer(new OracleAnswer("Is it <safe>?", FateChartOdds.LIKELY, 6, 44, FateChartAnswer.NORMAL_YES));

        Roleplay loaded = Roleplay.generateInstanceFromXML(parse(write(original)));

        assertEquals(hunt.getId(), loaded.getPlotThreads().get(0).getId(), "thread ids must be kept for tags");
        JournalEntry note = loaded.getJournal().get(0);
        assertEquals(JournalEntryType.NOTE, note.getType());
        assertEquals(DAY_TWO, note.getDate());
        assertEquals(original.getJournal().get(0).getText(), note.getText());
        assertEquals(Set.of(hunt.getId()), note.getThreads());
        assertEquals(Set.of(ana.getId()), note.getCharacters());

        JournalEntry log = loaded.getOracleLog().get(0);
        assertEquals(JournalEntryType.RANDOM_EVENT, log.getType());
        assertEquals("NPC Action\nCharacter: Ana & Co", log.getText());
        assertEquals(Set.of(ana.getId()), log.getCharacters());
        assertEquals(new OracleAnswer("Is it <safe>?", FateChartOdds.LIKELY, 6, 44, FateChartAnswer.NORMAL_YES),
              log.getAnswer());
        assertNull(note.getAnswer());
    }

    @Test
    void plainNotesFromOlderSavesBecomeRichText() throws Exception {
        String xml = "<roleplay><journal><entry><date>3025-01-02</date><text>Old note &lt;1&gt;\nLine two</text>"
                           + "</entry></journal></roleplay>";
        JournalEntry note = Roleplay.generateInstanceFromXML(parse(xml)).getJournal().get(0);

        assertEquals(JournalEntryType.NOTE, note.getType());
        assertTrue(JournalText.isHtml(note.getText()));
        assertEquals("Old note <1>\nLine two", note.getPlainText());
    }

    private static String write(final Roleplay roleplay) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter writer = new PrintWriter(stringWriter)) {
            roleplay.writeToXML(writer, 0);
        }
        return stringWriter.toString();
    }

    private static Node parse(final String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                     .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).getDocumentElement();
    }

    // endregion Persistence

    // region Export

    @Test
    void exportsMarkdownAndText() {
        PlotThread hunt = thread("Hunt");
        OracleCharacter ana = new OracleCharacter("Ana", null);
        List<JournalEntry> entries = List.of(
              new JournalEntry(DAY_ONE, JournalEntryType.FATE_CHART, "Asked: Is it safe?\nRolled 12: Yes."),
              note(DAY_TWO, "<p>We move <b>tonight</b>.</p>").tagThread(hunt).tagCharacter(ana));

        String markdown = JournalExporter.export(entries, Format.MARKDOWN, "Journal", "thread Hunt",
              id -> id.equals(hunt.getId()) ? "Hunt" : null, id -> id.equals(ana.getId()) ? "Ana" : null,
              LocalDate::toString);
        assertTrue(markdown.startsWith("# Journal\n"), markdown);
        assertTrue(markdown.contains("*Filtered: thread Hunt*"), markdown);
        assertTrue(markdown.contains("## 3025-01-01 - Fate Chart"), markdown);
        assertTrue(markdown.contains("Asked: Is it safe?  \nRolled 12: Yes."), markdown);
        assertTrue(markdown.contains("*Threads: Hunt; Characters: Ana*"), markdown);
        assertTrue(markdown.contains("We move **tonight**."), markdown);

        String text = JournalExporter.export(entries, Format.TEXT, "Journal", null, id -> null,
              id -> id.equals(ana.getId()) ? "Ana" : null, LocalDate::toString);
        assertTrue(text.startsWith("Journal\n=======\n"), text);
        assertFalse(text.contains("Filtered"), text);
        assertTrue(text.contains("Threads: (deleted thread); Characters: Ana"), text);
        assertTrue(text.contains("We move tonight."), text);
        assertFalse(text.contains("**"), text);
    }

    @Test
    void formatFollowsFileExtension() {
        assertEquals(Format.MARKDOWN, Format.forFileName("story.MD"));
        assertEquals(Format.MARKDOWN, Format.forFileName("story.markdown"));
        assertEquals(Format.TEXT, Format.forFileName("story.txt"));
    }

    // endregion Export
}
