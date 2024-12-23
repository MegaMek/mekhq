/*
 * Copyright (c) 2021-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog.nagDialogs;

import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.AbstractMHQNagDialog_NEW;

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
 *   <li>Extends {@link AbstractMHQNagDialog_NEW} for consistent nag dialog behavior.</li>
 * </ul>
 */
public class InsufficientAstechTimeNagDialog extends AbstractMHQNagDialog_NEW {
    private int asTechsTimeDeficit = 0;

    /**
     * Calculates the astech time deficit for the campaign.
     *
     * <p>
     * This method determines the total maintenance time required for all valid hangar units
     * in the campaign and compares it to the available astech work time. Units are only considered
     * valid if they meet the following conditions:
     * <ul>
     *   <li>Not marked as unmaintained.</li>
     *   <li>Present in the hangar.</li>
     *   <li>Not self-crewed (units maintained by their own crew are excluded).</li>
     * </ul>
     * Each valid unit requires six astechs per unit of maintenance time. If overtime is allowed in
     * the campaign, it is added to the available astech pool. The deficit is calculated, rounded up,
     * and stored in {@link #asTechsTimeDeficit}.
     * </p>
     *
     * @param campaign The {@link Campaign} from which to calculate the maintenance requirements and work time.
     */
    private void getAstechTimeDeficit(Campaign campaign) {
        // Units are only valid if they are maintained, present, and not self crewed (as the crew
        // maintain it in that case).
        // For each unit, this is valid for; we need six asTechs to help the tech for the maintenance.
        final int need = campaign.getHangar().getUnitsStream()
                .filter(unit -> !unit.isUnmaintained() && unit.isPresent() && !unit.isSelfCrewed())
                .mapToInt(unit -> unit.getMaintenanceTime() * 6).sum();

        int available = campaign.getPossibleAstechPoolMinutes();
        if (campaign.isOvertimeAllowed()) {
            available += campaign.getPossibleAstechPoolOvertime();
        }

        asTechsTimeDeficit = (int) Math.ceil((need - available) / (double) Person.PRIMARY_ROLE_SUPPORT_TIME);
    }

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

        getAstechTimeDeficit(campaign);

        String pluralizer = (asTechsTimeDeficit > 1) ? "s" : "";

        final String DIALOG_BODY = "InsufficientAstechTimeNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false), asTechsTimeDeficit, pluralizer));
    }

    /**
     * Determines whether the "Insufficient Astech Time" nag dialog should be displayed.
     *
     * <p>
     * This method checks the following conditions:
     * <ul>
     *   <li>If the "Insufficient Astech Time" nag dialog is flagged as ignored in the user settings.</li>
     *   <li>If there is a time deficit in the astech pool based on the campaign's
     *   maintenance requirements.</li>
     * </ul>
     * If both conditions are met, the dialog is displayed to alert the user about
     * insufficient available time for astechs.
     * </p>
     */
    public void checkNag() {
        final String NAG_KEY = MHQConstants.NAG_INSUFFICIENT_ASTECH_TIME;

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && (asTechsTimeDeficit > 0)) {
            showDialog();
        }
    }
}
