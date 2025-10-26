/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.MHQConstants.NAG_UNABLE_TO_AFFORD_RENT;
import static mekhq.campaign.Campaign.AdministratorSpecialization.LOGISTICS;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UnableToAffordRentNagLogic.unableToAffordRent;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.DayOfWeek;
import java.time.LocalDate;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.rentals.FacilityRentals;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

public class UnableToAffordRentNagDialog extends ImmersiveDialogNag {
    public UnableToAffordRentNagDialog(final Campaign campaign) {
        super(campaign, LOGISTICS, NAG_UNABLE_TO_AFFORD_RENT, "UnableToAffordRentNagDialog");
    }

    @Override
    protected String getInCharacterMessage(Campaign campaign, String key, String commanderAddress) {
        final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

        final LocalDate today = campaign.getLocalDate();
        final boolean isSunday = today.getDayOfWeek() == DayOfWeek.SUNDAY;
        final boolean isLastDayOfMonth = today.getDayOfMonth() == today.lengthOfMonth();

        Money rent = Money.zero();
        if (isSunday) {
            rent = rent.plus(FacilityRentals.getTotalRentSumFromRentedBays(campaign, campaign.getFinances()));
        }
        if (isLastDayOfMonth) {
            rent = rent.plus(campaign.getTotalRentFeesExcludingBays());
        }

        Money currentFunds = campaign.getFunds();
        Money deficit = rent.minus(currentFunds);

        return getFormattedTextAt(RESOURCE_BUNDLE,
              key + ".ic",
              commanderAddress,
              rent.toAmountString(),
              currentFunds.toAmountString(),
              deficit.toAmountString());
    }

    /**
     * Checks whether a user notification ("nag") about unpaid rent should be triggered for the campaign.
     *
     * <p>Calculates the current or end-of-month rental sum, depending on context, and returns whether conditions for
     * showing the nag are met: today is the last day of the month, the nag dialog has not been suppressed, and the
     * campaign cannot afford to pay rent.</p>
     *
     * @param campaign         the current {@link Campaign} context
     * @param isLastDayOfMonth {@code true} if the check is on the last day of the Month
     *
     * @return {@code true} if conditions warrant showing a rent payment nag, {@code false} otherwise
     */
    public static boolean checkNag(Campaign campaign, boolean isSunday, boolean isLastDayOfMonth) {
        Money rent = Money.zero();
        if (isSunday) {
            rent = rent.plus(FacilityRentals.getTotalRentSumFromRentedBays(campaign, campaign.getFinances()));
        }

        if (isLastDayOfMonth) {
            rent = rent.plus(campaign.getTotalRentFeesExcludingBays());
        }

        return unableToAffordRent(campaign.getFunds(), rent);
    }
}
