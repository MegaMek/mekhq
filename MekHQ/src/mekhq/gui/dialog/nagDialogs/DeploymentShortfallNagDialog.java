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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog.nagDialogs;

import static mekhq.MHQConstants.NAG_SHORT_DEPLOYMENT;
import static mekhq.gui.dialog.nagDialogs.nagLogic.DeploymentShortfallNagLogic.hasDeploymentShortfall;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * A dialog class used to notify players about deployment shortfalls in their campaign.
 *
 * <p>The {@code DeploymentShortfallNagDialog} extends {@link ImmersiveDialogNag} and provides
 * a specialized nag dialog designed to alert players when deployment shortfalls occur. It uses predefined parameters,
 * such as the {@code NAG_SHORT_DEPLOYMENT} constant, and no specific specialization for its speaker, allowing for a
 * default fallback during dialog creation.</p>
 */
public class DeploymentShortfallNagDialog extends ImmersiveDialogNag {
    /**
     * Constructs a new {@code DeploymentShortfallNagDialog} instance to display the deployment shortfall nag dialog.
     *
     * <p>This constructor initializes the nag dialog without a specific specialization, allowing the fallback
     * mechanism to determine the appropriate speaker. The {@code NAG_SHORT_DEPLOYMENT} constant is used to manage
     * dialog suppression, and the {@code "DeploymentShortfallNagDialog"} message key is utilized to retrieve localized
     * content.</p>
     *
     * @param campaign The {@link Campaign} instance associated with this dialog. Provides access to campaign data and
     *                 settings required for dialog construction.
     */
    public DeploymentShortfallNagDialog(final Campaign campaign) {
        super(campaign, null, NAG_SHORT_DEPLOYMENT, "DeploymentShortfallNagDialog");
    }

    /**
     * Determines whether a nag dialog should be displayed for deployment shortfalls in the specified campaign.
     *
     * <p>This method evaluates several conditions to decide if the nag dialog should be shown:</p>
     * <ul>
     *     <li>The campaign uses AtB (Against the Bot) rules.</li>
     *     <li>The user has not ignored the nag dialog for deployment shortfalls in their options.</li>
     *     <li>The campaign has a deployment shortfall, as determined by {@code #hasDeploymentShortfall}.</li>
     * </ul>
     *
     * @param isUseAtB A flag indicating whether the campaign is using AtB rules.
     * @param campaign The {@link Campaign} object used to check for deployment shortfalls.
     *
     * @return {@code true} if the nag dialog should be displayed due to deployment shortfalls; {@code false} otherwise.
     */
    public static boolean checkNag(boolean isUseAtB, Campaign campaign) {
        final String NAG_KEY = NAG_SHORT_DEPLOYMENT;

        return isUseAtB && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY) && hasDeploymentShortfall(campaign);
    }
}
