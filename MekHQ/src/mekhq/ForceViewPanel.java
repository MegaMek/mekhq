/*
 * ForceViewPanel
 *
 * Created on May 2, 2011
 */

package mekhq;

import java.awt.Color;
import java.awt.Image;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import megamek.common.Pilot;
import megamek.common.util.DirectoryItems;
import mekhq.campaign.Campaign;
import mekhq.campaign.Force;
import mekhq.campaign.Unit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PilotPerson;

/**
 * A custom panel that gets filled in with goodies from a Force record
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ForceViewPanel extends javax.swing.JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7004741688464105277L;

	private Force force;
	private Campaign campaign;
	
	private DirectoryItems portraits;
	private DirectoryItems forceIcons;


	private javax.swing.JLabel lblIcon;
	private javax.swing.JPanel pnlStats;
	private javax.swing.JPanel pnlSubUnits;
	private javax.swing.JTextArea txtDesc;
	
	public ForceViewPanel(Force f, Campaign c, DirectoryItems portraits, DirectoryItems ficons) {
		this.force = f;
		this.campaign = c;
		this.portraits = portraits;
		this.forceIcons = ficons;
		initComponents();
	}
	
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		lblIcon = new javax.swing.JLabel();
		pnlStats = new javax.swing.JPanel();
		pnlSubUnits = new javax.swing.JPanel();
		txtDesc = new javax.swing.JTextArea();
		       
		setLayout(new java.awt.GridBagLayout());

		setBackground(Color.WHITE);
		
		lblIcon.setName("lblPortait"); // NOI18N
		lblIcon.setBackground(Color.WHITE);
		setIcon();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(lblIcon, gridBagConstraints);
		
		pnlStats.setName("pnlStats");
		pnlStats.setBorder(BorderFactory.createTitledBorder(force.getName()));
		pnlStats.setBackground(Color.WHITE);
		fillStats();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlStats, gridBagConstraints);
		
		pnlSubUnits.setName("pnlSubUnits");
		//pnlSubUnits.setBorder(BorderFactory.createTitledBorder("Contents"));
		pnlSubUnits.setBackground(Color.WHITE);
		fillSubUnits();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlSubUnits, gridBagConstraints);
		
		txtDesc.setName("txtDesc");
		txtDesc.setText(force.getDescription());
		txtDesc.setEditable(false);
		txtDesc.setLineWrap(true);
		txtDesc.setWrapStyleWord(true);
		txtDesc.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Description"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(txtDesc, gridBagConstraints);
	}
	
	private void setIcon() {
        String category = force.getIconCategory();
        String file = force.getIconFileName();

        if(Pilot.ROOT_PORTRAIT.equals(category)) {
            category = "";
        }

        // Return a null if the player has selected no portrait file.
        if ((null == category) || (null == file) || Pilot.PORTRAIT_NONE.equals(file)) {
            return;
        }

        // Try to get the player's portrait file.
        Image portrait = null;
        try {
            portrait = (Image) forceIcons.getItem(category, file);
            //make sure no images are longer than 150 pixels
            if(null != portrait && portrait.getWidth(lblIcon) > 150) {
                portrait = portrait.getScaledInstance(150, -1, Image.SCALE_DEFAULT);               
            }
            lblIcon.setIcon(new ImageIcon(portrait));
        } catch (Exception err) {
            err.printStackTrace();
        }
	}

	
	private void fillStats() {
		//BV
		//Tonnage?
		//Cost?
		//Number of units?
		//Assigned to
	}
	
	private void fillSubUnits() {
		
		java.awt.GridBagConstraints gridBagConstraints;

		pnlSubUnits.setLayout(new java.awt.GridBagLayout());
		
		JLabel lblPerson;
		JLabel lblUnit;
		int nexty = 0;
		for(int pid : force.getPersonnel()) {
			Person p = campaign.getPerson(pid);
			if(null == p) {
				continue;
			}
			lblPerson = new JLabel();
			lblPerson.setText(campaign.getFullTitleFor(p));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = nexty;
			gridBagConstraints.gridwidth = 1;
			gridBagConstraints.weighty = 1.0;
			gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlSubUnits.add(lblPerson, gridBagConstraints);
			if(p instanceof PilotPerson) {
				Unit u = campaign.getUnit(((PilotPerson)p).getUnitId());
				if(null != u) {
					lblUnit = new JLabel();
					lblUnit.setText(u.getEntity().getDisplayName());
					gridBagConstraints = new java.awt.GridBagConstraints();
					gridBagConstraints.gridx = 1;
					gridBagConstraints.gridy = nexty;
					gridBagConstraints.gridwidth = 1;
					gridBagConstraints.weighty = 1.0;
					gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
					gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
					pnlSubUnits.add(lblUnit, gridBagConstraints);
				}
			}
			nexty++;
		}
	}

}
	