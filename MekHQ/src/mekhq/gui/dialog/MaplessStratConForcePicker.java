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
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;

/**
 * Dialog for selecting a force to deploy in a mapless StratCon scenario.
 *
 * <p>This immersive dialog presents the player with a list of available forces to choose from when deploying to a
 * scenario without using the map interface. The dialog is presented in-character through the campaign's command liaison
 * and includes proper handling for cases where no forces are available for deployment.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class MaplessStratConForcePicker extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.MaplessStratConForcePicker";

    public final int SELECTION_CANCELLED = 0;
    public final int SELECTION_CONFIRMED = 1;

    /**
     * Checks whether the user confirmed their force selection.
     *
     * @return {@code true} if the user confirmed their selection, {@code false} if they canceled
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean wasConfirmed() {
        return getDialogChoice() == SELECTION_CONFIRMED;
    }

    /**
     * Creates a new force picker dialog for mapless StratCon deployment.
     *
     * <p>The dialog adapts its presentation based on whether forces are available:</p>
     *
     * <ul>
     *   <li>If forces are available: Shows a dropdown to select from and a confirm button</li>
     *   <li>If no forces are available: Shows an informational message with only a cancel button</li>
     * </ul>
     *
     * @param campaign the current campaign
     * @param forces   the list of available forces the player can choose from
     *
     * @author Illiani
     * @since 0.50.10
     */
    public MaplessStratConForcePicker(Campaign campaign, List<Force> forces) {
        super(campaign,
              campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND),
              null,
              getInCharacterMessage(campaign.getCommanderAddress(), !forces.isEmpty()),
              getButtons(!forces.isEmpty()),
              null,
              ImmersiveDialogWidth.SMALL.getWidth(),
              false,
              getSupplementalPanel(forces),
              null,
              true);
    }

    /**
     * Generates the in-character message displayed in the dialog.
     *
     * <p>The message varies depending on whether forces are available for deployment.</p>
     *
     * @param commanderAddress the formal address/title of the campaign commander
     * @param hasForces        {@code true} if forces are available, {@code false} otherwise
     *
     * @return the formatted in-character message string
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getInCharacterMessage(String commanderAddress, boolean hasForces) {
        String key = "MaplessStratConForcePicker.inCharacterMessage." + (hasForces ? "normal" : "noForces");
        return getFormattedTextAt(RESOURCE_BUNDLE, key, commanderAddress);
    }

    /**
     * Creates the list of buttons to display in the dialog.
     *
     * <p>Always includes a Cancel button. If forces are available, also includes a Confirm button.</p>
     *
     * @param hasForces {@code true} if forces are available for selection, {@code false} otherwise
     *
     * @return a list of button configurations for the dialog
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static List<ButtonLabelTooltipPair> getButtons(boolean hasForces) {
        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();
        buttons.add(new ButtonLabelTooltipPair(getText("Cancel.text"), null));

        if (hasForces) {
            buttons.add(new ButtonLabelTooltipPair(getText("Confirm.text"), null));
        }

        return buttons;
    }

    /**
     * Creates the supplemental panel containing the force selection dropdown.
     *
     * <p>This panel is displayed below the main dialog message and contains a labeled combo box populated with the
     * names of all available forces.</p>
     *
     * @param forces the list of forces to display in the dropdown
     *
     * @return a {@link JPanel} containing the force selection UI
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static JPanel getSupplementalPanel(List<Force> forces) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;

        JLabel lblScenarios = new JLabel(getTextAt(RESOURCE_BUNDLE, "MaplessStratConForcePicker.combo.label"));
        addComponent(panel, lblScenarios, constraints, 0, 1, GridBagConstraints.NONE);

        MMComboBox<String> cboSkills = new MMComboBox<>("cboScenarios", getComboListItems(forces));
        addComponent(panel, cboSkills, constraints, 1, 2, GridBagConstraints.HORIZONTAL);

        return panel;
    }

    /**
     * Utility method to add a component to a panel with specific grid bag constraints.
     *
     * @param panel       the panel to add the component to
     * @param component   the component to add
     * @param constraints the base constraints to use (will be modified)
     * @param gridX       the grid x position
     * @param gridWidth   the grid width to span
     * @param fill        the fill constraint value
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void addComponent(JPanel panel, JComponent component, GridBagConstraints constraints, int gridX,
          int gridWidth, int fill) {
        constraints.gridx = gridX;
        constraints.gridy = 0;
        constraints.gridwidth = gridWidth;
        constraints.fill = fill;
        panel.add(component, constraints);
    }

    /**
     * Converts the list of forces into an array of strings suitable for display in a combo box.
     *
     * <p>Each force is represented by its full hierarchical name in the force structure.</p>
     *
     * @param forces the list of forces to convert
     *
     * @return an array of force names as strings
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String[] getComboListItems(List<Force> forces) {
        List<String> forceOptions = new ArrayList<>();

        for (Force force : forces) {
            String scenarioName = force.getFullName();
            forceOptions.add(scenarioName);
        }

        return forceOptions.toArray(new String[0]);
    }
}
