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
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

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
    private final Campaign campaign;

    /**
     * Checks whether the campaign has any unmaintained units in the hangar.
     *
     * <p>
     * This method iterates over the units in the campaign's hangar and identifies units
     * that meet the following criteria:
     * <ul>
     *     <li>The unit is classified as unmaintained ({@link Unit#isUnmaintained()}).</li>
     *     <li>The unit is not marked as salvage ({@link Unit#isSalvage()}).</li>
     * </ul>
     * If any units match these conditions, the method returns {@code true}.
     *
     * @return {@code true} if unmaintained units are found, otherwise {@code false}.
     */
    boolean campaignHasUnmaintainedUnits() {
        for (Unit unit : campaign.getHangar().getUnits()) {
            if ((unit.isUnmaintained()) && (!unit.isSalvage())) {
                return true;
            }
        }
        return false;
    }

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

        this.campaign = campaign;

        final String DIALOG_BODY = "UnmaintainedUnitsNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
    }

    /**
     * Determines whether the nag dialog for unmaintained units should be displayed.
     *
     * <p>
     * The dialog is shown if:
     * <ul>
     *     <li>Maintenance checks are enabled.</li>
     *     <li>The nag dialog for unmaintained units is not ignored in MekHQ options.</li>
     *     <li>There are unmaintained units in the campaign hangar, as determined by
     *     {@link #campaignHasUnmaintainedUnits()}.</li>
     * </ul>
     * If both conditions are met, the dialog is displayed to alert the player to address unit maintenance.
     */
    public void checkNag() {
        final String NAG_KEY = MHQConstants.NAG_UNMAINTAINED_UNITS;

        if (campaign.getCampaignOptions().isCheckMaintenance()
            && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && campaignHasUnmaintainedUnits()) {
            showDialog();
        }
    }
}
