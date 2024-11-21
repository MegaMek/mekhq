package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.IGame;
import megamek.common.InGameObject;
import mekhq.campaign.autoResolve.AutoResolveGame;

public class AcsPlayerTurn extends AcsTurn {

    /**
     * Creates a new player action turn for an SBF Game.
     *
     * @param playerId The player who has to take action
     */
    public AcsPlayerTurn(int playerId) {
        super(playerId);
    }

    @Override
    public boolean isValidEntity(InGameObject unit, IGame game) {
        return false;
    }

    @Override
    public boolean isValid(AutoResolveGame game) {
        return game.getPlayer(playerId()) != null;
    }
}
