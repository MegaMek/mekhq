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
package mekhq.campaign.mission.scenarios.salvage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.SmallCraft;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

/**
 * The player's plan for recovering a scenario's wrecks, and whether it works.
 *
 * <p>Each wreck has a {@link WreckRecovery} recording which units the player assigned to it. After any change,
 * {@link #revalidate()} works out whether each wreck can be recovered:</p>
 * <ul>
 *   <li>In space, a DropShip or larger needs a naval tug; fighters and small craft need a unit with a suitable bay;
 *   anything else must fit in a single unit's cargo space.</li>
 *   <li>On the ground, the wreck must fit in a single unit's cargo space, or the assigned units must be able to drag
 *   it between them.</li>
 *   <li>Where the salvage system lets a carrying unit recover several wrecks (see
 *   {@link AbstractSalvage#isMultipleSalvagePerUnitAllowed()}), a single unit carrying a wreck shares its cargo
 *   space (or, in space, its bays) with any other wrecks it carries. A unit that drags or tugs a wreck, or that teams
 *   up with another unit, is committed to that wreck alone.</li>
 *   <li>Without salvage operations (see {@link AbstractSalvage#isUseSalvageOperations()}), every wreck is recovered
 *   automatically.</li>
 * </ul>
 *
 * <p>This class holds no user interface; the post-scenario salvage picker reflects its results.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class SalvageRecoveryPlan {
    /** Allowance for floating-point error when summing cargo tonnages. */
    private static final double CAPACITY_TOLERANCE = 0.0001;

    private final AbstractSalvage salvageRules;
    private final boolean isInSpace;
    private final List<WreckRecovery> recoveries = new ArrayList<>();

    /**
     * The share of a recovery unit's cargo space or bays taken up by a wreck it carries.
     *
     * @param carrier      the unit carrying the wreck
     * @param cargoTons    the cargo space the wreck takes up, in tons ({@code 0} if carried in a bay)
     * @param isBayLoad    {@code true} if the wreck is a fighter or small craft carried in a bay
     * @param isSmallCraft {@code true} if the wreck is a small craft, which needs a small craft bay
     *
     * @author Illiani
     * @since 0.51.01
     */
    record CarryLoad(Unit carrier, double cargoTons, boolean isBayLoad, boolean isSmallCraft) {}

    /**
     * How much room a carrier has left for more wrecks.
     *
     * <p>A carrier can carry wrecks in its bays and its cargo space at the same time, so both are reported.</p>
     *
     * @param isBayCapacity   {@code true} if the carrier is carrying wrecks in its bays
     * @param freeBays        the number of bay slots still free, if carrying in bays
     * @param isCargoCapacity {@code true} if the carrier is carrying wrecks in its cargo space
     * @param freeCargoTons   the cargo space still free, in tons, if carrying in cargo space
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record RemainingCapacity(boolean isBayCapacity, int freeBays, boolean isCargoCapacity,
          double freeCargoTons) {}

    /**
     * Creates an empty recovery plan.
     *
     * @param salvageRules the rules of the campaign's salvage system
     * @param isInSpace    {@code true} if the salvage operation takes place in space
     *
     * @author Illiani
     * @since 0.51.01
     */
    public SalvageRecoveryPlan(AbstractSalvage salvageRules, boolean isInSpace) {
        this.salvageRules = salvageRules;
        this.isInSpace = isInSpace;
    }

    /**
     * Adds a wreck to be recovered.
     *
     * @param wreck the wreck
     *
     * @return the wreck's recovery, which records the units assigned to it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public WreckRecovery addWreck(TestUnit wreck) {
        WreckRecovery recovery = new WreckRecovery(wreck);
        recoveries.add(recovery);
        return recovery;
    }

    /**
     * @return every wreck's recovery, in the order they were added
     */
    public List<WreckRecovery> getRecoveries() {
        return Collections.unmodifiableList(recoveries);
    }

    /**
     * Checks whether the player chooses if a unit carries or drags a wreck. This is only offered on the ground, where
     * a carrying unit may recover several wrecks, as there it decides whether the unit is free to take on more.
     *
     * @return {@code true} if the player may choose how a single unit recovers a wreck
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isRecoveryMethodChoiceOffered() {
        return salvageRules.isUseSalvageOperations() && salvageRules.isMultipleSalvagePerUnitAllowed() && !isInSpace;
    }

    /**
     * Works out whether each wreck can be recovered, after any change to the plan.
     *
     * <p>Each wreck is first checked on its own. Where carriers may take on several wrecks, each wreck's share of
     * its carrier's cargo space or bays is then worked out, and wrecks that overload a carrier, or that use a unit
     * committed elsewhere, can no longer be recovered.</p>
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void revalidate() {
        for (WreckRecovery recovery : recoveries) {
            updateRecoveryMethod(recovery);
            recovery.status = getStatusAlone(recovery);
            recovery.carryLoad = getSharedCarryLoad(recovery);
        }

        if (salvageRules.isMultipleSalvagePerUnitAllowed()) {
            applySharedCapacityLimits();
            labelRecoveryMethods();
        }
    }

    /**
     * Checks whether a unit could be assigned to a wreck.
     *
     * <p>A unit not assigned anywhere else is always offered. Where carriers may take on several wrecks, a unit that
     * is only carrying salvage elsewhere is also offered, provided it would carry this wreck alone and still has room
     * for it.</p>
     *
     * @param recovery      the wreck's recovery
     * @param unit          the unit
     * @param otherSlotUnit the unit in the wreck's other recovery slot, or {@code null}
     *
     * @return {@code true} if the unit could be assigned
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isOffered(WreckRecovery recovery, Unit unit, @Nullable Unit otherSlotUnit) {
        if (unit == otherSlotUnit) {
            return false; // The same unit can't fill both slots
        }

        List<WreckRecovery> otherUses = getRecoveriesUsing(unit, recovery);
        if (otherUses.isEmpty()) {
            return true;
        }

        if (!salvageRules.isMultipleSalvagePerUnitAllowed() || (otherSlotUnit != null)) {
            return false; // Two-unit teams are committed to a single wreck
        }

        for (WreckRecovery otherUse : otherUses) {
            if (otherUse.carryLoad == null) {
                return false; // Committed elsewhere
            }
        }

        return canCarryAdditionally(unit, recovery.getWreck(), otherUses);
    }

    /**
     * Gets how much room a carrier has left for more wrecks.
     *
     * @param carrier the unit to check
     *
     * @return the carrier's remaining capacity, or {@code null} if it isn't carrying anything
     *
     * @author Illiani
     * @since 0.51.01
     */
    public @Nullable RemainingCapacity getRemainingCapacity(Unit carrier) {
        double cargoTonsUsed = 0.0;
        int vesselsCarried = 0;
        boolean isCarryingCargo = false;
        for (WreckRecovery recovery : recoveries) {
            CarryLoad carryLoad = recovery.carryLoad;
            if ((carryLoad != null) && (carryLoad.carrier() == carrier)) {
                if (carryLoad.isBayLoad()) {
                    vesselsCarried++;
                } else {
                    isCarryingCargo = true;
                    cargoTonsUsed += carryLoad.cargoTons();
                }
            }
        }

        boolean isCarryingInBays = vesselsCarried > 0;
        if (!isCarryingInBays && !isCarryingCargo) {
            return null;
        }

        int freeBays = 0;
        if (isCarryingInBays) {
            int baySlots = CamOpsSalvageUtilities.getFreeSmallCraftBaySlots(carrier) +
                                 CamOpsSalvageUtilities.getFreeFighterBaySlots(carrier);
            freeBays = Math.max(0, baySlots - vesselsCarried);
        }
        double freeCargoTons = isCarryingCargo ? Math.max(0.0, getCargoCapacity(carrier) - cargoTonsUsed) : 0.0;
        return new RemainingCapacity(isCarryingInBays, freeBays, isCarryingCargo, freeCargoTons);
    }

    /**
     * Checks whether a wreck's assigned units could recover it, ignoring any other wrecks they are assigned to.
     *
     * @param recovery the wreck's recovery
     *
     * @return the wreck's recovery status
     *
     * @author Illiani
     * @since 0.50.10
     */
    private RecoveryStatus getStatusAlone(WreckRecovery recovery) {
        if (!salvageRules.isUseSalvageOperations()) {
            return RecoveryStatus.RECOVERED_AUTOMATICALLY;
        }

        Unit firstUnit = recovery.getFirstUnit();
        Unit secondUnit = recovery.getSecondUnit();
        if (!recovery.hasRecoveryUnits()) {
            return RecoveryStatus.UNASSIGNED;
        }

        Entity targetEntity = recovery.getWreck().getEntity();
        double targetWeight = (targetEntity == null) ? 0.0 : targetEntity.getWeight();

        if (isInSpace && (targetEntity != null)) {
            if (isLargeVessel(targetEntity)) {
                return (hasNavalTug(firstUnit) || hasNavalTug(secondUnit)) ?
                             RecoveryStatus.RECOVERED :
                             RecoveryStatus.NO_NAVAL_TUG;
            }

            if (isSmallVessel(targetEntity)) {
                // It needs a free slot in a suitable bay with working doors: fighters fit in fighter or small craft
                // bays, small craft only in small craft bays
                int smallCraftCarried = isSmallCraft(targetEntity) ? 1 : 0;
                boolean hasSuitableBay = ((firstUnit != null) && hasBaysFor(firstUnit, 1, smallCraftCarried)) ||
                                               ((secondUnit != null) && hasBaysFor(secondUnit, 1, smallCraftCarried));
                return hasSuitableBay ?
                             RecoveryStatus.RECOVERED :
                             RecoveryStatus.NO_SUITABLE_BAY_EQUIPMENT;
            }
        }

        // A single wreck can't be split between two units' cargo space
        double bestCargoCapacity = Math.max(getCargoCapacity(firstUnit), getCargoCapacity(secondUnit));
        double combinedTowCapacity = getTowCapacity(firstUnit) + getTowCapacity(secondUnit);

        RecoveryMethod recoveryMethod = recovery.getRecoveryMethod();
        if (recoveryMethod == RecoveryMethod.CARRY) {
            if ((firstUnit != null) && (secondUnit != null)) {
                return RecoveryStatus.CARRY_NEEDS_SINGLE_UNIT;
            }
            return (bestCargoCapacity >= targetWeight) ?
                         RecoveryStatus.RECOVERED :
                         RecoveryStatus.NO_CARGO_CAPACITY;
        }
        if (recoveryMethod == RecoveryMethod.DRAG) {
            return (combinedTowCapacity >= targetWeight) ?
                         RecoveryStatus.RECOVERED :
                         RecoveryStatus.NO_TOW_CAPACITY;
        }

        if (bestCargoCapacity >= targetWeight) {
            return RecoveryStatus.RECOVERED;
        }

        // There's no dragging in space
        if (isInSpace) {
            return RecoveryStatus.NO_CARGO_CAPACITY;
        }

        if (combinedTowCapacity >= targetWeight) {
            return RecoveryStatus.RECOVERED;
        }

        // Report whichever option came closest
        return (combinedTowCapacity >= bestCargoCapacity) ?
                     RecoveryStatus.NO_TOW_CAPACITY :
                     RecoveryStatus.NO_CARGO_CAPACITY;
    }

    /**
     * Works out how the assigned units recover a wreck on the ground.
     *
     * <p>The player's choice always stands, even when the units can't recover the wreck that way; the wreck's status
     * then says why. Without a choice, a single unit carries the wreck if it can, as that leaves it free to take on
     * more salvage, and otherwise drags it; two units drag it together.</p>
     *
     * @param recovery the wreck's recovery
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void updateRecoveryMethod(WreckRecovery recovery) {
        recovery.recoveryMethod = null;
        if (!isRecoveryMethodChoiceOffered() || !recovery.hasRecoveryUnits()) {
            return;
        }

        RecoveryMethod preferredMethod = recovery.getPreferredRecoveryMethod();
        recovery.recoveryMethod = (preferredMethod != null) ? preferredMethod : getDefaultRecoveryMethod(recovery);
    }

    private RecoveryMethod getDefaultRecoveryMethod(WreckRecovery recovery) {
        List<Unit> recoveryUnits = recovery.getRecoveryUnits();
        if (recoveryUnits.size() > 1) {
            return RecoveryMethod.DRAG;
        }

        Entity targetEntity = recovery.getWreck().getEntity();
        if (targetEntity == null) {
            return RecoveryMethod.CARRY;
        }

        Unit unit = recoveryUnits.getFirst();
        double targetWeight = targetEntity.getWeight();
        double cargoCapacity = getCargoCapacity(unit);
        if (cargoCapacity >= targetWeight) {
            return RecoveryMethod.CARRY;
        }

        // Dragging if it works; otherwise whichever comes closer, so the status reports the smaller shortfall
        double towCapacity = getTowCapacity(unit);
        return ((towCapacity >= targetWeight) || (towCapacity > cargoCapacity)) ?
                     RecoveryMethod.DRAG :
                     RecoveryMethod.CARRY;
    }

    /**
     * Works out a wreck's share of its carrier's cargo space or bays.
     *
     * <p>A wreck only takes a share when a single unit is assigned, and that unit carries it rather than dragging or
     * tugging it. Carried wrecks go in the carrier's cargo space, except fighters and small craft in space, which go
     * in its bays.</p>
     *
     * @param recovery the wreck's recovery
     *
     * @return the wreck's share of its carrier's capacity, or {@code null} if it doesn't share its carrier
     *
     * @author Illiani
     * @since 0.51.01
     */
    private @Nullable CarryLoad getSharedCarryLoad(WreckRecovery recovery) {
        if (!salvageRules.isMultipleSalvagePerUnitAllowed() || !recovery.isRecovered()) {
            return null;
        }

        List<Unit> recoveryUnits = recovery.getRecoveryUnits();
        if (recoveryUnits.size() != 1) {
            return null; // Nothing assigned, or a two-unit team, which is committed to this wreck
        }

        Entity targetEntity = recovery.getWreck().getEntity();
        if (targetEntity == null) {
            return null;
        }

        Unit carrier = recoveryUnits.getFirst();
        if (isInSpace) {
            if (isLargeVessel(targetEntity)) {
                return null; // Tugged, which commits the carrier
            }
            if (isSmallVessel(targetEntity)) {
                return new CarryLoad(carrier, 0.0, true, isSmallCraft(targetEntity));
            }
        }

        double targetWeight = targetEntity.getWeight();
        boolean canCarry = getCargoCapacity(carrier) >= targetWeight;
        boolean isDragChosen = recovery.recoveryMethod == RecoveryMethod.DRAG;
        if (canCarry && !isDragChosen) {
            return new CarryLoad(carrier, targetWeight, false, false);
        }

        return null; // Dragged, which commits the carrier
    }

    /**
     * Stops recovering wrecks that overload a carrier's cargo space or bays, or that use a unit committed elsewhere.
     *
     * <p>A unit either carries salvage (sharing its capacity between wrecks) or is committed to a single wreck,
     * dragging or tugging it alone or as part of a two-unit team. It can never do both.</p>
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void applySharedCapacityLimits() {
        List<WreckRecovery> conflictingRecoveries = new ArrayList<>();

        // A committed unit can't be used anywhere else
        for (WreckRecovery recovery : recoveries) {
            if (!recovery.isRecovered() || (recovery.carryLoad != null)) {
                continue;
            }
            for (Unit unit : recovery.getRecoveryUnits()) {
                for (WreckRecovery otherRecovery : getRecoveriesUsing(unit, recovery)) {
                    conflictingRecoveries.add(recovery);
                    conflictingRecoveries.add(otherRecovery);
                }
            }
        }

        // Carriers can't take on more than fits
        for (Map.Entry<Unit, List<WreckRecovery>> entry : getRecoveriesByCarrier().entrySet()) {
            Unit carrier = entry.getKey();

            List<WreckRecovery> cargoRecoveries = new ArrayList<>();
            List<WreckRecovery> bayRecoveries = new ArrayList<>();
            double cargoTonsUsed = 0.0;
            int smallCraftCarried = 0;
            for (WreckRecovery recovery : entry.getValue()) {
                if (recovery.carryLoad.isBayLoad()) {
                    bayRecoveries.add(recovery);
                    if (recovery.carryLoad.isSmallCraft()) {
                        smallCraftCarried++;
                    }
                } else {
                    cargoRecoveries.add(recovery);
                    cargoTonsUsed += recovery.carryLoad.cargoTons();
                }
            }

            if (cargoTonsUsed > getCargoCapacity(carrier) + CAPACITY_TOLERANCE) {
                for (WreckRecovery recovery : cargoRecoveries) {
                    stopRecovery(recovery, RecoveryStatus.CARGO_FULL);
                }
            }

            if (!hasBaysFor(carrier, bayRecoveries.size(), smallCraftCarried)) {
                for (WreckRecovery recovery : bayRecoveries) {
                    stopRecovery(recovery, RecoveryStatus.NO_FREE_BAY);
                }
            }
        }

        for (WreckRecovery recovery : conflictingRecoveries) {
            stopRecovery(recovery, RecoveryStatus.UNIT_IN_USE);
        }
    }

    private static void stopRecovery(WreckRecovery recovery, RecoveryStatus status) {
        recovery.status = status;
        recovery.carryLoad = null;
    }

    /**
     * Records how each recovered wreck is brought in: carried in cargo, carried in a bay, or dragged.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void labelRecoveryMethods() {
        for (WreckRecovery recovery : recoveries) {
            if (recovery.status != RecoveryStatus.RECOVERED) {
                continue;
            }

            if (recovery.carryLoad == null) {
                recovery.status = RecoveryStatus.COMMITTED;
            } else {
                recovery.status = recovery.carryLoad.isBayLoad() ?
                                        RecoveryStatus.CARRIED_IN_BAY :
                                        RecoveryStatus.CARRIED_IN_CARGO;
            }
        }
    }

    /**
     * Checks whether a carrier has room for another wreck on top of those it already carries.
     *
     * @param carrier            the carrying unit
     * @param target             the additional wreck
     * @param carriedRecoveries  the wrecks the carrier already carries
     *
     * @return {@code true} if the additional wreck would fit
     *
     * @author Illiani
     * @since 0.51.01
     */
    private boolean canCarryAdditionally(Unit carrier, TestUnit target, List<WreckRecovery> carriedRecoveries) {
        Entity targetEntity = target.getEntity();
        if (targetEntity == null) {
            return false;
        }

        if (isInSpace && isLargeVessel(targetEntity)) {
            return false; // Tugging commits the carrier
        }

        if (isInSpace && isSmallVessel(targetEntity)) {
            int vesselsCarried = 1;
            int smallCraftCarried = isSmallCraft(targetEntity) ? 1 : 0;
            for (WreckRecovery carriedRecovery : carriedRecoveries) {
                if (carriedRecovery.carryLoad.isBayLoad()) {
                    vesselsCarried++;
                    if (carriedRecovery.carryLoad.isSmallCraft()) {
                        smallCraftCarried++;
                    }
                }
            }
            return hasBaysFor(carrier, vesselsCarried, smallCraftCarried);
        }

        double cargoTonsUsed = targetEntity.getWeight();
        for (WreckRecovery carriedRecovery : carriedRecoveries) {
            cargoTonsUsed += carriedRecovery.carryLoad.cargoTons();
        }
        return cargoTonsUsed <= getCargoCapacity(carrier) + CAPACITY_TOLERANCE;
    }

    /**
     * Groups the wrecks that share a carrier's capacity by their carrier.
     *
     * @return the wrecks carried by each carrier
     */
    private Map<Unit, List<WreckRecovery>> getRecoveriesByCarrier() {
        Map<Unit, List<WreckRecovery>> recoveriesByCarrier = new LinkedHashMap<>();
        for (WreckRecovery recovery : recoveries) {
            if (recovery.carryLoad != null) {
                recoveriesByCarrier.computeIfAbsent(recovery.carryLoad.carrier(), carrier -> new ArrayList<>())
                      .add(recovery);
            }
        }
        return recoveriesByCarrier;
    }

    /**
     * Finds the other wrecks a unit is assigned to.
     *
     * @param unit              the unit to look for
     * @param excludedRecovery  a wreck to ignore, usually the one being checked
     *
     * @return every other wreck with the unit assigned in either slot
     */
    private List<WreckRecovery> getRecoveriesUsing(Unit unit, WreckRecovery excludedRecovery) {
        List<WreckRecovery> recoveriesUsing = new ArrayList<>();
        for (WreckRecovery recovery : recoveries) {
            if ((recovery != excludedRecovery) && recovery.getRecoveryUnits().contains(unit)) {
                recoveriesUsing.add(recovery);
            }
        }
        return recoveriesUsing;
    }

    /**
     * Checks whether a carrier has enough free bay slots for the fighters and small craft assigned to it. Each wreck
     * fills one slot in a bay with a working door; the number of doors doesn't matter (see
     * {@link CamOpsSalvageUtilities#getFreeFighterBaySlots(Unit)}). Fighters fit in fighter or small craft bays;
     * small craft only fit in small craft bays.
     *
     * @param carrier           the carrying unit
     * @param vesselsCarried    the total number of fighters and small craft assigned to the carrier
     * @param smallCraftCarried how many of those are small craft
     *
     * @return {@code true} if every assigned fighter and small craft has a bay slot
     */
    private static boolean hasBaysFor(Unit carrier, int vesselsCarried, int smallCraftCarried) {
        if (vesselsCarried == 0) {
            return true;
        }

        int smallCraftSlots = CamOpsSalvageUtilities.getFreeSmallCraftBaySlots(carrier);
        int fighterSlots = CamOpsSalvageUtilities.getFreeFighterBaySlots(carrier);
        return (smallCraftCarried <= smallCraftSlots) && (vesselsCarried <= smallCraftSlots + fighterSlots);
    }

    private static boolean hasNavalTug(@Nullable Unit unit) {
        return (unit != null) && (unit.getEntity() != null) && CamOpsSalvageUtilities.hasNavalTug(unit.getEntity());
    }

    private static double getCargoCapacity(@Nullable Unit unit) {
        return (unit == null) ? 0.0 : unit.getCargoCapacityForSalvage();
    }

    private static double getTowCapacity(@Nullable Unit unit) {
        return (unit == null) ? 0.0 : CamOpsSalvageUtilities.getTowCapacity(unit);
    }

    /** Jumpship includes WarShips. */
    private static boolean isLargeVessel(Entity entity) {
        return (entity instanceof Dropship) || (entity instanceof Jumpship);
    }

    /** Dropship extends SmallCraft, so large vessels must be excluded. */
    private static boolean isSmallVessel(Entity entity) {
        return !isLargeVessel(entity) && ((entity instanceof SmallCraft) || (entity instanceof AeroSpaceFighter));
    }

    private static boolean isSmallCraft(Entity entity) {
        return !isLargeVessel(entity) && (entity instanceof SmallCraft);
    }
}
