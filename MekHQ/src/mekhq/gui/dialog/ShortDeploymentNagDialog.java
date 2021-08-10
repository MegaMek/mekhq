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
package mekhq.gui.dialog;

import mekhq.MekHQ;
import mekhq.MekHqConstants;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;
import java.time.DayOfWeek;

public class ShortDeploymentNagDialog extends AbstractMHQNagDialog {
    //region Constructors
    public ShortDeploymentNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "ShortDeploymentNagDialog", "ShortDeploymentNagDialog.title",
                "ShortDeploymentNagDialog.text", campaign, MekHqConstants.NAG_SHORT_DEPLOYMENT);
    }
    //endregion Constructors

    @Override
    protected boolean checkNag(final Campaign campaign) {
        if (MekHQ.getMekHQOptions().getNagDialogIgnore(getKey())
                || !campaign.getLocation().isOnPlanet()
                || (campaign.getLocalDate().getDayOfWeek() != DayOfWeek.SUNDAY)) {
            return false;
        }

        return campaign.getActiveAtBContracts().stream()
                .anyMatch(contract -> campaign.getDeploymentDeficit(contract) > 0);
    }
}
