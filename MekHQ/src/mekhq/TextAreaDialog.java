/*
 * TextAreaDialog.java
 *
 */

package mekhq;

import java.awt.Dimension;

/**
 *
 * @author Jay Lason
 */
public class TextAreaDialog extends javax.swing.JDialog {
	
	private static final long serialVersionUID = 3624327778807359294L;

	private javax.swing.JTextArea txtDesc;
	private javax.swing.JScrollPane scrText;
	private javax.swing.JButton btnOK;
	private javax.swing.JButton btnCancel;
	private boolean changed;

	public TextAreaDialog(java.awt.Frame parent, boolean modal, String title, String text) {
        super(parent, modal);
        setTitle(title);
        initComponents();     
        txtDesc.setText(text);
        changed = false;
        setMinimumSize(new Dimension(400, 500));
        setPreferredSize(new Dimension(400, 500));
        setLocationRelativeTo(parent);
    }
	
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		txtDesc = new javax.swing.JTextArea();
		scrText = new javax.swing.JScrollPane();
		btnOK = new javax.swing.JButton();
		btnCancel = new javax.swing.JButton();

		setLayout(new java.awt.GridBagLayout());
		
		txtDesc.setName("txtDesc");
		txtDesc.setEditable(true);
		txtDesc.setLineWrap(true);
		txtDesc.setWrapStyleWord(true);
		
		scrText.setViewportView(txtDesc);	
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(scrText, gridBagConstraints);
		
		btnOK.setText("OK");
		btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed();
            }
        });
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
		add(btnOK, gridBagConstraints);
		
		btnCancel.setText("Cancel");
		btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setVisible(false);
            }
        });
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
		add(btnCancel, gridBagConstraints);		
	}
	
	public String getText() {
		return txtDesc.getText();
	}
	
	private void btnOKActionPerformed() {
		changed = true;
		setVisible(false);
	}
	
	public boolean wasChanged() {
		return changed;
	}
	
}
