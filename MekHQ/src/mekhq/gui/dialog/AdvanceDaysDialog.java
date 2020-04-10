/*
 * Copyright (c) 2014 - The MegaMek Team. All rights reserved.
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
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.common.event.Subscribe;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.event.ReportEvent;
import mekhq.gui.CampaignGUI;
import mekhq.gui.DailyReportLogPanel;
import mekhq.gui.ReportHyperlinkListener;
import mekhq.gui.preferences.JIntNumberSpinnerPreference;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

/**
 * @author Dylan Myers <ralgith@gmail.com>
 */
public class AdvanceDaysDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;

    private ResourceBundle resourceMap;

    private JSpinner spnDays;
    private JButton btnStart;
    private JButton btnNextMonth;
    private JLabel lblDays;
    private JPanel pnlNumDays;
    private DailyReportLogPanel logPanel;
    private CampaignGUI gui;
    private ReportHyperlinkListener listener;

    public AdvanceDaysDialog(Frame owner, CampaignGUI gui, ReportHyperlinkListener listener) {
        super(owner, true);
        this.gui = gui;
        this.listener = listener;
        setName("formADD"); // NOI18N
        getContentPane().setLayout(new java.awt.GridBagLayout());
        this.setPreferredSize(new Dimension(500,500));
        initComponents();
        setLocationRelativeTo(owner);
        setUserPreferences();
    }

    public void initComponents() {
        setLayout(new BorderLayout());

        resourceMap = ResourceBundle.getBundle("mekhq.resources.AdvanceDaysDialog", new EncodeControl()); //$NON-NLS-1$

        this.setTitle(resourceMap.getString("dlgTitle.text"));
        lblDays = new JLabel(resourceMap.getString("dlgDays.text"));
        pnlNumDays = new JPanel();
        spnDays = new JSpinner(new SpinnerNumberModel(7, 1, 365, 1));
        ((JSpinner.DefaultEditor)spnDays.getEditor()).getTextField().setEditable(true);
        pnlNumDays.add(spnDays);
        pnlNumDays.add(lblDays);
        btnStart = new JButton(resourceMap.getString("dlgStartAdvancement.text"));
        btnStart.addActionListener(this);
        btnNextMonth = new JButton(resourceMap.getString("dlgAdvanceNextMonth.text"));
        btnNextMonth.addActionListener(this);
        pnlNumDays.add(btnStart);
        pnlNumDays.add(btnNextMonth);
        getContentPane().add(pnlNumDays, BorderLayout.NORTH);

        logPanel = new DailyReportLogPanel(listener);
        getContentPane().add(logPanel, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // We need to unregister here as unregistering in the actionPerformed
                // method will lead to incorrect behaviour if the user tries to advance
                // days again without exiting this dialog
                MekHQ.unregisterHandler(this);
            }
        });
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(AdvanceDaysDialog.class);

        spnDays.setName("numberDays");
        preferences.manage(new JIntNumberSpinnerPreference(spnDays));

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource().equals(btnStart) || event.getSource().equals(btnNextMonth)) {
            int days = (int) spnDays.getValue();
            boolean firstDay = true;
            MekHQ.registerHandler(this);
            if (event.getSource().equals(btnNextMonth)) {
                LocalDate today = gui.getCampaign().getLocalDate();
                // The number of days till the next month is the length of the month plus one minus
                // the current day, with the one added because otherwise we get the last day of the same
                // month
                days = today.lengthOfMonth() + 1 - today.getDayOfMonth();
            }

            int numDays;
            for (numDays = days; numDays > 0; numDays--) {
                if (gui.getCampaign().checkOverDueLoans()
                        || gui.nagShortMaintenance()
                        || (gui.getCampaign().getCampaignOptions().getUseAtB())
                        && (gui.nagShortDeployments() || gui.nagOutstandingScenarios())) {
                    break;
                }
                if (gui.getCampaign().checkRetirementDefections()
                        || gui.getCampaign().checkYearlyRetirements()) {
                    gui.showRetirementDefectionDialog();
                    break;
                }
                if (!gui.getCampaign().newDay()) {
                    break;
                }
                //String newLogString = logPanel.getLogText();
                //newLogString = newLogString.concat(gui.getCampaign().getCurrentReportHTML());
                if (firstDay) {
                    logPanel.refreshLog(gui.getCampaign().getCurrentReportHTML());
                    firstDay = false;
                } else {
                    logPanel.appendLog(Collections.singletonList("<hr/>")); //$NON-NLS-1$
                    logPanel.appendLog(gui.getCampaign().fetchAndClearNewReports());
                }
            }

            // We couldn't advance all days for some reason,
            // set the spinner to the number of remaining days
            if (numDays > 0) {
                this.spnDays.setValue(numDays);
            }

            gui.refreshCalendar();
            gui.refreshLocation();
            gui.initReport();

            gui.refreshAllTabs();
        }
    }

    @Subscribe(priority = 1)
    public void reportOverride(ReportEvent ev) {
        ev.cancel();
    }
}
