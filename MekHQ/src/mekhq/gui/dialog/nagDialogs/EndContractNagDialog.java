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

import static mekhq.MHQConstants.NAG_CONTRACT_ENDED;
import static mekhq.gui.dialog.nagDialogs.nagLogic.EndContractNagLogic.isContractEnded;

import java.time.LocalDate;
import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * A dialog class used to notify players when a contract has ended in their campaign.
 *
 * <p>The {@code EndContractNagDialog} extends {@link ImmersiveDialogNag} and provides
 * a specialized nag dialog to alert players about the conclusion of a contract. It leverages predefined parameters,
 * such as the {@code NAG_CONTRACT_ENDED} constant, and uses no specific specialization, relying on a fallback
 * speaker.</p>
 */
public class EndContractNagDialog extends ImmersiveDialogNag {

    /**
     * Constructs a new {@code EndContractNagDialog} instance to display the end-of-contract nag dialog.
     *
     * <p>This constructor initializes the nag dialog without a specific specialization, enabling
     * the fallback mechanism to determine the appropriate speaker. The {@code NAG_CONTRACT_ENDED} constant is used to
     * manage dialog suppression, and the {@code "EndContractNagDialog"} message key is utilized to fetch localized
     * message content.</p>
     *
     * @param campaign The {@link Campaign} instance associated with this dialog. Provides access to campaign data and
     *                 settings required for dialog construction.
     */
    public EndContractNagDialog(final Campaign campaign) {
        super(campaign, null, NAG_CONTRACT_ENDED, "EndContractNagDialog");
    }

    /**
     * Determines whether a nag dialog should be displayed for an ended contract in the given campaign.
     *
     * <p>This method evaluates the following conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for ended contracts in their options.</li>
     *     <li>A contract in the campaign has ended, as determined by {@code #isContractEnded}.</li>
     * </ul>
     *
     * @param today           The current local date used to check against the contracts' ending dates.
     * @param activeContracts A list of {@link AtBContract} objects representing the campaign's active contracts.
     *
     * @return {@code true} if the nag dialog should be displayed; {@code false} otherwise.
     */
    public static boolean checkNag(LocalDate today, List<AtBContract> activeContracts) {

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_CONTRACT_ENDED) && isContractEnded(today, activeContracts);
    }
}
