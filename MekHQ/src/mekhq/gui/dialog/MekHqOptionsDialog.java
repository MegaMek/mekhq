/*
 * MekHqOptionsDialog.java
 *
 * Copyright (c) 2019 - The MekHQ Team. All Rights Reserved.
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

import mekhq.MekHqConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class MekHqOptionsDialog extends BaseDialog {
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.MekHqOptionsDialog");
    private final Preferences userPreferences = Preferences.userRoot();

    //region Autosave
    private JRadioButton optionNoSave;
    private JRadioButton optionSaveDaily;
    private JRadioButton optionSaveWeekly;
    private JRadioButton optionSaveMonthly;
    private JRadioButton optionSaveYearly;
    private JCheckBox checkSaveBeforeMissions;
    private JSpinner spinnerSavedGamesCount;
    //endregion Autosave

    //region Campaign XML Save
    private JCheckBox optionWriteCustomsToXML;
    //endregion Campaign XML Save

    public MekHqOptionsDialog(JFrame parent) {
        super(parent);

        this.initialize(resources);
        this.setInitialState();
    }

    /**
     * This dialog uses the following Mnemonics:
     * C, D, M, M, S, W, Y
     */
    @Override
    protected Container createCustomUI() {
        //region Create UI components
        //region Autosave
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
        //endregion Autosave

        //region Campaign XML Save
        JLabel labelXMLSave = new JLabel(resources.getString("labelXMLSave.text"));

        optionWriteCustomsToXML = new JCheckBox(resources.getString("optionWriteCustomsToXML.text"));
        optionWriteCustomsToXML.setMnemonic(KeyEvent.VK_C);
        //endregion Campaign XML Save
        //endregion Create UI components

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
                    .addComponent(labelXMLSave)
                    .addComponent(optionWriteCustomsToXML)
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
                    .addComponent(labelXMLSave)
                    .addComponent(optionWriteCustomsToXML)
        );

        return body;
    }

    @Override
    protected void okAction() {
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(MekHqConstants.NO_SAVE_KEY, optionNoSave.isSelected());
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(MekHqConstants.SAVE_DAILY_KEY, optionSaveDaily.isSelected());
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(MekHqConstants.SAVE_WEEKLY_KEY, optionSaveWeekly.isSelected());
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(MekHqConstants.SAVE_MONTHLY_KEY, optionSaveMonthly.isSelected());
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(MekHqConstants.SAVE_YEARLY_KEY, optionSaveYearly.isSelected());
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(MekHqConstants.SAVE_BEFORE_MISSIONS_KEY, checkSaveBeforeMissions.isSelected());
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putInt(MekHqConstants.MAXIMUM_NUMBER_SAVES_KEY, (Integer) spinnerSavedGamesCount.getValue());
        userPreferences.node(MekHqConstants.XML_SAVES_NODE).putBoolean(MekHqConstants.WRITE_CUSTOMS_TO_XML, optionWriteCustomsToXML.isSelected());
    }

    private void setInitialState() {
        optionNoSave.setSelected(userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getBoolean(MekHqConstants.NO_SAVE_KEY, false));
        optionSaveDaily.setSelected(userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getBoolean(MekHqConstants.SAVE_DAILY_KEY, false));
        optionSaveWeekly.setSelected(userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getBoolean(MekHqConstants.SAVE_WEEKLY_KEY, true));
        optionSaveMonthly.setSelected(userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getBoolean(MekHqConstants.SAVE_MONTHLY_KEY, false));
        optionSaveYearly.setSelected(userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getBoolean(MekHqConstants.SAVE_YEARLY_KEY, false));
        checkSaveBeforeMissions.setSelected(userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getBoolean(MekHqConstants.SAVE_BEFORE_MISSIONS_KEY, false));
        spinnerSavedGamesCount.setValue(userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getInt(MekHqConstants.MAXIMUM_NUMBER_SAVES_KEY, MekHqConstants.DEFAULT_NUMBER_SAVES));
        optionWriteCustomsToXML.setSelected(userPreferences.node(MekHqConstants.XML_SAVES_NODE).getBoolean(MekHqConstants.WRITE_CUSTOMS_TO_XML, true));
    }
}
