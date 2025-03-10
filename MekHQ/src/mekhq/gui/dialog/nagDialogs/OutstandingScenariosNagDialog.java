/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
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
 */
package mekhq.gui.dialog.nagDialogs;

import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import static mekhq.gui.dialog.nagDialogs.nagLogic.OutstandingScenariosNagLogic.getOutstandingScenarios;
import static mekhq.gui.dialog.nagDialogs.nagLogic.OutstandingScenariosNagLogic.hasOutStandingScenarios;

/**
 * Represents a nag dialog for displaying the list of outstanding scenarios in a campaign.
 *
 * <p>
 * This dialog checks for active scenarios within the campaign, categorizes them by
 * their state (e.g., unresolved or requiring a track), and displays a list of these
 * scenarios to the user. Scenarios are considered "outstanding" if they are unresolved or
 * require attention on the current campaign date.
 * </p>
 *
 * <p>
 * The dialog includes logic to account for both AtB contracts and StratCon-enabled campaigns,
 * formatting the outstanding scenarios with additional details when appropriate.
 * </p>
 */
public class OutstandingScenariosNagDialog extends AbstractMHQNagDialog {
    /**
     * Constructs the OutstandingScenariosNagDialog for the given campaign.
     *
     * <p>
     * Upon initialization, this dialog prepares a formatted string of outstanding
     * scenarios (if any) and sets up the dialog UI for display.
     * </p>
     *
     * @param campaign The {@link Campaign} associated with this nag dialog.
     */
    public OutstandingScenariosNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_OUTSTANDING_SCENARIOS);

        final String DIALOG_BODY = "OutstandingScenariosNagDialog.text";

        String outstandingScenarios = getOutstandingScenarios(campaign);

        String addendum = "";
        if (campaign.getCampaignOptions().isUseStratCon()) {
            addendum = resources.getString("OutstandingScenariosNagDialog.stratCon");
        }

        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false), outstandingScenarios, addendum));
        showDialog();
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
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise
     */
    public static boolean checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_OUTSTANDING_SCENARIOS;

        return campaign.getCampaignOptions().isUseAtB()
            && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && hasOutStandingScenarios(campaign);
    }
}
