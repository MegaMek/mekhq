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
import mekhq.gui.baseComponents.AbstractMHQNagDialog_NEW;

public class PrisonersNagDialog extends AbstractMHQNagDialog_NEW {
    static boolean hasPrisoners (Campaign campaign) {
        if (!campaign.hasActiveContract()) {
            return !campaign.getCurrentPrisoners().isEmpty();
        }

        return false;
    }

    public PrisonersNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_PRISONERS);

        final String DIALOG_BODY = "PrisonersNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
    }

    public void checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_PRISONERS;

        if (campaign.getCampaignOptions().isUseAtB()
            && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && (hasPrisoners(campaign))) {
            showDialog();
        }
    }
}
