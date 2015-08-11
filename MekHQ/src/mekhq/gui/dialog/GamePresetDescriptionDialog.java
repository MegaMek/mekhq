/*
 * GamePresetDescriptionDialog.java
 *
 */

package mekhq.gui.dialog;

import java.awt.Dimension;
import java.util.ResourceBundle;

import javax.swing.JLabel;

/**
 *
 * @author Jay Lason
 */
public class GamePresetDescriptionDialog extends javax.swing.JDialog {
	
	private static final long serialVersionUID = 3624327778807359294L;

	private javax.swing.JTextField txtTitle;
	private javax.swing.JTextArea txtDesc;
	private javax.swing.JScrollPane scrText;
	private javax.swing.JButton btnOK;
	private javax.swing.JButton btnCancel;
	private boolean changed;

	public GamePresetDescriptionDialog(java.awt.Frame parent, boolean modal, String title, String desc) {
        super(parent, modal);
        setTitle("Enter description of preset");
        initComponents();    
        txtTitle.setText(title);
        txtDesc.setText(desc);
        changed = false;
        setMinimumSize(new Dimension(400, 250));
        setPreferredSize(new Dimension(400, 250));
        setLocationRelativeTo(parent);
    }
	
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		txtTitle = new javax.swing.JTextField();
		txtDesc = new javax.swing.JTextArea();
		scrText = new javax.swing.JScrollPane();
		btnOK = new javax.swing.JButton();
		btnCancel = new javax.swing.JButton();

		ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.TextAreaDialog");

		setLayout(new java.awt.GridBagLayout());
		
		txtDesc.setName("txtDesc");
		txtDesc.setEditable(true);
		txtDesc.setLineWrap(true);
		txtDesc.setWrapStyleWord(true);
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(new JLabel("Title:"), gridBagConstraints);
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(txtTitle, gridBagConstraints);
		
		scrText.setPreferredSize(new Dimension(450, 100));
		scrText.setViewportView(txtDesc);	
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(scrText, gridBagConstraints);
		
		btnOK.setText(resourceMap.getString("btnOK.text"));
		btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed();
            }
        });
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
		add(btnOK, gridBagConstraints);
		
		btnCancel.setText(resourceMap.getString("btnCancel.text"));
		btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setVisible(false);
            }
        });
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
		add(btnCancel, gridBagConstraints);		
	}
	
	public String getTitle() {
		return txtTitle.getText();
	}
	
	public String getDesc() {
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
