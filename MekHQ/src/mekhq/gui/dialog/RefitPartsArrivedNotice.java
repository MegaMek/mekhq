/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Tells the player that everything ordered ahead for a unit's refit has arrived (issue #7848). The refit does not
 * begin by itself, so without this the parts would sit in the warehouse with no sign that the refit can start. For
 * example: "Captain, everything we ordered for the Uziel UZL-2S refit to the UZL-3S has arrived."
 */
public final class RefitPartsArrivedNotice {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    private RefitPartsArrivedNotice() {}

    /**
     * Has the senior administrator say the refit's parts are here and how to begin the refit.
     *
     * @param campaign the campaign
     * @param unit     the unit whose parts ordered ahead have all arrived
     */
    public static void show(Campaign campaign, Unit unit) {
        String model = unit.getPendingRefitModel();
        String inCharacter = getFormattedTextAt(RESOURCE_BUNDLE, "RefitPartsArrivedNotice.inCharacter",
              campaign.getCommanderAddress(), unit.getName(), model);
        String outOfCharacter = getFormattedTextAt(RESOURCE_BUNDLE, "RefitPartsArrivedNotice.outOfCharacter",
              campaign.getCommanderAddress(), unit.getName(), model);
        Person speaker = campaign.getPlayerForce()
                               .getHumanResources()
                               .getSeniorAdminPerson(campaign.getCampaignOptions(),
                                     campaign.getPlayerForce().isClanForce(), campaign.getLocalDate());
        new ImmersiveDialogSimple(campaign, speaker, null, inCharacter, null, outOfCharacter, null, false);
    }
}
