/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ELITE;
import static mekhq.campaign.personnel.skills.SkillType.EXP_HEROIC;
import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_VETERAN;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Hangar;
import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.CargoStatistics;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * Calculates transportation costs and requirements for moving a force, based on the units, personnel, hangar space, and
 * cargo present.
 *
 * <p>This class supports cost estimation for DropShip bays, JumpShip collars, cargo space, and passenger transport
 * based on BattleTech Campaign Operations and other related rulesets. It handles per-day cost scaling, as well as cost
 * multipliers based on crew experience.</p>
 *
 * <p>Usage: Create an instance with the unit's hangar, relevant personnel, and statistics, then
 * call {@link #calculateJumpCostForEachDay()} or {@link #calculateJumpCostForEntireJourney(int)}.</p>
 *
 * <p>Call {@link #getJumpCostString()} for a detailed report.</p>
 */
public class TransportCostCalculations {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.TransportCostCalculations";
    private static final MMLogger LOGGER = MMLogger.create(TransportCostCalculations.class);

    // Most costs are listed as per month. There are n days in the average month. Therefore, the cost/day is the base
    // cost divided by n.
    private static final double PER_DAY_DIVIDER = 30.436875;
    // Collar hiring is per week. There are 7 days in a week. Therefore, the cost/day is the base cost divided by 7
    private static final double PER_DAY_WEEK = 7.0;

    private static final double ELITE_CREW_MULTIPLIER = 2.0;
    private static final double VETERAN_CREW_MULTIPLIER = 1.5;
    private static final double OTHER_CREW_MULTIPLIER = 1.0;

    // This value is derived from the Union (2708). We do make some assumptions, however. Namely, we assume that the
    // player is always able to find a DropShip that has the exact bay types they need. Use of this magical DropShip
    // allows us to greatly simplify the amount of processing. It also helps make the logic easier for players to
    // understand.
    private static final int BAYS_PER_DROPSHIP = 14;
    // This value is derived from the Union (2708) (Cargo).
    private static final double CARGO_PER_DROPSHIP = 1874.5;

    // These values are taken from CamOps pg 43.
    private static final double JUMP_SHIP_COLLAR_COST = 100000 / PER_DAY_WEEK; // Collar prices are per week
    private static final double SMALL_CRAFT_OR_SUPER_HEAVY_COST = 100000 / PER_DAY_DIVIDER;
    private static final double MEK_COST = 50000 / PER_DAY_DIVIDER;
    private static final double ASF_COST = 50000 / PER_DAY_DIVIDER;
    private static final double HEAVY_VEHICLE_COST = 50000 / PER_DAY_DIVIDER;
    private static final double LIGHT_VEHICLE_COST = 25000 / PER_DAY_DIVIDER;
    private static final double INFANTRY_COST = 25000 / PER_DAY_DIVIDER;
    private static final double BATTLE_ARMOR_COST = 25000 / PER_DAY_DIVIDER;
    private static final double PROTOMEK_COST = 20000 / PER_DAY_DIVIDER;
    private static final double OTHER_UNIT_COST = 50000 / PER_DAY_DIVIDER; // (Unofficial)
    private static final double CARGO_PER_TON_COST = 100000 / 1200.0 / PER_DAY_DIVIDER;

    // The only canon passenger DropShip is the Princess Luxury Liner. However, hiring one using CamOps rules proves
    // unreasonably expensive. Therefore, we're instead assuming that the player can find retrofit DropShips that has
    // passenger 'bays' that roughly equate to 15 passengers per bay. This number was determined by taking the
    // passenger capacity of the Princess Luxury Liner and dividing it by the number of bays in our magical DropShip.
    // We then assume a passenger bay cost is about the same as an Infantry Platoon bay. Presumably with nice
    // accommodations, which is why fewer people can fit.
    private static final double PASSENGERS_PER_BAY = 15;
    // Hiring a Princess for dependents proved to be insanely expensive. So we're instead assuming
    private static final double PASSENGERS_COST = INFANTRY_COST;

    private final Hangar hangar;
    private final Collection<Person> personnel;
    private final CargoStatistics cargoStatistics;
    private final HangarStatistics hangarStatistics;
    private final int crewExperienceLevel;

    private double additionalCargoSpaceRequired;
    private double cargoBayCost;

    private int additionalSmallCraftBaysRequired;
    private double additionalSmallCraftBaysCost;
    private int additionalASFBaysRequired;
    private double additionalASFBaysCost;
    private int additionalMekBaysRequired;
    private double additionalMekBaysCost;
    private int additionalSuperHeavyVehicleBaysRequired;
    private double additionalSuperHeavyVehicleBaysCost;
    private int additionalHeavyVehicleBaysRequired;
    private double additionalHeavyVehicleBaysCost;
    private int additionalLightVehicleBaysRequired;
    private double additionalLightVehicleBaysCost;
    private int additionalProtoMekBaysRequired;
    private double additionalProtoMekBaysCost;
    private int additionalBattleArmorBaysRequired;
    private double additionalBattleArmorBaysCost;
    private int additionalInfantryBaysRequired;
    private double additionalInfantryBaysCost;
    private double additionalOtherUnitBaysCost;
    private int additionalPassengerBaysRequired;
    private double additionalPassengerBaysCost;

    private double totalAdditionalBaysRequired;
    private int additionalDropShipsRequired;
    private int additionalCollarsRequired;
    private double dockingCollarCost;

    private int dropShipCount;
    private int smallCraftCount;
    private int superHeavyVehicleCount;
    private int heavyVehicleCount;
    private int lightVehicleCount;
    private int mekCount;
    private int asfCount;
    private int protoMekCount;
    private int battleArmorCount;
    private int infantryCount;
    private int otherUnitCount;

    private Money totalCost = Money.zero();

    /**
     * Constructs a new TransportCostCalculations class for evaluating jump and transport costs.
     *
     * @param hangar              The {@link Hangar} containing units for transport.
     * @param personnel           The {@link Person} list representing personnel to be transported.
     * @param cargoStatistics     The {@link CargoStatistics} describing cargo loads.
     * @param hangarStatistics    The {@link HangarStatistics} listing all available bay capacities.
     * @param crewExperienceLevel The experience level to use for crew-related cost multipliers.
     */
    public TransportCostCalculations(final Hangar hangar, final Collection<Person> personnel,
          final CargoStatistics cargoStatistics, final HangarStatistics hangarStatistics,
          final int crewExperienceLevel) {
        this.hangar = hangar;
        this.personnel = personnel;
        this.cargoStatistics = cargoStatistics;
        this.hangarStatistics = hangarStatistics;
        this.crewExperienceLevel = crewExperienceLevel;
    }


    /**
     * Returns a detailed HTML-formatted report string of bay, cargo, and passenger requirements and associated costs.
     *
     * @return a {@code String} containing the full jump cost breakdown.
     */
    public String getJumpCostString() {
        StringBuilder report = new StringBuilder("<html>MID-DEVELOPMENT PLACEHOLDER<br>DO NOT REPORT AS BROKEN<br>");
        report.append("additionalCargoSpaceRequired: ").append(additionalCargoSpaceRequired).append("<br>");
        report.append("cargoBayCost: ").append(cargoBayCost).append("<br>");
        report.append("<br>");

        report.append("additionalSmallCraftBaysRequired: ").append(additionalSmallCraftBaysRequired).append("<br>");
        report.append("additionalSmallCraftBaysCost: ").append(additionalSmallCraftBaysCost).append("<br>");
        report.append("additionalASFBaysRequired: ").append(additionalASFBaysRequired).append("<br>");
        report.append("additionalASFBaysCost: ").append(additionalASFBaysCost).append("<br>");
        report.append("additionalMekBaysRequired: ").append(additionalMekBaysRequired).append("<br>");
        report.append("additionalMekBaysCost: ").append(additionalMekBaysCost).append("<br>");
        report.append("additionalSuperHeavyVehicleBaysRequired: ")
              .append(additionalSuperHeavyVehicleBaysRequired)
              .append("<br>");
        report.append("additionalSuperHeavyVehicleBaysCost: ")
              .append(additionalSuperHeavyVehicleBaysCost)
              .append("<br>");
        report.append("additionalHeavyVehicleBaysRequired: ").append(additionalHeavyVehicleBaysRequired).append("<br>");
        report.append("additionalHeavyVehicleBaysCost: ").append(additionalHeavyVehicleBaysCost).append("<br>");
        report.append("additionalLightVehicleBaysRequired: ").append(additionalLightVehicleBaysRequired).append("<br>");
        report.append("additionalLightVehicleBaysCost: ").append(additionalLightVehicleBaysCost).append("<br>");
        report.append("additionalProtoMekBaysRequired: ").append(additionalProtoMekBaysRequired).append("<br>");
        report.append("additionalProtoMekBaysCost: ").append(additionalProtoMekBaysCost).append("<br>");
        report.append("additionalBattleArmorBaysRequired: ").append(additionalBattleArmorBaysRequired).append("<br>");
        report.append("additionalBattleArmorBaysCost: ").append(additionalBattleArmorBaysCost).append("<br>");
        report.append("additionalInfantryBaysRequired: ").append(additionalInfantryBaysRequired).append("<br>");
        report.append("additionalInfantryBaysCost: ").append(additionalInfantryBaysCost).append("<br>");
        report.append("additionalOtherUnitBaysRequired: ").append(otherUnitCount).append("<br>");
        report.append("additionalOtherUnitBaysCost: ").append(additionalOtherUnitBaysCost).append("<br>");
        report.append("<br>");

        report.append("additionalPassengerBaysRequired: ").append(additionalPassengerBaysRequired).append("<br>");
        report.append("additionalPassengerBaysCost: ").append(additionalPassengerBaysCost).append("<br>");
        report.append("<br>");

        report.append("totalAdditionalBaysRequired: ").append(totalAdditionalBaysRequired).append("<br>");
        report.append("additionalDropShipsRequired: ").append(additionalDropShipsRequired).append("<br>");
        report.append("additionalCollarsRequired: ").append(additionalCollarsRequired).append("<br>");
        report.append("dockingCollarCost: ").append(dockingCollarCost).append("<br>");
        report.append("totalCost: ").append(totalCost.toAmountString()).append("<br>");
        report.append("</html>");
        return report.toString();
    }

    /**
     * Calculates the total cost of transporting all units and personnel for a multi-day journey. The cost for each day
     * is computed, then multiplied by the number of days in the journey. The result is stored in {@link #totalCost} and
     * also returned.
     *
     * @param days the duration of the journey in days
     *
     * @return the total {@link Money} cost for the journey
     */
    public Money calculateJumpCostForEntireJourney(final int days) {
        calculateJumpCostForEachDay();
        totalCost = totalCost.multipliedBy(days);
        return totalCost;
    }

    /**
     * Calculates the cost of transporting the current hangar for a single day. This includes bay, collar, and
     * per-bay/per-collar costs, scaled for crew experience. The result is stored in {@link #totalCost} and also
     * returned.
     *
     * @return the daily {@link Money} cost of transport.
     */
    public Money calculateJumpCostForEachDay() {
        calculateCargoRequirements();

        countUnitsByType(hangar);
        calculateAdditionalBayRequirementsFromUnits();
        calculateAdditionalBayRequirementsFromPassengers();
        additionalDropShipsRequired += (int) ceil(totalAdditionalBaysRequired / BAYS_PER_DROPSHIP);

        calculateAdditionalJumpCollarsRequirements();

        dockingCollarCost = round(additionalDropShipsRequired * JUMP_SHIP_COLLAR_COST);
        totalCost = totalCost.plus(dockingCollarCost);

        double crewExperienceLevelMultiplier = switch (crewExperienceLevel) {
            case EXP_ELITE, EXP_HEROIC, EXP_LEGENDARY -> ELITE_CREW_MULTIPLIER;
            case EXP_VETERAN -> VETERAN_CREW_MULTIPLIER;
            default -> OTHER_CREW_MULTIPLIER;
        };
        totalCost = totalCost.multipliedBy(crewExperienceLevelMultiplier);

        return totalCost;
    }

    /**
     * Calculates and updates the number of additional JumpShip docking collars required for transportation based on the
     * current number of available docking collars and DropShips present.
     *
     * <p>The method determines collar usage by subtracting the number of DropShips in the hangar from the total
     * docking collars. If the number of DropShips exceeds the available collars, the shortage is recorded in
     * {@link #additionalCollarsRequired}. Any additional DropShips required by bay requirements are also added to the
     * total collars needed.</p>
     */
    private void calculateAdditionalJumpCollarsRequirements() {
        int totalCollars = hangarStatistics.getTotalDockingCollars();
        int collarUsage = totalCollars - dropShipCount;
        additionalCollarsRequired = -min(0, collarUsage);
        additionalCollarsRequired += additionalDropShipsRequired;
    }

    /**
     * Calculates and updates the cargo requirements, determines additional DropShips needed, and computes the cargo bay
     * costs. Updates running cost totals as a side effect.
     */
    private void calculateCargoRequirements() {
        final double totalCargoCapacity = cargoStatistics.getTotalCargoCapacity();
        LOGGER.info("Total cargo capacity: {}", totalCargoCapacity);

        double totalCargoUsage = cargoStatistics.getCargoTonnage(false, false);
        totalCargoUsage += cargoStatistics.getCargoTonnage(false, true);
        LOGGER.info("Total cargo usage: {}", totalCargoUsage);

        additionalCargoSpaceRequired = -min(0, totalCargoCapacity - totalCargoUsage);
        LOGGER.info("Cargo requirements: {}", additionalCargoSpaceRequired);
        cargoBayCost = round(additionalCargoSpaceRequired * CARGO_PER_TON_COST);

        additionalDropShipsRequired += (int) ceil(additionalCargoSpaceRequired / CARGO_PER_DROPSHIP);
        LOGGER.info("Additional drop ships required: {}", additionalDropShipsRequired);

        totalCost = totalCost.plus(cargoBayCost);
    }

    /**
     * Determines and updates the additional bay requirements and costs for each unit category, using current
     * HangarStatistics and counted unit types. Cargo and passenger bays are not included here. Updates running cost
     * totals as a side effect.
     */
    private void calculateAdditionalBayRequirementsFromUnits() {
        // Some unit types can fit in a bay that is ostensibly intended for other types
        int spareCapacity;

        // Small Craft
        int smallCraftBays = hangarStatistics.getTotalSmallCraftBays();
        int smallCraftBayUsage = smallCraftBays - smallCraftCount;
        additionalSmallCraftBaysRequired = -min(0, smallCraftBayUsage);
        additionalSmallCraftBaysCost = round(additionalSmallCraftBaysRequired * SMALL_CRAFT_OR_SUPER_HEAVY_COST);
        totalCost = totalCost.plus(additionalSmallCraftBaysCost);
        spareCapacity = Math.max(0, smallCraftBayUsage);

        // ASF (including Conv Fighters)
        int asfBays = hangarStatistics.getTotalASFBays() + spareCapacity;
        int asfBayUsage = asfBays - asfCount;
        additionalASFBaysRequired = -min(0, asfBayUsage);
        additionalASFBaysCost = round(additionalASFBaysRequired * ASF_COST);
        totalCost = totalCost.plus(additionalASFBaysCost);

        // Meks
        int mekBays = hangarStatistics.getTotalMekBays();
        int mekBayUsage = mekBays - mekCount;
        additionalMekBaysRequired = -min(0, mekBayUsage);
        additionalMekBaysCost = round(additionalMekBaysRequired * MEK_COST);
        totalCost = totalCost.plus(additionalMekBaysCost);

        // Super Heavy Vehicles
        int superHeavyVehicleBays = hangarStatistics.getTotalSuperHeavyVehicleBays();
        int superHeavyVehicleBayUsage = superHeavyVehicleBays - superHeavyVehicleCount;
        additionalSuperHeavyVehicleBaysRequired = -min(0, superHeavyVehicleBayUsage);
        spareCapacity = Math.max(0, superHeavyVehicleBayUsage);
        additionalSuperHeavyVehicleBaysCost =
              round(additionalSuperHeavyVehicleBaysRequired * SMALL_CRAFT_OR_SUPER_HEAVY_COST);
        totalCost = totalCost.plus(additionalSuperHeavyVehicleBaysCost);

        // Heavy Vehicles
        int heavyVehicleBays = hangarStatistics.getTotalHeavyVehicleBays() + spareCapacity;
        int heavyVehicleBayUsage = heavyVehicleBays - heavyVehicleCount;
        additionalHeavyVehicleBaysRequired = -min(0, heavyVehicleBayUsage);
        spareCapacity = Math.max(0, heavyVehicleBayUsage);
        additionalHeavyVehicleBaysCost = round(heavyVehicleBayUsage * HEAVY_VEHICLE_COST);
        totalCost = totalCost.plus(additionalHeavyVehicleBaysCost);

        // Light Vehicles
        int lightVehicleBays = hangarStatistics.getTotalLightVehicleBays() + spareCapacity;
        int lightVehicleBayUsage = lightVehicleBays - lightVehicleCount;
        additionalLightVehicleBaysRequired = -min(0, lightVehicleBayUsage);
        additionalLightVehicleBaysCost = round(additionalLightVehicleBaysRequired * LIGHT_VEHICLE_COST);
        totalCost = totalCost.plus(additionalLightVehicleBaysCost);

        // ProtoMeks
        int protoMekBays = hangarStatistics.getTotalProtoMekBays();
        int protoMekBayUsage = protoMekBays - protoMekCount;
        additionalProtoMekBaysRequired = -min(0, protoMekBayUsage);
        additionalProtoMekBaysCost = round(additionalProtoMekBaysRequired * PROTOMEK_COST);
        totalCost = totalCost.plus(additionalProtoMekBaysCost);

        // Battle Armor
        int battleArmorBays = hangarStatistics.getTotalBattleArmorBays();
        int battleArmorBayUsage = battleArmorBays - battleArmorCount;
        additionalBattleArmorBaysRequired = -min(0, battleArmorBayUsage);
        additionalBattleArmorBaysCost = round(additionalBattleArmorBaysRequired * BATTLE_ARMOR_COST);
        totalCost = totalCost.plus(additionalBattleArmorBaysCost);

        // Battle Armor
        int infantryBays = hangarStatistics.getTotalInfantryBays();
        int infantryBayUsage = infantryBays - infantryCount;
        additionalInfantryBaysRequired = -min(0, infantryBayUsage);
        additionalInfantryBaysCost = round(additionalInfantryBaysRequired * INFANTRY_COST);
        totalCost = totalCost.plus(additionalInfantryBaysCost);

        // Other Units
        additionalOtherUnitBaysCost = round(otherUnitCount * OTHER_UNIT_COST);
        totalCost = totalCost.plus(additionalOtherUnitBaysCost);

        // Total of all requirements
        totalAdditionalBaysRequired += additionalSmallCraftBaysRequired +
                                             additionalASFBaysRequired +
                                             additionalMekBaysRequired +
                                             additionalSuperHeavyVehicleBaysRequired +
                                             additionalHeavyVehicleBaysRequired +
                                             additionalLightVehicleBaysRequired +
                                             additionalProtoMekBaysRequired +
                                             additionalBattleArmorBaysRequired +
                                             additionalInfantryBaysRequired +
                                             additionalPassengerBaysRequired +
                                             otherUnitCount;
    }

    /**
     * Counts units by type (vehicles by weight and non-vehicles by entity type) and updates internal counters for each
     * tracked unit category, ignoring mothballed units and skipping units lacking valid entities.
     *
     * @param hangar the {@link Hangar} to scan units from.
     */
    private void countUnitsByType(Hangar hangar) {
        List<Unit> relevantUnits = hangar.getUnits().stream().filter(unit -> !unit.isMothballed()).toList();
        for (Unit unit : relevantUnits) {
            Entity entity = unit.getEntity();
            if (entity == null) {
                LOGGER.warn("Entity is null for unit: {}. Skipping", unit.getName());
                continue;
            }

            // Vehicles are handled separately based on their weight
            if (entity.isVehicle()) {
                double weight = entity.getWeight();
                if (weight > 100) {
                    superHeavyVehicleCount++;
                } else if (weight > 50) {
                    heavyVehicleCount++;
                } else {
                    lightVehicleCount++;
                }
            }

            // Non-vehicle entities are categorized based on the entity types
            else {
                if (entity.isDropShip()) {
                    dropShipCount++;
                } else if (entity.isSmallCraft()) {
                    smallCraftCount++;
                } else if (entity.isMek()) {
                    mekCount++;
                } else if (entity.isAerospaceFighter() || entity.isConventionalFighter()) {
                    asfCount++;
                } else if (entity.isProtoMek()) {
                    protoMekCount++;
                } else if (entity.isBattleArmor()) {
                    battleArmorCount++;
                } else if (entity.isInfantry()) {
                    infantryCount++;
                } else {
                    otherUnitCount++;
                }
            }
        }
    }

    /**
     * Calculates passenger bay requirements and costs based on the personnel list, counting only those present and
     * assigned to the force. Updates totals for passenger bay counts and costs.
     */
    private void calculateAdditionalBayRequirementsFromPassengers() {
        int passengerCount = 0;
        for (Person person : personnel) {
            if (person.getStatus().isAbsent() || person.getStatus().isDepartedUnit()) {
                continue;
            }
            passengerCount++;
        }
        additionalPassengerBaysRequired = (int) ceil(passengerCount / PASSENGERS_PER_BAY);
        additionalPassengerBaysCost = round(additionalPassengerBaysRequired * PASSENGERS_COST);
        totalCost = totalCost.plus(additionalPassengerBaysCost);
        totalAdditionalBaysRequired += additionalPassengerBaysRequired;
    }

    /**
     * Executes a financial transaction for performing a jump between two planetary systems, debiting the specified
     * journey cost from the provided finances. Generates and includes a report of the transaction outcome.
     *
     * <p>If the account lacks sufficient funds, returns a formatted message indicating payment failure. If the
     * transaction succeeds, returns an empty string.</p>
     *
     * @param finances      The {@link Finances} object to debit the jump cost from.
     * @param jumpPath      The {@link JumpPath} representing the journey, used to construct the report.
     * @param today         The current {@link LocalDate}, used to date the transaction and report.
     * @param journeyCost   The {@link Money} amount required for the jump.
     * @param currentSystem The {@link PlanetarySystem} where the journey begins.
     *
     * @return An HTML-formatted report string if payment failed, or an empty string if the transaction succeeded.
     */
    public static String performJumpTransaction(Finances finances, JumpPath jumpPath, LocalDate today,
          Money journeyCost, PlanetarySystem currentSystem) {
        String jumpReport = getFormattedTextAt(RESOURCE_BUNDLE, "TransportCostCalculations.transactionReport",
              currentSystem.getName(today), jumpPath.getLastSystem().getName(today));
        if (!finances.debit(TransactionType.TRANSPORTATION, today, journeyCost, jumpReport)) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "TransportCostCalculations.unableToAffordJump",
                  spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG);
        }

        return "";
    }
}
