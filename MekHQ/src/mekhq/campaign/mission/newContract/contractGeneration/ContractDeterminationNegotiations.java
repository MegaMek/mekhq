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
package mekhq.campaign.mission.newContract.contractGeneration;

import static java.lang.Math.floor;
import static mekhq.campaign.personnel.PersonnelOptions.EDGE_COMMANDER_NEGOTIATION;
import static mekhq.campaign.personnel.skills.SkillType.S_NEGOTIATION;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import mekhq.campaign.Campaign;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.SkillCheck;

/**
 * Negotiates a contract's terms (command rights and salvage/support/transport shares) through opposed negotiation skill
 * checks between the player's and employer's negotiators, rolling on the CamOps terms tables. Pirate contracts use
 * fixed terms and skip negotiation. This is a static utility class and is not instantiable.
 */
public class ContractDeterminationNegotiations {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractDeterminationNegotiations";

    private final static String NEGOTIATION_CHECK_REASON = getTextAt(RESOURCE_BUNDLE,
          "ContractDeterminationNegotiations.skillCheck.reason");

    /** CamOps pg 41 rev 5th printing */
    private final static double MARGIN_OF_SUCCESS_DIVIDER = 2.0;
    /** CamOps pg 41 rev 5th printing */
    private final static double PIRATE_SALVAGE_TERMS = 1.0;
    /** CamOps pg 41 rev 5th printing */
    private final static ContractCommandRights PIRATE_COMMAND_RIGHTS = ContractCommandRights.INDEPENDENT;
    /** CamOps pg 41 rev 5th printing */
    private final static double PIRATE_TRANSPORT_RIGHTS = 0.0;
    /** CamOps pg 41 rev 5th printing */
    private final static double PIRATE_SUPPORT_RIGHTS = 0.0;

    private ContractDeterminationNegotiations() {}

    /**
     * Negotiates the initial terms of a contract. Pirate campaigns receive fixed terms; all others negotiate every
     * clause via opposed skill checks.
     *
     * @param playerNegotiator     the player's negotiator
     * @param employerNegotiator   the employer's negotiator
     * @param campaign             the current campaign
     * @param campaignType         the campaign type, which selects the pirate or standard negotiation path
     * @param employerModifierData the accumulated employer negotiation modifiers
     *
     * @return the negotiated contract terms
     */
    public static NegotiationsData negotiateInitialContractTerms(Person playerNegotiator, Person employerNegotiator,
          Campaign campaign, CampaignTypeForContractDetermination campaignType,
          EmployerModifierData employerModifierData) {
        boolean isPirateCampaignType = campaignType == CampaignTypeForContractDetermination.PIRATE;
        if (isPirateCampaignType) {
            return determinePirateNegotiationTerms();
        }

        return determineInitialNegotiationTerms(employerModifierData,
              campaign,
              playerNegotiator,
              employerNegotiator);
    }

    /**
     * Renegotiates a single clause of an existing contract, rolling that clause's table afresh and returning updated
     * terms.
     *
     * @param employerModifierData the accumulated employer negotiation modifiers
     * @param playerNegotiator     the player's negotiator
     * @param employerNegotiator   the employer's negotiator
     * @param campaign             the current campaign
     * @param negotiationsData     the existing contract terms
     * @param clause               the clause to renegotiate
     *
     * @return the contract terms with the renegotiated clause updated
     */
    public static NegotiationsData renegotiateContractTerm(EmployerModifierData employerModifierData,
          Person playerNegotiator, Person employerNegotiator, Campaign campaign, NegotiationsData negotiationsData,
          ContractNegotiationClause clause) {
        if (clause == ContractNegotiationClause.COMMAND_RIGHTS) {
            ContractCommandRights commandRights = negotiateCommandRightsClause(employerModifierData,
                  campaign,
                  playerNegotiator,
                  employerNegotiator);
            return negotiationsData.updateClause(clause, commandRights);
        }

        double newValue = negotiateContractClause(employerModifierData, campaign,
              playerNegotiator, employerNegotiator, clause);
        return negotiationsData.updateClause(clause, newValue);
    }

    private static NegotiationsData determinePirateNegotiationTerms() {
        return new NegotiationsData(PIRATE_COMMAND_RIGHTS,
              PIRATE_SALVAGE_TERMS,
              PIRATE_SUPPORT_RIGHTS,
              PIRATE_TRANSPORT_RIGHTS);
    }

    /**
     * Negotiates every contract clause in turn, rolling each clause's table, and assembles the results into a full set
     * of terms. Used for the non-pirate negotiation path.
     *
     * @param employerModifierData the accumulated employer negotiation modifiers
     * @param campaign             the current campaign
     * @param playerNegotiator     the player's negotiator
     * @param employerNegotiator   the employer's negotiator
     *
     * @return the fully negotiated contract terms
     */
    public static NegotiationsData determineInitialNegotiationTerms(EmployerModifierData employerModifierData,
          Campaign campaign, Person playerNegotiator, Person employerNegotiator) {
        ContractCommandRights commandRights = ContractCommandRights.HOUSE;
        double salvageRights = 0.0;
        double supportRights = 0.0;
        double transportRights = 0.0;

        for (ContractNegotiationClause clause : ContractNegotiationClause.values()) {
            switch (clause) {
                case COMMAND_RIGHTS -> commandRights = negotiateCommandRightsClause(employerModifierData,
                      campaign,
                      playerNegotiator,
                      employerNegotiator);
                case SALVAGE_RIGHTS -> salvageRights = negotiateContractClause(employerModifierData, campaign,
                      playerNegotiator, employerNegotiator, clause);
                case SUPPORT_RIGHTS -> supportRights = negotiateContractClause(employerModifierData, campaign,
                      playerNegotiator, employerNegotiator, clause);
                case TRANSPORT_RIGHTS -> transportRights = negotiateContractClause(employerModifierData, campaign,
                      playerNegotiator, employerNegotiator, clause);
            }
            ;
        }

        return new NegotiationsData(commandRights, salvageRights, supportRights, transportRights);
    }

    private static double negotiateContractClause(EmployerModifierData employerModifierData, Campaign campaign,
          Person playerNegotiator, Person employerNegotiator, ContractNegotiationClause clause) {
        int commandTransportNegotiationModifier = makeOpposedNegotiationSkillCheck(campaign,
              playerNegotiator,
              employerNegotiator);

        return switch (clause) {
            case SALVAGE_RIGHTS -> NegotiationTermsTables.rollOnSalvageRightsTable(commandTransportNegotiationModifier,
                  employerModifierData.getSalvageModifier());
            case SUPPORT_RIGHTS -> NegotiationTermsTables.rollOnSupportRightsTable(commandTransportNegotiationModifier,
                  employerModifierData.getSupportModifier());
            case TRANSPORT_RIGHTS -> NegotiationTermsTables.rollOnTransportRightsTable(
                  commandTransportNegotiationModifier,
                  employerModifierData.getTransportModifier());
            default -> throw new IllegalStateException("Unexpected value: " + clause);
        };
    }

    private static ContractCommandRights negotiateCommandRightsClause(EmployerModifierData employerModifierData,
          Campaign campaign, Person playerNegotiator, Person employerNegotiator) {
        int commandTransportNegotiationModifier = makeOpposedNegotiationSkillCheck(campaign,
              playerNegotiator,
              employerNegotiator);

        return NegotiationTermsTables.rollOnCommandRightsTable(commandTransportNegotiationModifier,
              employerModifierData.getCommandModifier());
    }

    private static int makeOpposedNegotiationSkillCheck(Campaign campaign, Person playerNegotiator,
          Person employerNegotiator) {
        String skillName = S_NEGOTIATION;

        int playerMarginOfSuccess = performNegotiationCheck(campaign, playerNegotiator, skillName, false);
        int employerMarginOfSuccess = performNegotiationCheck(campaign, employerNegotiator, skillName, true);

        return getResultsOfOpposedSkillCheck(campaign, playerMarginOfSuccess, employerMarginOfSuccess);
    }

    private static int performNegotiationCheck(Campaign campaign, Person negotiator, String skillName,
          boolean isEmployerNegotiator) {
        // TODO replace with actual edge option, current is just a placeholder
        boolean useEdge = isEmployerNegotiator ||
                                negotiator.getOptions().booleanOption(EDGE_COMMANDER_NEGOTIATION);

        SkillCheck playerNegotiatorSkillCheck = negotiator.checkSkill(skillName, campaign);

        ActionCheckResult playerResult = playerNegotiatorSkillCheck.resolve(useEdge, NEGOTIATION_CHECK_REASON);
        int playerMarginOfSuccess = playerResult.getMarginOfSuccess();

        campaign.addReport(DailyReportType.SKILL_CHECKS, playerResult.getReport(true));

        return playerMarginOfSuccess;
    }

    private static int getResultsOfOpposedSkillCheck(Campaign campaign, int playerMarginOfSuccess,
          int employerMarginOfSuccess) {
        int totalMarginOfSuccess = playerMarginOfSuccess + employerMarginOfSuccess;
        totalMarginOfSuccess = (int) floor(totalMarginOfSuccess / MARGIN_OF_SUCCESS_DIVIDER);

        String report = getFormattedTextAt(RESOURCE_BUNDLE, "ContractDeterminationNegotiations.skillCheck.results",
              totalMarginOfSuccess);
        campaign.addReport(DailyReportType.SKILL_CHECKS, report);

        return totalMarginOfSuccess;
    }
}
