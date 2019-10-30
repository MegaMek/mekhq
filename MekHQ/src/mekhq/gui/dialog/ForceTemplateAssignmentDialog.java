package mekhq.gui.dialog;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import mekhq.MekHQ;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;

public class ForceTemplateAssignmentDialog extends JDialog {
    /**
     * 
     */
    private static final long serialVersionUID = -7171621116865584010L;
    
    private JLabel lblInstructions = new JLabel();
    private JList<Force> forceList = new JList<>();
    private JList<ScenarioForceTemplate> templateList = new JList<>();
    private JButton btnAssign = new JButton();
    private JButton btnOk = new JButton();
    
    private ResourceBundle resourceMap;
    private AtBDynamicScenario currentScenario;
    private Vector<Force> currentForceVector;
    private CampaignGUI campaignGUI;
    
    public ForceTemplateAssignmentDialog(CampaignGUI gui, Vector<Force> assignedForces, AtBDynamicScenario scenario) {
        //resourceMap = ResourceBundle.getBundle("mekhq.resources.ForceTemplateAssignmentDialog", new EncodeControl());
        currentScenario = scenario;
        currentForceVector = assignedForces;
        campaignGUI = gui;
        
        btnAssign.setText("Assign");//resourceMap.getString("chkExportSettings.text"));
        btnOk.setText("Ok");//resourceMap.getString("chkExportContractOffers.text"));
        lblInstructions.setText("Pick your force template assignments");//resourceMap.getString("lblMoney.text"));   
        
        refreshForceList();
        setupTemplateList();
        display();
    }
    
    public void display() {
        getContentPane().removeAll();
        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        getContentPane().add(lblInstructions, gbc);
        
        gbc.gridy++;
        
        getContentPane().add(forceList, gbc);
        gbc.gridx++;
        getContentPane().add(templateList, gbc);
        gbc.gridx++;
        getContentPane().add(btnAssign, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        getContentPane().add(btnOk, gbc);
        
        btnAssign.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                assignForceToTemplate();
            }
        });
        
        btnAssign.setEnabled(false);
        
        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        
        validate();
        pack();
        setLocationRelativeTo(getParent());
        setModalityType(ModalityType.APPLICATION_MODAL);
        setVisible(true);
    }
    
    private void refreshForceList() {
        DefaultListModel<Force> forceListModel = new DefaultListModel<>();
        for(Force force : currentForceVector) {
            forceListModel.addElement(force);
        }
        forceList.setModel(forceListModel);
        forceList.setCellRenderer(new ForceListCellRenderer());
        forceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        forceList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateAssignButtonState();
            }
        });
    }
    
    private void setupTemplateList() {
        DefaultListModel<ScenarioForceTemplate> templateListModel = new DefaultListModel<>();
        for(ScenarioForceTemplate forceTemplate : currentScenario.getTemplate().scenarioForces.values()) {
            if(forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerSupplied.ordinal() ||
                    forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerOrFixedUnitCount.ordinal()) {
                templateListModel.addElement(forceTemplate);
            }
        }
        
        if(!currentScenario.getTemplate().scenarioForces.containsKey(ScenarioForceTemplate.REINFORCEMENT_TEMPLATE_ID)) {
            templateListModel.addElement(ScenarioForceTemplate.getDefaultReinforcementsTemplate());
        }
        
        templateList.setModel(templateListModel);
        templateList.setCellRenderer(new TemplateListCellRenderer());
        templateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        templateList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateAssignButtonState();
            }
        });
    }
    
    /**
     * Handles logic for updating the assign button state.
     */
    private void updateAssignButtonState() {
        if(forceList.getSelectedIndex() >= 0 &&
                templateList.getSelectedIndex() >= 0) {
            btnAssign.setEnabled(true);
        } else {
            btnAssign.setEnabled(false);
        }
    }
    
    /**
     * Event handler for assigning a force to a scenario and specific template
     */
    private void assignForceToTemplate() {
        Force force = forceList.getSelectedValue();
        int forceID = forceList.getSelectedValue().getId();
        
        // all this stuff apparently needs to happen when assigning a force to a scenario
        campaignGUI.undeployForce(force);
        force.clearScenarioIds(campaignGUI.getCampaign(), true);
        force.setScenarioId(currentScenario.getId());
        currentScenario.addForce(forceID, templateList.getSelectedValue().getForceName());
        for (UUID uid : force.getAllUnits()) {
            Unit u = campaignGUI.getCampaign().getUnit(uid);
            if (null != u) {
                u.setScenarioId(currentScenario.getId());
            }
        }
        MekHQ.triggerEvent(new DeploymentChangedEvent(force, currentScenario));
        
        refreshForceList();
    }
    
    /*private void setupPersonList() {
        personList = new JList<>();
        DefaultListModel<Person> personListModel = new DefaultListModel<>();
        for(Person person : sourceCampaign.getActivePersonnel()) {
            personListModel.addElement(person);
        }
        personList.setModel(personListModel);
        personList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                lblStatus.setText(getPersonSelectionStatus());
                pack();
            }
        });
        personList.setCellRenderer(new PersonListCellRenderer());
    }
    
    private void setupUnitList() {
        unitList = new JList<>();
        DefaultListModel<Unit> unitListModel = new DefaultListModel<>();
        for(Unit unit : sourceCampaign.getUnits()) {
            unitListModel.addElement(unit);
        }
        unitList.setModel(unitListModel);
        unitList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                lblStatus.setText(getUnitSelectionStatus());
                pack();
            }
        });
        unitList.setCellRenderer(new UnitListCellRenderer());
    }
    
    private void setupPartList() {
        partList = new JList<>();
        DefaultListModel<Part> partListModel = new DefaultListModel<>();
        List<Part> parts = sourceCampaign.getSpareParts();
        parts.sort(new Comparator<Part>() {
            @Override
            public int compare(Part o1, Part o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        
        for(Part part : parts) {
            // if the part isn't part of some other activity
            if(!part.isReservedForRefit() &&
                    !part.isReservedForReplacement() &&
                    !part.isBeingWorkedOn() &&
                    part.isPresent() &&
                    part.isSpare()) {
                partListModel.addElement(part);
            }
        }
        partList.setModel(partListModel);    
    }*/
    
    private class ForceListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Force force = (Force) value;
            String cellValue = currentScenario.getPlayerForceTemplates().containsKey(force.getId()) ?
                    String.format("%s (%s)", force.getName(), currentScenario.getPlayerForceTemplates().get(force.getId()).getForceName()) :
                    force.getName();
            ((JLabel) cmp).setText(cellValue);
            return cmp;
        }
    }
    
    private class TemplateListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            ScenarioForceTemplate template = (ScenarioForceTemplate) value;
            String cellValue = String.format("%s (%s)", template.getForceName(), template.getAllowedUnitTypeName());
            ((JLabel) cmp).setText(cellValue);
            return cmp;
        }
    }
}
