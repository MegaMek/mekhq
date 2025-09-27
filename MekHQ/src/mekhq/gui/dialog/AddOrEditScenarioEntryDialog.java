/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.ServiceLogEntry;

/**
 * Dialog used to add or edit scenario entries.
 */
public class AddOrEditScenarioEntryDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(AddOrEditScenarioEntryDialog.class);

    private static final int ADD_OPERATION = 1;
    private static final int EDIT_OPERATION = 2;

    private final JFrame frame;
    private final int operationType;
    private LogEntry entry;
    private final LocalDate originalDate;
    private final String originalDescription;
    private LocalDate newDate;

    private JButton btnDate;
    private JTextField txtDesc;

    public AddOrEditScenarioEntryDialog(JFrame parent, boolean modal, LocalDate entryDate) {
        this(parent, modal, ADD_OPERATION, new ServiceLogEntry(entryDate, ""));
    }

    public AddOrEditScenarioEntryDialog(JFrame parent, boolean modal, LogEntry entry) {
        this(parent, modal, EDIT_OPERATION, entry);
    }

    private AddOrEditScenarioEntryDialog(JFrame parent, boolean modal, int operationType, LogEntry entry) {
        super(parent, modal);
        Objects.requireNonNull(entry);

        this.frame = parent;
        this.operationType = operationType;
        this.entry = entry;

        newDate = this.entry.getDate();
        originalDate = this.entry.getDate();
        originalDescription = this.entry.getDesc();

        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    public Optional<LogEntry> getEntry() {
        return Optional.ofNullable(entry);
    }

    private void initComponents() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AddOrEditScenarioEntryDialog",
              MekHQ.getMHQOptions().getLocale());
        GridBagConstraints gridBagConstraints;

        JPanel panMain = new JPanel();
        btnDate = new JButton();
        txtDesc = new JTextField();

        JPanel panBtn = new JPanel();
        JButton btnOK = new JButton();
        JButton btnClose = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");

        if (this.operationType == ADD_OPERATION) {
            setTitle(resourceMap.getString("dialogAdd.title"));
        } else {
            setTitle(resourceMap.getString("dialogEdit.title"));
        }

        getContentPane().setLayout(new BorderLayout());
        panMain.setLayout(new GridBagLayout());
        panBtn.setLayout(new GridLayout(0, 2));

        btnDate = new JButton();
        btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(newDate));
        btnDate.addActionListener(evt -> changeDate());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panMain.add(btnDate, gridBagConstraints);

        txtDesc.setText(entry.getDesc());
        txtDesc.setName("txtDesc");
        txtDesc.setEditable(true);
        txtDesc.setColumns(30);
        txtDesc.grabFocus();
        txtDesc.addActionListener(this::btnOKActionPerformed);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panMain.add(txtDesc, gridBagConstraints);

        btnOK.setText(resourceMap.getString("btnOkay.text"));
        btnOK.setName("btnOK");
        btnOK.addActionListener(this::btnOKActionPerformed);
        panBtn.add(btnOK);

        btnClose.setText(resourceMap.getString("btnCancel.text"));
        btnClose.setName("btnClose");
        btnClose.addActionListener(this::btnCloseActionPerformed);
        panBtn.add(btnClose);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);
        pack();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(AddOrEditScenarioEntryDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    private void changeDate() {
        DateChooser dc = new DateChooser(frame, newDate);
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            newDate = dc.getDate();
            btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(newDate));
        }
    }

    private void btnOKActionPerformed(ActionEvent evt) {
        entry.setDate(newDate);
        entry.setDesc(txtDesc.getText());
        entry.onLogEntryEdited(originalDate, newDate, originalDescription, txtDesc.getText(), null);
        this.setVisible(false);
    }

    private void btnCloseActionPerformed(ActionEvent evt) {
        entry = null;
        this.setVisible(false);
    }
}
