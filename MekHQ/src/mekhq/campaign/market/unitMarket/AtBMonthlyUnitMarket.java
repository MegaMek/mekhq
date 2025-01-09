/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.unitMarket;

import megamek.client.ratgenerator.MissionRole;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.Compute;
import megamek.common.EntityMovementMode;
import megamek.common.UnitType;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.againstTheBot.AtBStaticWeightGenerator;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.market.enums.UnitMarketRarity;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.campaign.universe.RandomFactionGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static mekhq.campaign.market.enums.UnitMarketRarity.*;
import static mekhq.campaign.market.enums.UnitMarketType.getPricePercentage;

public class AtBMonthlyUnitMarket extends AbstractUnitMarket {
    //region Constructors
    public AtBMonthlyUnitMarket() {
        super(UnitMarketMethod.ATB_MONTHLY);
    }
    //endregion Constructors

    //region Process New Day
    /**
     * This market runs monthly, so it only executes removal and generation on the first day of the
     * month
     * @param campaign the campaign to process the Unit Market new day using
     */
    @Override
    public void processNewDay(final Campaign campaign) {
        if (campaign.getLocalDate().getDayOfMonth() == 1) {
            removeUnitOffers(campaign);
            generateUnitOffers(campaign);
        }
    }

    //region Generate Offers

    /**
     * Generates unit offers based on market type, unit type, and rarity
     *
     * @param campaign The ongoing campaign
     */
    @Override
    public void generateUnitOffers(final Campaign campaign) {
        final List<AtBContract> contracts = campaign.getActiveAtBContracts();
        final AtBContract contract = contracts.isEmpty() ? null : contracts.get(0);

        // Open Market
        Faction faction = campaign.getFaction();
        int rarityModifier = campaign.getCampaignOptions().getUnitMarketRarityModifier();

        addOffers(campaign, getMarketItemCount(UNCOMMON, rarityModifier), UnitMarketType.OPEN, UnitType.MEK,
                faction, IUnitRating.DRAGOON_F, 1);

        addOffers(campaign, getMarketItemCount(UNCOMMON, rarityModifier), UnitMarketType.OPEN, UnitType.AEROSPACEFIGHTER,
                faction, IUnitRating.DRAGOON_F, 1);

        addOffers(campaign, getMarketItemCount(VERY_COMMON, rarityModifier), UnitMarketType.OPEN, UnitType.TANK,
                faction, IUnitRating.DRAGOON_F, 1);

        addOffers(campaign, getMarketItemCount(COMMON, rarityModifier), UnitMarketType.OPEN, UnitType.CONV_FIGHTER,
                faction, IUnitRating.DRAGOON_F, 1);

        if ((contract != null) && (campaign.getLocalDate().isAfter(contract.getStartDate().minusDays(1)))) {
            // Employer Market
            faction = contract.getEmployerFaction();

            addOffers(campaign, getMarketItemCount(RARE, rarityModifier), UnitMarketType.EMPLOYER, UnitType.MEK,
                    faction, IUnitRating.DRAGOON_D, -1);

            addOffers(campaign, getMarketItemCount(RARE, rarityModifier), UnitMarketType.EMPLOYER, UnitType.AEROSPACEFIGHTER,
                    faction, IUnitRating.DRAGOON_D, -1);

            addOffers(campaign, getMarketItemCount(COMMON, rarityModifier), UnitMarketType.EMPLOYER, UnitType.TANK,
                    faction, IUnitRating.DRAGOON_D, -1);

            addOffers(campaign, getMarketItemCount(UNCOMMON, rarityModifier), UnitMarketType.EMPLOYER, UnitType.CONV_FIGHTER,
                    faction, IUnitRating.DRAGOON_D, -1);

            // Unwanted Salvage Market
            faction = contract.getEnemy();

            addOffers(campaign, getMarketItemCount(RARE, rarityModifier), UnitMarketType.EMPLOYER, UnitType.MEK,
                    faction, IUnitRating.DRAGOON_F, 2);

            addOffers(campaign, getMarketItemCount(RARE, rarityModifier), UnitMarketType.EMPLOYER, UnitType.AEROSPACEFIGHTER,
                    faction, IUnitRating.DRAGOON_F, 2);

            addOffers(campaign, getMarketItemCount(UNCOMMON, rarityModifier), UnitMarketType.EMPLOYER, UnitType.TANK,
                    faction, IUnitRating.DRAGOON_F, 2);

            addOffers(campaign, getMarketItemCount(UNCOMMON, rarityModifier), UnitMarketType.EMPLOYER, UnitType.CONV_FIGHTER,
                    faction, IUnitRating.DRAGOON_F, 2);
        }

        // Mercenary Market
        if (!campaign.getFaction().isClan()) {
            faction = Factions.getInstance().getFaction("MERC");

            int modifier = 1;

            if (campaign.getFaction().isMercenary()) {
                modifier = -1;
            }

            addOffers(campaign, getMarketItemCount(UNCOMMON, rarityModifier), UnitMarketType.MERCENARY, UnitType.MEK,
                    faction, IUnitRating.DRAGOON_C, modifier);

            addOffers(campaign, getMarketItemCount(UNCOMMON, rarityModifier), UnitMarketType.MERCENARY, UnitType.AEROSPACEFIGHTER,
                    faction, IUnitRating.DRAGOON_C, modifier);

            addOffers(campaign, getMarketItemCount(VERY_COMMON, rarityModifier), UnitMarketType.MERCENARY, UnitType.TANK,
                    faction, IUnitRating.DRAGOON_C, modifier);

            addOffers(campaign, getMarketItemCount(UNCOMMON, rarityModifier), UnitMarketType.MERCENARY, UnitType.CONV_FIGHTER,
                    faction, IUnitRating.DRAGOON_C, modifier);
        }

        // Factory Market
        if (campaign.getAtBUnitRatingMod() >= IUnitRating.DRAGOON_B) {
            faction = ObjectUtility.getRandomItem(campaign.getCurrentSystem()
                    .getFactionSet(campaign.getLocalDate()));

            if ((!campaign.getFaction().isClan()) && (faction != null) && (!faction.isClan())) {
                addOffers(campaign, getMarketItemCount(RARE, rarityModifier), UnitMarketType.FACTORY, UnitType.MEK,
                        faction, IUnitRating.DRAGOON_A, 2);

                addOffers(campaign, getMarketItemCount(RARE, rarityModifier), UnitMarketType.FACTORY, UnitType.AEROSPACEFIGHTER,
                        faction, IUnitRating.DRAGOON_A, 2);

                addOffers(campaign, getMarketItemCount(COMMON, rarityModifier), UnitMarketType.FACTORY, UnitType.TANK,
                        faction, IUnitRating.DRAGOON_A, 2);

                addOffers(campaign, getMarketItemCount(UNCOMMON, rarityModifier), UnitMarketType.FACTORY, UnitType.CONV_FIGHTER,
                        faction, IUnitRating.DRAGOON_A, 2);
            }
        }

        faction = campaign.getFaction();

        // Clan Factory Market
        if ((faction.isClan()) && (campaign.getCurrentSystem().getFactionSet(campaign.getLocalDate()).contains(faction))) {
            addOffers(campaign, getMarketItemCount(VERY_COMMON, rarityModifier), UnitMarketType.FACTORY, UnitType.MEK,
                    faction, IUnitRating.DRAGOON_A, -4);

            addOffers(campaign, getMarketItemCount(COMMON, rarityModifier), UnitMarketType.FACTORY, UnitType.AEROSPACEFIGHTER,
                    faction, IUnitRating.DRAGOON_A, -4);

            addOffers(campaign, getMarketItemCount(UNCOMMON, rarityModifier), UnitMarketType.FACTORY, UnitType.TANK,
                    faction, IUnitRating.DRAGOON_A, -4);
        }

        // Black Market
        if (!campaign.getFaction().isClan()) {
            faction = ObjectUtility.getRandomItem(campaign.getCurrentSystem()
                    .getFactionSet(campaign.getLocalDate()));

            addOffers(campaign, getMarketItemCount(VERY_RARE, rarityModifier), UnitMarketType.BLACK_MARKET, UnitType.MEK,
                    faction, IUnitRating.DRAGOON_A, -8);

            addOffers(campaign, getMarketItemCount(VERY_RARE, rarityModifier), UnitMarketType.BLACK_MARKET, UnitType.AEROSPACEFIGHTER,
                    faction, IUnitRating.DRAGOON_A, -8);

            addOffers(campaign, getMarketItemCount(RARE, rarityModifier), UnitMarketType.BLACK_MARKET, UnitType.TANK,
                    faction, IUnitRating.DRAGOON_A, -8);

            addOffers(campaign, getMarketItemCount(RARE, rarityModifier), UnitMarketType.BLACK_MARKET, UnitType.CONV_FIGHTER,
                    faction, IUnitRating.DRAGOON_A, -8);
        }

        writeRefreshReport(campaign);
    }

    /**
     * Returns a count of market items based on the specified rarity.
     *
     * @param rarity the rarity of the market items
     * @param rarityModifier the unit count modifier specified in campaign options
     * @return an integer representing the count of market items
     */
    private int getMarketItemCount(UnitMarketRarity rarity, int rarityModifier) {
        return Compute.d6(1)
                + rarity.ordinal()
                + rarityModifier
                - 3;
    }

    @Override
    public void addOffers(final Campaign campaign, final int num, UnitMarketType market,
                             final int unitType, @Nullable Faction faction, final int quality,
                             final int priceModifier) {
        if (faction == null) {
            faction = RandomFactionGenerator.getInstance().getEmployerFaction();
        }

        if (faction == null) {
            faction = campaign.getFaction();
            market = UnitMarketType.OPEN;
        }

        for (int i = 0; i < num; i++) {
            final Collection<EntityMovementMode> movementModes = new ArrayList<>();
            final Collection<MissionRole> missionRoles = new ArrayList<>();

            if (unitType == UnitType.TANK) {
                movementModes.addAll(IUnitGenerator.MIXED_TANK_VTOL);

                // should a special unit type be picked? This allows us to force a MissionRole that would otherwise be filtered out
                int specialUnitChance = campaign.getCampaignOptions().getUnitMarketSpecialUnitChance();

                if (specialUnitChance != 0) {
                    if ((specialUnitChance == 1) || (Compute.randomInt(specialUnitChance) == 0)) {
                        // this will need to be incremented by 1,
                        // whenever we add additional unit types to this special handler
                        int roll = Compute.randomInt(6);

                        // while this gives even chances for each role,
                        // it gives greater control to the user
                        // to define how often they want to see special units.
                        // really, this is just a Band-Aid fix, and ideally we wouldn't be filtering out these units in the first place
                        switch (roll) {
                            case 0 -> missionRoles.add(MissionRole.CIVILIAN);
                            case 1 -> missionRoles.add(MissionRole.SUPPORT);
                            case 2 -> missionRoles.add(MissionRole.CARGO);
                            case 3, 4, 5 -> missionRoles.add(MissionRole.ARTILLERY);
                            default -> throw new IllegalStateException(
                                "Unexpected value in mekhq/campaign/market/unitMarket/AtBMonthlyUnitMarket.java/addOffers: "
                                    + roll);
                        }
                    }
                }
            }

            addSingleUnit(campaign, market, unitType, faction, quality, movementModes, missionRoles, getPricePercentage(priceModifier));
        }
    }

    /**
     * This generates a random weight using the static weight generation methods in this market
     * @param campaign the campaign to generate the unit weight based on
     * @param unitType the unit type to determine the format of weight to generate
     * @param faction the faction to generate the weight for
     * @return the generated weight
     */
    @Override
    protected int generateWeight(final Campaign campaign, final int unitType, final Faction faction) {
        return AtBStaticWeightGenerator.getRandomWeight(campaign, unitType, faction);
    }
    //endregion Offer Generation

    //region Offer Removal
    /**
     * The AtB Unit Market clears all offers from the unit market each month
     * @param campaign the campaign to use in determining the offers to remove
     */
    @Override
    public void removeUnitOffers(final Campaign campaign) {
        getOffers().clear();
    }
    //endregion Offer Removal
    //endregion Process New Day
}
