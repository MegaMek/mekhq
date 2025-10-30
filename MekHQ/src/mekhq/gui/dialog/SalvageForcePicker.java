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

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.campaign.mission.camOpsSalvage.CamOpsSalvageUtilities.getSalvageTooltip;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;

import megamek.common.annotations.Nullable;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * A dialog that allows the user to select forces for salvage operations before starting a scenario.
 *
 * <p>This dialog presents the user with a list of available forces that can perform salvage operations, displaying
 * each force's name and the number of units capable of salvage. The forces are presented as checkboxes arranged in a
 * three-column layout.</p>
 *
 * <p>The dialog provides context-appropriate messaging based on whether forces are available, and only allows
 * confirmation if at least one force is available for selection.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class SalvageForcePicker extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SalvageForcePicker";
    private static final int NUM_COLUMNS = 3;

    public final int SELECTION_CANCELLED = 0;
    public final int SELECTION_CONFIRMED = 1;

    private static Map<JCheckBox, Force> checkboxForceMap;

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
     * Retrieves the list of forces that were selected by the user.
     *
     * <p>This method examines all checkboxes in the dialog and returns a list of forces corresponding
     * to the checked checkboxes. If no checkboxes are selected or the dialog was canceled, an empty list is
     * returned.</p>
     *
     * @return a list of selected {@link Force} objects, or an empty list if none were selected
     *
     * @author Illiani
     * @since 0.50.10
     */
    public List<Force> getSelectedForces() {
        List<Force> selectedForces = new ArrayList<>();

        if (checkboxForceMap != null) {
            for (Map.Entry<JCheckBox, Force> entry : checkboxForceMap.entrySet()) {
                if (entry.getKey().isSelected()) {
                    selectedForces.add(entry.getValue());
                }
            }
        }

        return selectedForces;
    }


    /**
     * Creates a new salvage force picker dialog.
     *
     * @param campaign the current campaign
     * @param scenario the scenario for which salvage forces are being selected
     * @param forces   the list of available forces that can perform salvage operations
     *
     * @author Illiani
     * @since 0.50.10
     */
    public SalvageForcePicker(Campaign campaign, Scenario scenario, List<Force> forces) {
        super(campaign,
              campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND),
              null,
              getInCharacterMessage(campaign.getCommanderAddress(), !forces.isEmpty()),
              getButtons(!forces.isEmpty()),
              getOutOfCharacterMessage(),
              ImmersiveDialogWidth.SMALL.getWidth(),
              false,
              getSupplementalPanel(scenario.getBoardType() == AtBScenario.T_SPACE, campaign.getHangar(), forces),
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
        String key = "SalvageForcePicker.inCharacterMessage." + (hasForces ? "normal" : "noForces");
        return getFormattedTextAt(RESOURCE_BUNDLE, key, commanderAddress);
    }


    /**
     * Generates the out-of-character message displayed in the dialog.
     *
     * @return the out-of-character message string
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getOutOfCharacterMessage() {
        String key = "SalvageForcePicker.outOfCharacterMessage.salvage";
        return getTextAt(RESOURCE_BUNDLE, key);
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
    private static List<ImmersiveDialogCore.ButtonLabelTooltipPair> getButtons(boolean hasForces) {
        List<ImmersiveDialogCore.ButtonLabelTooltipPair> buttons = new ArrayList<>();
        buttons.add(new ImmersiveDialogCore.ButtonLabelTooltipPair(getText("Cancel.text"), null));

        if (hasForces) {
            buttons.add(new ImmersiveDialogCore.ButtonLabelTooltipPair(getText("Confirm.text"), null));
        }

        return buttons;
    }

    /**
     * Creates the supplemental panel containing force selection checkboxes.
     *
     * <p>This panel is displayed below the main dialog message and contains checkboxes arranged in three
     * columns. Each checkbox represents a force that can be selected for salvage operations. The checkboxes are labeled
     * with the force's name.</p>
     *
     * @param isInSpace {@code true} if the scenario is a space scenario
     * @param hangar    the current campaign hangar
     * @param forces    the list of forces to display as checkboxes
     *
     * @return a {@link JPanel} containing the force selection UI with checkboxes arranged in three columns
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static @Nullable JPanel getSupplementalPanel(boolean isInSpace, Hangar hangar, List<Force> forces) {
        if (forces.isEmpty()) {
            return null;
        }

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblForces = new JLabel(getTextAt(RESOURCE_BUNDLE, "SalvageForcePicker.combo.label"));
        panel.add(lblForces, constraints);

        // Create panel with three columns
        JPanel checkboxPanel = new JPanel(new GridLayout(1, NUM_COLUMNS, 10, 0));
        checkboxPanel.setBorder(RoundedLineBorder.createRoundedLineBorder());
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        JPanel middleColumn = new JPanel();
        middleColumn.setLayout(new BoxLayout(middleColumn, BoxLayout.Y_AXIS));
        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));

        checkboxForceMap = new LinkedHashMap<>();

        // Create checkboxes for each force
        for (int i = 0; i < forces.size(); i++) {
            Force force = forces.get(i);
            List<Unit> allUnitsInForce = force.getAllUnitsAsUnits(hangar, false);
            JCheckBox checkbox =
                  new JCheckBox(force.getFullName() + " (" + force.getSalvageUnitCount(hangar, isInSpace) + ")");
            checkbox.setToolTipText(wordWrap(getSalvageTooltip(allUnitsInForce, isInSpace)));
            checkbox.setAlignmentX(JComponent.LEFT_ALIGNMENT);

            checkboxForceMap.put(checkbox, force);

            // Distribute checkboxes across three columns
            if (i % NUM_COLUMNS == 0) {
                leftColumn.add(checkbox);
            } else if (i % NUM_COLUMNS == 1) {
                middleColumn.add(checkbox);
            } else {
                rightColumn.add(checkbox);
            }
        }

        checkboxPanel.add(leftColumn);
        checkboxPanel.add(middleColumn);
        checkboxPanel.add(rightColumn);

        FastJScrollPane scrollPane = new FastJScrollPane(checkboxPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        constraints.gridy = 1;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        panel.add(scrollPane, constraints);

        return panel;
    }
}
