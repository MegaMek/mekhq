/*
 * MekViewDialog.java
 *
 * Created on July 15, 2009, 9:30 PM
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ResourceBundle;

import megamek.common.MechView;
import megamek.common.util.EncodeControl;

/**
 *
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekViewDialog extends javax.swing.JDialog {

    /**
	 * 
	 */
	private static final long serialVersionUID = 5894364756899721545L;
	private MechView mview;
    
    /** Creates new form MekViewDialog */
    public MekViewDialog(java.awt.Frame parent, boolean modal, MechView mv) {
        super(parent, modal);
        this.mview = mv;
        initComponents();
    }

    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        txtMek = new javax.swing.JTextPane();
        btnOkay = new javax.swing.JButton();
        
		ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekViewDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Unit View"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        txtMek.setContentType(resourceMap.getString("txtMek.contentType")); // NOI18N
        txtMek.setEditable(false);
        txtMek.setFont(Font.decode(resourceMap.getString("txtMek.font"))); // NOI18N
        txtMek.setName("txtMek"); // NOI18N
        txtMek.setText(mview.getMechReadout());
        jScrollPane2.setViewportView(txtMek);

        btnOkay.setText(resourceMap.getString("btnOkay.text")); // NOI18N
        btnOkay.setName("btnOkay"); // NOI18N
        btnOkay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOkayActionPerformed(evt);
            }
        });
        
        getContentPane().add(jScrollPane2, BorderLayout.CENTER);
        getContentPane().add(btnOkay, BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void btnOkayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOkayActionPerformed
	    this.setVisible(false);
	}//GEN-LAST:event_btnOkayActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnOkay;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextPane txtMek;
    // End of variables declaration//GEN-END:variables

}
