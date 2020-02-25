/*
 * MekHqOptionsDialog.java
 *
 * Copyright (c) 2019 MekHQ Team. All rights reserved.
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

import megamek.common.logging.MMLogger;
import mekhq.MekHqConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class MekHqOptionsDialog extends BaseDialog {
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.MekHqOptionsDialog");
    private final Preferences userPreferences = Preferences.userRoot().node(MekHqConstants.AUTOSAVE_NODE);

    private JRadioButton optionNoSave;
    private JRadioButton optionSaveDaily;
    private JRadioButton optionSaveWeekly;
    private JRadioButton optionSaveMonthly;
    private JRadioButton optionSaveYearly;
    private JCheckBox checkSaveBeforeMissions;
    private JSpinner spinnerSavedGamesCount;

    public MekHqOptionsDialog(JFrame parent, MMLogger logger) {
        super(parent, logger);

        this.initialize(resources);
        this.setInitialState();
    }

    @Override
    protected Container createCustomUI() {
        // Create UI components
        JLabel labelSavedInfo = new JLabel(resources.getString("labelSavedInfo.text"));

        optionNoSave = new JRadioButton(resources.getString("optionNoSave.text"));
        optionNoSave.setMnemonic(KeyEvent.VK_N);

        optionSaveDaily = new JRadioButton(resources.getString("optionSaveDaily.text"));
        optionSaveDaily.setMnemonic(KeyEvent.VK_D);

        optionSaveWeekly = new JRadioButton(resources.getString("optionSaveWeekly.text"));
        optionSaveWeekly.setMnemonic(KeyEvent.VK_W);

        optionSaveMonthly = new JRadioButton(resources.getString("optionSaveMonthly.text"));
        optionSaveMonthly.setMnemonic(KeyEvent.VK_M);

        optionSaveYearly = new JRadioButton(resources.getString("optionSaveYearly.text"));
        optionSaveYearly.setMnemonic(KeyEvent.VK_Y);

        ButtonGroup saveFrequencyGroup = new ButtonGroup();
        saveFrequencyGroup.add(optionNoSave);
        saveFrequencyGroup.add(optionSaveDaily);
        saveFrequencyGroup.add(optionSaveWeekly);
        saveFrequencyGroup.add(optionSaveMonthly);
        saveFrequencyGroup.add(optionSaveYearly);

        checkSaveBeforeMissions = new JCheckBox(resources.getString("checkSaveBeforeMissions.text"));
        checkSaveBeforeMissions.setMnemonic(KeyEvent.VK_S);

        JLabel labelSavedGamesCount = new JLabel(resources.getString("labelSavedGamesCount.text"));
        spinnerSavedGamesCount = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        labelSavedGamesCount.setLabelFor(spinnerSavedGamesCount);

        // Layout the UI
        JPanel body = new JPanel();
        GroupLayout layout = new GroupLayout(body);
        body.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(labelSavedInfo)
                .addComponent(optionNoSave)
                .addComponent(optionSaveDaily)
                .addComponent(optionSaveWeekly)
                .addComponent(optionSaveMonthly)
                .addComponent(optionSaveYearly)
                .addComponent(checkSaveBeforeMissions)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(labelSavedGamesCount)
                    .addComponent(spinnerSavedGamesCount, GroupLayout.Alignment.TRAILING))
        );

        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(labelSavedInfo)
                .addComponent(optionNoSave)
                .addComponent(optionSaveDaily)
                .addComponent(optionSaveWeekly)
                .addComponent(optionSaveMonthly)
                .addComponent(optionSaveYearly)
                .addComponent(checkSaveBeforeMissions)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(labelSavedGamesCount)
                    .addComponent(spinnerSavedGamesCount))
        );

        return body;
    }

    @Override
    protected void okAction() {
        this.userPreferences.putBoolean(MekHqConstants.NO_SAVE_KEY, this.optionNoSave.isSelected());
        this.userPreferences.putBoolean(MekHqConstants.SAVE_DAILY_KEY, this.optionSaveDaily.isSelected());
        this.userPreferences.putBoolean(MekHqConstants.SAVE_WEEKLY_KEY, this.optionSaveWeekly.isSelected());
        this.userPreferences.putBoolean(MekHqConstants.SAVE_MONTHLY_KEY, this.optionSaveMonthly.isSelected());
        this.userPreferences.putBoolean(MekHqConstants.SAVE_YEARLY_KEY, this.optionSaveYearly.isSelected());
        this.userPreferences.putBoolean(MekHqConstants.SAVE_BEFORE_MISSIONS_KEY, this.checkSaveBeforeMissions.isSelected());
        this.userPreferences.putInt(MekHqConstants.MAXIMUM_NUMBER_SAVES_KEY, (Integer)this.spinnerSavedGamesCount.getValue());
    }

    private void setInitialState() {
        this.optionNoSave.setSelected(this.userPreferences.getBoolean(MekHqConstants.NO_SAVE_KEY, false));
        this.optionSaveDaily.setSelected(this.userPreferences.getBoolean(MekHqConstants.SAVE_DAILY_KEY, false));
        this.optionSaveWeekly.setSelected(this.userPreferences.getBoolean(MekHqConstants.SAVE_WEEKLY_KEY, true));
        this.optionSaveMonthly.setSelected(this.userPreferences.getBoolean(MekHqConstants.SAVE_MONTHLY_KEY, false));
        this.optionSaveYearly.setSelected(this.userPreferences.getBoolean(MekHqConstants.SAVE_YEARLY_KEY, false));
        this.checkSaveBeforeMissions.setSelected(this.userPreferences.getBoolean(MekHqConstants.SAVE_BEFORE_MISSIONS_KEY, false));
        this.spinnerSavedGamesCount.setValue(this.userPreferences.getInt(MekHqConstants.MAXIMUM_NUMBER_SAVES_KEY, MekHqConstants.DEFAULT_NUMBER_SAVES));
    }
}
