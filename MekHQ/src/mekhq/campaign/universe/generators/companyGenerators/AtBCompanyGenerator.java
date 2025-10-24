/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.generators.companyGenerators;

import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.companyGeneration.AtBRandomMekParameters;
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
     * @param faction        the faction to use in generating the commanding officer's rank
     * @param tracker        the commanding officer's tracker
     * @param numMekWarriors the number of MekWarriors in their force, used to determine their rank
     */
    @Override
    protected void generateCommandingOfficerRank(final Faction faction,
          final CompanyGenerationPersonTracker tracker,
          final int numMekWarriors) {
        if (numMekWarriors >= 36) {
            tracker.getPerson().setRank(Rank.RWO_MAX + (faction.isComStarOrWoB() ? 7 : 8));
        } else if (numMekWarriors >= 12) {
            tracker.getPerson().setRank(Rank.RWO_MAX + (faction.isComStarOrWoB() ? 7 : 5));
        } else if (numMekWarriors >= 4) {
            tracker.getPerson().setRank(Rank.RWO_MAX + 4);
        } else {
            tracker.getPerson().setRank(Rank.RWO_MAX + 3);
        }
    }
    //endregion Personnel

    //region Units

    /**
     * @param campaign   the campaign to generate for
     * @param parameters the parameters to use in generation
     * @param faction    the faction to generate the mek from
     *
     * @return the MekSummary generated from the provided parameters, or null if generation fails
     */
    @Override
    protected @Nullable MekSummary generateMekSummary(final Campaign campaign,
          final AtBRandomMekParameters parameters,
          final Faction faction) {
        if (parameters.isStarLeague() && !faction.isComStarOrWoB()) {
            if (faction.isClan()) {
                // Clan Pilots generate from Front Line tables instead of Star League
                parameters.setQuality(DragoonRating.DRAGOON_B.getRating());
                return generateMekSummary(campaign, parameters, faction.getShortName(), campaign.getGameYear());
            } else {
                // Roll on the Star League Royal table if you get an SL mek with A* Rating
                final String factionCode = (parameters.getQuality() == DragoonRating.DRAGOON_ASTAR.getRating()) ?
                                                 "SL.R" :
                                                 "SL";
                return generateMekSummary(campaign, parameters, factionCode, getOptions().getStarLeagueYear());
            }
        } else {
            // Clan Pilots Generate from 2nd Line Tables
            if (faction.isClan()) {
                parameters.setQuality(DragoonRating.DRAGOON_C.getRating());
            }
            return generateMekSummary(campaign, parameters, faction.getShortName(), campaign.getGameYear());
        }
    }
    //endregion Units
}
