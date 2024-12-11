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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component;

import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.planetaryconditions.PlanetaryConditions;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.reporter.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Luana Coppio
 */
public record AcsInitiativeHelper(AcsGameManager gameManager) implements AcsGameManagerHelper {

    /**
     * Determines the turn order for a given phase, setting the game's turn list and sending it to the
     * Clients. Also resets the turn index.
     *
     * @param phase The phase to find the turns for
     * @see AbstractGame#resetTurnIndex()
     */
    void determineTurnOrder(GamePhase phase) {
        final List<AcsTurn> turns;
        if (phase.isFiring() || phase.isMovement()) {
            turns = game().getInGameObjects().stream()
                .filter(AcsFormation.class::isInstance)
                .filter(unit -> ((AcsFormation) unit).isDeployed())
                .filter(unit -> ((AcsFormation) unit).isEligibleForPhase(phase))
                .map(InGameObject::getOwnerId)
                .map(AcsFormationTurn::new)
                .sorted(Comparator.comparing(t -> game().getPlayer(t.playerId()).getInitiative()))
                .collect(Collectors.toList());
        } else if (phase.isDeployment()) {
            // Deployment phase: sort by initiative
            turns = game().getInGameObjects().stream()
                .filter(AcsFormation.class::isInstance)
                .filter(unit -> !((AcsFormation) unit).isDeployed())
                .map(InGameObject::getOwnerId)
                .map(AcsFormationTurn::new)
                .sorted(Comparator.comparing(t -> game().getPlayer(t.playerId()).getInitiative()))
                .collect(Collectors.toList());

        } else {
            // As a fallback, provide unsorted turns
            turns = game().getInGameObjects().stream()
                .filter(AcsFormation.class::isInstance)
                .filter(unit -> ((AcsFormation) unit).isDeployed())
                .filter(unit -> ((AcsFormation) unit).isEligibleForPhase(phase))
                .map(InGameObject::getOwnerId)
                .map(AcsFormationTurn::new)
                .collect(Collectors.toList());

            // Now, assemble formations and sort by initiative and relative formation count
            Map<Integer, Long> unitCountsByPlayer = game().getInGameObjects().stream()
                .filter(AcsFormation.class::isInstance)
                .filter(unit -> ((AcsFormation) unit).isDeployed())
                .filter(unit -> ((AcsFormation) unit).isEligibleForPhase(phase))
                .collect(Collectors.groupingBy(InGameObject::getOwnerId, Collectors.counting()));

            if (!unitCountsByPlayer.isEmpty()) {
                final long lowestUnitCount = Collections.min(unitCountsByPlayer.values());

                int playerWithLowestUnitCount = unitCountsByPlayer.entrySet().stream()
                    .filter(e -> e.getValue() == lowestUnitCount)
                    .map(Map.Entry::getKey)
                    .findAny().orElse(Player.PLAYER_NONE);

                List<Integer> playersByInitiative = new ArrayList<>(unitCountsByPlayer.keySet());
                playersByInitiative.sort(Comparator.comparing(id -> game().getPlayer(id).getInitiative()));

                if ((playerWithLowestUnitCount != Player.PLAYER_NONE) && (lowestUnitCount > 0)) {
                    List<AcsTurn> sortedTurns = new ArrayList<>();
                    for (int initCycle = 0; initCycle < lowestUnitCount; initCycle++) {
                        long currentLowestUnitCount = Collections.min(unitCountsByPlayer.values());
                        for (int playerId : playersByInitiative) {
                            long unitsToMove = unitCountsByPlayer.get(playerId) / currentLowestUnitCount;
                            long remainingUnits = unitCountsByPlayer.get(playerId);
                            unitsToMove = Math.min(unitsToMove, remainingUnits);
                            for (int i = 0; i < unitsToMove; i++) {
                                sortedTurns.add(new AcsFormationTurn(playerId));
                            }
                            unitCountsByPlayer.put(playerId, remainingUnits - unitsToMove);
                        }
                    }
                    // When here, sorting has been successful; replace the unsorted turns
                    turns.clear();
                    turns.addAll(sortedTurns);
                }
            }
        }

        game().setTurns(turns);
        game().resetTurnIndex();
    }

    public void writeInitiativeReport() {
        writeHeader();
        writeInitiativeRolls();
        writeTurnOrder();
        writeFutureDeployment();
//        writeWeatherReport();
    }

    private void writeTurnOrder() {
        addReport(new AcsReportEntry(1020));

        for (AcsTurn turn : game().getTurnsList()) {
            Player player = game().getPlayer(turn.playerId());
            addReport(new AcsPlayerNameReportEntry(player).indent().addNL());
        }

    }

    private void writeFutureDeployment() {
        // remaining deployments
        Comparator<Deployable> comp = Comparator.comparingInt(Deployable::getDeployRound);
        List<Deployable> futureDeployments = game().getInGameObjects().stream()
            .filter(AcsFormation.class::isInstance)
            .map(Deployable.class::cast)
            .filter(unit -> !unit.isDeployed())
            .sorted(comp)
            .toList();

        if (!futureDeployments.isEmpty()) {
            addReport(new AcsPublicReportEntry(1060));
            int round = -1;
            for (Deployable deployable : futureDeployments) {
                if (round != deployable.getDeployRound()) {
                    round = deployable.getDeployRound();
                    addReport(new AcsPublicReportEntry(1065).add(round));
                }

                var r = new AcsReportEntry(1066).subject(((InGameObject) deployable).getId());
                r.add(((InGameObject) deployable).generalName());
                r.add(((InGameObject) deployable).getId());
                r.add(deployable.getDeployRound());
                addReport(r.indent());
            }
        }
    }

    private void writeWeatherReport() {
        PlanetaryConditions conditions = game().getPlanetaryConditions();
        addReport(new AcsPublicReportEntry(1025).add(conditions.getWindDirection().toString()));
        addReport(new AcsPublicReportEntry(1030).add(conditions.getWind().toString()));
        addReport(new AcsPublicReportEntry(1031).add(conditions.getWeather().toString()));
        addReport(new AcsPublicReportEntry(1032).add(conditions.getLight().toString()));
        addReport(new AcsPublicReportEntry(1033).add(conditions.getFog().toString()));
    }

    private void writeInitiativeRolls() {
        for (Team team : game().getTeams()) {
            // Teams with no active players can be ignored
            if (team.isObserverTeam()) {
                continue;
            }
            addReport(new AcsPublicReportEntry(1015).add(Player.TEAM_NAMES[team.getId()])
                .add(team.getInitiative().toString()));

            // Multiple players. List the team, then break it down.
            for (Player player : team.nonObserverPlayers()) {
                addReport(new AcsPublicReportEntry(2020)
                    .indent()
                    .add(player.getName())
                    .add(player.getInitiative().toString())
                );
            }
        }
    }

    private void writeHeader() {
        if (game().getLastPhase().isDeployment() || game().isDeploymentComplete()
            || !game().shouldDeployThisRound()) {
            addReport(new AcsReportHeader(1200));
            addReport(new AcsReportHeader(1000).add(game().getCurrentRound()));
        } else {
            if (game().getCurrentRound() == 0) {
                addReport(new AcsReportHeader(1005));
            } else {
                addReport(new AcsReportHeader(1000).add(game().getCurrentRound()));
            }
        }
    }
}
