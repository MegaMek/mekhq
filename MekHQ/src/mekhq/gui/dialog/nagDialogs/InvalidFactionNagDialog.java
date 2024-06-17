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
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;
import java.time.LocalDate;
import java.util.Objects;

public class InvalidFactionNagDialog extends AbstractMHQNagDialog {
    private static boolean isFactionInvalid (Campaign campaign) {
        Faction campaignFaction = campaign.getFaction();

        if (!campaign.getFaction().validIn(campaign.getLocalDate())) {
            return true;
        }

        // this is a special handler for FedSuns as they're the main culprit behind the issue of users having invalid factions.
        // FS and LA campaigns won't trigger the above conditional, because those factions aren't technically ended when the FedSuns forms
        // they just become dormant.
        if (Objects.equals(campaignFaction.getShortName(), "LA")) {
            // the dates picked are chosen as these are when mhq does the bulk of the faction ownership transfers
            return ((campaign.getLocalDate().isAfter(LocalDate.of(3040, 1, 18)))
                    && (campaign.getLocalDate().isBefore(LocalDate.of(3067, 4, 20))));
        }

        // this is another special handler for FedSuns as they're the main culprit behind the issue of users having invalid factions.
        if (Objects.equals(campaignFaction.getShortName(), "FS")) {
            return ((campaign.getLocalDate().isAfter(LocalDate.of(3040, 1, 18)))
                    && (campaign.getLocalDate().isBefore(LocalDate.of(3057, 9, 18))));
        }

        return false;
    }

    //region Constructors
    public InvalidFactionNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "InvalidFactionNagDialog", "InvalidFactionNagDialog.title",
                "InvalidFactionNagDialog.text", campaign, MHQConstants.NAG_INVALID_FACTION);
    }

    //endregion Constructors
    @Override
    protected boolean checkNag() {
        return !MekHQ.getMHQOptions().getNagDialogIgnore(getKey()) && (isFactionInvalid(getCampaign()));
    }
}
