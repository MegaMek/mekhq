/*
 * Copyright (c) 2021 - 2024 The MegaMek Team. All Rights Reserved.
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

public class NoCommanderNagDialog extends AbstractMHQNagDialog {
    private static boolean isCommanderMissing (Campaign campaign) {
        return (campaign.getFlaggedCommander() == null);
    }

    public NoCommanderNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "NoCommanderNagDialog", "NoCommanderNagDialog.title",
                "NoCommanderNagDialog.text", campaign, MHQConstants.NAG_NO_COMMANDER);
    }

    @Override
    protected boolean checkNag() {
        return !MekHQ.getMHQOptions().getNagDialogIgnore(getKey())
                && isCommanderMissing(getCampaign());
    }
}
