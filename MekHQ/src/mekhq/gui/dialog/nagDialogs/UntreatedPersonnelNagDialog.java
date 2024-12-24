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
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

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
    /**
     * Checks whether the campaign has any untreated personnel with injuries.
     *
     * <p>
     * This method iterates over the campaign's active personnel and identifies individuals
     * who meet the following criteria:
     * <ul>
     *     <li>The individual requires treatment ({@link Person#needsFixing()}).</li>
     *     <li>The individual has not been assigned to a doctor.</li>
     *     <li>The individual is not currently classified as a prisoner.</li>
     * </ul>
     * If any personnel match these conditions, the method returns {@code true}.
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     * @return {@code true} if untreated injuries are present, otherwise {@code false}.
     */
    static boolean campaignHasUntreatedInjuries(Campaign campaign) {
        for (Person person : campaign.getActivePersonnel()) {
            if (!person.getPrisonerStatus().isCurrentPrisoner()
                && person.needsFixing()
                && person.getDoctorId() == null) {
                return true;
            }
        }
        return false;
    }

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
    public UntreatedPersonnelNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_UNTREATED_PERSONNEL);

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
     *     {@link #campaignHasUntreatedInjuries(Campaign)}.</li>
     * </ul>
     * If these conditions are met, the dialog is displayed to remind the user to address untreated injuries.
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public void checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_UNTREATED_PERSONNEL;

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && campaignHasUntreatedInjuries(campaign)) {
            showDialog();
        }
    }
}
