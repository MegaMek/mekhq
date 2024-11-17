package mekhq.campaign.autoResolve.scenarioResolver.unitsMatter;

import megamek.common.Entity;
import megamek.common.IEntityRemovalConditions;
import megamek.common.event.PostGameResolution;

import java.util.Enumeration;
import java.util.Vector;
import java.util.List;

public class AutoResolveConcludedEvent implements PostGameResolution {

    private final List<Entity> removedEntities;
    private final List<Entity> survivingEntities;
    private final boolean controlledScenario;


    public AutoResolveConcludedEvent(boolean controlledScenario, List<Entity> removedEntities, List<Entity> survivingEntities) {
        this.controlledScenario = controlledScenario;
        this.removedEntities = removedEntities;
        this.survivingEntities = survivingEntities;
    }

    public String getEventName() {
        return "Auto Resolve Concluded";
    }

    public boolean controlledScenario() {
        return controlledScenario;
    }

    @Override
    public Enumeration<Entity> getEntities() {
        Vector<Entity> entities = new Vector<>();
        survivingEntities.forEach(entities::addElement);
        removedEntities.stream()
            .filter(entity -> entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_CAPTURED)
            .forEach(entities::addElement);
        return entities.elements();
    }

    @Override
    public Entity getEntity(int id) {
        return null;
    }

    @Override
    public Enumeration<Entity> getGraveyardEntities() {
        Vector<Entity> graveyard = new Vector<>();
        removedEntities.stream()
            .filter(entity -> entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_SALVAGEABLE
                || entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_EJECTED)
            .forEach(graveyard::addElement);
        return graveyard.elements();
    }

    @Override
    public Enumeration<Entity> getWreckedEntities() {
        return null;
    }

    @Override
    public Enumeration<Entity> getRetreatedEntities() {
        Vector<Entity> retreatedEntities = new Vector<>();
        removedEntities.stream()
            .filter(entity -> entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_IN_RETREAT)
            .forEach(retreatedEntities::addElement);
        return retreatedEntities.elements();
    }

    @Override
    public Enumeration<Entity> getDevastatedEntities() {
        Vector<Entity> devastated = new Vector<>();
        removedEntities.stream()
            .filter(entity -> entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED)
            .forEach(devastated::addElement);
        return devastated.elements();
    }
}

