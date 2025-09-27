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

import static mekhq.MHQConstants.NAG_INVALID_FACTION;
import static mekhq.gui.dialog.nagDialogs.nagLogic.InvalidFactionNagLogic.isFactionInvalid;

import java.time.LocalDate;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * A dialog class used to notify players about an invalid faction in their campaign.
 *
 * <p>The {@code InvalidFactionNagDialog} extends {@link ImmersiveDialogNag} and provides a specialized dialog
 * designed to alert players when an invalid or unexpected faction is encountered during campaign operations. It uses
 * predefined values, including the {@code NAG_INVALID_FACTION} constant, and does not include a specific speaker
 * specialization, relying on a default fallback mechanism instead.</p>
 */
public class InvalidFactionNagDialog extends ImmersiveDialogNag {

    /**
     * Constructs a new {@code InvalidFactionNagDialog} instance to display the invalid faction nag dialog.
     *
     * <p>This constructor initializes the dialog with preconfigured parameters, such as the
     * {@code NAG_INVALID_FACTION} constant for managing dialog suppression and the {@code "InvalidFactionNagDialog"}
     * message key for retrieving localized dialog content. No specific speaker is provided, triggering fallback logic
     * to determine a suitable speaker for the dialog.</p>
     *
     * @param campaign The {@link Campaign} instance associated with this dialog. Provides access to campaign data and
     *                 settings required for constructing the dialog.
     */
    public InvalidFactionNagDialog(final Campaign campaign) {
        super(campaign, null, NAG_INVALID_FACTION, "InvalidFactionNagDialog");
    }

    /**
     * Determines whether a nag dialog should be displayed for an invalid faction in the campaign.
     *
     * <p>This method checks two conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for invalid factions in their options.</li>
     *     <li>The faction associated with the campaign is invalid for the specified date.</li>
     * </ul>
     *
     * @param campaignFaction The {@link Faction} associated with the campaign to be checked.
     * @param today           The {@link LocalDate} representing the current in-game date.
     *
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise.
     */
    public static boolean checkNag(Faction campaignFaction, LocalDate today) {

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_INVALID_FACTION) &&
                     (isFactionInvalid(campaignFaction, today));
    }
}
