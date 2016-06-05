package mekhq.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import megamek.common.util.EncodeControl;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.gui.dialog.CampaignOptionsDialog;
import mekhq.gui.dialog.EditSpecialAbilityDialog;

/**
 * An extension of JPanel that displays information about special abilities
 * @author Jay Lawson
 *
 */
public class SpecialAbilityPanel extends JPanel {

        /**
         *
         */
        private static final long serialVersionUID = -7337823041775639463L;

        private SpecialAbility abil;
        private JButton btnRemove;
        private JButton btnEdit;
        private CampaignOptionsDialog cod;
        

        public SpecialAbilityPanel(SpecialAbility a, CampaignOptionsDialog cod) {
            this.abil = a;
            this.cod = cod;
            
            btnEdit = new JButton("Edit");
            btnRemove = new JButton("Remove");
           
            btnEdit.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    editSPA();
                }
            });
            
            btnRemove.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    remove();
                }
            });
            
            
            setLayout(new GridBagLayout());
            
            initComponents();
        }
        
        private void initComponents() {
            
            ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.SpecialAbilityPanel", new EncodeControl()); //$NON-NLS-1$

            GridBagConstraints c = new GridBagConstraints();
          
            JTextArea txtDesc = new JTextArea(abil.getDescription());
            txtDesc.setEditable(false);
            txtDesc.setBackground(this.getBackground());
            
            JPanel pnlButton = new JPanel(new GridLayout(0,2));
            pnlButton.add(btnEdit);
            pnlButton.add(btnRemove);
            
            c.gridx = 0;
            c.gridy = 0;
            c.weighty = 0.0;
            c.gridwidth = 4;
            c.weightx = 0.0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHWEST;
            add(pnlButton, c);
            
            c.gridx = 0;
            c.gridy = 1;
            c.weighty = 0.0;
            c.gridwidth = 4;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTHWEST;
            add(txtDesc, c);
            
            c.gridx = 0;
            c.gridy = 2;
            c.weighty = 0.0;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTHWEST;
            add(new JLabel("<html><b>XP Cost</b></html>"), c); 
            c.gridx = 0;
            c.gridy = 3;
            c.weighty = 1.0;
            add(new JLabel(Integer.toString(abil.getCost())), c);     
            
            c.gridx = 1;
            c.gridy = 2;
            c.weighty = 0.0;
            JLabel lblPrereq = new JLabel("<html><b>Prerequisites</b></html>");
            lblPrereq.setToolTipText(resourceMap.getString("lblPrereq.toolTipText"));
            add(lblPrereq, c);
            c.gridx = 1;
            c.gridy = 3;
            c.weighty = 1.0;
            add(new JLabel("<html>" + abil.getAllPrereqDesc() + "</html>"), c);     
            
            c.gridx = 2;
            c.gridy = 2;
            c.weighty = 0.0;
            JLabel lblIncompatible = new JLabel("<html><b>Incompatible</b></html>");
            lblIncompatible.setToolTipText(resourceMap.getString("lblIncompatible.toolTipText"));
            add(lblIncompatible, c);
            c.gridx = 2;
            c.gridy = 3;
            c.weighty = 1.0;
            add(new JLabel("<html>" + abil.getInvalidDesc() + "</html>"), c);     
            
            c.gridx = 3;
            c.gridy = 2;
            c.weighty = 0.0;
            JLabel lblRemove = new JLabel("<html><b>Removes</b></html>");
            lblRemove.setToolTipText(resourceMap.getString("lblRemove.toolTipText"));
            add(lblRemove, c);
            c.gridx = 3;
            c.gridy = 3;
            c.weighty = 1.0;
            add(new JLabel("<html>" + abil.getRemovedDesc() + "</html>"), c);     
            
            this.setBorder(BorderFactory.createTitledBorder(abil.getDisplayName()));
            
        }
        
        private void remove() {
        	cod.btnRemoveSPA(abil.getName());
        }
        
        private void editSPA() {
        	EditSpecialAbilityDialog esad = new EditSpecialAbilityDialog(null, abil, cod.getCurrentSPA());
            esad.setVisible(true);
            this.removeAll();
            initComponents();
            this.revalidate();
            this.repaint();
        }
    }