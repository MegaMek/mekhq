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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component;

import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.planetaryconditions.PlanetaryConditions;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.reporter.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Luana Coppio
 */
public record AcInitiativeHelper(AcGameManager gameManager) implements AcGameManagerHelper {

    /**
     * Determines the turn order for a given phase, setting the game's turn list and sending it to the
     * Clients. Also resets the turn index.
     *
     * @param phase The phase to find the turns for
     * @see AbstractGame#resetTurnIndex()
     */
    void determineTurnOrder(GamePhase phase) {
        final List<AcTurn> turns;
        if (phase.isFiring() || phase.isMovement()) {
            turns = game().getInGameObjects().stream()
                .filter(AcFormation.class::isInstance)
                .filter(unit -> ((AcFormation) unit).isDeployed())
                .filter(unit -> ((AcFormation) unit).isEligibleForPhase(phase))
                .map(InGameObject::getOwnerId)
                .map(AcFormationTurn::new)
                .sorted(Comparator.comparing(t -> game().getPlayer(t.playerId()).getInitiative()))
                .collect(Collectors.toList());
        } else if (phase.isDeployment()) {
            // Deployment phase: sort by initiative
            turns = game().getInGameObjects().stream()
                .filter(AcFormation.class::isInstance)
                .filter(unit -> !((AcFormation) unit).isDeployed())
                .map(InGameObject::getOwnerId)
                .map(AcFormationTurn::new)
                .sorted(Comparator.comparing(t -> game().getPlayer(t.playerId()).getInitiative()))
                .collect(Collectors.toList());

        } else {
            // As a fallback, provide unsorted turns
            turns = game().getInGameObjects().stream()
                .filter(AcFormation.class::isInstance)
                .filter(unit -> ((AcFormation) unit).isDeployed())
                .filter(unit -> ((AcFormation) unit).isEligibleForPhase(phase))
                .map(InGameObject::getOwnerId)
                .map(AcFormationTurn::new)
                .collect(Collectors.toList());

            // Now, assemble formations and sort by initiative and relative formation count
            Map<Integer, Long> unitCountsByPlayer = game().getInGameObjects().stream()
                .filter(AcFormation.class::isInstance)
                .filter(unit -> ((AcFormation) unit).isDeployed())
                .filter(unit -> ((AcFormation) unit).isEligibleForPhase(phase))
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
                    List<AcTurn> sortedTurns = new ArrayList<>();
                    for (int initCycle = 0; initCycle < lowestUnitCount; initCycle++) {
                        long currentLowestUnitCount = Collections.min(unitCountsByPlayer.values());
                        for (int playerId : playersByInitiative) {
                            long unitsToMove = unitCountsByPlayer.get(playerId) / currentLowestUnitCount;
                            long remainingUnits = unitCountsByPlayer.get(playerId);
                            unitsToMove = Math.min(unitsToMove, remainingUnits);
                            for (int i = 0; i < unitsToMove; i++) {
                                sortedTurns.add(new AcFormationTurn(playerId));
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
        addReport(new AcReportEntry(1020));

        for (AcTurn turn : game().getTurnsList()) {
            Player player = game().getPlayer(turn.playerId());
            addReport(new AcPlayerNameReportEntry(player).indent().addNL());
        }

    }

    private void writeFutureDeployment() {
        // remaining deployments
        Comparator<Deployable> comp = Comparator.comparingInt(Deployable::getDeployRound);
        List<Deployable> futureDeployments = game().getInGameObjects().stream()
            .filter(AcFormation.class::isInstance)
            .map(Deployable.class::cast)
            .filter(unit -> !unit.isDeployed())
            .sorted(comp)
            .toList();

        if (!futureDeployments.isEmpty()) {
            addReport(new AcPublicReportEntry(1060));
            int round = -1;
            for (Deployable deployable : futureDeployments) {
                if (round != deployable.getDeployRound()) {
                    round = deployable.getDeployRound();
                    addReport(new AcPublicReportEntry(1065).add(round));
                }

                var r = new AcReportEntry(1066)
                    .add(new AcFormationReportEntry((AcFormation) deployable, game()).text())
                    .add(((InGameObject) deployable).getId())
                    .add(deployable.getDeployRound())
                    .indent();
                addReport(r);
            }
        }
    }

    private void writeWeatherReport() {
        PlanetaryConditions conditions = game().getPlanetaryConditions();
        addReport(new AcPublicReportEntry(1025).add(conditions.getWindDirection().toString()));
        addReport(new AcPublicReportEntry(1030).add(conditions.getWind().toString()));
        addReport(new AcPublicReportEntry(1031).add(conditions.getWeather().toString()));
        addReport(new AcPublicReportEntry(1032).add(conditions.getLight().toString()));
        addReport(new AcPublicReportEntry(1033).add(conditions.getFog().toString()));
    }

    private void writeInitiativeRolls() {
        for (Team team : game().getTeams()) {
            // Teams with no active players can be ignored
            if (team.isObserverTeam()) {
                continue;
            }
            addReport(new AcPublicReportEntry(1015).add(Player.TEAM_NAMES[team.getId()])
                .add(team.getInitiative().toString()));

            // Multiple players. List the team, then break it down.
            for (Player player : team.nonObserverPlayers()) {
                addReport(new AcPublicReportEntry(2020)
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
            addReport(new AcReportHeader(1200));
            addReport(new AcReportHeader(1000).add(game().getCurrentRound()));
        } else {
            if (game().getCurrentRound() == 0) {
                addReport(new AcReportHeader(1005));
            } else {
                addReport(new AcReportHeader(1000).add(game().getCurrentRound()));
            }
        }
    }
}
