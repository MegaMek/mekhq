
/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.*;
import megamek.common.actions.EntityAction;
import megamek.common.enums.GamePhase;
import megamek.common.net.packets.Packet;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFReportEntry;
import megamek.logging.MMLogger;
import megamek.server.AbstractGameManager;
import megamek.server.Server;
import megamek.server.commands.ServerCommand;
import megamek.server.victory.VictoryResult;
import mekhq.campaign.autoResolve.AutoResolveGame;

import java.util.*;

/**
 * This class manages an SBF game on the server side. As of 2024, this is under
 * construction.
 */

public final class AcsGameManager extends AbstractGameManager {
    private static final MMLogger logger = MMLogger.create(megamek.server.sbf.SBFGameManager.class);

    private AutoResolveGame game;
    private final SBFFullGameReport gameReport = new SBFFullGameReport();
    private final List<SBFReportEntry> pendingReports = new ArrayList<>();

    final AcsPhaseEndManager phaseEndManager = new AcsPhaseEndManager(this);
    final AcsPhasePreparationManager phasePreparationManager = new AcsPhasePreparationManager(this);
    final AcsEngagementControlProcessor engagementControlProcessor = new AcsEngagementControlProcessor(this);
    final AcsAttackProcessor attackProcessor = new AcsAttackProcessor(this);
    final AcsActionsProcessor actionsProcessor = new AcsActionsProcessor(this);
    final AcsInitiativeHelper initiativeHelper = new AcsInitiativeHelper(this);
    final AcsRecoveringNerveProcessor recoveringNerveProcessor = new AcsRecoveringNerveProcessor(this);

    final List<PhaseHandler> phaseHandlers = new ArrayList<>();

    public AutoResolveGame getGame() {
        return game;
    }

    public void addPhaseHandler(PhaseHandler handler) {
        phaseHandlers.add(handler);
    }

    public void addAttack(List<EntityAction> actions, SBFFormation formation) {
        attackProcessor.processAttacks(actions, formation);
    }

    public void addNerveRecovery(AcsRecoveringNerveAction recoveringNerveAction, SBFFormation formation) {
        recoveringNerveProcessor.processRecoveringNerve(recoveringNerveAction, formation);
    }

    @Override
    public void setGame(IGame g) {
        if (!(g instanceof AutoResolveGame)) {
            logger.fatal("Attempted to set game to incorrect class.");
            return;
        }
        game = (AutoResolveGame) g;
    }

    @Override
    public List<ServerCommand> getCommandList(Server server) {
        return Collections.emptyList();
    }

    @Override
    public void addReport(ReportEntry r) {
        pendingReports.add((SBFReportEntry) r);
    }

    @Override
    public void calculatePlayerInitialCounts() {
        for (Player player : game.getPlayersList()) {
            player.setInitialEntityCount(Math.toIntExact(game.getActiveFormations(player).stream()
                .filter(entity -> !entity.isRouted()).count()));
            game.getActiveFormations(player).stream().map(SBFFormation::getPointValue).reduce(Integer::sum)
                .ifPresent(player::setInitialBV);
        }
    }

    @Override
    public void sendCurrentInfo(int connId) {
        // Offline game, no need to send anything
    }

    @Override
    public void endCurrentPhase() {
        logger.info("Ending phase {}", game.getPhase());
        phaseEndManager.managePhase();
    }

    @Override
    public void prepareForCurrentPhase() {
        logger.info("Preparing phase {}", game.getPhase());
        phasePreparationManager.managePhase();
    }

    @Override
    public void executeCurrentPhase() {
        logger.info("Executing phase {}", game.getPhase());
        phaseHandlers.forEach(PhaseHandler::execute);
    }

    /**
     * Called at the beginning of certain phases to make every player not ready.
     */
    public void resetPlayersDone() {
        for (Player player : game.getPlayersList()) {
            setPlayerDone(player, false);
        }
    }

    private void setPlayerDone(Player player, boolean done) {
        player.setDone(done);
    }


    public void addEngagementControl(AcsEngagementControlAction action, SBFFormation formation) {
        engagementControlProcessor.processEngagementControl(action, formation);
    }

    /**
     * Rolls initiative for all teams.
     */
    void rollInitiative() {
        TurnOrdered.rollInitiative(game.getTeams(), false);
    }

    public void clearPendingReports() {
        pendingReports.clear();
    }

    List<SBFReportEntry> getPendingReports() {
        return pendingReports;
    }

    void addPendingReportsToGame() {
        gameReport.add(game.getCurrentRound(), pendingReports);
    }

    public void changeToNextTurn() {
        var nextTurn = getNextValidTurn();
        if (nextTurn.isEmpty()) {
            endCurrentPhase();
        }
    }

    public Optional<AcsTurn> getNextValidTurn() {
        Optional<AcsTurn> nextTurn = game.changeToNextTurn();
        while (nextTurn.isPresent() && !nextTurn.get().isValid(game)) {
            nextTurn = game.changeToNextTurn();
        }
        return nextTurn;
    }

    void endCurrentTurn() {
        changeToNextTurn();
    }

    public void runGame() {
        changePhase(GamePhase.STARTING_SCENARIO);
    }


    /**
     * Returns true if victory conditions have been met. Victory conditions are
     * when there is only one player left with meks or only one team. will also
     * add some reports to reporting
     */

    public boolean checkForVictory() {
        VictoryResult vr = game.getVictoryResult();
        var reports = vr.processVictory(game);
        if (!reports.isEmpty()) {
            reports.forEach(this::addReport);
            vr.setVictory(true);
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

    public void resolveCallSupport() {
    }

    @Override
    public void handlePacket(int connId, Packet packet) {
    }

    @Override
    public void transmitAllPlayerUpdates() {
    }

    @Override
    public void send(int connId, Packet packet) {
    }

// endregion not in use
}
