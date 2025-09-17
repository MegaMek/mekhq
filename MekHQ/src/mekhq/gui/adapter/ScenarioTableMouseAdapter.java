/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.adapter;

import java.util.Optional;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import mekhq.MekHQ;
import mekhq.campaign.events.scenarios.ScenarioChangedEvent;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.CustomizeScenarioDialog;
import mekhq.gui.model.ScenarioTableModel;

public class ScenarioTableMouseAdapter extends JPopupMenuAdapter {
    //region Variable Declarations
    private final CampaignGUI gui;
    private final JTable scenarioTable;
    private final ScenarioTableModel scenarioModel;
    //endregion Variable Declarations

    protected ScenarioTableMouseAdapter(CampaignGUI gui, JTable scenarioTable, ScenarioTableModel scenarioModel) {
        this.gui = gui;
        this.scenarioTable = scenarioTable;
        this.scenarioModel = scenarioModel;
    }

    public static void connect(CampaignGUI gui, JTable scenarioTable, ScenarioTableModel scenarioModel) {
        new ScenarioTableMouseAdapter(gui, scenarioTable, scenarioModel)
              .connect(scenarioTable);
    }

    @Override
    protected Optional<JPopupMenu> createPopupMenu() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return Optional.empty();
        }

        JPopupMenu popup = new JPopupMenu();

        Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
        JMenuItem menuItem;
        JMenu menu;

        // let's fill the pop-up menu
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

        return Optional.of(popup);
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
            gui.getCampaign().removeScenario(scenario);
        }
    }
}
