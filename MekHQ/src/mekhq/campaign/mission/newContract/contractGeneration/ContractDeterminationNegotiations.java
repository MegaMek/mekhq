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
import static mekhq.campaign.personnel.skills.SkillType.S_INVESTIGATION;
import static mekhq.campaign.personnel.skills.SkillType.S_NEGOTIATION;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import mekhq.campaign.Campaign;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.SkillCheck;

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

    public ContractDeterminationNegotiations() {
        // TODO - roll 2d6 per term
        // TODO player negotiator makes Negotiation skill check
        // TODO NPC negotiator makes skill check
        // TODO Modify roll one step per 2 MoS (improve if player wins, decrease if NPC wins)

        // TODO pirates get special rules
    }

    public static NegotiationsData negotiateInitialContractTerms(Person playerNegotiator, Person employerNegotiator,
          Campaign campaign, CampaignTypeForContractDetermination campaignType) {
        boolean isPirateCampaignType = campaignType == CampaignTypeForContractDetermination.PIRATE;
        if (isPirateCampaignType) {
            return determinePirateNegotiationTerms();
        }

        EmployerModifierData employerModifierData = new EmployerModifierData();
        return determineInitialNegotiationTerms(employerModifierData,
              campaign,
              playerNegotiator,
              employerNegotiator);
    }

    private static NegotiationsData determinePirateNegotiationTerms() {
        return new NegotiationsData(PIRATE_COMMAND_RIGHTS,
              PIRATE_SALVAGE_TERMS,
              PIRATE_SUPPORT_RIGHTS,
              PIRATE_TRANSPORT_RIGHTS);
    }

    public static NegotiationsData determineInitialNegotiationTerms(EmployerModifierData employerModifierData,
          Campaign campaign, Person playerNegotiator, Person employerNegotiator) {
        boolean isPirateCampaignType = false;

        int commandRightsNegotiationModifier = makeOpposedNegotiationSkillCheck(campaign,
              playerNegotiator,
              employerNegotiator,
              isPirateCampaignType);
        ContractCommandRights commandRights = NegotiationTermsTables.rollOnCommandRightsTable(
              commandRightsNegotiationModifier,
              employerModifierData.getCommandModifier());

        int commandSalvageNegotiationModifier = makeOpposedNegotiationSkillCheck(campaign,
              playerNegotiator,
              employerNegotiator,
              isPirateCampaignType);
        double salvageRights = NegotiationTermsTables.rollOnSalvageRightsTable(commandSalvageNegotiationModifier,
              employerModifierData.getSalvageModifier());

        int commandSupportNegotiationModifier = makeOpposedNegotiationSkillCheck(campaign,
              playerNegotiator,
              employerNegotiator,
              isPirateCampaignType);
        double supportRights = NegotiationTermsTables.rollOnSupportRightsTable(commandSupportNegotiationModifier,
              employerModifierData.getSupportModifier());

        int commandTransportNegotiationModifier = makeOpposedNegotiationSkillCheck(campaign,
              playerNegotiator,
              employerNegotiator,
              isPirateCampaignType);
        double transportRights = NegotiationTermsTables.rollOnTransportRightsTable(commandTransportNegotiationModifier,
              employerModifierData.getTransportModifier());

        return new NegotiationsData(commandRights, salvageRights, supportRights, transportRights);
    }

    private static int makeOpposedNegotiationSkillCheck(Campaign campaign, Person playerNegotiator,
          Person employerNegotiator, boolean isPirateCampaignType) {
        String skillName = isPirateCampaignType ? S_INVESTIGATION : S_NEGOTIATION;

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
