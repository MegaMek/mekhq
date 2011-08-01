/*
 * PersonViewPanel
 *
 * Created on July 26, 2009, 11:32 PM
 */

package mekhq.gui.view;

import java.awt.Color;
import java.awt.Image;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import megamek.common.Pilot;
import megamek.common.options.PilotOptions;
import megamek.common.util.DirectoryItems;
import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;

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
	private javax.swing.JTextArea txtKills;

	private javax.swing.JLabel lblType;
	private javax.swing.JLabel lblCall1;
	private javax.swing.JLabel lblCall2;
	private javax.swing.JLabel lblAge1;
	private javax.swing.JLabel lblAge2;
	private javax.swing.JLabel lblGender1;
	private javax.swing.JLabel lblGender2;
	private javax.swing.JLabel lblStatus1;
	private javax.swing.JLabel lblStatus2;
	private javax.swing.JLabel lblTough1;
	private javax.swing.JLabel lblTough2;
	private javax.swing.JLabel lblEdge1;
	private javax.swing.JLabel lblEdge2;
	private javax.swing.JLabel lblAbility1;
	private javax.swing.JLabel lblAbility2;
	private javax.swing.JLabel lblImplants1;
	private javax.swing.JLabel lblImplants2;
	
	
	public PersonViewPanel(Person p, Campaign c, DirectoryItems portraits) {
		this.person = p;
		this.campaign = c;
		this.portraits = portraits;
		initComponents();
	}
	
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		lblPortrait = new javax.swing.JLabel();
		lblUnit = new javax.swing.JLabel();
		pnlStats = new javax.swing.JPanel();
		txtDesc = new javax.swing.JTextArea();
		txtKills = new javax.swing.JTextArea();

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
		pnlStats.setBorder(BorderFactory.createTitledBorder(person.getFullTitle()));
		pnlStats.setBackground(Color.WHITE);
		fillStats();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlStats, gridBagConstraints);
		
		txtDesc.setName("txtDesc");
		txtDesc.setText(person.getBiography());
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
		
		ArrayList<Kill> kills = campaign.getKillsFor(person.getId());
		if(!kills.isEmpty()) {
			String killRecord = "Kills: " + kills.size();
	    	SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
			for(Kill k : campaign.getKillsFor(person.getId())) {
				killRecord += "\n " + k.getWhatKilled() + " on " + dateFormat.format(k.getDate()) + " with " + k.getKilledByWhat();
			}
			txtKills.setName("txtKills");
			txtKills.setText(killRecord);
			txtKills.setEditable(false);
			txtKills.setLineWrap(true);
			txtKills.setWrapStyleWord(true);
			txtKills.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Kill Record"),
	                BorderFactory.createEmptyBorder(5,5,5,5)));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 3;
			gridBagConstraints.gridwidth = 2;
			gridBagConstraints.weighty = 1.0;
			gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
			gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			add(txtKills, gridBagConstraints);
		}
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
        	file = "default.gif";
        }

        // Try to get the player's portrait file.
        Image portrait = null;
        try {
            portrait = (Image) portraits.getItem(category, file);
            //make sure no images are longer than 150 pixels
            if(null != portrait && portrait.getWidth(lblPortrait) > 150) {
                portrait = portrait.getScaledInstance(150, -1, Image.SCALE_DEFAULT);               
            }
            lblPortrait.setIcon(new ImageIcon(portrait));
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
    
    private void fillStats() {
    	
    	ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PersonViewPanel");
    	
    	lblType = new javax.swing.JLabel();
    	lblCall1 = new javax.swing.JLabel();
		lblCall2 = new javax.swing.JLabel();
		lblAge1 = new javax.swing.JLabel();
		lblAge2 = new javax.swing.JLabel();
		lblGender1 = new javax.swing.JLabel();
		lblGender2 = new javax.swing.JLabel();
		lblStatus1 = new javax.swing.JLabel();
		lblStatus2 = new javax.swing.JLabel();
		lblTough1 = new javax.swing.JLabel();
		lblTough2 = new javax.swing.JLabel();
		lblEdge1 = new javax.swing.JLabel();
		lblEdge2 = new javax.swing.JLabel();
		lblAbility1 = new javax.swing.JLabel();
		lblAbility2 = new javax.swing.JLabel();
		lblImplants1 = new javax.swing.JLabel();
		lblImplants2 = new javax.swing.JLabel();
    	
    	java.awt.GridBagConstraints gridBagConstraints;
		pnlStats.setLayout(new java.awt.GridBagLayout());
		
		lblType.setName("lblType"); // NOI18N
		lblType.setText("<html><i>" + person.getRoleDesc() + "</i></html>");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 4;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblType, gridBagConstraints);
		
		int firsty = 0;
		
		if(!person.getCallsign().equals("-")) {
			firsty++;
			lblCall1.setName("lblCall1"); // NOI18N
			lblCall1.setText(resourceMap.getString("lblCall1.text"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = firsty;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblCall1, gridBagConstraints);
			
			lblCall2.setName("lblCall2"); // NOI18N
			lblCall2.setText(person.getCallsign());
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = firsty;
			gridBagConstraints.weightx = 0.5;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblCall2, gridBagConstraints);
		}
			
		firsty++;
		lblAge1.setName("lblAge1"); // NOI18N
		lblAge1.setText(resourceMap.getString("lblAge1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = firsty;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblAge1, gridBagConstraints);
		
		lblAge2.setName("lblAge2"); // NOI18N
		lblAge2.setText(Integer.toString(person.getAge(campaign.getCalendar())));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = firsty;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblAge2, gridBagConstraints);
		
		firsty++;
		lblGender1.setName("lblGender1"); // NOI18N
		lblGender1.setText(resourceMap.getString("lblGender1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = firsty;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblGender1, gridBagConstraints);
		
		lblGender2.setName("lblGender2"); // NOI18N
		lblGender2.setText(person.getGenderName());
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = firsty;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblGender2, gridBagConstraints);
		
		firsty++;
		lblStatus1.setName("lblStatus1"); // NOI18N
		lblStatus1.setText(resourceMap.getString("lblStatus1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = firsty;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblStatus1, gridBagConstraints);
		
		lblStatus2.setName("lblStatus2"); // NOI18N
		lblStatus2.setText(person.getStatusName());
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = firsty;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblStatus2, gridBagConstraints);
		
		int secondy = 0;
		JLabel lblName;
		JLabel lblValue;
		
		for(int i = 0; i < SkillType.getSkillList().length; i++) {
			if(person.hasSkill(SkillType.getSkillList()[i])) {
				secondy++;
				lblName = new JLabel("<html><b>" + SkillType.getSkillList()[i] + ":</b></html>");
				lblValue = new JLabel(person.getSkill(SkillType.getSkillList()[i]).toString());
				gridBagConstraints = new java.awt.GridBagConstraints();
				gridBagConstraints.gridx = 2;
				gridBagConstraints.gridy = secondy;
				gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
				gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
				pnlStats.add(lblName, gridBagConstraints);
				gridBagConstraints = new java.awt.GridBagConstraints();
				gridBagConstraints.gridx = 3;
				gridBagConstraints.gridy = secondy;
				gridBagConstraints.weightx = 0.5;
				gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
				gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
				gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
				pnlStats.add(lblValue, gridBagConstraints);
			}
		}
		
		if(campaign.getCampaignOptions().useToughness()) {
			secondy++;
			lblTough1.setName("lblTough1"); // NOI18N
			lblTough1.setText(resourceMap.getString("lblTough1.text"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 2;
			gridBagConstraints.gridy = secondy;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblTough1, gridBagConstraints);
				
			lblTough2.setName("lblTough2"); // NOI18N
			lblTough2.setText("+" + Integer.toString(person.getToughness()));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 3;
			gridBagConstraints.gridy = secondy;
			gridBagConstraints.weightx = 0.5;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblTough2, gridBagConstraints);
		}		
		if(campaign.getCampaignOptions().useEdge()) {
			secondy++;
			lblEdge1.setName("lblEdge1"); // NOI18N
			lblEdge1.setText(resourceMap.getString("lblEdge1.text"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 2;
			gridBagConstraints.gridy = secondy;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblEdge1, gridBagConstraints);
				
			lblEdge2.setName("lblEdge2"); // NOI18N
			lblEdge2.setText(Integer.toString(person.getEdge()));
			lblEdge2.setToolTipText(person.getEdgeTooltip());
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 3;
			gridBagConstraints.gridy = secondy;
			gridBagConstraints.weightx = 0.5;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblEdge2, gridBagConstraints);
		}
			
		//special abilities and implants need to be three columns wide to handle their large width
		if(firsty > secondy) {
			secondy = firsty;
		}
			
		if(campaign.getCampaignOptions().useAbilities() && person.countOptions(PilotOptions.LVL3_ADVANTAGES) > 0) {
			secondy++;
			lblAbility1.setName("lblAbility1"); // NOI18N
			lblAbility1.setText(resourceMap.getString("lblAbility1.text"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = secondy;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblAbility1, gridBagConstraints);
				
			lblAbility2.setName("lblAbility2"); // NOI18N
			lblAbility2.setText(person.getAbilityList(PilotOptions.LVL3_ADVANTAGES));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = secondy;
			gridBagConstraints.gridwidth = 3;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblAbility2, gridBagConstraints);
		}
			
		if(campaign.getCampaignOptions().useImplants() && person.countOptions(PilotOptions.MD_ADVANTAGES) > 0) {
			secondy++;
			lblImplants1.setName("lblImplants1"); // NOI18N
			lblImplants1.setText(resourceMap.getString("lblImplants1.text"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = secondy;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblImplants1, gridBagConstraints);
				
			lblImplants2.setName("lblImplants2"); // NOI18N
			lblImplants2.setText(person.getAbilityList(PilotOptions.MD_ADVANTAGES));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = secondy;
			gridBagConstraints.gridwidth = 3;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblImplants2, gridBagConstraints);
		}    	
    }
}