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
    private static final Pattern LIST = Pattern.compile("(?s)<(journal|oracleLog)>.*?(?:</\\1>|\\z)");
    private static final Pattern ENTRY = Pattern.compile("(?s)<entry>.*?</entry>");

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
            final File file = freeName(save);
            Files.writeString(file.toPath(), document, StandardCharsets.UTF_8);
            LOGGER.info("Saved {} journal entries from {} to {}", entries.size(), save, file);
            return file;
        } catch (Exception e) {
            LOGGER.error("Failed to rescue the journal from {}", save, e);
            return null;
        }
    }

    /**
     * Reads the roleplay state out of a save's text, as much of it as can be read: the whole {@code <roleplay>}
     * element if it is sound, otherwise each journal list, otherwise each entry on its own.
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

        // The element is damaged: keep whatever lists, or failing that entries, can still be read.
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
            final Matcher entries = ENTRY.matcher(list);
            while (entries.find()) {
                if (parse("<roleplay><" + tag + ">" + entries.group() + "</" + tag + "></roleplay>") != null) {
                    repaired.append(entries.group());
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
        } catch (Exception e) {
            return null;
        }
    }

    private static String readText(final File save) throws IOException {
        try (InputStream raw = new BufferedInputStream(Files.newInputStream(save.toPath()))) {
            raw.mark(2);
            final boolean zipped = raw.read() == 0x1f && raw.read() == 0x8b;
            raw.reset();
            final InputStream in = zipped ? new GZIPInputStream(raw) : raw;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * @return a file next to the save that does not exist yet, such as "My Campaign - journal.md"
     */
    static File freeName(final File save) {
        String name = save.getName();
        for (String extension : List.of(".gz", ".cpnx", ".xml")) {
            if (name.toLowerCase(Locale.ROOT).endsWith(extension)) {
                name = name.substring(0, name.length() - extension.length());
            }
        }
        final String suffix = getTextAt(RESOURCE_BUNDLE, "JournalRescue.fileSuffix");
        File file = new File(save.getAbsoluteFile().getParentFile(), name + suffix + ".md");
        for (int number = 2; file.exists(); number++) {
            file = new File(save.getAbsoluteFile().getParentFile(), name + suffix + " " + number + ".md");
        }
        return file;
    }
}
