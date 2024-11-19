package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import java.io.Serializable;

public abstract class AbstractAcsAttackAction implements AcsAttackAction, Serializable {

    private final int entityId;
    private final int targetId;

    public AbstractAcsAttackAction(int entityId, int targetId) {
        this.entityId = entityId;
        this.targetId = targetId;
    }

    @Override
    public int getEntityId() {
        return entityId;
    }

    @Override
    public int getTargetId() {
        return targetId;
    }

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + "]: Unit ID " + entityId + "; Target ID " + targetId;
    }
}
