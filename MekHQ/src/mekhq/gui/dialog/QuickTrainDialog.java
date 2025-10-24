/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;

/**
 * Dialog for performing Quick Training.
 *
 * <p>Presents options for training selected personnel in bulk, allowing the user to choose a training mode and
 * select a target skill level. Handles dialog construction, user interaction, and supplemental message/panel
 * display.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class QuickTrainDialog extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.QuickTrainDialog";

    /**
     * Returns {@code true} if the user has chosen to cancel the dialog.
     *
     * @return {@code true} if cancel was selected; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean isCancel() {
        final int cancelIndex = 0;
        return getDialogChoice() == cancelIndex;
    }

    /**
     * Returns {@code true} if the user has chosen the continuous training option, which instructs the system to keep
     * training selected personnel until stopping criteria are met.
     *
     * @return {@code true} if continuous training was selected; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean isContinuousTraining() {
        final int continuousTrainingIndex = 2;
        return getDialogChoice() == continuousTrainingIndex;
    }

    /**
     * Constructs a {@link QuickTrainDialog} with appropriate options for the current campaign and selection state.
     *
     * @param campaign         the current campaign context
     * @param isNobodySelected {@code true} if no personnel are selected; determines dialog content and available
     *                         buttons
     *
     * @author Illiani
     * @since 0.50.10
     */
    public QuickTrainDialog(Campaign campaign, boolean isNobodySelected) {
        super(campaign,
              campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.HR),
              null,
              getCenterMessage(campaign.getCommanderAddress(), isNobodySelected),
              getButtons(isNobodySelected),
              getOutOfCharacterMessage(),
              ImmersiveDialogWidth.SMALL.getWidth(),
              true,
              getSupplementalPanel(),
              null,
              true);
    }

    /**
     * Creates and returns the main information message to be displayed in the center of the dialog.
     *
     * @param commanderAddress the name or address of the campaign commander
     * @param isNobodySelected whether no personnel are currently selected
     *
     * @return a formatted center message for display
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getCenterMessage(String commanderAddress, boolean isNobodySelected) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "QuickTrainDialog.centerMessage."
                                                         + (isNobodySelected ? "empty" : "normal"),
              commanderAddress);
    }

    /**
     * Returns the button options for the dialog based on the selection state.
     *
     * @param isNobodySelected {@code true} if no personnel are selected
     *
     * @return a list of button-label/tooltip pairs for the dialog
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static List<ButtonLabelTooltipPair> getButtons(boolean isNobodySelected) {
        if (isNobodySelected) {
            return new ArrayList<>(List.of(
                  new ButtonLabelTooltipPair(getTextAt(RESOURCE_BUNDLE, "QuickTrainDialog.button.whoops"), null)
            ));
        }

        return new ArrayList<>(List.of(
              new ButtonLabelTooltipPair(getTextAt(RESOURCE_BUNDLE, "QuickTrainDialog.button.cancel"), null),
              new ButtonLabelTooltipPair(getTextAt(RESOURCE_BUNDLE, "QuickTrainDialog.button.single"), null),
              new ButtonLabelTooltipPair(getTextAt(RESOURCE_BUNDLE, "QuickTrainDialog.button.continuous"), null)
        ));
    }

    /**
     * Returns the out-of-character (OOC) context message for the dialog.
     *
     * @return the OOC message string from the resource bundle
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getOutOfCharacterMessage() {
        return getTextAt(RESOURCE_BUNDLE, "QuickTrainDialog.outOfCharacterMessage");
    }


    /**
     * Constructs and returns a supplemental panel containing user-selectable milestone attributes.
     *
     * @return a {@link JPanel} with UI controls for additional input
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static JPanel getSupplementalPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = createBaseConstraints();

        JLabel lblTargetMilestone = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "QuickTrainDialog.spinner"));
        addComponent(panel, lblTargetMilestone, constraints, 0, GridBagConstraints.NONE);

        JSpinner spnAttributes = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1));
        addComponent(panel, spnAttributes, constraints, 1, GridBagConstraints.HORIZONTAL);

        return panel;
    }

    /**
     * Creates and returns the base {@link GridBagConstraints} for the supplemental panel.
     *
     * @return the default GridBagConstraints instance
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static GridBagConstraints createBaseConstraints() {
        int padding = scaleForGUI(5);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(padding, padding, padding, padding);
        constraints.anchor = GridBagConstraints.WEST;
        return constraints;
    }

    /**
     * Adds a component to a panel with the given grid x position and fill constraints.
     *
     * @param panel       the panel to add a component to
     * @param component   the component to add
     * @param constraints base constraints to use (modified in-place)
     * @param gridX       the column (x) position in the grid
     * @param fill        the fill mode from {@link GridBagConstraints}
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void addComponent(JPanel panel, JComponent component, GridBagConstraints constraints, int gridX,
          int fill) {
        constraints.gridx = gridX;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = fill;
        panel.add(component, constraints);
    }
}
