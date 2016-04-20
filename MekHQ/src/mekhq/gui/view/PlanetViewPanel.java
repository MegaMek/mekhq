/*
 * PlanetViewPanel
 *
 * Created on July 26, 2009, 11:32 PM
 */

package mekhq.gui.view;

import java.awt.Color;
import java.awt.Component;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;

import org.joda.time.DateTime;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Planet;

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
	private javax.swing.JLabel lblPosition;
	private javax.swing.JTextArea txtPosition;
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
	private javax.swing.JLabel lblHPG;
	private javax.swing.JTextArea txtHPG;
	private javax.swing.JLabel lblAnimal;
	private javax.swing.JTextArea txtAnimal;
	private javax.swing.JLabel lblSocioIndustrial;
	private javax.swing.JTextArea txtSocioIndustrial;
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
		pnlStats.setBorder(BorderFactory.createTitledBorder(planet.getPrintableName(new DateTime(campaign.getCalendar()))));
		pnlStats.setBackground(Color.WHITE);
		fillStats();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
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
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlNeighbors, gridBagConstraints);
		
		txtDesc.setName("txtDesc");
		txtDesc.setText(planet.getDescription());
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
		gridBagConstraints.weightx = 1.0;
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
        javax.swing.JLabel lblDistance;
		DateTime currentDate = new DateTime(campaign.getCalendar());
        for(Planet neighbor : campaign.getAllReachablePlanetsFrom(planet)) {
            if(neighbor.equals(planet)) {
                continue;
            }
            lblNeighbor = new javax.swing.JLabel(neighbor.getPrintableName(currentDate) + " (" + neighbor.getFactionDesc(currentDate) + ")");
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlNeighbors.add(lblNeighbor, gridBagConstraints);
            
            lblDistance = new javax.swing.JLabel(String.format(Locale.ROOT, "%.2f ly", planet.getDistanceTo(neighbor)));
            lblDistance.setAlignmentX(Component.RIGHT_ALIGNMENT);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = i;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlNeighbors.add(lblDistance, gridBagConstraints);

            ++ i;
        }		
	}
	
    private void fillStats() {
    	
    	ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PlanetViewPanel", new EncodeControl()); //$NON-NLS-1$
    	
    	lblOwner = new javax.swing.JLabel();
    	lblStarType = new javax.swing.JLabel();
    	txtStarType = new javax.swing.JTextArea();
    	lblPosition = new javax.swing.JLabel();
    	txtPosition = new javax.swing.JTextArea();
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
    	lblHPG = new javax.swing.JLabel();
    	txtHPG = new javax.swing.JTextArea();
    	lblAnimal = new javax.swing.JLabel();
    	txtAnimal = new javax.swing.JTextArea();
    	lblSocioIndustrial = new javax.swing.JLabel();
    	txtSocioIndustrial = new javax.swing.JTextArea();
    	lblLandMass = new javax.swing.JLabel();
    	txtLandMass = new javax.swing.JTextArea();
    	
    	java.awt.GridBagConstraints gridBagConstraints;
		pnlStats.setLayout(new java.awt.GridBagLayout());
		DateTime currentDate = new DateTime(campaign.getCalendar());

		lblOwner.setName("lblOwner"); // NOI18N
		lblOwner.setText("<html><i>" + planet.getFactionDesc(currentDate) + "</i></html>");
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
		txtStarType.setText(planet.getSpectralType() + " (" + planet.getRechargeTime(currentDate) + " hours)");
		txtStarType.setEditable(false);
		txtStarType.setLineWrap(true);
		txtStarType.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		pnlStats.add(txtStarType, gridBagConstraints);
		
		int infoRow = 2;
		if(null != planet.getSystemPosition()) {
    		lblPosition.setName("lblPosition"); // NOI18N
    		lblPosition.setText(resourceMap.getString("lblPosition.text"));
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 0;
    		gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(lblPosition, gridBagConstraints);
    		
    		txtPosition.setName("txtPosition"); // NOI18N
    		txtPosition.setText(planet.getSystemPositionText());
    		txtPosition.setEditable(false);
    		txtPosition.setLineWrap(true);
    		txtPosition.setWrapStyleWord(true);
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 1;
    		gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.weightx = 0.5;
    		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    		pnlStats.add(txtPosition, gridBagConstraints);
    		
    		++ infoRow;
		}
		
		lblJumpPoint.setName("lblJumpPoint"); // NOI18N
		lblJumpPoint.setText(resourceMap.getString("lblJumpPoint1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = infoRow;
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
        gridBagConstraints.gridy = infoRow;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtJumpPoint, gridBagConstraints);
		
		++ infoRow;
		
		lblSatellite.setName("lblSatellite"); // NOI18N
		lblSatellite.setText(resourceMap.getString("lblSatellite1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = infoRow;
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
        gridBagConstraints.gridy = infoRow;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtSatellite, gridBagConstraints);
		
		++ infoRow;
		
		if(null != planet.getGravity()) {
    		lblGravity.setName("lblGravity1"); // NOI18N
    		lblGravity.setText(resourceMap.getString("lblGravity1.text"));
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(lblGravity, gridBagConstraints);
    		
    		txtGravity.setName("lblGravity2"); // NOI18N
    		txtGravity.setText(planet.getGravityText());
    		txtGravity.setEditable(false);
    		txtGravity.setLineWrap(true);
    		txtGravity.setWrapStyleWord(true);
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 1;
    		gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.weightx = 0.5;
    		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(txtGravity, gridBagConstraints);
    		
    		++ infoRow;
		}
		
		if(null != planet.getPressure(currentDate)) {
    		lblPressure.setName("lblPressure1"); // NOI18N
    		lblPressure.setText(resourceMap.getString("lblPressure1.text"));
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(lblPressure, gridBagConstraints);
    		
    		txtPressure.setName("lblPressure2"); // NOI18N
    		txtPressure.setText(planet.getPressureName(currentDate));
    		txtPressure.setEditable(false);
    		txtPressure.setLineWrap(true);
    		txtPressure.setWrapStyleWord(true);
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.weightx = 0.5;
    		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(txtPressure, gridBagConstraints);
    		
            ++ infoRow;
		}
		
		if((null != planet.getTemperature(currentDate)) || (null != planet.getClimate(currentDate))) {
    		lblTemp.setName("lblTemp1"); // NOI18N
    		lblTemp.setText(resourceMap.getString("lblTemp1.text"));
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(lblTemp, gridBagConstraints);
    		
    		txtTemp.setName("lblTemp2"); // NOI18N
    	    String text;
    	    if( null == planet.getClimate(currentDate) ) {
                text = planet.getTemperature(currentDate) + "°C";
            } else if( null == planet.getTemperature(currentDate) ) {
                text = "(" + planet.getClimateName(currentDate) + ")";
            } else {
                text = planet.getTemperature(currentDate) + "°C (" + planet.getClimateName(currentDate) + ")";
            }
            txtTemp.setText(text);
    		txtTemp.setEditable(false);
    		txtTemp.setLineWrap(true);
    		txtTemp.setWrapStyleWord(true);
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.weightx = 0.5;
    		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(txtTemp, gridBagConstraints);
    		
    		++ infoRow;
		}
		
		if(null != planet.getPercentWater(currentDate)) {
    		lblWater.setName("lblWater1"); // NOI18N
    		lblWater.setText(resourceMap.getString("lblWater1.text"));
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(lblWater, gridBagConstraints);
    		
    		txtWater.setName("lblWater2"); // NOI18N
    		txtWater.setText(planet.getPercentWater(currentDate) + " percent");
    		txtWater.setEditable(false);
    		txtWater.setLineWrap(true);
    		txtWater.setWrapStyleWord(true);
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.weightx = 0.5;
    		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(txtWater, gridBagConstraints);
    		
    		++ infoRow;
		}
		
		lblRecharge.setName("lblRecharge1"); // NOI18N
		lblRecharge.setText(resourceMap.getString("lblRecharge1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = infoRow;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblRecharge, gridBagConstraints);
		
		txtRecharge.setName("lblRecharge2"); // NOI18N
		txtRecharge.setText(planet.getRechargeStationsText(currentDate));
		txtRecharge.setEditable(false);
		txtRecharge.setLineWrap(true);
		txtRecharge.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = infoRow;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtRecharge, gridBagConstraints);
		
        ++ infoRow;
		
        if(null != planet.getHPGClass(currentDate)) {
    		lblHPG.setName("lblHPG1"); // NOI18N
    		lblHPG.setText(resourceMap.getString("lblHPG1.text"));
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(lblHPG, gridBagConstraints);
    		
    		txtHPG.setName("lblHPG2"); // NOI18N
    		txtHPG.setText(planet.getHPGClass(currentDate));
    		txtHPG.setEditable(false);
    		txtHPG.setLineWrap(true);
    		txtHPG.setWrapStyleWord(true);
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.weightx = 0.5;
    		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(txtHPG, gridBagConstraints);
    		
            ++ infoRow;
        }

        if(null != planet.getLifeForm(currentDate)) {
    		lblAnimal.setName("lblAnimal1"); // NOI18N
    		lblAnimal.setText(resourceMap.getString("lblAnimal1.text"));
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(lblAnimal, gridBagConstraints);
    		
    		txtAnimal.setName("lblAnimal2"); // NOI18N
    		txtAnimal.setText(planet.getLifeFormName(currentDate));
    		txtAnimal.setEditable(false);
    		txtAnimal.setLineWrap(true);
    		txtAnimal.setWrapStyleWord(true);
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.weightx = 0.5;
    		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(txtAnimal, gridBagConstraints);
    		
            ++ infoRow;
        }
        
        if(null != planet.getSocioIndustrial(currentDate)) {
    		lblSocioIndustrial.setName("lblSocioIndustrial1"); // NOI18N
    		lblSocioIndustrial.setText(resourceMap.getString("lblSocioIndustrial1.text"));
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(lblSocioIndustrial, gridBagConstraints);
    		
    		txtSocioIndustrial.setName("lblSocioIndustrial2"); // NOI18N
    		txtSocioIndustrial.setText(planet.getSocioIndustrialText(currentDate));
    		txtSocioIndustrial.setEditable(false);
    		txtSocioIndustrial.setLineWrap(true);
    		txtSocioIndustrial.setWrapStyleWord(true);
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.weightx = 0.5;
    		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(txtSocioIndustrial, gridBagConstraints);
    		
            ++ infoRow;
        }
        
        if(null != planet.getLandMasses()) {
    		lblLandMass.setName("lblLandMass1"); // NOI18N
    		lblLandMass.setText(resourceMap.getString("lblLandMass1.text"));
    		gridBagConstraints = new java.awt.GridBagConstraints();
    		gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = infoRow;
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
            gridBagConstraints.gridy = infoRow;
    		gridBagConstraints.weightx = 0.5;
    		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		pnlStats.add(txtLandMass, gridBagConstraints);
    		
            ++ infoRow;
        }
    }
}