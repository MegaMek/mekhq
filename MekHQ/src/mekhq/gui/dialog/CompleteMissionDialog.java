/*
 * CompleteMissionDialog.java
 *
 * Created on Jan 6, 2010, 10:46:02 PM
 */

package mekhq.gui.dialog;

import java.util.ResourceBundle;

import javax.swing.DefaultComboBoxModel;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.mission.Mission;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

/**
 *
 */
public class CompleteMissionDialog extends javax.swing.JDialog {

    private static final long serialVersionUID = 8376874926997734492L;

    Mission mission;
    int status;

    /** Creates new form */
    public CompleteMissionDialog(java.awt.Frame parent, boolean modal, Mission m) {
        super(parent, modal);
        this.mission = m;
        this.status = -1;
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        btnDone = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        choiceOutcome = new javax.swing.JComboBox<String>();
        lblOutcome = new javax.swing.JLabel();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CompleteMissionDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("title.text"));

        getContentPane().setLayout(new java.awt.GridBagLayout());

        lblOutcome.setText(resourceMap.getString("lblOutcome.text")); // NOI18N
        lblOutcome.setName("lblOutcome"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(lblOutcome, gridBagConstraints);

        DefaultComboBoxModel<String> outcomeModel = new DefaultComboBoxModel<String>();
        for (int i = 1; i < Mission.S_NUM; i++) {
            outcomeModel.addElement(Mission.getStatusName(i));
        }
        choiceOutcome.setModel(outcomeModel);
        choiceOutcome.setName("choiceOutcome"); // NOI18N
        choiceOutcome.setSelectedIndex(0);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(choiceOutcome, gridBagConstraints);

        btnDone.setText(resourceMap.getString("btnDone.text")); // NOI18N
        btnDone.setName("btnDone"); // NOI18N
        btnDone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDoneActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnDone, gridBagConstraints);

        btnCancel.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnCancel"); // NOI18N
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnCancel, gridBagConstraints);


        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(CompleteMissionDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void btnDoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDoneActionPerformed
        status = choiceOutcome.getSelectedIndex()+1;
        this.setVisible(false);
    }

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDoneActionPerformed
        status = -1;
        this.setVisible(false);
    }

    public int getStatus() {
        return status;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDone;
    private javax.swing.JButton btnCancel;
    private javax.swing.JLabel lblOutcome;
    private javax.swing.JComboBox<String> choiceOutcome;
    // End of variables declaration//GEN-END:variables
}
