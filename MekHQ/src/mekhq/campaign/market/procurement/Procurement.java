/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.procurement;

import megamek.common.Compute;
import megamek.common.ITechnology;
import megamek.common.SimpleTechLevel;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.universe.Faction;

import java.util.ArrayList;
import java.util.List;

import static megamek.common.ITechnology.*;
import static megamek.common.SimpleTechLevel.INTRO;
import static megamek.common.SimpleTechLevel.STANDARD;

/**
 * The Procurement class encapsulates the logic for deciding the availability
 * of parts based on factors such as era, technologies, and treaties between
 * various factions. It is capable of making procurement checks to simulate
 * rarity and scarcity of parts in a given time frame.
 */
public class Procurement {
    private final int negotiatorSkillRating;
    private final int gameYear;
    private final int techEra;
    private final Faction originFaction;
    private final int factionTechCode;
    private static final MMLogger logger = MMLogger.create(Procurement.class);

    /**
     * Procurement constructor.
     * Initializes class instance with negotiator skill rating, game year, and originating faction.
     *
     * @param negotiatorSkillRating the skill rating of the negotiator.
     * @param gameYear the current year of the game.
     * @param originFaction the faction from where procurement is initiated.
     */
    public Procurement(int negotiatorSkillRating, int gameYear, Faction originFaction) {
        this.negotiatorSkillRating = negotiatorSkillRating;
        this.gameYear = gameYear;
        this.originFaction = originFaction;

        factionTechCode = getFactionTechCode(originFaction);
        techEra = getTechEra(gameYear);
    }

    /**
     * Given a faction, returns the corresponding faction code.
     *
     * @param faction Faction instance
     * @return returns corresponding faction code.
     */
    public static int getFactionTechCode(Faction faction) {
        int allCodesCount = MM_FACTION_CODES.length;
        for (int i = 0; i < allCodesCount; i++) {
            if (MM_FACTION_CODES[i].equals(faction.getShortName())) {
                return i;
            }
        }

        logger.info("Unable to retrieve Tech Faction. Using fallback.");

        if (faction.isClan()) {
            logger.info("Returning: Clan");
            return F_CLAN;
        } else if (faction.isPeriphery()) {
            logger.info("Returning: Periphery");
            return F_PER;
        } else {
            logger.info("Returning: Inner Sphere");
            return F_IS;
        }
    }

    /**
     * Makes procurement checks for a list of parts and returns successful parts.
     *
     * @param parts List of parts that require procurement checks
     * @param useHardExtinction a boolean flag that indicates whether to enforce hard extinctions.
     * @param isResupply Flag indicating if procurement is for resupplying parts
     * @return List of parts that were successful in the procurement checks
     */
    public List<Part> makeProcurementChecks(List<Part> parts, boolean useHardExtinction, boolean isResupply) {
        List<Part> successfulParts = new ArrayList<>();

        for (Part part : parts) {
            int targetNumber = getProcurementTargetNumber(part, useHardExtinction, isResupply);
            int roll = Compute.d6(2);
            if (roll >= targetNumber) {
                successfulParts.add(part);
            }
        }

        logger.info(successfulParts);

        return successfulParts;
    }

    /**
     * Given a part, this method calculates the procurement target number.
     *
     * @param part The part for which the procurement target number is to be calculated.
     * @param useHardExtinction a boolean flag that indicates whether to enforce hard extinctions.
     * @param isResupply Flag indicating if procurement is for resupplying parts
     * @return The calculated procurement target number
     */
    private int getProcurementTargetNumber(Part part, boolean useHardExtinction, boolean isResupply) {
        // Get the base target number
        int targetNumber;
        if (part instanceof AmmoBin) {
            targetNumber = getConsumableBaseTargetNumber(part, useHardExtinction);
        } else {
            targetNumber = getBaseTargetNumber(part, useHardExtinction);
        }

        if (targetNumber > 12) {
            return targetNumber;
        }

        // Get the modifiers
        if (part.isClan() && !originFaction.isClan()) {
            if (gameYear >= 3050 && gameYear <= 3070) {
                targetNumber += 3;
            }
        }

        targetNumber += getNegotiatorModifier();

        if (isResupply) {
            targetNumber -= 2;
        }

        return Math.max(2, targetNumber);
    }

    /**
     * Returns negotiator modifier based on the negotiator's skill rating.
     *
     * @return Modifier for the procurement process based on negotiator's skill level
     */
    private int getNegotiatorModifier() {
        if (negotiatorSkillRating == SkillLevel.NONE.ordinal()) {
            return 4;
        }

        if (negotiatorSkillRating == SkillLevel.ULTRA_GREEN.ordinal()) {
            return 3;
        }

        if (negotiatorSkillRating == SkillLevel.VETERAN.ordinal()) {
            return -2;
        }

        if (negotiatorSkillRating >= SkillLevel.ELITE.ordinal()) {
            return -3;
        }

        return 0;
    }

    /**
     * Returns the base target number for the given consumable part.
     *
     * @param part The part for which the target number should be calculated.
     * @param useHardExtinction a boolean flag that indicates whether to enforce hard extinctions.
     * @return The calculated base target number.
     */
    private int getConsumableBaseTargetNumber(Part part, boolean useHardExtinction) {
        int availability = getAvailability(part, useHardExtinction);

        int targetNumber =  switch (availability) {
            case RATING_A -> 2;
            case RATING_B -> 3;
            case RATING_C -> 4;
            case RATING_D -> 6;
            case RATING_E -> 8;
            case RATING_F -> 10;
            // This value is deliberately impossible on 2d6
            default -> 13; // X or F*
        };

        SimpleTechLevel techLevel = part.getStaticTechLevel();

        if (techLevel == INTRO || techLevel == STANDARD) {
            targetNumber -= 2;
        }

        if (techLevel == SimpleTechLevel.ADVANCED) {
            targetNumber--;
        }

        return Math.max(2, targetNumber);
    }

    /**
     * Returns the base target number for the given part.
     *
     * @param part The part for which the target number should be calculated.
     * @param useHardExtinction a boolean flag that indicates whether to enforce hard extinctions.
     * @return The calculated base target number.
     */
    private int getBaseTargetNumber(Part part, boolean useHardExtinction) {
        int availability = getAvailability(part, useHardExtinction);

        return switch (availability) {
            case RATING_A -> 3;
            case RATING_B -> 4;
            case RATING_C -> 6;
            case RATING_D -> 8;
            case RATING_E -> 10;
            case RATING_F -> 11;
            // This value is deliberately impossible on 2d6
            default -> 13; // X or F*
        };
    }

    /**
     * Returns the availability rate for the given part.
     *
     * @param part The part for which the availability is to be calculated.
     * @param useHardExtinction a boolean flag that indicates whether to enforce hard extinctions
     * @return The calculated availability rate.
     */
    private int getAvailability(Part part, boolean useHardExtinction) {
        int availability = part.calcYearAvailability(gameYear, originFaction.isClan(), factionTechCode);

        if (part.getTechBase() == TECH_BASE_IS) {
            availability = getInnerSphereTechBaseRating(part, availability);
            return performTrulyExtinctCheck(part, availability, useHardExtinction);
        }

        if (part.getTechBase() == TECH_BASE_CLAN) {
            availability = getClanTechBaseRating(part, availability);
            return performTrulyExtinctCheck(part, availability, useHardExtinction);
        }

        // Tech Base: All
        availability = getCommonTechBaseRating(part, availability);
        return performTrulyExtinctCheck(part, availability, useHardExtinction);
    }

    /**
     * Assess the extinction state of a specific part and adjusts its availability
     * rating based on whether the part is truly extinct or not.
     *
     * @param part The part for which to assess the extinction state.
     * @param availability The calculated availability rate for the part.
     * @param useHardExtinction a boolean flag that indicates whether to enforce hard extinctions.
     * @return The revised availability rate based on the performed extinction check.
     */
    private int performTrulyExtinctCheck(Part part, int availability, boolean useHardExtinction) {
        if (part.isExtinct(gameYear, originFaction.isClan(), factionTechCode)) {
            int extinctionYear = part.getExtinctionDate(originFaction.isClan(), factionTechCode);

            if ((gameYear > extinctionYear) && useHardExtinction) {
                return RATING_X;
            }

            if (gameYear < (extinctionYear + 10)) {
                return Compute.d6() > 3 ? availability : RATING_X;
            }
        }

        return availability;
    }

    /**
     * Procedure for calculating the final part availability for Clan tech base.
     *
     * @param part The part for which the availability is to be calculated.
     * @param availability The calculated availability rate for the part.
     * @return The revised availability rate based on Clan Tech Base rules.
     */
    private int getClanTechBaseRating(Part part, int availability) {
        if (originFaction.isClan()) {
            if (gameYear >= part.getCommonDate()) {
                return Math.max(RATING_A, availability - 1);
            }
        }

        if (techEra < ERA_CLAN) {
            return RATING_X;
        }

        availability++;

        if (availability > RATING_F) {
            return Compute.d6() > 3 ? RATING_F : RATING_X;
        }

        return availability;
    }

    /**
     * Procedure for calculating the final part availability for Inner Sphere tech base.
     *
     * @param part The part for which the availability is to be calculated.
     * @param availability The calculated availability rate for the part.
     * @return The revised availability rate based on Inner Sphere Tech Base rules.
     */
    private int getInnerSphereTechBaseRating(Part part, int availability) {
        if (originFaction.isClan()) {
            if (techEra < ERA_CLAN && part.getPrototypeDate(false) >= 2780) {
                return ITechnology.RATING_X;
            } else {
                int extinctionYear = part.getExtinctionDate(true);

                if ((techEra == ERA_SW)
                    && (gameYear >= extinctionYear)) {
                    return Compute.d6() > 3 ? RATING_F : RATING_X;
                } else {
                    return availability;
                }
            }
        }

        return getCommonTechBaseRating(part, availability);
    }

    /**
     * Procedure for calculating the final part availability for common tech base.
     *
     * @param part The part for which the availability is to be calculated.
     * @param availability The calculated availability rate for the part.
     * @return The revised availability rate based on Common Tech Base rules.
     */
    private int getCommonTechBaseRating(Part part, int availability) {
        if (!originFaction.isClan()) {
            if (part.getBaseAvailability(techEra) >= RATING_E) {
                if (availability == RATING_FSTAR || availability == RATING_X) {
                    int extinctionYear = part.getExtinctionDate();

                    if ((techEra == ERA_SW)
                        && (gameYear >= extinctionYear)) {
                        return Compute.d6() > 3 ? RATING_F : RATING_X;
                    } else {
                        return availability;
                    }
                }
            }
        }

        return availability;
    }
}
