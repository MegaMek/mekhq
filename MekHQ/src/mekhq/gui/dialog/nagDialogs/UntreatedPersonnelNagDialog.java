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

import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;

public class UntreatedPersonnelNagDialog extends AbstractMHQNagDialog {
    static boolean isUntreatedInjury(Campaign campaign) {
        return campaign.getActivePersonnel().stream()
                .filter(p -> (p.needsFixing()) && (p.getDoctorId() == null))
                .anyMatch(p -> !p.getPrisonerStatus().isCurrentPrisoner());
    }

    //region Constructors
    public UntreatedPersonnelNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "UntreatedPersonnelNagDialog", "UntreatedPersonnelNagDialog.title",
                "UntreatedPersonnelNagDialog.text", campaign, MHQConstants.NAG_UNTREATED_PERSONNEL);
        pack();
    }
    //endregion Constructors

    @Override
    protected boolean checkNag() {
        return !MekHQ.getMHQOptions().getNagDialogIgnore(getKey())
                && isUntreatedInjury(getCampaign());
    }
}
