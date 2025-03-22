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
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import java.util.List;

import static mekhq.gui.dialog.nagDialogs.nagLogic.PregnantCombatantNagLogic.hasActivePregnantCombatant;

/**
 * A nag dialog that warns the user if there are pregnant personnel actively assigned to combat forces.
 *
 * <p>
 * This dialog checks the current campaign for any personnel who are actively pregnant and
 * assigned to a combat unit that is part of a force. If such personnel are detected, and the
 * dialog is not ignored in MekHQ options, the dialog is displayed to notify the user.
 * </p>
 */
public class PregnantCombatantNagDialog extends AbstractMHQNagDialog {
    /**
     * Constructs the pregnant combatant nag dialog for the given campaign.
     *
     * <p>
     * This constructor sets up the dialog to display a warning related to
     * pregnant personnel actively assigned to combat forces. It also sets
     * the appropriate resource message to be displayed as the dialog body.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public PregnantCombatantNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_PREGNANT_COMBATANT);

        final String DIALOG_BODY = "PregnantCombatantNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
        showDialog();
    }

    /**
     * Determines whether a nag dialog should be displayed for active pregnant combatants in the given campaign.
     *
     * <p>This method evaluates two main conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for active pregnant combatants in their options.</li>
     *     <li>There are active pregnant combatants in the campaign, as determined by
     *         {@code #hasActivePregnantCombatant}.</li>
     * </ul>
     *
     * @param hasActiveContract A flag indicating whether the campaign currently has an active contract.
     * @param activePersonnel A list of {@link Person} objects representing the active personnel in the campaign.
     *
     * @return {@code true} if the nag dialog should be displayed; {@code false} otherwise.
     */
    public static boolean checkNag(boolean hasActiveContract, List<Person> activePersonnel) {
        final String NAG_KEY = MHQConstants.NAG_PREGNANT_COMBATANT;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && (hasActivePregnantCombatant(hasActiveContract, activePersonnel));
    }
}
