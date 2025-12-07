/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.unitMarket;

import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.market.enums.UnitMarketRarity.COMMON;
import static mekhq.campaign.market.enums.UnitMarketRarity.MYTHIC;
import static mekhq.campaign.market.enums.UnitMarketRarity.RARE;
import static mekhq.campaign.market.enums.UnitMarketRarity.UBIQUITOUS;
import static mekhq.campaign.market.enums.UnitMarketRarity.UNCOMMON;
import static mekhq.campaign.market.enums.UnitMarketRarity.VERY_COMMON;
import static mekhq.campaign.market.enums.UnitMarketRarity.VERY_RARE;
import static mekhq.campaign.market.enums.UnitMarketType.getPricePercentage;
import static mekhq.campaign.randomEvents.GrayMonday.isGrayMonday;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getAmazingColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import megamek.client.ratgenerator.MissionRole;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
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
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

public class AtBMonthlyUnitMarket extends AbstractUnitMarket {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Market";

    //region Constructors
    public AtBMonthlyUnitMarket() {
        super(UnitMarketMethod.ATB_MONTHLY);
    }
    //endregion Constructors

    //region Process New Day

    /**
     * This market runs monthly, so it only executes removal and generation on the first day of the month
     *
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

        Faction faction = campaign.getFaction();
        int rarityModifier = campaign.getCampaignOptions().getUnitMarketRarityModifier();

        // Civilian Market
        addOffers(campaign, getMarketItemCount(campaign, UBIQUITOUS, rarityModifier),
              UnitMarketType.CIVILIAN, UnitType.TANK, faction, IUnitRating.DRAGOON_A, 2);

        // Open Market
        addOffers(campaign, getMarketItemCount(campaign, UNCOMMON, rarityModifier),
              UnitMarketType.OPEN, UnitType.MEK, faction, IUnitRating.DRAGOON_F, 1);

        addOffers(campaign, getMarketItemCount(campaign, UNCOMMON, rarityModifier),
              UnitMarketType.OPEN, UnitType.AEROSPACE_FIGHTER, faction, IUnitRating.DRAGOON_F, 1);

        addOffers(campaign, getMarketItemCount(campaign, VERY_COMMON, rarityModifier),
              UnitMarketType.OPEN, UnitType.TANK, faction, IUnitRating.DRAGOON_F, 1);

        addOffers(campaign, getMarketItemCount(campaign, COMMON, rarityModifier),
              UnitMarketType.OPEN, UnitType.CONV_FIGHTER, faction, IUnitRating.DRAGOON_F, 1);

        addOffers(campaign, getMarketItemCount(campaign, UNCOMMON, rarityModifier),
              UnitMarketType.OPEN, UnitType.BATTLE_ARMOR, faction, IUnitRating.DRAGOON_F, 1);

        addOffers(campaign, getMarketItemCount(campaign, UBIQUITOUS, rarityModifier),
              UnitMarketType.OPEN, UnitType.INFANTRY, faction, IUnitRating.DRAGOON_F, 1);

        addOffers(campaign, getMarketItemCount(campaign, VERY_RARE, rarityModifier),
              UnitMarketType.OPEN, UnitType.DROPSHIP, faction, IUnitRating.DRAGOON_F, 4);

        addOffers(campaign, getMarketItemCount(campaign, MYTHIC, rarityModifier),
              UnitMarketType.OPEN, UnitType.JUMPSHIP, faction, IUnitRating.DRAGOON_F, 4);

        if ((contract != null)
                  && (campaign.getLocalDate().isAfter(contract.getStartDate().minusDays(1)))) {
            // Employer Market
            faction = contract.getEmployerFaction();

            int standingsModifier = 0;
            if (campaign.getCampaignOptions().isUseFactionStandingUnitMarketSafe()) {
                FactionStandings factionStandings = campaign.getFactionStandings();
                double regard = factionStandings.getRegardForFaction(contract.getEmployerCode(), true);
                standingsModifier = FactionStandingUtilities.getUnitMarketRarityModifier(regard);
            }

            int totalModifier = rarityModifier + standingsModifier;

            addOffers(campaign, getMarketItemCount(campaign, RARE, totalModifier),
                  UnitMarketType.EMPLOYER, UnitType.MEK, faction, IUnitRating.DRAGOON_D, -1);

            addOffers(campaign, getMarketItemCount(campaign, RARE, totalModifier),
                  UnitMarketType.EMPLOYER, UnitType.AEROSPACE_FIGHTER, faction, IUnitRating.DRAGOON_D, -1);

            addOffers(campaign, getMarketItemCount(campaign, COMMON, totalModifier),
                  UnitMarketType.EMPLOYER, UnitType.TANK, faction, IUnitRating.DRAGOON_D, -1);

            addOffers(campaign, getMarketItemCount(campaign, UNCOMMON, totalModifier),
                  UnitMarketType.EMPLOYER, UnitType.CONV_FIGHTER, faction, IUnitRating.DRAGOON_D, -1);

            addOffers(campaign, getMarketItemCount(campaign, RARE, totalModifier),
                  UnitMarketType.EMPLOYER, UnitType.BATTLE_ARMOR, faction, IUnitRating.DRAGOON_D, -1);

            addOffers(campaign, getMarketItemCount(campaign, UBIQUITOUS, totalModifier),
                  UnitMarketType.EMPLOYER, UnitType.INFANTRY, faction, IUnitRating.DRAGOON_D, -1);

            // Unwanted Salvage Market
            faction = contract.getEnemy();

            addOffers(campaign, getMarketItemCount(campaign, RARE, totalModifier),
                  UnitMarketType.EMPLOYER, UnitType.MEK, faction, IUnitRating.DRAGOON_F, 2);

            addOffers(campaign, getMarketItemCount(campaign, RARE, totalModifier),
                  UnitMarketType.EMPLOYER, UnitType.AEROSPACE_FIGHTER, faction, IUnitRating.DRAGOON_F, 2);

            addOffers(campaign, getMarketItemCount(campaign, UNCOMMON, totalModifier),
                  UnitMarketType.EMPLOYER, UnitType.TANK, faction, IUnitRating.DRAGOON_F, 2);

            addOffers(campaign, getMarketItemCount(campaign, UNCOMMON, totalModifier),
                  UnitMarketType.EMPLOYER, UnitType.CONV_FIGHTER, faction, IUnitRating.DRAGOON_F, 2);

            addOffers(campaign, getMarketItemCount(campaign, RARE, totalModifier),
                  UnitMarketType.EMPLOYER, UnitType.BATTLE_ARMOR, faction, IUnitRating.DRAGOON_F, 2);

            addOffers(campaign, getMarketItemCount(campaign, UBIQUITOUS, totalModifier),
                  UnitMarketType.EMPLOYER, UnitType.INFANTRY, faction, IUnitRating.DRAGOON_F, 2);
        }

        // Mercenary Market
        if (!campaign.getFaction().isClan()) {
            faction = Factions.getInstance().getFaction("MERC");

            int modifier = 1;

            if (campaign.getFaction().isMercenary()) {
                modifier = -1;
            }

            addOffers(campaign, getMarketItemCount(campaign, UNCOMMON, rarityModifier),
                  UnitMarketType.MERCENARY, UnitType.MEK, faction, IUnitRating.DRAGOON_C, modifier);

            addOffers(campaign, getMarketItemCount(campaign, UNCOMMON, rarityModifier),
                  UnitMarketType.MERCENARY, UnitType.AEROSPACE_FIGHTER, faction, IUnitRating.DRAGOON_C, modifier);

            addOffers(campaign, getMarketItemCount(campaign, VERY_COMMON, rarityModifier),
                  UnitMarketType.MERCENARY, UnitType.TANK, faction, IUnitRating.DRAGOON_C, modifier);

            addOffers(campaign, getMarketItemCount(campaign, UNCOMMON, rarityModifier),
                  UnitMarketType.MERCENARY, UnitType.CONV_FIGHTER, faction, IUnitRating.DRAGOON_C, modifier);

            addOffers(campaign, getMarketItemCount(campaign, UNCOMMON, rarityModifier),
                  UnitMarketType.MERCENARY, UnitType.BATTLE_ARMOR, faction, IUnitRating.DRAGOON_C, modifier);

            addOffers(campaign, getMarketItemCount(campaign, UBIQUITOUS, rarityModifier),
                  UnitMarketType.MERCENARY, UnitType.INFANTRY, faction, IUnitRating.DRAGOON_C, modifier);
        }

        // Factory Market
        if (campaign.getAtBUnitRatingMod() >= IUnitRating.DRAGOON_B) {
            faction = ObjectUtility.getRandomItem(campaign.getCurrentSystem()
                                                        .getFactionSet(campaign.getLocalDate()));

            if ((!campaign.getFaction().isClan()) && (faction != null) && (!faction.isClan())) {
                addOffers(campaign, getMarketItemCount(campaign, RARE, rarityModifier),
                      UnitMarketType.FACTORY, UnitType.MEK, faction, IUnitRating.DRAGOON_A, 2);

                addOffers(campaign, getMarketItemCount(campaign, RARE, rarityModifier),
                      UnitMarketType.FACTORY, UnitType.AEROSPACE_FIGHTER, faction, IUnitRating.DRAGOON_A, 2);

                addOffers(campaign, getMarketItemCount(campaign, COMMON, rarityModifier),
                      UnitMarketType.FACTORY, UnitType.TANK, faction, IUnitRating.DRAGOON_A, 2);

                addOffers(campaign, getMarketItemCount(campaign, UNCOMMON, rarityModifier),
                      UnitMarketType.FACTORY, UnitType.CONV_FIGHTER, faction, IUnitRating.DRAGOON_A, 2);

                addOffers(campaign, getMarketItemCount(campaign, RARE, rarityModifier),
                      UnitMarketType.FACTORY, UnitType.BATTLE_ARMOR, faction, IUnitRating.DRAGOON_A, 2);

                addOffers(campaign, getMarketItemCount(campaign, UBIQUITOUS, rarityModifier),
                      UnitMarketType.FACTORY, UnitType.INFANTRY, faction, IUnitRating.DRAGOON_A, 2);
            }
        }

        faction = campaign.getFaction();

        // Clan Factory Market
        if ((faction.isClan()) &&
                  (campaign.getCurrentSystem().getFactionSet(campaign.getLocalDate()).contains(faction))) {
            addOffers(campaign, getMarketItemCount(campaign, VERY_COMMON, rarityModifier),
                  UnitMarketType.FACTORY, UnitType.MEK, faction, IUnitRating.DRAGOON_A, -4);

            addOffers(campaign, getMarketItemCount(campaign, COMMON, rarityModifier),
                  UnitMarketType.FACTORY, UnitType.AEROSPACE_FIGHTER, faction, IUnitRating.DRAGOON_A, -4);

            addOffers(campaign, getMarketItemCount(campaign, UNCOMMON, rarityModifier),
                  UnitMarketType.FACTORY, UnitType.TANK, faction, IUnitRating.DRAGOON_A, -4);

            addOffers(campaign, getMarketItemCount(campaign, VERY_COMMON, rarityModifier),
                  UnitMarketType.FACTORY, UnitType.BATTLE_ARMOR, faction, IUnitRating.DRAGOON_A, -4);

            addOffers(campaign, getMarketItemCount(campaign, UBIQUITOUS, rarityModifier),
                  UnitMarketType.FACTORY, UnitType.INFANTRY, faction, IUnitRating.DRAGOON_A, -4);

            addOffers(campaign, getMarketItemCount(campaign, VERY_RARE, rarityModifier),
                  UnitMarketType.FACTORY, UnitType.DROPSHIP, faction, IUnitRating.DRAGOON_A, 0);

            addOffers(campaign, getMarketItemCount(campaign, MYTHIC, rarityModifier),
                  UnitMarketType.FACTORY, UnitType.JUMPSHIP, faction, IUnitRating.DRAGOON_A, 0);
        }

        // Black Market
        if (!campaign.getFaction().isClan()) {
            faction = ObjectUtility.getRandomItem(campaign.getCurrentSystem()
                                                        .getFactionSet(campaign.getLocalDate()));

            addOffers(campaign, getMarketItemCount(campaign, VERY_RARE, rarityModifier),
                  UnitMarketType.BLACK_MARKET, UnitType.MEK, faction, IUnitRating.DRAGOON_A, -8);

            addOffers(campaign, getMarketItemCount(campaign, VERY_RARE, rarityModifier),
                  UnitMarketType.BLACK_MARKET, UnitType.AEROSPACE_FIGHTER, faction, IUnitRating.DRAGOON_A, -8);

            addOffers(campaign, getMarketItemCount(campaign, RARE, rarityModifier),
                  UnitMarketType.BLACK_MARKET, UnitType.TANK, faction, IUnitRating.DRAGOON_A, -8);

            addOffers(campaign, getMarketItemCount(campaign, RARE, rarityModifier),
                  UnitMarketType.BLACK_MARKET, UnitType.CONV_FIGHTER, faction, IUnitRating.DRAGOON_A, -8);

            addOffers(campaign, getMarketItemCount(campaign, VERY_RARE, rarityModifier),
                  UnitMarketType.BLACK_MARKET, UnitType.BATTLE_ARMOR, faction, IUnitRating.DRAGOON_A, -8);

            addOffers(campaign, getMarketItemCount(campaign, UBIQUITOUS, rarityModifier),
                  UnitMarketType.BLACK_MARKET, UnitType.INFANTRY, faction, IUnitRating.DRAGOON_A, -8);
        }

        writeRefreshReport(campaign);
    }

    /**
     * Returns a count of market items based on the specified rarity.
     *
     * @param rarity         the rarity of the market items
     * @param rarityModifier the unit count modifier specified in campaign options
     *
     * @return an integer representing the count of market items
     */
    private int getMarketItemCount(Campaign campaign, UnitMarketRarity rarity, int rarityModifier) {
        int totalRarity = rarity.getRarityValue() + rarityModifier;

        if (isGrayMonday(campaign.getLocalDate(), campaign.getCampaignOptions().isSimulateGrayMonday())) {
            totalRarity -= 4;
        }

        return Compute.d6(1)
                     + totalRarity
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

        if (num <= 1) {
            return;
        }

        for (int i = 0; i < num; i++) {
            final Collection<EntityMovementMode> movementModes = new ArrayList<>();
            final Collection<MissionRole> missionRoles = new ArrayList<>();

            if (unitType == UnitType.TANK) {
                if (market.isCivilianMarket()) {
                    int roll = Compute.randomInt(3);
                    switch (roll) {
                        case 0 -> missionRoles.add(MissionRole.CIVILIAN);
                        case 1 -> missionRoles.add(MissionRole.SUPPORT);
                        case 2 -> missionRoles.add(MissionRole.CARGO);
                    }
                } else {
                    movementModes.addAll(IUnitGenerator.MIXED_TANK_VTOL);
                    int specialUnitChance = campaign.getCampaignOptions().getUnitMarketArtilleryUnitChance();
                    if (specialUnitChance != 0) {
                        if ((specialUnitChance == 1) || (Compute.randomInt(specialUnitChance) == 0)) {
                            missionRoles.add(MissionRole.ARTILLERY);
                        }
                    }
                }
            }

            String unitName = addSingleUnit(campaign,
                  market,
                  unitType,
                  faction,
                  quality,
                  movementModes,
                  missionRoles,
                  getPricePercentage(priceModifier));
            if (unitName != null) {
                if (unitType == UnitType.DROPSHIP) {
                    String key = "AtBMonthlyUnitMarket.dropShip.report";
                    String report = getFormattedTextAt(RESOURCE_BUNDLE, key,
                          spanOpeningWithCustomColor(getPositiveColor()), CLOSING_SPAN_TAG);
                    campaign.addReport(GENERAL, report);
                } else if (unitType == UnitType.JUMPSHIP) {
                    String key = "AtBMonthlyUnitMarket.jumpShip.report";
                    String report = getFormattedTextAt(RESOURCE_BUNDLE, key,
                          spanOpeningWithCustomColor(getAmazingColor()), CLOSING_SPAN_TAG);
                    campaign.addReport(GENERAL, report);
                }
            }
        }
    }

    /**
     * This generates a random weight using the static weight generation methods in this market
     *
     * @param campaign the campaign to generate the unit weight based on
     * @param unitType the unit type to determine the format of weight to generate
     * @param faction  the faction to generate the weight for
     *
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
     *
     * @param campaign the campaign to use in determining the offers to remove
     */
    @Override
    public void removeUnitOffers(final Campaign campaign) {
        getOffers().clear();
    }
    //endregion Offer Removal
    //endregion Process New Day
}
