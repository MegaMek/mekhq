/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package mekhq.campaign.autoResolve.scenarioResolver.components;

import megamek.common.Entity;
import megamek.common.IEntityRemovalConditions;
import megamek.common.IGame;
import megamek.common.event.PostGameResolution;

import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 * @author Luana Coppio
 */
public class AutoResolveConcludedEvent implements PostGameResolution {

    private final List<Entity> removedEntities;
    private final List<Entity> survivingEntities;
    private final boolean controlledScenario;
    private final IGame game;


    public AutoResolveConcludedEvent(boolean controlledScenario, List<Entity> removedEntities, List<Entity> survivingEntities, IGame game) {
        this.controlledScenario = controlledScenario;
        this.removedEntities = removedEntities;
        this.survivingEntities = survivingEntities;
        this.game = game;
    }

    public String getEventName() {
        return "Auto Resolve Concluded";
    }

    public IGame getGame() {
        return game;
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

