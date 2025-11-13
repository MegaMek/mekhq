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

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;

public class BayRentalDialog extends ImmersiveDialogSimple {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FacilityRentals";

    private static final int DIALOG_CONFIRM_OPTION = 1;

    /**
     * Checks if the user confirmed the rental.
     *
     * @return {@code true} if the user chose to confirm the rental
     */
    public boolean wasConfirmed() {
        return this.getDialogChoice() == DIALOG_CONFIRM_OPTION;
    }

    public BayRentalDialog(Campaign campaign, Money rentalCost) {
        super(campaign,
              campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.LOGISTICS),
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
     * Provides the labeled buttons for the dialog (Cancel and Confirm).
     *
     * @return a list of button/tooltip pairs for dialog actions
     */
    private static List<String> getButtons() {
        return List.of(
              getTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.button.cancel"),
              getTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.button.confirm")
        );
    }

    private static String getOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "ContractStartRentalDialog.outOfCharacter.bay");
    }
}
