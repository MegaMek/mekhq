/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.phase;

import megamek.common.Entity;
import megamek.common.alphaStrike.ASRange;
import megamek.common.enums.GamePhase;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.util.weightedMaps.WeightedDoubleMap;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.AcsEngagementControlAction;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormation;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcFormationTurn;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcGameManager;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.EngagementControl;

import java.util.Map;
import java.util.Optional;

import static mekhq.campaign.autoResolve.helper.RandomUtils.toShuffledList;

public class MovementPhase extends PhaseHandler {

    private static final WeightedDoubleMap<EngagementControl> normal = WeightedDoubleMap.of(
        EngagementControl.FORCED_ENGAGEMENT, 1.0,
        EngagementControl.EVADE, 0.0,
        EngagementControl.STANDARD, 1.0,
        EngagementControl.OVERRUN, 0.5,
        EngagementControl.NONE, 0.0
    );

    private static final WeightedDoubleMap<EngagementControl> unsteady =  WeightedDoubleMap.of(
        EngagementControl.FORCED_ENGAGEMENT, 0.5,
        EngagementControl.EVADE, 0.02,
        EngagementControl.STANDARD, 1.0,
        EngagementControl.OVERRUN, 0.1,
        EngagementControl.NONE, 0.01
    );

    private static final WeightedDoubleMap<EngagementControl> shaken =  WeightedDoubleMap.of(
        EngagementControl.FORCED_ENGAGEMENT, 0.2,
        EngagementControl.EVADE, 0.1,
        EngagementControl.STANDARD, 0.8,
        EngagementControl.OVERRUN, 0.05,
        EngagementControl.NONE, 0.01
    );

    private static final WeightedDoubleMap<EngagementControl> broken = WeightedDoubleMap.of(
        EngagementControl.FORCED_ENGAGEMENT, 0.05,
        EngagementControl.EVADE, 1.0,
        EngagementControl.STANDARD, 0.5,
        EngagementControl.OVERRUN, 0.05,
        EngagementControl.NONE, 0.3
    );

    private static final WeightedDoubleMap<EngagementControl> routed = WeightedDoubleMap.of(
        EngagementControl.NONE, 1.0
    );

    private static final Map<AcFormation.MoraleStatus, WeightedDoubleMap<EngagementControl>> engagementControlOptions = Map.of(
        AcFormation.MoraleStatus.NORMAL, normal,
        AcFormation.MoraleStatus.UNSTEADY, unsteady,
        AcFormation.MoraleStatus.SHAKEN, shaken,
        AcFormation.MoraleStatus.BROKEN, broken,
        AcFormation.MoraleStatus.ROUTED, routed
    );

    public MovementPhase(AcGameManager gameManager) {
        super(gameManager, GamePhase.MOVEMENT);
    }

    @Override
    protected void executePhase() {

        while (getGameManager().getGame().hasMoreTurns()) {

            var optTurn = getGameManager().getGame().changeToNextTurn();
            if (optTurn.isEmpty()) {
                break;
            }
            var turn = optTurn.get();

            if (turn instanceof AcFormationTurn formationTurn) {
                var player = getGameManager().getGame().getPlayer(formationTurn.playerId());
                getGameManager().getGame().getActiveFormations(player)
                    .stream()
                    .filter(f -> f.isEligibleForPhase(getGameManager().getGame().getPhase())) // only eligible formations
                    .findAny()
                    .map(this::engage)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .ifPresent(this::engagementAndControl);
            }
        }
    }

    private Optional<EngagementControlRecord> engage(AcFormation activeFormation) {
        var target = this.selectTarget(activeFormation);
        return target.map(sbfFormation -> new EngagementControlRecord(activeFormation, sbfFormation));
    }

    private record EngagementControlRecord(AcFormation actingFormation, AcFormation target) { }

    private static final Map<Integer, EngagementControl[]> engagementAndControlExclusions = Map.of(
        0, new EngagementControl[] { EngagementControl.NONE },
        1, new EngagementControl[] { EngagementControl.FORCED_ENGAGEMENT },
        2, new EngagementControl[] { EngagementControl.OVERRUN },
        3, new EngagementControl[] { EngagementControl.OVERRUN, EngagementControl.FORCED_ENGAGEMENT },
        4, new EngagementControl[] { EngagementControl.STANDARD, EngagementControl.OVERRUN, EngagementControl.FORCED_ENGAGEMENT },
        5, new EngagementControl[] { EngagementControl.STANDARD, EngagementControl.OVERRUN, EngagementControl.FORCED_ENGAGEMENT }
    );

    private static final EngagementControl[] EMPTY_EAC = new EngagementControl[0];

    private void engagementAndControl(EngagementControlRecord engagement) {
        var eco = engagementControlOptions.get(engagement.target.moraleStatus());
        var engagementControlExclusion = 0;

        if (engagement.actingFormation.getStdDamage().usesDamage(ASRange.LONG)) {
            engagementControlExclusion += 1;
        }
        if (engagement.actingFormation.getStdDamage().getDamage(ASRange.SHORT).damage < 2) {
            engagementControlExclusion += 2;
        }
        if (engagement.actingFormation.isCrippled()) {
            engagementControlExclusion += 2;
        }

        var engagementControl = eco.randomOptionalItem(engagementAndControlExclusions.getOrDefault(engagementControlExclusion, EMPTY_EAC));

        getGameManager().addEngagementControl(
            new AcsEngagementControlAction(
                engagement.actingFormation.getId(),
                engagement.target.getId(),
                engagementControl.orElse(EngagementControl.NONE)),
            engagement.actingFormation);
    }

    private Optional<AcFormation> selectTarget(AcFormation actingFormation) {
        var game = getGameManager().getGame();
        var player = game.getPlayer(actingFormation.getOwnerId());
        var canBeTargets = getGameManager().getGame().getActiveFormations().stream()
            .filter(f -> actingFormation.getTargetFormationId() == Entity.NONE || f.getId() == actingFormation.getTargetFormationId())
            .filter(SBFFormation::isDeployed)
            .filter(f -> f.isGround() == actingFormation.isGround())
            .filter(f -> game.getPlayer(f.getOwnerId()).isEnemyOf(player))
            .collect(toShuffledList());

        if (canBeTargets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(canBeTargets.get(0));
    }
}
