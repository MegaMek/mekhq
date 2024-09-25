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
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;
/**
 * This class represents a nag dialog displayed when a campaign's Astech time deficit is greater than 0.
 * It extends the {@link AbstractMHQNagDialog} class.
 */
public class InsufficientAstechTimeNagDialog extends AbstractMHQNagDialog {
    private static String DIALOG_NAME = "InsufficientAstechTimeNagDialog";
    private static String DIALOG_TITLE = "InsufficientAstechTimeNagDialog.title";
    private static String DIALOG_BODY = "InsufficientAstechTimeNagDialog.text";

    /**
     * Checks if the given campaign has a deficit in Astech time.
     *
     * @param campaign the {@link Campaign} to check for an Astech time deficit
     * @return {@code true} if the campaign has a deficit in Astech time, {@code false} otherwise
     */
    static boolean checkAstechTimeDeficit(Campaign campaign) {
        return getAstechTimeDeficit(campaign) > 0;
    }

    /**
     * Calculates the time deficit of Astechs needed for a given campaign.
     * <p>
     * The method calculates the Astech time deficit by determining the number of Astechs
     * required to support maintenance for all valid units in the hangar of the campaign.
     *
     * @param campaign the {@link Campaign} for which to calculate the Astech time deficit
     * @return the Astech time deficit, rounded up to the nearest whole number
     */
    static int getAstechTimeDeficit(Campaign campaign) {
        // Units are only valid if they are maintained, present, and not self crewed (as the crew
        // maintain it in that case).
        // For each unit, this is valid for; we need six astechs to help the tech for the maintenance.
        final int need = campaign.getHangar().getUnitsStream()
                .filter(unit -> !unit.isUnmaintained() && unit.isPresent() && !unit.isSelfCrewed())
                .mapToInt(unit -> unit.getMaintenanceTime() * 6).sum();

        int available = campaign.getPossibleAstechPoolMinutes();
        if (campaign.isOvertimeAllowed()) {
            available += campaign.getPossibleAstechPoolOvertime();
        }

        return (int) Math.ceil((need - available) / (double) Person.PRIMARY_ROLE_SUPPORT_TIME);
    }

    //region Constructors
    /**
     * Creates a new instance of the {@link InsufficientAstechTimeNagDialog} class.
     *
     * @param frame the parent JFrame for the dialog
     * @param campaign the {@link Campaign} associated with the dialog
     */
    public InsufficientAstechTimeNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, DIALOG_NAME, DIALOG_TITLE, DIALOG_BODY, campaign, MHQConstants.NAG_INSUFFICIENT_ASTECH_TIME);
    }
    //endregion Constructors

    /**
     * Checks if the Astech time deficit is greater than zero.
     * If the count is greater than zero and the Nag dialog for the current key is not ignored,
     * it sets the description using the specified format and returns {@code true}.
     * Otherwise, it returns {@code false}.
     */
    @Override
    protected boolean checkNag() {
        if (!MekHQ.getMHQOptions().getNagDialogIgnore(getKey())
                && checkAstechTimeDeficit(getCampaign())) {
            setDescription(String.format(
                    resources.getString(DIALOG_BODY),
                    getAstechTimeDeficit(getCampaign())));
            return true;
        }

        return false;
    }
}
