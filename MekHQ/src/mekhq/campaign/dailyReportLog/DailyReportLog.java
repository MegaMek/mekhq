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
package mekhq.campaign.dailyReportLog;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import megamek.logging.MMLogger;
import mekhq.campaign.enums.DailyReportType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Owns the campaign daily log: the running set of report lines for every {@link DailyReportType}, the rendered HTML
 * cache for each, and the "new since last GUI refresh" delta for each.
 *
 * <p>This state used to live directly on {@code Campaign} as thirty hand-maintained fields with a per-type
 * copy-pasted lifecycle spread across the campaign, its XML I/O, the new-day manager, and the GUI. It is collected here
 * so the daily log is a single, self-contained unit.</p>
 *
 * <p>The {@link DailyReportType#AGGREGATE} channel is a derived, in-memory-only merge of every line in add order;
 * it is repopulated as a session runs and is deliberately <em>not</em> persisted (it cannot be reconstructed from the
 * other channels after a load because those do not retain global ordering).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class DailyReportLog {
    private static final MMLogger LOGGER = MMLogger.create(DailyReportLog.class);

    public static final String REPORT_LINEBREAK = "<br/><br/>";

    private static final String XML_ROOT = "dailyReportLog";
    private static final String XML_REPORT = "report";
    private static final String XML_ATTR_TYPE = "type";
    private static final String XML_LINE = "reportLine";

    /**
     * A single report channel: the full list of lines, the rendered HTML cache, and the delta of lines added since the
     * GUI last drained it.
     */
    private static final class Channel {
        private final List<String> lines = new ArrayList<>();
        private String html = "";
        private List<String> newLines = new ArrayList<>();

        void add(String report) {
            lines.add(report);
            if (!html.isEmpty()) {
                html = html + REPORT_LINEBREAK + report;
                newLines.add(REPORT_LINEBREAK);
            } else {
                html = report;
            }
            newLines.add(report);
        }

        void clear() {
            lines.clear();
            html = "";
            newLines.clear();
        }

        List<String> fetchAndClearNew() {
            List<String> old = newLines;
            newLines = new ArrayList<>();
            return old;
        }

        /**
         * Rebuilds the transient {@code html} cache and {@code newLines} delta from {@code lines}. Used after a load,
         * where only the lines are persisted. The delta is seeded with the full history to mirror the historical
         * behavior of the XML parser.
         */
        void rebuildFromLines() {
            StringBuilder rebuiltHtml = new StringBuilder();
            List<String> rebuiltNew = new ArrayList<>(lines.size() * 2);
            boolean first = true;
            for (String report : lines) {
                if (first) {
                    first = false;
                } else {
                    rebuiltHtml.append(REPORT_LINEBREAK);
                    rebuiltNew.add(REPORT_LINEBREAK);
                }
                rebuiltHtml.append(report);
                rebuiltNew.add(report);
            }
            html = rebuiltHtml.toString();
            newLines = rebuiltNew;
        }
    }

    private final EnumMap<DailyReportType, Channel> channels = new EnumMap<>(DailyReportType.class);

    public DailyReportLog() {
        for (DailyReportType type : DailyReportType.values()) {
            channels.put(type, new Channel());
        }
    }

    /**
     * Adds a single line to the given channel, updating its HTML cache and new-line delta.
     *
     * @param type   the channel to append to
     * @param report the report line
     */
    public void add(DailyReportType type, String report) {
        channels.get(type).add(report);
    }

    /**
     * Appends a line (typically the new-day date header) to every channel.
     *
     * @param report the report line
     */
    public void beginNewDay(String report) {
        for (DailyReportType type : DailyReportType.values()) {
            channels.get(type).add(report);
        }
    }

    /**
     * Clears every channel. Used when a new day resets the daily log.
     */
    public void clear() {
        for (Channel channel : channels.values()) {
            channel.clear();
        }
    }

    /**
     * @param type the channel
     *
     * @return the live backing list of lines for the channel (mutable, as the previous {@code Campaign} getters were)
     */
    public List<String> getLines(DailyReportType type) {
        return channels.get(type).lines;
    }

    /**
     * @param type the channel
     *
     * @return the rendered HTML cache for the channel
     */
    public String getHtml(DailyReportType type) {
        return channels.get(type).html;
    }

    public void setHtml(DailyReportType type, String html) {
        channels.get(type).html = html;
    }

    /**
     * Returns the lines added to the channel since this method was last called for it, then resets that delta.
     *
     * @param type the channel
     *
     * @return the drained new-line delta
     */
    public List<String> fetchAndClearNew(DailyReportType type) {
        return channels.get(type).fetchAndClearNew();
    }

    /**
     * Writes every persisted channel as a single {@value #XML_ROOT} element. {@link DailyReportType#AGGREGATE} is
     * intentionally skipped, as it is derived, in-memory-only state.
     *
     * @param writer the writer
     * @param indent the indent to write the root element at
     */
    public void writeToXML(PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, XML_ROOT);
        for (DailyReportType type : DailyReportType.values()) {
            if (type == DailyReportType.AGGREGATE) {
                continue;
            }
            Channel channel = channels.get(type);
            writer.println(MHQXMLUtility.indentStr(indent++) +
                                 '<' + XML_REPORT + ' ' + XML_ATTR_TYPE + "=\"" + type.name() + "\">");
            for (String report : channel.lines) {
                // This cannot use the MHQXMLUtility as it cannot be escaped
                writer.println(MHQXMLUtility.indentStr(indent) + '<' + XML_LINE + "><![CDATA[" + report + "]]></" +
                                     XML_LINE + '>');
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, XML_REPORT);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, XML_ROOT);
    }

    /**
     * Loads channels from a {@value #XML_ROOT} element previously written by {@link #writeToXML(PrintWriter, int)},
     * then rebuilds the transient HTML caches and new-line deltas. Unknown or missing report types are ignored.
     *
     * @param rootNode the {@value #XML_ROOT} element
     */
    public void readFromXML(Node rootNode) {
        clear();

        NodeList reportNodes = rootNode.getChildNodes();
        for (int i = 0; i < reportNodes.getLength(); i++) {
            Node reportNode = reportNodes.item(i);
            if ((reportNode.getParentNode() != rootNode) || !reportNode.getNodeName().equalsIgnoreCase(XML_REPORT)) {
                continue;
            }

            Node typeAttribute = (reportNode.getAttributes() == null) ? null
                                       : reportNode.getAttributes().getNamedItem(XML_ATTR_TYPE);
            if (typeAttribute == null) {
                continue;
            }

            DailyReportType type;
            try {
                type = DailyReportType.valueOf(typeAttribute.getTextContent().trim());
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("Ignoring unknown daily report type: {}", typeAttribute.getTextContent());
                continue;
            }

            Channel channel = channels.get(type);
            NodeList lineNodes = reportNode.getChildNodes();
            for (int j = 0; j < lineNodes.getLength(); j++) {
                Node lineNode = lineNodes.item(j);
                if ((lineNode.getParentNode() == reportNode) && lineNode.getNodeName().equalsIgnoreCase(XML_LINE)) {
                    channel.lines.add(lineNode.getTextContent());
                }
            }
        }

        for (Channel channel : channels.values()) {
            channel.rebuildFromLines();
        }
    }
}
