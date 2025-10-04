/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.stratCon;

import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.enums.SkillTypeNew;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.utilities.ReportingUtilities;

/**
 * This class handles Support Point negotiations for StratCon.
 * <p>
 * It includes functionality to negotiate both initial and weekly support points for contracts, based on the skill
 * levels of available Admin/Transport personnel.
 *
 * <p>The workflow includes:</p>
 * <ul>
 *     <li>Filtering and sorting Admin/Transport personnel by their skill levels.</li>
 *     <li>Negotiating support points for either a single contract (initial negotiation) or all
 *     active contracts (weekly negotiation).</li>
 *     <li>Calculating support points based on dice rolls and personnel skill levels.</li>
 *     <li>Generating appropriate campaign reports reflecting the success or failure of negotiations.</li>
 * </ul>
 */
public class SupportPointNegotiation {
    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.AtBStratCon",
          MekHQ.getMHQOptions().getLocale());

    /**
     * Negotiates weekly additional support points for all active AtB contracts.
     *
     * <p>Uses available Admin/Transport personnel to negotiate support points for contracts, with older contracts
     * being processed first. Personnel are removed from the available pool as they are assigned to contracts. If no
     * Admin/Transport personnel are available, an error report is generated, and the method exits early.</p>
     *
     * <p>Calculated support points are added to the contract if successful, and reports detailing the
     * outcome are appended to the campaign reports.</p>
     *
     * @param campaign The {@link Campaign} instance managing the current game state.
     */
    public static void negotiateAdditionalSupportPoints(Campaign campaign) {
        // Fetch all active contracts and sort them by start date (oldest -> newest)
        List<AtBContract> activeContracts = campaign.getActiveAtBContracts();

        if (activeContracts.isEmpty()) {
            return;
        }

        List<AtBContract> sortedContracts = getSortedContractsByStartDate(activeContracts);

        // Get sorted Admin/Transport personnel
        List<Person> adminTransport = getSortedAdminTransportPersonnel(campaign);

        // If no Admin/Transport personnel, exit early
        if (adminTransport.isEmpty()) {
            addReportNoPersonnel(campaign, null);
            return;
        }

        // Iterate over contracts and negotiate support points
        for (AtBContract contract : sortedContracts) {
            if (adminTransport.isEmpty()) {
                break;
            }

            processContractSupportPoints(campaign, contract, adminTransport, false);
        }
    }

    /**
     * Negotiates initial support points for a specific AtB contract.
     *
     * <p>This method processes a single contract and uses available Admin/Transport personnel to negotiate
     * support points. If no Admin/Transport personnel are available, an error report is generated, and the method exits
     * early.</p>
     *
     * <p>Calculated support points are added to the contract if successful, and a report detailing the
     * outcome is appended to the campaign reports.</p>
     *
     * @param campaign The {@link Campaign} instance managing the current game state.
     * @param contract The {@link AtBContract} instance representing the contract for which initial support points are
     *                 being negotiated.
     */
    public static void negotiateInitialSupportPoints(Campaign campaign, AtBContract contract) {
        // Get sorted Admin/Transport personnel
        List<Person> adminTransport = getSortedAdminTransportPersonnel(campaign);

        // If no Admin/Transport personnel, exit early
        if (adminTransport.isEmpty()) {
            addReportNoPersonnel(campaign, contract);
            return;
        }

        // Negotiate support points for the specific contract
        processContractSupportPoints(campaign, contract, adminTransport, true);
    }

    /**
     * Processes the negotiation of support points for a given AtB contract.
     *
     * <p>Rolls dice for assigned personnel to determine successful negotiations. Support points
     * are calculated based on skill levels and the success of the dice rolls. Personnel are removed from the pool once
     * assigned, and support points are added to the contract if successfully negotiated.</p>
     *
     * @param campaign             The {@link Campaign} instance managing the current game state.
     * @param contract             The {@link AtBContract} instance for which support points are being processed.
     * @param adminTransport       A {@link List} of available {@link Person} objects representing Admin/Transport
     *                             personnel.
     * @param isInitialNegotiation {@code true} if the negotiation took place at the beginning of the contract,
     *                             otherwise {@code false}
     */
    private static void processContractSupportPoints(Campaign campaign, AtBContract contract,
          List<Person> adminTransport, boolean isInitialNegotiation) {
        int negotiatedSupportPoints = 0;
        int maxSupportPoints = isInitialNegotiation ?
                                     contract.getRequiredCombatTeams() * 3 :
                                     contract.getRequiredCombatTeams();

        FactionStandings factionStandings = campaign.getFactionStandings();
        double regard = factionStandings.getRegardForFaction(contract.getEmployerCode(), true);
        boolean isUseFactionStandingSupportPoints = campaign.getCampaignOptions()
                                                          .isUseFactionStandingSupportPointsSafe();

        StratConCampaignState campaignState = contract.getStratconCampaignState();

        if (campaignState == null) {
            return;
        }

        int currentSupportPoints = campaignState.getSupportPoints();

        if (currentSupportPoints >= maxSupportPoints) {
            String pluralizer = (maxSupportPoints > 1) || (maxSupportPoints == 0) ? "s" : "";

            campaign.addReport(String.format(resources.getString("supportPoints.maximum"),
                  contract.getHyperlinkedName(),
                  spanOpeningWithCustomColor(ReportingUtilities.getWarningColor()),
                  CLOSING_SPAN_TAG,
                  maxSupportPoints,
                  pluralizer));

            return;
        }

        int modifier = 0;
        if (!isInitialNegotiation && isUseFactionStandingSupportPoints) {
            modifier = FactionStandingUtilities.getSupportPointModifierPeriodic(regard);
        }

        Iterator<Person> iterator = adminTransport.iterator();

        while (iterator.hasNext() && ((negotiatedSupportPoints + currentSupportPoints) < maxSupportPoints)) {
            Person admin = iterator.next();
            int rollResult = Compute.d6(2) + modifier;

            int adminSkill = admin.getSkill(SkillTypeNew.S_ADMIN.name())
                                   .getFinalSkillValue(admin.getOptions(), admin.getATOWAttributes());
            if (rollResult >= adminSkill) {
                negotiatedSupportPoints++;
            }
            iterator.remove();
        }

        if (isInitialNegotiation && isUseFactionStandingSupportPoints) {
            int multiplier = contract.getRequiredCombatTeams();
            negotiatedSupportPoints += FactionStandingUtilities.getSupportPointModifierContractStart(regard) *
                                             multiplier;
        }

        // Determine font color based on success or failure
        String fontColor = (negotiatedSupportPoints > 0) ?
                                 ReportingUtilities.getPositiveColor() :
                                 ReportingUtilities.getNegativeColor();

        // Add points to the contract if positive
        if (negotiatedSupportPoints > 0) {
            campaignState.changeSupportPoints(negotiatedSupportPoints);
        }

        // Add a report
        String pluralizer = (negotiatedSupportPoints > 1) || (negotiatedSupportPoints == 0) ? "s" : "";
        if (isInitialNegotiation) {
            campaign.addReport(String.format(resources.getString("supportPoints.initial"),
                  contract.getHyperlinkedName(),
                  spanOpeningWithCustomColor(fontColor),
                  negotiatedSupportPoints,
                  CLOSING_SPAN_TAG,
                  pluralizer));
        } else {
            campaign.addReport(String.format(resources.getString("supportPoints.weekly"),
                  spanOpeningWithCustomColor(fontColor),
                  negotiatedSupportPoints,
                  CLOSING_SPAN_TAG,
                  pluralizer,
                  contract.getHyperlinkedName()));
        }
    }

    /**
     * Filters and sorts Admin/Transport personnel from the campaign by their skill levels in descending order.
     *
     * @param campaign The {@link Campaign} instance containing personnel to be filtered and sorted.
     *
     * @return A {@link List} of {@link Person} objects representing Admin/Transport personnel, sorted by skill.
     */
    private static List<Person> getSortedAdminTransportPersonnel(Campaign campaign) {
        List<Person> adminTransport = new ArrayList<>();
        for (Person person : campaign.getAdmins()) {
            if (person.getPrimaryRole().isAdministratorTransport() ||
                      person.getSecondaryRole().isAdministratorTransport()) {
                // Each character gets to roll three times, so we add them to the list three times.
                adminTransport.add(person);
                adminTransport.add(person);
                adminTransport.add(person);
            }
        }

        boolean isUseAgingEffects = campaign.getCampaignOptions().isUseAgeEffects();
        boolean isClanCampaign = campaign.isClanCampaign();
        LocalDate today = campaign.getLocalDate();
        adminTransport.sort((p1, p2) -> Integer.compare(getSkillValue(p2,
              isUseAgingEffects,
              isClanCampaign,
              today,
              p2.getRankNumeric()), getSkillValue(p1, isUseAgingEffects, isClanCampaign, today, p1.getRankNumeric())));
        return adminTransport;
    }

    /**
     * Sorts all active AtB contracts by their start date in ascending order.
     *
     * @return A {@link List} of {@link AtBContract} instances, sorted by start date.
     */
    private static List<AtBContract> getSortedContractsByStartDate(List<AtBContract> activeContracts) {
        activeContracts.sort(Comparator.comparing(AtBContract::getStartDate));
        return activeContracts;
    }

    /**
     * Adds a report to the campaign log indicating the absence of Admin/Transport personnel for support point
     * negotiations.
     *
     * <p>If a contract is specified, the report is related to that contract. Otherwise, the report is general
     * (e.g., for weekly negotiations).</p>
     *
     * @param campaign The {@link Campaign} instance managing the current game state.
     * @param contract An optional {@link AtBContract} instance representing the affected contract (can be
     *                 {@code null}).
     */
    private static void addReportNoPersonnel(Campaign campaign, @Nullable AtBContract contract) {
        String reportKey = String.format("supportPoints.%s.noAdministrators", contract == null ? "weekly" : "initial");

        if (contract == null) {
            campaign.addReport(String.format(resources.getString(reportKey),
                  spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                  CLOSING_SPAN_TAG));
        } else {
            campaign.addReport(String.format(resources.getString(reportKey),
                  contract.getHyperlinkedName(),
                  spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                  CLOSING_SPAN_TAG));
        }
    }

    /**
     * Calculates the total skill value for a given person by combining their skill level and relevant bonuses.
     *
     * <p>This method retrieves the specified skill from the person and computes the total skill level,
     * taking into account the person's options, attributes, and adjusted reputation (which itself can be influenced by
     * aging effects, campaign type, the current date, and rank index).</p>
     *
     * @param person            The {@link Person} whose skill value is being calculated.
     * @param isUseAgingEffects Whether to apply aging effects to the reputation calculation.
     * @param isClanCampaign    Indicates whether the current campaign is a Clan campaign.
     * @param today             The current in-game date for age/reputation calculations.
     * @param rankIndex         The index representing the current rank for modifiers.
     *
     * @return An {@link Integer} representing the total skill value after modifiers.
     */
    private static int getSkillValue(Person person, boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today,
          int rankIndex) {
        Skill skill = person.getSkill(SkillTypeNew.S_ADMIN.name());
        return skill.getTotalSkillLevel(person.getOptions(),
              person.getATOWAttributes(),
              person.getAdjustedReputation(isUseAgingEffects, isClanCampaign, today, rankIndex));
    }
}
