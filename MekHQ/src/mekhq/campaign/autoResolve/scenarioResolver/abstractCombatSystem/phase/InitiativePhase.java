package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.phase;

import megamek.common.enums.GamePhase;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;

public class InitiativePhase extends PhaseHandler {

    public InitiativePhase(AcsGameManager gameManager) {
        super(gameManager, GamePhase.INITIATIVE);
    }

    @Override
    protected void executePhase() {
        getGameManager().calculatePlayerInitialCounts();
        getGameManager().resetPlayersDone();
        getGameManager().rollInitiative();
        getGameManager().incrementAndSendGameRound();
        getGameManager().getInitiativeHelper().writeInitiativeReport();
    }
}
