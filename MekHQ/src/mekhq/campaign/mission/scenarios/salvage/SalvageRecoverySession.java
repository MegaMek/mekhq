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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import megamek.common.annotations.Nullable;
import mekhq.campaign.finances.Money;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

/**
 * Everything the player decides after a scenario about its salvage: which units recover which wrecks (see
 * {@link SalvageRecoveryPlan}), and what happens to each recovered wreck (see {@link SalvageClaim}).
 *
 * <p>The session keeps the running tallies the player needs to decide - tech time used, the value of salvage kept,
 * sold, and left to the employer, the salvage percentage, purchase costs and cash shares - and says what, if
 * anything, stops the player confirming (see {@link ConfirmBlocker}). It holds no user interface; the recovery
 * console renders it.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class SalvageRecoverySession {
    private final AbstractSalvage salvageRules;
    private final SalvageSettlement settlement;
    private final SalvageRecoveryPlan plan;
    private final List<Unit> recoveryUnits;
    private final Map<WreckRecovery, SalvageClaim> claims = new HashMap<>();
    private final Map<UUID, RecoveryTimeData> recoveryTimes;
    private final int availableMinutes;
    private final Money unitSalvageInitial;
    private final Money employerSalvageInitial;
    private final Money availableFunds;

    /**
     * What happens to a recovered wreck.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public enum SalvageClaim {
        /** The wreck goes to the employer; under some settlements the player is paid a share of its value. */
        EMPLOYER,
        /** The player keeps the wreck. Under salvage purchases, this means buying it. */
        KEEP,
        /** The player sells the wreck straight away. */
        SELL
    }

    /**
     * Something that stops the player confirming their salvage.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public enum ConfirmBlocker {
        /** Units are assigned to wrecks they can't recover. */
        UNRECOVERABLE_ASSIGNMENTS,
        /** The player is claiming salvage while over their salvage cap. */
        OVER_SALVAGE_CAP,
        /** The player can't afford the salvage they're buying. */
        UNAFFORDABLE_PURCHASES,
        /** The recoveries need more tech time than the salvage techs have. */
        NOT_ENOUGH_TECH_TIME
    }

    /**
     * Starts a salvage session.
     *
     * @param salvageRules           the rules of the campaign's salvage system
     * @param settlement             how the salvage is divided between the player and their employer
     * @param isInSpace              {@code true} if the salvage operation takes place in space
     * @param wrecks                 the wrecks to recover; shown most valuable first
     * @param recoveryUnits          the units available to recover wrecks
     * @param recoveryTimes          each wreck's recovery time, by unit ID
     * @param availableMinutes       the tech time available for recovering wrecks
     * @param unitSalvageInitial     the salvage value the player had already claimed this contract
     * @param employerSalvageInitial the salvage value the employer had already received this contract
     * @param availableFunds         the player's funds, for checking that purchases can be afforded
     */
    public SalvageRecoverySession(AbstractSalvage salvageRules, SalvageSettlement settlement, boolean isInSpace,
          List<TestUnit> wrecks, List<Unit> recoveryUnits, Map<UUID, RecoveryTimeData> recoveryTimes,
          int availableMinutes, Money unitSalvageInitial, Money employerSalvageInitial, Money availableFunds) {
        this.salvageRules = salvageRules;
        this.settlement = settlement;
        this.plan = new SalvageRecoveryPlan(salvageRules, isInSpace);
        this.recoveryUnits = List.copyOf(recoveryUnits);
        this.recoveryTimes = Map.copyOf(recoveryTimes);
        this.availableMinutes = Math.max(0, availableMinutes);
        this.unitSalvageInitial = unitSalvageInitial;
        this.employerSalvageInitial = employerSalvageInitial;
        this.availableFunds = availableFunds;

        List<TestUnit> sortedWrecks = new ArrayList<>(wrecks);
        sortedWrecks.sort(Comparator.comparing(TestUnit::getSellValue).reversed()); // Most valuable first
        for (TestUnit wreck : sortedWrecks) {
            claims.put(plan.addWreck(wreck), SalvageClaim.EMPLOYER);
        }
        revalidate();
    }

    // region Accessors

    public AbstractSalvage getSalvageRules() {
        return salvageRules;
    }

    public SalvageSettlement getSettlement() {
        return settlement;
    }

    public SalvageRecoveryPlan getPlan() {
        return plan;
    }

    /**
     * @return every wreck's recovery, most valuable wreck first
     */
    public List<WreckRecovery> getRecoveries() {
        return plan.getRecoveries();
    }

    /**
     * @return the units available to recover wrecks
     */
    public List<Unit> getRecoveryUnits() {
        return recoveryUnits;
    }

    public int getAvailableMinutes() {
        return availableMinutes;
    }

    public Money getAvailableFunds() {
        return availableFunds;
    }

    /**
     * @return {@code true} if the player assigns units to recover wrecks; otherwise every wreck is recovered
     *       automatically
     */
    public boolean isUsingSalvageOperations() {
        return salvageRules.isUseSalvageOperations();
    }

    // endregion Accessors

    // region Assignments

    /**
     * Assigns the units that recover a wreck.
     *
     * @param recovery   the wreck's recovery
     * @param firstUnit  the first recovery unit, or {@code null}
     * @param secondUnit the second recovery unit, or {@code null}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void assign(WreckRecovery recovery, @Nullable Unit firstUnit, @Nullable Unit secondUnit) {
        recovery.setRecoveryUnits(firstUnit, secondUnit);
        revalidate();
    }

    /**
     * Assigns a unit to the first free slot of a wreck's recovery.
     *
     * @param recovery the wreck's recovery
     * @param unit     the unit to assign
     *
     * @return {@code true} if the unit was assigned; {@code false} if both slots are taken, or the unit couldn't be
     *       assigned there
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean assignToFreeSlot(WreckRecovery recovery, Unit unit) {
        Unit firstUnit = recovery.getFirstUnit();
        Unit secondUnit = recovery.getSecondUnit();
        if (firstUnit == null) {
            if (!plan.isOffered(recovery, unit, secondUnit)) {
                return false;
            }
            assign(recovery, unit, secondUnit);
            return true;
        }
        if (secondUnit == null) {
            if (!plan.isOffered(recovery, unit, firstUnit)) {
                return false;
            }
            assign(recovery, firstUnit, unit);
            return true;
        }
        return false;
    }

    /**
     * Sets how the player would like the assigned units to recover a wreck. If they can't recover it that way, the
     * wreck's status says why.
     *
     * @param recovery the wreck's recovery
     * @param method   the player's choice
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setPreferredRecoveryMethod(WreckRecovery recovery, @Nullable RecoveryMethod method) {
        recovery.setPreferredRecoveryMethod(method);
        revalidate();
    }

    /**
     * Unassigns every recovery unit.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void clearAssignments() {
        for (WreckRecovery recovery : getRecoveries()) {
            recovery.setRecoveryUnits(null, null);
        }
        revalidate();
    }

    /**
     * Assigns recovery units to unassigned wrecks, most valuable first, within the available tech time. Wrecks the
     * player has already assigned are left alone.
     *
     * <p>Each wreck gets the single unit with the least capacity that can recover it, keeping larger units free for
     * larger wrecks; failing that, the first pair of units that can recover it together.</p>
     *
     * @return the number of wrecks assigned
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int autoAssign() {
        if (!isUsingSalvageOperations()) {
            return 0;
        }

        int assignedCount = 0;
        for (WreckRecovery recovery : getRecoveries()) {
            if (recovery.hasRecoveryUnits()) {
                continue;
            }
            if (getUsedMinutes() + getRecoveryMinutes(recovery) > availableMinutes) {
                continue;
            }
            if (tryAssignSingleUnit(recovery) || tryAssignPair(recovery)) {
                assignedCount++;
            }
        }
        return assignedCount;
    }

    private boolean tryAssignSingleUnit(WreckRecovery recovery) {
        List<Unit> candidates = new ArrayList<>();
        for (Unit unit : recoveryUnits) {
            if (plan.isOffered(recovery, unit, null)) {
                candidates.add(unit);
            }
        }
        candidates.sort(Comparator.comparingDouble(SalvageRecoverySession::getLargestCapacity));

        for (Unit unit : candidates) {
            if (tryAssignment(recovery, unit, null)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryAssignPair(WreckRecovery recovery) {
        for (int first = 0; first < recoveryUnits.size(); first++) {
            Unit firstUnit = recoveryUnits.get(first);
            if (!plan.isOffered(recovery, firstUnit, null)) {
                continue;
            }
            for (int second = first + 1; second < recoveryUnits.size(); second++) {
                Unit secondUnit = recoveryUnits.get(second);
                if (plan.isOffered(recovery, secondUnit, firstUnit) && tryAssignment(recovery, firstUnit, secondUnit)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryAssignment(WreckRecovery recovery, Unit firstUnit, @Nullable Unit secondUnit) {
        assign(recovery, firstUnit, secondUnit);
        if (recovery.isRecovered()) {
            return true;
        }
        assign(recovery, null, null);
        return false;
    }

    private static double getLargestCapacity(Unit unit) {
        return Math.max(unit.getCargoCapacityForSalvage(), CamOpsSalvageUtilities.getTowCapacity(unit));
    }

    // endregion Assignments

    // region Claims

    /**
     * @return the claims the settlement lets the player make on a recovered wreck, in the order they are offered
     */
    public List<SalvageClaim> getAvailableClaims() {
        List<SalvageClaim> availableClaims = new ArrayList<>();
        availableClaims.add(SalvageClaim.EMPLOYER);
        if (settlement.canKeepSalvage()) {
            availableClaims.add(SalvageClaim.KEEP);
        }
        if (settlement.canSellSalvage()) {
            availableClaims.add(SalvageClaim.SELL);
        }
        return availableClaims;
    }

    /**
     * @param recovery the wreck's recovery
     *
     * @return what happens to the wreck if it is recovered
     */
    public SalvageClaim getClaim(WreckRecovery recovery) {
        return claims.getOrDefault(recovery, SalvageClaim.EMPLOYER);
    }

    /**
     * Sets what happens to a recovered wreck.
     *
     * @param recovery the wreck's recovery
     * @param claim    the claim
     *
     * @return {@code true} if the claim was made; {@code false} if the wreck isn't recovered, or the settlement doesn't
     *       allow the claim
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean setClaim(WreckRecovery recovery, SalvageClaim claim) {
        if (!recovery.isRecovered() || !getAvailableClaims().contains(claim)) {
            return false;
        }
        claims.put(recovery, claim);
        return true;
    }

    // endregion Claims

    /**
     * Works out whether each wreck can be recovered after a change, and hands unrecovered wrecks back to the employer.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void revalidate() {
        plan.revalidate();
        for (WreckRecovery recovery : getRecoveries()) {
            if (!recovery.isRecovered()) {
                claims.put(recovery, SalvageClaim.EMPLOYER);
            }
        }
    }

    // region Tallies

    /**
     * @param recovery the wreck's recovery
     *
     * @return the tech time recovering the wreck takes, in minutes
     */
    public int getRecoveryMinutes(WreckRecovery recovery) {
        RecoveryTimeData timeData = recoveryTimes.get(recovery.getWreck().getId());
        return (timeData == null) ? 0 : timeData.totalRecoveryTime();
    }

    /**
     * @param recovery the wreck's recovery
     *
     * @return the breakdown of the wreck's recovery time, or {@code null} if it isn't known
     */
    public @Nullable RecoveryTimeData getRecoveryTimeData(WreckRecovery recovery) {
        return recoveryTimes.get(recovery.getWreck().getId());
    }

    /**
     * @return the tech time used by the wrecks units are assigned to, in minutes
     */
    public int getUsedMinutes() {
        long usedMinutes = 0;
        for (WreckRecovery recovery : getRecoveries()) {
            if (recovery.hasRecoveryUnits()) {
                usedMinutes += getRecoveryMinutes(recovery);
            }
        }
        // Saturate rather than overflow, so an absurd recovery time can't wrap around to negative
        return (int) Math.min(Integer.MAX_VALUE, usedMinutes);
    }

    /**
     * @return the recovered wrecks the player keeps (or, under salvage purchases, buys)
     */
    public List<TestUnit> getKeptSalvage() {
        return getRecoveredSalvage(SalvageClaim.KEEP);
    }

    /**
     * @return the recovered wrecks the player sells
     */
    public List<TestUnit> getSoldSalvage() {
        return getRecoveredSalvage(SalvageClaim.SELL);
    }

    /**
     * @return the recovered wrecks that go to the employer
     */
    public List<TestUnit> getEmployerSalvage() {
        return getRecoveredSalvage(SalvageClaim.EMPLOYER);
    }

    private List<TestUnit> getRecoveredSalvage(SalvageClaim claim) {
        List<TestUnit> salvage = new ArrayList<>();
        for (WreckRecovery recovery : getRecoveries()) {
            if (recovery.isRecovered() && (getClaim(recovery) == claim)) {
                salvage.add(recovery.getWreck());
            }
        }
        return salvage;
    }

    /**
     * @return the number of wrecks that will be recovered
     */
    public int getRecoveredCount() {
        return countWhere(WreckRecovery::isRecovered);
    }

    /**
     * @return the number of wrecks with no recovery units assigned
     */
    public int getUnassignedCount() {
        return countWhere(recovery -> recovery.getStatus() == RecoveryStatus.UNASSIGNED);
    }

    /**
     * @return the number of wrecks whose assigned units can't recover them
     */
    public int getProblemCount() {
        return countWhere(recovery -> recovery.getStatus().isProblem());
    }

    private int countWhere(Predicate<WreckRecovery> condition) {
        int count = 0;
        for (WreckRecovery recovery : getRecoveries()) {
            if (condition.test(recovery)) {
                count++;
            }
        }
        return count;
    }

    public Money getKeptValue() {
        return sumValue(getKeptSalvage());
    }

    public Money getSoldValue() {
        return sumValue(getSoldSalvage());
    }

    public Money getEmployerValue() {
        return sumValue(getEmployerSalvage());
    }

    private static Money sumValue(List<TestUnit> salvage) {
        Money value = Money.zero();
        for (TestUnit unit : salvage) {
            value = value.plus(unit.getSellValue());
        }
        return value;
    }

    /**
     * @return the salvage value the player has claimed this contract, this scenario's kept and sold salvage included
     */
    public Money getUnitSalvageTotal() {
        return unitSalvageInitial.plus(getKeptValue()).plus(getSoldValue());
    }

    /**
     * @return the salvage value the employer has received this contract, this scenario's salvage included
     */
    public Money getEmployerSalvageTotal() {
        return employerSalvageInitial.plus(getEmployerValue());
    }

    /**
     * @return the salvage value the employer had received this contract before this scenario
     */
    public Money getEmployerSalvageInitial() {
        return employerSalvageInitial;
    }

    /**
     * @return the player's share of this contract's salvage so far, as a percentage with four decimal places
     */
    public BigDecimal getSalvagePercent() {
        Money unitTotal = getUnitSalvageTotal();
        Money total = getEmployerSalvageTotal().plus(unitTotal);
        if (!total.isPositive()) {
            return BigDecimal.ZERO;
        }
        return unitTotal.getAmount().multiply(BigDecimal.valueOf(100)).divide(total.getAmount(), 4,
              RoundingMode.HALF_UP);
    }

    /**
     * @return {@code true} if salvage is capped and the player's share of this contract's salvage is over the cap
     */
    public boolean isOverSalvageCap() {
        return settlement.isSalvageCapped() &&
                     (getSalvagePercent().compareTo(BigDecimal.valueOf(settlement.getPlayerSharePercent())) > 0);
    }

    /**
     * @return what the player pays their employer for the salvage they buy
     */
    public Money getPurchaseCost() {
        return settlement.getPurchaseCost(getKeptValue());
    }

    /**
     * @return what the player is paid as their share of the salvage going to their employer
     */
    public Money getCashShare() {
        return settlement.getCashShare(getEmployerValue());
    }

    /**
     * @return under salvage exchange rights, the salvage value the player has earned this contract, this scenario's
     *       cash share included
     */
    public Money getExchangeUnitTotal() {
        return unitSalvageInitial.plus(getCashShare());
    }

    // endregion Tallies

    /**
     * Lists what stops the player confirming their salvage.
     *
     * <p>Being over the salvage cap only blocks the player while they claim salvage in this scenario; if they were
     * already over the cap, they can still confirm a scenario in which they claim nothing.</p>
     *
     * @return the blockers, in the order they should be shown; empty if the player can confirm
     *
     * @author Illiani
     * @since 0.51.01
     */
    public List<ConfirmBlocker> getConfirmBlockers() {
        List<ConfirmBlocker> blockers = new ArrayList<>();
        if (getProblemCount() > 0) {
            blockers.add(ConfirmBlocker.UNRECOVERABLE_ASSIGNMENTS);
        }
        if (isOverSalvageCap() && (getUnitSalvageTotal().compareTo(unitSalvageInitial) > 0)) {
            blockers.add(ConfirmBlocker.OVER_SALVAGE_CAP);
        }
        if (!settlement.canAfford(getKeptValue(), availableFunds)) {
            blockers.add(ConfirmBlocker.UNAFFORDABLE_PURCHASES);
        }
        if (getUsedMinutes() > availableMinutes) {
            blockers.add(ConfirmBlocker.NOT_ENOUGH_TECH_TIME);
        }
        return blockers;
    }

    /**
     * @return {@code true} if nothing stops the player confirming their salvage
     */
    public boolean canConfirm() {
        return getConfirmBlockers().isEmpty();
    }
}
