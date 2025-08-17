/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.procurement;

import static megamek.common.interfaces.ITechnology.AvailabilityValue;
import static megamek.common.interfaces.ITechnology.getTechEra;
import static megamek.common.SimpleTechLevel.INTRO;
import static megamek.common.SimpleTechLevel.STANDARD;

import java.util.ArrayList;
import java.util.List;

import megamek.common.compute.Compute;
import megamek.common.interfaces.ITechnology;
import megamek.common.interfaces.ITechnology.Era;
import megamek.common.interfaces.ITechnology.TechBase;
import megamek.common.SimpleTechLevel;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.universe.Faction;

/**
 * The Procurement class encapsulates the logic for deciding the availability of parts based on factors such as era,
 * technologies, and treaties between various factions. It is capable of making procurement checks to simulate rarity
 * and scarcity of parts in a given time frame.
 */
public class Procurement {
    private final int negotiatorSkillRating;
    private final int gameYear;
    private final Era techEra;
    private final Faction originFaction;
    private final ITechnology.Faction factionTechCode;
    private static final MMLogger logger = MMLogger.create(Procurement.class);

    /**
     * Procurement constructor. Initializes class instance with negotiator skill rating, game year, and originating
     * faction.
     *
     * @param negotiatorSkillRating the skill rating of the negotiator.
     * @param gameYear              the current year of the game.
     * @param originFaction         the faction from where procurement is initiated.
     */
    public Procurement(int negotiatorSkillRating, int gameYear, Faction originFaction) {
        this.negotiatorSkillRating = negotiatorSkillRating;
        this.gameYear = gameYear;
        this.originFaction = originFaction;

        factionTechCode = getTechFaction(originFaction);
        techEra = getTechEra(gameYear);
    }

    /**
     * Given a faction, returns the corresponding tech faction code.
     *
     * @param faction Faction instance
     *
     * @return returns corresponding faction.
     *
     * @deprecated Use {@link #getTechFaction(Faction)} instead.
     */
    @Deprecated
    public static ITechnology.Faction getFactionTechCode(Faction faction) {
        return getTechFaction(faction);
    }

    /**
     * Given a faction, returns the corresponding tech faction code.
     *
     * @param faction Faction instance
     *
     * @return returns corresponding faction.
     */
    public static ITechnology.Faction getTechFaction(Faction faction) {
        ITechnology.Faction result = ITechnology.Faction.fromMMAbbr(faction.getShortName());
        if (result != ITechnology.Faction.NONE) {
            return result;
        }

        // If the result faction is NONE, I check if I maybe got a not found in the ENUM.
        if (result.getCodeMM().toUpperCase().equals(faction.getShortName().toUpperCase())) {
            return result;
        }

        logger.info("Unable to retrieve Tech Faction. Using fallback.");

        if (faction.isClan()) {
            logger.info("Returning: Clan");
            return ITechnology.Faction.CLAN;
        } else if (faction.isPeriphery()) {
            logger.info("Returning: Periphery");
            return ITechnology.Faction.PER;
        } else {
            logger.info("Returning: Inner Sphere");
            return ITechnology.Faction.IS;
        }
    }

    /**
     * Makes procurement checks for a list of parts and returns successful parts.
     *
     * @param parts             List of parts that require procurement checks
     * @param useHardExtinction a boolean flag that indicates whether to enforce hard extinctions.
     * @param isResupply        Flag indicating if procurement is for resupplying parts
     *
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

        return successfulParts;
    }

    /**
     * Given a part, this method calculates the procurement target number.
     *
     * @param part              The part for which the procurement target number is to be calculated.
     * @param useHardExtinction a boolean flag that indicates whether to enforce hard extinctions.
     * @param isResupply        Flag indicating if procurement is for resupplying parts
     *
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
     * @param part              The part for which the target number should be calculated.
     * @param useHardExtinction a boolean flag that indicates whether to enforce hard extinctions.
     *
     * @return The calculated base target number.
     */
    private int getConsumableBaseTargetNumber(Part part, boolean useHardExtinction) {
        AvailabilityValue availability = getAvailability(part, useHardExtinction);

        int targetNumber = switch (availability) {
            case A -> 2;
            case B -> 3;
            case C -> 4;
            case D -> 6;
            case E -> 8;
            case F -> 10;
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
     * @param part              The part for which the target number should be calculated.
     * @param useHardExtinction a boolean flag that indicates whether to enforce hard extinctions.
     *
     * @return The calculated base target number.
     */
    private int getBaseTargetNumber(Part part, boolean useHardExtinction) {
        AvailabilityValue availability = getAvailability(part, useHardExtinction);

        return switch (availability) {
            case A -> 3;
            case B -> 4;
            case C -> 6;
            case D -> 8;
            case E -> 10;
            case F -> 11;
            // This value is deliberately impossible on 2d6
            default -> 13; // X or F*
        };
    }

    /**
     * Returns the availability rate for the given part.
     *
     * @param part              The part for which the availability is to be calculated.
     * @param useHardExtinction a boolean flag that indicates whether to enforce hard extinctions
     *
     * @return The calculated availability rate.
     */
    private AvailabilityValue getAvailability(Part part, boolean useHardExtinction) {
        AvailabilityValue availability = part.calcYearAvailability(gameYear, originFaction.isClan(), factionTechCode);

        if (part.getTechBase() == TechBase.IS) {
            availability = getInnerSphereBaseAvailability(part, availability);
            return performTrulyExtinctCheck(part, availability, useHardExtinction);
        }

        if (part.getTechBase() == ITechnology.TechBase.CLAN) {
            availability = getClanBaseAvailability(part, availability);
            return performTrulyExtinctCheck(part, availability, useHardExtinction);
        }

        // Tech Base: All
        availability = getCommonBaseAvailability(part, availability);
        return performTrulyExtinctCheck(part, availability, useHardExtinction);
    }

    /**
     * Assess the extinction state of a specific part and adjusts its availability rating based on whether the part is
     * truly extinct or not.
     *
     * @param part              The part for which to assess the extinction state.
     * @param availability      The calculated availability rate for the part.
     * @param useHardExtinction a boolean flag that indicates whether to enforce hard extinctions.
     *
     * @return The revised availability rate based on the performed extinction check.
     */
    private AvailabilityValue performTrulyExtinctCheck(Part part, AvailabilityValue availability,
          boolean useHardExtinction) {
        if (part.isExtinct(gameYear, originFaction.isClan(), factionTechCode)) {
            int extinctionYear = part.getExtinctionDate(originFaction.isClan(), factionTechCode);

            if ((gameYear > extinctionYear) && useHardExtinction) {
                return AvailabilityValue.X;
            }

            if (gameYear < (extinctionYear + 10)) {
                return Compute.d6() > 3 ? availability : AvailabilityValue.X;
            }
        }

        return availability;
    }

    /**
     * Procedure for calculating the final part availability for Clan tech base.
     *
     * @param part         The part for which the availability is to be calculated.
     * @param availability The calculated availability rate for the part.
     *
     * @return The revised availability rate based on Clan Tech Base rules.
     */
    private AvailabilityValue getClanBaseAvailability(Part part, AvailabilityValue availability) {
        if (originFaction.isClan()) {
            if (gameYear >= part.getCommonDate()) {
                int easier = Math.max(AvailabilityValue.A.getIndex(), availability.getIndex() - 1);
                return AvailabilityValue.fromIndex(easier);
            }
        }

        if (techEra.getIndex() < Era.CLAN.getIndex()) {
            return AvailabilityValue.X;
        }

        int harder = Math.min(availability.getIndex() + 1, AvailabilityValue.F.getIndex());
        AvailabilityValue result = AvailabilityValue.fromIndex(harder);

        // If the result is above F, randomize between F and X
        if (result == AvailabilityValue.F && availability.getIndex() + 1 > AvailabilityValue.F.getIndex()) {
            return Compute.d6() > 3 ? AvailabilityValue.F : AvailabilityValue.X;
        }

        return result;
    }

    /**
     * Procedure for calculating the final part availability for Inner Sphere tech base.
     *
     * @param part         The part for which the availability is to be calculated.
     * @param availability The calculated availability rate for the part.
     *
     * @return The revised availability rate based on Inner Sphere Tech Base rules.
     */
    private AvailabilityValue getInnerSphereBaseAvailability(Part part, AvailabilityValue availability) {
        if (originFaction.isClan()) {
            if ((techEra.getIndex() < Era.CLAN.getIndex()) && (part.getPrototypeDate(false) >= 2780)) {
                return AvailabilityValue.X;
            } else {
                int extinctionYear = part.getExtinctionDate(true);

                if ((techEra == Era.SW)
                          && (gameYear >= extinctionYear)) {
                    return Compute.d6() > 3 ? AvailabilityValue.F : AvailabilityValue.X;
                } else {
                    return availability;
                }
            }
        }

        return getCommonBaseAvailability(part, availability);
    }

    /**
     * Procedure for calculating the final part availability for common tech base.
     *
     * @param part         The part for which the availability is to be calculated.
     * @param availability The calculated availability rate for the part.
     *
     * @return The revised availability rate based on Common Tech Base rules.
     */
    private AvailabilityValue getCommonBaseAvailability(Part part, AvailabilityValue availability) {
        if (!originFaction.isClan()) {
            if (part.getBaseAvailability(techEra).isBetterOrEqualThan(AvailabilityValue.E)) {
                if (availability == AvailabilityValue.X) {
                    int extinctionYear = part.getExtinctionDate();

                    if ((techEra == Era.SW)
                              && (gameYear >= extinctionYear)) {
                        return Compute.d6() > 3 ? AvailabilityValue.F : AvailabilityValue.X;
                    } else {
                        return availability;
                    }
                }
            }
        }

        return availability;
    }
}
