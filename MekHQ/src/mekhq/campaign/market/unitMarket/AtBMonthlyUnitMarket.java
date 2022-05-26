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
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.enums.UnitMarketMethod;
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
     * This generates Unit Offers as per the AtB Unit Market rules
     * @param campaign the campaign to generate the unit offers for
     */
    @Override
    public void generateUnitOffers(final Campaign campaign) {
        final List<AtBContract> contracts = campaign.getActiveAtBContracts();
        final AtBContract contract = contracts.isEmpty() ? null : contracts.get(0);

        addOffers(campaign, Compute.d6() - 2, UnitMarketType.OPEN, UnitType.MEK,
                null, IUnitRating.DRAGOON_F, 7);
        addOffers(campaign, Compute.d6() - 1, UnitMarketType.OPEN, UnitType.TANK,
                null, IUnitRating.DRAGOON_F, 7);
        addOffers(campaign, Compute.d6() - 2, UnitMarketType.OPEN, UnitType.AERO,
                null, IUnitRating.DRAGOON_F, 7);
        addOffers(campaign, Compute.d6() - 2, UnitMarketType.OPEN, UnitType.CONV_FIGHTER,
                null, IUnitRating.DRAGOON_F, 7);

        if (contract != null) {
            final Faction employer = contract.getEmployerFaction();
            addOffers(campaign, Compute.d6() - 3, UnitMarketType.EMPLOYER, UnitType.MEK,
                    employer, IUnitRating.DRAGOON_D, 7);
            addOffers(campaign, Compute.d6() - 2, UnitMarketType.EMPLOYER, UnitType.TANK,
                    employer, IUnitRating.DRAGOON_D, 7);
            addOffers(campaign, Compute.d6() - 3, UnitMarketType.EMPLOYER, UnitType.AERO,
                    employer, IUnitRating.DRAGOON_D, 7);
            addOffers(campaign, Compute.d6() - 3, UnitMarketType.EMPLOYER, UnitType.CONV_FIGHTER,
                    employer, IUnitRating.DRAGOON_D, 7);
        }

        if (!campaign.getFaction().isClan()) {
            final Faction mercenaryFaction = Factions.getInstance().getFaction("MERC");
            addOffers(campaign, Compute.d6(3) - 9, UnitMarketType.MERCENARY,
                    UnitType.MEK, mercenaryFaction, IUnitRating.DRAGOON_C, 5);
            addOffers(campaign, Compute.d6(3) - 6, UnitMarketType.MERCENARY,
                    UnitType.TANK, mercenaryFaction, IUnitRating.DRAGOON_C, 5);
            addOffers(campaign, Compute.d6(3) - 9, UnitMarketType.MERCENARY,
                    UnitType.AERO, mercenaryFaction, IUnitRating.DRAGOON_C, 5);
            addOffers(campaign, Compute.d6(3) - 9, UnitMarketType.MERCENARY,
                    UnitType.CONV_FIGHTER, mercenaryFaction, IUnitRating.DRAGOON_C, 5);
        }

        if (campaign.getUnitRatingMod() >= IUnitRating.DRAGOON_B) {
            final Faction faction = ObjectUtility.getRandomItem(campaign.getCurrentSystem()
                    .getFactionSet(campaign.getLocalDate()));
            if (campaign.getFaction().isClan() || (((faction != null)) && !faction.isClan())) {
                addOffers(campaign, Compute.d6() - 3, UnitMarketType.FACTORY, UnitType.MEK,
                        faction, IUnitRating.DRAGOON_A, 6);
                addOffers(campaign, Compute.d6() - 2, UnitMarketType.FACTORY, UnitType.TANK,
                        faction, IUnitRating.DRAGOON_A, 6);
                addOffers(campaign, Compute.d6() - 3, UnitMarketType.FACTORY, UnitType.AERO,
                        faction, IUnitRating.DRAGOON_A, 6);
                addOffers(campaign, Compute.d6() - 3, UnitMarketType.FACTORY, UnitType.CONV_FIGHTER,
                        faction, IUnitRating.DRAGOON_A, 6);
            }
        }

        if (!campaign.getFaction().isClan()) {
            addOffers(campaign, Compute.d6(2) - 6, UnitMarketType.BLACK_MARKET, UnitType.MEK,
                    null, IUnitRating.DRAGOON_C, 6);
            addOffers(campaign, Compute.d6(2) - 4, UnitMarketType.BLACK_MARKET, UnitType.TANK,
                    null, IUnitRating.DRAGOON_C, 6);
            addOffers(campaign, Compute.d6(2) - 6, UnitMarketType.BLACK_MARKET, UnitType.AERO,
                    null, IUnitRating.DRAGOON_C, 6);
            addOffers(campaign, Compute.d6(2) - 6, UnitMarketType.BLACK_MARKET, UnitType.CONV_FIGHTER,
                    null, IUnitRating.DRAGOON_C, 6);
        }

        writeRefreshReport(campaign);
    }

    @Override
    protected void addOffers(final Campaign campaign, final int num, UnitMarketType market,
                             final int unitType, @Nullable Faction faction, final int quality,
                             final int priceTarget) {
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
                missionRoles.add(MissionRole.MIXED_ARTILLERY);
            }
            final int percent = 100 - (Compute.d6(2) - priceTarget) * 5;
            addSingleUnit(campaign, market, unitType, faction, quality, movementModes, missionRoles, percent);
        }
    }

    //region Random Weight
    /**
     * This generates a random weight using the static weight generation methods in this market
     * @param campaign the campaign to generate the unit weight based on
     * @param unitType the unit type to determine the format of weight to generate
     * @param faction the faction to generate the weight for
     * @return the generated weight
     */
    @Override
    protected int generateWeight(final Campaign campaign, final int unitType, final Faction faction) {
        return getRandomWeight(unitType, faction, campaign.getCampaignOptions().useUnitMarketRegionalMechVariations());
    }

    /**
     * This is a simplification method that is used for regional 'Mech variations not part of the
     * Unit Market while sharing the same code
     * @param campaign the campaign to generate the unit weight based on
     * @param unitType the unit type to determine the format of weight to generate
     * @param faction the faction to generate the weight for
     * @return the generated weight
     */
    public static int getRandomWeight(final Campaign campaign, final int unitType, final Faction faction) {
        return getRandomWeight(unitType, faction, campaign.getCampaignOptions().getRegionalMechVariations());
    }


    /**
     * @param unitType the unit type to determine the format of weight to generate
     * @param faction the faction to generate the weight for
     * @param regionVariations whether to generate 'Mech weights based on hardcoded regional variations
     * @return the generated weight
     */
    private static int getRandomWeight(final int unitType, final Faction faction, final boolean regionVariations) {
        if (unitType == UnitType.AERO) {
            return getRandomAerospaceWeight();
        } else if ((unitType == UnitType.MEK) && regionVariations) {
            return getRegionalMechWeight(faction);
        } else {
            return getRandomMechWeight();
        }
    }

    /**
     * @return the generated weight for a BattleMech
     */
    private static int getRandomMechWeight() {
        final int roll = Compute.randomInt(10);
        if (roll < 3) {
            return EntityWeightClass.WEIGHT_LIGHT;
        } else if (roll < 7) {
            return EntityWeightClass.WEIGHT_MEDIUM;
        } else if (roll < 9) {
            return EntityWeightClass.WEIGHT_HEAVY;
        } else {
            return EntityWeightClass.WEIGHT_ASSAULT;
        }
    }

    /**
     * @param faction the faction to determine the regional BattleMech weight for
     * @return the generated weight for a BattleMech
     */
    private static int getRegionalMechWeight(final Faction faction) {
        final int roll = Compute.randomInt(100);
        switch (faction.getShortName()) {
            case "DC":
                if (roll < 40) {
                    return EntityWeightClass.WEIGHT_LIGHT;
                } else if (roll < 60) {
                    return EntityWeightClass.WEIGHT_MEDIUM;
                } else if (roll < 90) {
                    return EntityWeightClass.WEIGHT_HEAVY;
                } else {
                    return EntityWeightClass.WEIGHT_ASSAULT;
                }
            case "LA":
                if (roll < 20) {
                    return EntityWeightClass.WEIGHT_LIGHT;
                } else if (roll < 50) {
                    return EntityWeightClass.WEIGHT_MEDIUM;
                } else if (roll < 85) {
                    return EntityWeightClass.WEIGHT_HEAVY;
                } else {
                    return EntityWeightClass.WEIGHT_ASSAULT;
                }
            case "FWL":
                if (roll < 30) {
                    return EntityWeightClass.WEIGHT_LIGHT;
                } else if (roll < 70) {
                    return EntityWeightClass.WEIGHT_MEDIUM;
                } else if (roll < 92) {
                    return EntityWeightClass.WEIGHT_HEAVY;
                } else {
                    return EntityWeightClass.WEIGHT_ASSAULT;
                }
            default:
                if (roll < 30) {
                    return EntityWeightClass.WEIGHT_LIGHT;
                } else if (roll < 70) {
                    return EntityWeightClass.WEIGHT_MEDIUM;
                } else if (roll < 90) {
                    return EntityWeightClass.WEIGHT_HEAVY;
                } else {
                    return EntityWeightClass.WEIGHT_ASSAULT;
                }
        }
    }

    /**
     * @return the generated random weight for an Aerospace Fighter
     */
    private static int getRandomAerospaceWeight() {
        final int roll = Compute.randomInt(8);
        if (roll < 3) {
            return EntityWeightClass.WEIGHT_LIGHT;
        } else if (roll < 7) {
            return EntityWeightClass.WEIGHT_MEDIUM;
        } else {
            return EntityWeightClass.WEIGHT_HEAVY;
        }
    }
    //endregion Random Weight
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
