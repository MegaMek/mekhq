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
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
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

    private static JSpinner spnHospitals;
    private static JSpinner spnKitchens;
    private static JSpinner spnSecurity;
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
     * Gets the current hospital beds spinner value.
     *
     * @return the selected number of hospitals as an int
     */
    public static int getHospitalSpinnerValue() {
        return (int) spnHospitals.getValue();
    }

    /**
     * Gets the current kitchens spinner value.
     *
     * @return the selected number of kitchens as an int
     */
    public static int getKitchensSpinnerValue() {
        return (int) spnKitchens.getValue();
    }

    /**
     * Gets the current security (holding cells) spinner value.
     *
     * @return the selected number of security units as an int
     */
    public static int getSecuritySpinnerValue() {
        return (int) spnSecurity.getValue();
    }

    public ContractStartRentalDialog(Campaign campaign, Contract contract, int hospitalBedCost, int kitchenCost,
          int holdingCellCost) {
        super(campaign,
              campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.LOGISTICS),
              null,
              getCenterMessage(campaign.getCommanderAddress(), contract, campaign.getGameYear()),
              getButtons(),
              getOutOfCharacterMessage(hospitalBedCost, kitchenCost, holdingCellCost),
              ImmersiveDialogWidth.LARGE.getWidth(),
              false,
              getSupplementalPanel(hospitalBedCost, kitchenCost, holdingCellCost),
              null,
              true);
    }

    private static String getCenterMessage(String commanderAddress, Contract contract, int currentYear) {
        String employerName = contract.getEmployer();
        if (contract instanceof AtBContract atBContract) {
            Faction employerFaction = atBContract.getEmployerFaction();
            employerName = FactionStandingUtilities.getFactionName(employerFaction, currentYear);
        }

        return getFormattedTextAt(RESOURCE_BUNDLE,
              "ContractStartRentalDialog.inCharacter",
              commanderAddress,
              employerName);
    }

    /**
     * Provides the labeled buttons for the dialog (Cancel and Confirm).
     *
     * @return a list of button/tooltip pairs for dialog actions
     */
    private static List<ButtonLabelTooltipPair> getButtons() {
        return List.of(
              new ButtonLabelTooltipPair(getTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.button.cancel"), null),
              new ButtonLabelTooltipPair(getTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.button.confirm"), null)
        );
    }

    private static String getOutOfCharacterMessage(int hospitalBedCost, int kitchenCost, int holdingCellCost) {
        StringBuilder outOfCharacterMessage = new StringBuilder();
        outOfCharacterMessage.append(getTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.outOfCharacter.intro"));

        if (hospitalBedCost > 0) {
            outOfCharacterMessage.append(getFormattedTextAt(RESOURCE_BUNDLE,
                  "ContractStartRentalDialog.outOfCharacter.hospitals", hospitalBedCost));
        }

        if (kitchenCost > 0) {
            outOfCharacterMessage.append(getFormattedTextAt(RESOURCE_BUNDLE,
                  "ContractStartRentalDialog.outOfCharacter.kitchens", kitchenCost));
        }

        if (holdingCellCost > 0) {
            outOfCharacterMessage.append(getFormattedTextAt(RESOURCE_BUNDLE,
                  "ContractStartRentalDialog.outOfCharacter.security", holdingCellCost));
        }

        return outOfCharacterMessage.toString();
    }

    private static JPanel getSupplementalPanel(int hospitalBedCost, int kitchenCost, int holdingCellCost) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;

        JLabel lblHospitals = new JLabel(getTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.spinner.hospitals"));
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(lblHospitals, constraints);

        spnHospitals = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(spnHospitals, constraints);

        JLabel lblKitchens = new JLabel(getTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.spinner.kitchens"));
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(lblKitchens, constraints);

        spnKitchens = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(spnKitchens, constraints);

        JLabel lblSecurity = new JLabel(getTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.spinner.security"));
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(lblSecurity, constraints);

        spnSecurity = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(spnSecurity, constraints);

        lblRentalCost = new JLabel(getTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.label.total"));
        updateTotal(hospitalBedCost, kitchenCost, holdingCellCost);
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(lblRentalCost, constraints);

        // These need to be last to ensure that the various other components have substantiated before being accessed
        spnHospitals.addChangeListener(e -> updateTotal(hospitalBedCost, kitchenCost, holdingCellCost));
        spnKitchens.addChangeListener(e -> updateTotal(hospitalBedCost, kitchenCost, holdingCellCost));
        spnSecurity.addChangeListener(e -> updateTotal(hospitalBedCost, kitchenCost, holdingCellCost));

        return panel;
    }

    private static void updateTotal(long hospitalBedCost, long kitchenCost, long holdingCellCost) {
        int hospitalCount = (int) spnHospitals.getValue();
        long hospitalMoneyCost = hospitalBedCost * hospitalCount;

        int kitchenCount = (int) spnKitchens.getValue();
        long kitchenMoneyCost = kitchenCost * kitchenCount;

        int securityCount = (int) spnSecurity.getValue();
        long securityMoneyCost = holdingCellCost * securityCount;

        long totalCost = hospitalMoneyCost + kitchenMoneyCost + securityMoneyCost;

        lblRentalCost.setText(getFormattedTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.label.total",
              totalCost));
    }
}
