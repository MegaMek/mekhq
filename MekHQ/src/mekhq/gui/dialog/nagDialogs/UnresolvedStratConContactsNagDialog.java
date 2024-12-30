/*
 * Copyright (c) 2021-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog.nagDialogs;

import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import static mekhq.gui.dialog.nagDialogs.nagLogic.UnresolvedStratConContactsNagLogic.determineUnresolvedContacts;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UnresolvedStratConContactsNagLogic.hasUnresolvedContacts;

/**
 * A nag dialog that warns the user about unresolved StratCon contacts within the campaign.
 *
 * <p>
 * This dialog identifies unresolved scenarios in StratCon tracks attached to active contracts
 * where the player can deploy forces. It provides a detailed report of unresolved contacts to
 * notify the player of critical actions required before advancing the campaign.
 * </p>
 */
public class UnresolvedStratConContactsNagDialog extends AbstractMHQNagDialog {
    /**
     * Constructs the nag dialog for unresolved StratCon contacts.
     *
     * <p>
     * The dialog is initialized with information about unresolved StratCon scenarios and the
     * campaign's current state. The dynamic message is formatted to include the name or title
     * of the commander, providing context for the player.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public UnresolvedStratConContactsNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_UNRESOLVED_STRATCON_CONTACTS);

        String unresolvedContactsReport = determineUnresolvedContacts(campaign);

        String addendum = "";
        if (unresolvedContactsReport.isEmpty()) {
            addendum = resources.getString("UnresolvedStratConContactsNagDialog.stratcon");
        }

        final String DIALOG_BODY = "UnresolvedStratConContactsNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false), addendum));
        showDialog();
    }

    /**
     * Checks if a nag dialog should be displayed for unresolved StratCon contacts in the given campaign.
     *
     * <p>The method evaluates the following conditions to determine if the nag dialog should appear:</p>
     * <ul>
     *     <li>If StratCon is enabled in the campaign options.</li>
     *     <li>If the nag dialog for unresolved StratCon contacts has not been ignored in the user options.</li>
     *     <li>If the campaign has unresolved StratCon contacts.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} to check for nagging conditions
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise
     */
    public static boolean checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_UNRESOLVED_STRATCON_CONTACTS;

        return campaign.getCampaignOptions().isUseStratCon()
            && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && hasUnresolvedContacts(campaign);
    }
}
