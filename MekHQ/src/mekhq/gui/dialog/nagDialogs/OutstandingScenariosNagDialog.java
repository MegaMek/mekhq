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

import static mekhq.MHQConstants.NAG_OUTSTANDING_SCENARIOS;
import static mekhq.gui.dialog.nagDialogs.nagLogic.OutstandingScenariosNagLogic.getOutstandingScenarios;
import static mekhq.gui.dialog.nagDialogs.nagLogic.OutstandingScenariosNagLogic.hasOutStandingScenarios;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * A dialog class used to notify players about outstanding scenarios in their campaign.
 *
 * <p>The {@code OutstandingScenariosNagDialog} extends {@link ImmersiveDialogNag} and provides a specialized
 * dialog designed to alert players when there are unresolved or pending scenarios in the campaign. It uses predefined
 * values, including the {@code NAG_OUTSTANDING_SCENARIOS} constant, and does not include a specific speaker, relying
 * instead on a default fallback mechanism.</p>
 */
public class OutstandingScenariosNagDialog extends ImmersiveDialogNag {

    /**
     * Constructs a new {@code OutstandingScenariosNagDialog} instance to display the outstanding scenarios nag dialog.
     *
     * <p>This constructor initializes the dialog with preconfigured parameters, such as the
     * {@code NAG_OUTSTANDING_SCENARIOS} constant for managing dialog suppression and the
     * {@code "OutstandingScenariosNagDialog"} message key for retrieving localized dialog content. No specific speaker
     * is provided, triggering fallback logic to determine a suitable speaker for the dialog.</p>
     *
     * @param campaign The {@link Campaign} instance associated with this dialog. Provides access to campaign data and
     *                 settings required for constructing the dialog.
     */
    public OutstandingScenariosNagDialog(final Campaign campaign) {
        super(campaign, null, NAG_OUTSTANDING_SCENARIOS, "OutstandingScenariosNagDialog");
    }

    @Override
    protected String getInCharacterMessage(Campaign campaign, String key, String commanderAddress) {
        final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

        String outstandingScenarios = getOutstandingScenarios(campaign);

        return getFormattedTextAt(RESOURCE_BUNDLE, key + ".ic", commanderAddress, outstandingScenarios);
    }

    /**
     * Checks if a nag dialog should be displayed for outstanding scenarios in the given campaign.
     *
     * <p>The method evaluates the following conditions to determine if the nag dialog should appear:</p>
     * <ul>
     *     <li>If the campaign is set to use AtB (Against the Bot) rules.</li>
     *     <li>If the nag dialog for outstanding scenarios has not been ignored in the user options.</li>
     *     <li>If there are outstanding scenarios in the campaign.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} to check for nagging conditions
     *
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise
     */
    public static boolean checkNag(Campaign campaign) {
        final String NAG_KEY = NAG_OUTSTANDING_SCENARIOS;

        return campaign.getCampaignOptions().isUseAtB() &&
                     !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY) &&
                     hasOutStandingScenarios(campaign);
    }
}
