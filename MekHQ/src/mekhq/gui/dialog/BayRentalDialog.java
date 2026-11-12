/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;

public class BayRentalDialog extends ImmersiveDialogSimple {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FacilityRentals";

    private static final int DIALOG_CONFIRM_OPTION = 1;
    private static final int DIALOG_FREE_GM_OPTION = 2;
    private static final int DIALOG_IGNORE_OPTION = 1;

    /**
     * Checks if the user confirmed the rental.
     *
     * @return {@code true} if the user chose to confirm the rental
     */
    public boolean wasConfirmed() {
        return this.getDialogChoice() == DIALOG_CONFIRM_OPTION;
    }

    /**
     * Checks if the user chose the GM option to change site without paying the rental fee.
     *
     * <p>This option is always offered, regardless of whether GM mode is enabled.</p>
     *
     * @return {@code true} if the user chose to waive the rental fee
     */
    public boolean wasFeeWaived() {
        return wasResponseSelected() && this.getDialogChoice() == DIALOG_FREE_GM_OPTION;
    }

    public BayRentalDialog(Campaign campaign, Money rentalCost) {
        super(campaign,
              campaign.getPlayerForce().getHumanResources()
                    .getSeniorAdminPerson(campaign.getCampaignOptions(),
                          campaign.getPlayerForce().isClanForce(),
                          campaign.getLocalDate()),
              null,
              getCenterMessage(campaign.getCommanderAddress(), rentalCost),
              getButtons(),
              getOutOfCharacterMessage(),
              null,
              false,
              ImmersiveDialogWidth.SMALL);
    }


    private static String getCenterMessage(String commanderAddress, Money rentalCost) {
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "ContractStartRentalDialog.inCharacter.bay",
              commanderAddress,
              rentalCost.toAmountString());
    }

    /**
     * Provides the labeled buttons for the dialog (Cancel, Confirm, and Free (GM)).
     *
     * @return a list of button/tooltip pairs for dialog actions
     */
    private static List<String> getButtons() {
        return List.of(
              getTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.button.cancel"),
              getTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.button.confirm"),
              getTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.button.freeGm")
        );
    }

    private static String getOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.outOfCharacter.bay");
    }

    /**
     * Informs the player that bay rentals are unavailable (because the force is not planetside, or is on a contract
     * that does not allow rentals) and lets them choose whether to proceed anyway.
     *
     * <p>The player is offered two options: <b>Confirm</b>, which acknowledges the notice and leaves the rental
     * blocked, and <b>Ignore (GM)</b>, which bypasses the restriction. The Ignore option is always shown, regardless
     * of whether GM mode is enabled. Closing the dialog from the window frame is treated as Confirm.</p>
     *
     * @param campaign the current campaign
     *
     * @return {@code true} if the player chose to ignore the restriction; {@code false} otherwise
     */
    public static boolean showNoFacilitiesAvailableDialog(Campaign campaign) {
        boolean isInSpace = !campaign.getPlayerForce().getForceDetachment().getCurrentLocation().isOnPlanet();

        String message;
        if (isInSpace) {
            message = getTextAt(RESOURCE_BUNDLE, "UnitBayRental.inSpace");
        } else {
            message = getTextAt(RESOURCE_BUNDLE, "UnitBayRental.wrongContractType");
        }

        List<String> buttons = List.of(
              getTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.button.confirm"),
              getTextAt(RESOURCE_BUNDLE, "UnitBayRental.button.ignoreGm")
        );

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign, null, message, buttons, null);
        return dialog.wasResponseSelected() && dialog.getDialogChoice() == DIALOG_IGNORE_OPTION;
    }
}
