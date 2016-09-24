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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.UnitEditorDialog;
import megamek.client.ui.swing.MechViewPanel;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.util.EncodeControl;
import mekhq.Utilities;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.ResolveScenarioTracker.PersonStatus;
import mekhq.campaign.ResolveScenarioTracker.PrisonerStatus;
import mekhq.campaign.ResolveScenarioTracker.UnitStatus;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

/**
 *
 * @author  Taharqa
 */
public class ResolveScenarioWizardDialog extends JDialog {
	private static final long serialVersionUID = -8038099101234445018L;

	final static String UNITSPANEL   = "Unit Status";
	final static String PILOTPANEL   = "Pilot Status";
	final static String SALVAGEPANEL = "Claim Salvage";
    final static String PRISONERPANEL= "Captured Personnel Status";
	final static String KILLPANEL    = "Assign Kills";
	final static String REWARDPANEL  = "Collect Rewards";
	final static String PREVIEWPANEL = "Preview";
	/* Used by AtB to determine minor contract breaches and bonus rolls */
	final static String ALLYPANEL    = "Ally Status";

	final static String[] panelOrder = {UNITSPANEL,ALLYPANEL,PILOTPANEL,SALVAGEPANEL,PRISONERPANEL,KILLPANEL,REWARDPANEL,PREVIEWPANEL};

	private JFrame frame;

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
    private JPanel pnlUnitStatus;
    private JPanel pnlAllyStatus;
    private JPanel pnlPilotStatus;
    private JPanel pnlSalvage;
    private JPanel pnlPrisonerStatus;
    private JPanel pnlKills;
    private JPanel pnlRewards;
    private JPanel pnlPreview;

    /*
     * Unit status panel components
     */
    private ArrayList<JCheckBox> chksTotaled;
    private ArrayList<JButton> btnsViewUnit;
    private ArrayList<JButton> btnsEditUnit;
    private ArrayList<UnitStatus> ustatuses;
    private ArrayList<JLabel> lblsUnitName;

    /*
     * Ally status panel components
     */
    private ArrayList<JCheckBox> chksAllyLost;

    /*
     * Pilot status panel components
     */
    private ArrayList<JCheckBox> miaBtns = new ArrayList<JCheckBox>();
    private ArrayList<JSlider> hitSliders = new ArrayList<JSlider>();
    private ArrayList<PersonStatus> pstatuses = new ArrayList<PersonStatus>();

    /*
     * Prisoner status panel components
     */
    private ArrayList<JCheckBox> prisonerBtns = new ArrayList<JCheckBox>();
    private ArrayList<JSlider> pr_hitSliders = new ArrayList<JSlider>();
    private ArrayList<PrisonerStatus> prstatuses = new ArrayList<PrisonerStatus>();

    /*
     * Salvage panel components
     */
    private ArrayList<JCheckBox> salvageBoxes;
    private ArrayList<JCheckBox> escapeBoxes;
    private ArrayList<Unit> salvageables;
    private ArrayList<JButton> btnsSalvageViewUnit;
    private ArrayList<JButton> btnsSalvageEditUnit;

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
    private Hashtable<String, JComboBox<String>> killChoices;

    /*
     * Collect Rewards components
     */
    private ArrayList<JCheckBox> lootBoxes;
    private ArrayList<Loot> loots;

    /*
     * Preview Panel components
     */
    private javax.swing.JComboBox<String> choiceStatus;
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
    private javax.swing.JTextArea txtRewards;
    private javax.swing.JLabel lblStatus;

    public ResolveScenarioWizardDialog(JFrame parent, boolean modal, ResolveScenarioTracker t) {
        super(parent, modal);
        this.frame = parent;
        this.tracker = t;
        loots = tracker.getPotentialLoot();
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

    	ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ResolveScenarioWizardDialog", new EncodeControl()); //$NON-NLS-1$
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
        txtInstructions.setMinimumSize(new Dimension(590,120));
        txtInstructions.setPreferredSize(new Dimension(590,120));
        getContentPane().add(txtInstructions, BorderLayout.PAGE_START);

        /*
         * Set up main panel with cards
         */
        pnlMain = new JPanel(cardLayout);

    	/*
    	 * Unit Status Panel
    	 */

    	pnlUnitStatus = new JPanel();
    	pnlUnitStatus.setLayout(new GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlUnitStatus.add(new JLabel(resourceMap.getString("totaled")), gridBagConstraints);
        chksTotaled = new ArrayList<JCheckBox>();
        ustatuses = new ArrayList<UnitStatus>();
        btnsViewUnit = new ArrayList<JButton>();
        btnsEditUnit = new ArrayList<JButton>();
        lblsUnitName = new ArrayList<JLabel>();
        int i = 2;
        int j = 0;
        JLabel nameLbl;
        JCheckBox chkTotaled;
        JButton btnViewUnit;
        JButton btnEditUnit;
        for(Unit unit : tracker.getUnits()) {
            UnitStatus status = tracker.getUnitsStatus().get(unit.getId());
            ustatuses.add(status);
            nameLbl = new JLabel(status.getDesc());
            lblsUnitName.add(nameLbl);
            chkTotaled = new JCheckBox("");
            chkTotaled.setSelected(status.isTotalLoss());
            chkTotaled.setName(Integer.toString(j));
            chkTotaled.setActionCommand(unit.getId().toString());
            chksTotaled.add(chkTotaled);
            chkTotaled.addItemListener(new CheckTotalListener());
            btnViewUnit = new JButton("View Unit");
            btnViewUnit.setEnabled(!status.isTotalLoss());
            btnViewUnit.setActionCommand(unit.getId().toString());
            btnViewUnit.addActionListener(new ViewUnitListener());
            btnsViewUnit.add(btnViewUnit);
            btnEditUnit = new JButton("Edit Unit");
            btnEditUnit.setEnabled(!status.isTotalLoss());
            btnEditUnit.setActionCommand(unit.getId().toString());
            btnEditUnit.setName(Integer.toString(j));
            btnEditUnit.addActionListener(new EditUnitListener());
            btnsEditUnit.add(btnEditUnit);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            j++;
            if(j == tracker.getUnits().size()) {
                gridBagConstraints.weighty = 1.0;
            }
            pnlUnitStatus.add(nameLbl, gridBagConstraints);
            gridBagConstraints.gridx = 1;
            pnlUnitStatus.add(chkTotaled, gridBagConstraints);
            gridBagConstraints.gridx = 2;
            pnlUnitStatus.add(btnViewUnit, gridBagConstraints);
            gridBagConstraints.gridx = 3;
            gridBagConstraints.weightx = 1.0;
            pnlUnitStatus.add(btnEditUnit, gridBagConstraints);
            i++;
        }
        pnlMain.add(pnlUnitStatus, UNITSPANEL);

        /*
         * Ally Status Panel
         */
        pnlAllyStatus = new JPanel();
        if (tracker.getCampaign().getCampaignOptions().getUseAtB() &&
        		tracker.getScenario() instanceof AtBScenario) {
        	pnlAllyStatus.setLayout(new GridBagLayout());
        	gridBagConstraints = new java.awt.GridBagConstraints();
        	gridBagConstraints.gridx = 1;
        	gridBagConstraints.gridy = 1;
        	gridBagConstraints.gridwidth = 1;
        	gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        	gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        	pnlAllyStatus.add(new JLabel("Lost"), gridBagConstraints);
        	chksAllyLost = new ArrayList<JCheckBox>();
        	i = 2;
        	j = 0;
        	JCheckBox chkAllyLost;
        	ArrayList<UUID> allyIds = new ArrayList<>();
        	allyIds.addAll(((AtBScenario)tracker.getScenario()).getAttachedUnitIds());
        	allyIds.addAll(((AtBScenario)tracker.getScenario()).getSurvivalBonusIds());
        	for (UUID id : allyIds) {
        		j++;
        		chkAllyLost = new JCheckBox();
        		chksAllyLost.add(chkAllyLost);
        		UnitStatus status = tracker.getUnitsStatus().get(id);
        		chkAllyLost.setSelected( status == null
        				|| status.isTotalLoss()
        				|| status.isLikelyCaptured());
        		gridBagConstraints = new java.awt.GridBagConstraints();
        		gridBagConstraints.gridx = 0;
        		gridBagConstraints.gridy = i;
        		gridBagConstraints.gridwidth = 1;
        		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        		gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        		gridBagConstraints.weightx = 0.0;
        		if (j == allyIds.size()) {
        			gridBagConstraints.weighty = 1.0;
        		}
        		pnlAllyStatus.add(chkAllyLost, gridBagConstraints);
        		gridBagConstraints.gridx = 1;
        		pnlAllyStatus.add(new JLabel(((AtBScenario)tracker.getScenario()).getEntity(id).getShortName()), gridBagConstraints);
        		i++;
        	}
        }
        pnlMain.add(pnlAllyStatus, ALLYPANEL);

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
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPilotStatus.add(new JLabel(resourceMap.getString("hits")), gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPilotStatus.add(new JLabel(resourceMap.getString("mia")), gridBagConstraints);
        i = 2;
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
        for(PersonStatus status : tracker.getSortedPeople()) {
        	j++;
            pstatuses.add(status);
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
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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
         * Prisoner Status Panel
         */
        pnlPrisonerStatus = new JPanel();
        pnlPrisonerStatus.setLayout(new GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPrisonerStatus.add(new JLabel(resourceMap.getString("hits")), gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        /*pnlPrisonerStatus.add(new JLabel(resourceMap.getString("mia")), gridBagConstraints);*/
        //gridBagConstraints.gridx = 2;
        pnlPrisonerStatus.add(new JLabel(resourceMap.getString("prisoner")), gridBagConstraints);
        i = 2;
        JCheckBox prisonerCheck;
        j = 0;
        for(PrisonerStatus status : tracker.getSortedPrisoners()) {
            j++;
            prstatuses.add(status);
            nameLbl = new JLabel("<html>" + status.getName() + "<br><i> " + status.getUnitName() + "</i></html>");
            miaCheck = new JCheckBox("");
            hitSlider = new JSlider(JSlider.HORIZONTAL, 0, 6, status.getHits());
            hitSlider.setMajorTickSpacing(1);
            hitSlider.setPaintTicks(true);
            hitSlider.setLabelTable(labelTable);
            hitSlider.setPaintLabels(true);
            hitSlider.setSnapToTicks(true);
            hitSlider.setName(Integer.toString(j-1));
            pr_hitSliders.add(hitSlider);
            miaCheck.setSelected(status.isMissing());
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            if(j == tracker.getPrisonerStatus().keySet().size()) {
                gridBagConstraints.weighty = 1.0;
            }
            pnlPrisonerStatus.add(nameLbl, gridBagConstraints);
            gridBagConstraints.gridx = 1;
            pnlPrisonerStatus.add(hitSlider, gridBagConstraints);
            gridBagConstraints.gridx = 2;
            prisonerCheck = new JCheckBox("");
            prisonerBtns.add(prisonerCheck);
            prisonerCheck.setSelected(status.isCaptured());
            gridBagConstraints.weightx = 1.0;
            pnlPrisonerStatus.add(prisonerCheck, gridBagConstraints);
            i++;
            if (status.isCaptured() &&
                    tracker.getCampaign().getCampaignOptions().getUseAtB() &&
                    tracker.getCampaign().getCampaignOptions().getUseAtBCapture()) {
                boolean wasCaptured = false;
                if (status.wasPickedUp()) {
                    wasCaptured = true;
                } else {
                    for (int n = 0; n < status.getHits() + 1; n++) {
                        if (Utilities.dice(1, 6) == 1) {
                            wasCaptured = true;
                            break;
                        }
                    }
                }
                prisonerCheck.setSelected(wasCaptured);
            }
            //if the person is dead then ignore them
            if(status.isDead()) {
            	prisonerCheck.setSelected(false);
            	prisonerCheck.setEnabled(false);
            }
            hitSlider.addChangeListener(new CheckDeadPrisonerListener());
        }
        pnlMain.add(pnlPrisonerStatus, PRISONERPANEL);

    	/*
    	 * Salvage panel
    	 */
    	pnlSalvage = new JPanel();
    	salvageBoxes = new ArrayList<JCheckBox>();
    	escapeBoxes = new ArrayList<JCheckBox>();
    	pnlSalvage.setLayout(new GridBagLayout());
        btnsSalvageViewUnit = new ArrayList<JButton>();
        btnsSalvageEditUnit = new ArrayList<JButton>();
        JPanel pnlSalvageValue = new JPanel(new GridBagLayout());
        i = 0;
        if((tracker.getMission() instanceof Contract) && !tracker.usesSalvageExchange()) {
        	lblSalvageValueUnit1 = new JLabel(resourceMap.getString("lblSalvageValueUnit1.text"));
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            pnlSalvageValue.add(lblSalvageValueUnit1, gridBagConstraints);
        	lblSalvageValueUnit2 = new JLabel(formatter.format(salvageUnit) + " C-Bills");
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 1.0;
            pnlSalvageValue.add(lblSalvageValueUnit2, gridBagConstraints);
            i++;
            lblSalvageValueEmployer1 = new JLabel(resourceMap.getString("lblSalvageValueEmployer1.text"));
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            pnlSalvageValue.add(lblSalvageValueEmployer1, gridBagConstraints);
        	lblSalvageValueEmployer2 = new JLabel(formatter.format(salvageEmployer) + " C-Bills");
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            pnlSalvageValue.add(lblSalvagePct1, gridBagConstraints);
            lblSalvagePct2 = new JLabel(lead + currentSalvagePct + "%</font> <font color='black'>(max " + maxSalvagePct + "%)</font></html>");
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            pnlSalvageValue.add(lblSalvagePct2, gridBagConstraints);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 4;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(0, 0, 20, 0);
            pnlSalvage.add(pnlSalvageValue, gridBagConstraints);
            i++;
        }
        i++;
        j = 0;
        JCheckBox box;
        JCheckBox escaped;
        JButton btnSalvageViewUnit;
        JButton btnSalvageEditUnit;
        for(TestUnit u : tracker.getPotentialSalvage()) {
        	j++;
        	salvageables.add(u);
        	UnitStatus status = tracker.getSalvageStatus().get(u.getId());
        	String txtBoxString = status.getDesc(formatter);
        	box = new JCheckBox(txtBoxString);
        	box.setSelected(false);
        	box.setEnabled(!tracker.usesSalvageExchange());
        	box.addItemListener(new ItemListener() {
        		@Override
        		public void itemStateChanged(ItemEvent evt) {
     				checkSalvageRights();
     			}
        	});
        	salvageBoxes.add(box);
        	escaped = new JCheckBox("Escapes");
        	escaped.setSelected(!status.isLikelyCaptured());
        	escaped.setEnabled(!(u.getEntity().isDestroyed() || u.getEntity().isDoomed()));
        	escaped.addItemListener(new ItemListener() {
        		@Override
        		public void itemStateChanged(ItemEvent evt) {
     				checkSalvageRights();
     			}
        	});
        	escapeBoxes.add(escaped);
            btnSalvageViewUnit = new JButton("View Unit");
            btnSalvageViewUnit.setEnabled(true);
            btnSalvageViewUnit.setActionCommand(u.getId().toString());
            btnSalvageViewUnit.addActionListener(new ViewUnitListener(true));
            btnsSalvageViewUnit.add(btnSalvageViewUnit);
            btnSalvageEditUnit = new JButton("Edit Unit");
            btnSalvageEditUnit.setEnabled(true);
            btnSalvageEditUnit.setActionCommand(u.getId().toString());
            btnSalvageEditUnit.setName(Integer.toString(j));
            btnSalvageEditUnit.addActionListener(new EditUnitListener(true));
            btnsSalvageEditUnit.add(btnSalvageEditUnit);
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.weightx = 0.0;
            if(j == tracker.getPotentialSalvage().size()) {
            	gridBagConstraints.weighty = 1.0;
            }
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            pnlSalvage.add(box, gridBagConstraints);
            gridBagConstraints.gridx = 1;
            pnlSalvage.add(escaped, gridBagConstraints);
            gridBagConstraints.gridx = 2;
            pnlSalvage.add(btnSalvageViewUnit, gridBagConstraints);
            gridBagConstraints.gridx = 3;
            gridBagConstraints.weightx = 1.0;
            pnlSalvage.add(btnSalvageEditUnit, gridBagConstraints);
            i++;
        }
        checkSalvageRights();
    	pnlMain.add(pnlSalvage, SALVAGEPANEL);

    	/*
    	 * Assign Kills panel
    	 */
    	pnlKills = new JPanel();
        killChoices = new Hashtable<String, JComboBox<String>>();
        pnlKills.setLayout(new GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlKills.add(new JLabel(resourceMap.getString("kill")), gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        gridBagConstraints.weightx = 1.0;
        pnlKills.add(new JLabel(resourceMap.getString("claim")), gridBagConstraints);

        i = 2;
        JComboBox<String> comboAssign;
        DefaultComboBoxModel<String> assignModel;
        j = 0;
        for(String killName : tracker.getKillCredits().keySet()) {
        	j++;
        	nameLbl = new JLabel(killName);
        	assignModel = new DefaultComboBoxModel<String>();
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
        	comboAssign = new JComboBox<String>(assignModel);
        	comboAssign.setSelectedIndex(selected);
        	killChoices.put(killName, comboAssign);
        	gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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
         * Collect Rewards Panel
         */
        pnlRewards = new JPanel();
        pnlRewards.setLayout(new GridBagLayout());
        lootBoxes = new ArrayList<JCheckBox>();
        i = 0;
        j = 0;
        for(Loot loot : loots) {
            j++;
            box = new JCheckBox(loot.getShortDescription());
            box.setSelected(false);
            lootBoxes.add(box);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            if(j == (loots.size())) {
                gridBagConstraints.weighty = 1.0;
            }
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            pnlRewards.add(box, gridBagConstraints);
            i++;
        }
        pnlMain.add(pnlRewards, REWARDPANEL);

    	/*
    	 * Preview Panel
    	 */
    	pnlPreview = new JPanel();
    	choiceStatus = new javax.swing.JComboBox<String>();
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
        txtRewards = new javax.swing.JTextArea();
        lblStatus = new javax.swing.JLabel();

        pnlPreview.setLayout(new GridBagLayout());

        JPanel pnlStatus = new JPanel();

        lblStatus.setText(resourceMap.getString("lblStatus.text"));
        DefaultComboBoxModel<String> statusModel = new DefaultComboBoxModel<String>();
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
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(pnlStatus, gridBagConstraints);

        txtRewards.setText(resourceMap.getString("none"));
        txtRewards.setEditable(false);
        txtRewards.setLineWrap(true);
        txtRewards.setWrapStyleWord(true);
        txtRewards.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtRewards.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(new JScrollPane(txtRewards), gridBagConstraints);

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
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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
    	final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ResolveScenarioWizardDialog", new EncodeControl()); //$NON-NLS-1$
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
        else if(currentPanel.equals(PRISONERPANEL)) {
            txtInstructions.setText(resourceMap.getString("txtInstructions.text.personnel"));
        }
    	else if(currentPanel.equals(PREVIEWPANEL)) {
    		txtInstructions.setText(resourceMap.getString("txtInstructions.text.preview"));
    	}
    	else if(currentPanel.equals(REWARDPANEL)) {
            txtInstructions.setText(resourceMap.getString("txtInstructions.text.reward"));
    	}
        else if(currentPanel.equals(ALLYPANEL)) {
                txtInstructions.setText(resourceMap.getString("txtInstructions.text.ally"));
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
        txtInstructions.setMinimumSize(new Dimension(590,150));
        txtInstructions.setPreferredSize(new Dimension(590,150));
        getContentPane().add(txtInstructions, BorderLayout.PAGE_START);
    }

    private void next() {
		btnNext.setEnabled(false);
		btnBack.setEnabled(true);
		btnFinish.setEnabled(false);
    	boolean passedCurrent = false;
    	boolean switchMade = false;
    	if (currentPanel.equals(ResolveScenarioWizardDialog.SALVAGEPANEL)) {
    	}
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


    	//unit status
    	for(int i = 0; i < chksTotaled.size(); i++) {
    		JCheckBox box = chksTotaled.get(i);
    		UUID id = UUID.fromString(box.getActionCommand());
    		if(null == id || null == tracker.getUnitsStatus().get(id)) {
    		    continue;
    		}
    		tracker.getUnitsStatus().get(id).setTotalLoss(box.isSelected());
    	}

    	if (tracker.getCampaign().getCampaignOptions().getUseAtB() &&
    			tracker.getScenario() instanceof AtBScenario) {
    		AtBScenario scenario = (AtBScenario)tracker.getScenario();
	    	int breaches = 0;
	    	int bonuses = 0;
	    	for (int i = 0; i < chksAllyLost.size(); i++) {
	    		if (i < scenario.getAttachedUnitIds().size()) {
	    			if (chksAllyLost.get(i).isSelected()) {
	    				breaches++;
	    				long etype = scenario.getEntity(scenario.getAttachedUnitIds().get(i)).getEntityType();
	    				if ((etype & megamek.common.Entity.ETYPE_DROPSHIP) != 0) {
	    					breaches += 4;
	    				}
	    			}
	    		} else {
	    			if (!chksAllyLost.get(i).isSelected()) {
	    				bonuses++;
	    			}
	    		}
	    	}
	    	tracker.setContractBreaches(breaches);
	    	tracker.setBonusRolls(bonuses);
    	}

        //now personnel
        for(int i = 0; i < pstatuses.size(); i++) {
            PersonStatus status = pstatuses.get(i);
            status.setMissing(miaBtns.get(i).isSelected());
            status.setHits(hitSliders.get(i).getValue());
        }

        //now prisoners
        for(int i = 0; i < prstatuses.size(); i++) {
            PrisonerStatus status = prstatuses.get(i);
            //status.setMissing(pr_miaBtns.get(i).isSelected());
            status.setHits(pr_hitSliders.get(i).getValue());
            status.setCaptured(prisonerBtns.get(i).isSelected());
        }

    	//now salvage
    	for(int i = 0; i < salvageBoxes.size(); i++) {
    		JCheckBox box = salvageBoxes.get(i);
    		if(box.isSelected()) {
    			tracker.salvageUnit(i);
    		} else if (!escapeBoxes.get(i).isSelected()) { // Only salvage if they don't escape
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

    	//now get loot
        for(int i = 0; i < lootBoxes.size(); i++) {
            JCheckBox box = lootBoxes.get(i);
            if(box.isSelected()) {
                tracker.addLoot(loots.get(i));
            }
        }

    	//now process
    	tracker.resolveScenario(choiceStatus.getSelectedIndex()+1,txtReport.getText());
    	this.setVisible(false);
    }

    private void cancel() {
    	setVisible(false);
    }

    private boolean usePanel(String panelName) {
    	if(panelName.equals(UNITSPANEL)) {
    		return tracker.getUnitsStatus().keySet().size() > 0;
    	}
    	else if (panelName.equals(ALLYPANEL)) {
    		return tracker.getCampaign().getCampaignOptions().getUseAtB() &&
    				tracker.getScenario() instanceof AtBScenario &&
    				(((AtBScenario)tracker.getScenario()).getAttachedUnitIds().size() +
    				((AtBScenario)tracker.getScenario()).getSurvivalBonusIds().size() > 0);
    	}
        else if(panelName.equals(PILOTPANEL)) {
            return tracker.getPeopleStatus().keySet().size() > 0;
        }
        else if(panelName.equals(PRISONERPANEL)) {
            return tracker.getPrisonerStatus().keySet().size() > 0;
        }
    	else if(panelName.equals(SALVAGEPANEL)) {
    		return tracker.getPotentialSalvage().size() > 0
    				&& (!(tracker.getMission() instanceof Contract) || ((Contract)tracker.getMission()).canSalvage());
    	}
    	else if(panelName.equals(KILLPANEL)) {
    		return !tracker.getKillCredits().isEmpty();
    	}
    	if(panelName.equals(REWARDPANEL)) {
            return loots.size() > 0;
        }
    	else if(panelName.equals(PREVIEWPANEL)) {
    		return true;
    	}
    	return false;
    }

    private void checkSalvageRights() {
    	// Perform a little magic to make sure we aren't trying to do both of these things
    	for (JCheckBox escaped : escapeBoxes) {
    		JCheckBox box = salvageBoxes.get(escapeBoxes.indexOf(escaped));
			if (escaped.isSelected()) {
				box.setSelected(false);
				box.setEnabled(false);
			} else if (box.isSelected()) {
				escaped.setSelected(false);
				escaped.setEnabled(false);
			} else {
				if(!tracker.usesSalvageExchange()) {
					box.setEnabled(true);
				}
				escaped.setEnabled(true);
			}
    	}
    	if(!(tracker.getMission() instanceof Contract) || tracker.usesSalvageExchange()) {
    		return;
    	}
    	salvageEmployer = ((Contract)tracker.getMission()).getSalvagedByEmployer();
    	salvageUnit = ((Contract)tracker.getMission()).getSalvagedByUnit();
    	for(int i = 0; i < salvageBoxes.size(); i++) {
    		// Skip the escaping units
    		if (escapeBoxes.get(i).isSelected()) {
    			continue;
    		}

    		// Set up the values
    		if(salvageBoxes.get(i).isSelected()) {
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
        for(int i = 0; i < pstatuses.size(); i++) {
    		PersonStatus status = pstatuses.get(i);
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
        for(int i = 0; i < chksTotaled.size(); i++) {
            String name = ustatuses.get(i).getName();
            if(chksTotaled.get(i).isSelected()) {
                missUnits += name + "\n";
            } else {
                recoverUnits += name + "\n";
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

        //now rewards
        String claimed = "";
        for(int i = 0; i < lootBoxes.size(); i++) {
            JCheckBox box = lootBoxes.get(i);
            if(box.isSelected()) {
                claimed += loots.get(i).getShortDescription() + "\n";
            }
        }
        txtRewards.setText(claimed);

    }

    @SuppressWarnings("unused") // FIXME
	private void showUnit(UUID id) {
    	showUnit(id, false);
    }

    private void showUnit(UUID id, boolean salvage) {
        //TODO: I am not sure I like the pop up dialog, might just make this a view on this
        //dialog
        UnitStatus ustatus;
        if(salvage) {
            ustatus = tracker.getSalvageStatus().get(id);
        } else {
            ustatus = tracker.getUnitsStatus().get(id);
        }
        if(null == ustatus || null == ustatus.getEntity()) {
            return;
        }
        Entity entity = ustatus.getEntity();
        final JDialog dialog = new JDialog(frame, "Unit View", true); //$NON-NLS-1$
        MechViewPanel mvp = new MechViewPanel();
        mvp.setMech(entity, true);
        JButton btn = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });

        dialog.getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints c;

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        dialog.getContentPane().add(mvp, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        dialog.getContentPane().add(btn, c);
        dialog.setSize(mvp.getBestWidth(), mvp.getBestHeight() + 75);
        dialog.validate();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    @SuppressWarnings("unused") // FIXME
	private void editUnit(UUID id, int idx) {
    	editUnit(id, idx, false);
    }

    private void editUnit(UUID id, int idx, boolean salvage) {
        UnitStatus ustatus = salvage ? tracker.getSalvageStatus().get(id) : tracker.getUnitsStatus().get(id);
        if(null == ustatus || null == ustatus.getEntity()) {
            return;
        }
        Entity entity = ustatus.getEntity();
        UnitEditorDialog med = new UnitEditorDialog(frame, entity);
        med.setVisible(true);
        JLabel name = lblsUnitName.get(idx);
        name.setText(ustatus.getDesc());
    }

    private class CheckTotalListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent evt) {
            int idx = Integer.parseInt(((JCheckBox)evt.getItem()).getName());
            btnsViewUnit.get(idx).setEnabled(!chksTotaled.get(idx).isSelected());
            btnsEditUnit.get(idx).setEnabled(!chksTotaled.get(idx).isSelected());
        }
    }
    
    private class CheckDeadPrisonerListener implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent evt) {
			JSlider hitslider = (JSlider)evt.getSource();
			if(hitslider.getValueIsAdjusting()) {
				return;
			}
			int idx = Integer.parseInt(hitslider.getName());
            JCheckBox captured = prisonerBtns.get(idx);
            if(null == captured) {
            	return;
            }
            int hits = hitslider.getValue();
            if(hits >= 6) {
            	captured.setSelected(false);
            	captured.setEnabled(false);
            } else if(!captured.isEnabled()) {
            	captured.setEnabled(true);
            }
		}
    }

    private class ViewUnitListener implements ActionListener {
    	boolean salvage = false;

        public ViewUnitListener(boolean b) {
			salvage = b;
		}

        public ViewUnitListener() {
		}

		@Override
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            UUID id = UUID.fromString(evt.getActionCommand());
            showUnit(id, salvage);
        }
    }

    private class EditUnitListener implements ActionListener {
    	boolean salvage = false;

        public EditUnitListener(boolean b) {
			salvage = b;
		}

		public EditUnitListener() {
		}

		@Override
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            UUID id = UUID.fromString(evt.getActionCommand());
            int idx = Integer.parseInt(((JButton)evt.getSource()).getName());
            editUnit(id, idx, salvage);
        }
    }
}
