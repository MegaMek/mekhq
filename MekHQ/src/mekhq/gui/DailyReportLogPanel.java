/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import java.awt.BorderLayout;
import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import megamek.codeUtilities.StringUtility;
import megamek.common.ui.FastJScrollPane;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.enums.DailyReportType;

/**
 * This is a panel for displaying the reporting log for each day. We are putting it into its own panel so that we can
 * later extend this to include chat and maybe break up the log into different sections.
 *
 * @author Jay Lawson
 */
public class DailyReportLogPanel extends JPanel {
    //region Variable Declarations
    private final CampaignGUI gui;
    final JScrollPane logPanel = new FastJScrollPane();
    private JTextPane txtLog;
    private String logText = "";
    //endregion Variable Declarations

    public DailyReportLogPanel(final CampaignGUI gui) {
        this.gui = gui;
        initialize();
    }

    //region Getters/Setters
    public CampaignGUI getGUI() {
        return gui;
    }

    public JTextPane getTxtLog() {
        return txtLog;
    }

    public void setTxtLog(final JTextPane txtLog) {
        this.txtLog = txtLog;
    }

    public String getLogText() {
        return logText;
    }

    public void setLogText(final String logText) {
        this.logText = logText;
    }
    //region Getters/Setters

    //region Initialization
    private void initialize() {
        setLayout(new BorderLayout());

        setTxtLog(new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        });
        getTxtLog().setContentType("text/html");
        getTxtLog().setEditable(false);
        ((DefaultCaret) getTxtLog().getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        getTxtLog().getAccessibleContext().setAccessibleName("Daily Log");
        getTxtLog().addHyperlinkListener(gui.getReportHLL());

        logPanel.setViewportView(getTxtLog());
        SwingUtilities.invokeLater(() -> logPanel.getVerticalScrollBar().setValue(0));
        logPanel.setBorder(new EmptyBorder(2, 5, 2, 2));
        add(logPanel, BorderLayout.CENTER);
    }
    //endregion Initialization

    public void clearLogPanel() {
        setLogText("");
        getTxtLog().setText("");
        SwingUtilities.invokeLater(() -> logPanel.getVerticalScrollBar().setValue(0));
    }

    public void refreshLog(final String text, DailyReportType type) {
        if (text.equals(getLogText())) {
            return;
        }

        setLogText(text);
        final Reader stringReader = new StringReader(getLogText());
        final HTMLEditorKit htmlKit = new HTMLEditorKit();
        final HTMLDocument blank = (HTMLDocument) htmlKit.createDefaultDocument();
        try {
            htmlKit.read(stringReader, blank, 0);
        } catch (Exception ignored) {

        }
        getTxtLog().setDocument(blank);
        getTxtLog().setCaretPosition(blank.getLength());

        // If there is only one line in the log, it means it's just the date header, so we don't want to alert the
        // player for no reason
        if (!isDateOnly(List.of(text))) {
            getGUI().checkDailyLogNag(type);
        }
        SwingUtilities.invokeLater(() -> logPanel.getVerticalScrollBar().setValue(0));
    }

    public void appendLog(final List<String> newReports, final DailyReportType type) {
        final String addedText = Utilities.combineString(newReports, "");
        if (StringUtility.isNullOrBlank(addedText)) {
            return;
        }

        if (getLogText().isBlank()) {
            refreshLog(addedText, type);
            return;
        }

        final HTMLDocument doc = (HTMLDocument) getTxtLog().getDocument();
        try {
            // Element 0 is <head>, Element 1 is <body>
            doc.insertBeforeEnd(doc.getDefaultRootElement().getElement(1).getElement(0), addedText);
            setLogText(getLogText() + addedText);
        } catch (Exception ignored) {

        }
        getTxtLog().setCaretPosition(doc.getLength());

        // We only want to nag the player if there is something of value. So no nag occurs if we're just adding the date
        if (!isDateOnly(newReports)) {
            getGUI().checkDailyLogNag(type);
        }
        SwingUtilities.invokeLater(() -> logPanel.getVerticalScrollBar().setValue(0));
    }

    /**
     * Checks whether the given list of report lines represents a single, date-only entry.
     *
     * <p>This method returns {@code true} only when all the following are true:</p>
     * <ul>
     *     <li>{@code reports} contains exactly one element,</li>
     *     <li>that element is wrapped in {@code <b>} and {@code </b>} tags, and</li>
     *     <li>the inner text parses successfully as a {@link LocalDate} using the user's configured date format.</li>
     * </ul>
     *
     * <p>If the list size is not exactly one, the element is not correctly formatted, or the inner text cannot be
     * parsed as a date, this method returns {@code false} without throwing an exception.</p>
     *
     * @param reports the list of report lines to inspect
     *
     * @return {@code true} if the list represents a single bolded, correctly formatted date-only entry; {@code false}
     *       otherwise
     *
     * @author Illiani
     * @since 0.50.11
     */
    public static boolean isDateOnly(List<String> reports) {
        boolean isDateOnly = false;
        if (reports.size() == 1) {
            // If parsing succeeds, it's a real report date
            try {
                String line = reports.get(0);
                String inner = line.substring(3, line.length() - 4); // strip <b> and </b>

                // Use the user's configured date format and locale to match what's displayed
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                      MekHQ.getMHQOptions().getLongDisplayDateFormat()
                ).withLocale(MekHQ.getMHQOptions().getDateLocale());

                LocalDate.parse(inner, formatter);
                isDateOnly = true;
            } catch (Exception ignored) {
                // Not a formatted date — do nothing
            }
        }
        return isDateOnly;
    }
}
