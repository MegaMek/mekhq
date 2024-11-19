package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

public interface AcsAttackAction extends AcsAction {

    /**
     * @return The game ID of the target of the attack
     */
    int getTargetId();

}

