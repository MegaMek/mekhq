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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

import megamek.common.annotations.Nullable;

/**
 * Writes journal entries out as a Markdown or plain-text document, for sharing or backup.
 */
public final class JournalExporter {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    /** The export formats, each with its file extension. */
    public enum Format {
        MARKDOWN("md"),
        TEXT("txt");

        private final String extension;

        Format(final String extension) {
            this.extension = extension;
        }

        public String getExtension() {
            return extension;
        }

        /**
         * @param fileName a file name
         *
         * @return {@link #MARKDOWN} for a {@code .md} or {@code .markdown} file, otherwise {@link #TEXT}
         */
        public static Format forFileName(final String fileName) {
            String lower = fileName.toLowerCase(Locale.ROOT);
            return (lower.endsWith(".md") || lower.endsWith(".markdown")) ? MARKDOWN : TEXT;
        }
    }

    private JournalExporter() {}

    /**
     * @param entries        the entries to write, in the order to write them
     * @param format         the output format
     * @param title          the document's title
     * @param filterSummary  a description of the filter applied, or {@code null} if the export is unfiltered
     * @param threadNames    looks up a thread's name from its id; returns {@code null} for a deleted thread
     * @param castNames      looks up a character's name from its id; returns {@code null} if unknown
     * @param dateFormatter  formats an entry's date for display
     *
     * @return the document
     */
    public static String export(final List<JournalEntry> entries, final Format format, final String title,
          final @Nullable String filterSummary, final Function<UUID, String> threadNames,
          final Function<UUID, String> castNames, final Function<LocalDate, String> dateFormatter) {
        final boolean markdown = format == Format.MARKDOWN;
        final StringBuilder out = new StringBuilder();

        if (markdown) {
            out.append("# ").append(title).append("\n\n");
        } else {
            out.append(title).append('\n').append("=".repeat(title.length())).append("\n\n");
        }
        if (filterSummary != null && !filterSummary.isBlank()) {
            String line = getFormattedTextAt(RESOURCE_BUNDLE, "JournalExporter.filtered", filterSummary);
            out.append(markdown ? "*" + line + "*" : line).append("\n\n");
        }
        if (entries.isEmpty()) {
            out.append(getTextAt(RESOURCE_BUNDLE, "JournalExporter.empty")).append('\n');
            return out.toString();
        }

        for (JournalEntry entry : entries) {
            String heading = dateFormatter.apply(entry.getDate()) + " - " + entry.getType().getLabel();
            if (markdown) {
                out.append("## ").append(heading).append("\n\n");
            } else {
                out.append(heading).append('\n').append("-".repeat(heading.length())).append('\n');
            }

            String tags = describeTags(entry, threadNames, castNames);
            if (!tags.isEmpty()) {
                out.append(markdown ? "*" + JournalText.escapeMarkdown(tags) + "*" : tags)
                      .append(markdown ? "\n\n" : "\n");
            }

            String body;
            if (entry.isNote()) {
                body = markdown ? JournalText.htmlToMarkdown(entry.getText())
                             : JournalText.htmlToPlain(entry.getText());
            } else {
                // Oracle records are plain text; in Markdown, keep their line breaks with hard breaks.
                body = markdown ? JournalText.escapeMarkdown(entry.getText().strip()).replace("\n", "  \n")
                             : entry.getText().strip();
            }
            out.append(body).append("\n\n");
        }
        if (markdown) {
            // Hidden from readers, this lets the file be imported again without losing anything.
            out.append(JournalArchive.encode(entries, threadNames, castNames));
        }
        return out.toString().strip() + '\n';
    }

    private static String describeTags(final JournalEntry entry, final Function<UUID, String> threadNames,
          final Function<UUID, String> castNames) {
        List<String> parts = new ArrayList<>();
        if (!entry.getThreads().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (UUID id : entry.getThreads()) {
                String name = threadNames.apply(id);
                names.add(name == null ? getTextAt(RESOURCE_BUNDLE, "JournalExporter.deletedThread") : name);
            }
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "JournalExporter.threads", String.join(", ", names)));
        }
        if (!entry.getCharacters().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (UUID id : entry.getCharacters()) {
                String name = castNames.apply(id);
                if (name != null) {
                    names.add(name);
                }
            }
            if (!names.isEmpty()) {
                parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "JournalExporter.characters", String.join(", ", names)));
            }
        }
        return String.join("; ", parts);
    }
}
