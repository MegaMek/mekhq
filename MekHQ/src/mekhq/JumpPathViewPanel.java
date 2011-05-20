/*
 * JumpPathViewPanel
 *
 * Created on July 26, 2009, 11:32 PM
 */

package mekhq;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.BorderFactory;

import mekhq.campaign.Campaign;
import mekhq.campaign.Faction;
import mekhq.campaign.Planet;

/**
 * A custom panel that gets filled in with goodies from a JumpPath record
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class JumpPathViewPanel extends javax.swing.JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7004741688464105277L;

	private ArrayList<Planet> path;
	private Campaign campaign;
	
	private int jumps;
	private double startTime;
	private double endTime;
	private double rechargeTime;
	private double totalTime;
	
	private javax.swing.JPanel pnlPath;
	private javax.swing.JPanel pnlStats;

	private javax.swing.JLabel lblJumps;
	private javax.swing.JTextArea txtJumps;
	private javax.swing.JLabel lblTimeStart;
	private javax.swing.JTextArea txtTimeStart;
	private javax.swing.JLabel lblTimeEnd;
	private javax.swing.JTextArea txtTimeEnd;
	private javax.swing.JLabel lblRechargeTime;
	private javax.swing.JTextArea txtRechargeTime;
	private javax.swing.JLabel lblTotalTime;
	private javax.swing.JTextArea txtTotalTime;

	public JumpPathViewPanel(ArrayList<Planet> p, Campaign c) {
		this.path = p;
		this.campaign = c;
		initComponents();
	}
	
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		pnlStats = new javax.swing.JPanel();
		pnlPath = new javax.swing.JPanel();
		       
		setLayout(new java.awt.GridBagLayout());

		setBackground(Color.WHITE);

		pnlStats.setName("pnlStats");
		pnlStats.setBorder(BorderFactory.createTitledBorder("Summary"));
		pnlStats.setBackground(Color.WHITE);
		fillStats();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlStats, gridBagConstraints);
		
		pnlPath.setName("pnlPath");
		pnlPath.setBorder(BorderFactory.createTitledBorder("Full Path"));
		pnlPath.setBackground(Color.WHITE);
		getPath();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.gridheight = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlPath, gridBagConstraints);
	}

	private void getPath() {
		java.awt.GridBagConstraints gridBagConstraints;
		pnlPath.setLayout(new java.awt.GridBagLayout());
		int i = 0;
		javax.swing.JLabel lblPlanet;
		for(Planet planet : path) {
			lblPlanet = new javax.swing.JLabel(planet.getShortName() + " (" + planet.getRechargeTime() + " hours)");
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = i;
			gridBagConstraints.gridwidth = 1;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.weighty = 1.0;
			gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlPath.add(lblPlanet, gridBagConstraints);
			i++;
		}
		
	}
	
    private void fillStats() {
    	
    	org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mekhq.MekHQApp.class).getContext().getResourceMap(JumpPathViewPanel.class);
    	
    	lblJumps = new javax.swing.JLabel();
    	txtJumps = new javax.swing.JTextArea();
    	lblTimeStart = new javax.swing.JLabel();
    	txtTimeStart = new javax.swing.JTextArea();
    	lblTimeEnd = new javax.swing.JLabel();
    	txtTimeEnd = new javax.swing.JTextArea();
    	lblRechargeTime = new javax.swing.JLabel();
    	txtRechargeTime = new javax.swing.JTextArea();
    	lblTotalTime = new javax.swing.JLabel();
    	txtTotalTime = new javax.swing.JTextArea();
    	
    	String startName = "?";
    	String endName = "?";
    	jumps = path.size();
    	if(!path.isEmpty()) {
    		startTime = path.get(0).getTimeToJumpPoint(1.0);
    		endTime = path.get(path.size() - 1).getTimeToJumpPoint(1.0);
    		startName = path.get(0).getShortName();
    		endName = path.get(path.size() - 1).getShortName();
    	} else {
    		startTime = 0;
    		endTime = 0;
    	}
    	rechargeTime = 0.0;
    	for(Planet planet : path) {
    		rechargeTime += planet.getRechargeTime();
    	}
    	rechargeTime = rechargeTime/24.0;
    	totalTime = startTime + endTime + rechargeTime;
    	
    	startTime = Math.round(startTime*100.0)/100.0;
    	endTime = Math.round(endTime*100.0)/100.0;
    	rechargeTime = Math.round(rechargeTime*100.0)/100.0;
    	totalTime = Math.round(totalTime*100.0)/100.0;
    	
    	java.awt.GridBagConstraints gridBagConstraints;
		pnlStats.setLayout(new java.awt.GridBagLayout());
		
		lblJumps.setName("lblJumps"); // NOI18N
		lblJumps.setText(resourceMap.getString("lblJumps1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblJumps, gridBagConstraints);
		
		txtJumps.setName("lblJumps2"); // NOI18N
		txtJumps.setText(jumps + " jumps");
		txtJumps.setEditable(false);
		txtJumps.setLineWrap(true);
		txtJumps.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtJumps, gridBagConstraints);
		
		lblTimeStart.setName("lblTimeStart"); // NOI18N
		lblTimeStart.setText(resourceMap.getString("lblTimeStart1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblTimeStart, gridBagConstraints);
		
		txtTimeStart.setName("lblTimeStart2"); // NOI18N
		txtTimeStart.setText(startTime + " days from "+ startName + " to jump point");
		txtTimeStart.setEditable(false);
		txtTimeStart.setLineWrap(true);
		txtTimeStart.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtTimeStart, gridBagConstraints);
		
		lblTimeEnd.setName("lblTimeEnd"); // NOI18N
		lblTimeEnd.setText(resourceMap.getString("lblTimeEnd1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblTimeEnd, gridBagConstraints);
		
		txtTimeEnd.setName("lblTimeEnd2"); // NOI18N
		txtTimeEnd.setText(endTime + " days from final jump point to " + endName);
		txtTimeEnd.setEditable(false);
		txtTimeEnd.setLineWrap(true);
		txtTimeEnd.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtTimeEnd, gridBagConstraints);
		
		lblRechargeTime.setName("lblRechargeTime1"); // NOI18N
		lblRechargeTime.setText(resourceMap.getString("lblRechargeTime1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblRechargeTime, gridBagConstraints);
		
		txtRechargeTime.setName("lblRechargeTime2"); // NOI18N
		txtRechargeTime.setText(rechargeTime + " days");
		txtRechargeTime.setEditable(false);
		txtRechargeTime.setLineWrap(true);
		txtRechargeTime.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtRechargeTime, gridBagConstraints);
		
		lblTotalTime.setName("lblTotalTime1"); // NOI18N
		lblTotalTime.setText(resourceMap.getString("lblTotalTime1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblTotalTime, gridBagConstraints);
		
		txtTotalTime.setName("lblTotalTime2"); // NOI18N
		txtTotalTime.setText(totalTime + " days");
		txtTotalTime.setEditable(false);
		txtTotalTime.setLineWrap(true);
		txtTotalTime.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtTotalTime, gridBagConstraints);
	
    }
}