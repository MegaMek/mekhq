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
package mekhq.gui.dialog.nagDialogs;

import static mekhq.MHQConstants.NAG_EXPIRING_NAV_POINTS;
import static mekhq.gui.dialog.nagDialogs.nagLogic.ExpiringNavPointsNagLogic.determineExpiringNavPoints;
import static mekhq.gui.dialog.nagDialogs.nagLogic.ExpiringNavPointsNagLogic.hasExpiringNavPoints;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * Warns the player, before the day advances, about StratCon Nav Points that will expire without having been dealt
 * with.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ExpiringNavPointsNagDialog extends ImmersiveDialogNag {
    public ExpiringNavPointsNagDialog(final Campaign campaign) {
        super(campaign, NAG_EXPIRING_NAV_POINTS, "ExpiringNavPointsNagDialog");
    }

    @Override
    protected String getInCharacterMessage(Campaign campaign, String key, String commanderAddress) {
        final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

        String expiringNavPointsReport = determineExpiringNavPoints(campaign.getActiveContracts(),
              campaign.getLocalDate());

        return getFormattedTextAt(RESOURCE_BUNDLE, key + ".ic", commanderAddress, expiringNavPointsReport);
    }

    /**
     * @param isUseStratCon   whether StratCon is enabled in the campaign options
     * @param activeContracts the active contracts to check
     * @param today           the current campaign date
     *
     * @return {@code true} if StratCon is in use, the player has not suppressed this nag, and a visible Nav Point will
     *       expire when the day advances
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean checkNag(boolean isUseStratCon, List<AbstractContract> activeContracts, LocalDate today) {
        return isUseStratCon &&
                     !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_EXPIRING_NAV_POINTS) &&
                     hasExpiringNavPoints(activeContracts, today);
    }
}
