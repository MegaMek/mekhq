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
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Money;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;
import java.util.Objects;

/**
 * This class represents a nag dialog displayed when the campaign can't afford its next jump
 * It extends the {@link AbstractMHQNagDialog} class.
 */
public class UnableToAffordJumpNagDialog extends AbstractMHQNagDialog {
    private static String DIALOG_NAME = "UnableToAffordJumpNagDialog";
    private static String DIALOG_TITLE = "UnableToAffordJumpNagDialog.title";
    private static String DIALOG_BODY = "UnableToAffordJumpNagDialog.text";

    /**
     * Checks if the campaign is unable to afford the cost of the next jump.
     *
     * @param campaign The campaign for which to check the affordability of the next jump.
     * @return {@code true} if the campaign is unable to afford the cost of the next jump, {@code false} otherwise.
     */
    static boolean isUnableToAffordNextJump (Campaign campaign) {
        Money currentFunds = campaign.getFunds();
        Money nextJumpCost = getNextJumpCost(campaign);

        if (nextJumpCost.isZero()) {
            return false;
        } else {
            return currentFunds.isLessThan(nextJumpCost);
        }
    }

    /**
     * Calculates the cost of the next jump for a given campaign.
     * <p>
     * This method determines the cost of the next jump by using the campaign's
     * options and calculates the cost per jump based on whether the contract pays
     * based on the value of the units in the campaign's TO&E.
     *
     * @param campaign the campaign for which to calculate the next jump cost
     * @return the cost of the next jump for the campaign
     */
    static Money getNextJumpCost(Campaign campaign) {
        CurrentLocation location = campaign.getLocation();
        JumpPath jumpPath = location.getJumpPath();

        if (jumpPath == null) {
            return Money.zero();
        }

        if (Objects.equals(jumpPath.getLastSystem(), location.getCurrentSystem())) {
            return Money.zero();
        }

        boolean isContractPayBasedOnToeUnitsValue = campaign.getCampaignOptions().isEquipmentContractBase();

        return campaign.calculateCostPerJump(true, isContractPayBasedOnToeUnitsValue);
    }

    //region Constructors
    /**
     * Creates a new instance of the {@link UnableToAffordJumpNagDialog} class.
     *
     * @param frame the parent JFrame for the dialog
     * @param campaign the {@link Campaign} associated with the dialog
     */
    public UnableToAffordJumpNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, DIALOG_NAME, DIALOG_TITLE, DIALOG_BODY, campaign, MHQConstants.NAG_UNABLE_TO_AFFORD_JUMP);
    }
    //endregion Constructors

    /**
     * Checks if the campaign is able to afford its next jump.
     * If the campaign is unable to afford its next jump and the Nag dialog for the current key is
     * not ignored, it sets the description using the specified format and returns {@code true}.
     * Otherwise, it returns {@code false}.
     */
    @Override
    protected boolean checkNag() {
        if (!MekHQ.getMHQOptions().getNagDialogIgnore(getKey())
                && isUnableToAffordNextJump(getCampaign())) {
            setDescription(String.format(
                    resources.getString(DIALOG_BODY),
                    getNextJumpCost(getCampaign()).toAmountAndSymbolString()));
            return true;
        }

        return false;
    }
}
