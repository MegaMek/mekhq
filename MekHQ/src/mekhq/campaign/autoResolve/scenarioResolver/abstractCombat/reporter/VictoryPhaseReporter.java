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
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.reporter;

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Player;
import mekhq.campaign.autoResolve.AutoResolveGame;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.component.AcGameManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class VictoryPhaseReporter {

    private final IGame game;
    private final Consumer<AcReportEntry> reportConsumer;

    public VictoryPhaseReporter(IGame game, Consumer<AcReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public void victoryHeader() {
        reportConsumer.accept(new AcPublicReportEntry(999));
        reportConsumer.accept(new AcPublicReportEntry(5000));
    }

    public void victoryResult(AcGameManager gameManager) {
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
            var teamReport = new AcPublicReportEntry(5002).add(team);
            reportConsumer.accept(teamReport);
            for (var player : teamPlayers) {
                var playerEntities = acsAutoResolveGame.getInGameObjects().stream()
                    .filter(e -> e.getOwnerId() == player.getId())
                    .filter(Entity.class::isInstance).toList();

                reportConsumer.accept(new AcPublicReportEntry(5003).add(new AcPlayerNameReportEntry(player).reportText())
                    .add(playerEntities.size()).indent());

                for (var entity : playerEntities) {
                    reportConsumer.accept(new AcPublicReportEntry(5004)
                        .add(new AcEntityNameReportEntry((Entity) entity).reportText())
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
