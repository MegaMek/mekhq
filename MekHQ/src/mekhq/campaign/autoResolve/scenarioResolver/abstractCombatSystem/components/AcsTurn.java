package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.AbstractPlayerTurn;
import mekhq.campaign.autoResolve.AutoResolveGame;

public abstract class AcsTurn extends AbstractPlayerTurn {

    public AcsTurn(int playerId) {
        super(playerId);
    }

    /**
     * Returns true when this turn can be played given the current state of the given game. Should
     * return false when e.g. no valid formation can be found to move for the player or the player or
     * formation of the turn is null (e.g. because it has previously been destroyed).
     *
     * @param game The game object
     * @return True when the turn can be played, false when it should be skipped
     */
    public abstract boolean isValid(AutoResolveGame game);
}
