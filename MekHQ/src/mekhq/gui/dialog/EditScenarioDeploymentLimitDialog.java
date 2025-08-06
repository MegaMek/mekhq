/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javax.swing.*;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.UnitType;
import mekhq.MekHQ;
import mekhq.campaign.mission.ScenarioDeploymentLimit;
import mekhq.campaign.mission.ScenarioDeploymentLimit.CountType;
import mekhq.campaign.mission.ScenarioDeploymentLimit.QuantityType;

public class EditScenarioDeploymentLimitDialog extends JDialog {

    private ScenarioDeploymentLimit deploymentLimit;
    private boolean newLimit;

    private JSpinner spnQuantity;
    private MMComboBox<QuantityType> choiceQuantityType;
    private MMComboBox<CountType> choiceCountType;
    private JCheckBox checkAllUnits;
    private JCheckBox[] checkAllowedUnits;

    public EditScenarioDeploymentLimitDialog(JFrame parent, boolean modal, ScenarioDeploymentLimit limit) {
        super(parent, modal);
        if (limit == null) {
            deploymentLimit = new ScenarioDeploymentLimit();
            newLimit = true;
        } else {
            deploymentLimit = limit;
            newLimit = false;
        }
        initComponents();
        setLocationRelativeTo(parent);
        pack();
    }

    private void initComponents() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditScenarioDeploymentLimitsDialog",
              MekHQ.getMHQOptions().getLocale());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("dialog.title"));

        getContentPane().setLayout(new BorderLayout());
        JPanel panMain = new JPanel(new GridBagLayout());
        JPanel panButtons = new JPanel(new FlowLayout());

        GridBagConstraints leftGbc = new GridBagConstraints();
        leftGbc.gridx = 0;
        leftGbc.gridy = 0;
        leftGbc.gridwidth = 1;
        leftGbc.weightx = 0.0;
        leftGbc.weighty = 0.0;
        leftGbc.insets = new Insets(5, 5, 5, 10);
        leftGbc.fill = GridBagConstraints.NONE;
        leftGbc.anchor = GridBagConstraints.NORTHWEST;

        GridBagConstraints rightGbc = new GridBagConstraints();
        rightGbc.gridx = 1;
        rightGbc.gridy = 0;
        rightGbc.gridwidth = 1;
        rightGbc.weightx = 1.0;
        rightGbc.weighty = 0.0;
        rightGbc.insets = new Insets(5, 10, 5, 5);
        rightGbc.fill = GridBagConstraints.HORIZONTAL;
        rightGbc.anchor = GridBagConstraints.NORTHWEST;

        panMain.add(new JLabel(resourceMap.getString("lblQuantityType.text")), leftGbc);
        choiceQuantityType = new MMComboBox<>("choiceQuantityType", QuantityType.values());
        choiceQuantityType.setSelectedItem(deploymentLimit.getQuantityType());
        choiceQuantityType.addActionListener(this::setQuantityModel);
        panMain.add(choiceQuantityType, rightGbc);


        leftGbc.gridy++;
        panMain.add(new JLabel(resourceMap.getString("lblCountType.text")), leftGbc);
        choiceCountType = new MMComboBox<>("choiceCountType", CountType.values());
        choiceCountType.setSelectedItem(deploymentLimit.getCountType());
        choiceCountType.addActionListener(this::setQuantityModel);
        rightGbc.gridy++;
        panMain.add(choiceCountType, rightGbc);


        leftGbc.gridy++;
        panMain.add(new JLabel(resourceMap.getString("lblQuantity.text")), leftGbc);
        spnQuantity = new JSpinner();
        spnQuantity.setValue(deploymentLimit.getQuantityLimit());
        setQuantityModel(null);
        rightGbc.gridy++;
        panMain.add(spnQuantity, rightGbc);

        JPanel panAllowedUnits = new JPanel(new GridLayout(UnitType.SIZE + 1, 1));
        panAllowedUnits.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createEmptyBorder(0, 0, 10, 0),
              BorderFactory.createTitledBorder(resourceMap.getString("panAllowedUnits.title"))));
        checkAllUnits = new JCheckBox(resourceMap.getString("checkAllUnits.text"));
        checkAllUnits.setSelected(deploymentLimit.getAllowedUnitTypes().isEmpty());
        checkAllUnits.addActionListener(this::checkAllUnits);
        panAllowedUnits.add(checkAllUnits);
        checkAllowedUnits = new JCheckBox[UnitType.SIZE];
        for (int i = UnitType.MEK; i < UnitType.SIZE; i++) {
            JCheckBox check = new JCheckBox(UnitType.getTypeName(i));
            check.setSelected(deploymentLimit.getAllowedUnitTypes().contains(i));
            check.setEnabled(!checkAllUnits.isSelected());
            checkAllowedUnits[i] = check;
            panAllowedUnits.add(check);
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        rightGbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridheight = 3;
        panMain.add(panAllowedUnits, gbc);

        JButton btnOk = new JButton(resourceMap.getString("btnOK.text"));
        btnOk.addActionListener(this::complete);
        JButton btnClose = new JButton(resourceMap.getString("btnCancel.text"));
        btnClose.addActionListener(this::cancel);
        panButtons.add(btnOk);
        panButtons.add(btnClose);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panButtons, BorderLayout.PAGE_END);
    }

    private void checkAllUnits(ActionEvent evt) {
        for (JCheckBox box : checkAllowedUnits) {
            if (checkAllUnits.isSelected()) {
                box.setSelected(false);
                box.setEnabled(false);
            } else {
                box.setEnabled(true);
            }
        }
    }

    private void setQuantityModel(ActionEvent evt) {
        int currentQuantity = (int) spnQuantity.getValue();
        if (currentQuantity < 1) {
            currentQuantity = 1;
        }
        CountType currentCountType = choiceCountType.getSelectedItem();
        QuantityType currentQuantityType = choiceQuantityType.getSelectedItem();
        if (currentQuantityType == QuantityType.PERCENT) {
            if (currentQuantity > 100) {
                currentQuantity = 100;
            }
            spnQuantity.setModel(new SpinnerNumberModel(currentQuantity, 1, 100, 5));
        } else {
            if (currentCountType == CountType.UNIT) {
                spnQuantity.setModel(new SpinnerNumberModel(currentQuantity, 1, null, 1));
            } else {
                spnQuantity.setModel(new SpinnerNumberModel(currentQuantity, 1, null, 500));
            }
        }
    }

    private void complete(ActionEvent evt) {
        deploymentLimit.setQuantityLimit((int) spnQuantity.getValue());
        deploymentLimit.setQuantityType(choiceQuantityType.getSelectedItem());
        deploymentLimit.setCountType(choiceCountType.getSelectedItem());
        ArrayList<Integer> allowed = new ArrayList<>();
        if (!checkAllUnits.isSelected()) {
            for (int i = UnitType.MEK; i < UnitType.SIZE; i++) {
                if (checkAllowedUnits[i].isSelected()) {
                    allowed.add(i);
                }
            }
        }
        deploymentLimit.setAllowedUnitTypes(allowed);
        this.setVisible(false);
    }

    private void cancel(ActionEvent evt) {
        if (newLimit) {
            deploymentLimit = null;
        }
        this.setVisible(false);
    }

    public ScenarioDeploymentLimit getDeploymentLimit() {
        return deploymentLimit;
    }
}
