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
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;

/**
 * Notifies the player that freeing prisoners resulted in an Intel Breach, worsening an active contract's morale.
 *
 * <p>The dialog reports the affected contract along with the morale change caused by the breach.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class PrisonerIntelBreachDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    /**
     * Displays the Intel Breach notification for the given contract.
     *
     * @param campaign  the current campaign instance
     * @param contract  the {@link AtBContract} whose morale was affected by the breach
     * @param oldMorale the contract's morale level before the breach
     * @param newMorale the contract's morale level after the breach
     *
     * @author Illiani
     * @since 0.51.01
     */
    public PrisonerIntelBreachDialog(Campaign campaign, AtBContract contract, AtBMoraleLevel oldMorale,
          AtBMoraleLevel newMorale) {
        String centerMessage = getFormattedTextAt(RESOURCE_BUNDLE, "intelBreach.ic",
              spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG);
        String bottomMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              "intelBreach.occ",
              contract.getName(),
              oldMorale.toString(),
              spanOpeningWithCustomColor(getNegativeColor()),
              newMorale.toString(),
              CLOSING_SPAN_TAG);

        new ImmersiveDialogNotification(campaign, centerMessage, bottomMessage, true);
    }
}
