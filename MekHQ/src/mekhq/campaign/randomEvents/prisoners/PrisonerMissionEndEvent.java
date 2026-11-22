/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.enums.DailyReportType.POLITICS;
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

import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.reputation.chaosReputation.ChaosReputation;
import mekhq.campaign.universe.factionStanding.FactionStandings;
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
    private final AbstractContract mission;

    final static int GOOD_EVENT_CHANCE = 20;

    /**
     * The responses offered by the end-of-contract prisoner dialog. Each button is built from one of these, so the
     * player's choice maps straight back to its meaning, whichever buttons a given outcome shows.
     */
    enum EndOfContractResponse {
        DECLINE_RANSOM("decline.button"),
        ACCEPT_RANSOM("accept.button"),
        RELEASE("releaseThem.button"),
        EXECUTE("executeThem.button"),
        ACKNOWLEDGE("successful.button");

        private final String buttonKey;

        EndOfContractResponse(String buttonKey) {
            this.buttonKey = buttonKey;
        }

        String getButtonLabel() {
            return getFormattedTextAt(RESOURCE_BUNDLE, buttonKey);
        }
    }

    /**
     * Creates a new instance of `PrisonerMissionEndEvent` to handle mission-end prisoner events.
     *
     * @param campaign The current campaign instance, providing context and data about prisoners and finances.
     * @param mission  The current mission related to this event.
     */
    public PrisonerMissionEndEvent(Campaign campaign, AbstractContract mission) {
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
              campaign.getPlayerForce().getHumanResources()
                    .getSeniorAdminPerson(campaign.getCampaignOptions(),
                          campaign.getPlayerForce().isClanForce(),
                          campaign.getLocalDate()),
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
        List<Person> prisoners;
        prisoners = isAllied ?
                          campaign.getPlayerForce().getHumanResources().getFriendlyPrisoners() :
                          campaign.getPlayerForce().getHumanResources().getCurrentPrisoners();
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

        List<EndOfContractResponse> responses = getEndOfContractResponses(isAllied, isSuccess, isGoodEvent);
        List<String> buttons = responses.stream().map(EndOfContractResponse::getButtonLabel).toList();

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              campaign.getPlayerForce().getHumanResources()
                    .getSeniorAdminPerson(campaign.getCampaignOptions(),
                          campaign.getPlayerForce().isClanForce(),
                          campaign.getLocalDate()),
              null,
              inCharacterMessage,
              buttons,
              outOfCharacterMessage,
              null,
              false);

        int choiceIndex = dialog.getDialogChoice();
        EndOfContractResponse response = (choiceIndex >= 0 && choiceIndex < responses.size()) ?
                                               responses.get(choiceIndex) :
                                               responses.getFirst();

        processPlayerResponse(isAllied, isSuccess, isGoodEvent, ransom, response, prisoners);
    }

    /**
     * Builds the responses offered by the dialog, in button order.
     *
     * <p>The available responses depend on the ownership (allied/enemy), mission outcome, and the type of event. A
     * ransom is offered for enemy prisoners on a good event, and for allied prisoners on a good event after a failed
     * mission. Enemy prisoners can always be released or executed.</p>
     *
     * @param isAllied    Indicates whether the prisoners are allied.
     * @param isSuccess   Indicates whether the mission was successful.
     * @param isGoodEvent Indicates whether the event is positive.
     *
     * @return the responses, one per button, in the order they are displayed
     */
    static List<EndOfContractResponse> getEndOfContractResponses(boolean isAllied, boolean isSuccess,
          boolean isGoodEvent) {
        List<EndOfContractResponse> responses = new ArrayList<>();

        boolean isRansom = isGoodEvent && (!isAllied || !isSuccess);

        if (isRansom) {
            if (isAllied) {
                responses.add(EndOfContractResponse.DECLINE_RANSOM);
            }

            responses.add(EndOfContractResponse.ACCEPT_RANSOM);
        }

        if (!isAllied) {
            responses.add(EndOfContractResponse.RELEASE);
            responses.add(EndOfContractResponse.EXECUTE);
        }

        if (isAllied && !isRansom) {
            responses.add(EndOfContractResponse.ACKNOWLEDGE);
        }

        return responses;
    }

    /**
     * Determines the likelihood of an end of contract event being classified as a "good event" based on mission
     * success, prisoner allegiance, and contextual factors such as recent crimes.
     *
     * <p>If the prisoners are allied POWs, the method evaluates whether recent crimes have occurred after the start
     * of the current contract or mission. If such crimes exist, the chance of a "good event" is adjusted downward based
     * on the crime rating, which is never positive: each point of crime removes one side from the die. For non-allied
     * prisoners or situations without recent crimes, a default chance value is used.</p>
     *
     * @param isAllied {@code true} if the prisoners are allied POWs, {@code false} otherwise.
     *
     * @return the likelihood of a "good event" as an integer value, where higher values indicate a greater chance.
     */
    int determineGoodEventChance(boolean isAllied) {
        if (isAllied) {
            LocalDate lastCrime = campaign.getPlayerForce().getCampOpsDateOfLastCrime();
            LocalDate startDate = getContractOrMissionStartDate();

            if ((startDate != null) && (lastCrime != null)) {
                if (lastCrime.isAfter(startDate)) {
                    // Adjust the chance of a good event based on crime rating
                    int crimeRating = min(0, campaign.getPlayerForce().getAdjustedCrimeRating());
                    return max(1, GOOD_EVENT_CHANCE + crimeRating);
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
    private @Nullable LocalDate getContractOrMissionStartDate() {
        LocalDate startDate = mission.getStartDate();
        return (startDate == null) ? null : startDate.minusDays(1);
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
     * @param isAllied    {@code true} if the prisoners are allied POWs, {@code false} if they belong to the enemy.
     * @param isSuccess   {@code true} if the mission was a success, {@code false} otherwise.
     * @param isGoodEvent {@code true} if the event is classified as positive, {@code false} otherwise.
     * @param ransom      The calculated ransom amount.
     * @param response    The response the player chose.
     * @param prisoners   The list of prisoners involved in the event.
     */
    void processPlayerResponse(boolean isAllied, boolean isSuccess, boolean isGoodEvent, Money ransom,
          EndOfContractResponse response, List<Person> prisoners) {
        if (response == EndOfContractResponse.RELEASE) {
            removeAllPrisoners(prisoners);
            return;
        }

        if (response == EndOfContractResponse.EXECUTE) {
            if (campaign.getCampaignOptions().get(CampaignOption.TRACK_FACTION_STANDING)) {
                FactionStandings factionStandings = campaign.getPlayerForce().getFactionStandings();

                List<String> reports = factionStandings.executePrisonersOfWar(
                      campaign.getPlayerForce().getFaction().getShortName(),
                      prisoners,
                      campaign.getGameYear(),
                      campaign.getCampaignOptions().get(CampaignOption.REGARD_MULTIPLIER));

                for (String report : reports) {
                    campaign.addReport(POLITICS, report);
                }
            }

            executePrisoners(prisoners);
            removeAllPrisoners(prisoners);
            return;
        }

        final LocalDate today = campaign.getLocalDate();

        if (isAllied) {
            if (!isGoodEvent) {
                changeStatusOfAllPrisoners(prisoners, today, HOMICIDE);
                return;
            }

            if (isSuccess) {
                changeStatusOfAllPrisoners(prisoners, today, ACTIVE);
                return;
            }

            // A good event after a failed mission: the enemy offers to ransom them back
            boolean isAccepted = response == EndOfContractResponse.ACCEPT_RANSOM;
            if (isAccepted) {
                performRansom(false, ransom, today);
            }

            changeStatusOfAllPrisoners(prisoners, today, isAccepted ? ACTIVE : LEFT);
            return;
        }

        // Enemy prisoners: releasing and executing are handled above, which leaves accepting a ransom
        if (response == EndOfContractResponse.ACCEPT_RANSOM) {
            performRansom(true, ransom, today);
            removeAllPrisoners(prisoners);
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
    void performRansom(boolean isCredit, Money ransom, LocalDate today) {
        if (isCredit) {
            campaign.getPlayerForce().getFinances()
                  .credit(RANSOM, today, ransom, getFormattedTextAt(RESOURCE_BUNDLE, "transaction.ransom"));
        } else {
            // That this can take a player into negative is a deliberate decision. As this dialog
            // is only presented after the point of no return, we don't want the player left in a
            // situation where 1 C-Bill locks them out of ransoming back their people.
            campaign.getPlayerForce().getFinances()
                  .debit(RANSOM, today, ransom, getFormattedTextAt(RESOURCE_BUNDLE, "transaction.ransom"));
        }
    }

    /**
     * Removes all prisoners involved in the event from the campaign.
     *
     * @param prisoners The list of prisoners to be removed.
     */
    void removeAllPrisoners(List<Person> prisoners) {
        for (Person prisoner : prisoners) {
            campaign.getPlayerForce().getHumanResources().removePerson(campaign, prisoner);
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
    void executePrisoners(List<Person> prisoners) {
        // Was the crime noticed?
        int crimeNoticeRoll = randomInt(100);
        boolean crimeNoticed = crimeNoticeRoll < prisoners.size();
        int penalty;

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isUseChaosReputation = campaignOptions.get(CampaignOption.USE_CHAOS_REPUTATION);
        boolean isUseCampaignReputationTracking = campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION);
        if (isUseChaosReputation) {
            penalty = ChaosReputation.getPrisonerExecutionPenalty(campaign.getPlayerForce(),
                  prisoners.size(),
                  crimeNoticed,
                  isUseCampaignReputationTracking);
        } else {
            penalty = min(MAX_CRIME_PENALTY, prisoners.size() * 2);
            if (crimeNoticed) {
                int change = -penalty;
                campaign.getPlayerForce().changeCrimeRating(change);
                LocalDate dateOfLastCrime = campaign.getLocalDate();
                campaign.getPlayerForce().setCampOpsDateOfLastCrime(dateOfLastCrime);
            }
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
        campaign.addReport(GENERAL, crimeMessage);
    }
}
