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

import static mekhq.gui.dialog.nagDialogs.nagLogic.UntreatedPersonnelNagLogic.campaignHasUntreatedInjuries;

/**
 * A nag dialog that alerts the user about untreated injuries within the campaign's personnel.
 *
 * <p>
 * This dialog checks for active, injured personnel who have not been assigned to a doctor,
 * excluding those currently classified as prisoners. It provides a reminder to the player, ensuring
 * that injured personnel receive immediate treatment.
 * </p>
 */
public class UntreatedPersonnelNagDialog extends AbstractMHQNagDialog {
    private final Campaign campaign;

    /**
     * Constructs the nag dialog for untreated personnel injuries.
     *
     * <p>
     * This constructor initializes the dialog with relevant campaign details
     * and formats the displayed message to include context for the commander.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public UntreatedPersonnelNagDialog(Campaign campaign) {
        super(campaign, MHQConstants.NAG_UNTREATED_PERSONNEL);

        this.campaign = campaign;

        final String DIALOG_BODY = "UntreatedPersonnelNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
    }

    /**
     * Determines whether the untreated personnel nag dialog should be displayed.
     *
     * <p>
     * The dialog is triggered if:
     * <ul>
     *     <li>The nag dialog for untreated personnel is not ignored in MekHQ options.</li>
     *     <li>There are untreated injuries in the campaign, as determined by
     *     {@code campaignHasUntreatedInjuries()}.</li>
     * </ul>
     * If these conditions are met, the dialog is displayed to remind the user to address untreated injuries.
     */
    public void checkNag() {
        final String NAG_KEY = MHQConstants.NAG_UNTREATED_PERSONNEL;

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && campaignHasUntreatedInjuries(campaign)) {
            showDialog();
        }
    }
}
