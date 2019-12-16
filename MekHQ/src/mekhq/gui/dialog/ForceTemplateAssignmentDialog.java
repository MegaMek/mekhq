/*
 * Copyright (C) 2011-2016, 2019 MegaMek team
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;

/**
 * Class that handles the GUI for assigning forces and units to individual templates
 * associated with a dynamic scenario.
 * @author NickAragua
 *
 */
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
    private JButton btnClose = new JButton();
    
    private ResourceBundle resourceMap;
    private AtBDynamicScenario currentScenario;
    private Vector<Force> currentForceVector;
    private Vector<Unit> currentUnitVector;
    private CampaignGUI campaignGUI;
    
    public ForceTemplateAssignmentDialog(CampaignGUI gui, Vector<Force> assignedForces, Vector<Unit> assignedUnits, AtBDynamicScenario scenario) {
        currentForceVector = assignedForces;
        currentUnitVector = assignedUnits;

        resourceMap = ResourceBundle.getBundle("mekhq.resources.ForceTemplateAssignmentDialog", new EncodeControl());
        currentScenario = scenario;
        campaignGUI = gui;
        
        btnAssign.setText(resourceMap.getString("btnAssign.text"));
        btnClose.setText(resourceMap.getString("btnClose.text"));
        lblInstructions.setText(String.format("<html>%s</html>", resourceMap.getString("lblInstructions.text")));   
        
        setupTemplateList();
        display(currentForceVector == null);
    }
    
    private void display(boolean individualUnits) {
        getContentPane().removeAll();
        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        getContentPane().add(lblInstructions, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy++;
        
        JScrollPane itemListPane = new JScrollPane();
        if(individualUnits) {
            itemListPane.setViewportView(unitList);
            refreshUnitList();
        } else {
            itemListPane.setViewportView(forceList);
            refreshForceList();
        }
        getContentPane().add(itemListPane, gbc);
        gbc.gridx++;
        
        JScrollPane templateListPane = new JScrollPane();
        templateListPane.setViewportView(templateList);
        itemListPane.setPreferredSize(
                new Dimension((int) itemListPane.getPreferredSize().getWidth() + (int) templateListPane.getPreferredSize().getWidth(), 
                        (int) itemListPane.getPreferredSize().getHeight()));
        
        getContentPane().add(templateListPane, gbc);
        gbc.gridx++;
        getContentPane().add(btnAssign, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        getContentPane().add(btnClose, gbc);
        
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
        
        btnClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        
        validate();
        pack();
        setResizable(false);
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
        if (!unit.getTransportedUnits().isEmpty()) {
            // Prompt the player to also deploy any units transported by this one
            if (0 == JOptionPane.showConfirmDialog(
                    null,
                    unit.getName() +  " is a transport with units assigned to it. \n"
                            + "Would you also like to deploy these units?",
                            "Also deploy transported units?", JOptionPane.YES_NO_OPTION)) {
                for (UUID id : unit.getTransportedUnits()) {
                    Unit cargo = unit.getCampaign().getUnit(id);
                    if (cargo != null) {
                        currentScenario.removeUnit(cargo.getId());
                        currentScenario.addUnit(cargo.getId(), templateList.getSelectedValue().getForceName());
                        cargo.setScenarioId(currentScenario.getId());
                        MekHQ.triggerEvent(new DeploymentChangedEvent(cargo, currentScenario));
                    }
                }
            }
        }
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
                // If your force includes transports with units assigned, 
                // prompt the player to also deploy any units transported by this one
                if (!u.getTransportedUnits().isEmpty()) {
                    int optionChoice = JOptionPane.showConfirmDialog(null,
                            u.getName() +  " is a transport with units assigned to it. \n"
                                    + "Would you also like to deploy these units?",
                                    "Also deploy transported units?", JOptionPane.YES_NO_OPTION);
                    if (optionChoice == JOptionPane.YES_OPTION) {
                        for (UUID id : u.getTransportedUnits()) {
                            Unit cargo = u.getCampaign().getUnit(id);
                            if (cargo != null) {
                                currentScenario.removeUnit(cargo.getId());
                                currentScenario.addUnit(cargo.getId(), templateList.getSelectedValue().getForceName());
                                cargo.setScenarioId(currentScenario.getId());
                                MekHQ.triggerEvent(new DeploymentChangedEvent(cargo, currentScenario));
                            }
                        }
                    }
                }
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
    
    private static class TemplateListCellRenderer extends DefaultListCellRenderer {
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
