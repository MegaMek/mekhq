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
    private JList<Unit> unitList = new JList<>();
    private JList<ScenarioForceTemplate> templateList = new JList<>();
    private JButton btnAssign = new JButton();
    private JButton btnOk = new JButton();
    
    private ResourceBundle resourceMap;
    private AtBDynamicScenario currentScenario;
    private Vector<Force> currentForceVector;
    private Vector<Unit> currentUnitVector;
    private CampaignGUI campaignGUI;
    
    public ForceTemplateAssignmentDialog(CampaignGUI gui, Vector<Force> assignedForces, Vector<Unit> assignedUnits, AtBDynamicScenario scenario) {
        currentForceVector = assignedForces;
        currentUnitVector = assignedUnits;

        //resourceMap = ResourceBundle.getBundle("mekhq.resources.ForceTemplateAssignmentDialog", new EncodeControl());
        currentScenario = scenario;
        campaignGUI = gui;
        
        btnAssign.setText("Assign");//resourceMap.getString("chkExportSettings.text"));
        btnOk.setText("Ok");//resourceMap.getString("chkExportContractOffers.text"));
        lblInstructions.setText("Pick your force template assignments");//resourceMap.getString("lblMoney.text"));   
        
        setupTemplateList();
        display(currentForceVector == null);
    }
    
    private void display(boolean individualUnits) {
        getContentPane().removeAll();
        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        getContentPane().add(lblInstructions, gbc);
        
        gbc.gridy++;
        
        if(individualUnits) {
            getContentPane().add(unitList, gbc);
            refreshUnitList();
        } else {
            getContentPane().add(forceList, gbc);
            refreshForceList();
        }
        gbc.gridx++;
        getContentPane().add(templateList, gbc);
        gbc.gridx++;
        getContentPane().add(btnAssign, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        getContentPane().add(btnOk, gbc);
        
        if(individualUnits) {
            btnAssign.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    assignUnitToTemplate();
                }
            });
        } else {
            btnAssign.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    assignForceToTemplate();
                }
            });
        }
        
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
    
    private void refreshUnitList() {
        DefaultListModel<Unit> unitListModel = new DefaultListModel<>();
        for(Unit unit : currentUnitVector) {
            unitListModel.addElement(unit);
        }
        unitList.setModel(unitListModel);
        unitList.setCellRenderer(new UnitListCellRenderer());
        unitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        unitList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateAssignButtonState();
            }
        });
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
        if(((forceList.getSelectedIndex() >= 0) ||
                (unitList.getSelectedIndex() >= 0)) &&
                (templateList.getSelectedIndex() >= 0)) {
            btnAssign.setEnabled(true);
        } else {
            btnAssign.setEnabled(false);
        }
    }
    
    /**
     * Event handler for assigning a unit to a scenario and a specific template
     */
    private void assignUnitToTemplate() {
        Unit unit = unitList.getSelectedValue();
        
        currentScenario.removeUnit(unit.getId());
        currentScenario.addUnit(unit.getId(), templateList.getSelectedValue().getForceName());
        unit.setScenarioId(currentScenario.getId());
        MekHQ.triggerEvent(new DeploymentChangedEvent(unit, currentScenario));
        
        refreshUnitList();
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
        
    private class UnitListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Unit unit = (Unit) value;
            String cellValue = currentScenario.getPlayerUnitTemplates().containsKey(unit.getId()) ?
                    String.format("%s (%s)", unit.getName(), currentScenario.getPlayerUnitTemplates().get(unit.getId()).getForceName()) :
                        unit.getName();
            ((JLabel) cmp).setText(cellValue);
            return cmp;
        }
    }
    
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
