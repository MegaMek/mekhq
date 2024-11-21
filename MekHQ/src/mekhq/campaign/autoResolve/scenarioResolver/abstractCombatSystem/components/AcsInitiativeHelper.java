package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.strategicBattleSystems.*;


import java.util.*;
import java.util.stream.Collectors;

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
                .filter(SBFFormation.class::isInstance)
                .filter(unit -> ((SBFFormation) unit).isDeployed())
                .filter(unit -> ((SBFFormation) unit).isEligibleForPhase(phase))
                .map(InGameObject::getOwnerId)
                .map(AcsFormationTurn::new)
                .collect(Collectors.toList());

            turns.sort(Comparator.comparing(t -> game().getPlayer(t.playerId()).getInitiative()));

        } else {
            // As a fallback, provide unsorted turns
            turns = game().getInGameObjects().stream()
                .filter(SBFFormation.class::isInstance)
                .filter(unit -> ((SBFFormation) unit).isDeployed())
                .filter(unit -> ((SBFFormation) unit).isEligibleForPhase(phase))
                .map(InGameObject::getOwnerId)
                .map(AcsFormationTurn::new)
                .collect(Collectors.toList());

            // Now, assemble formations and sort by initiative and relative formation count
            Map<Integer, Long> unitCountsByPlayer = game().getInGameObjects().stream()
                .filter(SBFFormation.class::isInstance)
                .filter(unit -> ((SBFFormation) unit).isDeployed())
                .filter(unit -> ((SBFFormation) unit).isEligibleForPhase(phase))
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

    void writeInitiativeReport() {
        writeHeader();
        writeInitiativeRolls();
        writeTurnOrder();
        writeFutureDeployment();
        writeWeatherReport();
    }

    private void writeTurnOrder() {
        addReport(new SBFReportEntry(1020));

        for (AcsTurn turn : game().getTurnsList()) {
            Player player = game().getPlayer(turn.playerId());
            addReport(new SBFPlayerNameReportEntry(player).indent().addNL());
        }

    }

    private void writeFutureDeployment() {
        // remaining deployments
        Comparator<Deployable> comp = Comparator.comparingInt(Deployable::getDeployRound);
        List<Deployable> futureDeployments = game().getInGameObjects().stream()
            .filter(Deployable.class::isInstance)
            .map(Deployable.class::cast)
            .filter(unit -> !unit.isDeployed())
            .sorted(comp)
            .toList();

        if (!futureDeployments.isEmpty()) {
            addReport(new SBFPublicReportEntry(1060));
            int round = -1;

            for (Deployable deployable : futureDeployments) {
                if (round != deployable.getDeployRound()) {
                    round = deployable.getDeployRound();
                    addReport(new SBFPublicReportEntry(1065).add(round));
                }

                SBFReportEntry r = new SBFReportEntry(1066).subject(((InGameObject) deployable).getId());
                r.add(((InGameObject) deployable).generalName());
                r.add("1");
                r.add("2");
                addReport(r);
            }
            addReport(new SBFPublicReportEntry(1210).newLines(2));
        }
    }

    private void writeWeatherReport() {
        PlanetaryConditions conditions = game().getPlanetaryConditions();
        addReport(new SBFPublicReportEntry(1025).add(conditions.getWindDirection().toString()));
        addReport(new SBFPublicReportEntry(1030).add(conditions.getWind().toString()));
        addReport(new SBFPublicReportEntry(1031).add(conditions.getWeather().toString()));
        addReport(new SBFPublicReportEntry(1032).add(conditions.getLight().toString()));
        addReport(new SBFPublicReportEntry(1033).add(conditions.getFog().toString()));
    }

    private void writeInitiativeRolls() {
        for (Team team : game().getTeams()) {
            // Teams with no active players can be ignored
            if (team.isObserverTeam()) {
                continue;
            }

            // If there is only one non-observer player, list them as the 'team', and use the team initiative
            if (team.getNonObserverSize() == 1) {
                final Player player = team.nonObserverPlayers().get(0);
                addReport(new SBFPlayerNameReportEntry(player));
                addReport(new SBFPublicReportEntry(1015).noNL());
                addReport(new SBFInitiativeRollReportEntry(team.getInitiative()));
            } else {
                // Multiple players. List the team, then break it down.
                SBFReportEntry r = new SBFPublicReportEntry(1015).add(Player.TEAM_NAMES[team.getId()]);
                r.add(team.getInitiative().toString());
                addReport(r);
                for (Player player : team.nonObserverPlayers()) {
                    addReport(new SBFPublicReportEntry(1015).indent().add(player.getName()).add(player.getInitiative().toString()));
                }
            }
        }
    }

    private void writeHeader() {
        if (game().getLastPhase().isDeployment() || game().isDeploymentComplete()
            || !game().shouldDeployThisRound()) {
            addReport(new SBFReportHeader(1000).add(game().getCurrentRound()));
        } else {
            if (game().getCurrentRound() == 0) {
                addReport(new SBFReportHeader(1005));
            } else {
                addReport(new SBFReportHeader(1010).add(game().getCurrentRound()));
            }
        }
    }
}
