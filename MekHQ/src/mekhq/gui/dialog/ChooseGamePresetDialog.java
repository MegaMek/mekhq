/*
 * ChooseGamePresetDialog.java
 *
 * Copyright (c) 2009 - Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.GamePreset;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

/**
 * This dialog will allow the users to choose from a set of possible game presets
 * @author Taharqa
 */
public class ChooseGamePresetDialog extends javax.swing.JDialog {
    private static final long serialVersionUID = -8038099101234445018L;


    private javax.swing.JPanel panButtons;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnOk;

    private List<GamePreset> presets;
    private List<JRadioButton> presetButtons;
    private GamePreset selectedPreset;
    private boolean cancelled;

    /** Creates new form NewTeamDialog */
    public ChooseGamePresetDialog(java.awt.Frame parent, boolean modal, List<GamePreset> p) {
        super(parent, modal);
        presets = p;
        cancelled = false;
        selectedPreset = null;
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {
         java.awt.GridBagConstraints gridBagConstraints;

         panButtons = new javax.swing.JPanel();
         btnOk = new javax.swing.JButton();
         btnCancel = new javax.swing.JButton();
         presetButtons = new ArrayList<>();

         JPanel mainPanel = new JPanel(new GridBagLayout());
         JScrollPane scrPane = new JScrollPane(mainPanel);

         ButtonGroup group = new ButtonGroup();

         ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ChooseGamePresetDialog", new EncodeControl()); //$NON-NLS-1$
         setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
         setName("Form"); // NOI18N

         getContentPane().setLayout(new BorderLayout());
         setTitle(resourceMap.getString("title"));

         int i = 0;
         for (GamePreset preset : presets) {
             JRadioButton presetButton = new JRadioButton("");
             presetButtons.add(presetButton);
             presetButton.setSelected(i == 0);
             group.add(presetButton);
             JLabel label = new JLabel("<html><b>" + preset.getTitle() + "</b><br>" + preset.getDescription() + "</html>");

             JPanel presetPanel = new JPanel(new BorderLayout());
             presetPanel.add(presetButton, BorderLayout.WEST);
             presetPanel.add(label, BorderLayout.CENTER);
             presetPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
             presetPanel.setPreferredSize(new Dimension(450, 100));

             gridBagConstraints = new java.awt.GridBagConstraints();
             gridBagConstraints.gridx = 0;
             gridBagConstraints.gridy = i;
             gridBagConstraints.weightx = 1.0;
             gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
             gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
             gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
             mainPanel.add(presetPanel, gridBagConstraints);
             i++;
         }

         panButtons.setName("panButtons");
         panButtons.setLayout(new GridBagLayout());

         btnOk.setText(resourceMap.getString("btnOk.text")); // NOI18N
         btnOk.setName("btnOk"); // NOI18N
         btnOk.addActionListener(this::btnOkActionPerformed);
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 0;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.gridwidth = 1;
         gridBagConstraints.weightx = 1.0;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
         panButtons.add(btnOk, gridBagConstraints);

         btnCancel.setText(resourceMap.getString("btnCancel.text")); // NOI18N
         btnCancel.setName("btnClose"); // NOI18N
         btnCancel.addActionListener(this::btnCancelActionPerformed);
         gridBagConstraints = new java.awt.GridBagConstraints();
         gridBagConstraints.gridx = 1;
         gridBagConstraints.gridy = 0;
         gridBagConstraints.gridwidth = 1;
         gridBagConstraints.weightx = 0.0;
         gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
         gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
         panButtons.add(btnCancel, gridBagConstraints);

         scrPane.setPreferredSize(new Dimension(500, 430));

         getContentPane().add(scrPane, BorderLayout.CENTER);
         getContentPane().add(panButtons, BorderLayout.SOUTH);

         pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(ChooseGamePresetDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void btnOkActionPerformed(java.awt.event.ActionEvent evt) {
        int sel = 0;
        for (JRadioButton button : presetButtons) {
            if (button.isSelected()) {
                selectedPreset = presets.get(sel);
                break;
            }
            sel++;
        }

        this.setVisible(false);
    }

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {
        cancelled = true;
        this.setVisible(false);
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    public GamePreset getSelectedPreset() {
        return selectedPreset;
    }
}
