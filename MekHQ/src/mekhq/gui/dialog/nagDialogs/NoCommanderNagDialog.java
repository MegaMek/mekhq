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

import static mekhq.MHQConstants.NAG_NO_COMMANDER;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.gui.dialog.nagDialogs.nagLogic.NoCommanderNagLogic.hasNoCommander;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * A dialog class used to notify players that a commander is missing in their campaign.
 *
 * <p>The {@code NoCommanderNagDialog} extends {@link ImmersiveDialogNag} and provides a specialized dialog
 * designed to alert players when no commander is assigned or present in the campaign. It uses predefined values,
 * including the {@code HR} speaker and the {@code NAG_NO_COMMANDER} constant, to configure dialog settings and
 * content.</p>
 */
@Deprecated(since = "0.50.11", forRemoval = true)
public class NoCommanderNagDialog extends ImmersiveDialogNag {
    /**
     * Constructs a new {@code NoCommanderNagDialog} instance to display the no commander nag dialog.
     *
     * <p>This constructor initializes the dialog with preconfigured parameters, such as the
     * {@code NAG_NO_COMMANDER} constant for managing dialog suppression, the {@code "NoCommanderNagDialog"} message key
     * for localization, and the {@code HR} speaker to deliver the dialog message.</p>
     *
     * @param campaign The {@link Campaign} instance associated with this dialog. Provides access to campaign data and
     *                 settings required for constructing the dialog.
     */
    public NoCommanderNagDialog(final Campaign campaign) {
        super(campaign, HR, NAG_NO_COMMANDER, "NoCommanderNagDialog");
    }

    /**
     * Determines whether a nag dialog should be displayed for the absence of a commander.
     *
     * <p>This method checks two conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for the absence of a commander in their options.</li>
     *     <li>No flagged commander is assigned to the campaign.</li>
     * </ul>
     *
     * @param flaggedCommander The {@link Person} designated as the flagged commander, or {@code null} if no commander
     *                         is assigned.
     *
     * @return {@code true} if the nag dialog should be displayed due to the absence of a commander, {@code false}
     *       otherwise.
     */
    public static boolean checkNag(@Nullable Person flaggedCommander) {

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_NO_COMMANDER) && hasNoCommander(flaggedCommander);
    }
}
