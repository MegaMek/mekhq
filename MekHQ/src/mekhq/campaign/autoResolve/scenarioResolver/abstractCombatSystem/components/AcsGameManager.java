/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.*;
import megamek.common.actions.EntityAction;
import megamek.common.enums.GamePhase;
import megamek.common.net.packets.Packet;
import megamek.common.preference.PreferenceManager;
import megamek.common.strategicBattleSystems.SBFReportEntry;
import megamek.logging.MMLogger;
import megamek.server.AbstractGameManager;
import megamek.server.Server;
import megamek.server.commands.ServerCommand;
import megamek.server.victory.VictoryResult;
import mekhq.campaign.autoResolve.AutoResolveGame;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.actions.*;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.phase.PhaseHandler;
import mekhq.campaign.autoResolve.scenarioResolver.components.HtmlGameLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * @author Luana Coppio
 */
public final class AcsGameManager extends AbstractGameManager {
    private static final MMLogger logger = MMLogger.create(megamek.server.sbf.SBFGameManager.class);
    private final HtmlGameLogger gameLogger = HtmlGameLogger
        .create(PreferenceManager.getClientPreferences().getGameLogFilename())
        .printToConsole();


    private AutoResolveGame game;
    private final SBFFullGameReport gameReport = new SBFFullGameReport();
    private final List<SBFReportEntry> pendingReports = new ArrayList<>();

    final AcsPhaseEndManager phaseEndManager = new AcsPhaseEndManager(this);
    final AcsPhasePreparationManager phasePreparationManager = new AcsPhasePreparationManager(this);
    final AcsActionsProcessor actionsProcessor = new AcsActionsProcessor(this);
    final AcsInitiativeHelper initiativeHelper = new AcsInitiativeHelper(this);
    final List<PhaseHandler> phaseHandlers = new ArrayList<>();


    public void runGame() {
        changePhase(GamePhase.STARTING_SCENARIO);
        while (!game.getPhase().equals(GamePhase.VICTORY)) {
            changePhase(GamePhase.INITIATIVE);
        }
    }


    public AutoResolveGame getGame() {
        return game;
    }

    public AcsPhasePreparationManager getPhasePreparationManager() {
        return phasePreparationManager;
    }

    public AcsInitiativeHelper getInitiativeHelper() {
        return initiativeHelper;
    }

    public void addPhaseHandler(PhaseHandler handler) {
        phaseHandlers.add(handler);
    }

    public void addMoraleCheck(AcsMoraleCheckAction acsMoraleCheckAction, AcsFormation formation) {
        getGame().addAction(acsMoraleCheckAction);
        formation.setDone(true);
    }

    public void addAttack(List<EntityAction> actions, AcsFormation formation) {
        actions.forEach(getGame()::addAction);
        formation.setDone(true);
    }

    public void addNerveRecovery(AcsRecoveringNerveAction recoveringNerveAction, AcsFormation formation) {
        getGame().addAction(recoveringNerveAction);
    }

    public void addWithdraw(AcsWithdrawAction acsWithdrawAction, AcsFormation formation) {
        getGame().addAction(acsWithdrawAction);
    }

    public void addEngagementControl(AcsEngagementControlAction action, AcsFormation formation) {
        getGame().addAction(action);
        formation.setDone(true);
    }

    public void flushPendingReports() {
        pendingReports.forEach(r -> gameReport.add(game.getCurrentRound(), r));
        pendingReports.clear();
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
            game.getActiveFormations(player).stream().map(AcsFormation::getPointValue).reduce(Integer::sum)
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
        for (AcsFormation formation : game.getActiveFormations()) {
            formation.setDone(false);
        }
    }

    public void resetFormations() {
        for (AcsFormation formation : game.getActiveFormations()) {
            formation.reset();
        }
    }

    /**
     * Rolls initiative for all teams.
     */
    public void rollInitiative() {
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
