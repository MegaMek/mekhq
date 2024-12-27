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
    private final Campaign campaign;

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

        this.campaign = campaign;

        final String DIALOG_BODY = "PregnantCombatantNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
    }

    /**
     * Determines whether the nag dialog should be displayed to the user, based on campaign state
     * and configuration.
     *
     * <p>
     * This method checks if MekHQ options allow the "pregnant combatant" nag dialog to be shown,
     * and if there are any pregnant personnel actively assigned to combat. If both conditions
     * are met, the dialog is displayed to notify the user.
     * </p>
     */
    public void checkNag() {
        final String NAG_KEY = MHQConstants.NAG_PREGNANT_COMBATANT;

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && (hasActivePregnantCombatant(campaign))) {
            showDialog();
        }
    }
}
