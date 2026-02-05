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
package mekhq.campaign.market.contractMarket;

import static megamek.common.units.EntityWeightClass.WEIGHT_ASSAULT;
import static megamek.common.units.EntityWeightClass.WEIGHT_HEAVY;
import static megamek.common.units.EntityWeightClass.WEIGHT_LIGHT;
import static megamek.common.units.EntityWeightClass.WEIGHT_MEDIUM;
import static megamek.common.units.EntityWeightClass.WEIGHT_SUPER_HEAVY;
import static megamek.common.units.EntityWeightClass.WEIGHT_ULTRA_LIGHT;
import static mekhq.campaign.force.FormationLevel.BATTALION;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Infantry;
import megamek.common.units.LandAirMek;
import megamek.logging.MMLogger;
import mekhq.campaign.Hangar;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;

/**
 * Enumerates the per-unit baseline values used by the "Alternate Payment Model" when evaluating contract compensation.
 *
 * <p>The constants represent standardized {@link Money} values for different unit categories (BattleMeks,
 * aerospace, conventional infantry, vehicles, large craft, etc.). These values are sourced from Campaign Operations
 * (CamOps) and are used to compute an abstract "force value" for a set of forces under a contract.</p>
 *
 * <p>Primary usage:</p>
 * <ul>
 *     <li>Use {@link #getValue()} to retrieve the baseline {@link Money} value for a given category.</li>
 *     <li>Use {@link #getForceValue(Faction, List, Hangar, boolean, boolean, double, double, double, double)} to
 *     compute the total contract value for a set of forces, applying contract percentage multipliers per category.</li>
 * </ul>
 *
 * <p><b>Notes:</b></p>
 * <ul>
 *     <li>Only forces that are both {@code standard} and have a {@code combat role} are included in calculations.</li>
 *     <li>Null units and units without an {@link Entity} are ignored.</li>
 *     <li>Some unexpected enum/value cases are treated as errors and logged; in those cases, the contribution is
 *     treated as {@link Money#zero()}.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.50.11
 */
public enum AlternatePaymentModelValues {
    // These values are taken from CamOps pg 14
    AEROSPACE_FIGHTER_HEAVY(Money.of(9000000)),
    AEROSPACE_FIGHTER_LIGHT(Money.of(3000000)),
    AEROSPACE_FIGHTER_MEDIUM(Money.of(6000000)),
    BATTLEMEK_ASSAULT(Money.of(12000000)),
    BATTLEMEK_HEAVY(Money.of(9000000)),
    BATTLEMEK_LIGHT(Money.of(3000000)),
    BATTLEMEK_MEDIUM(Money.of(6000000)),
    BATTLEMEK_SUPER_HEAVY(Money.of(50000000)), // CamOps Unique Unit
    BATTLE_ARMOR_PER_SUIT(Money.of(750000)),
    COMBAT_VEHICLE_ASSAULT(Money.of(2000000)),
    COMBAT_VEHICLE_HEAVY(Money.of(1500000)),
    COMBAT_VEHICLE_LIGHT(Money.of(500000)),
    COMBAT_VEHICLE_MEDIUM(Money.of(1000000)),
    COMBAT_VEHICLE_SUPER_HEAVY(Money.of(50000000)), // CamOps Unique Unit
    CONVENTIONAL_FIGHTER(Money.of(1000000)),
    CONVENTIONAL_INFANTRY_FOOT(Money.of(1200000)),
    CONVENTIONAL_INFANTRY_JUMP(Money.of(2800000)),
    CONVENTIONAL_INFANTRY_MECHANIZED(Money.of(2800000)),
    CONVENTIONAL_INFANTRY_MOTORIZED(Money.of(2000000)),
    LAM(Money.of(12000000)), // CamOps says this should be 50 mil, but that's excessive, so we've lowered it
    LARGE_CRAFT(Money.of(50000000)), // CamOps Unique Unit
    PROTOMEK(Money.of(1000000)),
    SATELLITE(Money.of(3000000)),
    SMALL_CRAFT(Money.of(12000000)),
    SUPPORT_VEHICLE_HEAVY(Money.of(2250000)),
    SUPPORT_VEHICLE_LIGHT(Money.of(250000)),
    SUPPORT_VEHICLE_MEDIUM(Money.of(750000)),
    SUPPORT_VEHICLE_SUPER_HEAVY(Money.of(20000000));

    private static final MMLogger LOGGER = MMLogger.create(AlternatePaymentModelValues.class);


    // With a slope of 0.1233 we're going to hit our floor around 4 battalions (or factional equivalent)
    static final double DIMINISHING_RETURNS_SLOPE = 0.1233; // higher = faster diminishing returns
    static final double DIMINISHING_RETURNS_FLOOR = 0.1; // floor: never worth less than 10%
    static final double DIMINISHING_RETURNS_POWER = 2.0; // >1 makes diminishing returns much stricter

    private final Money value;

    /**
     * Creates a new enum entry with the provided baseline value.
     *
     * @param value the baseline {@link Money} value for this category
     *
     * @author Illiani
     * @since 0.50.11
     */
    AlternatePaymentModelValues(Money value) {
        this.value = value;
    }

    /**
     * Returns the baseline {@link Money} value associated with this unit category.
     *
     * @return the baseline value for this enum constant
     *
     * @author Illiani
     * @since 0.50.11
     */
    public Money getValue() {
        return value;
    }

    /**
     * Calculates the total alternate-payment-model value of the supplied forces, applying contract multipliers and
     * (optionally) diminishing returns.
     *
     * <p>This method iterates all forces and includes only those that are:</p>
     * <ul>
     *     <li>{@code standard} (as determined by {@code force.getForceType().isStandard()}); and</li>
     *     <li>marked as a {@code combat role} (as determined by {@code force.getCombatRoleInMemory().isCombatRole()}).</li>
     * </ul>
     *
     * <p>For each included force, all units returned by {@link Formation#getUnitsAsUnits(Hangar)} are examined.
     * {@code null} units and units whose {@link Unit#getEntity()} is {@code null} are skipped.</p>
     *
     * <p>The category-specific percentage parameters are provided as whole percentages (for example, {@code 50.0} for
     * 50%) and are converted into fractional multipliers by dividing by {@code 100.0}.</p>
     *
     * <p>If {@code useDiminishingContractPay} is {@code true} and the number of included units exceeds the
     * diminishing returns start ({@link #getDiminishingReturnsStart(Faction)}), the total is computed via
     * {@link #adjustValuesForDiminishingReturns(Faction, List)}. Otherwise, the method returns the straight sum.</p>
     *
     * @param campaignFaction           the current campaign faction (used to determine the diminishing-returns cutoff)
     * @param allFormations                 the forces to evaluate
     * @param hangar                    the campaign hangar used for resolving units within forces
     * @param useDiminishingContractPay whether diminishing returns should be applied (only when relevant)
     * @param excludeInfantry           if {@code true}, infantry and battle armor entities contribute
     *                                  {@link Money#zero()} to the total
     * @param combatUnitContractPercent percentage multiplier for combat units (for example, BattleMeks, vehicles,
     *                                  fighters, ProtoMeks, etc.)
     * @param dropShipContractPercent   percentage multiplier for DropShips (large craft)
     * @param warShipContractPercent    percentage multiplier for WarShips (large craft; the non-drop, non-jump branch)
     * @param jumpShipContractPercent   percentage multiplier for JumpShips (large craft)
     *
     * @return the total alternate-payment-model value for the qualifying forces and units
     *
     * @author Illiani
     * @since 0.50.11
     */
    public static Money getForceValue(Faction campaignFaction, List<Formation> allFormations, Hangar hangar,
                                      boolean useDiminishingContractPay, boolean excludeInfantry, double combatUnitContractPercent,
                                      double dropShipContractPercent, double warShipContractPercent, double jumpShipContractPercent) {
        final double combatMultiplier = combatUnitContractPercent / 100.0;
        final double dropShipMultiplier = dropShipContractPercent / 100.0;
        final double warShipMultiplier = warShipContractPercent / 100.0;
        final double jumpShipMultiplier = jumpShipContractPercent / 100.0;

        Money total = Money.zero(); // We store this here in case we're not using diminishing returns
        List<Money> unitValues = new ArrayList<>();

        for (Formation formation : allFormations) {
            if (!formation.getForceType().isStandard() || !formation.getCombatRoleInMemory().isCombatRole()) {
                continue;
            }

            for (Unit unit : formation.getUnitsAsUnits(hangar)) {
                if (unit == null) {
                    continue;
                }
                Entity entity = unit.getEntity();
                if (entity == null) {
                    continue;
                }

                Money valueAdded = getUnitContractValue(entity,
                      excludeInfantry,
                      combatMultiplier,
                      dropShipMultiplier,
                      warShipMultiplier,
                      jumpShipMultiplier);
                unitValues.add(valueAdded);
                total = total.plus(valueAdded);
            }
        }

        if (unitValues.isEmpty()) {
            return Money.zero();
        }

        // Only process diminishing returns if it is both enabled and relevant.
        boolean isAffectedByDiminishingReturns = unitValues.size() > getDiminishingReturnsStart(campaignFaction);
        if (useDiminishingContractPay && isAffectedByDiminishingReturns) {
            return adjustValuesForDiminishingReturns(campaignFaction, unitValues);
        }

        return total;
    }


    /**
     * Computes the contract-scaled value contribution for a single {@link Entity}.
     *
     * <p>This method categorizes the entity into one of the supported unit groups and returns the corresponding
     * baseline {@link Money} value multiplied by the appropriate contract multiplier:</p>
     * <ul>
     *     <li><b>Battle Armor</b>: per-suit value multiplied by the number of active troopers, then by combat multiplier.</li>
     *     <li><b>Infantry</b>: value depends on {@link EntityMovementMode} (foot/motorized/jump/mechanized), then
     *     scaled by combat multiplier.</li>
     *     <li><b>Large Craft</b>: uses {@code dropShipMultiplier} if {@link Entity#isDropShip()},
     *     {@code jumpShipMultiplier} if {@link Entity#isJumpShip()}, otherwise {@code warShipMultiplier}.</li>
     *     <li><b>ProtoMek</b>: scaled by combat multiplier.</li>
     *     <li><b>Support Vehicle</b>: value determined by weight bands, then scaled by combat multiplier.</li>
     *     <li><b>Aerospace Fighter</b>: value determined by {@link Entity#getWeightClass()}, then scaled by combat multiplier.</li>
     *     <li><b>BattleMek</b>: value determined by {@link Entity#getWeightClass()}, then scaled by combat
     *     multiplier.</li>
     *     <li><b>Combat Vehicle</b>: value determined by {@link Entity#getWeightClass()}, then scaled by combat multiplier.</li>
     * </ul>
     *
     * <p>If {@code excludeInfantry} is {@code true}, infantry and battle armor contributions are forced to
     * {@link Money#zero()}.</p>
     *
     * <p>If an unexpected movement mode or weight class is encountered for a category, the condition is logged and
     * this method returns {@link Money#zero()} for that entity.</p>
     *
     * @param entity             the entity to evaluate
     * @param excludeInfantry    whether infantry and battle armor should be excluded
     * @param combatMultiplier   multiplier applied to combat-unit categories (provided as a fraction, e.g.
     *                           {@code 0.5})
     * @param dropShipMultiplier multiplier applied to DropShips (provided as a fraction)
     * @param warShipMultiplier  multiplier applied to WarShips (provided as a fraction)
     * @param jumpShipMultiplier multiplier applied to JumpShips (provided as a fraction)
     *
     * @return the contract-scaled value contribution of the entity, or {@link Money#zero()} if unsupported/excluded
     *
     * @author Illiani
     * @since 0.50.11
     */
    private static Money getUnitContractValue(Entity entity, boolean excludeInfantry, double combatMultiplier,
          double dropShipMultiplier, double warShipMultiplier, double jumpShipMultiplier) {
        int weightClass = entity.getWeightClass();
        if (entity.isAerospaceFighter()) {
            Money base = switch (weightClass) {
                case WEIGHT_LIGHT -> AEROSPACE_FIGHTER_LIGHT.getValue();
                case WEIGHT_MEDIUM -> AEROSPACE_FIGHTER_MEDIUM.getValue();
                case WEIGHT_HEAVY -> AEROSPACE_FIGHTER_HEAVY.getValue();
                default -> {
                    LOGGER.error(new IllegalStateException("Unexpected value (ASF): " + weightClass));
                    yield Money.zero();
                }
            };
            return base.multipliedBy(combatMultiplier);
        }

        if (entity.isBattleMek()) {
            if (entity instanceof LandAirMek) {
                return LAM.getValue();
            }

            Money base = switch (weightClass) {
                case WEIGHT_ULTRA_LIGHT, WEIGHT_LIGHT -> BATTLEMEK_LIGHT.getValue();
                case WEIGHT_MEDIUM -> BATTLEMEK_MEDIUM.getValue();
                case WEIGHT_HEAVY -> BATTLEMEK_HEAVY.getValue();
                case WEIGHT_ASSAULT -> BATTLEMEK_ASSAULT.getValue();
                case WEIGHT_SUPER_HEAVY -> BATTLEMEK_SUPER_HEAVY.getValue();
                default -> {
                    LOGGER.error(new IllegalStateException("Unexpected value (Mek): " + weightClass));
                    yield Money.zero();
                }
            };
            return base.multipliedBy(combatMultiplier);
        }

        if (entity.isCombatVehicle()) {
            Money base = switch (weightClass) {
                case WEIGHT_ULTRA_LIGHT, WEIGHT_LIGHT -> COMBAT_VEHICLE_LIGHT.getValue();
                case WEIGHT_MEDIUM -> COMBAT_VEHICLE_MEDIUM.getValue();
                case WEIGHT_HEAVY -> COMBAT_VEHICLE_HEAVY.getValue();
                case WEIGHT_ASSAULT -> COMBAT_VEHICLE_ASSAULT.getValue();
                case WEIGHT_SUPER_HEAVY -> COMBAT_VEHICLE_SUPER_HEAVY.getValue();
                default -> {
                    LOGGER.error(new IllegalStateException("Unexpected value (CV): " + weightClass));
                    yield Money.zero();
                }
            };
            return base.multipliedBy(combatMultiplier);
        }

        if (entity instanceof BattleArmor battleArmor) {
            if (excludeInfantry) {
                return Money.zero();
            }
            int suits = battleArmor.getNumberActiveTroopers();
            return BATTLE_ARMOR_PER_SUIT.getValue().multipliedBy(suits).multipliedBy(combatMultiplier);
        }

        if (entity instanceof Infantry infantry) {
            if (excludeInfantry) {
                return Money.zero();
            }
            EntityMovementMode movementMode = infantry.getMovementMode();
            Money base = switch (movementMode) {
                case INF_UMU, INF_LEG -> CONVENTIONAL_INFANTRY_FOOT.getValue();
                case INF_MOTORIZED -> CONVENTIONAL_INFANTRY_MOTORIZED.getValue();
                case INF_JUMP -> CONVENTIONAL_INFANTRY_JUMP.getValue();
                case TRACKED, WHEELED, HOVER -> CONVENTIONAL_INFANTRY_MECHANIZED.getValue();
                default -> {
                    LOGGER.error(new IllegalStateException("Unexpected value (infantry): " + movementMode));
                    yield Money.zero();
                }
            };
            return base.multipliedBy(combatMultiplier);
        }

        if (entity.isLargeCraft()) {
            double multiplier = entity.isDropShip() ?
                                      dropShipMultiplier :
                                      (entity.isJumpShip() ? jumpShipMultiplier : warShipMultiplier);
            if (multiplier <= 0) {
                return Money.zero();
            }

            return LARGE_CRAFT.getValue().multipliedBy(multiplier);
        }

        // Must be after large craft
        if (entity.isSmallCraft()) {
            return SMALL_CRAFT.getValue();
        }

        if (entity.isProtoMek()) {
            return PROTOMEK.getValue().multipliedBy(combatMultiplier);
        }

        if (entity.isSupportVehicle()) {
            double weight = entity.getWeight();
            Money base = (weight < 5.0) ?
                               SUPPORT_VEHICLE_LIGHT.getValue() :
                               (weight <= 100) ?
                                     SUPPORT_VEHICLE_MEDIUM.getValue() :
                                     (weight <= 1000) ?
                                           SUPPORT_VEHICLE_HEAVY.getValue() :
                                           SUPPORT_VEHICLE_SUPER_HEAVY.getValue();
            return base.multipliedBy(combatMultiplier);
        }

        // Must be before Aerospace Fighter
        if (entity.isConventionalFighter()) {
            return CONVENTIONAL_FIGHTER.getValue();
        }

        return Money.zero();
    }

    /**
     * Applies a diminishing-returns curve to a list of per-unit values and returns the discounted total.
     *
     * <p>The intent is to reduce the marginal value of very large forces by progressively discounting unit
     * contributions after a configurable cutoff, while leaving the first portion of the force at full value.</p>
     *
     * <p><b>How it works</b></p>
     * <ol>
     *     <li>The input {@code unitValues} list is sorted in descending order so that the highest-value units are
     *     counted first at full value (i.e., the least valuable units are discounted first).</li>
     *     <li>A cutoff index is computed as {@code 2 * battalionSize}, where {@code battalionSize} is derived from
     *     {@link CombatTeam#getStandardForceSize(Faction, int)} using {@link FormationLevel#BATTALION} depth.</li>
     *     <li>For units beyond the cutoff, a diminishing multiplier is applied:
     *     {@code multiplier = 1 / (1 + slope * distanceFromCutOff)^power}.</li>
     *     <li>The multiplier is floored at {@code minMultiplier} so unit contributions never drop below a fixed
     *     percentage of their original value.</li>
     * </ol>
     *
     * <p>Units at indices {@code 0 .. diminishingReturnsStart-1} receive a multiplier of {@code 1.0} (no discount).
     * The first discounted unit uses {@code distanceFromCutOff = 1}.</p>
     *
     * <p><b>Side effect:</b> this method sorts {@code unitValues} in-place. If callers need the original ordering
     * preserved, pass a copy of the list instead.</p>
     *
     * @param campaignFaction the campaign's faction, used to determine the factional-equivalent battalion size for the
     *                        cutoff computation; must not be {@code null}
     * @param unitValues      a list of per-unit {@link Money} values to be summed with diminishing returns applied;
     *                        must not be {@code null}
     *
     * @return the discounted total {@link Money} value after applying diminishing returns
     *
     * @author Illiani
     * @since 0.50.11
     */
    public static Money adjustValuesForDiminishingReturns(Faction campaignFaction, List<Money> unitValues) {
        // Discount the least valuable units first
        unitValues.sort(Comparator.reverseOrder());

        int diminishingReturnsStart = getDiminishingReturnsStart(campaignFaction);

        Money total = Money.zero();
        for (int i = 0; i < unitValues.size(); i++) {
            Money unitValue = unitValues.get(i);

            double multiplier = 1.0;
            if (i >= diminishingReturnsStart) {
                int distanceFromCutOff = (i - diminishingReturnsStart) + 1;
                multiplier = 1.0 / Math.pow(1.0 + DIMINISHING_RETURNS_SLOPE * distanceFromCutOff,
                      DIMINISHING_RETURNS_POWER);
                multiplier = Math.max(DIMINISHING_RETURNS_FLOOR, multiplier);
            }

            total = total.plus(unitValue.multipliedBy(multiplier));
        }

        return total;
    }

    /**
     * Returns the unit-count index at which diminishing returns begin for the given campaign faction.
     *
     * <p>The cutoff is defined as {@code 2 * battalionSize}, where {@code battalionSize} is the faction-adjusted
     * standard force size computed by {@link CombatTeam#getStandardForceSize(Faction, int)} for a
     * {@link FormationLevel#BATTALION} formation depth.</p>
     *
     * <p>Units with indices {@code 0 .. (start - 1)} are not discounted; the first discounted unit is at index
     * {@code start}. Callers typically compare the total unit count against this value to determine whether diminishing
     * returns are relevant.</p>
     *
     * @param campaignFaction the campaign faction used to determine the factional-equivalent battalion size; must not
     *                        be {@code null}
     *
     * @return the zero-based cutoff index where diminishing returns start
     *
     * @author Illiani
     * @since 0.50.11
     */
    public static int getDiminishingReturnsStart(Faction campaignFaction) {
        return CombatTeam.getStandardForceSize(campaignFaction, BATTALION.getDepth()) * 2;
    }
}
