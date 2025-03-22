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

import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import java.util.Collection;

import static mekhq.gui.dialog.nagDialogs.nagLogic.UnmaintainedUnitsNagLogic.campaignHasUnmaintainedUnits;

/**
 * A nag dialog that alerts the user about unmaintained units in the campaign's hangar.
 *
 * <p>
 * This dialog identifies units that require maintenance but have not received it yet,
 * excluding units marked as salvage. It provides a reminder to the player to keep
 * active units in proper working order.
 * </p>
 */
public class UnmaintainedUnitsNagDialog extends AbstractMHQNagDialog {
    /**
     * Constructs the nag dialog for unmaintained units.
     *
     * <p>
     * This constructor initializes the dialog with relevant campaign details and
     * formats the displayed message to include context for the commander.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public UnmaintainedUnitsNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_UNMAINTAINED_UNITS);

        final String DIALOG_BODY = "UnmaintainedUnitsNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
        showDialog();
    }

    /**
     * Determines whether a nag dialog should be displayed for unmaintained units in the campaign.
     *
     * <p>This method evaluates the following conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The check for unmaintained units is enabled ({@code isCheckMaintenance} is {@code true}).</li>
     *     <li>The user has not ignored the nag dialog for unmaintained units in the options.</li>
     *     <li>The campaign's hangar contains unmaintained units that are not salvage.</li>
     * </ul>
     *
     * @param units A {@link Collection} of {@link Unit} objects representing the campaign's hangar units.
     * @param isCheckMaintenance A flag indicating whether to check for unmaintained units.
     * @return {@code true} if the nag dialog should be displayed due to unmaintained units,
     *         {@code false} otherwise.
     */
    public static boolean checkNag(Collection<Unit> units, boolean isCheckMaintenance) {
        final String NAG_KEY = MHQConstants.NAG_UNMAINTAINED_UNITS;

        return isCheckMaintenance
              && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
              && campaignHasUnmaintainedUnits(units);
    }
}
