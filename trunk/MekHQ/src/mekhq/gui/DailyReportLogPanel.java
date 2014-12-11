/*
 * ReportLogPanel.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui;


import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

/**
 * This is a panel for displaying the reporting log for each day. We are putting it into
 * its own panel so that we can later extend this to include chat and maybe break up the log
 * into different sections
 *
 * @author Jay Lawson
 *
 */
public class DailyReportLogPanel extends JPanel {

    /**
     *
     */
    private static final long serialVersionUID = -6512675362473724385L;
    JTextPane txtLog;
    String logText = new String();

    public DailyReportLogPanel(ReportHyperlinkListener listener) {
        txtLog = new JTextPane() {
            /**
             *
             */
            private static final long serialVersionUID = 9000659006965230883L;

            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        txtLog.addHyperlinkListener(listener);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Daily Log"));
        txtLog.setContentType("text/html"); // NOI18N
        txtLog.setEditable(false);
        DefaultCaret caret = (DefaultCaret)txtLog.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrLog = new JScrollPane(txtLog);
        scrLog.setBorder(new EmptyBorder(2,10,2,2));
        add(scrLog, BorderLayout.CENTER);
    }

    public void refreshLog(String s) {
        logText = s;
        txtLog.setText(logText);
    }

    public String getLogText() {
        return logText;
    }
}