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
package mekhq.campaign.randomEvents.prisoners;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.Person;
import mekhq.gui.dialog.randomEvents.prisonerDialogs.PrisonerRansomEventDialog;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.max;
import static mekhq.campaign.personnel.enums.PersonnelStatus.ACTIVE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * Handles ransom events for prisoners of war (POWs) within the campaign.
 *
 * <p>This class manages both scenarios where the opposing force requests ransom for captured
 * friendly personnel and where the player can accept ransom offers for enemy prisoners.
 * It calculates the total ransom, determines the list of prisoners involved, and manages the
 * corresponding financial and personnel updates based on the player's decision.</p>
 */
public class PrisonerRansomEvent {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    private static final int RANSOM_COST_DIVIDER = 5;
    private static final int RANSOM_COST_MULTIPLIER = 10;
    private static final int ACCEPTED = 1; // Choice for accepting ransom
    private static final double RANSOM_PERCENTAGE = 0.1; // Allow 10% of prisoners to be ransomed in any given event

    /**
     * Creates a new ransom event for prisoners of war (POWs).
     *
     * <p>Depending on the event type (friendly or enemy POWs), this constructor:</p>
     * <ul>
     *     <li>Determines the list of prisoners involved in the ransom event.</li>
     *     <li>Calculates the ransom amount for the selected prisoners.</li>
     *     <li>Prompts the player to accept or decline the ransom via a dialog.</li>
     *     <li>Handles the financial transaction and updates the prisoners' status based on the
     *     player's choice.</li>
     * </ul>
     *
     * @param campaign       The current campaign instance, which provides game state and relevant data.
     * @param isFriendlyPOWs {@code true} if the ransom event is for friendly POWs (player's personnel),
     *                       {@code false} if it's for enemy prisoners.
     */
    public PrisonerRansomEvent(Campaign campaign, boolean isFriendlyPOWs) {
        List<Person> prisoners = isFriendlyPOWs
            ? campaign.getFriendlyPrisoners()
            : campaign.getCurrentPrisoners();

        // Exit early if there are no prisoners
        if (prisoners.isEmpty()) {
            return;
        }

        // Sort prisoners by experience level in descending order
        if (!isFriendlyPOWs) {
            // The OpFor always requests their best personnel first
            prisoners.sort(Comparator.comparing(person -> person.getExperienceLevel(campaign, false),
                Comparator.reverseOrder()));
        } else {
            // The OpFor offers ransom POWs
            Collections.shuffle(prisoners);
        }

        int prisonerRansomCount = calculateRansomCount(prisoners.size());
        List<Person> ransomList = prisoners.subList(0, prisonerRansomCount);

        Money totalRansom = calculateTotalRansom(ransomList, campaign, isFriendlyPOWs);

        // Handle friendly POWs (player's prisoners) specifically
        if (isFriendlyPOWs && !canAffordRansom(campaign, totalRansom)) {
            return; // Exit if funds are insufficient to cover the ransom
        }

        // Launch ransom dialog to ask for the player's decision
        PrisonerRansomEventDialog eventDialog = new PrisonerRansomEventDialog(campaign, ransomList,
            totalRansom, isFriendlyPOWs);
        int choice = eventDialog.getDialogChoice();

        if (choice == ACCEPTED) {
            handleRansomOutcome(campaign, ransomList, totalRansom, isFriendlyPOWs);
        }
    }

    /**
     * Calculates the number of prisoners involved in the ransom event.
     *
     * <p>The number is determined as a percentage of the total prisoners available, ensuring
     * that at least one prisoner is always included in the event.</p>
     *
     * @param prisonerCount The total number of prisoners available for the ransom.
     * @return The number of prisoners to be involved in the ransom event.
     */
    private int calculateRansomCount(int prisonerCount) {
        return max(1, (int) Math.ceil(prisonerCount * RANSOM_PERCENTAGE));
    }

    /**
     * Calculates the total ransom amount for the list of prisoners.
     *
     * <p>The ransom value for each prisoner is fetched and aggregated. The total ransom is adjusted
     * based on whether the prisoners are friendly or enemy personnel:</p>
     * <ul>
     *     <li>For friendly POWs, the ransom is multiplied by a predefined multiplier.</li>
     *     <li>For enemy prisoners, the ransom is divided by a predefined divider.</li>
     * </ul>
     *
     * @param ransomList     A list of {@link Person} objects representing the prisoners involved
     *                      in the event.
     * @param campaign       The current campaign context for calculating ransom values.
     * @param isFriendlyPOWs {@code true} if the ransom is for friendly POWs, {@code false} for
     *                                  enemy prisoners.
     * @return The total ransom amount as {@link Money}.
     */
    private Money calculateTotalRansom(List<Person> ransomList, Campaign campaign, boolean isFriendlyPOWs) {
        Money ransom = Money.zero();
        for (Person person : ransomList) {
            Money ransomValue = person.getRansomValue(campaign);
            ransom = ransom.plus(ransomValue);
        }

        return isFriendlyPOWs
            ? ransom.multipliedBy(RANSOM_COST_MULTIPLIER)
            : ransom.dividedBy(RANSOM_COST_DIVIDER);
    }

    /**
     * Determines if the player can afford the ransom amount for friendly POWs.
     *
     * @param campaign The current campaign context containing financial information.
     * @param ransom   The calculated ransom amount.
     * @return {@code true} if the player has sufficient funds to cover the ransom, {@code false}
     * otherwise.
     */
    private boolean canAffordRansom(Campaign campaign, Money ransom) {
        Money currentFunds = campaign.getFinances().getBalance();
        return !currentFunds.isLessThan(ransom);
    }

    /**
     * Handles the outcome of the ransom event, based on the player's decision.
     *
     * <p>If the ransom is accepted:</p>
     * <ul>
     *     <li>For friendly POWs: Funds are debited, and the prisoners' status changes to active
     *     duty.</li>
     *     <li>For enemy prisoners: Funds are credited, and the prisoners are removed from the
     *     campaign.</li>
     * </ul>
     *
     * @param campaign       The current campaign instance for managing updates.
     * @param ransomList     A list of {@link Person} objects representing the prisoners involved in the event.
     * @param ransom         The total ransom amount.
     * @param isFriendlyPOWs {@code true} if the event is for friendly POWs, {@code false} for enemy prisoners.
     */
    private void handleRansomOutcome(Campaign campaign, List<Person> ransomList, Money ransom,
                                  boolean isFriendlyPOWs) {
        if (isFriendlyPOWs) {
            // Debit funds and return POWs to active duty
            campaign.getFinances().debit(TransactionType.RANSOM, campaign.getLocalDate(), ransom,
                getFormattedTextAt(RESOURCE_BUNDLE, "ransom.entry"));
            ransomList.forEach(pow -> pow.changeStatus(campaign, campaign.getLocalDate(), ACTIVE));
        } else {
            // Credit funds and remove enemy prisoners from campaign
            campaign.getFinances().credit(TransactionType.RANSOM, campaign.getLocalDate(), ransom,
                getFormattedTextAt(RESOURCE_BUNDLE, "ransom.entry"));
            ransomList.forEach(campaign::removePerson);
        }
    }
}
