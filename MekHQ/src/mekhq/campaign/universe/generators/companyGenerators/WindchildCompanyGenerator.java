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
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;

public class WindchildCompanyGenerator extends AbstractCompanyGenerator {
    //region Constructors
    public WindchildCompanyGenerator(final Campaign campaign, final CompanyGenerationOptions options) {
        super(campaign, CompanyGenerationMethod.WINDCHILD, options);
    }
    //endregion Constructors

    //region Personnel
    /**
     * Set based on greater than instead of the greater than or equal to of AtB
     * @param commandingOfficer the commanding officer
     * @param numMechWarriors the number of MechWarriors in their force, used to determine their rank
     */
    @Override
    protected void generateCommandingOfficerRank(Person commandingOfficer, int numMechWarriors) {
        if (numMechWarriors > 36) {
            commandingOfficer.setRankNumeric(Ranks.RWO_MAX + (getOptions().getFaction().isComStarOrWoB() ? 7 : 8));
        } else if (numMechWarriors > 12) {
            commandingOfficer.setRankNumeric(Ranks.RWO_MAX + (getOptions().getFaction().isComStarOrWoB() ? 7 : 5));
        } else if (numMechWarriors > 4) {
            commandingOfficer.setRankNumeric(Ranks.RWO_MAX + 4);
        } else {
            commandingOfficer.setRankNumeric(Ranks.RWO_MAX + 3);
        }
    }
    //endregion Personnel

    //region Units
    /**
     * This guarantees a BattleMech, and rolls an overall heavier lance
     *
     * @param roll the modified roll to use
     * @return the generated EntityWeightClass
     * EntityWeightClass.WEIGHT_ULTRA_LIGHT for none,
     * EntityWeightClass.WEIGHT_SUPER_HEAVY for SL tables
     */
    @Override
    protected int determineBattleMechWeight(int roll) {
        switch (roll) {
            case 2:
            case 3:
            case 4:
                return EntityWeightClass.WEIGHT_LIGHT;
            case 5:
            case 6:
            case 7:
                return EntityWeightClass.WEIGHT_MEDIUM;
            case 8:
            case 9:
            case 10:
                return EntityWeightClass.WEIGHT_HEAVY;
            case 11:
            case 12:
                return EntityWeightClass.WEIGHT_ASSAULT;
            default:
                return EntityWeightClass.WEIGHT_SUPER_HEAVY;
        }
    }

    /**
     * This generates a slightly higher average quality rating
     * @param roll the modified roll to use
     * @return the generated IUnitRating magic int for Dragoon Quality
     */
    @Override
    protected int determineBattleMechQuality(int roll) {
        switch (roll) {
            case 2:
            case 3:
            case 4:
                return IUnitRating.DRAGOON_F;
            case 5:
            case 6:
                return IUnitRating.DRAGOON_D;
            case 7:
            case 8:
                return IUnitRating.DRAGOON_C;
            case 9:
            case 10:
                return IUnitRating.DRAGOON_B;
            case 11:
            case 12:
                return IUnitRating.DRAGOON_A;
            default:
                return IUnitRating.DRAGOON_ASTAR;
        }
    }

    /**
     * This generates clan mech differently, so you can get any of the quality ratings for clanners
     *
     * @param campaign the campaign to generate for
     * @param parameters the parameters to use in generation
     * @param faction the faction to generate the mech from
     * @return the MechSummary generated from the provided parameters
     */
    @Override
    protected MechSummary generateMechSummary(final Campaign campaign,
                                              final AtBRandomMechParameters parameters,
                                              final Faction faction) {
        if (parameters.isStarLeague()) {
            if (faction.isClan()) {
                // Clanners generate using the Keshik Table if they roll A*, otherwise they roll on
                // the Front Line tables
                parameters.setQuality((parameters.getQuality() < IUnitRating.DRAGOON_ASTAR)
                        ? IUnitRating.DRAGOON_B : parameters.getQuality());
                return generateMechSummary(campaign, parameters, faction.getShortName(), campaign.getGameYear());
            } else {
                // Roll on the Star League Royal table if you get a SL mech with A* Rating
                final String factionCode = ((parameters.getQuality() == IUnitRating.DRAGOON_ASTAR) ? "SL.R" : "SL");
                return generateMechSummary(campaign, parameters, factionCode, getOptions().getStarLeagueYear());
            }
        } else {
            // Clanners Generate from 2nd Line (or lesser) Tables (core AtB is just 2nd Line,
            // but this is more interesting)
            if (faction.isClan() && (parameters.getQuality() > IUnitRating.DRAGOON_C)) {
                parameters.setQuality(IUnitRating.DRAGOON_C);
            }
            return generateMechSummary(campaign, parameters, faction.getShortName(), campaign.getGameYear());
        }
    }
    //endregion Units
}
