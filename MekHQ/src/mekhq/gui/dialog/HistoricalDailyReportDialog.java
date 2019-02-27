/*
 * HistoricalDailyReportDialog.java
 *
 * Copyright (c) 2018
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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.log.LogEntry;
import mekhq.gui.CampaignGUI;
import mekhq.gui.DailyReportLogPanel;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;


public class HistoricalDailyReportDialog extends JDialog {
    private static final long serialVersionUID = -4373796917722483042L;

    // max number of days that will be stored in the history, also used as a limit in the UI
    public static final int MAX_DAYS_HISTORY = 120;

    private ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.HistoricalDailyReportDialog", new EncodeControl()); //$NON-NLS-1$

    private CampaignGUI gui;
    private JPanel filterPanel;
    private JLabel pickTimeLabel;
    private JComboBox<Integer> pickTime;
    private JLabel daysLabel;
    private DailyReportLogPanel logPanel;
    private JButton closeBtn;
    private JLabel cacheInfoLabel;
    private DateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d yyyy");

    /**
     * HistoricalDailyReportDialog - opens a dialog that shows a history of the daily log
     * @param owner - the Frame owner
     * @param gui - a CampaignGUI object
     */
    public HistoricalDailyReportDialog (Frame owner, CampaignGUI gui) {
        super(owner, true);
        this.gui = gui;
        this.setPreferredSize(new Dimension(650,500));
        initComponents();

        setLocationRelativeTo(owner);
        setUserPreferences();
    }

    private void initComponents() {
        setTitle(resourceMap.getString("title.text"));

        getContentPane().setLayout(new GridBagLayout());

        if (gui.getCampaign().getCampaignOptions().historicalDailyLog()) {
            pickTimeLabel = new JLabel(resourceMap.getString("pickTime.text"));
            Integer[] days = new Integer[] {7, 30, 60, 90, MAX_DAYS_HISTORY};
            pickTime = new JComboBox<>(days);
            logPanel = new DailyReportLogPanel(null);
            daysLabel = new JLabel(resourceMap.getString("days.text"));
            filterPanel = new JPanel();
            closeBtn = new JButton(resourceMap.getString("closeBtn.text"));
            cacheInfoLabel = new JLabel(resourceMap.getString("cachedInformationMessage.text"));

            updateLogPanel((Integer)pickTime.getSelectedItem());

            pickTime.addActionListener(event -> updateLogPanel((Integer)pickTime.getSelectedItem()));

            closeBtn.addActionListener(event -> setVisible(false));

            filterPanel.add(pickTimeLabel);
            filterPanel.add(pickTime);
            filterPanel.add(daysLabel);

            GridBagConstraints gridBag = new GridBagConstraints();
            gridBag.fill = GridBagConstraints.HORIZONTAL;
            gridBag.anchor = GridBagConstraints.NORTHWEST;
            gridBag.gridx = 0;
            gridBag.gridy = 0;
            gridBag.insets = new Insets(15,15,15,15); //add some spacing for readability
            getContentPane().add(cacheInfoLabel, gridBag);

            gridBag = new GridBagConstraints();
            gridBag.fill = GridBagConstraints.HORIZONTAL;
            gridBag.gridx = 0;
            gridBag.gridy = 1;
            getContentPane().add(filterPanel, gridBag);

            gridBag = new GridBagConstraints();
            gridBag.fill = GridBagConstraints.BOTH;
            gridBag.gridx = 0;
            gridBag.gridy = 2;
            gridBag.weightx = 1.0;
            gridBag.weighty = 1.0;
            getContentPane().add(logPanel, gridBag);

            gridBag = new GridBagConstraints();
            gridBag.fill = GridBagConstraints.HORIZONTAL;
            gridBag.anchor = GridBagConstraints.PAGE_END;
            gridBag.gridx = 0;
            gridBag.gridy = 2;
            getContentPane().add(closeBtn, gridBag);
        } else {
            GridBagConstraints gridBag = new GridBagConstraints();
            gridBag.fill = GridBagConstraints.HORIZONTAL;
            gridBag.anchor = GridBagConstraints.NORTHWEST;
            gridBag.gridx = 0;
            gridBag.gridy = 0;
            JLabel notice = new JLabel(resourceMap.getString("enableInCampaignOptions.text"));
            getContentPane().add(notice, gridBag);
        }
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(HistoricalDailyReportDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void updateLogPanel(Integer days) {
        logPanel.clearLogPanel();
        Date trackDay = null;
        long currentTime = gui.getCampaign().getDate().getTime();
        for (LogEntry log : gui.getCampaign().inMemoryLogHistory) {
            long diff = currentTime - log.getDate().getTime();
            if (TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) < days) {
                if (!log.getDate().equals(trackDay)) {
                    logPanel.appendLog(Collections.singletonList("<hr>"));
                    logPanel.appendLog(Collections.singletonList("<b>"+dateFormat.format(log.getDate())+"</b>"));
                    logPanel.appendLog(Collections.singletonList("<br><br>"));
                    trackDay = log.getDate();
                }
                logPanel.appendLog(Collections.singletonList(log.getDesc()+"<br>"));
            }
        }
    }
}

