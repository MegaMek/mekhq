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

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Reads journals back in from exported Markdown files.
 *
 * <p>A Markdown export ends with a hidden comment holding the entries exactly as saved, with their tags, answers and
 * checks, so importing it loses nothing. Markdown renderers do not show the comment. Files exported before the
 * comment existed are read from their text instead: each "## date - type" heading starts an entry, and notes are
 * converted back from Markdown.</p>
 */
public final class JournalArchive {
    private static final MMLogger LOGGER = MMLogger.create(JournalArchive.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    static final String START = "<!-- mekhq-journal-data";
    static final String END = "-->";
    private static final int LINE_LENGTH = 76;
    /**
     * Far beyond any real journal, even 100,000 records plus notes, so no genuine export is ever refused. It bounds a
     * damaged or hostile file rather than making it harmless: one that decompresses to this size still needs a few
     * gigabytes of memory to read, and a player who imports it may run out.
     */
    private static final int MAXIMUM_DATA_BYTES = 1024 * 1024 * 1024;

    /**
     * What an import found.
     *
     * @param entries    the entries, oldest first
     * @param threads    the names of the threads they are tagged with, by id
     * @param characters the names of the cast members they are tagged with, by id
     * @param exact      {@code true} if read from the hidden data, {@code false} if read from the text
     */
    public record Archive(List<JournalEntry> entries, Map<UUID, String> threads, Map<UUID, String> characters,
          boolean exact) {}

    /**
     * What an import added.
     *
     * @param notes      how many notes were added
     * @param records    how many Oracle and campaign records were added
     * @param duplicates how many entries were skipped because the journal already had them
     * @param trimmed    how many of the oldest records were dropped to keep the Oracle log within its limit
     */
    public record ImportResult(int notes, int records, int duplicates, int trimmed) {}

    private JournalArchive() {
    }

    // region Writing

    /**
     * @param entries     the entries to store
     * @param threadNames looks up a thread's name from its id
     * @param castNames   looks up a cast member's name from its id
     *
     * @return the hidden comment to end a Markdown export with
     */
    static String encode(final List<JournalEntry> entries, final Function<UUID, String> threadNames,
          final Function<UUID, String> castNames) {
        final StringWriter xml = new StringWriter();
        try (PrintWriter writer = new PrintWriter(xml)) {
            int indent = 0;
            MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "journalArchive");
            final Set<UUID> threads = new HashSet<>();
            final Set<UUID> characters = new HashSet<>();
            entries.forEach(entry -> {
                threads.addAll(entry.getThreads());
                characters.addAll(entry.getCharacters());
            });
            writeNames(writer, indent, "thread", threads, threadNames);
            writeNames(writer, indent, "character", characters, castNames);
            JournalEntry.writeListToXML(writer, indent, "entries", entries);
            MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "journalArchive");
        }

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream zip = new GZIPOutputStream(bytes)) {
            zip.write(xml.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
        final String data = Base64.getEncoder().encodeToString(bytes.toByteArray());
        final StringBuilder out = new StringBuilder(START).append('\n');
        for (int i = 0; i < data.length(); i += LINE_LENGTH) {
            out.append(data, i, Math.min(data.length(), i + LINE_LENGTH)).append('\n');
        }
        return out.append(END).append('\n').toString();
    }

    private static void writeNames(final PrintWriter writer, final int indent, final String tag, final Set<UUID> ids,
          final Function<UUID, String> names) {
        for (UUID id : ids) {
            final String name = names.apply(id);
            if (name != null) {
                writer.println("\t".repeat(indent) + "<" + tag + " id=\"" + id + "\">"
                                     + JournalText.escapeHtml(name) + "</" + tag + ">");
            }
        }
    }

    // endregion Writing

    // region Reading

    /**
     * Reads an exported journal.
     *
     * @param document   the file's text
     * @param dateParser reads a date as the export wrote it, for files without the hidden data
     *
     * @return what was found; empty if the file holds no entries
     */
    public static Archive read(final String document, final Function<String, LocalDate> dateParser) {
        final Archive exact = decode(document);
        return (exact != null) ? exact : readText(document, dateParser);
    }

    /**
     * @return the entries in the hidden data, or {@code null} if there is none or it can't be read
     */
    static @Nullable Archive decode(final String document) {
        final int start = document.lastIndexOf(START);
        if (start < 0) {
            return null;
        }
        final int end = document.indexOf(END, start + START.length());
        if (end < 0) {
            return null;
        }
        try {
            final String data = document.substring(start + START.length(), end).replaceAll("\\s", "");
            final byte[] xml;
            try (GZIPInputStream zip = new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder()
                                                                                          .decode(data)))) {
                xml = zip.readNBytes(MAXIMUM_DATA_BYTES + 1);
            }
            if (xml.length > MAXIMUM_DATA_BYTES) {
                throw new IOException("journal data is too large");
            }
            final Node root = MHQXMLUtility.newSafeDocumentBuilder()
                                    .parse(new ByteArrayInputStream(xml)).getDocumentElement();
            final List<JournalEntry> entries = new ArrayList<>();
            final Map<UUID, String> threads = new LinkedHashMap<>();
            final Map<UUID, String> characters = new LinkedHashMap<>();
            final NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                final Node child = children.item(i);
                switch (child.getNodeName()) {
                    case "thread" -> threads.put(idOf(child), child.getTextContent());
                    case "character" -> characters.put(idOf(child), child.getTextContent());
                    case "entries" -> entries.addAll(JournalEntry.parseList(child, JournalEntryType.NOTE));
                    default -> { }
                }
            }
            entries.sort(Comparator.comparing(JournalEntry::getDate));
            return new Archive(entries, threads, characters, true);
        } catch (Exception exception) {
            LOGGER.warn("The journal's hidden data could not be read; reading its text instead", exception);
            return null;
        }
    }

    private static UUID idOf(final Node node) {
        return UUID.fromString(node.getAttributes().getNamedItem("id").getTextContent().trim());
    }

    /**
     * Reads entries from an export's text: each "## date - type" heading starts one. Headings whose date can't be read
     * are treated as part of the entry before them.
     */
    static Archive readText(final String document, final Function<String, LocalDate> dateParser) {
        final List<JournalEntry> entries = new ArrayList<>();
        final Map<UUID, String> threads = new LinkedHashMap<>();
        final Map<UUID, String> characters = new LinkedHashMap<>();
        final Pattern threadsLine = labelPattern("JournalExporter.threads");
        final Pattern charactersLine = labelPattern("JournalExporter.characters");

        LocalDate date = null;
        JournalEntryType type = null;
        List<String> tags = List.of();
        StringBuilder body = null;
        final String text = document.contains(START) ? document.substring(0, document.lastIndexOf(START)) : document;
        final String[] lines = text.split("\\R", -1);
        for (int index = 0; index <= lines.length; index++) {
            // One pass past the last line flushes the final entry.
            final boolean last = index == lines.length;
            final String line = last ? "" : lines[index];
            final Heading heading = (!last && line.startsWith("## ")) ? heading(line.substring(3), dateParser) : null;
            if (heading == null && !last) {
                if (body == null) {
                    continue;
                }
                if (body.toString().isBlank() && tags.isEmpty() && line.startsWith("*") && line.endsWith("*")
                          && line.length() > 2) {
                    final List<String> parts = List.of(line.substring(1, line.length() - 1).split("; "));
                    // Only a line made wholly of tag lists is tags; anything else is an italic first line.
                    final boolean allTags = parts.stream()
                                                  .allMatch(part -> threadsLine.matcher(part.strip()).matches()
                                                                          || charactersLine.matcher(part.strip())
                                                                                   .matches());
                    if (allTags) {
                        tags = parts;
                        continue;
                    }
                }
                body.append(line).append('\n');
                continue;
            }
            if (body != null) {
                final JournalEntry entry = entryFromText(date, type, body.toString().strip());
                for (String tag : tags) {
                    tagByName(entry, tag, threadsLine, threads, true);
                    tagByName(entry, tag, charactersLine, characters, false);
                }
                entries.add(entry);
            }
            if (heading != null) {
                date = heading.date();
                type = heading.type();
                tags = List.of();
                body = new StringBuilder();
            }
        }
        entries.sort(Comparator.comparing(JournalEntry::getDate));
        return new Archive(entries, threads, characters, false);
    }

    private record Heading(LocalDate date, JournalEntryType type) {}

    private static @Nullable Heading heading(final String text, final Function<String, LocalDate> dateParser) {
        final int split = text.lastIndexOf(" - ");
        if (split < 0) {
            return null;
        }
        final LocalDate date;
        try {
            date = dateParser.apply(text.substring(0, split).strip());
        } catch (Exception exception) {
            return null;
        }
        if (date == null) {
            return null;
        }
        final String label = text.substring(split + 3).strip();
        JournalEntryType type = JournalEntryType.NOTE;
        for (JournalEntryType candidate : JournalEntryType.values()) {
            if (candidate.getLabel().equalsIgnoreCase(label)) {
                type = candidate;
            }
        }
        return new Heading(date, type);
    }

    private static JournalEntry entryFromText(final LocalDate date, final JournalEntryType type, final String body) {
        if (type == JournalEntryType.NOTE) {
            return new JournalEntry(date, type, markdownToHtml(body));
        }
        // Records were written with Markdown hard breaks.
        return new JournalEntry(date, type, JournalText.unescapeMarkdown(body.replaceAll(" {2}\\n", "\n")));
    }

    /**
     * Tags an entry by name, noting a made-up id against the name so the tag can be matched up on import.
     */
    private static void tagByName(final JournalEntry entry, final String tag, final Pattern line,
          final Map<UUID, String> names, final boolean thread) {
        final Matcher matcher = line.matcher(tag.strip());
        if (!matcher.matches()) {
            return;
        }
        for (String name : JournalText.unescapeMarkdown(matcher.group(1)).split(", ")) {
            if (name.isBlank()) {
                continue;
            }
            UUID id = names.entrySet().stream().filter(known -> known.getValue().equals(name)).map(Map.Entry::getKey)
                            .findFirst().orElse(null);
            if (id == null) {
                id = UUID.nameUUIDFromBytes(((thread ? "thread:" : "cast:") + name).getBytes(StandardCharsets.UTF_8));
                names.put(id, name);
            }
            (thread ? entry.getThreads() : entry.getCharacters()).add(id);
        }
    }

    private static Pattern labelPattern(final String key) {
        final String format = getTextAt(RESOURCE_BUNDLE, key);
        final int slot = format.indexOf("{0}");
        return Pattern.compile(Pattern.quote(format.substring(0, slot)) + "(.*)"
                                     + Pattern.quote(format.substring(slot + 3)));
    }

    /**
     * Converts the Markdown a note was exported as back to the journal's HTML: paragraphs, "### " headings, "- "
     * bullets, bold and italic.
     */
    static String markdownToHtml(final String markdown) {
        final StringBuilder html = new StringBuilder("<html><body>");
        boolean inList = false;
        final StringBuilder paragraph = new StringBuilder();
        for (String line : (markdown + "\n").split("\\R", -1)) {
            final String stripped = line.strip();
            final boolean bullet = stripped.startsWith("- ");
            final boolean heading = stripped.startsWith("#");
            if (stripped.isEmpty() || bullet || heading) {
                if (!paragraph.isEmpty()) {
                    html.append("<p>").append(paragraph).append("</p>");
                    paragraph.setLength(0);
                }
            }
            if (inList && !bullet) {
                html.append("</ul>");
                inList = false;
            }
            if (bullet) {
                if (!inList) {
                    html.append("<ul>");
                    inList = true;
                }
                html.append("<li>").append(inline(stripped.substring(2))).append("</li>");
            } else if (heading) {
                html.append("<h3>").append(inline(stripped.replaceFirst("^#+\\s*", ""))).append("</h3>");
            } else if (!stripped.isEmpty()) {
                if (!paragraph.isEmpty()) {
                    paragraph.append("<br>");
                }
                paragraph.append(inline(stripped));
            }
        }
        return html.append("</body></html>").toString();
    }

    private static String inline(final String text) {
        // An escaped asterisk is the player's own, not formatting.
        final String literalAsterisk = "\uE004";
        final String plain = JournalText.unescapeMarkdown(text.replace("\\*", literalAsterisk));
        return JournalText.escapeHtml(plain)
                     .replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>")
                     .replaceAll("\\*(.+?)\\*", "<i>$1</i>")
                     .replace(literalAsterisk, "*");
    }

    // endregion Reading

    // region Importing

    /**
     * Adds an archive's entries to a campaign's journal. Entries the journal already has (same date, type and text)
     * are skipped. Tags are matched to this campaign's threads and cast by id, then by name; a thread tag that
     * matches neither is dropped, and a cast member who matches neither is added to the cast as removed, so the tag
     * is kept.
     *
     * @param roleplay       the campaign's roleplay state
     * @param archive        what to import
     * @param maximumRecords the Oracle log's size limit
     *
     * @return what was added
     */
    public static ImportResult importInto(final Roleplay roleplay, final Archive archive, final int maximumRecords) {
        // Only what the campaign already has counts as a duplicate: two identical dice rolls on one day in the
        // imported file are both real.
        final Set<String> known = new HashSet<>();
        roleplay.getJournal().forEach(entry -> known.add(key(entry)));
        roleplay.getOracleLog().forEach(entry -> known.add(key(entry)));

        final Map<UUID, UUID> threadIds = new LinkedHashMap<>();
        final Map<UUID, UUID> characterIds = new LinkedHashMap<>();
        int notes = 0;
        int records = 0;
        int duplicates = 0;
        for (JournalEntry entry : archive.entries()) {
            if (known.contains(key(entry))) {
                duplicates++;
                continue;
            }
            final List<UUID> threads = new ArrayList<>(entry.getThreads());
            entry.getThreads().clear();
            for (UUID id : threads) {
                final UUID mapped = threadIds.computeIfAbsent(id, old -> matchThread(roleplay, old,
                      archive.threads().get(old)));
                if (mapped != null) {
                    entry.getThreads().add(mapped);
                }
            }
            final List<UUID> characters = new ArrayList<>(entry.getCharacters());
            entry.getCharacters().clear();
            for (UUID id : characters) {
                final UUID mapped = characterIds.computeIfAbsent(id, old -> matchCharacter(roleplay, old,
                      archive.characters().get(old)));
                if (mapped != null) {
                    entry.getCharacters().add(mapped);
                }
            }
            if (entry.isNote()) {
                roleplay.getJournal().add(entry);
                notes++;
            } else {
                roleplay.getOracleLog().add(entry);
                records++;
            }
        }
        roleplay.getJournal().sort(Comparator.comparing(JournalEntry::getDate));
        roleplay.getOracleLog().sort(Comparator.comparing(JournalEntry::getDate));
        final int trimmed = roleplay.trimOracleLog(maximumRecords);
        return new ImportResult(notes, records, duplicates, trimmed);
    }

    private static String key(final JournalEntry entry) {
        final String text = entry.isNote() ? JournalText.htmlToPlain(entry.getText()) : entry.getText();
        return entry.getDate() + "|" + entry.getType() + "|" + text.strip().replaceAll("\\s+", " ");
    }

    private static @Nullable UUID matchThread(final Roleplay roleplay, final UUID id, final @Nullable String name) {
        if (roleplay.getPlotThread(id) != null) {
            return id;
        }
        return roleplay.getPlotThreads().stream()
                     .filter(thread -> name != null && thread.getName().equalsIgnoreCase(name.strip()))
                     .map(PlotThread::getId)
                     .findFirst()
                     .orElse(null);
    }

    private static @Nullable UUID matchCharacter(final Roleplay roleplay, final UUID id,
          final @Nullable String name) {
        if (roleplay.getCharacter(id) != null) {
            return id;
        }
        if (name == null || name.isBlank()) {
            return null;
        }
        for (OracleCharacter character : roleplay.getCharacters()) {
            if (character.getName().toLowerCase(Locale.ROOT).equals(name.strip().toLowerCase(Locale.ROOT))) {
                return character.getId();
            }
        }
        final OracleCharacter added = roleplay.addCharacter(name);
        if (added == null) {
            return null;
        }
        roleplay.removeCharacter(added);
        return added.getId();
    }

    // endregion Importing
}
