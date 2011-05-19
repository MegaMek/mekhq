/*
 * PlanetViewPanel
 *
 * Created on July 26, 2009, 11:32 PM
 */

package mekhq;

import java.awt.Color;

import javax.swing.BorderFactory;

import mekhq.campaign.Campaign;
import mekhq.campaign.Faction;
import mekhq.campaign.Planet;

/**
 * A custom panel that gets filled in with goodies from a Planet record
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class PlanetViewPanel extends javax.swing.JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7004741688464105277L;

	private Planet planet;
	private Campaign campaign;
	
	private javax.swing.JPanel pnlNeighbors;
	private javax.swing.JPanel pnlStats;
	private javax.swing.JTextArea txtDesc;
	
	private javax.swing.JLabel lblOwner;
	private javax.swing.JLabel lblStarType;
	private javax.swing.JTextArea txtStarType;
	private javax.swing.JLabel lblJumpPoint;
	private javax.swing.JTextArea txtJumpPoint;
	private javax.swing.JLabel lblSatellite;
	private javax.swing.JTextArea txtSatellite;
	private javax.swing.JLabel lblGravity;
	private javax.swing.JTextArea txtGravity;
	private javax.swing.JLabel lblPressure;
	private javax.swing.JTextArea txtPressure;
	private javax.swing.JLabel lblTemp;
	private javax.swing.JTextArea txtTemp;
	private javax.swing.JLabel lblWater;
	private javax.swing.JTextArea txtWater;
	private javax.swing.JLabel lblRecharge;
	private javax.swing.JTextArea txtRecharge;
	private javax.swing.JLabel lblAnimal;
	private javax.swing.JTextArea txtAnimal;
	private javax.swing.JLabel lblLandMass;
	private javax.swing.JTextArea txtLandMass;

	
	public PlanetViewPanel(Planet p, Campaign c) {
		this.planet = p;
		this.campaign = c;
		initComponents();
	}
	
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		pnlStats = new javax.swing.JPanel();
		pnlNeighbors = new javax.swing.JPanel();
		txtDesc = new javax.swing.JTextArea();
		       
		setLayout(new java.awt.GridBagLayout());

		setBackground(Color.WHITE);

		pnlStats.setName("pnlStats");
		pnlStats.setBorder(BorderFactory.createTitledBorder(planet.getShortName()));
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
		
		pnlNeighbors.setName("pnlNeighbors");
		pnlNeighbors.setBorder(BorderFactory.createTitledBorder("Planets within 30 light years"));
		pnlNeighbors.setBackground(Color.WHITE);
		getNeighbors();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridheight = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlNeighbors, gridBagConstraints);
		
		txtDesc.setName("txtDesc");
		txtDesc.setText("Nothing here yet. Who wants to volunteer to enter planet data?");
		txtDesc.setEditable(false);
		txtDesc.setLineWrap(true);
		txtDesc.setWrapStyleWord(true);
		txtDesc.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Description"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		add(txtDesc, gridBagConstraints);
	}

	private void getNeighbors() {
		java.awt.GridBagConstraints gridBagConstraints;
		pnlNeighbors.setLayout(new java.awt.GridBagLayout());
		int i = 0;
		javax.swing.JLabel lblNeighbor;
		for(String neighborKey : campaign.getAllReachablePlanetsFrom(planet)) {
			Planet neighbor = campaign.getPlanet(neighborKey);
			lblNeighbor = new javax.swing.JLabel(neighbor.getShortName() + " (" + Faction.getFactionName(neighbor.getCurrentFaction(campaign.getCalendar().getTime())) + ")");
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = i;
			gridBagConstraints.gridwidth = 1;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.weighty = 1.0;
			gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlNeighbors.add(lblNeighbor, gridBagConstraints);
			i++;
		}
		
	}
	
    private void fillStats() {
    	
    	org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mekhq.MekHQApp.class).getContext().getResourceMap(PlanetViewPanel.class);
    	
    	lblOwner = new javax.swing.JLabel();
    	lblStarType = new javax.swing.JLabel();
    	txtStarType = new javax.swing.JTextArea();
    	lblSatellite = new javax.swing.JLabel();
    	txtSatellite = new javax.swing.JTextArea();
    	lblJumpPoint = new javax.swing.JLabel();
    	txtJumpPoint = new javax.swing.JTextArea();
    	lblGravity = new javax.swing.JLabel();
    	txtGravity = new javax.swing.JTextArea();
    	lblPressure = new javax.swing.JLabel();
    	txtPressure = new javax.swing.JTextArea();
    	lblTemp = new javax.swing.JLabel();
    	txtTemp = new javax.swing.JTextArea();
    	lblWater = new javax.swing.JLabel();
    	txtWater = new javax.swing.JTextArea();
    	lblRecharge = new javax.swing.JLabel();
    	txtRecharge = new javax.swing.JTextArea();
    	lblAnimal = new javax.swing.JLabel();
    	txtAnimal = new javax.swing.JTextArea();
    	lblLandMass = new javax.swing.JLabel();
    	txtLandMass = new javax.swing.JTextArea();
    	
    	java.awt.GridBagConstraints gridBagConstraints;
		pnlStats.setLayout(new java.awt.GridBagLayout());
		
		lblOwner.setName("lblOwner"); // NOI18N
		lblOwner.setText("<html><i>" + Faction.getFactionName(planet.getCurrentFaction(campaign.getCalendar().getTime())) + "</i></html>");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblOwner, gridBagConstraints);
		
		lblStarType.setName("lblStarType"); // NOI18N
		lblStarType.setText(resourceMap.getString("lblStarType1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblStarType, gridBagConstraints);
		
		txtStarType.setName("lblStarType2"); // NOI18N
		txtStarType.setText(planet.getStarType() + " (" + planet.getRechargeTime() + " hours)");
		txtStarType.setEditable(false);
		txtStarType.setLineWrap(true);
		txtStarType.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtStarType, gridBagConstraints);
		
		lblJumpPoint.setName("lblJumpPoint"); // NOI18N
		lblJumpPoint.setText(resourceMap.getString("lblJumpPoint1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblJumpPoint, gridBagConstraints);
		
		txtJumpPoint.setName("lblJumpPoint2"); // NOI18N
		txtJumpPoint.setText(Double.toString(Math.round(100 * planet.getTimeToJumpPoint(1))/100.0) + " days");
		txtJumpPoint.setEditable(false);
		txtJumpPoint.setLineWrap(true);
		txtJumpPoint.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtJumpPoint, gridBagConstraints);
		
		lblSatellite.setName("lblSatellite"); // NOI18N
		lblSatellite.setText(resourceMap.getString("lblSatellite1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblSatellite, gridBagConstraints);
		
		txtSatellite.setName("lblSatellite2"); // NOI18N
		txtSatellite.setText(planet.getSatelliteDescription());
		txtSatellite.setEditable(false);
		txtSatellite.setLineWrap(true);
		txtSatellite.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtSatellite, gridBagConstraints);
		
		lblGravity.setName("lblGravity1"); // NOI18N
		lblGravity.setText(resourceMap.getString("lblGravity1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblGravity, gridBagConstraints);
		
		txtGravity.setName("lblGravity2"); // NOI18N
		txtGravity.setText(Double.toString(planet.getGravity()));
		txtGravity.setEditable(false);
		txtGravity.setLineWrap(true);
		txtGravity.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtGravity, gridBagConstraints);
		
		lblPressure.setName("lblPressure1"); // NOI18N
		lblPressure.setText(resourceMap.getString("lblPressure1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblPressure, gridBagConstraints);
		
		txtPressure.setName("lblPressure2"); // NOI18N
		txtPressure.setText(planet.getPressureName());
		txtPressure.setEditable(false);
		txtPressure.setLineWrap(true);
		txtPressure.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtPressure, gridBagConstraints);
		
		lblTemp.setName("lblTemp1"); // NOI18N
		lblTemp.setText(resourceMap.getString("lblTemp1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 6;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblTemp, gridBagConstraints);
		
		txtTemp.setName("lblTemp2"); // NOI18N
		txtTemp.setText(planet.getTemperature() + "C (" + planet.getClimateName() + ")");
		txtTemp.setEditable(false);
		txtTemp.setLineWrap(true);
		txtTemp.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 6;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtTemp, gridBagConstraints);
		
		lblWater.setName("lblWater1"); // NOI18N
		lblWater.setText(resourceMap.getString("lblWater1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 7;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblWater, gridBagConstraints);
		
		txtWater.setName("lblWater2"); // NOI18N
		txtWater.setText(planet.getPercentWater() + " percent");
		txtWater.setEditable(false);
		txtWater.setLineWrap(true);
		txtWater.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 7;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtWater, gridBagConstraints);
		
		lblRecharge.setName("lblRecharge1"); // NOI18N
		lblRecharge.setText(resourceMap.getString("lblRecharge1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 8;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblRecharge, gridBagConstraints);
		
		txtRecharge.setName("lblRecharge2"); // NOI18N
		txtRecharge.setText(planet.getRechargeStations());
		txtRecharge.setEditable(false);
		txtRecharge.setLineWrap(true);
		txtRecharge.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 8;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtRecharge, gridBagConstraints);
		
		lblAnimal.setName("lblAnimal1"); // NOI18N
		lblAnimal.setText(resourceMap.getString("lblAnimal1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 9;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblAnimal, gridBagConstraints);
		
		txtAnimal.setName("lblAnimal2"); // NOI18N
		txtAnimal.setText(planet.getLifeFormName());
		txtAnimal.setEditable(false);
		txtAnimal.setLineWrap(true);
		txtAnimal.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 9;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtAnimal, gridBagConstraints);
		
		lblLandMass.setName("lblLandMass1"); // NOI18N
		lblLandMass.setText(resourceMap.getString("lblLandMass1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 10;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblLandMass, gridBagConstraints);
		
		txtLandMass.setName("lblLandMass2"); // NOI18N
		txtLandMass.setText(planet.getLandMassDescription());
		txtLandMass.setEditable(false);
		txtLandMass.setLineWrap(true);
		txtLandMass.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 10;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtLandMass, gridBagConstraints);
		
    }
}