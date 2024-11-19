
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
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFReportEntry;
import megamek.logging.MMLogger;
import megamek.server.AbstractGameManager;
import megamek.server.Server;
import megamek.server.commands.ServerCommand;
import mekhq.campaign.autoResolve.helper.AutoResolveGame;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.PhaseHandler;

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
    final AcsMovementProcessor movementProcessor = new AcsMovementProcessor(this);
    final AcsAttackProcessor attackProcessor = new AcsAttackProcessor(this);
    final AcsActionsProcessor actionsProcessor = new AcsActionsProcessor(this);
    final AcsInitiativeHelper initiativeHelper = new AcsInitiativeHelper(this);
    final AcsDetectionHelper detectionHelper = new AcsDetectionHelper(this);
    final List<PhaseHandler> phaseHandlers = new ArrayList<>();

    public AutoResolveGame getGame() {
        return game;
    }

    public AcsGameManager addPhaseHandler(PhaseHandler handler) {
        phaseHandlers.add(handler);
        return this;
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
    public void send(Packet packet) {
        // This runs offline, there are no packets to send
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

    /**
     * Rolls initiative for all teams.
     */
    void rollInitiative() {
        TurnOrdered.rollInitiative(game.getTeams(), false);
        transmitAllPlayerUpdates();
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

    /**
     * Tries to change to the next turn. If there are no more turns, ends the
     * current phase. If the player whose turn it is next is not connected, we
     * allow the other players to skip that player.
     */
    private void changeToNextTurn() {
        if (!game.hasMoreTurns()) {
            endCurrentPhase();
            return;
        }

        AcsTurn nextTurn = game.changeToNextTurn();
        boolean isValidTurn = nextTurn.isValid(game);
        while (game.hasMoreTurns() && !isValidTurn) {
            nextTurn = game.changeToNextTurn();
            isValidTurn = nextTurn.isValid(game);
        }

        if (!isValidTurn) {
            endCurrentPhase();
        }
    }

    private List<InGameObject> getVisibleUnits(Player viewer) {
        return game.getFullyVisibleUnits(viewer);
    }

    /**
     * Send the round report to all connected clients.
     */
    public void sendReport() {
        // EmailService mailer = Server.getServerInstance().getEmailService();
        // if (mailer != null) {
        // for (var player: mailer.getEmailablePlayers(game)) {
        // try {
        // var reports = filterReportVector(vPhaseReport, player);
        // var message = mailer.newReportMessage(game, reports, player);
        // mailer.send(message);
        // } catch (Exception ex) {
        // logger.error("Error sending round report", ex);
        // }
        // }
        // }
        // game.getPlayersList().forEach(player -> send(player.getId(), createReportPacket(player)));
    }

    /**
     * Receives an entity movement packet, and if valid, executes it and ends
     * the current turn.
     */
    private void receiveMovement(Packet packet, int connId) {
        var movePath = (AcsMovePath) packet.getObject(0);
        movePath.restore(game);
        Optional<SBFFormation> formationInfo = game.getFormation(movePath.getEntityId());
        if (formationInfo.isEmpty()) {
            logger.error("Malformed packet {}", packet);
            return;
        }
        AcsTurn turn = game.getTurn();
        if ((turn == null) || !turn.isValid(connId, formationInfo.get(), game)) {
            logger.error("It is not player {}'s turn! ", connId);
            return;
        }

        movementProcessor.processMovement(movePath, formationInfo.get());
    }

    /**
     * Called when the current player has done his current turn and the turn
     * counter needs to be advanced.
     */
    void endCurrentTurn() {
        changeToNextTurn();
    }

    public void receiveAttack(int formationId, List<EntityAction> attacks, int playerId) {
        Optional<SBFFormation> formationInfo = game.getFormation(formationId);

        if (formationInfo.isEmpty()
            || !attacks.stream().map(EntityAction::getEntityId).allMatch(id -> id == formationId)) {
            logger.error("Invalid formation ID or diverging attacker IDs");
            changeToNextTurn();
            return;
        }

        for (EntityAction action : attacks) {
            if (!validateEntityAction(action, playerId)) {
                return;
            }
        }

        // is this the right phase?
        if (!getGame().getPhase().isFiring() && !getGame().getPhase().isPhysical()
            && !getGame().getPhase().isTargeting() && !getGame().getPhase().isOffboard()) {
            logger.error("Server got attack packet in wrong phase");
            return;
        }

        // looks like mostly everything's okay
        attackProcessor.processAttacks(attacks, formationInfo.get());
    }

    private boolean validateEntityAction(EntityAction action, int playerId) {
        Optional<SBFFormation> formationInfo = game.getFormation(action.getEntityId());
        if (formationInfo.isEmpty()) {
            logger.error("Incorrect formation ID {}", action.getEntityId());
            return false;
        }
        AcsTurn turn = game.getTurn();
        if ((turn == null) || !turn.isValid(playerId, formationInfo.get(), game)) {
            logger.error("It is not player {}'s turn! ", playerId);
            return false;
        }

        return true;
    }

    public void detectHiddenUnits() {
    }

    public void applyBuildingDamage() {
    }

    public void resolveCallSupport() {
    }

    public void updateSpacecraftDetection() {
    }

    public void detectSpacecraft() {
    }


    @Override
    public void handlePacket(int connId, Packet packet) {
        // This runs offline, there are no packets to send
    }

    @Override
    public void transmitAllPlayerUpdates() {
        // This runs offline, there are no packets to send
    }

    @Override
    public void send(int connId, Packet packet) {
        // This runs offline, there are no packets to send
    }

    public void runGame() {
        changePhase(GamePhase.STARTING_SCENARIO);
    }
}
