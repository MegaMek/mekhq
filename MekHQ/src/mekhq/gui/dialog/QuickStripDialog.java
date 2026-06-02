/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

public class QuickStripDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.QuickStripDialog";

    private boolean wasConfirmed = false;

    public boolean wasConfirmed() {
        return wasConfirmed;
    }

    public QuickStripDialog(Campaign campaign) {
        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              campaign.getSeniorTechPerson(),
              null,
              getICText(campaign.getCommanderAddress()),
              getOptions(),
              getOOCText(),
              null,
              true);

        wasConfirmed = dialog.getDialogChoice() == 0;
    }

    private String getICText(String commanderAddress) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "TaskTableMouseAdapter.quickStrip.dialog.ic", commanderAddress);
    }

    private String getOOCText() {
        return getTextAt(RESOURCE_BUNDLE, "TaskTableMouseAdapter.quickStrip.dialog.ooc");
    }

    private List<String> getOptions() {
        String mrms = getTextAt(RESOURCE_BUNDLE, "TaskTableMouseAdapter.quickStrip.button.mrms");
        String manual = getTextAt(RESOURCE_BUNDLE, "TaskTableMouseAdapter.quickStrip.button.manual");
        String cancel = getTextAt(RESOURCE_BUNDLE, "TaskTableMouseAdapter.quickStrip.button.cancel");

        return List.of(mrms, manual, cancel);
    }
}
