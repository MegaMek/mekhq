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

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * One dated entry in the roleplay journal: either a rich-text note the player wrote, or an automatic Oracle log
 * record. Entries can be tagged with the plot threads and Oracle characters they involve, so a storyline can be read
 * on its own.
 *
 * <p>Notes are stored as HTML; Oracle log records are plain text. {@link #getPlainText()} gives either as plain
 * text.</p>
 */
public class JournalEntry {
    private static final MMLogger LOGGER = MMLogger.create(JournalEntry.class);

    private LocalDate date;
    private final JournalEntryType type;
    private String text;
    private final Set<UUID> threads = new LinkedHashSet<>();
    private final Set<UUID> characters = new LinkedHashSet<>();
    private OracleAnswer answer;
    private CheckRecord check;
    /** The note's text without formatting, worked out when first needed; cleared whenever the text changes. */
    private String plainText;
    /** Character names from saves made before characters had ids; {@link Roleplay} turns them into ids on load. */
    final List<String> legacyCharacterNames = new ArrayList<>();

    /**
     * @param date the in-game date of the entry
     * @param type what kind of entry this is
     * @param text the entry's text: HTML for a {@link JournalEntryType#NOTE}, plain text otherwise
     */
    public JournalEntry(final LocalDate date, final JournalEntryType type, final String text) {
        this.date = date;
        this.type = type;
        this.text = text;
    }

    /**
     * Creates an empty note.
     *
     * @param date the in-game date of the note
     *
     * @return the note
     */
    public static JournalEntry newNote(final LocalDate date) {
        return new JournalEntry(date, JournalEntryType.NOTE, JournalText.plainToHtml(""));
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(final LocalDate date) {
        this.date = date;
    }

    public JournalEntryType getType() {
        return type;
    }

    /**
     * @return {@code true} if this is a note the player wrote, rather than an Oracle log record
     */
    public boolean isNote() {
        return type == JournalEntryType.NOTE;
    }

    /**
     * @return the stored text: HTML for a note, plain text for an Oracle log record
     */
    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
        plainText = null;
    }

    /**
     * @return the entry's text with any formatting removed
     */
    public String getPlainText() {
        if (!isNote()) {
            return text;
        }
        // Converting HTML is slow enough to matter when a list of notes is drawn, so keep the result.
        if (plainText == null) {
            plainText = JournalText.htmlToPlain(text);
        }
        return plainText;
    }

    /**
     * @return the entry's text as an HTML fragment, suitable for placing inside a larger HTML document
     */
    public String getHtmlBody() {
        return isNote() ? JournalText.bodyOf(text) : JournalText.escapeHtml(text).replace("\n", "<br>");
    }

    /**
     * @return the live set of ids of the plot threads this entry is tagged with
     */
    public Set<UUID> getThreads() {
        return threads;
    }

    /**
     * @return the live set of ids of the Oracle characters this entry is tagged with
     */
    public Set<UUID> getCharacters() {
        return characters;
    }

    /**
     * @return the Fate Chart question this record logs, or {@code null} if it is not a Fate Chart record
     */
    public @Nullable OracleAnswer getAnswer() {
        return answer;
    }

    public void setAnswer(final @Nullable OracleAnswer answer) {
        this.answer = answer;
    }

    /**
     * @return the check this record logs, or {@code null} if it is not a check record
     */
    public @Nullable CheckRecord getCheck() {
        return check;
    }

    public void setCheck(final @Nullable CheckRecord check) {
        this.check = check;
    }

    /**
     * Tags this entry with a plot thread.
     *
     * @param thread the thread, or {@code null} to do nothing
     *
     * @return this entry, for chaining
     */
    public JournalEntry tagThread(final @Nullable PlotThread thread) {
        if (thread != null) {
            threads.add(thread.getId());
        }
        return this;
    }

    /**
     * Tags this entry with an Oracle character.
     *
     * @param character the character, or {@code null} to do nothing
     *
     * @return this entry, for chaining
     */
    public JournalEntry tagCharacter(final @Nullable OracleCharacter character) {
        if (character != null) {
            characters.add(character.getId());
        }
        return this;
    }

    /**
     * Writes a list of entries inside an element with the given name.
     *
     * @param writer  the writer to output to
     * @param indent  the current indentation level
     * @param tag     the name of the wrapping element
     * @param entries the entries to write; nothing is written if empty
     */
    static void writeListToXML(final PrintWriter writer, int indent, final String tag,
          final List<JournalEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, tag);
        for (JournalEntry entry : entries) {
            MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "entry");
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "date", entry.date);
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "type", entry.type.name());
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "text", entry.text);
            for (UUID thread : entry.threads) {
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "thread", thread);
            }
            for (UUID character : entry.characters) {
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "cast", character);
            }
            if (entry.answer != null) {
                entry.answer.writeToXML(writer, indent);
            }
            if (entry.check != null) {
                entry.check.writeToXML(writer, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "entry");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, tag);
    }

    /**
     * Reads the {@code <entry>} children of a list element written by
     * {@link #writeListToXML(PrintWriter, int, String, List)}. Entries that cannot be read are logged and skipped.
     *
     * @param node        the list element
     * @param defaultType the type to give entries saved before entries had types
     *
     * @return the entries, in saved order
     */
    static List<JournalEntry> parseList(final Node node, final JournalEntryType defaultType) {
        final List<JournalEntry> entries = new ArrayList<>();
        final NodeList entryNodes = node.getChildNodes();
        for (int i = 0; i < entryNodes.getLength(); i++) {
            final Node entryNode = entryNodes.item(i);
            if (entryNode.getNodeName().equals("entry")) {
                final JournalEntry entry = parse(entryNode, defaultType);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    private static @Nullable JournalEntry parse(final Node node, final JournalEntryType defaultType) {
        try {
            LocalDate date = null;
            JournalEntryType type = defaultType;
            String text = "";
            final List<UUID> threads = new ArrayList<>();
            final List<UUID> characters = new ArrayList<>();
            final List<String> legacyNames = new ArrayList<>();
            OracleAnswer answer = null;
            CheckRecord check = null;

            final NodeList fields = node.getChildNodes();
            for (int i = 0; i < fields.getLength(); i++) {
                final Node field = fields.item(i);
                switch (field.getNodeName()) {
                    case "date" -> date = MHQXMLUtility.parseDate(field.getTextContent().trim());
                    case "type" -> type = JournalEntryType.valueOf(field.getTextContent().trim());
                    case "text" -> text = field.getTextContent();
                    case "thread" -> threads.add(UUID.fromString(field.getTextContent().trim()));
                    case "cast" -> characters.add(UUID.fromString(field.getTextContent().trim()));
                    case "character" -> legacyNames.add(field.getTextContent());
                    case "answer" -> answer = OracleAnswer.parse(field);
                    case "check" -> check = CheckRecord.parse(field);
                    default -> { }
                }
            }
            if (date == null) {
                return null;
            }

            // Notes saved before the journal had rich text are plain; convert them so every note is HTML.
            if (type == JournalEntryType.NOTE && !JournalText.isHtml(text)) {
                text = JournalText.plainToHtml(text);
            }

            final JournalEntry entry = new JournalEntry(date, type, text);
            entry.threads.addAll(threads);
            entry.characters.addAll(characters);
            entry.legacyCharacterNames.addAll(legacyNames);
            entry.answer = answer;
            entry.check = check;
            return entry;
        } catch (Exception e) {
            LOGGER.error("Failed to load journal entry", e);
            return null;
        }
    }
}
