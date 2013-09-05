/*
 * ResolveScenarioWizardDialog.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 * 
 * This file is part of MekHQ.
 * 
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;

import megamek.common.Entity;
import megamek.common.GunEmplacement;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.ResolveScenarioTracker.PersonStatus;
import mekhq.campaign.Unit;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Scenario;

/**
 *
 * @author  Taharqa
 */
public class ResolveScenarioWizardDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
	
	final static String UNITSPANEL   = "Missing Units";
	final static String PILOTPANEL   = "Pilot Status";
	final static String SALVAGEPANEL = "Claim Salvage";
	final static String KILLPANEL    = "Assign Kills";
	final static String PREVIEWPANEL = "Preview";

	final static String[] panelOrder = {UNITSPANEL,PILOTPANEL,SALVAGEPANEL,KILLPANEL,PREVIEWPANEL};
	
	private ResolveScenarioTracker tracker;
    
	private javax.swing.JPanel panButtons;
	private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnNext;
    private javax.swing.JButton btnFinish;
    private javax.swing.JButton btnBack;
    private javax.swing.JTextArea txtInstructions;
    
    private CardLayout cardLayout;
    private String currentPanel;
    
    private JScrollPane scrMain;
    private JPanel pnlMain;
    private JPanel pnlMissingUnits;
    private JPanel pnlPilotStatus;
    private JPanel pnlSalvage;
    private JPanel pnlKills;
    private JPanel pnlPreview;
 
    /*
     * Missing units panel components
     */
    private ArrayList<JCheckBox> unitBoxes;
    private ArrayList<Unit> missingUnits;
    
    /*
     * Pilot status panel components
     */
    private ArrayList<JCheckBox> miaBtns;
    private ArrayList<JSlider> hitSliders;
    private ArrayList<PersonStatus> statuses;
    
    /*
     * Salvage panel components
     */
    private ArrayList<javax.swing.JCheckBox> salvageBoxes;
    private ArrayList<Unit> salvageables;
    
    private JLabel lblSalvageValueUnit1;
    private JLabel lblSalvageValueEmployer1;
    private JLabel lblSalvagePct1;
    private JLabel lblSalvageValueUnit2;
    private JLabel lblSalvageValueEmployer2;
    private JLabel lblSalvagePct2;
	
    private long salvageEmployer;
    private long salvageUnit;
    private int currentSalvagePct;
    private int maxSalvagePct;
    
    DecimalFormat formatter;
    
    /*
     * Assign Kills components
     */
    private Hashtable<String, JComboBox> killChoices;
    
    /*
     * Preview Panel components
     */
    private javax.swing.JComboBox choiceStatus;
    private javax.swing.JScrollPane scrReport;
    private javax.swing.JScrollPane scrRecoveredUnits;
    private javax.swing.JScrollPane scrRecoveredPilots;
    private javax.swing.JScrollPane scrMissingUnits;
    private javax.swing.JScrollPane scrMissingPilots;
    private javax.swing.JScrollPane scrDeadPilots;
    private javax.swing.JScrollPane scrSalvage;
    private javax.swing.JTextArea txtReport;
    private javax.swing.JTextArea txtRecoveredUnits;
    private javax.swing.JTextArea txtRecoveredPilots;
    private javax.swing.JTextArea txtMissingUnits;
    private javax.swing.JTextArea txtMissingPilots;
    private javax.swing.JTextArea txtDeadPilots;
    private javax.swing.JTextArea txtSalvage;
    private javax.swing.JLabel lblStatus;
    
    public ResolveScenarioWizardDialog(java.awt.Frame parent, boolean modal, ResolveScenarioTracker t) {
        super(parent, modal);
        this.tracker = t;
        missingUnits = tracker.getMissingUnits();
        salvageables = new ArrayList<Unit>();
        if(tracker.getMission() instanceof Contract) {
	        salvageEmployer = ((Contract)tracker.getMission()).getSalvagedByEmployer();
	    	salvageUnit = ((Contract)tracker.getMission()).getSalvagedByUnit();
	    	maxSalvagePct = ((Contract)tracker.getMission()).getSalvagePct();
	    	currentSalvagePct = (int)(100*((double)salvageUnit)/(salvageUnit+salvageEmployer));
        }
        formatter = new DecimalFormat();
        currentPanel = UNITSPANEL;
        initComponents();
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
    	java.awt.GridBagConstraints gridBagConstraints;

    	cardLayout = new CardLayout();
    	
    	ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ResolveScenarioWizardDialog");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        
        getContentPane().setLayout(new BorderLayout());
        
        setTitle(resourceMap.getString("title"));

        /*
         * Instructions at the top
         */
        txtInstructions = new javax.swing.JTextArea();
        txtInstructions.setText("");
        txtInstructions.setEditable(false);
        txtInstructions.setLineWrap(true);
        txtInstructions.setWrapStyleWord(true);
        txtInstructions.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder(resourceMap.getString("txtInstructions.title")),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        txtInstructions.setMinimumSize(new Dimension(590,100));
        txtInstructions.setPreferredSize(new Dimension(590,100));
        getContentPane().add(txtInstructions, BorderLayout.PAGE_START);
        
        /*
         * Set up main panel with cards
         */
        pnlMain = new JPanel(cardLayout); 
       
    	/*
    	 * Missing Units Panel
    	 */
    	pnlMissingUnits = new JPanel();
        pnlMissingUnits.setLayout(new GridBagLayout()); 
        unitBoxes = new ArrayList<JCheckBox>();
        JCheckBox box;
        int i = 0;
        int j = 0;
        for(Unit u : missingUnits) {
        	j++;
        	box = new JCheckBox(u.getName());
        	box.setSelected(false);
        	unitBoxes.add(box);
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            if(j == (missingUnits.size())) {
            	gridBagConstraints.weighty = 1.0;
            }
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            pnlMissingUnits.add(box, gridBagConstraints);
            i++;
        }
    	pnlMain.add(pnlMissingUnits, UNITSPANEL);
   
    	/*
    	 * Pilot Status Panel
    	 */
    	pnlPilotStatus = new JPanel();
        pnlPilotStatus.setLayout(new GridBagLayout()); 
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        pnlPilotStatus.add(new JLabel(resourceMap.getString("hits")), gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        gridBagConstraints.weightx = 1.0;
        pnlPilotStatus.add(new JLabel(resourceMap.getString("mia")), gridBagConstraints);
        miaBtns = new ArrayList<JCheckBox>();
        hitSliders = new ArrayList<JSlider>();
        statuses = new ArrayList<PersonStatus>();
        i = 2;
        JLabel nameLbl;
        JCheckBox miaCheck;
        JSlider hitSlider; 
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put( new Integer( 0 ), new JLabel("0") );
        labelTable.put( new Integer( 1 ), new JLabel("1") );
        labelTable.put( new Integer( 2 ), new JLabel("2") );
        labelTable.put( new Integer( 3 ), new JLabel("3") );
        labelTable.put( new Integer( 4 ), new JLabel("4") );
        labelTable.put( new Integer( 5 ), new JLabel("5") );
        labelTable.put( new Integer( 6 ), new JLabel(resourceMap.getString("dead")) );
        j = 0;
        for(UUID pid : tracker.getPeopleStatus().keySet()) {
        	j++;
        	PersonStatus status = tracker.getPeopleStatus().get(pid);
        	statuses.add(status);
        	nameLbl = new JLabel("<html>" + status.getName() + "<br><i> " + status.getUnitName() + "</i></html>");
        	miaCheck = new JCheckBox("");
        	miaBtns.add(miaCheck);
        	hitSlider = new JSlider(JSlider.HORIZONTAL, 0, 6, status.getHits());
        	hitSlider.setMajorTickSpacing(1);
        	hitSlider.setPaintTicks(true);
        	hitSlider.setLabelTable(labelTable);
        	hitSlider.setPaintLabels(true);
        	hitSlider.setSnapToTicks(true);
        	hitSliders.add(hitSlider);
        	if(status.isMissing()) {
        		miaCheck.setSelected(true);
        	} 
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            if(j == tracker.getPeopleStatus().keySet().size()) {
            	gridBagConstraints.weighty = 1.0;
            }
            pnlPilotStatus.add(nameLbl, gridBagConstraints);
            gridBagConstraints.gridx = 1;
            pnlPilotStatus.add(hitSlider, gridBagConstraints);
            gridBagConstraints.gridx = 2;
            gridBagConstraints.weightx = 1.0;
            pnlPilotStatus.add(miaCheck, gridBagConstraints);
            i++;
        }    	
    	pnlMain.add(pnlPilotStatus, PILOTPANEL);
    	
    	/*
    	 * Salvage panel
    	 */
    	pnlSalvage = new JPanel();
        salvageBoxes = new ArrayList<JCheckBox>();
        pnlSalvage.setLayout(new GridBagLayout()); 
        JPanel pnlSalvageValue = new JPanel(new GridBagLayout());
        i = 0;
        if(tracker.getMission() instanceof Contract) {
        	lblSalvageValueUnit1 = new JLabel(resourceMap.getString("lblSalvageValueUnit1.text"));   	
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            pnlSalvageValue.add(lblSalvageValueUnit1, gridBagConstraints);
        	lblSalvageValueUnit2 = new JLabel(formatter.format(salvageUnit) + " C-Bills");   	
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 1.0;
            pnlSalvageValue.add(lblSalvageValueUnit2, gridBagConstraints);
            i++;
            lblSalvageValueEmployer1 = new JLabel(resourceMap.getString("lblSalvageValueEmployer1.text"));   	
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            pnlSalvageValue.add(lblSalvageValueEmployer1, gridBagConstraints);
        	lblSalvageValueEmployer2 = new JLabel(formatter.format(salvageEmployer) + " C-Bills");
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 1.0;
            pnlSalvageValue.add(lblSalvageValueEmployer2, gridBagConstraints);
            i++;
            String lead = "<html><font color='black'>";
            if(currentSalvagePct > maxSalvagePct) {
            	lead = "<html><font color='red'>";
            }
            lblSalvagePct1 = new JLabel(resourceMap.getString("lblSalvagePct1.text"));   	
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            pnlSalvageValue.add(lblSalvagePct1, gridBagConstraints);
            lblSalvagePct2 = new JLabel(lead + currentSalvagePct + "%</font> <font color='black'>(max " + maxSalvagePct + "%)</font></html>");
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            pnlSalvageValue.add(lblSalvagePct2, gridBagConstraints);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
            pnlSalvage.add(pnlSalvageValue, gridBagConstraints);
            i++;
        }
        i++;
        j = 0;
        for(Entity en : tracker.getPotentialSalvage()) {
        	j++;
        	Unit u = new Unit(en, tracker.getCampaign());
        	u.initializeParts(false);
        	u.runDiagnostic();
        	salvageables.add(u);
        	box = new JCheckBox(en.getDisplayName() + " (" + formatter.format(u.getSellValue()) + " C-Bills)");
        	box.setSelected(false);
        	box.addItemListener(new ItemListener() {
        		@Override
        		public void itemStateChanged(ItemEvent evt) {
     				checkSalvageRights();
     			}
        	});
        	salvageBoxes.add(box);
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            if(j == tracker.getPotentialSalvage().size()) {
            	gridBagConstraints.weighty = 1.0;
            }
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            pnlSalvage.add(box, gridBagConstraints);
            i++;
        }
        checkSalvageRights();
    	pnlMain.add(pnlSalvage, SALVAGEPANEL);
    	
    	/*
    	 * Assign Kills panel
    	 */
    	pnlKills = new JPanel();
        killChoices = new Hashtable<String, JComboBox>();
        pnlKills.setLayout(new GridBagLayout()); 
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        pnlKills.add(new JLabel(resourceMap.getString("kill")), gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        pnlKills.add(new JLabel(resourceMap.getString("claim")), gridBagConstraints);
        
        i = 2;
        JComboBox comboAssign;
        DefaultComboBoxModel assignModel; 
        j = 0;
        for(String killName : tracker.getKillCredits().keySet()) {
        	j++;
        	nameLbl = new JLabel(killName);
        	assignModel = new DefaultComboBoxModel();
        	assignModel.addElement(resourceMap.getString("none"));
        	int idx = 0;
        	int selected = 0;
        	if(null == tracker.getKillCredits().get(killName)) {
        		continue;
        	}
    		for(Unit u : tracker.getUnits()) {	
    			idx++;
    			if (u.getEntity() instanceof GunEmplacement) {
    				assignModel.addElement("AutoTurret, " + u.getName());
    			} else {
    				assignModel.addElement(u.getCommander().getFullTitle() + ", " + u.getName());
    			}
    			if(u.getId().toString().equals(tracker.getKillCredits().get(killName))) {
    				selected = idx;
    			}
    		}
        	comboAssign = new JComboBox(assignModel);
        	comboAssign.setSelectedIndex(selected);
        	killChoices.put(killName, comboAssign);
        	gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            if(j == tracker.getKillCredits().keySet().size()) {
                gridBagConstraints.weighty = 1.0;
            }
            pnlKills.add(nameLbl, gridBagConstraints);
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.gridx = 1;
            pnlKills.add(comboAssign, gridBagConstraints);
            i++;
        }     
    	pnlMain.add(pnlKills, KILLPANEL);
    	
    	/*
    	 * Preview Panel
    	 */
    	pnlPreview = new JPanel();
    	choiceStatus = new javax.swing.JComboBox();
        scrReport = new javax.swing.JScrollPane();
        scrRecoveredUnits = new javax.swing.JScrollPane();
        scrRecoveredPilots = new javax.swing.JScrollPane();
        scrMissingUnits = new javax.swing.JScrollPane();
        scrMissingPilots = new javax.swing.JScrollPane();
        scrDeadPilots = new javax.swing.JScrollPane();
        scrSalvage = new javax.swing.JScrollPane();
        txtInstructions = new javax.swing.JTextArea();
        txtReport = new javax.swing.JTextArea();
        txtRecoveredUnits = new javax.swing.JTextArea();
        txtRecoveredPilots = new javax.swing.JTextArea();
        txtMissingUnits = new javax.swing.JTextArea();
        txtMissingPilots = new javax.swing.JTextArea();
        txtDeadPilots = new javax.swing.JTextArea();
        txtSalvage = new javax.swing.JTextArea();
        lblStatus = new javax.swing.JLabel();
    	
        pnlPreview.setLayout(new GridBagLayout());
        
        JPanel pnlStatus = new JPanel();
        
        lblStatus.setText(resourceMap.getString("lblStatus.text"));
        DefaultComboBoxModel statusModel = new DefaultComboBoxModel();
		for (int k = 1; k < Scenario.S_NUM; k++) {
			statusModel.addElement(Scenario.getStatusName(k));
		}
		choiceStatus.setModel(statusModel);
		choiceStatus.setName("choiceStatus"); // NOI18N
		choiceStatus.setSelectedIndex(0);     
		pnlStatus.setLayout(new FlowLayout(FlowLayout.LEADING, 5, 5));
		pnlStatus.add(lblStatus);
		pnlStatus.add(choiceStatus);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        pnlPreview.add(pnlStatus, gridBagConstraints);
		
        txtReport.setText("");
        txtReport.setName("txtReport");
        txtReport.setEditable(true);
        txtReport.setLineWrap(true);
        txtReport.setWrapStyleWord(true);
        txtReport.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder(resourceMap.getString("txtReport.title")),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        scrReport.setViewportView(txtReport);
        scrReport.setPreferredSize(new Dimension(500,200));
        scrReport.setMinimumSize(new Dimension(500,200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        pnlPreview.add(scrReport, gridBagConstraints);
	         
        txtRecoveredUnits.setName("txtRecoveredUnits");
        txtRecoveredUnits.setText(resourceMap.getString("none"));
        txtRecoveredUnits.setEditable(false);
        txtRecoveredUnits.setLineWrap(true);
        txtRecoveredUnits.setWrapStyleWord(true);
        txtRecoveredUnits.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(resourceMap.getString("txtRecoveredUnits.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrRecoveredUnits.setViewportView(txtRecoveredUnits);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        pnlPreview.add(scrRecoveredUnits, gridBagConstraints);
        
        txtRecoveredPilots.setName("txtRecoveredPilots");
        txtRecoveredPilots.setText(resourceMap.getString("none"));
        txtRecoveredPilots.setEditable(false);
        txtRecoveredPilots.setLineWrap(true);
        txtRecoveredPilots.setWrapStyleWord(true);
        txtRecoveredPilots.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(resourceMap.getString("txtRecoveredPilots.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrRecoveredPilots.setViewportView(txtRecoveredPilots);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        pnlPreview.add(scrRecoveredPilots, gridBagConstraints);
        
        txtMissingUnits.setName("txtMissingUnits");
        txtMissingUnits.setText(resourceMap.getString("none"));
        txtMissingUnits.setEditable(false);
        txtMissingUnits.setLineWrap(true);
        txtMissingUnits.setWrapStyleWord(true);
        txtMissingUnits.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(resourceMap.getString("txtMissingUnits.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrMissingUnits.setViewportView(txtMissingUnits);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        pnlPreview.add(scrMissingUnits, gridBagConstraints);
        
        txtMissingPilots.setName("txtMissingPilots");
        txtMissingPilots.setText(resourceMap.getString("none"));
        txtMissingPilots.setEditable(false);
        txtMissingPilots.setLineWrap(true);
        txtMissingPilots.setWrapStyleWord(true);
        txtMissingPilots.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(resourceMap.getString("txtMissingPilots.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrMissingPilots.setViewportView(txtMissingPilots);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        pnlPreview.add(scrMissingPilots, gridBagConstraints);
        
        txtSalvage.setName("txtSalvage");
        txtSalvage.setText("None");
        txtSalvage.setEditable(false);
        txtSalvage.setLineWrap(true);
        txtSalvage.setWrapStyleWord(true);
        txtSalvage.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(resourceMap.getString("txtSalvagedUnits.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrSalvage.setViewportView(txtSalvage);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        pnlPreview.add(scrSalvage, gridBagConstraints);
        
        txtDeadPilots.setName("txtDeadPilots");
        txtDeadPilots.setText(resourceMap.getString("none"));
        txtDeadPilots.setEditable(false);
        txtDeadPilots.setLineWrap(true);
        txtDeadPilots.setWrapStyleWord(true);
        txtDeadPilots.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(resourceMap.getString("txtDeadPilots.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrDeadPilots.setViewportView(txtDeadPilots);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        pnlPreview.add(scrDeadPilots, gridBagConstraints);
    	pnlMain.add(pnlPreview, PREVIEWPANEL);
        
        
    	scrMain = new JScrollPane(pnlMain);
    	scrMain.setMinimumSize(new Dimension(600,500));
    	scrMain.setPreferredSize(new Dimension(600,500));
    	getContentPane().add(scrMain, BorderLayout.CENTER);
    	
    	
    	/*
    	 * Set up button panel
    	 */
    	panButtons = new JPanel();
        panButtons.setName("panButtons");
        panButtons.setLayout(new GridBagLayout());  
        
        btnCancel = new JButton(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnClose"); // NOI18N
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
        	public void actionPerformed(java.awt.event.ActionEvent evt) {
        		cancel();
        	}
        });
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panButtons.add(btnCancel, gridBagConstraints);
		
        btnBack = new JButton(resourceMap.getString("btnBack.text")); // NOI18N
        btnBack.setName("btnBack"); // NOI18N
        btnBack.addActionListener(new java.awt.event.ActionListener() {
        	public void actionPerformed(java.awt.event.ActionEvent evt) {
        		back();
        	}
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panButtons.add(btnBack, gridBagConstraints);
        
        btnNext = new JButton(resourceMap.getString("btnNext.text")); // NOI18N
        btnNext.setName("btnNext"); // NOI18N
        btnNext.addActionListener(new java.awt.event.ActionListener() {
        	public void actionPerformed(java.awt.event.ActionEvent evt) {
        		next();
        	}
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panButtons.add(btnNext, gridBagConstraints);
		
        btnFinish = new JButton(resourceMap.getString("btnFinish.text")); // NOI18N
        btnFinish.setName("btnFinish"); // NOI18N
        btnFinish.addActionListener(new java.awt.event.ActionListener() {
        	public void actionPerformed(java.awt.event.ActionEvent evt) {
        		finish();
        	}
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panButtons.add(btnFinish, gridBagConstraints);
        
        getContentPane().add(panButtons, BorderLayout.PAGE_END);
    
        switchPanel(currentPanel);
        if(!usePanel(currentPanel)) {
        	next();
        }
        
      	btnNext.setEnabled(true);
      	btnBack.setEnabled(false);
      	btnFinish.setEnabled(false);
      	
        pack();     
        

    }
    
    private void switchPanel(String name) {
    	if(name.equals(PREVIEWPANEL)) {
    		updatePreviewPanel();
    	}
    	currentPanel = name;
    	setTitle(currentPanel);
    	cardLayout.show(pnlMain, currentPanel);
    	switchInstructions();
    	javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() { 
				scrMain.getVerticalScrollBar().setValue(0);
			}
		});
    }
    
    private void switchInstructions() {
    	final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ResolveScenarioWizardDialog");
    	if(currentPanel.equals(UNITSPANEL)) {
    		txtInstructions.setText(resourceMap.getString("txtInstructions.text.missingunits"));
    	}
    	else if(currentPanel.equals(PILOTPANEL)) {
    		txtInstructions.setText(resourceMap.getString("txtInstructions.text.personnel"));
    	}
    	else if(currentPanel.equals(SALVAGEPANEL)) {
    		txtInstructions.setText(resourceMap.getString("txtInstructions.text.salvage"));
    	}
    	else if(currentPanel.equals(KILLPANEL)) {
    		txtInstructions.setText(resourceMap.getString("txtInstructions.text.kills"));
    	}
    	else if(currentPanel.equals(PREVIEWPANEL)) {
    		txtInstructions.setText(resourceMap.getString("txtInstructions.text.preview"));
    	}
    	else {
    		txtInstructions.setText("");
    	}
    	txtInstructions.setEditable(false);
        txtInstructions.setLineWrap(true);
        txtInstructions.setWrapStyleWord(true);
        txtInstructions.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder(resourceMap.getString("txtInstructions.title")),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        txtInstructions.setMinimumSize(new Dimension(590,100));
        txtInstructions.setPreferredSize(new Dimension(590,100));
        getContentPane().add(txtInstructions, BorderLayout.PAGE_START);
    }
    
    private void next() {
		btnNext.setEnabled(false);
		btnBack.setEnabled(true);
		btnFinish.setEnabled(false);
    	boolean passedCurrent = false;
    	boolean switchMade = false;
    	for(int i = 0; i < panelOrder.length; i++) {
    		String name = panelOrder[i];
    		if(passedCurrent) {
    			if(usePanel(name)) {
    				if(!switchMade) {
    					switchPanel(name);
    					switchMade = true;
    					if(name.equals(PREVIEWPANEL)) {
    						btnFinish.setEnabled(true);
    					}
    				} else {
    					btnNext.setEnabled(true);
    					break;
    				}
    			}
    		}
    		else if(name.equals(currentPanel)) {
    			passedCurrent = true;
    		} 
    	}
	}
    
    private void back() {
    	btnNext.setEnabled(true);
		btnBack.setEnabled(false);
		btnFinish.setEnabled(false);
    	boolean passedCurrent = false;
    	boolean switchMade = false;
    	for(int i = (panelOrder.length-1); i >= 0; i--) {
    		String name = panelOrder[i];
    		if(passedCurrent) {
    			if(passedCurrent) {
        			if(usePanel(name)) {
        				if(!switchMade) {
        					switchPanel(name);
        					switchMade = true;					
        				} else {
        					btnBack.setEnabled(true);
        					break;
        				}
        			}
        		}
    		}
    		else if(name.equals(currentPanel)) {
    			passedCurrent = true;
    		} 
    	}
    }
    
    private void finish() {
    	
    	//missing units
    	for(int i = 0; i < unitBoxes.size(); i++) {
    		JCheckBox box = unitBoxes.get(i);
    		if(box.isSelected()) {
    			tracker.recoverUnit(missingUnits.get(i));
    		}
    	}
    	
    	//now personnel
    	for(int i = 0; i < statuses.size(); i++) {
    		PersonStatus status = statuses.get(i);
    		status.setMissing(miaBtns.get(i).isSelected());
    		status.setHits(hitSliders.get(i).getValue());
    	}
    	
    	//now salvage
    	for(int i = 0; i < salvageBoxes.size(); i++) {
    		JCheckBox box = salvageBoxes.get(i);
    		if(box.isSelected()) {
    			tracker.salvageUnit(i);
    		} else {
    			tracker.dontSalvageUnit(i);
    		}
    	}
    	
    	//now assign kills
    	for(String killName : tracker.getKillCredits().keySet()) {
    		if(killChoices.get(killName).getSelectedIndex() == 0) {
    			tracker.getKillCredits().put(killName, "None");
    		} else {
	    		Unit u = tracker.getUnits().get(killChoices.get(killName).getSelectedIndex()-1);
	    		if(null != u) {
	    			tracker.getKillCredits().put(killName, u.getId().toString());
	    		}
    		}
    	}
    	tracker.assignKills();
    	
    	//now process
    	tracker.resolveScenario(choiceStatus.getSelectedIndex()+1,txtReport.getText());
    	this.setVisible(false);
    }
    
    private void cancel() {
    	setVisible(false);
    }
    
    private boolean usePanel(String panelName) {
    	if(panelName.equals(UNITSPANEL)) {
    		return missingUnits.size() > 0;
    	}
    	else if(panelName.equals(PILOTPANEL)) {
    		return tracker.getPeopleStatus().keySet().size() > 0;
    	}
    	else if(panelName.equals(SALVAGEPANEL)) {
    		return tracker.getPotentialSalvage().size() > 0 
    				&& (!(tracker.getMission() instanceof Contract) || ((Contract)tracker.getMission()).canSalvage());
    	}
    	else if(panelName.equals(KILLPANEL)) {
    		return !tracker.getKillCredits().isEmpty();
    	}
    	else if(panelName.equals(PREVIEWPANEL)) {
    		return true;
    	}
    	return false;
    }
    
    private void checkSalvageRights() {
    	if(!(tracker.getMission() instanceof Contract)) {
    		return;
    	}
    	salvageEmployer = ((Contract)tracker.getMission()).getSalvagedByEmployer();
    	salvageUnit = ((Contract)tracker.getMission()).getSalvagedByUnit();
    	for(int i = 0; i < salvageBoxes.size(); i++) {
    		JCheckBox box = salvageBoxes.get(i);
    		if(box.isSelected()) {
    			salvageUnit += salvageables.get(i).getSellValue();
    		} else {
    			salvageEmployer += salvageables.get(i).getSellValue();
    		}
    	}
    	currentSalvagePct = (int)(100*((double)salvageUnit)/(salvageUnit+salvageEmployer));
    	for(JCheckBox box : salvageBoxes) {
    		if(!box.isSelected() && currentSalvagePct >= maxSalvagePct) {
    			box.setEnabled(false);
    		} else {
    			box.setEnabled(true);
    		}
    	}
    	lblSalvageValueUnit2.setText(formatter.format(salvageUnit) + " C-Bills");
    	lblSalvageValueEmployer2.setText(formatter.format(salvageEmployer) + " C-Bills");
    	String lead = "<html><font color='black'>";
        if(currentSalvagePct > maxSalvagePct) {
        	lead = "<html><font color='red'>";
        }
        lblSalvagePct2.setText(lead + currentSalvagePct + "%</font> <font color='black'>(max " + maxSalvagePct + "%)</font></html>");

    }
    
    private void updatePreviewPanel() {
    	        
        //pilots first
        String missingNames = "";
        String kiaNames = "";
        String recoverNames = "";
        for(int i = 0; i < statuses.size(); i++) {
    		PersonStatus status = statuses.get(i);
    		if(hitSliders.get(i).getValue() >= 6) {
    			kiaNames  += status.getName() + "\n";
    		} else if(miaBtns.get(i).isSelected()) {
    			missingNames += status.getName() + "\n";
    		} else {
    			recoverNames += status.getName() + "\n";
    		}
    	}
        txtRecoveredPilots.setText(recoverNames);
        txtMissingPilots.setText(missingNames);
        txtDeadPilots.setText(kiaNames);
        
        //now units
        String recoverUnits = "";
        String missUnits = "";
        for(Unit u : tracker.getRecoveredUnits()) {
        	recoverUnits += u.getName() + "\n";
        }
        for(int i = 0; i < unitBoxes.size(); i++) {
    		JCheckBox box = unitBoxes.get(i);
    		if(box.isSelected()) {
    			recoverUnits += missingUnits.get(i).getName() + "\n";
    		} else {
    			missUnits += missingUnits.get(i).getName() + "\n";
    		}
    	}
        txtRecoveredUnits.setText(recoverUnits);
        txtMissingUnits.setText(missUnits);
        
        //now salvage
        String salvageUnits = "";
        for(int i = 0; i < salvageBoxes.size(); i++) {
    		JCheckBox box = salvageBoxes.get(i);
    		if(box.isSelected()) {
    			salvageUnits += salvageables.get(i).getName() + "\n";
    		} 
    	}
        txtSalvage.setText(salvageUnits);
    }
}
