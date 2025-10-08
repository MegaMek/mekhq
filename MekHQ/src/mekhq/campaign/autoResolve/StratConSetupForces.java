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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.autoResolve;

import java.util.List;
import java.util.Objects;

import megamek.common.Player;
import megamek.common.autoResolve.converter.ForceConsolidation;
import megamek.common.enums.SkillLevel;
import megamek.common.equipment.Minefield;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.unit.Unit;

/**
 * This class is responsible for setting up the forces for a StratCon scenario
 *
 * @author Luana Coppio
 */
public class StratConSetupForces extends ScenarioSetupForces<AtBScenario> {
    private static final MMLogger LOGGER = MMLogger.create(StratConSetupForces.class);

    public StratConSetupForces(Campaign campaign, List<Unit> units, AtBScenario scenario,
          ForceConsolidation forceConsolidationMethod) {
        this(campaign, units, scenario, forceConsolidationMethod, new OrderFactory(campaign, scenario));
    }

    public StratConSetupForces(Campaign campaign, List<Unit> units, AtBScenario scenario,
          ForceConsolidation forceConsolidationMethod, OrderFactory orderFactory) {
        super(campaign, units, scenario, forceConsolidationMethod, orderFactory);
    }

    @Override
    protected SkillLevel getEnemySkillLevel() {
        return getScenario().getContract(campaign).getEnemySkill();
    }

    @Override
    protected SkillLevel getAlliedSkillLevel() {
        return getScenario().getContract(campaign).getAllySkill();
    }

    /**
     * Create a player object from the campaign and scenario which doesn't have a reference to the original player
     *
     * @return The clean player object
     */
    @Override
    protected Player getCleanPlayer() {
        var player = super.getCleanPlayer();
        player.setNbrMFActive(getScenario().getNumPlayerMinefields(Minefield.TYPE_ACTIVE));
        player.setNbrMFConventional(getScenario().getNumPlayerMinefields(Minefield.TYPE_CONVENTIONAL));
        player.setNbrMFInferno(getScenario().getNumPlayerMinefields(Minefield.TYPE_INFERNO));
        player.setNbrMFVibra(getScenario().getNumPlayerMinefields(Minefield.TYPE_VIBRABOMB));
        return player;
    }

    @Override
    protected EntitySource getUnitEntitySource() {
        return new UnitEntitySource();
    }

    @Override
    protected EntitySource getAllyEntitySource() {
        return new AllyEntitySource();
    }

    private class UnitEntitySource implements EntitySource {
        @Override
        public Iterable<?> getSources() {
            return units;
        }

        @Override
        public Entity setupEntity(Player player, Object source, boolean useDropship) {
            return setupPlayerEntityFromUnit(player, (Unit) source, useDropship);
        }
    }

    private class AllyEntitySource implements EntitySource {
        @Override
        public Iterable<?> getSources() {
            return getScenario().getAlliesPlayer();
        }

        @Override
        public Entity setupEntity(Player player, Object source, boolean useDropship) {
            return setupPlayerAllyEntity(player, (Entity) source, useDropship);
        }
    }

    private Entity setupPlayerAllyEntity(Player player, Entity originalAllyEntity, boolean useDropship) {
        var entity = moveByCopy(originalAllyEntity);
        if (Objects.isNull(entity)) {
            LOGGER.error("Could not setup ally entity {}", originalAllyEntity);
            return null;
        }

        entity.setOwner(player);
        AtBScenario scenario = getScenario();
        int deploymentRound = entity.getDeployRound();
        if (!(getScenario() instanceof AtBDynamicScenario)) {
            int speed = entity.getWalkMP();
            if (entity.getAnyTypeMaxJumpMP() > 0) {
                if (entity instanceof Infantry) {
                    speed = entity.getJumpMP();
                } else {
                    speed++;
                }
            }
            deploymentRound = Math.max(entity.getDeployRound(), scenario.getDeploymentDelay() - speed);
            if (!useDropship
                      && scenario.getCombatRole().isPatrol()
                      && (scenario.getCombatTeamById(campaign) != null)
                      && (scenario.getCombatTeamById(campaign).getForceId() == scenario.getCombatTeamId())) {
                deploymentRound = Math.max(deploymentRound, 6 - speed);
            }
        }

        entity.setDeployRound(deploymentRound);
        return entity;
    }

    private Entity setupPlayerEntityFromUnit(Player player, Unit unit, boolean useDropship) {
        var entity = moveByCopy(unit.getEntity());
        AtBScenario scenario = getScenario();
        if (Objects.isNull(entity)) {
            LOGGER.error("Could not setup unit {} for player {}", unit, player);
            return null;
        }
        entity.setOwner(player);

        // Set the TempID for auto reporting
        entity.setExternalIdAsString(unit.getId().toString());

        // If this unit is a spacecraft, set the crew size and marine size values
        if (entity.isLargeCraft() || (entity.getUnitType() == UnitType.SMALL_CRAFT)) {
            entity.setNCrew(unit.getActiveCrew().size());
            entity.setNMarines(unit.getMarineCount());
        }
        // Calculate deployment round
        int deploymentRound = entity.getDeployRound();
        if (!(scenario instanceof AtBDynamicScenario)) {
            int speed = entity.getWalkMP();
            if (entity.getAnyTypeMaxJumpMP() > 0) {
                if (entity instanceof Infantry) {
                    speed = entity.getJumpMP();
                } else {
                    speed++;
                }
            }
            // Set scenario type-specific delay
            deploymentRound = Math.max(entity.getDeployRound(), scenario.getDeploymentDelay() - speed);
            // Lances deployed in scout roles always deploy units in 6-walking speed turns
            if (scenario.getCombatRole().isPatrol()
                      && (scenario.getCombatTeamById(campaign) != null)
                      && (scenario.getCombatTeamById(campaign).getForceId() == scenario.getCombatTeamId())
                      && !useDropship) {
                deploymentRound = Math.max(deploymentRound, 6 - speed);
            }
        }
        entity.setDeployRound(deploymentRound);
        var force = campaign.getForceFor(unit);
        if (force != null) {
            entity.setForceString(force.getFullMMName());
        } else if (!unit.getEntity().getForceString().isBlank()) {
            // this was added mostly to make it easier to run tests
            entity.setForceString(unit.getEntity().getForceString());
        }
        return entity;
    }

    /**
     * Check if using dropships for patrol scenario
     *
     * @return True if using dropships under specific conditions, false otherwise
     */
    private boolean isUsingDropship() {
        if (getScenario().getCombatRole().isPatrol()) {
            for (Entity en : getScenario().getAlliesPlayer()) {
                if (en.getUnitType() == UnitType.DROPSHIP) {
                    return true;
                }
            }
            for (Unit unit : units) {
                if (unit.getEntity().getUnitType() == UnitType.DROPSHIP) {
                    return true;
                }
            }
        }
        return false;
    }
}
