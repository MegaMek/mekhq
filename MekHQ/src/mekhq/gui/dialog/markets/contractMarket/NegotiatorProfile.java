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
package mekhq.gui.dialog.markets.contractMarket;

import static java.lang.Math.max;

import jakarta.annotation.Nullable;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.mission.contract.utilities.ActiveNegotiationMath;
import mekhq.campaign.mission.contract.utilities.ActiveNegotiationMath.Odds;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;

/**
 * What a negotiator brings to the negotiation table, worked out from their contract SPAs and Flaws: how far they can
 * raise each term, how much they can give up, how many re-negotiations they get, and the modifiers their traits add
 * to a re-negotiation's net margin. The negotiation table and the negotiator picker both read it, so the picker shows
 * exactly what the table will allow.
 *
 * @param raiseCap          how many steps above the employer's offer each term can be raised
 * @param sacrificeTerms    how many distinct terms can be given up
 * @param sacrificeSteps    how many steps can be given up in total
 * @param attempts          how many re-negotiations the contract allows
 * @param generalModifier   the modifier to every re-negotiation's net margin
 * @param haggleModifier    the further modifier when asking for a better offer
 * @param exceptionModifier the further modifier when asking for an exception
 *
 * @author Illiani
 * @since 0.51.01
 */
record NegotiatorProfile(int raiseCap, int sacrificeTerms, int sacrificeSteps, int attempts, int generalModifier,
      int haggleModifier, int exceptionModifier) {
    static final int BASE_MAXIMUM_SACRIFICED_TERMS = 2;
    static final int BASE_MAXIMUM_SACRIFICED_STEPS = 4;

    /**
     * A re-negotiation's odds for both options.
     *
     * @param haggle    the odds when asking for a better offer
     * @param exception the odds when asking for an exception
     */
    record TableOdds(Odds haggle, Odds exception) {
    }

    /**
     * @param negotiator the player's negotiator, or {@code null} for none (the base values apply)
     * @param scale      the contract's scale, which sets the raise cap before traits
     *
     * @return what that negotiator brings to the table
     */
    static NegotiatorProfile of(@Nullable Person negotiator, int scale) {
        int raiseCap = scale;
        int sacrificeTerms = BASE_MAXIMUM_SACRIFICED_TERMS;
        int sacrificeSteps = BASE_MAXIMUM_SACRIFICED_STEPS;
        int attempts = 1;
        int general = 0;
        int haggle = 0;
        int exception = 0;
        if (negotiator != null) {
            PersonnelOptions options = negotiator.getOptions();
            if (options.booleanOption(PersonnelOptions.HARD_BARGAINER)) {
                raiseCap++;
            }
            if (options.booleanOption(PersonnelOptions.PUSHOVER)) {
                raiseCap--;
            }
            if (options.booleanOption(PersonnelOptions.SHREWD_TRADER)) {
                sacrificeTerms++;
                sacrificeSteps += 2;
            }
            if (options.booleanOption(PersonnelOptions.INFLEXIBLE)) {
                sacrificeTerms--;
                sacrificeSteps -= 2;
            }
            if (options.booleanOption(PersonnelOptions.RELENTLESS_BARGAINER)) {
                attempts = 2;
            }
            // Abrasive worsens every re-negotiation.
            if (options.booleanOption(PersonnelOptions.ABRASIVE)) {
                general--;
            }
            // Fine Print Reader wins better terms; Easily Fooled, its opposite, loses ground.
            if (options.booleanOption(PersonnelOptions.FINE_PRINT_READER)) {
                haggle++;
            }
            if (options.booleanOption(PersonnelOptions.EASILY_FOOLED)) {
                haggle--;
            }
            // Loophole Finder frees more terms; Blacklisted, its opposite, locks more.
            if (options.booleanOption(PersonnelOptions.LOOPHOLE_FINDER)) {
                exception++;
            }
            if (options.booleanOption(PersonnelOptions.BLACKLISTED)) {
                exception--;
            }
        }
        return new NegotiatorProfile(max(1, raiseCap), max(0, sacrificeTerms), max(0, sacrificeSteps), attempts,
              general, haggle, exception);
    }

    /** Whether a negotiation trait works in the player's favour (an SPA) rather than against them (a Flaw). */
    static boolean isHelpfulTrait(String trait) {
        return trait.equals(PersonnelOptions.HARD_BARGAINER) || trait.equals(PersonnelOptions.SHREWD_TRADER)
                     || trait.equals(PersonnelOptions.FINE_PRINT_READER)
                     || trait.equals(PersonnelOptions.LOOPHOLE_FINDER)
                     || trait.equals(PersonnelOptions.RELENTLESS_BARGAINER)
                     || trait.equals(PersonnelOptions.EDGE_COMMANDER_NEGOTIATION);
    }

    /** Whether the player's negotiator spends Edge to reroll a failed re-negotiation check. */
    static boolean playerUsesEdge(Person negotiator, Campaign campaign) {
        return campaign.getCampaignOptions().get(CampaignOption.USE_EDGE)
                     && negotiator.getOptions().booleanOption(PersonnelOptions.EDGE_COMMANDER_NEGOTIATION);
    }

    /**
     * The odds of each re-negotiation for a player's negotiator against the employer's, worked out exactly from both
     * target numbers, natural aptitude, Edge rerolls and the player's traits.
     *
     * @param campaign           the campaign
     * @param playerNegotiator   the player's negotiator
     * @param employerNegotiator the employer's negotiator, or {@code null} when the employer always barely makes it
     * @param scale              the contract's scale
     *
     * @return the odds of asking for a better offer and of asking for an exception
     */
    static TableOdds odds(Campaign campaign, Person playerNegotiator, @Nullable Person employerNegotiator, int scale) {
        double[] player = marginChances(campaign, playerNegotiator, playerUsesEdge(playerNegotiator, campaign));
        double[] employer = employerNegotiator == null ? null
                                  : marginChances(campaign, employerNegotiator,
                                        campaign.getCampaignOptions().get(CampaignOption.USE_EDGE));
        NegotiatorProfile profile = of(playerNegotiator, scale);
        return new TableOdds(
              ActiveNegotiationMath.opposedOdds(player, employer, profile.generalModifier + profile.haggleModifier),
              ActiveNegotiationMath.opposedOdds(player, employer,
                    profile.generalModifier + profile.exceptionModifier));
    }

    /** The chance of each margin on a person's Negotiation check, as a re-negotiation rolls it. */
    private static double[] marginChances(Campaign campaign, Person person, boolean useEdge) {
        TargetRoll target = person.checkSkill(SkillType.S_NEGOTIATION, campaign).getTargetNumber();
        Skill skill = person.getSkill(SkillType.S_NEGOTIATION);
        return ActiveNegotiationMath.marginChances(target.getValue(), target.cannotSucceed(),
              SkillType.getType(SkillType.S_NEGOTIATION).isCountUp(), skill != null && skill.getHasNaturalAptitude(),
              useEdge && person.getCurrentEdge() > 0);
    }
}
