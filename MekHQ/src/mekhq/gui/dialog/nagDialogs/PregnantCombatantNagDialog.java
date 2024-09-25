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
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;

/**
 * This class represents a nag dialog displayed when the campaign has an active mission and has one
 * or more pregnant personnel assigned to the TOE
 * It extends the {@link AbstractMHQNagDialog} class.
 */
public class PregnantCombatantNagDialog extends AbstractMHQNagDialog {
    private static String DIALOG_NAME = "PregnantCombatantNagDialog";
    private static String DIALOG_TITLE = "PregnantCombatantNagDialog.title";
    private static String DIALOG_BODY = "PregnantCombatantNagDialog.text";

    /**
     * Checks if there is a pregnant combatant in the provided campaign. Combatants are defined as
     * personnel assigned to a {@link Unit} in the TO&E during an active {@link Mission}
     *
     * @param campaign the campaign to check
     * @return {@code true} if there is a pregnant combatant, {@code false} otherwise
     */
    static boolean isPregnantCombatant(Campaign campaign) {
        if (campaign.getActiveMissions(false).isEmpty()) {
            return false;
        }

        // there is no reason to use a stream here, as there won't be enough iterations to warrant it
        for (Person person : campaign.getActivePersonnel()) {
            if (person.isPregnant()) {
                Unit unit = person.getUnit();

                if (unit != null) {
                    if (unit.getForceId() != Force.FORCE_NONE) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    //region Constructors
    /**
     * Creates a new instance of the {@link PregnantCombatantNagDialog} class.
     *
     * @param frame the parent JFrame for the dialog
     * @param campaign the {@link Campaign} associated with the dialog
     */
    public PregnantCombatantNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, DIALOG_NAME, DIALOG_TITLE, DIALOG_BODY, campaign, MHQConstants.NAG_PREGNANT_COMBATANT);
    }
    //endregion Constructors

    /**
     * Checks if there is a nag message to display.
     *
     * @return {@code true} if there is a nag message to display, {@code false} otherwise
     */
    @Override
    protected boolean checkNag() {
        return !MekHQ.getMHQOptions().getNagDialogIgnore(getKey())
                && isPregnantCombatant(getCampaign());
    }
}
