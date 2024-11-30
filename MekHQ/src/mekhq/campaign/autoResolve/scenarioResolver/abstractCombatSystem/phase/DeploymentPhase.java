package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.phase;

import megamek.common.enums.GamePhase;
import mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components.AcsGameManager;

public class DeploymentPhase extends PhaseHandler {

        public DeploymentPhase(AcsGameManager gameManager) {
            super(gameManager, GamePhase.DEPLOYMENT);
        }

        @Override
        protected void executePhase() {
            // Automatically deploy all formations that are set to deploy this round
            getGameManager().getGame().getActiveFormations().stream()
                .filter( f-> !f.isDeployed())
                .filter( f-> f.getDeployRound() == getGameManager().getGame().getCurrentRound())
                .forEach( f-> f.setDeployed(true));
        }
}
