package mekhq.gui.dialog;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import mekhq.campaign.mission.ScenarioForceTemplate;

public class ScenarioTemplateEditorDialog extends JDialog {
    
    private final Dimension spinnerSize = new Dimension(55, 25);
    
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
        
        JLabel lblReinforcementZones = new JLabel("Possible Reinforcement Zones");
        gbc.gridx++;
        getContentPane().add(lblReinforcementZones, gbc);
        
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
        
        JComboBox<String> cboAlignment = new JComboBox<String>(ScenarioForceTemplate.FORCE_ALIGNMENTS);
        getContentPane().add(cboAlignment, gbc);
        
        JComboBox<String> cboGenerationMethod = new JComboBox<String>(ScenarioForceTemplate.FORCE_GENERATION_METHODS);
        gbc.gridx++;
        getContentPane().add(cboGenerationMethod, gbc);
        
        JSpinner spnMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 2.0, .05));
        spnMultiplier.setPreferredSize(spinnerSize);
        gbc.gridx++;
        getContentPane().add(spnMultiplier, gbc);
        
        DefaultListModel<String> zoneModel = new DefaultListModel<String>();
        for(String s : ScenarioForceTemplate.DEPLOYMENT_ZONES) {
            zoneModel.addElement(s); 
        }
        
        JList<String> lstDeployZones = new JList<String>();
        lstDeployZones.setModel(zoneModel);
        gbc.gridx++;
        getContentPane().add(lstDeployZones, gbc);
        
        JList<String> lstReinforceZones = new JList<String>();
        lstReinforceZones.setModel(zoneModel);
        gbc.gridx++;
        getContentPane().add(lstReinforceZones, gbc);
        
        JSpinner spnRetreatThreshold = new JSpinner(new SpinnerNumberModel(50, 0, 100, 5));
        spnRetreatThreshold.setPreferredSize(spinnerSize);
        gbc.gridx++;
        getContentPane().add(spnRetreatThreshold, gbc);
        
        DefaultListModel<String> unitTypeModel = new DefaultListModel<String>();
        for(String s : ScenarioForceTemplate.UNIT_TYPES) {
            unitTypeModel.addElement(s); 
        }
        
        JList<String> lstUnitTypes = new JList<String>();
        lstUnitTypes.setModel(unitTypeModel);
        gbc.gridx++;
        getContentPane().add(lstUnitTypes, gbc);
        
        JButton btnAdd = new JButton("Add");
        gbc.gridx++;
        getContentPane().add(btnAdd, gbc);
    }
}
