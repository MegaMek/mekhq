package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.IGame;
import megamek.common.InGameObject;
import megamek.common.strategicBattleSystems.SBFFormation;
import mekhq.campaign.autoResolve.AutoResolveGame;

public class AcsFormationTurn extends AcsTurn {

        /**
         * Creates a new player turn for an SBF Game.
         *
         * @param playerId The player who has to take action
         */
        public AcsFormationTurn(int playerId) {
            super(playerId);
        }

        @Override
        public boolean isValid(AutoResolveGame game) {
            return (game.getPlayer(playerId()) != null) && game.hasEligibleFormation(this);
        }

        @Override
        public boolean isValidEntity(InGameObject unit, IGame game) {
            return (unit.getOwnerId() == playerId()) && unit instanceof SBFFormation
                && ((SBFFormation) unit).isEligibleForPhase(game.getPhase());
        }
}
