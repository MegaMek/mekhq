/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.autoResolve.damageHandler;

import megamek.common.*;


/**
 * Choose the correct DamageHandler for the given entity.
 * @author Luana Coppio
 */
public class DamageHandlerChooser {

    public enum EntityFinalState {
        ANY(false, false),
        CREW_MUST_SURVIVE(true, false),
        ENTITY_MUST_SURVIVE(false, true),
        CREW_AND_ENTITY_MUST_SURVIVE(true, true);

        final boolean crewMustSurvive;
        final boolean entityMustSurvive;

        EntityFinalState(boolean crewMustSurvive, boolean entityMustSurvive) {
            this.crewMustSurvive = crewMustSurvive;
            this.entityMustSurvive = entityMustSurvive;
        }

    }

    /**
     * Choose the correct DamageHandler for the given entity.
     * A damage handler is a class that handles applying damage on an entity, be it a Mek, Infantry, etc.
     * It can damage internal, armor, cause criticals, kill crew, set limbs as blown-off, can even destroy the entity,
     * @param entity the entity to choose the handler for
     * @return the correct DamageHandler for the given entity
     */
    public static DamageHandler<?> chooseHandler(Entity entity) {
        return chooseHandler(entity, EntityFinalState.ANY);
    }

    /**
     * Choose the correct DamageHandler for the given entity.
     * A damage handler is a class that handles applying damage on an entity, be it a Mek, Infantry, etc.
     * It can damage internal, armor, cause criticals, kill crew, set limbs as blown-off, can even destroy the entity,
     * This one also accepts parameters to indicate if the crew must survive and if the entity must survive.
     * @param entity the entity to choose the handler for
     * @param entityFinalState if the crew must survive and/or entity must survive
     * @return the correct DamageHandler for the given entity
     */
    public static DamageHandler<?> chooseHandler(
        Entity entity, EntityFinalState entityFinalState) {
        return chooseHandler(entity, entityFinalState.crewMustSurvive, entityFinalState.entityMustSurvive);
    }

    /**
     * Choose the correct DamageHandler for the given entity.
     * A damage handler is a class that handles applying damage on an entity, be it a Mek, Infantry, etc.
     * It can damage internal, armor, cause criticals, kill crew, set limbs as blown-off, can even destroy the entity,
     * This one also accepts parameters to indicate if the crew must survive and if the entity must survive.
     * @param entity the entity to choose the handler for
     * @param crewMustSurvive if the crew must survive
     * @param entityMustSurvive if the entity must survive
     * @return the correct DamageHandler for the given entity
     */
    public static DamageHandler<?> chooseHandler(
        Entity entity, boolean crewMustSurvive, boolean entityMustSurvive) {

        if (entity instanceof Infantry) {
            return new InfantryDamageHandler((Infantry) entity);
        } else if (entity instanceof Mek) {
            return new MekDamageHandler((Mek) entity, crewMustSurvive, entityMustSurvive);
        } else if (entity instanceof GunEmplacement) {
            return new GunEmplacementDamageHandler((GunEmplacement) entity, crewMustSurvive, entityMustSurvive);
        } else if (entity instanceof Aero) {
            return new AeroDamageHandler((Aero) entity, crewMustSurvive, entityMustSurvive);
        }
        return new SimpleDamageHandler(entity, crewMustSurvive, entityMustSurvive);
    }

    public static void damageRemovedEntity(Entity entity, int removalCondition) {
        double targetDamage = switch (removalCondition) {
            case IEntityRemovalConditions.REMOVE_CAPTURED, IEntityRemovalConditions.REMOVE_EJECTED -> (double) (entity.getTotalOArmor() * (1 / (Compute.d6() + 1)));
            case IEntityRemovalConditions.REMOVE_DEVASTATED -> -1; // no damage is actually applied
            case IEntityRemovalConditions.REMOVE_IN_RETREAT -> entity.getTotalOArmor() * 0.8;
            case IEntityRemovalConditions.REMOVE_SALVAGEABLE -> entity.getTotalOArmor() * 0.75;
            default -> entity.getTotalOArmor() * 0.33;
        };
        var numberOfDices = Math.max(1, (int) (targetDamage / 6 / 0.6));
        var damage = Compute.d6(numberOfDices);
        var clusterSize = 5;

        var retreating = removalCondition == IEntityRemovalConditions.REMOVE_IN_RETREAT;
        var captured = removalCondition == IEntityRemovalConditions.REMOVE_CAPTURED;
        var ejected = removalCondition == IEntityRemovalConditions.REMOVE_EJECTED;
        var devastated = removalCondition == IEntityRemovalConditions.REMOVE_DEVASTATED;

        var crewMustSurvive = (retreating || captured || ejected);
        var entityMustSurvive = !devastated;

        DamageHandlerChooser.chooseHandler(entity, crewMustSurvive, entityMustSurvive)
            .applyDamageInClusters(damage, clusterSize);
    }
}
