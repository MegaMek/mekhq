/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.EntityWeightClass;
import megamek.common.MechSummary;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;

public class AtBCompanyGenerator extends AbstractCompanyGenerator {
    //region Constructors
    public AtBCompanyGenerator(final Campaign campaign, final CompanyGenerationOptions options) {
        super(CompanyGenerationMethod.AGAINST_THE_BOT, campaign, options);
    }
    //endregion Constructors

    //region Personnel
    /**
     * @param campaign the campaign to use in generating the commanding officer's rank
     * @param commandingOfficer the commanding officer
     * @param numMechWarriors the number of MechWarriors in their force, used to determine their rank
     */
    @Override
    protected void generateCommandingOfficerRank(final Campaign campaign,
                                                 final Person commandingOfficer,
                                                 final int numMechWarriors) {
        if (numMechWarriors >= 36) {
            commandingOfficer.setRank(Rank.RWO_MAX + (campaign.getFaction().isComStarOrWoB() ? 7 : 8));
        } else if (numMechWarriors >= 12) {
            commandingOfficer.setRank(Rank.RWO_MAX + (campaign.getFaction().isComStarOrWoB() ? 7 : 5));
        } else if (numMechWarriors >= 4) {
            commandingOfficer.setRank(Rank.RWO_MAX + 4);
        } else {
            commandingOfficer.setRank(Rank.RWO_MAX + 3);
        }
    }
    //endregion Personnel

    //region Units
    /**
     * @param roll the modified roll to use
     * @return the generated EntityWeightClass
     * EntityWeightClass.WEIGHT_ULTRA_LIGHT for none,
     * EntityWeightClass.WEIGHT_SUPER_HEAVY for SL tables
     */
    @Override
    protected int determineBattleMechWeight(final int roll) {
        switch (roll) {
            case 2:
            case 3:
                return EntityWeightClass.WEIGHT_ULTRA_LIGHT;
            case 4:
            case 5:
            case 6:
                return EntityWeightClass.WEIGHT_LIGHT;
            case 7:
            case 8:
            case 9:
                return EntityWeightClass.WEIGHT_MEDIUM;
            case 10:
            case 11:
                return EntityWeightClass.WEIGHT_HEAVY;
            case 12:
                return EntityWeightClass.WEIGHT_ASSAULT;
            default:
                return EntityWeightClass.WEIGHT_SUPER_HEAVY;
        }
    }

    /**
     * @param roll the modified roll to use
     * @return the generated IUnitRating magic int for Dragoon Quality
     */
    @Override
    protected int determineBattleMechQuality(final int roll) {
        switch (roll) {
            case 2:
            case 3:
            case 4:
            case 5:
                return IUnitRating.DRAGOON_F;
            case 6:
            case 7:
            case 8:
                return IUnitRating.DRAGOON_D;
            case 9:
            case 10:
                return IUnitRating.DRAGOON_C;
            case 11:
                return IUnitRating.DRAGOON_B;
            case 12:
                return IUnitRating.DRAGOON_A;
            default:
                return IUnitRating.DRAGOON_ASTAR;
        }
    }

    /**
     * @param campaign the campaign to generate for
     * @param parameters the parameters to use in generation
     * @param faction the faction to generate the mech from
     * @return the MechSummary generated from the provided parameters
     */
    @Override
    protected MechSummary generateMechSummary(final Campaign campaign,
                                              final AtBRandomMechParameters parameters,
                                              final Faction faction) {
        if (parameters.isStarLeague() && !faction.isComStarOrWoB()) {
            if (faction.isClan()) {
                // Clanners generate from Front Line tables instead of Star League
                parameters.setQuality(IUnitRating.DRAGOON_B);
                return generateMechSummary(campaign, parameters, faction.getShortName(), campaign.getGameYear());
            } else {
                // Roll on the Star League Royal table if you get a SL mech with A* Rating
                final String factionCode = ((parameters.getQuality() == IUnitRating.DRAGOON_ASTAR) ? "SL.R" : "SL");
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
