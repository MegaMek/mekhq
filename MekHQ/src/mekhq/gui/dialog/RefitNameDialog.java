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
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.loaders.MekSummaryCache;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.parts.Refit;

/**
 * @author Taharqa
 */
public class RefitNameDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(RefitNameDialog.class);

    private final Refit refit;
    private boolean cancelled;

    private JTextField txtChassis;
    private JTextField txtModel;

    public RefitNameDialog(final JFrame frame, final boolean modal, final Refit refit) {
        super(frame, modal);
        this.refit = refit;
        cancelled = false;
        initComponents();
        setLocationRelativeTo(frame);
        setUserPreferences();
    }

    private void initComponents() {

        txtChassis = new JTextField();
        JLabel lblChassis = new JLabel();
        txtModel = new JTextField();
        JLabel lblModel = new JLabel();
        JButton btnOK = new JButton();
        JButton btnCancel = new JButton();

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.RefitNameDialog",
              MekHQ.getMHQOptions().getLocale());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        setTitle(resourceMap.getString("Form.title"));
        getContentPane().setLayout(new GridBagLayout());

        lblChassis.setText(resourceMap.getString("lblChassis.text"));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(lblChassis, gridBagConstraints);

        txtChassis.setText(refit.getNewEntity().getChassis());
        txtChassis.setMinimumSize(new Dimension(150, 28));
        // only allow chassis renaming for conventional infantry
        if (!refit.getNewEntity().isConventionalInfantry()) {
            txtChassis.setEditable(false);
            txtChassis.setEnabled(false);
        }
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(txtChassis, gridBagConstraints);

        lblModel.setText(resourceMap.getString("lblModel.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(lblModel, gridBagConstraints);

        txtModel.setText(refit.getNewEntity().getModel());
        txtModel.setMinimumSize(new Dimension(150, 28));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(txtModel, gridBagConstraints);

        btnOK.setText(resourceMap.getString("btnOK.text"));
        btnOK.setName("btnOK");
        btnOK.addActionListener(this::btnOKActionPerformed);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(btnOK, gridBagConstraints);

        btnCancel.setText(resourceMap.getString("btnCancel.text"));
        btnCancel.setName("btnClose");
        btnCancel.addActionListener(this::btnCloseActionPerformed);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(btnCancel, gridBagConstraints);

        pack();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(RefitNameDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    private void btnOKActionPerformed(ActionEvent evt) {
        String chassis = txtChassis.getText().trim();
        String model = txtModel.getText().trim();
        if (chassis.isEmpty()) {
            chassis = refit.getOriginalEntity().getChassis();
        }
        if (model.isEmpty()) {
            model = refit.getOriginalEntity().getModel() + " Mk II";
            model = model.trim(); // remove leading space if original model name was blank
        }
        refit.getNewEntity().setChassis(chassis);
        refit.getNewEntity().setModel(model);
        if (null != MekSummaryCache.getInstance().getMek(refit.getNewEntity().getShortNameRaw())) {
            JOptionPane.showMessageDialog(null,
                  "There is already a unit in the database with this name.\nPlease select a different name.",
                  "Name already in use",
                  JOptionPane.ERROR_MESSAGE);
            return;
        }
        this.setVisible(false);
    }

    private void btnCloseActionPerformed(ActionEvent evt) {
        cancelled = true;
        this.setVisible(false);
    }

    public boolean wasCancelled() {
        return cancelled;
    }
}
