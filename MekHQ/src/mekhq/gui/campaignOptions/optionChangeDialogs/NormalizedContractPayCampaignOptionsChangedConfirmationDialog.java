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
package mekhq.gui.campaignOptions.optionChangeDialogs;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;

public class NormalizedContractPayCampaignOptionsChangedConfirmationDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.NormalizedContractPayCampaignOptionsChangedConfirmationDialog";

    public NormalizedContractPayCampaignOptionsChangedConfirmationDialog(Campaign campaign) {
        String message = getTextAt(RESOURCE_BUNDLE,
              "NormalizedContractPayCampaignOptionsChangedConfirmationDialog.message");
        String cancelButton = getTextAt(RESOURCE_BUNDLE,
              "NormalizedContractPayCampaignOptionsChangedConfirmationDialog.button.cancel");
        String confirmButton = getTextAt(RESOURCE_BUNDLE,
              "NormalizedContractPayCampaignOptionsChangedConfirmationDialog.button.confirm");

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              null,
              null,
              message,
              List.of(cancelButton, confirmButton),
              null,
              null,
              false,
              ImmersiveDialogWidth.LARGE);

        final int cancelOptionIndex = 0;

        if (dialog.getDialogChoice() == cancelOptionIndex) {
            campaign.getCampaignOptions().setUseDiminishingContractPay(false);
        }
    }
}
