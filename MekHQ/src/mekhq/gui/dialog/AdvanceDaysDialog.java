/*
 * Copyright (c) 2014, 2020 - The MegaMek Team. All Rights Reserved.
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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

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
import mekhq.gui.preferences.JIntNumberSpinnerPreference;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

/**
 * @author Dylan Myers <ralgith@gmail.com>
 */
public class AdvanceDaysDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;

    private JSpinner spnDays;
    private JButton btnStart;
    private JButton btnNextWeek;
    private JButton btnNextMonth;
    private JButton btnNextYear;
    private DailyReportLogPanel logPanel;
    private CampaignGUI gui;

    public AdvanceDaysDialog(Frame owner, CampaignGUI gui) {
        super(owner, true);
        this.gui = gui;
        setName("formADD"); // NOI18N
        getContentPane().setLayout(new GridBagLayout());
        initComponents();
        this.setPreferredSize(new Dimension(500,500));
        setLocationRelativeTo(owner);
        setUserPreferences();
    }

    public void initComponents() {
        setLayout(new BorderLayout());

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AdvanceDaysDialog", new EncodeControl()); //$NON-NLS-1$

        this.setTitle(resourceMap.getString("dlgTitle.text"));
        JPanel pnlNumDays = new JPanel();

        // Maxing out at 10 years for dev testing reasons
        spnDays = new JSpinner(new SpinnerNumberModel(7, 1, 3650, 1));
        ((JSpinner.DefaultEditor) spnDays.getEditor()).getTextField().setEditable(true);
        pnlNumDays.add(spnDays);

        JLabel lblDays = new JLabel(resourceMap.getString("dlgDays.text"));
        pnlNumDays.add(lblDays);

        btnStart = new JButton(resourceMap.getString("dlgStartAdvancement.text"));
        btnStart.addActionListener(this);
        pnlNumDays.add(btnStart);

        btnNextWeek = new JButton(resourceMap.getString("dlgAdvanceNextWeek.text"));
        btnNextWeek.addActionListener(this);
        pnlNumDays.add(btnNextWeek);

        btnNextMonth = new JButton(resourceMap.getString("dlgAdvanceNextMonth.text"));
        btnNextMonth.addActionListener(this);
        pnlNumDays.add(btnNextMonth);

        btnNextYear = new JButton(resourceMap.getString("dlgAdvanceNextYear.text"));
        btnNextYear.addActionListener(this);
        pnlNumDays.add(btnNextYear);

        getContentPane().add(pnlNumDays, BorderLayout.NORTH);

        logPanel = new DailyReportLogPanel(gui);
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
        if (event.getSource().equals(btnStart) || event.getSource().equals(btnNextWeek)
                || event.getSource().equals(btnNextMonth) || event.getSource().equals(btnNextYear)) {
            int days = (int) spnDays.getValue();
            boolean firstDay = true;
            MekHQ.registerHandler(this);

            LocalDate today = gui.getCampaign().getLocalDate();
            if (btnNextWeek.equals(event.getSource())) {
                // The number of days till the next monday is eight days (a week and a day) minus
                // the current day of the week. The additional one is added to ensure we get the next
                // Monday instead of the Sunday
                days = 8 - today.getDayOfWeek().getValue();
            } else if (btnNextMonth.equals(event.getSource())) {
                // The number of days till the next month is the length of the month plus one minus
                // the current day, with the one added because otherwise we get the last day of the same
                // month
                days = today.lengthOfMonth() + 1 - today.getDayOfMonth();
            } else if (btnNextYear.equals(event.getSource())) {
                // The number of days till the next year is the length of the year plus one minus
                // the current day, with the one added because otherwise we get the last day of the same
                // year
                days = today.lengthOfYear() + 1 - today.getDayOfYear();
            }

            List<String> reports = new ArrayList<>();
            for (; days > 0; days--) {
                if (gui.getCampaign().checkOverDueLoans()
                        || gui.nagShortMaintenance()
                        || (gui.getCampaign().getCampaignOptions().getUseAtB())
                        && (gui.nagShortDeployments() || gui.nagOutstandingScenarios())) {
                    break;
                } else if (gui.getCampaign().checkRetirementDefections()
                        || gui.getCampaign().checkYearlyRetirements()) {
                    gui.showRetirementDefectionDialog();
                    break;
                } else if (!gui.getCampaign().newDay()) {
                    break;
                }
                String report = gui.getCampaign().getCurrentReportHTML();
                if (firstDay) {
                    logPanel.refreshLog(report);
                    firstDay = false;
                } else {
                    reports.add("<hr/>");
                    reports.add(report);
                }
                gui.getCampaign().fetchAndClearNewReports();
            }
            logPanel.appendLog(reports);

            // We couldn't advance all days for some reason,
            // set the spinner to the number of remaining days
            if (days > 0) {
                this.spnDays.setValue(days);
            }

            gui.refreshCalendar();
            gui.refreshLocation();

            gui.refreshAllTabs();
        }
    }

    @Subscribe(priority = 1)
    public void reportOverride(ReportEvent ev) {
        ev.cancel();
    }
}
