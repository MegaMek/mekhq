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
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.rentals.ContractRentalType;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;

/**
 * Dialog for initiating a facility rental agreement with the player in the campaign.
 *
 * <p>Presents the cost, allows the player to input a quantity (if applicable), and offers confirm/cancel choices.</p>
 *
 * <p>Used for renting hospital beds, kitchens, holding cells, or similar contract-related facilities.</p>
 */
public class ContractStartRentalDialog extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FacilityRentals";

    private static final int DIALOG_CONFIRM_OPTION = 1;

    private static JLabel lblRentalCost;

    /**
     * Checks if the user confirmed the rental.
     *
     * @return {@code true} if the user chose to confirm the rental
     */
    public boolean wasConfirmed() {
        return this.getDialogChoice() == DIALOG_CONFIRM_OPTION;
    }


    /**
     * Constructs a new {@link ContractStartRentalDialog} for the specified campaign and rental type.
     *
     * @param campaign   the {@link Campaign} the rental belongs to
     * @param rentalType the {@link ContractRentalType} of facility being rented (label placeholder may differ)
     * @param rentalCost the cost per unit or total cost of the rental being considered
     */
    public ContractStartRentalDialog(Campaign campaign, ContractRentalType rentalType, Money rentalCost) {
        super(campaign,
              campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.LOGISTICS),
              null,
              getCenterMessage(campaign.getCommanderAddress()),
              getButtons(),
              getOutOfCharacterMessage(),
              ImmersiveDialogWidth.SMALL.getWidth(),
              false,
              getSupplementalPanel(),
              null,
              true);
    }

    /**
     * Gets the formatted message to display in the center of the dialog, usually addressed to the commander.
     *
     * @param commanderAddress the name/address/pronoun of the commander for context
     *
     * @return the message string to display
     */
    private static String getCenterMessage(String commanderAddress) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "PLACEHOLDER_MESSAGE", commanderAddress);
    }

    /**
     * Provides the labeled buttons for the dialog (Cancel and Confirm).
     *
     * @return a list of button/tooltip pairs for dialog actions
     */
    private static List<ButtonLabelTooltipPair> getButtons() {
        return List.of(
              new ButtonLabelTooltipPair(getTextAt(RESOURCE_BUNDLE, "CANCEL"), null),
              new ButtonLabelTooltipPair(getTextAt(RESOURCE_BUNDLE, "CONFIRM"), null)
        );
    }

    /**
     * Gets the out-of-character message (footer/help text) for instructional dialog context.
     *
     * @return out-of-character/informational text
     */
    private static String getOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "PLACEHOLDER_MESSAGE");
    }

    /**
     * Creates the supplemental panel for the dialog, which includes the spinner for rental count and displays the total
     * cost.
     *
     * @return a {@link JPanel} containing the extra controls for the dialog
     */
    private static JPanel getSupplementalPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;

        // Add label for ComboBox
        JLabel lblRentalCount = new JLabel(getTextAt(RESOURCE_BUNDLE, "PLACEHOLDER_MESSAGE"));
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(lblRentalCount, constraints);

        JSpinner spnRentalCount = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spnRentalCount.addChangeListener(e -> {
            int count = (Integer) spnRentalCount.getValue();
            lblRentalCost.setText(getFormattedTextAt(RESOURCE_BUNDLE, "PLACEHOLDER_MESSAGE"));
        });
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(lblRentalCount, constraints);

        lblRentalCost = new JLabel(getTextAt(RESOURCE_BUNDLE, "PLACEHOLDER_MESSAGE"));
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(lblRentalCount, constraints);

        return panel;
    }
}
