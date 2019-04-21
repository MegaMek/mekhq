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

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

public class MekHqOptionsDialog extends BaseDialog {
    ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.MekHqOptionsDialog");

    public MekHqOptionsDialog(JFrame parent, MMLogger logger) {
        super(parent, logger);

        this.initialize(resources);
        this.setInitialState();
    }

    @Override
    protected Container createCustomUI() {
        // Create UI components
        JLabel labelSavedInfo = new JLabel(resources.getString("labelSavedInfo.text"));

        JRadioButton optionSaveDaily = new JRadioButton(resources.getString("optionSaveDaily.text"));
        optionSaveDaily.setMnemonic(KeyEvent.VK_D);

        JRadioButton optionSaveWeekly = new JRadioButton(resources.getString("optionSaveWeekly.text"));
        optionSaveWeekly.setMnemonic(KeyEvent.VK_W);

        ButtonGroup saveFrequencyGroup = new ButtonGroup();
        saveFrequencyGroup.add(optionSaveDaily);
        saveFrequencyGroup.add(optionSaveWeekly);

        JCheckBox checkSaveBeforeMissions = new JCheckBox(resources.getString("checkSaveBeforeMissions.text"));
        checkSaveBeforeMissions.setMnemonic(KeyEvent.VK_S);

        JLabel labelSavedGamesCount = new JLabel(resources.getString("labelSavedGamesCount.text"));
        JSpinner spinnerSavedGamesCount = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
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
                .addComponent(optionSaveDaily)
                .addComponent(optionSaveWeekly)
                .addComponent(checkSaveBeforeMissions)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(labelSavedGamesCount)
                    .addComponent(spinnerSavedGamesCount, GroupLayout.Alignment.TRAILING))
        );

        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(labelSavedInfo)
                .addComponent(optionSaveDaily)
                .addComponent(optionSaveWeekly)
                .addComponent(checkSaveBeforeMissions)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(labelSavedGamesCount)
                    .addComponent(spinnerSavedGamesCount))
        );

        return body;
    }

    @Override
    protected void okAction() {
    }

    private void setInitialState() {
    }
}
