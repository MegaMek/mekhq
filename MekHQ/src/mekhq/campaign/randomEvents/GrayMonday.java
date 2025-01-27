/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.randomEvents;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.gui.dialog.randomEvents.GrayMondayDialog;

import java.time.LocalDate;

import static java.time.temporal.ChronoUnit.DAYS;
import static mekhq.campaign.Campaign.AdministratorSpecialization.LOGISTICS;
import static mekhq.campaign.finances.enums.TransactionType.STARTING_CAPITAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

public class GrayMonday {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.GrayMonday";

    public final static LocalDate EVENT_DATE_CLARION_NOTE = LocalDate.of(3132, 8,4);
    public final static LocalDate EVENT_DATE_GRAY_MONDAY = LocalDate.of(3132, 8,7);

    private final Campaign campaign;

    public GrayMonday(Campaign campaign, LocalDate today) {
        this.campaign = campaign;
        Person speaker = getSpeaker();

        int daysAfterClarionNote = (int) DAYS.between(EVENT_DATE_CLARION_NOTE, today);
        if (daysAfterClarionNote >= 0 && daysAfterClarionNote <= 3) {
            new GrayMondayDialog(campaign, speaker, true, daysAfterClarionNote);
        }

        int daysAfterGrayMonday = (int) DAYS.between(EVENT_DATE_GRAY_MONDAY, today);
        if (daysAfterGrayMonday > 0 && daysAfterGrayMonday <= 4) {
            boolean shouldShowDialog = daysAfterGrayMonday != 2;

            if (daysAfterGrayMonday == 2) {
                for (AtBContract contract : campaign.getAtBContracts()) {
                    LocalDate startDate = contract.getStartDate();
                    if (!startDate.isBefore(today)) {
                        shouldShowDialog = true;
                        break;
                    }
                }
            }

            if (shouldShowDialog) {
                new GrayMondayDialog(campaign, speaker, false, daysAfterGrayMonday);
            }
        }

        if (daysAfterGrayMonday == 1) {
            Finances finances = campaign.getFinances();
            Money balance = finances.getBalance();
            Money adjustedBalance = balance.multipliedBy(0.01);

            finances.getTransactions().clear();

            finances.getLoans().clear();

            finances.credit(STARTING_CAPITAL, today, adjustedBalance,
                getFormattedTextAt(RESOURCE_BUNDLE, "transaction.message"));

        }

        if (daysAfterGrayMonday == 3) {
            for (AtBContract contract : campaign.getAtBContracts()) {
                LocalDate startDate = contract.getStartDate();
                if (!startDate.isBefore(today)) {
                    contract.setBaseAmount(Money.of(0));
                    contract.setOverheadComp(0);
                    contract.setBattleLossComp(0);
                    contract.setStraightSupport(0);
                    contract.setTransportComp(0);
                    contract.setTransitAmount(Money.of(0));
                }
            }

            campaign.getContractMarket().getContracts().clear();

            getFormattedTextAt(RESOURCE_BUNDLE, "employer.report");
        }
    }


    /**
     * Retrieves the speaker for the dialogs.
     *
     * <p>The speaker is determined as the senior administrator personnel with the Logistics
     * specialization within the campaign. If no such person exists, this method returns {@code null}.</p>
     *
     * @return a {@link Person} representing the left speaker, or {@code null} if no suitable speaker is available
     */
    private @Nullable Person getSpeaker() {
        return campaign.getSeniorAdminPerson(LOGISTICS);
    }
}
