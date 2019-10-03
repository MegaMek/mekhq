/*
 * TextAreaDialog.java
 *
 */

package mekhq.gui.dialog;

import java.awt.Dimension;
import java.util.ResourceBundle;

import megamek.common.util.EncodeControl;
import mekhq.gui.utilities.MarkdownEditorPanel;

/**
 *
 * @author Jay Lason
 */
public class MarkdownEditorDialog extends javax.swing.JDialog {
    
    private static final long serialVersionUID = 3624327778807359294L;

    private MarkdownEditorPanel mkEditor;
    private javax.swing.JButton btnOK;
    private javax.swing.JButton btnCancel;
    private boolean changed;

    public MarkdownEditorDialog(java.awt.Frame parent, boolean modal, String title, String text) {
        super(parent, modal);
        setTitle(title);
        
        initComponents();
        
        setPreferredSize(new Dimension(400, 500));

        pack();

        setLocationRelativeTo(parent);

        mkEditor.setText(text);
        changed = false;
    }
    
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        mkEditor = new MarkdownEditorPanel();
        btnOK = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.TextAreaDialog", new EncodeControl()); //$NON-NLS-1$

        setLayout(new java.awt.GridBagLayout());
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(mkEditor, gridBagConstraints);
        
        btnOK.setText(resourceMap.getString("btnOK.text"));
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
        
        btnCancel.setText(resourceMap.getString("btnCancel.text"));
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
        return mkEditor.getText();
    }
    
    private void btnOKActionPerformed() {
        changed = true;
        setVisible(false);
    }
    
    public boolean wasChanged() {
        return changed;
    }
    
}
