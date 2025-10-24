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
package mekhq.campaign.randomEvents;

import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.finances.enums.TransactionType.STARTING_CAPITAL;
import static mekhq.campaign.personnel.enums.PersonnelRole.ADMINISTRATOR_COMMAND;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

public class GrayMonday {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.GrayMonday";

    public final static LocalDate GRAY_MONDAY_EVENTS_BEGIN = LocalDate.of(3132, 8, 3);
    public final static LocalDate BANKRUPTCY = LocalDate.of(3132, 8, 9);
    public final static LocalDate EMPLOYER_BEGGING = LocalDate.of(3132, 8, 10);
    public final static LocalDate GRAY_MONDAY_EVENTS_END = LocalDate.of(3132, 8, 12);

    /**
     * @deprecated unused
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public final static LocalDate EVENT_DATE_CLARION_NOTE = LocalDate.of(3132, 8, 4);

    /**
     * @deprecated unused except in deprecated classes
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public final static LocalDate EVENT_DATE_GRAY_MONDAY = LocalDate.of(3132, 8, 7);

    private final Campaign campaign;

    public GrayMonday(Campaign campaign, LocalDate today) {
        this.campaign = campaign;

        boolean isEmployerBegging = today.equals(EMPLOYER_BEGGING);
        if (campaign.getCampaignOptions().isSimulateGrayMonday()) {
            if (today.equals(BANKRUPTCY)) {
                Finances finances = campaign.getFinances();
                Money balance = finances.getBalance();
                Money adjustedBalance = balance.multipliedBy(0.99);

                finances.debit(STARTING_CAPITAL,
                      today,
                      adjustedBalance,
                      getFormattedTextAt(RESOURCE_BUNDLE, "transaction.message"));

                finances.getLoans().clear();
            }

            if (isEmployerBegging) {
                for (AtBContract contract : campaign.getAtBContracts()) {
                    LocalDate startDate = contract.getStartDate();
                    if (!startDate.isBefore(today)) {
                        contract.setBaseAmount(Money.of(0));
                        contract.setOverheadComp(0);
                        contract.setBattleLossComp(0);
                        contract.setStraightSupport(0);
                        contract.setTransportComp(0);
                        contract.calculateContract(campaign);

                        contract.setSalvagePct(100);
                    }
                }

                campaign.getContractMarket().getContracts().clear();

                getFormattedTextAt(RESOURCE_BUNDLE, "employer.report");
            }
        }

        String resourceKey;
        if (today.isAfter(GRAY_MONDAY_EVENTS_BEGIN) && today.isBefore(GRAY_MONDAY_EVENTS_END)) {
            resourceKey = "event." + today.getDayOfMonth() + ".message";
        } else {
            return;
        }

        Person speaker = null;

        if (isEmployerBegging) {
            for (AtBContract contract : campaign.getAtBContracts()) {
                LocalDate startDate = contract.getStartDate();
                if (!startDate.isBefore(today)) {
                    speaker = getEmployerSpeaker(contract);
                    break;
                }
            }
        } else {
            speaker = getSpeaker();
        }

        // This means there is no active contract
        if (isEmployerBegging && speaker == null) {
            return;
        }

        String commanderAddress = campaign.getCommanderAddress();
        String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, resourceKey, commanderAddress);
        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "dialog.ooc");

        new ImmersiveDialogSimple(campaign,
              speaker,
              null,
              inCharacterMessage,
              null,
              outOfCharacterMessage,
              null,
              false);
    }

    /**
     * Retrieves the speaker for the dialogs.
     *
     * <p>The speaker is determined as the senior administrator personnel with the Command
     * specialization within the campaign. If no such person exists, this method returns {@code null}.</p>
     *
     * @return a {@link Person} representing the left speaker, or {@code null} if no suitable speaker is available
     */
    private @Nullable Person getSpeaker() {
        return campaign.getSeniorAdminPerson(COMMAND);
    }

    /**
     * Creates and returns a {@link Person} object representing the employer for a given contract.
     *
     * <p>
     * The employer speaker is initialized with the contract's employer faction, assigned a randomized gender, and its
     * origin faction is set accordingly.
     * </p>
     *
     * @param contract the {@link AtBContract} whose employer faction is used to create the speaker
     *
     * @return a {@link Person} representing the employer, with appropriate faction and origin set
     *
     * @author Illiani
     * @since 0.50.06
     */
    private Person getEmployerSpeaker(AtBContract contract) {
        String employer = contract.getEmployerFaction().getShortName();
        Person speaker = campaign.newPerson(ADMINISTRATOR_COMMAND, employer, Gender.RANDOMIZE);
        speaker.setOriginFaction(Factions.getInstance().getFaction(employer));

        return speaker;
    }

    /**
     * Determines whether the current date falls within the Gray Monday event period.
     *
     * <p>This method checks if the Gray Monday event is enabled and whether the given date
     * falls within the defined period of the Gray Monday event.
     *
     * @param today           The current date as a {@link LocalDate} object.
     * @param isUseGrayMonday A {@code boolean} flag indicating whether the campaign is tracking Gray Monday.
     *
     * @return {@code true} if Gray Monday is active for the given date and campaign configuration, {@code false}
     *       otherwise.
     */
    public static boolean isGrayMonday(LocalDate today, boolean isUseGrayMonday) {
        return isUseGrayMonday &&
                     today.isAfter(GRAY_MONDAY_EVENTS_BEGIN) &&
                     today.isBefore(GRAY_MONDAY_EVENTS_BEGIN.plusMonths(12));
    }
}
