/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import static mekhq.gui.dialog.nagDialogs.nagLogic.AdminStrainNagLogic.hasAdminStrain;

/**
 * Represents a nag dialog that warns the user about administrative strain in a campaign.
 *
 * <p>This dialog is triggered when the campaign has a positive administrative strain. The purpose
 * of the dialog is to notify the user about the issue, allowing them to take any corrective action
 * as necessary.</p>
 */
public class AdminStrainNagDialog extends AbstractMHQNagDialog {
    /**
     * Constructs the administrative strain nag dialog for the given campaign.
     *
     * <p>This dialog displays a detailed message describing the administrative strain
     * issue in the campaign.</p>
     *
     * @param campaign The {@link Campaign} for which the administrative strain nag
     *                 dialog is to be displayed.
     */
    public AdminStrainNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_ADMIN_STRAIN);

        final String DIALOG_BODY = "AdminStrainNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
        showDialog();
    }


    /**
     * Determines if the administrative strain nag dialog should be displayed.
     *
     * <p>This method evaluates whether a warning about administrative strain should
     * be shown to the user based on the following conditions:</p>
     * <ul>
     *     <li>Turnover checks are enabled.</li>
     *     <li>Administrative strain checks are enabled.</li>
     *     <li>The nag dialog for administrative strain has not been ignored in the user options.</li>
     *     <li>The campaign's administrative strain level is above 0.</li>
     * </ul>
     *
     * @param isUseTurnover {@code true} if turnover-based checks are enabled, {@code false} otherwise.
     * @param isUseAdminStrain {@code true} if administrative strain checks are enabled, {@code false} otherwise.
     * @param adminStrainLevel The current level of administrative strain in the campaign.
     *
     * @return {@code true} if the administrative strain nag dialog should be displayed;
     *         {@code false} otherwise.
     */
    public static boolean checkNag(boolean isUseTurnover, boolean isUseAdminStrain, int adminStrainLevel) {
        final String NAG_KEY = MHQConstants.NAG_ADMIN_STRAIN;

        return isUseTurnover && isUseAdminStrain && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && hasAdminStrain(adminStrainLevel);
    }
}
