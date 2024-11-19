package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.actions.EntityAction;

public interface AcsActionHandler extends AcsGameManagerHelper {

    /**
     * @return True when this handler should be called at the present state of the SBFGame (e.g. the phase).
     * In that case, {@link #handle()} should be called.
     */
    boolean cares();

    /**
     * Handles the action, e.g. attack with everything that's necessary, such as adding a report and sending
     * changes to the Clients. When the handler has finished handling the action and is no longer needed,
     * it must call {@link #setFinished()} to mark itself as a candidate for removal.
     */
    void handle();

    void setFinished();

    /**
     * If it returns true, it must be removed from the list of active handlers
     *  (handling this action is finished entirely). If it returns false, it must remain.
     *
     * @return False when this handler must remain active after doing its present handling, true otherwise
     */
    boolean isFinished();

    /**
     * @return The EntityAction that this handler is executing.
     */
    EntityAction getAction();

}
