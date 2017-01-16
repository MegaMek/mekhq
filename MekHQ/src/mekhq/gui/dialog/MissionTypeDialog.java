/*
 * MissionTypeDialog.java
 *
 * Created on Jan 6, 2010, 10:46:02 PM
 */

package mekhq.gui.dialog;

import java.awt.Frame;
import java.util.ResourceBundle;

import javax.swing.JButton;

import megamek.common.util.EncodeControl;

/**
 *
 * @author natit
 */
public class MissionTypeDialog extends javax.swing.JDialog {

	private boolean contract;
	
	private static final long serialVersionUID = 8376874926997734492L;
	/** Creates new form */
    public MissionTypeDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.setLocationRelativeTo(parent);
    }

    private void initComponents() {

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MissionTypeDialog", new EncodeControl()); //$NON-NLS-1$

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title"));
        
        getContentPane().setLayout(new java.awt.GridLayout(2,1));
      
        JButton btnMission = new javax.swing.JButton(resourceMap.getString("btnMission.text"));
        btnMission.setToolTipText(resourceMap.getString("btnMission.tooltip"));
        btnMission.setName("btnMission"); // NOI18N
        btnMission.addActionListener(ev -> {
        	contract = false;
        	setVisible(false);
        });
        getContentPane().add(btnMission);
        
        JButton btnContract = new javax.swing.JButton(resourceMap.getString("btnContract.text"));
        btnContract.setToolTipText(resourceMap.getString("btnContract.tooltip"));
        btnContract.setName("btnContract"); // NOI18N
        btnContract.addActionListener(ev -> {
        	contract = true;
        	setVisible(false);
        });
        getContentPane().add(btnContract);

        setSize(250, 150);
    }
    
    public boolean isContract() {
    	return contract;
    }
}
