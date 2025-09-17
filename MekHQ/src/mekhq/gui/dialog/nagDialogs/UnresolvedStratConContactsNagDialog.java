/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.nagDialogs;

import static mekhq.MHQConstants.NAG_UNRESOLVED_STRATCON_CONTACTS;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UnresolvedStratConContactsNagLogic.determineUnresolvedContacts;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UnresolvedStratConContactsNagLogic.hasUnresolvedContacts;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

public class UnresolvedStratConContactsNagDialog extends ImmersiveDialogNag {
    public UnresolvedStratConContactsNagDialog(final Campaign campaign) {
        super(campaign, COMMAND, NAG_UNRESOLVED_STRATCON_CONTACTS, "UnresolvedStratConContactsNagDialog");
    }

    @Override
    protected String getInCharacterMessage(Campaign campaign, String key, String commanderAddress) {
        final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

        String unresolvedContactsReport = determineUnresolvedContacts(campaign.getActiveAtBContracts(),
              campaign.getLocalDate());

        return getFormattedTextAt(RESOURCE_BUNDLE, key + ".ic", commanderAddress, unresolvedContactsReport);
    }

    /**
     * Determines whether a nag dialog should be displayed for unresolved StratCon contacts in the campaign.
     *
     * <p>This method checks multiple conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>StratCon functionality is enabled in the campaign options.</li>
     *     <li>The user has not ignored the nag dialog for unresolved StratCon contacts in their preferences.</li>
     *     <li>The campaign has unresolved StratCon contacts, as determined by {@code #hasUnresolvedContacts}.</li>
     * </ul>
     *
     * @param isUseStratCon   A flag indicating whether StratCon functionality is enabled in the campaign options.
     * @param activeContracts A list of active {@link AtBContract} objects to evaluate for unresolved StratCon
     *                        contacts.
     * @param today           The current campaign date, used to filter unresolved scenarios.
     *
     * @return {@code true} if all conditions are met and the nag dialog should be displayed; {@code false} otherwise.
     */
    public static boolean checkNag(boolean isUseStratCon, List<AtBContract> activeContracts, LocalDate today) {

        return isUseStratCon &&
                     !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_UNRESOLVED_STRATCON_CONTACTS) &&
                     hasUnresolvedContacts(activeContracts, today);
    }
}
