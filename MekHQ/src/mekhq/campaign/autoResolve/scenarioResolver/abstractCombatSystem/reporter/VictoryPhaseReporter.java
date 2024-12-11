package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.reporter;

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Player;
import mekhq.campaign.autoResolve.AutoResolveGame;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.component.AcsGameManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class VictoryPhaseReporter {

    private final IGame game;
    private final Consumer<AcsReportEntry> reportConsumer;

    public VictoryPhaseReporter(IGame game, Consumer<AcsReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public void victoryHeader() {
        reportConsumer.accept(new AcsPublicReportEntry(999));
        reportConsumer.accept(new AcsPublicReportEntry(5000));
    }

    public void victoryResult(AcsGameManager gameManager) {
        var players = gameManager.getGame().getPlayersList();
        var teamMap = new HashMap<Integer, List<Player>>();

        for (var player : players) {
            var team = player.getTeam();
            if (!teamMap.containsKey(team)) {
                teamMap.put(team, new ArrayList<>());
            }
            teamMap.get(team).add(player);
        }

        var acsAutoResolveGame = (AutoResolveGame) game;

        for (var team : teamMap.keySet()) {
            var teamPlayers = teamMap.get(team);
            var teamReport = new AcsPublicReportEntry(5002).add(team);
            reportConsumer.accept(teamReport);
            for (var player : teamPlayers) {
                var playerEntities = acsAutoResolveGame.getInGameObjects().stream()
                    .filter(e -> e.getOwnerId() == player.getId())
                    .filter(Entity.class::isInstance).toList();

                reportConsumer.accept(new AcsPublicReportEntry(5003).add(new AcsPlayerNameReportEntry(player).reportText())
                    .add(playerEntities.size()).indent());

                for (var entity : playerEntities) {
                    reportConsumer.accept(new AcsPublicReportEntry(5004)
                        .add(new AcsEntityNameReportEntry((Entity) entity).reportText())
                        .add(String.format("%.2f%%", ((Entity) entity).getArmorRemainingPercent() * 100))
                        .add(String.format("%.2f%%", ((Entity) entity).getInternalRemainingPercent() * 100))
                        .add(((Entity) entity).getCrew().getName())
                        .add(((Entity) entity).getCrew().getHits())
                        .indent(2));
                }
            }
        }
    }
}
