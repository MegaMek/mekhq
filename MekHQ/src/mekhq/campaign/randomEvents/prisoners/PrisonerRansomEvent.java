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
package mekhq.campaign.randomEvents.prisoners;

import static java.lang.Math.max;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.personnel.enums.PersonnelStatus.ACTIVE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Handles ransom events for prisoners of war (POWs) within the campaign.
 *
 * <p>This class manages both scenarios where the opposing force requests ransom for captured
 * friendly personnel and where the player can accept ransom offers for enemy prisoners. It calculates the total ransom,
 * determines the list of prisoners involved, and manages the corresponding financial and personnel updates based on the
 * player's decision.</p>
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
     * @param isFriendlyPOWs {@code true} if the ransom event is for friendly POWs (player's personnel), {@code false}
     *                       if it's for enemy prisoners.
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
        int choice = getChoiceIndex(campaign, isFriendlyPOWs, totalRansom, ransomList);

        if (choice == ACCEPTED) {
            handleRansomOutcome(campaign, ransomList, totalRansom, isFriendlyPOWs);
        }
    }

    /**
     * Presents a dialog to the player for making a decision regarding a ransom event involving prisoners or friendly
     * POWs.
     *
     * <p>
     * Builds an in-character message and displays a dialog with options to accept or decline the ransom offer. The
     * dialog content and options are customized based on whether the event involves friendly POWs or enemy prisoners.
     * Returns the index of the player's chosen response.
     * </p>
     *
     * @param campaign       the current {@link Campaign} context
     * @param isFriendlyPOWs {@code true} if the ransom offer concerns friendly POWs, {@code false} for enemy prisoners
     * @param totalRansom    the total {@link Money} amount required for the ransom transaction
     * @param ransomList     the list of {@link Person} objects affected by the ransom event
     *
     * @return the index of the chosen response option in the dialog (e.g., 0 for decline, 1 for accept)
     *
     * @author Illiani
     * @since 0.50.06
     */
    private static int getChoiceIndex(Campaign campaign, boolean isFriendlyPOWs, Money totalRansom,
          List<Person> ransomList) {
        String inCharacterMessage = createInCharacterMessage(campaign.getCommanderAddress(),
              totalRansom,
              ransomList,
              isFriendlyPOWs);

        List<String> options = List.of(getFormattedTextAt(RESOURCE_BUNDLE, "decline.button"),
              getFormattedTextAt(RESOURCE_BUNDLE, "accept.button"));

        String key = isFriendlyPOWs ? "pows" : "prisoners";
        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, key + ".ooc");

        ImmersiveDialogSimple eventDialog = new ImmersiveDialogSimple(campaign,
              campaign.getSeniorAdminPerson(COMMAND),
              null,
              inCharacterMessage,
              options,
              outOfCharacterMessage,
              null,
              false);

        return eventDialog.getDialogChoice();
    }

    /**
     * Creates the immersive in-character message for the dialog.
     *
     * <p>The generated message is tailored to whether the offer involves friendly or enemy prisoners.
     * It includes details about the ransom amount, the list of prisoners, and addresses the player in their in-universe
     * title. The prisoners are listed in a structured table format for clarity and immersion.</p>
     *
     * @param commanderAddress The term used to address the campaign commander.
     * @param payment          The ransom amount offered for the prisoners.
     * @param prisoners        The list of prisoners involved in the transaction.
     * @param isFriendlyPOWs   {@code true} if the ransom is for friendly prisoners captured by the enemy, {@code false}
     *                         if it involves enemy prisoners held by the player.
     *
     * @return A formatted HTML string containing the narrative in-character message for the dialog.
     */
    private static String createInCharacterMessage(String commanderAddress, Money payment, List<Person> prisoners,
          boolean isFriendlyPOWs) {
        StringBuilder message = new StringBuilder();
        String key = isFriendlyPOWs ? "pows" : "prisoners";
        message.append(getFormattedTextAt(RESOURCE_BUNDLE, key + ".message", commanderAddress, payment));

        // Create a table to hold the personnel
        message.append("<br><table style='width:100%; text-align:left;'>");

        for (int i = 0; i < prisoners.size(); i++) {
            if (i % 2 == 0) {
                message.append("<tr>");
            }

            // Add the person in a column
            Person person = prisoners.get(i);
            message.append("<td>- ").append(person.getHyperlinkedFullTitle()).append("</td>");

            if ((i + 1) % 2 == 0 || i == prisoners.size() - 1) {
                message.append("</tr>");
            }
        }

        message.append("</table>");
        return message.toString();
    }

    /**
     * Calculates the number of prisoners involved in the ransom event.
     *
     * <p>The number is determined as a percentage of the total prisoners available, ensuring
     * that at least one prisoner is always included in the event.</p>
     *
     * @param prisonerCount The total number of prisoners available for the ransom.
     *
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
     * @param ransomList     A list of {@link Person} objects representing the prisoners involved in the event.
     * @param campaign       The current campaign context for calculating ransom values.
     * @param isFriendlyPOWs {@code true} if the ransom is for friendly POWs, {@code false} for enemy prisoners.
     *
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
     *
     * @return {@code true} if the player has sufficient funds to cover the ransom, {@code false} otherwise.
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
