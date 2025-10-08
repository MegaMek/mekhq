/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;
import javax.swing.*;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.ResolveScenarioTracker;

/**
 * @author Taharqa
 */
public class ChooseMulFilesDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(ChooseMulFilesDialog.class);

    private final ResolveScenarioTracker tracker;

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
     * This initializes the dialog's components It currently uses the following Mnemonics: B, C, O, ESCAPE
     */
    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ChooseMulFilesDialog",
              MekHQ.getMHQOptions().getLocale());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // Adding Escape Mnemonic
        getRootPane().registerKeyboardAction(e -> btnCancelActionPerformed(),
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
              JComponent.WHEN_IN_FOCUSED_WINDOW);

        setName("Form");

        getContentPane().setLayout(new GridBagLayout());
        setTitle(resourceMap.getString("title"));

        JTextArea txtInstructions = new JTextArea(resourceMap.getString("txtInstructions.text"));
        txtInstructions.setName("txtInstructions");
        txtInstructions.setEditable(false);
        txtInstructions.setLineWrap(true);
        txtInstructions.setWrapStyleWord(true);
        txtInstructions.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(resourceMap.getString(
              "txtInstructions.title")), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        txtInstructions.setPreferredSize(new Dimension(400, 200));
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

        JButton btnUnitFile = new JButton(resourceMap.getString("btnUnitFile.text"));
        btnUnitFile.setName("btnUnitFile");
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

        JButton btnCancel = new JButton(resourceMap.getString("btnCancel.text"));
        btnCancel.setName("btnClose");
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

        JButton btnNext = new JButton(resourceMap.getString("btnNext.text"));
        btnNext.setName("btnNext");
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

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ChooseMulFilesDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
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
