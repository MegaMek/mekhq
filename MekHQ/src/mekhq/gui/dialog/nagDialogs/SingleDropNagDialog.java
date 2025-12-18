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

import static mekhq.MHQConstants.NAG_SINGLE_DROP_SET_UP;

import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;
import mekhq.gui.dialog.nagDialogs.nagLogic.SingleDropSetUpNagLogic;

/**
 * Nag dialog reminding the player to configure a single-drop setup when using
 * StratCon singles mode with at least one active StratCon contract.
 * <p>
 * This dialog is typically shown at campaign time progression (e.g. weekly on
 * Sundays) when {@link #checkNag(List, boolean, boolean)} evaluates to
 * {@code true} and the corresponding
 * {@link mekhq.MHQConstants#NAG_SINGLE_DROP_SET_UP} nag option has not been
 * disabled in {@link MekHQ#getMHQOptions()}.
 * </p>
 * <p>
 * It extends {@link ImmersiveDialogNag} to provide a themed, immersive warning
 * that the current campaign configuration may not match the expected single-drop
 * StratCon setup.
 * </p>
 */
public class SingleDropNagDialog extends ImmersiveDialogNag {
    public SingleDropNagDialog(final Campaign campaign) {
        super(campaign, null, NAG_SINGLE_DROP_SET_UP, "SingleDropNagDialog");
    }

    public static boolean checkNag(List<AtBContract> activeContracts, boolean isSunday,
          boolean isUseStratConSinglesMode) {
        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_SINGLE_DROP_SET_UP) &&
                     SingleDropSetUpNagLogic.hasActiveStratConContract(activeContracts) &&
                     isSunday &&
                     isUseStratConSinglesMode;
    }
}
