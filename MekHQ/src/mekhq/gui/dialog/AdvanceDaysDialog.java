/*
 * Copyright (c) 2014-2021 - The MegaMek Team. All Rights Reserved.
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

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.preferences.JIntNumberSpinnerPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.event.Subscribe;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.event.ReportEvent;
import mekhq.gui.CampaignGUI;
import mekhq.gui.DailyReportLogPanel;
import mekhq.gui.baseComponents.AbstractMHQDialog;

public class AdvanceDaysDialog extends AbstractMHQDialog {
    private static final MMLogger logger = MMLogger.create(AdvanceDaysDialog.class);

    // region Variable Declarations
    private final CampaignGUI gui;
    private boolean running;

    private JSpinner spnDays;
    private JButton btnStartAdvancement;
    private JButton btnNewDay;
    private JButton btnNewWeek;
    private JButton btnNewMonth;
    private JButton btnNewQuarter;
    private JButton btnNewYear;
    private JButton btnNewQuinquennial;
    private DailyReportLogPanel dailyLogPanel;
    // endregion Variable Declarations

    // region Constructors
    public AdvanceDaysDialog(final JFrame frame, final CampaignGUI gui) {
        super(frame, "AdvanceDaysDialog", "AdvanceDaysDialog.title");
        this.gui = gui;
        initialize();
    }
    // endregion Constructors

    // region Getters/Setters
    public CampaignGUI getGUI() {
        return gui;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(final boolean running) {
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

    public void setBtnStartAdvancement(final JButton btnStartAdvancement) {
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

    public JButton getBtnNewQuarter() {
        return btnNewQuarter;
    }

    public void setBtnNewQuarter(final JButton btnNewQuarter) {
        this.btnNewQuarter = btnNewQuarter;
    }

    public JButton getBtnNewYear() {
        return btnNewYear;
    }

    public void setBtnNewYear(final JButton btnNewYear) {
        this.btnNewYear = btnNewYear;
    }

    public JButton getBtnNewQuinquennial() {
        return btnNewQuinquennial;
    }

    public void setBtnNewQuinquennial(final JButton btnNewQuinquennial) {
        this.btnNewQuinquennial = btnNewQuinquennial;
    }

    public DailyReportLogPanel getDailyLogPanel() {
        return dailyLogPanel;
    }

    public void setDailyLogPanel(final DailyReportLogPanel dailyLogPanel) {
        this.dailyLogPanel = dailyLogPanel;
    }
    // endregion Getters/Setters

    // region Initialization
    @Override
    protected Container createCenterPane() {
        // Create Panel Components
        final JPanel advanceDaysDurationPanel = createDurationPanel();

        setDailyLogPanel(new DailyReportLogPanel(getGUI()));
        getDailyLogPanel().refreshLog(gui.getCommandCenterTab().getPanLog().getLogText());

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setName("advanceDaysPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(advanceDaysDurationPanel)
                        .addComponent(getDailyLogPanel()));

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(advanceDaysDurationPanel)
                        .addComponent(getDailyLogPanel()));

        return panel;
    }

    private JPanel createDurationPanel() {
        final JPanel panel = new JPanel(new GridLayout(0, 3));
        panel.setName("advanceDaysDurationPanel");

        // Maxing out at 100 years for dev testing reasons, which is cancellable by
        // pressing escape
        setSpnDays(new JSpinner(new SpinnerNumberModel(7, 1, 36525, 1)));
        getSpnDays().setToolTipText(resources.getString("spnDays.toolTipText"));
        getSpnDays().setName("spnDays");
        panel.add(getSpnDays());

        final JLabel lblDays = new JLabel(resources.getString("lblDays.text"));
        lblDays.setToolTipText(resources.getString("spnDays.toolTipText"));
        lblDays.setName("lblDays");
        lblDays.setLabelFor(getSpnDays());
        panel.add(lblDays);

        setBtnStartAdvancement(new MMButton("btnStartAdvancement", resources.getString("btnStartAdvancement.text"),
                resources.getString("btnStartAdvancement.toolTipText"), this::startAdvancement));
        panel.add(getBtnStartAdvancement());

        setBtnNewDay(new MMButton("btnNewDay", resources.getString("btnNewDay.text"),
                resources.getString("btnNewDay.toolTipText"), this::startAdvancement));
        panel.add(getBtnNewDay());

        setBtnNewWeek(new MMButton("btnNewWeek", resources.getString("btnNewWeek.text"),
                resources.getString("btnNewWeek.toolTipText"), this::startAdvancement));
        panel.add(getBtnNewWeek());

        setBtnNewMonth(new MMButton("btnNewMonth", resources.getString("btnNewMonth.text"),
                resources.getString("btnNewMonth.toolTipText"), this::startAdvancement));
        panel.add(getBtnNewMonth());

        setBtnNewQuarter(new MMButton("btnNewQuarter", resources.getString("btnNewQuarter.text"),
                resources.getString("btnNewQuarter.toolTipText"), this::startAdvancement));
        panel.add(getBtnNewQuarter());

        setBtnNewYear(new MMButton("btnNewYear", resources.getString("btnNewYear.text"),
                resources.getString("btnNewYear.toolTipText"), this::startAdvancement));
        panel.add(getBtnNewYear());

        setBtnNewQuinquennial(new MMButton("btnNewQuinquennial", resources.getString("btnNewQuinquennial.text"),
                resources.getString("btnNewQuinquennial.toolTipText"), this::startAdvancement));
        panel.add(getBtnNewQuinquennial());

        return panel;
    }

    @Override
    protected void finalizeInitialization() throws Exception {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent evt) {
                // We need to unregister here as unregistering in the actionPerformed method
                // will
                // lead to incorrect behaviour if the user tries to advance days again without
                // exiting this dialog
                MekHQ.unregisterHandler(this);
            }
        });
        super.finalizeInitialization();
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) throws Exception {
        super.setCustomPreferences(preferences);
        preferences.manage(new JIntNumberSpinnerPreference(getSpnDays()));
    }
    // endregion Initialization

    public void startAdvancement(final ActionEvent evt) {
        MekHQ.registerHandler(this);
        final LocalDate today = getGUI().getCampaign().getLocalDate();
        int days;

        if (getBtnStartAdvancement().equals(evt.getSource())) {
            days = (int) getSpnDays().getValue();
        } else if (getBtnNewDay().equals(evt.getSource())) {
            days = 1;
        } else if (getBtnNewWeek().equals(evt.getSource())) {
            // The number of days until the next monday is eight days (a week and a day)
            // minus
            // the current day of the week. The additional one is added to ensure we get the
            // next
            // Monday instead of the Sunday
            days = 8 - today.getDayOfWeek().getValue();
        } else if (getBtnNewMonth().equals(evt.getSource())) {
            // The number of days until the next month is the length of the month plus one
            // minus
            // the current day, with the one added because otherwise we get the last day of
            // the same
            // month
            days = today.lengthOfMonth() + 1 - today.getDayOfMonth();
        } else if (getBtnNewQuarter().equals(evt.getSource())) {
            days = Math.toIntExact(ChronoUnit.DAYS.between(today,
                    today.with(IsoFields.DAY_OF_QUARTER, 1).plusMonths(3)));
        } else if (getBtnNewYear().equals(evt.getSource())) {
            // The number of days until the next year is the length of the year plus one
            // minus
            // the current day, with the one added because otherwise we get the last day of
            // the same
            // year
            days = today.lengthOfYear() + 1 - today.getDayOfYear();
        } else if (getBtnNewQuinquennial().equals(evt.getSource())) {
            days = Math.toIntExact(ChronoUnit.DAYS.between(today,
                    LocalDate.ofYearDay(today.getYear() + 5 - (today.getYear() % 5), 1)));
        } else {
            logger.error("Unknown source to start advancing days. Advancing to tomorrow.");
            days = 1;
        }

        setRunning(true);
        boolean firstDay = true;
        final List<String> reports = new ArrayList<>();
        for (; days > 0; days--) {
            try {
                if (!getGUI().getCampaign().newDay()) {
                    break;
                }

                final String report = getGUI().getCampaign().getCurrentReportHTML();
                if (firstDay) {
                    getDailyLogPanel().refreshLog(report);
                    firstDay = false;
                } else {
                    reports.add("<hr>");
                    reports.add(report);
                }
                reports.addAll(getGUI().getCampaign().fetchAndClearNewReports());
            } catch (Exception ex) {
                logger.error("", ex);
                break;
            }
        }

        setRunning(false);
        getDailyLogPanel().appendLog(reports);

        // We couldn't advance all days for some reason,
        // set the spinner to the number of remaining days
        if (days > 0) {
            getSpnDays().setValue(days);
        }

        getGUI().refreshCalendar();
        getGUI().refreshLocation();
        getGUI().refreshAllTabs();
    }

    @Subscribe(priority = 1)
    public void reportOverride(final ReportEvent evt) {
        if (isRunning()) {
            evt.cancel();
        } else {
            getDailyLogPanel().refreshLog(getGUI().getCampaign().getCurrentReportHTML());
        }
    }
}
