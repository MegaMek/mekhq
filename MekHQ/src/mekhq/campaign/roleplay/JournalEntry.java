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
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * One dated entry in the roleplay journal: either a note the player wrote, or an automatic Oracle log record.
 */
public class JournalEntry {
    private static final MMLogger LOGGER = MMLogger.create(JournalEntry.class);

    private LocalDate date;
    private String text;

    /**
     * @param date the in-game date of the entry
     * @param text the entry's text
     */
    public JournalEntry(final LocalDate date, final String text) {
        this.date = date;
        this.text = text;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(final LocalDate date) {
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
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
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "text", entry.text);
            MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "entry");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, tag);
    }

    /**
     * Reads the {@code <entry>} children of a list element written by
     * {@link #writeListToXML(PrintWriter, int, String, List)}. Entries that cannot be read are logged and skipped.
     *
     * @param node the list element
     *
     * @return the entries, in saved order
     */
    static List<JournalEntry> parseList(final Node node) {
        final List<JournalEntry> entries = new ArrayList<>();
        final NodeList entryNodes = node.getChildNodes();
        for (int i = 0; i < entryNodes.getLength(); i++) {
            final Node entryNode = entryNodes.item(i);
            if (entryNode.getNodeName().equals("entry")) {
                final JournalEntry entry = parse(entryNode);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    private static @Nullable JournalEntry parse(final Node node) {
        try {
            LocalDate date = null;
            String text = "";
            final NodeList fields = node.getChildNodes();
            for (int i = 0; i < fields.getLength(); i++) {
                final Node field = fields.item(i);
                if (field.getNodeName().equals("date")) {
                    date = MHQXMLUtility.parseDate(field.getTextContent().trim());
                } else if (field.getNodeName().equals("text")) {
                    text = field.getTextContent();
                }
            }
            return (date == null) ? null : new JournalEntry(date, text);
        } catch (Exception e) {
            LOGGER.error("Failed to load journal entry", e);
            return null;
        }
    }
}
