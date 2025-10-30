/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Scenario;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;

public class MaplessStratConScenarioPicker extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.MaplessStratConScenarioPicker";

    public final int SCENARIO_SELECTION_CANCELLED = 0;
    public final int SCENARIO_SELECTION_CONFIRMED = 1;

    public boolean wasConfirmed() {
        return getDialogChoice() == SCENARIO_SELECTION_CONFIRMED;
    }

    public MaplessStratConScenarioPicker(Campaign campaign, List<Scenario> scenarios) {
        super(campaign,
              campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND),
              null,
              getInCharacterMessage(campaign.getCommanderAddress(), !scenarios.isEmpty()),
              getButtons(!scenarios.isEmpty()),
              null,
              ImmersiveDialogWidth.SMALL.getWidth(),
              false,
              getSupplementalPanel(scenarios),
              null,
              true);
    }

    private static String getInCharacterMessage(String commanderAddress, boolean hasScenarios) {
        String key = "MaplessStratConScenarioPicker.inCharacterMessage." + (hasScenarios ? "normal" : "noScenarios");
        return getFormattedTextAt(RESOURCE_BUNDLE, key, commanderAddress);
    }

    private static List<ButtonLabelTooltipPair> getButtons(boolean hasScenarios) {
        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();
        buttons.add(new ButtonLabelTooltipPair(getText("Cancel.text"), null));

        if (hasScenarios) {
            buttons.add(new ButtonLabelTooltipPair(getText("Confirm.text"), null));
        }

        return buttons;
    }

    private static JPanel getSupplementalPanel(List<Scenario> scenarios) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;

        JLabel lblScenarios = new JLabel(getTextAt(RESOURCE_BUNDLE, "MaplessStratConScenarioPicker.combo.label"));
        addComponent(panel, lblScenarios, constraints, 0, 1, GridBagConstraints.NONE);

        MMComboBox<String> cboSkills = new MMComboBox<>("cboScenarios", getComboListItems(scenarios));
        addComponent(panel, cboSkills, constraints, 1, 2, GridBagConstraints.HORIZONTAL);

        return panel;
    }

    private static void addComponent(JPanel panel, JComponent component, GridBagConstraints constraints, int gridX,
          int gridWidth, int fill) {
        constraints.gridx = gridX;
        constraints.gridy = 0;
        constraints.gridwidth = gridWidth;
        constraints.fill = fill;
        panel.add(component, constraints);
    }

    private static String[] getComboListItems(List<Scenario> scenarios) {
        List<String> scenarioOptions = new ArrayList<>();

        for (Scenario scenario : scenarios) {
            String scenarioName = scenario.getName();
            LocalDate scenarioDueDate = scenario.getDate();

            scenarioOptions.add(scenarioName + " (" + scenarioDueDate + ")");
        }

        return scenarioOptions.toArray(new String[0]);
    }
}
