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

import jakarta.annotation.Nullable;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.Engine;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.EnginePart;
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
 *     <li>A fusion engine in a combat vehicle:
 *     {@link CampaignOption#COMBAT_VEHICLE_FUSION_ENGINE_PRICE_MULTIPLIER}.</li>
 *     <li>An XL engine: {@link CampaignOption#XL_ENGINE_PRICE_MULTIPLIER}.</li>
 *     <li>A light fusion engine: {@link CampaignOption#LIGHT_ENGINE_PRICE_MULTIPLIER}.</li>
 * </ul>
 *
 * <p>Every multiplier that applies stacks additively: ×5 and ×1.25 together make ×5.25. Outside the listed years,
 * nothing replaces the era multipliers, so tech rating E and F fall back to their usual multipliers and Clan tech has
 * no multiplier at all.</p>
 *
 * <p>The multiplier for a unit covers its whole price. Engine multipliers also apply to engines bought as parts.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class AcquisitionPriceMultipliers {
    static final int SUCCESSION_WARS_START_YEAR = 2860;
    static final int SUCCESSION_WARS_END_YEAR = 3040;
    static final int CLAN_INVASION_START_YEAR = 3049;
    static final int CLAN_INVASION_END_YEAR = 3070;

    /** The engine type {@link megamek.common.loaders.MekSummaryCache} records for a unit without an engine. */
    private static final int NO_ENGINE_TYPE = -1;

    private AcquisitionPriceMultipliers() {
    }

    /**
     * @param campaignOptions the campaign options holding the multipliers
     * @param faction         the buying campaign's faction, or {@code null} to treat it as neither Clan nor ComStar
     * @param gameYear        the current year
     * @param entity          the unit's entity
     *
     * @return the acquisition price multiplier for the whole unit
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getUnitMultiplier(CampaignOptions campaignOptions, @Nullable Faction faction, int gameYear,
          Entity entity) {
        int engineType = entity.hasEngine() ? entity.getEngine().getEngineType() : NO_ENGINE_TYPE;
        return getMultiplier(campaignOptions, faction, gameYear, entity.isClan() || entity.isMixedTech(),
              entity.getTechRating(), engineType, entity.isCombatVehicle());
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
        int unitTypeCode = UnitType.determineUnitTypeCode(summary.getUnitType());
        boolean isVehicle = (unitTypeCode == UnitType.TANK) || (unitTypeCode == UnitType.VTOL)
                                  || (unitTypeCode == UnitType.NAVAL);
        return getMultiplier(campaignOptions, faction, gameYear, summary.isClan() || summary.isMixedTech(),
              summary.getTechRating(), summary.getEngineType(), isVehicle && !summary.isSupport());
    }

    /**
     * @param campaignOptions the campaign options holding the multipliers
     * @param faction         the buying campaign's faction, or {@code null} to treat it as neither Clan nor ComStar
     * @param gameYear        the current year
     * @param part            the part
     *
     * @return the acquisition price multiplier for the part; only engine parts pick up the engine multipliers
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getPartMultiplier(CampaignOptions campaignOptions, @Nullable Faction faction, int gameYear,
          Part part) {
        int engineType = NO_ENGINE_TYPE;
        boolean isCombatVehicleEngine = false;
        if (part instanceof EnginePart enginePart) {
            Engine engine = enginePart.getEngine();
            engineType = engine.getEngineType();
            isCombatVehicleEngine = engine.hasFlag(Engine.TANK_ENGINE) && !engine.hasFlag(Engine.SUPPORT_VEE_ENGINE);
        }

        return getMultiplier(campaignOptions, faction, gameYear, part.getTechBase() == TechBase.CLAN,
              part.getTechRating(), engineType, isCombatVehicleEngine);
    }

    /**
     * @param campaignOptions the campaign options holding the multipliers
     * @param faction         the buying campaign's faction, or {@code null} to treat it as neither Clan nor ComStar
     * @param gameYear        the current year
     * @param isClanTech      whether the item is, or carries, Clan tech
     * @param techRating      the item's tech rating, or {@code null} if it is unknown
     * @param engineType      the item's engine type, such as {@link Engine#XL_ENGINE}, or a negative value for none
     * @param isCombatVehicle whether the item is a combat vehicle, or an engine built for one
     *
     * @return the sum of every applicable multiplier's increase over ×1, never below zero
     *
     * @author Illiani
     * @since 0.51.01
     */
    static double getMultiplier(CampaignOptions campaignOptions, @Nullable Faction faction, int gameYear,
          boolean isClanTech, @Nullable TechRating techRating, int engineType, boolean isCombatVehicle) {
        boolean isClanFaction = (faction != null) && faction.isClan();
        // The Word of Blake grew out of ComStar and kept its stockpiles of advanced technology
        boolean isComStarFaction = (faction != null) && faction.isComStarOrWoB();

        double multiplier = 1.0;
        if (isClanTech && !isClanFaction) {
            if ((gameYear >= CLAN_INVASION_START_YEAR) && (gameYear <= CLAN_INVASION_END_YEAR)) {
                multiplier += campaignOptions.get(CampaignOption.CLAN_TECH_INVASION_PRICE_MULTIPLIER) - 1.0;
            } else if (gameYear > CLAN_INVASION_END_YEAR) {
                multiplier += campaignOptions.get(CampaignOption.CLAN_TECH_POST_INVASION_PRICE_MULTIPLIER) - 1.0;
            }
        }

        if ((techRating == TechRating.E) || (techRating == TechRating.F)) {
            boolean isSuccessionWars = (gameYear >= SUCCESSION_WARS_START_YEAR)
                                             && (gameYear <= SUCCESSION_WARS_END_YEAR);
            if (isSuccessionWars && !isClanFaction && !isComStarFaction) {
                multiplier += campaignOptions.get(CampaignOption.SUCCESSION_WARS_ADVANCED_TECH_PRICE_MULTIPLIER) - 1.0;
            } else if (techRating == TechRating.E) {
                multiplier += campaignOptions.get(CampaignOption.TECH_RATING_E_PRICE_MULTIPLIER) - 1.0;
            } else {
                multiplier += campaignOptions.get(CampaignOption.TECH_RATING_F_PRICE_MULTIPLIER) - 1.0;
            }
        }

        if (engineType >= 0) {
            if (isCombatVehicle && Engine.isFusion(engineType)) {
                multiplier += campaignOptions.get(CampaignOption.COMBAT_VEHICLE_FUSION_ENGINE_PRICE_MULTIPLIER) - 1.0;
            }

            if (engineType == Engine.XL_ENGINE) {
                multiplier += campaignOptions.get(CampaignOption.XL_ENGINE_PRICE_MULTIPLIER) - 1.0;
            } else if (engineType == Engine.LIGHT_ENGINE) {
                multiplier += campaignOptions.get(CampaignOption.LIGHT_ENGINE_PRICE_MULTIPLIER) - 1.0;
            }
        }

        return Math.max(0.0, multiplier);
    }
}
