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
package mekhq.campaign.digitalGM.stratCon.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import mekhq.campaign.unit.Unit;

/**
 * Mutable working state for one visit to the StratCon deployment wizard: which page (mode) the player is on, what they
 * have staged for deployment, and the budget inputs the current page spends against. The live budget summaries all
 * defer to {@link DeploymentEvaluator}, so this class holds selections and totals rather than re-deriving any of the
 * deployment arithmetic.
 *
 * <p>The wizard populates the budget inputs from the campaign and scenario when a page is opened; the panels read the
 * summaries below to drive their meters and to decide whether a commit is affordable.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class DeploymentContext {
    private DeploymentMode mode;

    private final Set<Integer> stagedFormationIds = new LinkedHashSet<>();
    private final List<Unit> stagedUnits = new ArrayList<>();

    private int chosenSupportPoints;
    private boolean instantArrival;

    private int availableSupportPoints;
    private int leadershipSkill;
    private int leadershipPointsUsed;
    private int defensivePoints;

    public DeploymentContext(DeploymentMode mode) {
        this.mode = mode;
    }

    public DeploymentMode getMode() {
        return mode;
    }

    /**
     * Switches the active page. The staged selections are intentionally preserved across a switch, so a player can move
     * between pages of the same deployment without losing what they have already picked.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setMode(DeploymentMode mode) {
        this.mode = mode;
    }

    // region formation staging (primary / reinforce pages)

    public void stageFormation(int formationId) {
        stagedFormationIds.add(formationId);
    }

    public void unstageFormation(int formationId) {
        stagedFormationIds.remove(formationId);
    }

    public boolean isFormationStaged(int formationId) {
        return stagedFormationIds.contains(formationId);
    }

    public Set<Integer> getStagedFormationIds() {
        return Collections.unmodifiableSet(stagedFormationIds);
    }

    public int getStagedFormationCount() {
        return stagedFormationIds.size();
    }

    // endregion

    // region unit staging (auxiliaries / utility pages)

    public void stageUnit(Unit unit) {
        if (!stagedUnits.contains(unit)) {
            stagedUnits.add(unit);
        }
    }

    public void unstageUnit(Unit unit) {
        stagedUnits.remove(unit);
    }

    public List<Unit> getStagedUnits() {
        return Collections.unmodifiableList(stagedUnits);
    }

    // endregion

    // region reinforcement options

    public int getChosenSupportPoints() {
        return chosenSupportPoints;
    }

    public void setChosenSupportPoints(int chosenSupportPoints) {
        this.chosenSupportPoints = chosenSupportPoints;
    }

    public boolean isInstantArrival() {
        return instantArrival;
    }

    public void setInstantArrival(boolean instantArrival) {
        this.instantArrival = instantArrival;
    }

    // endregion

    // region budget inputs (supplied by the wizard from campaign/scenario state)

    public void setAvailableSupportPoints(int availableSupportPoints) {
        this.availableSupportPoints = availableSupportPoints;
    }

    public void setLeadershipSkill(int leadershipSkill) {
        this.leadershipSkill = leadershipSkill;
    }

    public void setLeadershipPointsUsed(int leadershipPointsUsed) {
        this.leadershipPointsUsed = leadershipPointsUsed;
    }

    public void setDefensivePoints(int defensivePoints) {
        this.defensivePoints = defensivePoints;
    }

    public int getDefensivePoints() {
        return defensivePoints;
    }

    // endregion

    // region live budget summaries

    /**
     * @return the support-point cost of committing the currently staged reinforcement forces at the chosen spend and
     *       arrival speed
     *
     * @author Illiani
     * @since 0.51.01
     */
    public ReinforcementCost getReinforcementCost() {
        return DeploymentEvaluator.reinforcementCost(chosenSupportPoints, instantArrival, getStagedFormationCount());
    }

    /**
     * @return the support points that would remain after committing the staged reinforcements; may be negative, which
     *       the wizard surfaces as an over-budget warning
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int getSupportPointsRemainingAfterCommit() {
        return availableSupportPoints - getReinforcementCost().totalSupportPoints();
    }

    /**
     * @return the combined battle value of the units staged on the auxiliaries page
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int getStagedLeadershipBattleValue() {
        int total = 0;
        for (Unit unit : stagedUnits) {
            total += unit.getEntity().calculateBattleValue(true, true);
        }
        return total;
    }

    /**
     * @return the leadership battle value still available after the staged auxiliary units; may be negative, which the
     *       wizard surfaces as an over-budget warning
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int getLeadershipBattleValueRemaining() {
        int remainingBudget = DeploymentEvaluator.leadershipPointsRemaining(leadershipSkill, leadershipPointsUsed);
        return remainingBudget - getStagedLeadershipBattleValue();
    }

    public boolean isOverLeadershipBudget() {
        return getLeadershipBattleValueRemaining() < 0;
    }

    /**
     * @return the minefields the scenario will field once the staged utility units take their share of its defensive
     *       points
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int getMinefieldsRemaining() {
        return DeploymentEvaluator.minefieldsRemaining(defensivePoints, stagedUnits.size());
    }

    // endregion
}
