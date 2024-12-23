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
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.AbstractMHQNagDialog_NEW;

public class PregnantCombatantNagDialog extends AbstractMHQNagDialog_NEW {
    static boolean hasActivePregnantCombatant(Campaign campaign) {
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

    public PregnantCombatantNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_PREGNANT_COMBATANT);

        final String DIALOG_BODY = "PregnantCombatantNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
    }

    public void checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_PREGNANT_COMBATANT;

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && (hasActivePregnantCombatant(campaign))) {
            showDialog();
        }
    }
}
