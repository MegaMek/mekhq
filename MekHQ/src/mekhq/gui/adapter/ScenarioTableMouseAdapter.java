/*
 * Copyright (c) 2014-2020 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.adapter;

import java.awt.event.MouseEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;

import mekhq.MekHQ;
import mekhq.campaign.event.ScenarioChangedEvent;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.CustomizeScenarioDialog;
import mekhq.gui.model.ScenarioTableModel;

public class ScenarioTableMouseAdapter extends MouseInputAdapter {
    //region Variable Declarations
    private CampaignGUI gui;
    private JTable scenarioTable;
    private ScenarioTableModel scenarioModel;
    //endregion Variable Declarations

    public ScenarioTableMouseAdapter(CampaignGUI gui, JTable scenarioTable, ScenarioTableModel scenarioModel) {
        super();
        this.gui = gui;
        this.scenarioTable = scenarioTable;
        this.scenarioModel = scenarioModel;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        if (e.isPopupTrigger() && (scenarioTable.getSelectedRowCount() > 0)) {
            int row = scenarioTable.getSelectedRow();
            if (row < 0) {
                return;
            }
            Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
            JMenuItem menuItem;
            JMenu menu;

            // lets fill the pop up menu
            menuItem = new JMenuItem("Edit...");
            menuItem.addActionListener(evt -> editScenario(scenario));
            popup.add(menuItem);

            //region GM mode
            if (gui.getCampaign().isGM()) {
                popup.addSeparator();

                menu = new JMenu("GM Mode");
                // remove scenario
                menuItem = new JMenuItem("Remove Scenario");
                menuItem.addActionListener(evt -> removeScenario(scenario));
                menu.add(menuItem);

                popup.add(menu);
            }

            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void editScenario(Scenario scenario) {
        Mission mission = gui.getCampaign().getMission(scenario.getMissionId());
        if (mission != null) {
            CustomizeScenarioDialog csd = new CustomizeScenarioDialog(gui.getFrame(), true,
                    scenario, mission, gui.getCampaign());
            csd.setVisible(true);
            MekHQ.triggerEvent(new ScenarioChangedEvent(scenario));
        }
    }

    private void removeScenario(Scenario scenario) {
        if (0 == JOptionPane.showConfirmDialog(null,
                "Do you really want to delete the scenario?",
                "Delete Scenario?", JOptionPane.YES_NO_OPTION)) {
            gui.getCampaign().removeScenario(scenario.getId());
        }
    }
}
