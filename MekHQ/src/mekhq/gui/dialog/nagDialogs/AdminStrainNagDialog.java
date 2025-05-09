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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog.nagDialogs;

import static mekhq.MHQConstants.NAG_ADMIN_STRAIN;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.gui.dialog.nagDialogs.nagLogic.AdminStrainNagLogic.hasAdminStrain;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * A dialog class used to notify players about administrative strain in their campaign.
 *
 * <p>The {@code AdminStrainNagDialog} extends {@link ImmersiveDialogNag} and provides a specialized
 * nag dialog specifically intended to alert players when administrative strain occurs. It utilizes predefined
 * parameters such as the "HR" specialization, the "NAG_ADMIN_STRAIN" constant, and a specific message key to display
 * relevant information to the player.</p>
 */
public class AdminStrainNagDialog extends ImmersiveDialogNag {

    /**
     * Constructs a new {@code AdminStrainNagDialog} instance to display the administrative strain nag dialog.
     *
     * <p>This constructor sets up the nag dialog using predefined parameters specific to administrative
     * strain scenarios. It passes the {@code HR} specialization to highlight administrators relevant to human resources
     * and uses the {@code NAG_ADMIN_STRAIN} constant for suppression control. The {@code "AdminStrainNagDialog"}
     * message key is used to retrieve localized message content.</p>
     *
     * @param campaign The {@link Campaign} instance associated with this dialog. Provides access to campaign data and
     *                 settings required for dialog construction.
     */
    public AdminStrainNagDialog(final Campaign campaign) {
        super(campaign, HR, NAG_ADMIN_STRAIN, "AdminStrainNagDialog");
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
     * @param isUseTurnover    {@code true} if turnover-based checks are enabled, {@code false} otherwise.
     * @param isUseAdminStrain {@code true} if administrative strain checks are enabled, {@code false} otherwise.
     * @param adminStrainLevel The current level of administrative strain in the campaign.
     *
     * @return {@code true} if the administrative strain nag dialog should be displayed; {@code false} otherwise.
     */
    public static boolean checkNag(boolean isUseTurnover, boolean isUseAdminStrain, int adminStrainLevel) {
        final String NAG_KEY = NAG_ADMIN_STRAIN;

        return isUseTurnover &&
                     isUseAdminStrain &&
                     !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY) &&
                     hasAdminStrain(adminStrainLevel);
    }
}
