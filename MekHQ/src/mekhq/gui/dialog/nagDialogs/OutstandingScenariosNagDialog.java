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
package mekhq.gui.dialog.nagDialogs;

import mekhq.MekHQ;
import mekhq.MekHqConstants;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;

public class OutstandingScenariosNagDialog extends AbstractMHQNagDialog {
    //region Constructors
    public OutstandingScenariosNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "OutstandingScenariosNagDialog", "OutstandingScenariosNagDialog.title",
                "OutstandingScenariosNagDialog.text", campaign,
                MekHqConstants.NAG_OUTSTANDING_SCENARIOS);
    }
    //endregion Constructors

    @Override
    protected boolean checkNag(final Campaign campaign) {
        // If this isn't ignored, check all active AtB contracts for current AtB scenarios whose
        // date is today
        return !MekHQ.getMekHQOptions().getNagDialogIgnore(getKey())
                && campaign.getActiveAtBContracts(true).stream()
                        .anyMatch(contract -> contract.getCurrentAtBScenarios().stream()
                                .anyMatch(scenario -> (scenario.getDate() != null)
                                        && scenario.getDate().isEqual(campaign.getLocalDate())));
    }
}
