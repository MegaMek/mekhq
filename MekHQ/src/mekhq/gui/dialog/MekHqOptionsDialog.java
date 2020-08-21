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

import mekhq.MekHQ;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

/**
 * This dialog is used for MekHQ Options.
 * Note: If you add any options to this you will also need to add the preference initialization in
 * MekHQ Options.
 */
public class MekHqOptionsDialog extends BaseDialog {
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.MekHqOptionsDialog");

    private JRadioButton optionNoSave;
    private JRadioButton optionSaveDaily;
    private JRadioButton optionSaveWeekly;
    private JRadioButton optionSaveMonthly;
    private JRadioButton optionSaveYearly;
    private JCheckBox checkSaveBeforeMissions;
    private JSpinner spinnerSavedGamesCount;

    public MekHqOptionsDialog(JFrame parent) {
        super(parent);

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
        MekHQ.getMekHQOptions().setNoAutosaveValue(optionNoSave.isSelected());
        MekHQ.getMekHQOptions().setAutosaveDailyValue(optionSaveDaily.isSelected());
        MekHQ.getMekHQOptions().setAutosaveWeeklyValue(optionSaveWeekly.isSelected());
        MekHQ.getMekHQOptions().setAutosaveMonthlyValue(optionSaveMonthly.isSelected());
        MekHQ.getMekHQOptions().setAutosaveYearlyValue(optionSaveYearly.isSelected());
        MekHQ.getMekHQOptions().setAutosaveBeforeMissionsValue(checkSaveBeforeMissions.isSelected());
        MekHQ.getMekHQOptions().setMaximumNumberOfAutosavesValue((Integer) spinnerSavedGamesCount.getValue());
    }

    private void setInitialState() {
        optionNoSave.setSelected(MekHQ.getMekHQOptions().getNoAutosaveValue());
        optionSaveDaily.setSelected(MekHQ.getMekHQOptions().getAutosaveDailyValue());
        optionSaveWeekly.setSelected(MekHQ.getMekHQOptions().getAutosaveWeeklyValue());
        optionSaveMonthly.setSelected(MekHQ.getMekHQOptions().getAutosaveMonthlyValue());
        optionSaveYearly.setSelected(MekHQ.getMekHQOptions().getAutosaveYearlyValue());
        checkSaveBeforeMissions.setSelected(MekHQ.getMekHQOptions().getAutosaveBeforeMissionsValue());
        spinnerSavedGamesCount.setValue(MekHQ.getMekHQOptions().getMaximumNumberOfAutosavesValue());
    }
}
