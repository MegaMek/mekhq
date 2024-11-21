package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.IGame;
import megamek.common.actions.EntityAction;

public interface AcsAction extends EntityAction {

    /**
     * @return A handler that will process this action as an extension of the SBFGameManager
     */
    AcsActionHandler getHandler(AcsGameManager gameManager);

    /**
     * Validates the data of this action. Validation should not check game rule details, only if the action
     * can be handled without running into missing or false data (NullPointerExceptions). Errors should
     * be logged. The action will typically be ignored and removed if validation fails.
     *
     * @param game The game
     * @return true when this action is valid, false otherwise
     */
    boolean isDataValid(IGame game);
}

