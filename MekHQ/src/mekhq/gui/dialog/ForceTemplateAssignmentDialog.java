/*
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.Vector;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import mekhq.MekHQ;
import mekhq.campaign.events.DeploymentChangedEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * Class that handles the GUI for assigning forces and units to individual templates associated with a dynamic
 * scenario.
 *
 * @author NickAragua
 */
public class ForceTemplateAssignmentDialog extends JDialog {
    private final JLabel lblInstructions = new JLabel();
    private final JList<Force> forceList = new JList<>();
    private final JList<Unit> unitList = new JList<>();
    private final JList<ScenarioForceTemplate> templateList = new JList<>();
    private final JButton btnAssign = new JButton();
    private final JButton btnClose = new JButton();

    private final AtBDynamicScenario currentScenario;
    private final Vector<Force> currentForceVector;
    private final Vector<Unit> currentUnitVector;
    private final CampaignGUI campaignGUI;

    // FIXME : Unlocalized text
    private static final String DEPLOY_TRANSPORTED_DIALOG_TEXT = " is a transport with units assigned to it. \n" +
                                                                       "Would you also like to deploy these units?";
    private static final String DEPLOY_TRANSPORTED_DIALOG_TITLE = "Also deploy transported units?";

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle(
          "mekhq.resources.ForceTemplateAssignmentDialog",
          MekHQ.getMHQOptions().getLocale());

    public ForceTemplateAssignmentDialog(CampaignGUI gui, Vector<Force> assignedForces, Vector<Unit> assignedUnits,
          AtBDynamicScenario scenario) {
        currentForceVector = assignedForces;
        currentUnitVector = assignedUnits;

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

        JScrollPane itemListPane = new JScrollPaneWithSpeed();
        if (individualUnits) {
            itemListPane.setViewportView(unitList);
            refreshUnitList();
        } else {
            itemListPane.setViewportView(forceList);
            refreshForceList();
        }
        getContentPane().add(itemListPane, gbc);
        gbc.gridx++;

        JScrollPane templateListPane = new JScrollPaneWithSpeed();
        templateListPane.setViewportView(templateList);
        itemListPane.setPreferredSize(
              new Dimension((int) itemListPane.getPreferredSize().getWidth() +
                                  (int) templateListPane.getPreferredSize().getWidth(),
                    (int) itemListPane.getPreferredSize().getHeight()));

        getContentPane().add(templateListPane, gbc);
        gbc.gridx++;
        getContentPane().add(btnAssign, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        getContentPane().add(btnClose, gbc);

        if (individualUnits) {
            btnAssign.addActionListener(e -> assignUnitToTemplate());
        } else {
            btnAssign.addActionListener(e -> assignForceToTemplate());
        }

        btnAssign.setEnabled(false);

        btnClose.addActionListener(e -> setVisible(false));

        validate();
        pack();
        setResizable(false);
        setLocationRelativeTo(getParent());
        setModalityType(ModalityType.APPLICATION_MODAL);
        setVisible(true);
    }

    private void refreshUnitList() {
        DefaultListModel<Unit> unitListModel = new DefaultListModel<>();
        for (Unit unit : currentUnitVector) {
            unitListModel.addElement(unit);
        }
        unitList.setModel(unitListModel);
        unitList.setCellRenderer(new UnitListCellRenderer());
        unitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        unitList.addListSelectionListener(e -> updateAssignButtonState());
    }

    private void refreshForceList() {
        DefaultListModel<Force> forceListModel = new DefaultListModel<>();
        for (Force force : currentForceVector) {
            forceListModel.addElement(force);
        }
        forceList.setModel(forceListModel);
        forceList.setCellRenderer(new ForceListCellRenderer());
        forceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        forceList.addListSelectionListener(e -> updateAssignButtonState());
    }

    private void setupTemplateList() {
        DefaultListModel<ScenarioForceTemplate> templateListModel = new DefaultListModel<>();
        for (ScenarioForceTemplate forceTemplate : currentScenario.getTemplate().getAllScenarioForces()) {
            if (forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerSupplied.ordinal() ||
                      forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerOrFixedUnitCount.ordinal()) {
                templateListModel.addElement(forceTemplate);
            }
        }

        templateList.setModel(templateListModel);
        templateList.setCellRenderer(new TemplateListCellRenderer());
        templateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        templateList.addListSelectionListener(e -> updateAssignButtonState());
    }

    /**
     * Handles logic for updating the assign button state.
     */
    private void updateAssignButtonState() {
        btnAssign.setEnabled(((forceList.getSelectedIndex() >= 0) ||
                                    (unitList.getSelectedIndex() >= 0)) &&
                                   (templateList.getSelectedIndex() >= 0));
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
        if (unit.hasShipTransportedUnits()) {
            // Prompt the player to also deploy any units transported by this one
            deployTransportedUnitsDialog(unit);
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
        force.setScenarioId(currentScenario.getId(), campaignGUI.getCampaign());
        currentScenario.addForce(forceID, templateList.getSelectedValue().getForceName());
        for (UUID uid : force.getAllUnits(true)) {
            Unit u = campaignGUI.getCampaign().getUnit(uid);
            if (null != u) {
                u.setScenarioId(currentScenario.getId());
                // If your force includes transports with units assigned,
                // prompt the player to also deploy any units transported by this one
                if (u.hasShipTransportedUnits()) {
                    deployTransportedUnitsDialog(u);
                }
            }
        }
        MekHQ.triggerEvent(new DeploymentChangedEvent(force, currentScenario));

        refreshForceList();
    }

    /**
     * Worker function that prompts the player to deploy any units assigned to transport to a scenario when the
     * transport is deployed to that scenario
     *
     * @param unit The transport unit whose name and cargo we wish to deal with
     */
    private void deployTransportedUnitsDialog(Unit unit) {
        int optionChoice = JOptionPane.showConfirmDialog(null,
              unit.getName() + ForceTemplateAssignmentDialog.DEPLOY_TRANSPORTED_DIALOG_TEXT,
              ForceTemplateAssignmentDialog.DEPLOY_TRANSPORTED_DIALOG_TITLE, JOptionPane.YES_NO_OPTION);
        if (optionChoice == JOptionPane.YES_OPTION) {
            deployTransportedUnits(unit);
        }
    }

    private void deployTransportedUnits(final Unit unit) {
        for (final Unit cargo : unit.getShipTransportedUnits()) {
            currentScenario.removeUnit(cargo.getId());
            currentScenario.addUnit(cargo.getId(), templateList.getSelectedValue().getForceName());
            cargo.setScenarioId(currentScenario.getId());
            MekHQ.triggerEvent(new DeploymentChangedEvent(cargo, currentScenario));

            if (cargo.hasShipTransportedUnits()) {
                deployTransportedUnits(cargo);
            }
        }
    }

    private class UnitListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Unit unit = (Unit) value;
            String cellValue = currentScenario.getPlayerUnitTemplates().containsKey(unit.getId()) ?
                                     String.format("%s (%s)",
                                           unit.getName(),
                                           currentScenario.getPlayerUnitTemplates().get(unit.getId()).getForceName()) :
                                     unit.getName();
            ((JLabel) cmp).setText(cellValue);
            return cmp;
        }
    }

    private class ForceListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Force force = (Force) value;
            String cellValue = currentScenario.getPlayerForceTemplates().containsKey(force.getId()) ?
                                     String.format("%s (%s)",
                                           force.getName(),
                                           currentScenario.getPlayerForceTemplates()
                                                 .get(force.getId())
                                                 .getForceName()) :
                                     force.getName();
            ((JLabel) cmp).setText(cellValue);
            return cmp;
        }
    }

    private static class TemplateListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            ScenarioForceTemplate template = (ScenarioForceTemplate) value;
            String cellValue = String.format("%s (%s)", template.getForceName(), template.getAllowedUnitTypeName());
            ((JLabel) cmp).setText(cellValue);
            return cmp;
        }
    }
}
