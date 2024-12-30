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
     * Checks if a nag dialog should be displayed for active pregnant combatants in the given campaign.
     *
     * <p>The method evaluates the following conditions to determine if the nag dialog should appear:</p>
     * <ul>
     *     <li>If the nag dialog for active pregnant combatants has not been ignored in the user options.</li>
     *     <li>If there are active pregnant combatants in the campaign.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} to check for nagging conditions
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise
     */
    public static boolean checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_PREGNANT_COMBATANT;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && (hasActivePregnantCombatant(campaign));
    }
}
