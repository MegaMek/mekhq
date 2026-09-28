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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBScenario;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

/**
 * Builds the {@link SalvageRecoverySession} for a scenario's salvage: the wrecks left on the field, the units that can
 * recover them, how long each wreck takes, and how much tech time the salvage techs have.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class SalvageRecoverySessionFactory {
    private static final MMLogger LOGGER = MMLogger.create(SalvageRecoverySessionFactory.class);

    private SalvageRecoverySessionFactory() {}

    /**
     * Builds the recovery session for a scenario's salvage.
     *
     * <p>This also removes the scenario's salvage formations and techs from every other active scenario, as a
     * formation or tech can only salvage one scenario.</p>
     *
     * @param campaign      the current campaign
     * @param salvageRules  the rules of the campaign's salvage system
     * @param contract      the contract the scenario belongs to
     * @param scenario      the scenario whose salvage is being recovered
     * @param actualSalvage the wrecks left on the battlefield
     * @param soldSalvage   wrecks the resolve wizard marked for sale
     *
     * @return the session, or {@code null} if there is nothing to recover: the contract grants no salvage rights, or
     *       no wrecks were left on the field
     */
    public static @Nullable SalvageRecoverySession createSession(Campaign campaign, AbstractSalvage salvageRules,
          AbstractContract contract, Scenario scenario, List<TestUnit> actualSalvage, List<TestUnit> soldSalvage) {
        boolean isInSpace = scenario.getBoardType() == AtBScenario.T_SPACE;
        int availableMinutes = getAvailableTechMinutes(campaign, scenario);
        List<Integer> salvageFormations = new ArrayList<>(scenario.getSalvageFormations());
        List<Unit> recoveryUnits = getRecoveryUnits(campaign, scenario, salvageRules, isInSpace);
        sanitizeOtherScenarioAssignments(campaign.getActiveScenarios(), scenario, scenario.getSalvageTechs(),
              salvageFormations);

        List<TestUnit> wrecks = new ArrayList<>(actualSalvage);
        wrecks.addAll(soldSalvage);

        if (!contract.canSalvage()) {
            LOGGER.debug("[Salvage] {}: the contract grants no salvage rights, so there is nothing to recover",
                  scenario.getName());
            return null;
        }

        if (wrecks.isEmpty()) {
            LOGGER.debug("[Salvage] {}: no wrecks were left on the field, so there is nothing to recover",
                  scenario.getName());
            return null;
        }

        SalvageSettlement settlement = salvageRules.createSettlement(contract);
        return new SalvageRecoverySession(salvageRules, settlement, isInSpace, wrecks, recoveryUnits,
              getRecoveryTimes(campaign, scenario, wrecks), availableMinutes, contract.getSalvagedByUnitValue(),
              contract.getSalvagedByEmployerValue(), campaign.getPlayerForce().getFinances().getBalance());
    }

    /**
     * Sums the remaining work time of the techs assigned to the scenario's salvage.
     */
    private static int getAvailableTechMinutes(Campaign campaign, Scenario scenario) {
        int minutes = 0;
        for (UUID techId : scenario.getSalvageTechs()) {
            Person tech = campaign.getPlayerForce().getHumanResources().getPerson(techId);
            if (tech == null) {
                LOGGER.error("Salvage tech {} not found in campaign", techId);
                continue;
            }
            // I don't expect we'll have negative tech minutes, but you never know
            minutes += Math.max(0, tech.getMinutesLeft());
        }
        return minutes;
    }

    /**
     * Collects the units of the scenario's salvage formations that can recover wrecks. Units that fought in the
     * scenario are left out, unless the salvage system lets their formation fight and then salvage.
     */
    private static List<Unit> getRecoveryUnits(Campaign campaign, Scenario scenario, AbstractSalvage salvageRules,
          boolean isInSpace) {
        // A set, as a formation and one nested inside it can both be assigned, and their units must only be listed once
        Set<Unit> recoveryUnits = new LinkedHashSet<>();
        LocalHangar hangar = campaign.getPlayerForce().getHangar();
        for (Integer formationId : scenario.getSalvageFormations()) {
            Formation formation = campaign.getPlayerForce().getFormation(formationId);
            if (formation == null) {
                LOGGER.error("Force {} not found in campaign", formationId);
                continue;
            }

            boolean canCombatUnitsSalvage = salvageRules.canSalvageAfterFighting(formation);
            for (Unit unit : formation.getAllUnitsAsUnits(hangar, false)) {
                boolean didFightInScenario = unit.getScenarioId() == scenario.getId();
                if ((didFightInScenario && !canCombatUnitsSalvage) ||
                          !salvageRules.isAvailableForSalvage(unit, isInSpace)) {
                    continue;
                }
                recoveryUnits.add(unit);
            }
        }
        return new ArrayList<>(recoveryUnits);
    }

    /**
     * A convoluted series of steps can leave the same formation or tech assigned to several salvage operations on the
     * same day. This removes this operation's formations and techs from every other active scenario.
     */
    private static void sanitizeOtherScenarioAssignments(List<Scenario> activeScenarios, Scenario currentScenario,
          List<UUID> salvageTechs, List<Integer> salvageFormations) {
        for (Scenario activeScenario : activeScenarios) {
            if (activeScenario == currentScenario) {
                continue;
            }
            activeScenario.removeSalvageFormation(salvageFormations);
            activeScenario.removeSalvageTechs(salvageTechs);
        }
    }

    private static Map<UUID, RecoveryTimeData> getRecoveryTimes(Campaign campaign, Scenario scenario,
          List<TestUnit> wrecks) {
        Map<UUID, RecoveryTimeData> recoveryTimes = new HashMap<>();
        for (TestUnit wreck : wrecks) {
            Entity entity = wreck.getEntity();
            if (entity == null) {
                LOGGER.error("Entity for unit {} not found in campaign", wreck.getId());
                continue;
            }
            recoveryTimes.put(wreck.getId(), RecoveryTimeCalculations.calculateRecoveryTimeForEntity(
                  entity.getDisplayName(), entity.getRecoveryTime(), entity.isAero(), scenario,
                  campaign.getPlayerForce().getForceDetachment().getCurrentLocation().getPlanet()));
        }
        return recoveryTimes;
    }
}
