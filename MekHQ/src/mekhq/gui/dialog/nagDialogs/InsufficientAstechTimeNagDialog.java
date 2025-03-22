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

import static mekhq.gui.dialog.nagDialogs.nagLogic.InsufficientAstechTimeNagLogic.getAsTechTimeDeficit;
import static mekhq.gui.dialog.nagDialogs.nagLogic.InsufficientAstechTimeNagLogic.hasAsTechTimeDeficit;

/**
 * A dialog used to notify the user about insufficient available time for astechs to complete the
 * required maintenance tasks. Not to be confused with {@link InsufficientAstechsNagDialog}.
 *
 * <p>
 * This nag dialog is triggered when the available work time for the astech pool is inadequate to meet
 * the maintenance time requirements for the current campaign's hangar units. It provides a localized
 * message detailing the time deficit and allows the user to take necessary action or dismiss the dialog.
 * </p>
 *
 * <strong>Features:</strong>
 * <ul>
 *   <li>Calculates the time deficit for the astech pool based on hangar unit maintenance requirements.</li>
 *   <li>Notifies the user when there is inadequate time available to maintain all units.</li>
 *   <li>Extends {@link AbstractMHQNagDialog} for consistent nag dialog behavior.</li>
 * </ul>
 */
public class InsufficientAstechTimeNagDialog extends AbstractMHQNagDialog {
    /**
     * Constructs an {@code InsufficientAstechTimeNagDialog} for the given campaign.
     *
     * <p>
     * This dialog calculates the astech time deficit and uses a localized message
     * to notify the user about the shortage of available time. The message provides
     * the commander's address, the time deficit, and a pluralized suffix for correctness.
     * </p>
     *
     * @param campaign The {@link Campaign} tied to this nag dialog.
     *                 The campaign provides data about hangar units and astech availability.
     */
    public InsufficientAstechTimeNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_INSUFFICIENT_ASTECH_TIME);

        int asTechsTimeDeficit = getAsTechTimeDeficit(campaign.getUnits(), campaign.getPossibleAstechPoolMinutes(),
              campaign.isOvertimeAllowed(), campaign.getPossibleAstechPoolOvertime());

        String pluralizer = (asTechsTimeDeficit > 1) ? "s" : "";

        final String DIALOG_BODY = "InsufficientAstechTimeNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false), asTechsTimeDeficit, pluralizer));
        showDialog();
    }

    /**
     * Determines whether a nag dialog should be displayed due to insufficient AsTech time in the campaign.
     *
     * <p>This method evaluates the following conditions to determine whether the nag dialog needs to appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for insufficient AsTech time in their options.</li>
     *     <li>There is a positive deficit in the available AsTech time for maintaining the campaign's units.</li>
     * </ul>
     *
     * @param units A collection of {@link Unit} objects to evaluate for maintenance needs.
     * @param possibleAstechPoolMinutes The total available AsTech work minutes without considering overtime.
     * @param isOvertimeAllowed A flag indicating whether overtime is allowed, which adds to the available AsTech work time.
     * @param possibleAstechPoolOvertime The additional AsTech work minutes available if overtime is allowed.
     *
     * @return {@code true} if the nag dialog should be displayed due to insufficient AsTech time,
     *         {@code false} otherwise.
     */
    public static boolean checkNag(Collection<Unit> units, int possibleAstechPoolMinutes,
                                   boolean isOvertimeAllowed, int possibleAstechPoolOvertime) {
        final String NAG_KEY = MHQConstants.NAG_INSUFFICIENT_ASTECH_TIME;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
              && hasAsTechTimeDeficit(units, possibleAstechPoolMinutes, isOvertimeAllowed, possibleAstechPoolOvertime);
    }
}
