/*
 * PersonViewPanel
 *
 * Created on July 26, 2009, 11:32 PM
 */

package mekhq;

import java.awt.Color;
import java.awt.Image;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import megamek.client.ui.swing.util.ImageFileFactory;
import megamek.common.Pilot;
import megamek.common.util.DirectoryItems;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PilotPerson;

/**
 * A custom panel that gets filled in with goodies from a Person record
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class PersonViewPanel extends javax.swing.JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7004741688464105277L;

	private Person person;
	private Campaign campaign;
	
	private DirectoryItems portraits;
	
	private javax.swing.JLabel lblPortrait;
	private javax.swing.JLabel lblUnit;
	private javax.swing.JPanel pnlStats;
	private javax.swing.JTextArea txtDesc;
	
	private javax.swing.JPanel pnlColFluff;
	private javax.swing.JPanel pnlColStats;
	private javax.swing.JLabel lblType;
	private javax.swing.JLabel lblCall1;
	private javax.swing.JLabel lblCall2;
	private javax.swing.JLabel lblAge1;
	private javax.swing.JLabel lblAge2;
	private javax.swing.JLabel lblGender1;
	private javax.swing.JLabel lblGender2;
	private javax.swing.JLabel lblGun1;
	private javax.swing.JLabel lblGun2;
	private javax.swing.JLabel lblPilot1;
	private javax.swing.JLabel lblPilot2;
	
	
	public PersonViewPanel(Person p, Campaign c) {
		this.person = p;
		this.campaign = c;
		try {
            portraits = new DirectoryItems(new File("data/images/portraits"), "", //$NON-NLS-1$ //$NON-NLS-2$
                    ImageFileFactory.getInstance());
        } catch (Exception e) {
            portraits = null;
        }
		initComponents();
	}
	
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		lblPortrait = new javax.swing.JLabel();
		lblUnit = new javax.swing.JLabel();
		pnlStats = new javax.swing.JPanel();
		txtDesc = new javax.swing.JTextArea();
		
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mekhq.MekHQApp.class).getContext().getResourceMap(PersonViewPanel.class);
        
		setLayout(new java.awt.GridBagLayout());

		setBackground(Color.WHITE);
		
		lblPortrait.setName("lblPortait"); // NOI18N
		lblPortrait.setBackground(Color.WHITE);
		setPortrait();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(lblPortrait, gridBagConstraints);
	
		lblUnit.setName("lblUnit"); // NOI18N
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(lblUnit, gridBagConstraints);
		
		pnlStats.setName("pnlStats");
		pnlStats.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(campaign.getFullTitleFor(person)),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		//pnlStats.setMinimumSize(new java.awt.Dimension(250,200));
		//pnlStats.setPreferredSize(new java.awt.Dimension(250,200));
		pnlStats.setBackground(Color.WHITE);
		fillStats();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlStats, gridBagConstraints);
		
		txtDesc.setName("txtDesc");
		txtDesc.setText(person.getBiography());
		txtDesc.setEditable(false);
		txtDesc.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Description"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(txtDesc, gridBagConstraints);
	}
	
	/**
     * set the portrait for the given person.
     *
     * @return The <code>Image</code> of the pilot's portrait. This value
     *         will be <code>null</code> if no portrait was selected
     *          or if there was an error loading it.
     */
    public void setPortrait() {

        String category = person.getPortraitCategory();
        String file = person.getPortraitFileName();

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
            portrait = (Image) portraits.getItem(category, file);
            //make sure no images are longer than 72 pixels
            if(null != portrait && portrait.getWidth(lblPortrait) > 150) {
                portrait = portrait.getScaledInstance(-1, 150, Image.SCALE_DEFAULT);               
            }
            lblPortrait.setIcon(new ImageIcon(portrait));
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
    
    public void fillStats() {
    	
    	pnlColFluff = new javax.swing.JPanel();
    	pnlColStats = new javax.swing.JPanel();
    	lblCall1 = new javax.swing.JLabel();
		lblCall2 = new javax.swing.JLabel();
		lblAge1 = new javax.swing.JLabel();
		lblAge2 = new javax.swing.JLabel();
		lblGender1 = new javax.swing.JLabel();
		lblGender2 = new javax.swing.JLabel();
		lblGun1 = new javax.swing.JLabel();
		lblGun2 = new javax.swing.JLabel();
		lblPilot1 = new javax.swing.JLabel();
		lblPilot2 = new javax.swing.JLabel();
    	
    	java.awt.GridBagConstraints gridBagConstraints;
		pnlStats.setLayout(new java.awt.GridBagLayout());
		
		pnlColFluff.setName("pnlColFluff");
		pnlColFluff.setBackground(Color.RED);
		pnlColFluff.setLayout(new java.awt.GridBagLayout());
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		pnlStats.add(pnlColFluff, gridBagConstraints);
		
		lblCall1.setName("lblCall1"); // NOI18N
		lblCall1.setText("Callsign:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlColFluff.add(lblCall1, gridBagConstraints);
		
		lblCall2.setName("lblCall2"); // NOI18N
		lblCall2.setText(person.getCallsign());
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlColFluff.add(lblCall2, gridBagConstraints);
		
		lblAge1.setName("lblAge1"); // NOI18N
		lblAge1.setText("Age:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlColFluff.add(lblAge1, gridBagConstraints);
		
		lblAge2.setName("lblAge2"); // NOI18N
		lblAge2.setText(Integer.toString(person.getAge(campaign.getCalendar())));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlColFluff.add(lblAge2, gridBagConstraints);
		
		lblGender1.setName("lblGender1"); // NOI18N
		lblGender1.setText("Gender:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlColFluff.add(lblGender1, gridBagConstraints);
		
		lblGender2.setName("lblGender2"); // NOI18N
		lblGender2.setText(person.getGenderName());
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlColFluff.add(lblGender2, gridBagConstraints);
		
		pnlColStats.setName("pnlColStats");
		pnlColStats.setBackground(Color.RED);
		pnlColStats.setLayout(new java.awt.GridBagLayout());
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		pnlStats.add(pnlColStats, gridBagConstraints);
		
		
		
		lblGun1.setName("lblGun1"); // NOI18N
		lblGun1.setText("Gunnery:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlColStats.add(lblGun1, gridBagConstraints);
		
		lblGun2.setName("lblGun2"); // NOI18N
		if(person instanceof PilotPerson) {
			lblGun2.setText(Integer.toString(((PilotPerson)person).getPilot().getGunnery()));
		}
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlColStats.add(lblGun2, gridBagConstraints);
		
    	
    }
}