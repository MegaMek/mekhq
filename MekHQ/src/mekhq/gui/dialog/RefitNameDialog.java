/*
 * RefitNameDialog.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
import megamek.common.MekSummaryCache;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.parts.Refit;

/**
 * @author Taharqa
 */
public class RefitNameDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(RefitNameDialog.class);

    private JFrame frame;
    private Refit refit;
    private boolean cancelled;

    private JButton btnCancel;
    private JButton btnOK;
    private JLabel lblChassis;
    private JTextField txtChassis;
    private JLabel lblModel;
    private JTextField txtModel;

    public RefitNameDialog(final JFrame frame, final boolean modal, final Refit refit) {
        super(frame, modal);
        this.frame = frame;
        this.refit = refit;
        cancelled = false;
        initComponents();
        setLocationRelativeTo(frame);
        setUserPreferences();
    }

    private void initComponents() {

        txtChassis = new JTextField();
        lblChassis = new JLabel();
        txtModel = new JTextField();
        lblModel = new JLabel();
        btnOK = new JButton();
        btnCancel = new JButton();

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

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(RefitNameDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
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
