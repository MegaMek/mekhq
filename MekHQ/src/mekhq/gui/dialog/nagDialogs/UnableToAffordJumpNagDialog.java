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
import mekhq.gui.baseComponents.AbstractMHQNagDialog_NEW;

import java.util.Objects;

/**
 * A nag dialog that warns the user if the campaign's available funds are not enough to cover the
 * next jump cost.
 *
 * <p>
 * This dialog calculates the cost of the next jump based on the campaign's current location and
 * contract options. It alerts the user when the campaign's available funds are less than the
 * calculated jump cost, ensuring players are notified of financial constraints before moving
 * to another system.
 * </p>
 */
public class UnableToAffordJumpNagDialog extends AbstractMHQNagDialog_NEW {
    private Money nextJumpCost = Money.zero();

    /**
     * Calculates the cost of the next jump based on the campaign's location and financial settings.
     *
     * <p>
     * This method retrieves the {@link JumpPath} for the campaign's current location and only
     * calculates the jump cost if the next system on the path differs from the current system.
     * The actual jump cost is determined by the campaign's settings, particularly whether
     * contracts base their costs on the value of units in the player's TOE (Table of Equipment).
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    private void getNextJumpCost(Campaign campaign) {
        CurrentLocation location = campaign.getLocation();
        JumpPath jumpPath = location.getJumpPath();

        if (jumpPath == null) {
            return;
        }

        if (Objects.equals(jumpPath.getLastSystem(), location.getCurrentSystem())) {
            return;
        }

        boolean isContractPayBasedOnToeUnitsValue = campaign.getCampaignOptions().isEquipmentContractBase();

        nextJumpCost = campaign.calculateCostPerJump(true, isContractPayBasedOnToeUnitsValue);
    }

    /**
     * Constructs the nag dialog for insufficient funds to afford a jump.
     *
     * <p>
     * This constructor initializes the dialog with relevant campaign data. It formats the
     * message to display the name or title of the commander and the calculated cost of the
     * next jump, enabling the user to take appropriate financial action if necessary.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public UnableToAffordJumpNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_UNABLE_TO_AFFORD_JUMP);

        final String DIALOG_BODY = "UnableToAffordJumpNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false),
            nextJumpCost.toAmountAndSymbolString()));
    }

    /**
     * Determines whether the insufficient funds nag dialog for a jump should be displayed.
     *
     * <p>
     * This method calculates the cost of the next jump and alerts the user when:
     * <ul>
     *     <li>The nag dialog for jump costs is not ignored in MekHQ options.</li>
     *     <li>The campaign's available funds are less than the cost of the next jump.</li>
     * </ul>
     * If both conditions are satisfied, the dialog is displayed to notify the user.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public void checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_UNABLE_TO_AFFORD_JUMP;

        getNextJumpCost(campaign);

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && campaign.getFunds().isLessThan(nextJumpCost)) {
            showDialog();
        }
    }
}
