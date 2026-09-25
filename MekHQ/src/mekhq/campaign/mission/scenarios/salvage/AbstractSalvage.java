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

import java.util.List;

import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.unit.ITransportAssignment;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

/**
 * Defines the rules of a salvage system (see {@link SalvageSystem}).
 *
 * <p>Each salvage system decides three things:</p>
 * <ul>
 *   <li><b>How wrecks are recovered</b>: whether salvage teams must be assigned before the scenario, and how
 *   post-scenario salvage is resolved (see {@link #resolveScenarioSalvage}).</li>
 *   <li><b>Which units can recover them</b>: what a unit needs to take part in salvage operations (see
 *   {@link #canSalvage(Unit, boolean)} and {@link #isAvailableForSalvage(Unit, boolean)}).</li>
 *   <li><b>How salvage is paid for</b>: how the contract's salvage rights divide the salvage between the player and
 *   their employer (see {@link #createSettlement(AbstractContract)}).</li>
 * </ul>
 *
 * <p>Where a rule has a sensible default, it is that of {@link CamOpsStrictSalvage CamOps (Strict)}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public abstract class AbstractSalvage {
    /**
     * Checks whether salvage is recovered through salvage operations.
     *
     * <p>When salvage operations are used, the player assigns salvage formations and techs before a scenario starts,
     * and those teams recover the wrecks afterward.</p>
     *
     * @return {@code true} if salvage operations are used
     *
     * @author Illiani
     * @since 0.51.01
     */
    public abstract boolean isUseSalvageOperations();

    /**
     * Checks whether salvage is claimed in the resolve scenario wizard, rather than afterward in the post-scenario
     * salvage picker.
     *
     * @return {@code true} if salvage is claimed in the resolve scenario wizard
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isSalvageClaimedInResolveWizard() {
        return false;
    }

    /**
     * Checks whether a formation set to 'Salvage' may fight in a scenario and still recover that scenario's salvage.
     *
     * <p>Under CamOps (Strict) rules, a salvage team cannot take part in the fighting; it sits on the sidelines until
     * the OpFor has been routed. Other systems may allow Salvage formations to fight and then salvage. Units in any
     * other kind of formation can never salvage a scenario they fought in.</p>
     *
     * @return {@code true} if Salvage formations may fight and then salvage the same scenario
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isSalvageFormationCombatAllowed() {
        return false;
    }

    /**
     * Checks whether units in a formation may salvage a scenario they fought in.
     *
     * @param formation the formation the units belong to
     *
     * @return {@code true} if the formation's units may fight and then salvage the same scenario
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean canSalvageAfterFighting(Formation formation) {
        return isSalvageFormationCombatAllowed() && formation.getFormationType().isSalvage();
    }

    /**
     * Checks whether a unit carrying salvage may recover more than one wreck.
     *
     * <p>Under CamOps (Strict), each recovery unit recovers a single wreck. Other systems let a unit carry as many
     * wrecks as fit in its cargo space. In space, fighters and small craft are instead limited by the number of
     * suitable bays with working doors: fighters fit in fighter or small craft bays, small craft only in small craft
     * bays.</p>
     *
     * <p>Either way, a unit either drags (or tugs) salvage or carries it; never both. A unit that drags salvage, or
     * that teams up with another unit to recover a wreck, is committed to that one wreck.</p>
     *
     * @return {@code true} if a carrying unit may recover several wrecks
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isMultipleSalvagePerUnitAllowed() {
        return false;
    }

    /**
     * Checks whether a unit is capable of salvaging in the current environment.
     *
     * <p>By default, this is {@link Unit#canSalvage(boolean)}.</p>
     *
     * @param unit      the unit to check
     * @param isInSpace {@code true} if the salvage operation takes place in space
     *
     * @return {@code true} if the unit is capable of salvaging
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean canSalvage(Unit unit, boolean isInSpace) {
        return unit.canSalvage(isInSpace);
    }

    /**
     * Checks whether a unit is able to take part in a salvage operation.
     *
     * <p>The unit must:</p>
     * <ul>
     *   <li>Be capable of salvaging in the current environment (see {@link #canSalvage(Unit, boolean)})</li>
     *   <li>Be repairable, and not being stripped for parts</li>
     *   <li>Be able to move (see {@link #isImmobilized(Entity)})</li>
     * </ul>
     *
     * <p>Trailers can't move on their own, so instead they must be hitched to something.</p>
     *
     * @param unit      the unit to check
     * @param isInSpace {@code true} if the salvage operation takes place in space
     *
     * @return {@code true} if the unit can take part in the salvage operation
     *
     * @author Illiani
     * @since 0.51.01
     */
    public final boolean isAvailableForSalvage(Unit unit, boolean isInSpace) {
        if (!canSalvage(unit, isInSpace)) {
            return false;
        }

        // A unit that can't be repaired, or is being stripped for parts, is in no state to recover anything
        if (unit.isSalvage() || !unit.isRepairable()) {
            return false;
        }

        Entity entity = unit.getEntity();
        if (entity instanceof Tank tank && tank.isTrailer()) {
            ITransportAssignment transportAssignment = unit.getTransportAssignment(CampaignTransportType.TOW_TRANSPORT);
            return transportAssignment != null && transportAssignment.hasTransport();
        }

        return !isImmobilized(entity);
    }

    /**
     * Checks whether damage has left an entity unable to move, and therefore unable to reach or haul salvage.
     *
     * <p>Only lasting damage counts, such as a destroyed motive system, gyro, or engine. Campaign Operations only
     * requires a salvaging 'Mek to have two working hands, so by default a 'Mek's mobility isn't checked.</p>
     *
     * @param entity the entity to check
     *
     * @return {@code true} if the entity is immobilized
     *
     * @author Illiani
     * @since 0.51.01
     */
    protected boolean isImmobilized(Entity entity) {
        if (entity instanceof Mek) {
            return false;
        }

        return entity.isPermanentlyImmobilized(false);
    }

    /**
     * Creates the settlement that decides how a contract's salvage is divided between the player and their employer.
     *
     * <p>By default, the contract's salvage rights cap how much of the salvage the player may claim, or, under
     * salvage exchange rights, set the player's cash share of the employer's salvage.</p>
     *
     * @param contract the contract the salvage was recovered under
     *
     * @return the salvage settlement for the contract
     *
     * @author Illiani
     * @since 0.51.01
     */
    public SalvageSettlement createSettlement(AbstractContract contract) {
        return SalvageSettlement.forCappedSalvageRights(contract);
    }

    /**
     * Resolves a scenario's salvage once the scenario itself has been resolved.
     *
     * @param campaign              the current campaign
     * @param contract              the contract the scenario belongs to
     * @param scenario              the scenario being resolved
     * @param hasBattlefieldControl {@code true} if the player controls the battlefield
     * @param claimedSalvage        the wrecks the player claimed in the resolve scenario wizard or, where salvage
     *                              is decided afterward, every wreck
     * @param soldSalvage           the wrecks the player chose to sell in the resolve scenario wizard
     * @param unclaimedSalvage      the wrecks the player left for their employer in the resolve scenario wizard
     *
     * @author Illiani
     * @since 0.51.01
     */
    public abstract void resolveScenarioSalvage(Campaign campaign, AbstractContract contract, Scenario scenario,
          boolean hasBattlefieldControl, List<TestUnit> claimedSalvage, List<TestUnit> soldSalvage,
          List<TestUnit> unclaimedSalvage);
}
