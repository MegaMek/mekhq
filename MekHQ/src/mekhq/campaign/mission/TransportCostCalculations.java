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
import static java.lang.Math.max;
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

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
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
 * call {@link #calculateJumpCostForEachDay()} or {@link #calculateJumpCostForEntireJourney(int, int)}.</p>
 *
 * @author Illiani
 * @since 50.10
 */
public class TransportCostCalculations {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.TransportCostCalculations";
    private static final MMLogger LOGGER = MMLogger.create(TransportCostCalculations.class);

    // Most costs are listed as per month. There are n days in the average month. Therefore, the cost/day is the base
    // cost divided by n.
    public static final double PER_DAY_DIVIDER = 30.436875;
    // Collar hiring is per week. There are 7 days in a week. Therefore, the cost/day is the base cost divided by 7
    private static final double PER_DAY_WEEK = 7.0;

    private static final double LEGENDARY_CREW_MULTIPLIER = 3.0; // Unofficial
    private static final double HEROIC_CREW_MULTIPLIER = 2.5; // Unofficial
    private static final double ELITE_CREW_MULTIPLIER = 2.0;
    private static final double VETERAN_CREW_MULTIPLIER = 1.5;
    private static final double OTHER_CREW_MULTIPLIER = 1.0;

    static final double COLLARS_PER_JUMPSHIP = 4.0;
    static final Money COST_PER_JUMP_PER_JUMPSHIP = Money.of(100000);
    public static final int COST_PER_JUMP_PER_JUMPSHIP_AS_INT = 100000;

    // This value is derived from the Union (2708). We do make some assumptions, however. Namely, we assume that the
    // player is always able to find a DropShip that has the exact bay types they need. Use of this magical DropShip
    // allows us to greatly simplify the amount of processing. It also helps make the logic easier for players to
    // understand.
    static final double BAYS_PER_DROPSHIP = 14.0;
    // This value is derived from the Union (2708) (Cargo).
    static final double CARGO_PER_DROPSHIP = 1874.5;

    // These values are taken from CamOps pg 43.
    static final double JUMP_SHIP_COLLAR_COST = 100000 / PER_DAY_WEEK; // Collar prices are per week
    static final double SMALL_CRAFT_COST = 100000 / PER_DAY_DIVIDER;
    static final double MEK_COST = 50000 / PER_DAY_DIVIDER;
    static final double ASF_COST = 50000 / PER_DAY_DIVIDER;
    static final double SUPER_HEAVY_VEHICLE_COST = 100000 / PER_DAY_DIVIDER;
    static final double HEAVY_VEHICLE_COST = 50000 / PER_DAY_DIVIDER;
    static final double LIGHT_VEHICLE_COST = 25000 / PER_DAY_DIVIDER;
    static final double INFANTRY_COST = 100000 / PER_DAY_DIVIDER;
    static final double BATTLE_ARMOR_COST = 100000 / PER_DAY_DIVIDER;
    static final double PROTOMEK_COST = 100000 / PER_DAY_DIVIDER;
    static final double OTHER_UNIT_COST = 50000 / PER_DAY_DIVIDER; // (Unofficial)
    static final double CARGO_PER_TON_COST = 100000 / 1200.0 / PER_DAY_DIVIDER;

    // Some bays can accept multiple units. These values are based on the bay sizes of various DropShips. Largely
    // Unions and union equivalents.
    static final int PLATOONS_PER_BAY = 4;
    static final int BATTLE_ARMOR_SQUADS_PER_BAY = 4;
    static final int PROTOMEKS_PER_BAY = 5;

    // The only canon passenger DropShip is the Princess Luxury Liner. However, hiring one using CamOps rules proves
    // unreasonably expensive. Therefore, we're instead assuming that the player can find retrofit DropShips that has
    // passenger 'bays' that roughly equate to 15 passengers per bay. This number was determined by taking the
    // passenger capacity of the Princess Luxury Liner and dividing it by the number of bays in our magical DropShip.
    // We then assume a passenger bay cost is about the same as an Infantry Platoon bay. Presumably with nice
    // accommodations, which is why fewer people can fit.
    static final double PASSENGERS_PER_BAY = 15;
    // Hiring a Princess for dependents proved to be insanely expensive. So we're instead assuming
    static final double PASSENGERS_COST = INFANTRY_COST;

    private final Collection<Unit> hangarContents;
    private final Collection<Person> personnel;
    private final CargoStatistics cargoStatistics;
    private final HangarStatistics hangarStatistics;
    private final int crewExperienceLevel;

    private double additionalCargoSpaceRequired;
    private double cargoBayCost;
    private int requiredCargoDropShips;

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

    private int totalAdditionalBaysRequired;
    private int additionalDropShipsRequired;
    private int additionalCollarsRequired;
    private double dockingCollarCost;
    private double jumpShipsRequired;

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

    private Money totalCost = null;

    public @Nullable Money getTotalCost() {
        return totalCost;
    }

    void setTotalCost(Money totalCost) {
        this.totalCost = totalCost;
    }

    public double getAdditionalCargoSpaceRequired() {
        return additionalCargoSpaceRequired;
    }

    public double getCargoBayCost() {
        return cargoBayCost;
    }

    public int getRequiredCargoDropShips() {
        return requiredCargoDropShips;
    }

    public int getAdditionalSmallCraftBaysRequired() {
        return additionalSmallCraftBaysRequired;
    }

    public double getAdditionalSmallCraftBaysCost() {
        return additionalSmallCraftBaysCost;
    }

    public int getAdditionalASFBaysRequired() {
        return additionalASFBaysRequired;
    }

    public double getAdditionalASFBaysCost() {
        return additionalASFBaysCost;
    }

    public int getAdditionalMekBaysRequired() {
        return additionalMekBaysRequired;
    }

    public double getAdditionalMekBaysCost() {
        return additionalMekBaysCost;
    }

    public int getAdditionalSuperHeavyVehicleBaysRequired() {
        return additionalSuperHeavyVehicleBaysRequired;
    }

    public double getAdditionalSuperHeavyVehicleBaysCost() {
        return additionalSuperHeavyVehicleBaysCost;
    }

    public int getAdditionalHeavyVehicleBaysRequired() {
        return additionalHeavyVehicleBaysRequired;
    }

    public double getAdditionalHeavyVehicleBaysCost() {
        return additionalHeavyVehicleBaysCost;
    }

    public int getAdditionalLightVehicleBaysRequired() {
        return additionalLightVehicleBaysRequired;
    }

    public double getAdditionalLightVehicleBaysCost() {
        return additionalLightVehicleBaysCost;
    }

    public int getAdditionalProtoMekBaysRequired() {
        return additionalProtoMekBaysRequired;
    }

    public double getAdditionalProtoMekBaysCost() {
        return additionalProtoMekBaysCost;
    }

    public int getAdditionalBattleArmorBaysRequired() {
        return additionalBattleArmorBaysRequired;
    }

    public double getAdditionalBattleArmorBaysCost() {
        return additionalBattleArmorBaysCost;
    }

    public int getAdditionalInfantryBaysRequired() {
        return additionalInfantryBaysRequired;
    }

    public double getAdditionalInfantryBaysCost() {
        return additionalInfantryBaysCost;
    }

    public double getAdditionalOtherUnitBaysCost() {
        return additionalOtherUnitBaysCost;
    }

    public int getAdditionalPassengerBaysRequired() {
        return additionalPassengerBaysRequired;
    }

    public double getAdditionalPassengerBaysCost() {
        return additionalPassengerBaysCost;
    }

    public int getTotalAdditionalBaysRequired() {
        return totalAdditionalBaysRequired;
    }

    public int getAdditionalDropShipsRequired() {
        return additionalDropShipsRequired;
    }

    void setAdditionalDropShipsRequired(int additionalDropShipsRequired) {
        this.additionalDropShipsRequired = additionalDropShipsRequired;
    }

    public int getAdditionalCollarsRequired() {
        return additionalCollarsRequired;
    }

    public double getDockingCollarCost() {
        return dockingCollarCost;
    }

    public double getJumpShipsRequired() {
        return jumpShipsRequired;
    }

    int getDropShipCount() {
        return dropShipCount;
    }

    void setDropShipCount(int dropShipCount) {
        this.dropShipCount = dropShipCount;
    }

    int getSmallCraftCount() {
        return smallCraftCount;
    }

    public void setSmallCraftCount(int smallCraftCount) {
        this.smallCraftCount = smallCraftCount;
    }

    int getSuperHeavyVehicleCount() {
        return superHeavyVehicleCount;
    }

    public void setSuperHeavyVehicleCount(int superHeavyVehicleCount) {
        this.superHeavyVehicleCount = superHeavyVehicleCount;
    }

    int getHeavyVehicleCount() {
        return heavyVehicleCount;
    }

    public void setHeavyVehicleCount(int heavyVehicleCount) {
        this.heavyVehicleCount = heavyVehicleCount;
    }

    int getLightVehicleCount() {
        return lightVehicleCount;
    }

    public void setLightVehicleCount(int lightVehicleCount) {
        this.lightVehicleCount = lightVehicleCount;
    }

    int getMekCount() {
        return mekCount;
    }

    public void setMekCount(int mekCount) {
        this.mekCount = mekCount;
    }

    int getAsfCount() {
        return asfCount;
    }

    public void setASFCount(int asfCount) {
        this.asfCount = asfCount;
    }

    int getProtoMekCount() {
        return protoMekCount;
    }

    public void setProtoMekCount(int protoMekCount) {
        this.protoMekCount = protoMekCount;
    }

    int getBattleArmorCount() {
        return battleArmorCount;
    }

    public void setBattleArmorCount(int battleArmorCount) {
        this.battleArmorCount = battleArmorCount;
    }

    int getInfantryCount() {
        return infantryCount;
    }

    public void setInfantryCount(int infantryCount) {
        this.infantryCount = infantryCount;
    }

    public int getOtherUnitCount() {
        return otherUnitCount;
    }

    public void setOtherUnitCount(int otherUnitCount) {
        this.otherUnitCount = otherUnitCount;
    }

    /**
     * Constructs a new TransportCostCalculations class for evaluating jump and transport costs.
     *
     * @param hangarContents      The contents of the campaign's {@link Hangar}
     * @param personnel           The {@link Person} list representing personnel to be transported.
     * @param cargoStatistics     The {@link CargoStatistics} describing cargo loads.
     * @param hangarStatistics    The {@link HangarStatistics} listing all available bay capacities.
     * @param crewExperienceLevel The experience level to use for crew-related cost multipliers.
     *
     * @author Illiani
     * @since 50.10
     */
    public TransportCostCalculations(final Collection<Unit> hangarContents, final Collection<Person> personnel,
          final CargoStatistics cargoStatistics, final HangarStatistics hangarStatistics,
          final int crewExperienceLevel) {
        this.hangarContents = hangarContents;
        this.personnel = personnel;
        this.cargoStatistics = cargoStatistics;
        this.hangarStatistics = hangarStatistics;
        this.crewExperienceLevel = crewExperienceLevel;
    }

    /**
     * Calculates and returns the total cost of transporting all units and personnel over a specified number of days.
     *
     * <p>This method first ensures the single-day jump cost is calculated and stored in {@link #totalCost}. It then
     * multiplies this per-day cost by the given number of days to account for the full journey duration.</p>
     *
     * <p>The calculated total is also stored in {@link #totalCost} if it was previously uninitialized.</p>
     *
     * @param days       the total number of days in the journey; must be a positive integer
     * @param totalJumps the total number of jumps in the journey; must be a positive integer
     *
     * @return the total {@link Money} cost for the journey of the specified duration
     *
     * @author Illiani
     * @since 50.10
     */
    public Money calculateJumpCostForEntireJourney(final int days, final int totalJumps) {
        if (totalCost == null) { // totalCost will be null if calculateJumpCostForEachDay hasn't yet been run
            calculateJumpCostForEachDay();
        }

        Money runningTotal = totalCost.multipliedBy(days);
        Money perJumpCost = COST_PER_JUMP_PER_JUMPSHIP.multipliedBy(jumpShipsRequired);
        Money totalJumpsCost = perJumpCost.multipliedBy(totalJumps);

        return runningTotal.plus(totalJumpsCost).round();
    }

    /**
     * Calculates the cost of transporting the current hangar for a single day. This includes bay, collar, and
     * per-bay/per-collar costs, scaled for crew experience. The result is stored in {@link #totalCost} and also
     * returned.
     *
     * @author Illiani
     * @since 50.10
     */
    public void calculateJumpCostForEachDay() {
        // Initialize totalCost
        totalCost = Money.zero();

        calculateCargoRequirements();

        countUnitsByType();
        calculateAdditionalBayRequirementsFromUnits();
        calculateAdditionalBayRequirementsFromPassengers(hangarStatistics.getTotalLargeCraftPassengerCapacity());
        additionalDropShipsRequired += (int) ceil(totalAdditionalBaysRequired / BAYS_PER_DROPSHIP);

        calculateAdditionalJumpCollarsRequirements();

        adjustForCrewExperienceLevel();

        totalCost = totalCost.round();
    }

    /**
     * Adjusts the total transport cost by applying a multiplier determined by the crew's experience level.
     *
     * <p>The method selects a specific multiplier based on {@link #crewExperienceLevel}:</p>
     *
     * <ul>
     *   <li>{@code EXP_LEGENDARY}: uses {@code LEGENDARY_CREW_MULTIPLIER}</li>
     *   <li>{@code EXP_HEROIC}: uses {@code HEROIC_CREW_MULTIPLIER}</li>
     *   <li>{@code EXP_ELITE}: uses {@code ELITE_CREW_MULTIPLIER}</li>
     *   <li>{@code EXP_VETERAN}: uses {@code VETERAN_CREW_MULTIPLIER}</li>
     *   <li>Any other value: uses {@code OTHER_CREW_MULTIPLIER}</li>
     * </ul>
     *
     * <p>This multiplier is applied to the {@code totalCost} field, updating it to reflect the adjusted cost after
     * considering crew quality.</p>
     *
     * @author Illiani
     * @since 50.10
     */
    private void adjustForCrewExperienceLevel() {
        double crewExperienceLevelMultiplier = switch (crewExperienceLevel) {
            case EXP_LEGENDARY -> LEGENDARY_CREW_MULTIPLIER;
            case EXP_HEROIC -> HEROIC_CREW_MULTIPLIER;
            case EXP_ELITE -> ELITE_CREW_MULTIPLIER;
            case EXP_VETERAN -> VETERAN_CREW_MULTIPLIER;
            default -> OTHER_CREW_MULTIPLIER;
        };

        totalCost = totalCost.multipliedBy(crewExperienceLevelMultiplier);
    }

    /**
     * Calculates and updates the number of additional JumpShip docking collars required for transportation based on the
     * current number of available docking collars and DropShips present.
     *
     * <p>The method determines collar usage by subtracting the number of DropShips in the hangar from the total
     * docking collars. If the number of DropShips exceeds the available collars, the shortage is recorded in
     * {@link #additionalCollarsRequired}. Any additional DropShips required by bay requirements are also added to the
     * total collars needed.</p>
     *
     * @author Illiani
     * @since 50.10
     */
    void calculateAdditionalJumpCollarsRequirements() {
        int totalCollars = hangarStatistics.getTotalDockingCollars();
        int collarUsage = totalCollars - dropShipCount;
        additionalCollarsRequired = -min(0, collarUsage);
        additionalCollarsRequired += additionalDropShipsRequired;

        dockingCollarCost = round(additionalDropShipsRequired * JUMP_SHIP_COLLAR_COST);
        totalCost = totalCost.plus(dockingCollarCost);

        jumpShipsRequired = ceil(additionalDropShipsRequired / COLLARS_PER_JUMPSHIP);
    }

    /**
     * Calculates and updates the cargo requirements, determines additional DropShips needed, and computes the cargo bay
     * costs. Updates running cost totals as a side effect.
     *
     * @author Illiani
     * @since 50.10
     */
    void calculateCargoRequirements() {
        final double totalCargoCapacity = cargoStatistics.getTotalCargoCapacity();

        double totalCargoUsage = cargoStatistics.getCargoTonnage(false, false);
        totalCargoUsage += cargoStatistics.getCargoTonnage(false, true);

        additionalCargoSpaceRequired = -min(0, totalCargoCapacity - totalCargoUsage);
        cargoBayCost = round(additionalCargoSpaceRequired * CARGO_PER_TON_COST);

        requiredCargoDropShips = (int) ceil(additionalCargoSpaceRequired / CARGO_PER_DROPSHIP);

        additionalDropShipsRequired += requiredCargoDropShips;

        totalCost = totalCost.plus(cargoBayCost);
    }

    /**
     * Determines and updates the additional bay requirements and costs for each unit category, using current
     * HangarStatistics and counted unit types. Cargo and passenger bays are not included here. Updates running cost
     * totals as a side effect.
     *
     * @author Illiani
     * @since 50.10
     */
    void calculateAdditionalBayRequirementsFromUnits() {
        // Small Craft
        int smallCraftBays = hangarStatistics.getTotalSmallCraftBays();
        int smallCraftBayUsage = smallCraftBays - smallCraftCount;
        additionalSmallCraftBaysRequired = -min(0, smallCraftBayUsage);
        additionalSmallCraftBaysCost = round(additionalSmallCraftBaysRequired * SMALL_CRAFT_COST);
        totalCost = totalCost.plus(additionalSmallCraftBaysCost);
        int smallCraftSpareCapacity = max(0, smallCraftBayUsage);

        // ASF (including Conv Fighters)
        int asfBays = hangarStatistics.getTotalASFBays() + smallCraftSpareCapacity;
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
        int superHeavyVehicleSpareCapacity = max(0, superHeavyVehicleBayUsage);
        additionalSuperHeavyVehicleBaysCost = round(additionalSuperHeavyVehicleBaysRequired * SUPER_HEAVY_VEHICLE_COST);
        totalCost = totalCost.plus(additionalSuperHeavyVehicleBaysCost);

        // Heavy Vehicles`
        int heavyVehicleBays = hangarStatistics.getTotalHeavyVehicleBays() + superHeavyVehicleSpareCapacity;
        int heavyVehicleBayUsage = heavyVehicleBays - heavyVehicleCount;
        additionalHeavyVehicleBaysRequired = -min(0, heavyVehicleBayUsage);
        int heavyVehicleSpareCapacity = max(0, heavyVehicleBayUsage);
        additionalHeavyVehicleBaysCost = round(additionalHeavyVehicleBaysRequired * HEAVY_VEHICLE_COST);
        totalCost = totalCost.plus(additionalHeavyVehicleBaysCost);

        // Light Vehicles
        // heavyVehicleSpareCapacity also factors in any surplus super heavy vehicle bays
        int lightVehicleBays = hangarStatistics.getTotalLightVehicleBays() + heavyVehicleSpareCapacity;
        int lightVehicleBayUsage = lightVehicleBays - lightVehicleCount;
        additionalLightVehicleBaysRequired = -min(0, lightVehicleBayUsage);
        additionalLightVehicleBaysCost = round(additionalLightVehicleBaysRequired * LIGHT_VEHICLE_COST);
        totalCost = totalCost.plus(additionalLightVehicleBaysCost);

        // ProtoMeks
        int protoMekBays = hangarStatistics.getTotalProtoMekBays();
        double protoMekBayUsage = protoMekBays - protoMekCount;
        protoMekBayUsage = protoMekBayUsage / PROTOMEKS_PER_BAY;
        int adjustedProtoMekBayUsage = MathUtility.roundAwayFromZero(protoMekBayUsage);
        additionalProtoMekBaysRequired = -min(0, adjustedProtoMekBayUsage);
        additionalProtoMekBaysCost = round(additionalProtoMekBaysRequired * PROTOMEK_COST);
        totalCost = totalCost.plus(additionalProtoMekBaysCost);

        // Battle Armor
        int battleArmorBays = hangarStatistics.getTotalBattleArmorBays();
        double battleArmorBayUsage = battleArmorBays - battleArmorCount;
        battleArmorBayUsage = battleArmorBayUsage / BATTLE_ARMOR_SQUADS_PER_BAY;
        int adjustedBattleArmorBayUsage = MathUtility.roundAwayFromZero(battleArmorBayUsage);
        additionalBattleArmorBaysRequired = -min(0, adjustedBattleArmorBayUsage);
        additionalBattleArmorBaysCost = round(additionalBattleArmorBaysRequired * BATTLE_ARMOR_COST);
        totalCost = totalCost.plus(additionalBattleArmorBaysCost);

        // Infantry
        int infantryBays = hangarStatistics.getTotalInfantryBays();
        double infantryBayUsage = infantryBays - infantryCount;
        infantryBayUsage = infantryBayUsage / PLATOONS_PER_BAY;
        int adjustedInfantryBayUsage = MathUtility.roundAwayFromZero(infantryBayUsage);
        additionalInfantryBaysRequired = -min(0, adjustedInfantryBayUsage);
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
     * @author Illiani
     * @since 50.10
     */
    void countUnitsByType() {
        List<Unit> relevantUnits = hangarContents.stream().filter(unit -> !unit.isMothballed()).toList();
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
     *
     * @param passengerCapacity the total passenger capacity of all Large Craft in the campaign combined
     *
     * @author Illiani
     * @since 50.10
     */
    void calculateAdditionalBayRequirementsFromPassengers(int passengerCapacity) {
        int passengerCount = 0;
        for (Person person : personnel) {
            if (person.getStatus().isAbsent() || person.getStatus().isDepartedUnit()) {
                continue;
            }
            passengerCount++;
        }

        int additionalPassengerNeeds = max(0, passengerCount - passengerCapacity);
        additionalPassengerBaysRequired = (int) ceil(additionalPassengerNeeds / PASSENGERS_PER_BAY);
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
     *
     * @author Illiani
     * @since 50.10
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
