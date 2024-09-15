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
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;
import java.time.LocalDate;
import java.util.Objects;

/**
 * This class represents a nag dialog displayed when a campaign's faction has become extinct.
 * It extends the {@link AbstractMHQNagDialog} class.
 */
public class InvalidFactionNagDialog extends AbstractMHQNagDialog {
    private static String DIALOG_NAME = "InvalidFactionNagDialog";
    private static String DIALOG_TITLE = "InvalidFactionNagDialog.title";
    private static String DIALOG_BODY = "InvalidFactionNagDialog.text";

    /**
     * Checks if the given campaign's faction is valid.
     *
     * @param campaign the campaign to check
     * @return {@code true} if the campaign's faction is invalid, {@code false} otherwise
     */
    static boolean isFactionInvalid(Campaign campaign) {
        Faction campaignFaction = campaign.getFaction();

        if (!campaign.getFaction().validIn(campaign.getLocalDate())) {
            return true;
        }

        // this is a special handler for FedSuns as they're the main culprit behind the issue of users having invalid factions.
        // FS and LA campaigns won't trigger the above conditional, because those factions aren't technically ended when the FedSuns forms
        // they just become dormant.
        if (Objects.equals(campaignFaction.getShortName(), "LA")) {
            return lyranAllianceSpecialHandler(campaign);
        }

        // this is another special handler for FedSuns as they're the main culprit behind the issue of users having invalid factions.
        if (Objects.equals(campaignFaction.getShortName(), "FS")) {
            return federatedSunsSpecialHandler(campaign);
        }

        return false;
    }

    /**
     * Checks if the given campaign falls within the inactive date range of the Federated Suns.
     *
     * @param campaign The current campaign.
     * @return Returns {@code true} if the campaign falls within the active date range, otherwise {@code false}.
     */
    static boolean federatedSunsSpecialHandler(Campaign campaign) {
        boolean isAfterActiveDate = campaign.getLocalDate().isAfter(LocalDate.of(3040, 1, 18));
        boolean isBeforeInactiveDate = campaign.getLocalDate().isBefore(LocalDate.of(3057, 9, 18));

        return isAfterActiveDate && isBeforeInactiveDate;
    }

    /**
     * Checks if the given campaign falls within the inactive date range of the Lyran Alliance.
     *
     * @param campaign The current campaign.
     * @return Returns {@code true} if the campaign falls within the active date range, otherwise {@code false}.
     */
    static boolean lyranAllianceSpecialHandler(Campaign campaign) {
        // the dates picked are chosen as these are when mhq does the bulk of the faction ownership transfers
        boolean isAfterActiveDate = campaign.getLocalDate().isAfter(LocalDate.of(3040, 1, 18));
        boolean isBeforeInactiveDate = campaign.getLocalDate().isBefore(LocalDate.of(3067, 4, 20));

        return isAfterActiveDate && isBeforeInactiveDate;
    }

    //region Constructors
    /**
     * Creates a new instance of the {@link InvalidFactionNagDialog} class.
     *
     * @param frame the parent JFrame for the dialog
     * @param campaign the {@link Campaign} associated with the dialog
     */
    public InvalidFactionNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, DIALOG_NAME, DIALOG_TITLE, DIALOG_BODY, campaign, MHQConstants.NAG_INVALID_FACTION);
    }
    //endregion Constructors

    /**
     * Checks if there is a nag message to display.
     *
     * @return {@code true} if there is a nag message to display, {@code false} otherwise
     */
    @Override
    protected boolean checkNag() {
        return !MekHQ.getMHQOptions().getNagDialogIgnore(getKey()) && (isFactionInvalid(getCampaign()));
    }
}
