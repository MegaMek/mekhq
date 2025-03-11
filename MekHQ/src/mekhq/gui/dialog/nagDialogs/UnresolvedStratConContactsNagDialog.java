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
 */
package mekhq.gui.dialog.nagDialogs;

import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import java.time.LocalDate;
import java.util.List;

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

        String unresolvedContactsReport = determineUnresolvedContacts(campaign.getActiveAtBContracts(),
              campaign.getLocalDate());

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
     * Determines whether a nag dialog should be displayed for unresolved StratCon contacts in the campaign.
     *
     * <p>This method checks multiple conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>StratCon functionality is enabled in the campaign options.</li>
     *     <li>The user has not ignored the nag dialog for unresolved StratCon contacts in their preferences.</li>
     *     <li>The campaign has unresolved StratCon contacts, as determined by {@code #hasUnresolvedContacts}.</li>
     * </ul>
     *
     * @param isUseStratCon A flag indicating whether StratCon functionality is enabled in the campaign options.
     * @param activeContracts A list of active {@link AtBContract} objects to evaluate for unresolved StratCon contacts.
     * @param today The current campaign date, used to filter unresolved scenarios.
     *
     * @return {@code true} if all conditions are met and the nag dialog should be displayed; {@code false} otherwise.
     */
    public static boolean checkNag(boolean isUseStratCon, List<AtBContract> activeContracts, LocalDate today) {
        final String NAG_KEY = MHQConstants.NAG_UNRESOLVED_STRATCON_CONTACTS;

        return isUseStratCon
              && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
              && hasUnresolvedContacts(activeContracts, today);
    }
}
