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
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.enums.HiringHallLevel;

public class ContractDeterminationNegotiations {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractDeterminationNegotiations";

    private final static String NEGOTIATION_CHECK_REASON = getTextAt(RESOURCE_BUNDLE,
          "ContractDeterminationNegotiations.skillCheck.reason");

    /** CamOps pg 41 rev 5th printing */
    private final static double MARGIN_OF_SUCCESS_DIVIDER = 2.0;

    public ContractDeterminationNegotiations() {
        // TODO - roll 2d6 per term
        // TODO player negotiator makes Negotiation skill check
        // TODO NPC negotiator makes skill check
        // TODO Modify roll one step per 2 MoS (improve if player wins, decrease if NPC wins)

        // TODO pirates get special rules
    }

    public static void negotiateInitialContractTerms(Person playerNegotiator, Campaign campaign,
          CampaignTypeForContractDetermination campaignType, Faction employerFaction, HiringHallLevel hiringHallLevel) {
        Person employerNegotiator = EmployerNegotiator.generateNegotiator(campaign,
              campaignType,
              employerFaction,
              hiringHallLevel);

        // TODO Command Rights
        // TODO Salvage Rights
        // TODO Support Rights
        // TODO Transport Rights
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
