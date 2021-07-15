/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import mekhq.MekHQ;
import mekhq.MekHqConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;

public class InsufficientAstechTimeNagDialog extends AbstractMHQNagDialog {
    //region Constructors
    public InsufficientAstechTimeNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "InsufficientAstechTimeNagDialog", "InsufficientAstechTimeNagDialog.title",
                "", campaign, MekHqConstants.NAG_INSUFFICIENT_ASTECH_TIME);
    }
    //endregion Constructors

    @Override
    protected boolean checkNag(Campaign campaign) {
        if (MekHQ.getMekHQOptions().getNagDialogIgnore(getKey())
                || !campaign.getCampaignOptions().checkMaintenance()) {
            return false;
        }

        // Units are only valid if the are maintained, present, and not self crewed (as the crew
        // maintain it in that case). For each unit this is valid for, we need six astechs to assist
        // the tech for the maintenance.
        final int need = campaign.getHangar().getUnitsStream()
                .filter(unit -> !unit.isUnmaintained() && unit.isPresent() && !unit.isSelfCrewed())
                .mapToInt(unit -> unit.getMaintenanceTime() * 6).sum();

        int available = campaign.getPossibleAstechPoolMinutes();
        if (campaign.isOvertimeAllowed()) {
            available += campaign.getPossibleAstechPoolOvertime();
        }

        if (available < need) {
            final int astechsNeeded = (int) Math.ceil((need - available) / (double) Person.PRIMARY_ROLE_SUPPORT_TIME);
            setDescription(String.format(resources.getString("InsufficientAstechTimeNagDialog.text"), astechsNeeded));
            return true;
        } else {
            return false;
        }
    }
}
