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
import static java.lang.Math.min;
import static megamek.common.Compute.randomInt;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.campaign.finances.enums.TransactionType.RANSOM;
import static mekhq.campaign.personnel.enums.PersonnelStatus.ACTIVE;
import static mekhq.campaign.personnel.enums.PersonnelStatus.HOMICIDE;
import static mekhq.campaign.personnel.enums.PersonnelStatus.LEFT;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.MAX_CRIME_PENALTY;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.utilities.ReportingUtilities;

/**
 * Handles events involving prisoners at the end of a mission.
 *
 * <p>This class manages the outcomes for prisoners of war (POWs) at the conclusion
 * of a mission. These outcomes can include release, execution, ransom, or other status updates based on mission
 * success, alliances, and player choices. It also handles financial transactions related to ransoms and updates the
 * campaign state accordingly.</p>
 */
public class PrisonerMissionEndEvent {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    private final Campaign campaign;
    private final Mission mission;
    private boolean isSuccess;
    private boolean isAllied;

    final static int GOOD_EVENT_CHANCE = 20;

    private final int CHOICE_ACCEPTED = 0;
    private final int CHOICE_RELEASE_THEM = 1;
    private final int CHOICE_EXECUTE_THEM = 2;

    /**
     * Creates a new instance of `PrisonerMissionEndEvent` to handle mission-end prisoner events.
     *
     * @param campaign The current campaign instance, providing context and data about prisoners and finances.
     * @param mission  The current mission related to this event.
     */
    public PrisonerMissionEndEvent(Campaign campaign, Mission mission) {
        this.campaign = campaign;
        this.mission = mission;
    }

    /**
     * Handles defectors at the end of a mission.
     *
     * <p>Prompts the player with a dialog to remind them they have unresolved Prisoner Defectors.</p>
     *
     * @return An integer representing the player's choice in the defector-handling dialog.
     */
    public int handlePrisonerDefectors() {
        String commanderAddress = campaign.getCommanderAddress();
        String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "prisonerDefectors.message", commanderAddress);

        List<String> dialogOptions = List.of(getFormattedTextAt(RESOURCE_BUNDLE, "cancel.button"),
              getFormattedTextAt(RESOURCE_BUNDLE, "continue.button"));

        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "prisonerDefectors.ooc");
        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              campaign.getSeniorAdminPerson(HR),
              null,
              inCharacterMessage,
              dialogOptions,
              outOfCharacterMessage,
              null,
              false);

        return dialog.getDialogChoice();
    }

    /**
     * Processes the handling of prisoners after a mission ends.
     *
     * <p>This method determines the list of prisoners involved, calculates ransom amounts if
     * applicable, determines the likelihood of a positive ("good") event, and presents the player with a dialog for
     * managing prisoners. Outcomes depend on whether the player succeeds in the mission, prisoner allegiance, and
     * good/bad events.</p>
     *
     * @param isSuccess {@code true} if the mission was a success, {@code false} otherwise.
     * @param isAllied  {@code true} if the prisoners are allied POWs, {@code false} if they belong to the enemy.
     */
    public void handlePrisoners(boolean isSuccess, boolean isAllied) {
        this.isAllied = isAllied;
        this.isSuccess = isSuccess;

        List<Person> prisoners = isAllied ? campaign.getFriendlyPrisoners() : campaign.getCurrentPrisoners();
        Money ransom = getRansom(prisoners);

        int goodEventChance = determineGoodEventChance(isAllied);
        boolean isGoodEvent = randomInt(goodEventChance) > 0;

        String key = "prisoners." +
                           (isAllied ? "player" : "enemy") +
                           '.' +
                           (isSuccess ? "victory" : "defeat") +
                           '.' +
                           (isGoodEvent ? "good" : "bad") +
                           '.' +
                           randomInt(50);

        String commanderAddress = campaign.getCommanderAddress();
        String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, key, commanderAddress, ransom.toAmountString());

        String outOfCharacterMessage = null;
        if (isAllied && !isSuccess && isGoodEvent) {
            outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "prisoners.ransom.ooc");
        }

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              campaign.getSeniorAdminPerson(COMMAND),
              null,
              inCharacterMessage,
              getEndOfContractDialogButtons(isAllied, isSuccess, isGoodEvent),
              outOfCharacterMessage,
              null,
              false);

        processPlayerResponse(ransom, isGoodEvent, dialog.getDialogChoice(), prisoners);
    }

    /**
     * Builds the list of buttons to be displayed in the dialog based on the parameters.
     *
     * <p>
     * The available buttons depend on the ownership (allied/enemy), mission outcome, and the type of event. Buttons may
     * include options such as "Accept Ransom", "Decline Ransom", "Release Prisoners", and "Execute Prisoners".
     * </p>
     *
     * @param isAllied    Indicates whether the prisoners are allied.
     * @param isSuccess   Indicates whether the mission was successful.
     * @param isGoodEvent Indicates whether the event is positive.
     *
     * @return A list of buttons to be displayed on the dialog.
     */
    private static List<String> getEndOfContractDialogButtons(boolean isAllied, boolean isSuccess,
          boolean isGoodEvent) {
        List<String> buttons = new ArrayList<>();

        boolean isRansom = (!isAllied && isSuccess && isGoodEvent) ||
                                 (!isAllied && !isSuccess && isGoodEvent) ||
                                 (isAllied && !isSuccess && isGoodEvent);

        if (isRansom) {
            if (isAllied) {
                buttons.add(getFormattedTextAt(RESOURCE_BUNDLE, "decline.button"));
            }

            buttons.add(getFormattedTextAt(RESOURCE_BUNDLE, "accept.button"));
        }

        if (!isAllied) {
            buttons.add(getFormattedTextAt(RESOURCE_BUNDLE, "releaseThem.button"));
            buttons.add(getFormattedTextAt(RESOURCE_BUNDLE, "executeThem.button"));
        }

        if (isAllied && !isRansom) {
            buttons.add(getFormattedTextAt(RESOURCE_BUNDLE, "successful.button"));
        }

        return buttons;
    }

    /**
     * Determines the likelihood of an end of contract event being classified as a "good event" based on mission
     * success, prisoner allegiance, and contextual factors such as recent crimes.
     *
     * <p>If the prisoners are allied POWs, the method evaluates whether recent crimes have occurred after the start
     * of the current contract or mission. If such crimes exist, the chance of a "good event" is adjusted downward based
     * on the crime rating. For non-allied prisoners or situations without recent crimes, a default chance value is
     * used.</p>
     *
     * @param isAllied {@code true} if the prisoners are allied POWs, {@code false} otherwise.
     *
     * @return the likelihood of a "good event" as an integer value, where higher values indicate a greater chance.
     */
    int determineGoodEventChance(boolean isAllied) {
        if (isAllied) {
            LocalDate lastCrime = campaign.getDateOfLastCrime();
            LocalDate startDate = getContractOrMissionStartDate();

            if ((startDate != null) && (lastCrime != null)) {
                if (lastCrime.isAfter(startDate)) {
                    // Adjust the chance of a good event based on crime rating
                    return max(1, GOOD_EVENT_CHANCE - campaign.getAdjustedCrimeRating());
                }
            }
        }
        // Default chance for goodEvent (used for both non-allied and no recent crimes in allied)
        return GOOD_EVENT_CHANCE;
    }

    /**
     * Retrieves the earliest starting date from either the active contract or completed mission scenarios.
     *
     * <p>The method determines the start date of the current contract or mission by examining the contract's
     * start date or the earliest date among completed scenarios. Each start date is adjusted slightly earlier by one
     * day.</p>
     *
     * @return the start date of the contract or mission as a {@link LocalDate}, or {@code null} if no valid date is
     *       found.
     */
    private LocalDate getContractOrMissionStartDate() {
        LocalDate startDate = null;
        if (mission instanceof Contract) {
            startDate = ((Contract) mission).getStartDate();
        } else {
            for (Scenario scenario : mission.getCompletedScenarios()) {
                LocalDate scenarioDate = scenario.getDate();

                if (startDate == null) {
                    startDate = scenarioDate;
                    continue;
                }

                if (scenarioDate.isBefore(startDate)) {
                    startDate = scenarioDate;
                }
            }
        }

        if (startDate == null) {
            return null;
        }

        return startDate.minusDays(1);
    }

    /**
     * Calculates the total ransom amount for a list of prisoners.
     *
     * @param alliedPoWs The list of prisoners for whom the ransom is calculated.
     *
     * @return The total ransom as {@link Money}.
     */
    Money getRansom(List<Person> alliedPoWs) {
        Money alliedRansom = Money.zero();

        for (Person person : alliedPoWs) {
            Money ransomValue = person.getRansomValue(campaign);
            alliedRansom = alliedRansom.plus(ransomValue);
        }

        return alliedRansom;
    }

    /**
     * Processes the player's response from the prisoner-handling dialog.
     *
     * <p>Based on the player's choice, this method applies appropriate actions, including
     * releasing, executing, or ransoming prisoners. It also modifies prisoner status and manages any associated
     * financial transactions.</p>
     *
     * @param ransom      The calculated ransom amount.
     * @param isGoodEvent {@code true} if the event is classified as positive, {@code false} otherwise.
     * @param choiceIndex The player's choice index from the dialog.
     * @param prisoners   The list of prisoners involved in the event.
     */
    private void processPlayerResponse(Money ransom, boolean isGoodEvent, int choiceIndex, List<Person> prisoners) {
        if (choiceIndex == CHOICE_RELEASE_THEM) {
            removeAllPrisoners(prisoners);
        }

        if (choiceIndex == CHOICE_EXECUTE_THEM) {
            removeAllPrisoners(prisoners);
            executePrisoners(prisoners);
        }

        final LocalDate today = campaign.getLocalDate();

        if (isAllied && isSuccess && isGoodEvent) {
            changeStatusOfAllPrisoners(prisoners, today, ACTIVE);
            return;
        }

        // Your IDE is going to tell you that some of these conditions can be removed as they're
        // always true. That's correct, but they should be left in place as that makes this
        // sequence much easier to follow.
        if (isAllied && isSuccess && !isGoodEvent) {
            changeStatusOfAllPrisoners(prisoners, today, HOMICIDE);
            return;
        }

        if (isAllied && !isSuccess && isGoodEvent) {
            boolean isAccepted = choiceIndex == CHOICE_ACCEPTED;
            if (isAccepted) {
                performRansom(false, ransom, today);
            }

            changeStatusOfAllPrisoners(prisoners, today, isAccepted ? ACTIVE : LEFT);
            return;
        }

        if (isAllied && !isSuccess && !isGoodEvent) {
            changeStatusOfAllPrisoners(prisoners, today, HOMICIDE);
            return;
        }

        if (!isAllied && isSuccess && isGoodEvent) {
            if (choiceIndex == CHOICE_ACCEPTED) {
                performRansom(true, ransom, today);
                removeAllPrisoners(prisoners);
            }
            return;
        }

        if (!isAllied && isSuccess && !isGoodEvent) {
            if (choiceIndex == CHOICE_ACCEPTED) {
                removeAllPrisoners(prisoners);
            }
            return;
        }

        if (!isAllied && !isSuccess && isGoodEvent) {
            if (choiceIndex == CHOICE_ACCEPTED) {
                performRansom(true, ransom, today);
                removeAllPrisoners(prisoners);
            }
            return;
        }

        if (!isAllied && !isSuccess && !isGoodEvent) {
            if (choiceIndex == CHOICE_ACCEPTED) {
                removeAllPrisoners(prisoners);
            }
        }
    }

    /**
     * Changes the status of all prisoners involved in the event.
     *
     * @param prisoners The list of prisoners whose status will be updated.
     * @param today     The current date, representing when the status change occurs.
     * @param newStatus The new {@link PersonnelStatus} to be applied to the prisoners.
     */
    private void changeStatusOfAllPrisoners(List<Person> prisoners, LocalDate today, PersonnelStatus newStatus) {
        for (Person prisoner : prisoners) {
            prisoner.changeStatus(campaign, today, newStatus);
        }
    }

    /**
     * Performs a financial transaction for a ransom, either crediting or debiting funds.
     *
     * @param isCredit {@code true} if the ransom results in funds being credited to the player, {@code false} if
     *                 debited.
     * @param ransom   The ransom amount being transacted.
     * @param today    The current campaign date for the transaction record.
     */
    private void performRansom(boolean isCredit, Money ransom, LocalDate today) {
        if (isCredit) {
            campaign.getFinances()
                  .credit(RANSOM, today, ransom, getFormattedTextAt(RESOURCE_BUNDLE, "transaction.ransom"));
        } else {
            // That this can take a player into negative is a deliberate decision. As this dialog
            // is only presented after the point of no return, we don't want the player left in a
            // situation where 1 C-Bill locks them out of ransoming back their people.
            campaign.getFinances()
                  .debit(RANSOM, today, ransom, getFormattedTextAt(RESOURCE_BUNDLE, "transaction.ransom"));
        }
    }

    /**
     * Removes all prisoners involved in the event from the campaign.
     *
     * @param prisoners The list of prisoners to be removed.
     */
    private void removeAllPrisoners(List<Person> prisoners) {
        for (Person prisoner : prisoners) {
            campaign.removePerson(prisoner);
        }
    }

    /**
     * Executes prisoners involved in the event and applies related penalties or notifications.
     *
     * <p>Depending on the severity of the execution, this method adjusts the campaign's crime
     * rating, generates reports, and determines whether the crime was noticed based on a random roll.</p>
     *
     * @param prisoners The list of prisoners to be executed.
     */
    private void executePrisoners(List<Person> prisoners) {
        // Was the crime noticed?
        int crimeNoticeRoll = randomInt(100);
        boolean crimeNoticed = crimeNoticeRoll < prisoners.size();

        int penalty = min(MAX_CRIME_PENALTY, prisoners.size() * 2);
        if (crimeNoticed && campaign.getCampaignOptions().getUnitRatingMethod().isCampaignOperations()) {
            campaign.changeCrimeRating(-penalty);
            campaign.setDateOfLastCrime(campaign.getLocalDate());
        }

        // Build the report
        String crimeColor = crimeNoticed ?
                                  spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()) :
                                  spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor());

        String crimeMessage = crimeNoticed ?
                                    getFormattedTextAt(RESOURCE_BUNDLE,
                                          "execute.crimeNoticed",
                                          crimeColor,
                                          CLOSING_SPAN_TAG,
                                          penalty) :
                                    getFormattedTextAt(RESOURCE_BUNDLE,
                                          "execute.crimeUnnoticed",
                                          crimeColor,
                                          CLOSING_SPAN_TAG);

        // Add the report
        campaign.addReport(crimeMessage);
    }
}
