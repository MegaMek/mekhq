/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.log.LogEntry;
import mekhq.gui.CampaignGUI;
import mekhq.gui.DailyReportLogPanel;

public class HistoricalDailyReportDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(HistoricalDailyReportDialog.class);

    private final CampaignGUI gui;
    private JComboBox<Integer> pickTime;
    private DailyReportLogPanel logPanel;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle(
          "mekhq.resources.HistoricalDailyReportDialog",
          MekHQ.getMHQOptions().getLocale());

    /**
     * HistoricalDailyReportDialog - opens a dialog that shows a history of the daily log
     *
     * @param frame the JFrame
     * @param gui   the CampaignGUI object
     */
    public HistoricalDailyReportDialog(final JFrame frame, final CampaignGUI gui) {
        super(frame, true);
        this.gui = gui;
        this.setPreferredSize(new Dimension(650, 500));
        initComponents();

        setLocationRelativeTo(frame);
        setUserPreferences();
    }

    private void initComponents() {
        setTitle(resourceMap.getString("title.text"));

        getContentPane().setLayout(new GridBagLayout());

        if (MekHQ.getMHQOptions().getHistoricalDailyLog()) {
            JLabel pickTimeLabel = new JLabel(resourceMap.getString("pickTime.text"));
            Integer[] days = new Integer[] { 7, 30, 60, 90, MHQConstants.MAX_HISTORICAL_LOG_DAYS };
            pickTime = new JComboBox<>(days);
            logPanel = new DailyReportLogPanel(gui);
            JLabel daysLabel = new JLabel(resourceMap.getString("days.text"));
            JPanel filterPanel = new JPanel();
            JButton closeBtn = new JButton(resourceMap.getString("closeBtn.text"));
            JLabel cacheInfoLabel = new JLabel(resourceMap.getString("cachedInformationMessage.text"));

            updateLogPanel((Integer) pickTime.getSelectedItem());

            pickTime.addActionListener(event -> updateLogPanel((Integer) pickTime.getSelectedItem()));

            closeBtn.addActionListener(event -> setVisible(false));

            filterPanel.add(pickTimeLabel);
            filterPanel.add(pickTime);
            filterPanel.add(daysLabel);

            GridBagConstraints gridBag = new GridBagConstraints();
            gridBag.fill = GridBagConstraints.HORIZONTAL;
            gridBag.anchor = GridBagConstraints.NORTHWEST;
            gridBag.gridx = 0;
            gridBag.gridy = 0;
            gridBag.insets = new Insets(15, 15, 15, 15); // add some spacing for readability
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

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(HistoricalDailyReportDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    private void updateLogPanel(Integer days) {
        logPanel.clearLogPanel();
        LocalDate trackDay = null;
        for (LogEntry log : gui.getCampaign().inMemoryLogHistory) {
            if (ChronoUnit.DAYS.between(log.getDate(), gui.getCampaign().getLocalDate()) < days) {
                if (!log.getDate().equals(trackDay)) {
                    logPanel.appendLog(Collections.singletonList("<hr>"));
                    logPanel.appendLog(Collections.singletonList("<b>" +
                                                                       MekHQ.getMHQOptions()
                                                                             .getDisplayFormattedDate(log.getDate()) +
                                                                       "</b>"));
                    logPanel.appendLog(Collections.singletonList("<br><br>"));
                    trackDay = log.getDate();
                }
                logPanel.appendLog(Collections.singletonList(log.getDesc() + "<br>"));
            }
        }
    }
}
