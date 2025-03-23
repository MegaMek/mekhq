/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.gui.dialog.nagDialogs;

import static mekhq.gui.dialog.nagDialogs.nagLogic.UntreatedPersonnelNagLogic.campaignHasUntreatedInjuries;

import java.util.List;

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

        final String DIALOG_BODY = "UntreatedPersonnelNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
        showDialog();
    }

    /**
     * Determines whether a nag dialog should be displayed for untreated injuries among personnel.
     *
     * <p>This method evaluates two conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for untreated personnel injuries in their options.</li>
     *     <li>There are untreated injuries among the campaign's personnel.</li>
     * </ul>
     *
     * @param activePersonnel A {@link List} of active personnel in the campaign.
     * @param doctorCapacity The maximum number of patients each doctor can medicate.
     *
     * @return {@code true} if the nag dialog should be displayed due to untreated injuries, {@code false} otherwise.
     */
    public static boolean checkNag(List<Person> activePersonnel, int doctorCapacity) {
        final String NAG_KEY = MHQConstants.NAG_UNTREATED_PERSONNEL;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && campaignHasUntreatedInjuries(activePersonnel, doctorCapacity);
    }
}
