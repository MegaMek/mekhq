/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.generators.companyGenerators;

import megamek.common.MechSummary;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.companyGeneration.AtBRandomMechParameters;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationPersonTracker;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;

/**
 * @author Justin "Windchild" Bowen
 */
public class AtBCompanyGenerator extends AbstractCompanyGenerator {
    //region Constructors
    public AtBCompanyGenerator(final Campaign campaign, final CompanyGenerationOptions options) {
        super(CompanyGenerationMethod.AGAINST_THE_BOT, campaign, options);
    }
    //endregion Constructors

    //region Personnel
    /**
     * @param campaign the campaign to use in generating the commanding officer's rank
     * @param tracker the commanding officer's tracker
     * @param numMechWarriors the number of MechWarriors in their force, used to determine their rank
     */
    @Override
    protected void generateCommandingOfficerRank(final Campaign campaign,
                                                 final CompanyGenerationPersonTracker tracker,
                                                 final int numMechWarriors) {
        if (numMechWarriors >= 36) {
            tracker.getPerson().setRank(Rank.RWO_MAX + (campaign.getFaction().isComStarOrWoB() ? 7 : 8));
        } else if (numMechWarriors >= 12) {
            tracker.getPerson().setRank(Rank.RWO_MAX + (campaign.getFaction().isComStarOrWoB() ? 7 : 5));
        } else if (numMechWarriors >= 4) {
            tracker.getPerson().setRank(Rank.RWO_MAX + 4);
        } else {
            tracker.getPerson().setRank(Rank.RWO_MAX + 3);
        }
    }
    //endregion Personnel

    //region Units
    /**
     * @param campaign the campaign to generate for
     * @param parameters the parameters to use in generation
     * @param faction the faction to generate the mech from
     * @return the MechSummary generated from the provided parameters, or null if generation fails
     */
    @Override
    protected @Nullable MechSummary generateMechSummary(final Campaign campaign,
                                                        final AtBRandomMechParameters parameters,
                                                        final Faction faction) {
        if (parameters.isStarLeague() && !faction.isComStarOrWoB()) {
            if (faction.isClan()) {
                // Clanners generate from Front Line tables instead of Star League
                parameters.setQuality(IUnitRating.DRAGOON_B);
                return generateMechSummary(campaign, parameters, faction.getShortName(), campaign.getGameYear());
            } else {
                // Roll on the Star League Royal table if you get a SL mech with A* Rating
                final String factionCode = (parameters.getQuality() == IUnitRating.DRAGOON_ASTAR) ? "SL.R" : "SL";
                return generateMechSummary(campaign, parameters, factionCode, getOptions().getStarLeagueYear());
            }
        } else {
            // Clanners Generate from 2nd Line Tables
            if (faction.isClan()) {
                parameters.setQuality(IUnitRating.DRAGOON_C);
            }
            return generateMechSummary(campaign, parameters, faction.getShortName(), campaign.getGameYear());
        }
    }
    //endregion Units
}
