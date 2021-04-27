/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.preferences.JIntNumberSpinnerPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.event.ReportEvent;
import mekhq.gui.CampaignGUI;
import mekhq.gui.DailyReportLogPanel;
import mekhq.gui.baseComponents.AbstractMHQDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdvanceDaysDialog extends AbstractMHQDialog implements ActionListener {
    //region Variable Declarations
    private final CampaignGUI gui;
    private AtomicBoolean running;
    private LocalDate tomorrow;
    private LocalDate nextMonday;
    private LocalDate nextMonth;
    private LocalDate nextYear;
    private LocalDate nextDecade;

    private JSpinner spnDays;
    private JButton btnStartAdvancement;
    private JButton btnNewDay;
    private JButton btnNewWeek;
    private JButton btnNewMonth;
    private JButton btnNewYear;
    private JButton btnNewDecade;
    private DailyReportLogPanel dailyLogPanel;
    //endregion Variable Declarations

    //region Constructors
    public AdvanceDaysDialog(final JFrame frame, final CampaignGUI gui) {
        super(frame, "AdvanceDaysDialog", "AdvanceDaysDialog.title");
        this.gui = gui;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public CampaignGUI getGUI() {
        return gui;
    }

    public AtomicBoolean getRunning() {
        return running;
    }

    public void setRunning(final AtomicBoolean running) {
        this.running = running;
    }

    public JSpinner getSpnDays() {
        return spnDays;
    }

    public void setSpnDays(final JSpinner spnDays) {
        this.spnDays = spnDays;
    }

    public JButton getBtnStartAdvancement() {
        return btnStartAdvancement;
    }

    public void setBtnStartAdvancement(JButton btnStartAdvancement) {
        this.btnStartAdvancement = btnStartAdvancement;
    }

    public JButton getBtnNewDay() {
        return btnNewDay;
    }

    public void setBtnNewDay(final JButton btnNewDay) {
        this.btnNewDay = btnNewDay;
    }

    public JButton getBtnNewWeek() {
        return btnNewWeek;
    }

    public void setBtnNewWeek(final JButton btnNewWeek) {
        this.btnNewWeek = btnNewWeek;
    }

    public JButton getBtnNewMonth() {
        return btnNewMonth;
    }

    public void setBtnNewMonth(final JButton btnNewMonth) {
        this.btnNewMonth = btnNewMonth;
    }

    public JButton getBtnNewYear() {
        return btnNewYear;
    }

    public void setBtnNewYear(final JButton btnNewYear) {
        this.btnNewYear = btnNewYear;
    }

    public JButton getBtnNewDecade() {
        return btnNewDecade;
    }

    public void setBtnNewDecade(final JButton btnNewDecade) {
        this.btnNewDecade = btnNewDecade;
    }

    public DailyReportLogPanel getDailyLogPanel() {
        return dailyLogPanel;
    }

    public void setDailyLogPanel(final DailyReportLogPanel dailyLogPanel) {
        this.dailyLogPanel = dailyLogPanel;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        // Create Panel Components
        // Maxing out at 100 years for dev testing reasons, which is cancellable by pressing escape
        setSpnDays(new JSpinner(new SpinnerNumberModel(7, 1, 36525, 1)));
        getSpnDays().setName("spnDays");

        final JLabel lblDays = new JLabel(resources.getString("lblDays.text"));
        lblDays.setName("lblDays");
        lblDays.setLabelFor(getSpnDays());

        setBtnStartAdvancement(new MMButton("btnStartAdvancement", resources.getString("btnStartAdvancement.text"),
                String.format(resources.getString("btnStartAdvancement.toolTipText"),
                        MekHQ.getMekHQOptions().getDisplayFormattedDate(getGUI().getCampaign().getLocalDate())),
                this::validateButtonActionPerformed));

        setBtnNewDay(new MMButton("btnNewDay", resources.getString("btnNewDay.text"),
                String.format(resources.getString("advanceToDay.toolTipText"),
                        MekHQ.getMekHQOptions().getDisplayFormattedDate(getGUI().getCampaign().getLocalDate())),
                this::validateButtonActionPerformed));

        setBtnNewWeek(new MMButton("btnNewWeek", resources.getString("btnNewWeek.text"),
                String.format(resources.getString("advanceToDay.toolTipText"),
                        MekHQ.getMekHQOptions().getDisplayFormattedDate(getGUI().getCampaign().getLocalDate())),
                this::validateButtonActionPerformed));

        setBtnNewMonth(new MMButton("btnNewMonth", resources.getString("btnNewMonth.text"),
                String.format(resources.getString("advanceToDay.toolTipText"),
                        MekHQ.getMekHQOptions().getDisplayFormattedDate(getGUI().getCampaign().getLocalDate())),
                this::validateButtonActionPerformed));

        setBtnNewYear(new MMButton("btnNewYear", resources.getString("btnNewYear.text"),
                String.format(resources.getString("advanceToDay.toolTipText"),
                        MekHQ.getMekHQOptions().getDisplayFormattedDate(getGUI().getCampaign().getLocalDate())),
                this::validateButtonActionPerformed));

        setBtnNewDecade(new MMButton("btnNewDecade", resources.getString("btnNewDecade.text"),
                String.format(resources.getString("advanceToDay.toolTipText"),
                        MekHQ.getMekHQOptions().getDisplayFormattedDate(getGUI().getCampaign().getLocalDate())),
                this::validateButtonActionPerformed));

        setDailyLogPanel(new DailyReportLogPanel(getGUI()));

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setName("advanceDaysPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(getSpnDays())
                                .addComponent(lblDays)
                                .addComponent(getBtnStartAdvancement(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(btnst)
                                .addComponent(lblChanceProcreationNoRelationship)
                                .addComponent(spnChanceProcreationNoRelationship, GroupLayout.Alignment.LEADING))
                        .addComponent(chkDisplayTrueDueDate)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblChanceProcreation)
                                .addComponent(spnChanceProcreation))
                        .addComponent(chkUseProcreationNoRelationship)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblChanceProcreationNoRelationship)
                                .addComponent(spnChanceProcreationNoRelationship))
                        .addComponent(chkDisplayTrueDueDate)
                        .addComponent(chkLogConception)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblBabySurnameStyle)
                                .addComponent(comboBabySurnameStyle))
                        .addComponent(chkDetermineFatherAtBirth)
        );

        return panel;
    }

    @Override
    protected void finalizeInitialization() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                // We need to unregister here as unregistering in the actionPerformed method will
                // lead to incorrect behaviour if the user tries to advance days again without
                // exiting this dialog
                MekHQ.unregisterHandler(this);
            }
        });
        super.finalizeInitialization();
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) {
        super.setCustomPreferences(preferences);
        preferences.manage(new JIntNumberSpinnerPreference(getSpnDays()));
    }
    //endregion Initialization

    //region Button Actions
    @Override
    protected void cancelActionPerformed(final ActionEvent evt) {

    }
    //endregion Button Actions

    //region ActionListener
    @Override
    public void actionPerformed(final ActionEvent evt) {
        if (evt.getSource().equals(btnStart) || evt.getSource().equals(btnNextWeek)
                || evt.getSource().equals(btnNextMonth) || evt.getSource().equals(btnNextYear)) {
            int days = (int) spnDays.getValue();
            boolean firstDay = true;
            MekHQ.registerHandler(this);

            LocalDate today = gui.getCampaign().getLocalDate();
            if (btnNextWeek.equals(evt.getSource())) {
                // The number of days till the next monday is eight days (a week and a day) minus
                // the current day of the week. The additional one is added to ensure we get the next
                // Monday instead of the Sunday
                days = 8 - today.getDayOfWeek().getValue();
            } else if (btnNextMonth.equals(evt.getSource())) {
                // The number of days till the next month is the length of the month plus one minus
                // the current day, with the one added because otherwise we get the last day of the same
                // month
                days = today.lengthOfMonth() + 1 - today.getDayOfMonth();
            } else if (btnNextYear.equals(evt.getSource())) {
                // The number of days till the next year is the length of the year plus one minus
                // the current day, with the one added because otherwise we get the last day of the same
                // year
                days = today.lengthOfYear() + 1 - today.getDayOfYear();
            }

            List<String> reports = new ArrayList<>();
            for (; days > 0; days--) {
                if (!getRunning().get()) {
                    break;
                } else if (!getGUI().getCampaign().newDay()) {
                    break;
                }
                final String report = gui.getCampaign().getCurrentReportHTML();
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
    //endregion ActionListener

    @Subscribe(priority = 1)
    public void reportOverride(ReportEvent evt) {
        evt.cancel();
    }
}
