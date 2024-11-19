package mekhq.campaign.autoResolve.scenarioResolver.abstractCombatSystem.components;

import megamek.common.actions.EntityAction;
import mekhq.campaign.autoResolve.helper.AutoResolveGame;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class AcsMovePath implements EntityAction, Serializable {

    private final int formationId;
    private final AtomicInteger steps = new AtomicInteger(0);

    public AcsMovePath(int formationId) {
        this.formationId = formationId;
    }

    @Override
    public int getEntityId() {
        return formationId;
    }

    public int getMpUsed() {
        return steps.get();
    }

    public void addStep() {
        steps.incrementAndGet();
    }

    public boolean isIllegal() {
        return false;
    }

    public void restore(AutoResolveGame game) {
    }

    @Override
    public String toString() {
        return "[AcsMovePath]: ID: " + formationId + "; steps: " + steps;
    }
}

