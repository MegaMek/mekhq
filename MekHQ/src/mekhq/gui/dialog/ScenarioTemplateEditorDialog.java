package mekhq.gui.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;

import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioTemplate;

public class ScenarioTemplateEditorDialog extends JDialog implements ActionListener {
    
    private final Dimension spinnerSize = new Dimension(55, 25);
    
    private final static String ADD_FORCE_COMMAND = "ADDFORCE"; 
    
    // controls which need to be accessible across the lifetime of this dialog
    JComboBox<String> cboAlignment;
    JComboBox<String> cboGenerationMethod;
    JSpinner spnMultiplier;
    JList<String> lstDeployZones;
    JComboBox<String> cboDestinationZone;
    JSpinner spnRetreatThreshold;
    JList<String> lstUnitTypes;
    JPanel panForceList;
    
    // the scenario template we're working on
    ScenarioTemplate scenarioTemplate = new ScenarioTemplate(); 
    
    public ScenarioTemplateEditorDialog(Frame parent) {
        super(parent, true);
        initComponents();
        pack();
    }
    
    protected void initComponents() {
        this.setTitle("Scenario Template Editor");
        getContentPane().setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        setupTopFluff(gbc);
        setupForceEditorHeaders(gbc);
        setupForceEditor(gbc);
        initializeForceList(gbc);
    }
    
    /**
     * Sets up text entry boxes in the top
     * @param gbc
     */
    private void setupTopFluff(GridBagConstraints gbc) {
        JLabel lblScenarioName = new JLabel("Scenario Name:");

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.WEST;
        getContentPane().add(lblScenarioName, gbc);
        
        JTextField txtScenarioName = new JTextField();
        txtScenarioName.setColumns(160);
        gbc.gridy++;
        getContentPane().add(txtScenarioName);
        
        JLabel lblScenarioBriefing = new JLabel("Briefing:");
        gbc.gridy++;
        getContentPane().add(lblScenarioBriefing, gbc);
        
        JTextArea txtScenarioBriefing = new JTextArea();
        txtScenarioBriefing.setEditable(true);
        txtScenarioBriefing.setLineWrap(true);
        txtScenarioBriefing.setRows(5);
        txtScenarioBriefing.setColumns(160);
        gbc.gridy++;
        getContentPane().add(txtScenarioBriefing, gbc);
    }
    
    private void setupForceEditorHeaders(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        
        JLabel lblForces = new JLabel("Participating Forces");
        gbc.gridy++;
        getContentPane().add(lblForces, gbc);
        
        gbc.gridy++;
        JLabel lblForceAlignment = new JLabel("Force Alignment");
        getContentPane().add(lblForceAlignment, gbc);
        
        JLabel lblGenerationMethod = new JLabel("Generation Method");
        gbc.gridx++;
        getContentPane().add(lblGenerationMethod, gbc);
        
        JLabel lblMultiplier = new JLabel("Multiplier");
        gbc.gridx++;
        getContentPane().add(lblMultiplier, gbc);
        
        JLabel lblDeploymentZones = new JLabel("Possible Deployment Zones");
        gbc.gridx++;
        getContentPane().add(lblDeploymentZones, gbc);
        
        JLabel lblDestinationZones = new JLabel("Possible Destination Zone");
        gbc.gridx++;
        getContentPane().add(lblDestinationZones, gbc);
        
        JLabel lblRetreatThreshold = new JLabel("Retreat Threshold");
        gbc.gridx++;
        getContentPane().add(lblRetreatThreshold, gbc);
        
        JLabel lblAllowedUnitTypes = new JLabel("Allowed Unit Types");
        gbc.gridx++;
        getContentPane().add(lblAllowedUnitTypes, gbc);
    }
    
    private void setupForceEditor(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        
        cboAlignment = new JComboBox<String>(ScenarioForceTemplate.FORCE_ALIGNMENTS);
        getContentPane().add(cboAlignment, gbc);
        
        cboGenerationMethod = new JComboBox<String>(ScenarioForceTemplate.FORCE_GENERATION_METHODS);
        gbc.gridx++;
        getContentPane().add(cboGenerationMethod, gbc);
        
        spnMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 2.0, .05));
        spnMultiplier.setPreferredSize(spinnerSize);
        gbc.gridx++;
        getContentPane().add(spnMultiplier, gbc);
        
        DefaultListModel<String> zoneModel = new DefaultListModel<String>();
        for(String s : ScenarioForceTemplate.DEPLOYMENT_ZONES) {
            zoneModel.addElement(s); 
        }
        
        lstDeployZones = new JList<String>();
        lstDeployZones.setModel(zoneModel);
        gbc.gridx++;
        getContentPane().add(lstDeployZones, gbc);
        
        cboDestinationZone = new JComboBox<String>(ScenarioForceTemplate.BOT_DESTINATION_ZONES);
        gbc.gridx++;
        getContentPane().add(cboDestinationZone, gbc);
        
        spnRetreatThreshold = new JSpinner(new SpinnerNumberModel(50, 0, 100, 5));
        spnRetreatThreshold.setPreferredSize(spinnerSize);
        gbc.gridx++;
        getContentPane().add(spnRetreatThreshold, gbc);
        
        DefaultListModel<String> unitTypeModel = new DefaultListModel<String>();
        for(String s : ScenarioForceTemplate.UNIT_TYPES) {
            unitTypeModel.addElement(s); 
        }
        
        lstUnitTypes = new JList<String>();
        lstUnitTypes.setModel(unitTypeModel);
        gbc.gridx++;
        getContentPane().add(lstUnitTypes, gbc);
        
        JButton btnAdd = new JButton("Add");
        btnAdd.setActionCommand(ADD_FORCE_COMMAND);
        btnAdd.addActionListener(this);
        gbc.gridx++;
        getContentPane().add(btnAdd, gbc);
    }
        
    private void initializeForceList(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = GridBagConstraints.RELATIVE;
        panForceList = new JPanel(new GridBagLayout());
        panForceList.setBorder(new LineBorder(Color.GREEN));

        renderForceList();
        
        getContentPane().add(panForceList, gbc);
    }
    
    private void renderForceList() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.ipadx = 5;
        gbc.ipady = 5;
        
        panForceList.removeAll();
        
        for(ScenarioForceTemplate sft : scenarioTemplate.scenarioForces) {
            JLabel lblForceAlignment = new JLabel(ScenarioForceTemplate.FORCE_ALIGNMENTS[sft.getForceAlignment()]);
            panForceList.add(lblForceAlignment, gbc);
            
            JLabel lblGenerationMethod = new JLabel(ScenarioForceTemplate.FORCE_GENERATION_METHODS[sft.getGenerationMethod()]);
            gbc.gridx++;
            panForceList.add(lblGenerationMethod, gbc);
            
            JLabel lblMultiplier = new JLabel(((Double) sft.getForceMultiplier()).toString());
            gbc.gridx++;
            panForceList.add(lblMultiplier, gbc);
            
            JLabel lblDeploymentZones = new JLabel("PDZ");
            gbc.gridx++;
            panForceList.add(lblDeploymentZones, gbc);
            
            JLabel lblDestinationZones = new JLabel(ScenarioForceTemplate.BOT_DESTINATION_ZONES[sft.getDestinationZone()]);
            gbc.gridx++;
            panForceList.add(lblDestinationZones, gbc);
            
            JLabel lblRetreatThreshold = new JLabel(((Double) sft.getRetreatThreshold()).toString());
            gbc.gridx++;
            panForceList.add(lblRetreatThreshold, gbc);
            
            JLabel lblAllowedUnitTypes = new JLabel("AUT");
            gbc.gridx++;
            panForceList.add(lblAllowedUnitTypes, gbc);
            
            gbc.gridy++;
            gbc.gridx = 0;
        }
        
        panForceList.setPreferredSize(panForceList.getSize());
    }
    
    
    private void addButtonHandler() {
        int forceAlignment = cboAlignment.getSelectedIndex();
        int generationMethod = cboGenerationMethod.getSelectedIndex();
        double forceMultiplier = (double) spnMultiplier.getValue();
        
        List<Integer> deploymentZones = new ArrayList<>();
        for(int x : lstDeployZones.getSelectedIndices()) {
            deploymentZones.add(x);
        }
        
        int destinationZone = cboDestinationZone.getSelectedIndex();
        double retreatThreshold = (int) spnRetreatThreshold.getValue() / 100.0;
        
        List<Integer> allowedUnitTypes = new ArrayList<>();
        for(int x : lstUnitTypes.getSelectedIndices()) {
            allowedUnitTypes.add(x);
        }
        
        ScenarioForceTemplate sft = new ScenarioForceTemplate(forceAlignment, generationMethod, forceMultiplier,
                deploymentZones, destinationZone, retreatThreshold, allowedUnitTypes);
        scenarioTemplate.scenarioForces.add(sft);
        
        renderForceList();
        this.pack();
        this.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand() == ADD_FORCE_COMMAND) {
            addButtonHandler();
        }
        
    }
}
