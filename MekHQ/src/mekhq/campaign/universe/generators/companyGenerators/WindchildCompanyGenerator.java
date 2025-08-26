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

import megamek.common.loaders.MekSummary;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.companyGeneration.AtBRandomMekParameters;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationPersonTracker;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;

/**
 * @author Justin "Windchild" Bowen
 */
public class WindchildCompanyGenerator extends AbstractCompanyGenerator {
    //region Constructors
    public WindchildCompanyGenerator(final Campaign campaign, final CompanyGenerationOptions options) {
        super(CompanyGenerationMethod.WINDCHILD, campaign, options);
    }
    //endregion Constructors

    //region Personnel

    /**
     * Set based on greater than instead of the greater than or equal to of AtB
     *
     * @param faction        the faction to use in generating the commanding officer's rank
     * @param tracker        the commanding officer's tracker
     * @param numMekWarriors the number of MekWarriors in their force, used to determine their rank
     */
    @Override
    protected void generateCommandingOfficerRank(final Faction faction,
          final CompanyGenerationPersonTracker tracker,
          final int numMekWarriors) {
        if (numMekWarriors > 36) {
            tracker.getPerson().setRank(Rank.RWO_MAX + (faction.isComStarOrWoB() ? 7 : 8));
        } else if (numMekWarriors > 12) {
            tracker.getPerson().setRank(Rank.RWO_MAX + (faction.isComStarOrWoB() ? 7 : 5));
        } else if (numMekWarriors > 4) {
            tracker.getPerson().setRank(Rank.RWO_MAX + 4);
        } else {
            tracker.getPerson().setRank(Rank.RWO_MAX + 3);
        }
    }
    //endregion Personnel

    //region Units

    /**
     * This generates Clan 'Meks differently, so you can get any of the quality ratings for Clan Pilots.
     *
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
        if (parameters.isStarLeague()) {
            if (faction.isClan()) {
                // Clan Pilots generate using the Keshik Table if they roll A*, otherwise they roll on
                // the Front Line tables
                parameters.setQuality((parameters.getQuality() == IUnitRating.DRAGOON_ASTAR)
                                            ? IUnitRating.DRAGOON_ASTAR : IUnitRating.DRAGOON_B);
                return generateMekSummary(campaign, parameters, faction.getShortName(), campaign.getGameYear());
            } else {
                // Roll on the Star League Royal table if you get a SL mek with A* Rating
                final String factionCode = (parameters.getQuality() == IUnitRating.DRAGOON_ASTAR) ? "SL.R" : "SL";
                return generateMekSummary(campaign, parameters, factionCode, getOptions().getStarLeagueYear());
            }
        } else {
            // Clan Pilots Generate from 2nd Line (or lesser) Tables (core AtB is just 2nd Line,
            // but this is more interesting)
            if (faction.isClan() && (parameters.getQuality() > IUnitRating.DRAGOON_C)) {
                parameters.setQuality(IUnitRating.DRAGOON_C);
            }
            return generateMekSummary(campaign, parameters, faction.getShortName(), campaign.getGameYear());
        }
    }
    //endregion Units
}
