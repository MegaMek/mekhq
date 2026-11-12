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
package mekhq.campaign.finances;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.Part;
import mekhq.campaign.universe.Faction;

/**
 * The acquisition price multipliers from Campaign Operations (CO p.14), each set by its own campaign option.
 *
 * <ul>
 *     <li>Clan tech bought by a non-Clan campaign: {@link CampaignOption#CLAN_TECH_INVASION_PRICE_MULTIPLIER} from
 *     {@value #CLAN_INVASION_START_YEAR} to {@value #CLAN_INVASION_END_YEAR}, then
 *     {@link CampaignOption#CLAN_TECH_POST_INVASION_PRICE_MULTIPLIER}.</li>
 *     <li>Tech rating E and F bought by a campaign that is neither Clan nor ComStar (Word of Blake counts as
 *     ComStar), from
 *     {@value #SUCCESSION_WARS_START_YEAR} to {@value #SUCCESSION_WARS_END_YEAR}:
 *     {@link CampaignOption#SUCCESSION_WARS_ADVANCED_TECH_PRICE_MULTIPLIER}. Otherwise
 *     {@link CampaignOption#TECH_RATING_E_PRICE_MULTIPLIER} or
 *     {@link CampaignOption#TECH_RATING_F_PRICE_MULTIPLIER}.</li>
 * </ul>
 *
 * <p>Every surcharge that applies stacks additively: ×5 and ×1.25 together make ×5.25. Discounts (multipliers below
 * ×1) are multiplied in afterwards, so no combination of them can make an item free: ×0.5 and ×0.5 make ×0.25. Outside
 * the listed years, nothing replaces the era multipliers, so tech rating E and F fall back to their usual multipliers
 * and Clan tech has no multiplier at all.</p>
 *
 * <p>The multiplier for a unit covers its whole price. Parts only pay the multipliers when the unofficial
 * {@link CampaignOption#APPLY_ACQUISITION_MULTIPLIERS_TO_PARTS} option is on.</p>
 *
 * <p>A mixed-tech unit pays none of the Clan tech multipliers, whatever its chassis: its mixed-tech unit price
 * multiplier already covers its Clan equipment.</p>
 *
 * <p>These multipliers only apply to what the campaign pays when buying and receives when selling (see
 * {@link mekhq.campaign.unit.Unit#getPurchaseCost()}, {@link mekhq.campaign.unit.Unit#getSalePrice()},
 * {@link Part#getPurchaseCost()} and {@link Part#getSalePrice()}), and to the spare parts it buys each month for its
 * units ({@link mekhq.campaign.unit.Unit#getSparePartsCost()}, CO p.24), never to the value of what it owns
 * (maintenance, contract pay, battle loss compensation, salvage). They only apply under the
 * {@link mekhq.campaign.finances.enums.UnitCostModel#TOTAL_WARFARE} {@link CampaignOption#UNIT_COST_MODEL}; every
 * other model leaves them at ×1.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class AcquisitionPriceMultipliers {
    static final int SUCCESSION_WARS_START_YEAR = 2860;
    static final int SUCCESSION_WARS_END_YEAR = 3040;
    static final int CLAN_INVASION_START_YEAR = 3049;
    static final int CLAN_INVASION_END_YEAR = 3070;

    private AcquisitionPriceMultipliers() {
    }

    /**
     * @param campaignOptions the campaign options holding the multipliers
     * @param faction         the buying campaign's faction, or {@code null} to treat it as neither Clan nor ComStar
     * @param gameYear        the current year
     * @param entity          the unit's entity
     *
     * @return the acquisition price multiplier for the whole unit. Only a pure Clan unit pays the Clan tech
     *       multiplier: a mixed-tech unit, on either chassis, is covered by its mixed-tech unit price multiplier.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getUnitMultiplier(CampaignOptions campaignOptions, @Nullable Faction faction, int gameYear,
          Entity entity) {
        return getUnitMultiplier(campaignOptions, faction, gameYear, entity.isClan(), entity.isMixedTech(),
              entity.getTechRating());
    }

    /**
     * For callers that cache a unit's traits rather than keep its entity loaded, such as unit market offers.
     *
     * @param campaignOptions the campaign options holding the multipliers
     * @param faction         the buying campaign's faction, or {@code null} to treat it as neither Clan nor ComStar
     * @param gameYear        the current year
     * @param isClan          whether the unit is Clan tech
     * @param isMixedTech     whether the unit is mixed tech, which never pays the Clan tech multiplier
     * @param techRating      the unit's tech rating, or {@code null} if it is unknown
     *
     * @return the acquisition price multiplier for the whole unit, as
     *       {@link #getUnitMultiplier(CampaignOptions, Faction, int, Entity)} works it out
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getUnitMultiplier(CampaignOptions campaignOptions, @Nullable Faction faction, int gameYear,
          boolean isClan, boolean isMixedTech, @Nullable TechRating techRating) {
        return getMultiplier(campaignOptions, faction, gameYear, isClan && !isMixedTech, techRating);
    }

    /**
     * @param campaignOptions the campaign options holding the multipliers
     * @param faction         the buying campaign's faction, or {@code null} to treat it as neither Clan nor ComStar
     * @param gameYear        the current year
     * @param summary         the unit's summary
     *
     * @return the acquisition price multiplier for the whole unit, as
     *       {@link #getUnitMultiplier(CampaignOptions, Faction, int, Entity)} works it out once the unit is loaded
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getUnitMultiplier(CampaignOptions campaignOptions, @Nullable Faction faction, int gameYear,
          MekSummary summary) {
        boolean isClanTech = summary.isClan() && !summary.isMixedTech();
        return getMultiplier(campaignOptions, faction, gameYear, isClanTech, summary.getTechRating());
    }

    /**
     * @param campaignOptions the campaign options holding the multipliers
     * @param faction         the buying campaign's faction, or {@code null} to treat it as neither Clan nor ComStar
     * @param gameYear        the current year
     * @param part            the part
     *
     * @return the acquisition price multiplier for the part; ×1 unless
     *       {@link CampaignOption#APPLY_ACQUISITION_MULTIPLIERS_TO_PARTS} is on
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getPartMultiplier(CampaignOptions campaignOptions, @Nullable Faction faction, int gameYear,
          Part part) {
        if (!campaignOptions.get(CampaignOption.APPLY_ACQUISITION_MULTIPLIERS_TO_PARTS)) {
            return 1.0;
        }

        return getMultiplier(campaignOptions, faction, gameYear, part.getTechBase() == TechBase.CLAN,
              part.getTechRating());
    }

    /**
     * @param campaignOptions the campaign options holding the multipliers
     * @param faction         the buying campaign's faction, or {@code null} to treat it as neither Clan nor ComStar
     * @param gameYear        the current year
     * @param isClanTech      whether the item is Clan tech
     * @param techRating      the item's tech rating, or {@code null} if it is unknown
     *
     * @return the sum of every applicable surcharge's increase over ×1, times every applicable discount; ×1 unless
     *       the campaign uses the {@link mekhq.campaign.finances.enums.UnitCostModel#TOTAL_WARFARE} unit cost model
     *
     * @author Illiani
     * @since 0.51.01
     */
    static double getMultiplier(CampaignOptions campaignOptions, @Nullable Faction faction, int gameYear,
          boolean isClanTech, @Nullable TechRating techRating) {
        if (!campaignOptions.get(CampaignOption.UNIT_COST_MODEL).isTotalWarfare()) {
            return 1.0;
        }

        boolean isClanFaction = (faction != null) && faction.isClan();
        // The Word of Blake grew out of ComStar and kept its stockpiles of advanced technology
        boolean isComStarFaction = (faction != null) && faction.isComStarOrWoB();

        List<Double> multipliers = new ArrayList<>();
        if (isClanTech && !isClanFaction) {
            if ((gameYear >= CLAN_INVASION_START_YEAR) && (gameYear <= CLAN_INVASION_END_YEAR)) {
                multipliers.add(campaignOptions.get(CampaignOption.CLAN_TECH_INVASION_PRICE_MULTIPLIER));
            } else if (gameYear > CLAN_INVASION_END_YEAR) {
                multipliers.add(campaignOptions.get(CampaignOption.CLAN_TECH_POST_INVASION_PRICE_MULTIPLIER));
            }
        }

        if ((techRating == TechRating.E) || (techRating == TechRating.F)) {
            boolean isSuccessionWars = (gameYear >= SUCCESSION_WARS_START_YEAR)
                                             && (gameYear <= SUCCESSION_WARS_END_YEAR);
            if (isSuccessionWars && !isClanFaction && !isComStarFaction) {
                multipliers.add(campaignOptions.get(CampaignOption.SUCCESSION_WARS_ADVANCED_TECH_PRICE_MULTIPLIER));
            } else if (techRating == TechRating.E) {
                multipliers.add(campaignOptions.get(CampaignOption.TECH_RATING_E_PRICE_MULTIPLIER));
            } else {
                multipliers.add(campaignOptions.get(CampaignOption.TECH_RATING_F_PRICE_MULTIPLIER));
            }
        }

        return combine(multipliers);
    }

    /**
     * Surcharges stack additively, as Campaign Operations does. Adding discounts the same way would let two ×0.5
     * discounts make an item free, so they are multiplied in instead.
     *
     * @param multipliers the applicable multipliers
     *
     * @return the sum of every surcharge's increase over ×1, times every discount; never below zero
     */
    private static double combine(List<Double> multipliers) {
        double surcharge = 1.0;
        double discount = 1.0;
        for (double multiplier : multipliers) {
            if (multiplier >= 1.0) {
                surcharge += multiplier - 1.0;
            } else {
                discount *= Math.max(0.0, multiplier);
            }
        }
        return surcharge * discount;
    }
}
