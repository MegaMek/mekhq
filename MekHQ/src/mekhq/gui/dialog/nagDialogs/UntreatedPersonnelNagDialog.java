/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

public class UntreatedPersonnelNagDialog extends AbstractMHQNagDialog_NEW {
    static boolean campaignHasUntreatedInjuries(Campaign campaign) {
        for (Person person : campaign.getActivePersonnel()) {
            if (person.needsFixing()
                && person.getDoctorId() == null
                && !person.getPrisonerStatus().isCurrentPrisoner()) {
                return true;
            }
        }
        return false;
    }

    public UntreatedPersonnelNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_UNTREATED_PERSONNEL);

        final String DIALOG_BODY = "UntreatedPersonnelNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
    }

    public void checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_UNTREATED_PERSONNEL;

        if (campaign.getCampaignOptions().isUseStratCon()
            && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && !campaignHasUntreatedInjuries(campaign)) {
            showDialog();
        }
    }
}
