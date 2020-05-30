/*
 * ResolveWizardChooseFilesDialog.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import javax.swing.*;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

/**
 * @author  Taharqa
 */
public class ChooseMulFilesDialog extends JDialog {
    private static final long serialVersionUID = -8038099101234445018L;

    private ResolveScenarioTracker tracker;

    private JTextField txtUnitFile;
    private boolean cancelled;

    public ChooseMulFilesDialog(JFrame parent, boolean modal, ResolveScenarioTracker t) {
        super(parent, modal);
        this.tracker = t;
        cancelled = false;
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    /**
     * This initializes the dialog's components
     * It currently uses the following Mnemonics:
     * B, C, O, ESCAPE
     */
    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ChooseMulFilesDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // Adding Escape Mnemonic
        getRootPane().registerKeyboardAction(e -> btnCancelActionPerformed(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setName("Form"); // NOI18N

        getContentPane().setLayout(new GridBagLayout());
        setTitle(resourceMap.getString("title"));

        JTextArea txtInstructions = new JTextArea(resourceMap.getString("txtInstructions.text"));
        txtInstructions.setName("txtInstructions");
        txtInstructions.setEditable(false);
        txtInstructions.setLineWrap(true);
        txtInstructions.setWrapStyleWord(true);
        txtInstructions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtInstructions.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        txtInstructions.setPreferredSize(new Dimension(400,200));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        getContentPane().add(txtInstructions, gridBagConstraints);

        JButton btnUnitFile = new JButton(resourceMap.getString("btnUnitFile.text")); // NOI18N
        btnUnitFile.setName("btnUnitFile"); // NOI18N
        btnUnitFile.setMnemonic(KeyEvent.VK_B);
        btnUnitFile.addActionListener(e -> {
            tracker.findUnitFile();
            txtUnitFile.setText(tracker.getUnitFilePath());
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        getContentPane().add(btnUnitFile, gridBagConstraints);

        txtUnitFile = new JTextField(tracker.getUnitFilePath());
        txtUnitFile.setName("txtUnitFile");
        txtUnitFile.setEditable(false);
        txtUnitFile.setOpaque(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        getContentPane().add(txtUnitFile, gridBagConstraints);

        JPanel panButtons = new JPanel();
        panButtons.setName("panButtons");
        panButtons.setLayout(new GridBagLayout());

        JButton btnCancel = new JButton(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnClose"); // NOI18N
        btnCancel.setMnemonic(KeyEvent.VK_C);
        btnCancel.addActionListener(e -> btnCancelActionPerformed());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panButtons.add(btnCancel, gridBagConstraints);

        JButton btnNext = new JButton(resourceMap.getString("btnNext.text")); // NOI18N
        btnNext.setName("btnNext"); // NOI18N
        btnNext.setMnemonic(KeyEvent.VK_O);
        btnNext.addActionListener(e -> btnNextActionPerformed());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panButtons.add(btnNext, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        getContentPane().add(panButtons, gridBagConstraints);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(ChooseMulFilesDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void btnNextActionPerformed() {
        tracker.processMulFiles();
        this.setVisible(false);
    }

    private void btnCancelActionPerformed() {
        cancelled = true;
        this.setVisible(false);
    }

    public boolean wasCancelled() {
        return cancelled;
    }
}
