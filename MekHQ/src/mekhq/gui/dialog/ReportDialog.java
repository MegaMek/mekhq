/*
 * Copyright (c) 2013-2020 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;

import mekhq.MekHQ;
import mekhq.campaign.report.Report;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;

/**
 * @author Jay Lawson
 */
public class ReportDialog extends JDialog {
    //region Variable Declarations
    private static final long serialVersionUID = 3624327778807359294L;

    private JTextPane txtReport;
    //endregion Variable Declarations

    public ReportDialog(Frame parent, Report report) {
        super(parent, false);
        setTitle(report.getTitle());
        txtReport = report.getReport();
        initComponents();
        pack();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JScrollPane scrReport = new JScrollPane(txtReport);
        txtReport.setEditable(false);
        scrReport.setBorder( new EmptyBorder(2,10,2,2));

        getContentPane().add(scrReport, BorderLayout.CENTER);
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(ReportDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }
}
