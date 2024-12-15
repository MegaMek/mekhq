/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.autoresolve.acar;

import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.ReportEntry;
import megamek.common.TurnOrdered;
import megamek.common.enums.GamePhase;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import megamek.server.AbstractGameManager;
import megamek.server.Server;
import megamek.server.commands.ServerCommand;
import megamek.server.victory.VictoryResult;
import mekhq.campaign.autoresolve.acar.action.*;
import mekhq.campaign.autoresolve.acar.manager.ActionsProcessor;
import mekhq.campaign.autoresolve.acar.manager.InitiativeHelper;
import mekhq.campaign.autoresolve.acar.manager.PhaseEndManager;
import mekhq.campaign.autoresolve.acar.manager.PhasePreparationManager;
import mekhq.campaign.autoresolve.acar.phase.PhaseHandler;
import mekhq.campaign.autoresolve.acar.report.HtmlGameLogger;
import mekhq.campaign.autoresolve.acar.report.PublicReportEntry;
import mekhq.campaign.autoresolve.component.Formation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimulationManager extends AbstractGameManager {
    private static final MMLogger logger = MMLogger.create(SimulationManager.class);
    private final HtmlGameLogger gameLogger = HtmlGameLogger
        .create(PreferenceManager.getClientPreferences().getAutoResolveGameLogFilename());

    private final List<ReportEntry> pendingReports = new ArrayList<>();

    final PhaseEndManager phaseEndManager = new PhaseEndManager(this);
    final PhasePreparationManager phasePreparationManager = new PhasePreparationManager(this);
    public final ActionsProcessor actionsProcessor = new ActionsProcessor(this);
    public final InitiativeHelper initiativeHelper = new InitiativeHelper(this);
    final List<PhaseHandler> phaseHandlers = new ArrayList<>();

    private SimulationContext simulationContext;

    public void execute() {
        changePhase(GamePhase.STARTING_SCENARIO);
        while (!simulationContext.getPhase().equals(GamePhase.VICTORY)) {
            changePhase(GamePhase.INITIATIVE);
        }
    }

    public void addPhaseHandler(PhaseHandler phaseHandler) {
        phaseHandlers.add(phaseHandler);
    }

    @Override
    protected void endCurrentPhase() {
        logger.debug("Ending phase {}", getGame().getPhase());
        phaseEndManager.managePhase();
    }

    @Override
    public void prepareForCurrentPhase() {
        logger.debug("Preparing phase {}", getGame().getPhase());
        phasePreparationManager.managePhase();
    }

    @Override
    public void executeCurrentPhase() {
        logger.debug("Executing phase {}", getGame().getPhase());
        phaseHandlers.forEach(PhaseHandler::execute);
        endCurrentPhase();
    }

    /**
     * Called at the beginning of certain phases to make every player ready.
     */
    public void resetPlayersDone() {
        for (Player player : getGame().getPlayersList()) {
            player.setDone(false);
        }
    }

    public void resetFormations() {
        for (var formation : getGame().getActiveFormations()) {
            formation.reset();
        }
    }

    /**
     * Rolls initiative for all teams.
     */
    public void rollInitiative() {
        TurnOrdered.rollInitiative(getGame().getTeams(), false);
    }

    /**
     * Returns true if victory conditions have been met. Victory conditions are
     * when there is only one player left with meks or only one team. will also
     * add some reports to reporting
     */
    public boolean checkForVictory() {
        if (getGame().getVictoryTeam() > 0) {
            // latch
            return true;
        }
        VictoryResult vr = getGame().getVictoryResult();
        var reports = vr.processVictory(getGame());
        if (!reports.isEmpty()) {
            reports.forEach(this::addReport);
            vr.setVictory(true);
            getGame().setVictoryTeam(vr.getWinningTeam());
        }
        return vr.isVictory();
    }

    @Override
    public SimulationContext getGame() {
        return simulationContext;
    }

    public void setSimulationContext(SimulationContext simulationContext) {
        setGame(simulationContext);
    }

    @Override
    public void setGame(IGame game) {
        if (!(game instanceof SimulationContext)) {
            throw new IllegalArgumentException("SimulationManager can only manage Simulation games");
        }
        this.simulationContext = (SimulationContext) game;
    }

    public InitiativeHelper getInitiativeHelper() {
        return initiativeHelper;
    }

    public void addMoraleCheck(MoraleCheckAction acsMoraleCheckAction, Formation formation) {
        getGame().addAction(acsMoraleCheckAction);
        formation.setDone(true);
    }

    public void addAttack(List<Action> actions, Formation formation) {
        actions.forEach(getGame()::addAction);
        formation.setDone(true);
    }

    public void addNerveRecovery(RecoveringNerveAction recoveringNerveAction, Formation formation) {
        getGame().addAction(recoveringNerveAction);
    }

    public void addWithdraw(WithdrawAction acsWithdrawAction, Formation formation) {
        getGame().addAction(acsWithdrawAction);
    }

    public void addEngagementControl(EngagementControlAction action, Formation formation) {
        getGame().addAction(action);
        formation.setDone(true);
    }

    public void flushPendingReports() {
        pendingReports.forEach(r -> gameLogger.add(r.text()));
        pendingReports.clear();
    }

    @Override
    public List<ServerCommand> getCommandList(Server server) {
        return Collections.emptyList();
    }

    @Override
    public void addReport(ReportEntry r) {
        if (r instanceof PublicReportEntry publicReportEntry) {
            pendingReports.add(publicReportEntry);
        } else {
            pendingReports.add(new PublicReportEntry(999).add(r.text()));
        }
    }

    @Override
    public void calculatePlayerInitialCounts() {
        for (Player player : getGame().getPlayersList()) {
            player.setInitialEntityCount(Math.toIntExact(getGame().getActiveFormations(player).stream()
                .filter(entity -> !entity.isRouted()).count()));
            getGame().getActiveFormations(player).stream().map(Formation::getPointValue).reduce(Integer::sum)
                .ifPresent(player::setInitialBV);
        }
    }

    @Override
    public void removeAllEntitiesOwnedBy(Player player) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void resetGame() {
        throw new UnsupportedOperationException("Not implemented");
    }

    // not to be implemented methods
    @Override
    public void disconnect(Player player) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void sendCurrentInfo(int connId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void handleCfrPacket(Server.ReceivedPacket rp) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void requestGameMaster(Player player) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void requestTeamChange(int teamId, Player player) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
