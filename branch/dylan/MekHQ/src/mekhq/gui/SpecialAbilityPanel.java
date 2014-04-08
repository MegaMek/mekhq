package mekhq.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import mekhq.campaign.personnel.SpecialAbility;

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
        

        public SpecialAbilityPanel(SpecialAbility a) {
            this.abil = a;
           
            setLayout(new GridBagLayout());

            GridBagConstraints c = new GridBagConstraints();
          
            JTextArea txtDesc = new JTextArea(abil.getDescription());
            txtDesc.setEditable(false);
            txtDesc.setBackground(this.getBackground());
            
            c.gridx = 0;
            c.gridy = 0;
            c.weighty = 0.0;
            c.gridwidth = 4;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTHWEST;
            add(txtDesc, c);
            
            c.gridx = 0;
            c.gridy = 1;
            c.weighty = 0.0;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTHWEST;
            add(new JLabel("<html><b>XP Cost</b></html>"), c); 
            c.gridx = 0;
            c.gridy = 2;
            c.weighty = 1.0;
            add(new JLabel(Integer.toString(abil.getCost())), c);     
            
            c.gridx = 1;
            c.gridy = 1;
            c.weighty = 0.0;
            add(new JLabel("<html><b>Prerequisites</b></html>"), c);
            c.gridx = 1;
            c.gridy = 2;
            c.weighty = 1.0;
            add(new JLabel("<html>" + abil.getPrereqDesc() + "</html>"), c);     
            
            c.gridx = 2;
            c.gridy = 1;
            c.weighty = 0.0;
            add(new JLabel("<html><b>Incompatible</b></html>"), c);
            c.gridx = 2;
            c.gridy = 2;
            c.weighty = 1.0;
            add(new JLabel("<html>" + abil.getInvalidDesc() + "</html>"), c);     
            
            c.gridx = 3;
            c.gridy = 1;
            c.weighty = 0.0;
            add(new JLabel("<html><b>Removes</b></html>"), c);
            c.gridx = 3;
            c.gridy = 2;
            c.weighty = 1.0;
            add(new JLabel("<html>" + abil.getRemovedDesc() + "</html>"), c);     
            
            this.setBorder(BorderFactory.createTitledBorder(abil.getDisplayName()));
            
        }
    }