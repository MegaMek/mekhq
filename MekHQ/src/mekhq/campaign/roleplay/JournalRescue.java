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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.roleplay.JournalExporter.Format;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;

/**
 * Saves a player's journal from a campaign save that can't be loaded. The journal is read straight out of the save's
 * XML, so it survives problems anywhere else in the file, and written next to the save as a Markdown file that can be
 * imported into a campaign.
 */
public final class JournalRescue {
    private static final MMLogger LOGGER = MMLogger.create(JournalRescue.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";
    private static final Pattern LIST = Pattern.compile(
          "(?s)<(journal|oracleLog|characters|plotThreads)>.*?(?:</\\1>|\\z)");
    private static final Pattern ITEM = Pattern.compile("(?s)<(entry|character|plotThread)\\b.*?</\\1>");
    private static final int READ_CHUNK = 64 * 1024;

    private JournalRescue() {
    }

    /**
     * Reads the journal from a save and writes it next to the save as Markdown.
     *
     * @param save          the save file, compressed or not
     * @param dateFormatter formats dates for the export's headings
     *
     * @return the file written, or {@code null} if the save had no journal or it couldn't be read or written
     */
    public static @Nullable File rescue(final File save, final Function<LocalDate, String> dateFormatter) {
        try {
            final Roleplay roleplay = read(readText(save));
            if (roleplay == null) {
                return null;
            }
            final List<JournalEntry> entries = new ArrayList<>(roleplay.getJournal());
            entries.addAll(roleplay.getOracleLog());
            if (entries.isEmpty()) {
                return null;
            }
            entries.sort(Comparator.comparing(JournalEntry::getDate));
            final String document = JournalExporter.export(entries, Format.MARKDOWN,
                  getTextAt(RESOURCE_BUNDLE, "JournalRescue.title"), null, roleplay::getPlotThreadName,
                  roleplay::getCharacterName, dateFormatter);
            final File earlier = findEarlierRescue(save, document);
            if (earlier != null) {
                // Trying the same broken save again shouldn't leave another copy each time.
                return earlier;
            }
            final File file = freeName(save);
            Files.writeString(file.toPath(), document, StandardCharsets.UTF_8);
            LOGGER.info("Saved {} journal entries from {} to {}", entries.size(), save, file);
            return file;
        } catch (Exception exception) {
            LOGGER.error("Failed to rescue the journal from {}", save, exception);
            return null;
        }
    }

    /**
     * Reads the roleplay state out of a save's text, as much of it as can be read: the whole {@code <roleplay>}
     * element if it is sound, otherwise each list (journal, Oracle log, cast and threads), otherwise each item on its
     * own.
     *
     * @param xml the save's text
     *
     * @return the roleplay state found, or {@code null} if there was none
     */
    static @Nullable Roleplay read(final String xml) {
        final int open = xml.indexOf("<roleplay>");
        if (open < 0) {
            return null;
        }
        final int close = xml.lastIndexOf("</roleplay>");
        // With no closing tag the file was cut short; what follows the opening tag is the best there is.
        final String section = (close > open) ? xml.substring(open, close + "</roleplay>".length())
                                     : xml.substring(open);
        final Roleplay parsed = parse(section);
        if (parsed != null) {
            return parsed;
        }

        // The element is damaged: keep whatever lists, or failing that items, can still be read. The cast and threads
        // are kept too, so the journal's tags still name them.
        final StringBuilder repaired = new StringBuilder("<roleplay>");
        final Matcher lists = LIST.matcher(section);
        while (lists.find()) {
            final String list = lists.group();
            final String tag = lists.group(1);
            if (parse("<roleplay>" + list + "</roleplay>") != null) {
                repaired.append(list);
                continue;
            }
            repaired.append('<').append(tag).append('>');
            final Matcher items = ITEM.matcher(list);
            while (items.find()) {
                if (parse("<roleplay><" + tag + ">" + items.group() + "</" + tag + "></roleplay>") != null) {
                    repaired.append(items.group());
                }
            }
            repaired.append("</").append(tag).append('>');
        }
        return parse(repaired.append("</roleplay>").toString());
    }

    private static @Nullable Roleplay parse(final String xml) {
        try {
            final Node node = MHQXMLUtility.newSafeDocumentBuilder()
                                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                                    .getDocumentElement();
            return node.getNodeName().equals("roleplay") ? Roleplay.generateInstanceFromXML(node) : null;
        } catch (Exception unreadable) {
            return null;
        }
    }

    /**
     * Reads a save's text, decompressing it if needed. A compressed save that was cut short still gives up everything
     * before the cut.
     */
    static String readText(final File save) throws IOException {
        try (InputStream raw = new BufferedInputStream(Files.newInputStream(save.toPath()))) {
            raw.mark(2);
            final boolean zipped = raw.read() == 0x1f && raw.read() == 0x8b;
            raw.reset();
            final InputStream source = zipped ? new GZIPInputStream(raw) : raw;
            final ByteArrayOutputStream text = new ByteArrayOutputStream();
            final byte[] buffer = new byte[READ_CHUNK];
            try {
                for (int read = source.read(buffer); read >= 0; read = source.read(buffer)) {
                    text.write(buffer, 0, read);
                }
            } catch (EOFException truncated) {
                LOGGER.warn("{} ends early; reading the part that is there", save);
            }
            return text.toString(StandardCharsets.UTF_8);
        }
    }

    /**
     * @return an earlier rescue of this save with exactly this content, or {@code null} if there is none
     */
    private static @Nullable File findEarlierRescue(final File save, final String document) throws IOException {
        for (int number = 1; candidate(save, number).exists(); number++) {
            final File file = candidate(save, number);
            if (Files.readString(file.toPath(), StandardCharsets.UTF_8).equals(document)) {
                return file;
            }
        }
        return null;
    }

    /**
     * @return a file next to the save that does not exist yet, such as "My Campaign - journal.md"
     */
    static File freeName(final File save) {
        int number = 1;
        while (candidate(save, number).exists()) {
            number++;
        }
        return candidate(save, number);
    }

    /**
     * @return the name for a save's {@code number}th rescue: "My Campaign - journal.md", then "... journal 2.md"
     */
    private static File candidate(final File save, final int number) {
        String name = save.getName();
        for (String extension : List.of(".gz", ".cpnx", ".xml")) {
            if (name.toLowerCase(Locale.ROOT).endsWith(extension)) {
                name = name.substring(0, name.length() - extension.length());
            }
        }
        final String suffix = getTextAt(RESOURCE_BUNDLE, "JournalRescue.fileSuffix");
        return new File(save.getAbsoluteFile().getParentFile(),
              name + suffix + ((number > 1) ? " " + number : "") + ".md");
    }
}
