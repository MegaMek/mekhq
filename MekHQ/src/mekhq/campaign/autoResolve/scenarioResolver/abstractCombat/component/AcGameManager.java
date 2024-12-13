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

import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.ReportEntry;
import megamek.common.TurnOrdered;
import megamek.common.actions.EntityAction;
import megamek.common.enums.GamePhase;
import megamek.common.net.packets.Packet;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import megamek.server.AbstractGameManager;
import megamek.server.Server;
import megamek.server.commands.ServerCommand;
import megamek.server.victory.VictoryResult;
import mekhq.campaign.autoResolve.AutoResolveGame;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.actions.*;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.phase.PhaseHandler;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombat.reporter.AcReportEntry;
import mekhq.campaign.autoResolve.scenarioResolver.component.HtmlGameLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Luana Coppio
 */
public class AcGameManager extends AbstractGameManager {
    private static final MMLogger logger = MMLogger.create(AcGameManager.class);
    private final HtmlGameLogger gameLogger = HtmlGameLogger
        .create(PreferenceManager.getClientPreferences().getAutoResolveGameLogFilename());

    private AutoResolveGame game;
    private final List<AcReportEntry> pendingReports = new ArrayList<>();

    final AcPhaseEndManager phaseEndManager = new AcPhaseEndManager(this);
    final AcPhasePreparationManager phasePreparationManager = new AcPhasePreparationManager(this);
    final AcActionsProcessor actionsProcessor = new AcActionsProcessor(this);
    final AcInitiativeHelper initiativeHelper = new AcInitiativeHelper(this);
    final List<PhaseHandler> phaseHandlers = new ArrayList<>();

    /**
     * Iterates through the game phases until the victory condition is met.
     */
    public void runGame() {
        changePhase(GamePhase.STARTING_SCENARIO);
        while (!game.getPhase().equals(GamePhase.VICTORY)) {
            changePhase(GamePhase.INITIATIVE);
        }
    }

    public AutoResolveGame getGame() {
        return game;
    }

    public AcInitiativeHelper getInitiativeHelper() {
        return initiativeHelper;
    }

    public void addPhaseHandler(PhaseHandler handler) {
        phaseHandlers.add(handler);
    }

    public void addMoraleCheck(AcsMoraleCheckAction acsMoraleCheckAction, AcFormation formation) {
        getGame().addAction(acsMoraleCheckAction);
        formation.setDone(true);
    }

    public void addAttack(List<EntityAction> actions, AcFormation formation) {
        actions.forEach(getGame()::addAction);
        formation.setDone(true);
    }

    public void addNerveRecovery(AcsRecoveringNerveAction recoveringNerveAction, AcFormation formation) {
        getGame().addAction(recoveringNerveAction);
    }

    public void addWithdraw(AcsWithdrawAction acsWithdrawAction, AcFormation formation) {
        getGame().addAction(acsWithdrawAction);
    }

    public void addEngagementControl(AcsEngagementControlAction action, AcFormation formation) {
        getGame().addAction(action);
        formation.setDone(true);
    }

    public void flushPendingReports() {
        pendingReports.forEach(r -> gameLogger.add(r.text()));
        pendingReports.clear();
    }

    @Override
    public void setGame(IGame g) {
        if (!(g instanceof AutoResolveGame)) {
            logger.error("Attempted to set game to incorrect class. Received class {}", g.getClass().getSimpleName());
            throw new IllegalArgumentException("Attempted to set game to incorrect class.");
        }
        game = (AutoResolveGame) g;
    }

    @Override
    public List<ServerCommand> getCommandList(Server server) {
        return Collections.emptyList();
    }

    @Override
    public void addReport(ReportEntry r) {
        if (r instanceof AcReportEntry) {
            pendingReports.add((AcReportEntry) r);
        } else {
            pendingReports.add(new AcReportEntry(999).add(r.text()));
        }
    }

    @Override
    public void calculatePlayerInitialCounts() {
        for (Player player : game.getPlayersList()) {
            player.setInitialEntityCount(Math.toIntExact(game.getActiveFormations(player).stream()
                .filter(entity -> !entity.isRouted()).count()));
            game.getActiveFormations(player).stream().map(AcFormation::getPointValue).reduce(Integer::sum)
                .ifPresent(player::setInitialBV);
        }
    }


    @Override
    public void endCurrentPhase() {
        logger.debug("Ending phase {}", game.getPhase());
        phaseEndManager.managePhase();
    }

    @Override
    public void prepareForCurrentPhase() {
        logger.debug("Preparing phase {}", game.getPhase());
        phasePreparationManager.managePhase();
    }

    @Override
    public void executeCurrentPhase() {
        logger.debug("Executing phase {}", game.getPhase());
        phaseHandlers.forEach(PhaseHandler::execute);
        endCurrentPhase();
    }

    /**
     * Called at the beginning of certain phases to make every player not ready.
     */
    public void resetPlayersDone() {
        for (Player player : game.getPlayersList()) {
            player.setDone(false);
        }
    }

    public void resetFormationsDone() {
        for (AcFormation formation : game.getActiveFormations()) {
            formation.setDone(false);
        }
    }

    public void resetFormations() {
        for (AcFormation formation : game.getActiveFormations()) {
            formation.reset();
        }
    }

    /**
     * Rolls initiative for all teams.
     */
    public void rollInitiative() {
        TurnOrdered.rollInitiative(game.getTeams(), false);
    }

    /**
     * Returns true if victory conditions have been met. Victory conditions are
     * when there is only one player left with meks or only one team. will also
     * add some reports to reporting
     */
    public boolean checkForVictory() {
        if (game.getVictoryTeam() > 0) {
            // latch
            return true;
        }
        VictoryResult vr = game.getVictoryResult();
        var reports = vr.processVictory(game);
        if (!reports.isEmpty()) {
            reports.forEach(this::addReport);
            vr.setVictory(true);
            game.setVictoryTeam(vr.getWinningTeam());
        }
        return vr.isVictory();
    }


// region not in use
    @Override
    public void send(Packet packet) {
    }

    @Override
    public void resetGame() {
    }

    @Override
    public void disconnect(Player player) {
    }

    @Override
    public void removeAllEntitiesOwnedBy(Player player) {
    }

    @Override
    public void handleCfrPacket(Server.ReceivedPacket rp) {
    }

    @Override
    public void requestGameMaster(Player player) {
    }

    @Override
    public void requestTeamChange(int teamId, Player player) {
    }

    @Override
    public void handlePacket(int connId, Packet packet) {
    }

    @Override
    public void transmitAllPlayerUpdates() {
    }

    @Override
    public void sendCurrentInfo(int connId) {
    }

    @Override
    public void send(int connId, Packet packet) {
    }
// endregion not in use
}
