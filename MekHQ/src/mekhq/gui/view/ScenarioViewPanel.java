/*
 * ScenarioViewPanel
 *
 * Created on July 26, 2009, 11:32 PM
 */

package mekhq.gui.view;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import mekhq.campaign.Campaign;
import mekhq.campaign.Force;
import mekhq.campaign.Unit;
import mekhq.campaign.mission.Scenario;
import mekhq.gui.CampaignGUI;
import mekhq.gui.CampaignGUI.ForceRenderer;
import mekhq.gui.CampaignGUI.OrgTreeModel;

/**
 * A custom panel that gets filled in with goodies from a scenario object
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ScenarioViewPanel extends javax.swing.JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7004741688464105277L;

	private Scenario scenario;
	private Campaign campaign;
	private Force forces;
	private CampaignGUI view;
	
	private javax.swing.JPanel pnlStats;
	private javax.swing.JTextArea txtDesc;
	private javax.swing.JTextArea txtReport;
	private javax.swing.JTree forceTree;
	private javax.swing.JLabel lblStatus;
	
	private OrgTreeModel forceModel;
	
	public ScenarioViewPanel(Scenario s, Campaign c, CampaignGUI v) {
		this.scenario = s;
		this.campaign = c;
		this.forces = s.getForces(campaign);
		this.view = v;
		forceModel = view.new OrgTreeModel(forces);
		initComponents();
	}
	
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		pnlStats = new javax.swing.JPanel();
		txtDesc = new javax.swing.JTextArea();
		txtReport = new javax.swing.JTextArea();
		forceTree = new javax.swing.JTree();
		       
		setLayout(new java.awt.GridBagLayout());

		setBackground(Color.WHITE);

		pnlStats.setName("pnlStats");
		pnlStats.setBorder(BorderFactory.createTitledBorder(scenario.getName()));
		pnlStats.setBackground(Color.WHITE);
		fillStats();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 1;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlStats, gridBagConstraints);
		
		if(forces.getAllUnits().size() > 0) {
			forceTree.setModel(forceModel);
			//forceTree.addMouseListener(orgMouseAdapter);
			forceTree.setCellRenderer(view.new ForceRenderer());
			forceTree.setRowHeight(50);
			forceTree.setRootVisible(false);
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 1;
			gridBagConstraints.gridheight = 1;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
			gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
			add(forceTree, gridBagConstraints);
		}
		
		txtReport.setName("txtReport");
		txtReport.setText(scenario.getReport());
		txtReport.setEditable(false);
		txtReport.setLineWrap(true);
		txtReport.setWrapStyleWord(true);
		txtReport.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("After-Action Report"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(txtReport, gridBagConstraints);
	}

    private void fillStats() {
    	    	
    	lblStatus = new javax.swing.JLabel();
    	
    	java.awt.GridBagConstraints gridBagConstraints;
		pnlStats.setLayout(new java.awt.GridBagLayout());
		
		lblStatus.setName("lblOwner"); // NOI18N
		lblStatus.setText("<html><b>" + scenario.getStatusName() + "</b></html>");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblStatus, gridBagConstraints);
		
		txtDesc.setName("txtDesc");
		txtDesc.setText(scenario.getDescription());
		txtDesc.setEditable(false);
		txtDesc.setLineWrap(true);
		txtDesc.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtDesc, gridBagConstraints);
		
		
    }
}