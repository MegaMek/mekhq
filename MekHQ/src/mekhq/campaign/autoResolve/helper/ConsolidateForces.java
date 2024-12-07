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
package mekhq.campaign.autoResolve.helper;

import megamek.common.Entity;
import megamek.common.ForceAssignable;
import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.icons.Camouflage;

import java.util.ArrayList;
import java.util.List;

/**
 * Redistributes entities and subforces in forces to ensure that each force has a maximum of 20 entities and 4 subforces.
 *
 * @author Luana Coppio
 */
public class ConsolidateForces {

    /**
     * Consolidates forces by redistributing entities and subforces as needed.
     * @param game The game to consolidate forces for
     */
    public static void consolidateForces(IGame game) {
        Forces forces = game.getForces();
        var allForces = forces.getAllForces();

        // First, process subforces
        List<Force> subforcesToSplit = new ArrayList<>();

        for (Force force : allForces) {
            if (force.getParentId() != Force.NO_FORCE) { // It's a subforce
                // Check if the subforce has subforces of its own
                ArrayList<Force> subSubForces = forces.getFullSubForces(force);
                if (!subSubForces.isEmpty()) {
                    // Flatten the hierarchy by moving sub-subforces to the parent force
                    Force parentForce = forces.getForce(force.getParentId());

                    for (Force subSubForce : subSubForces) {
                        // Attach sub-subforce to the parent force
                        forces.attachForce(subSubForce, parentForce);
                    }
                }

                // Check if the subforce has zero entities
                List<ForceAssignable> entities = forces.getFullEntities(force);
                if (entities.isEmpty()) {
                    // Remove the subforce from its parent
                    forces.deleteForce(force.getId());
                    continue; // Move to the next force
                }

                // Check if the subforce has more than 6 entities or mixed unit types
                if (entities.size() > 6 || hasMixedUnitTypes(entities)) {
                    subforcesToSplit.add(force);
                }
            }
        }

        // Split subforces that need splitting
        for (Force force : subforcesToSplit) {
            splitSubforce(force, forces, game);
        }

        // Now process top-level forces
        List<Force> topLevelForces = forces.getTopLevelForces();

        for (Force force : topLevelForces) {
            // Handle top-level forces that have entities directly
            List<ForceAssignable> entities = forces.getFullEntities(force);
            if (!entities.isEmpty()) {
                // Entities are directly under the top-level force, which is invalid
                // We need to create subforces to hold these entities
                splitTopLevelForceEntities(force, forces, game);
            }

            // After assigning entities to subforces, get the updated list of subforces
            ArrayList<Force> subForces = forces.getFullSubForces(force);

            // Check if the force has more than 4 subforces
            if (subForces.size() > 4) {
                redistributeSubforces(force, forces, game);
            }

            // Check if the total number of entities exceeds 20
            int totalEntities = getTotalEntities(force, forces);
            if (totalEntities > 20) {
                redistributeEntities(force, forces, game);
            }
        }

        var forceIds = forces.getAllForces().stream().map(Force::getId).toList();

        // Remove all empty forces and subforces after consolidation
        for (var forceId : forceIds) {
            var forceToEval = forces.getForce(forceId);
            var entitiesOnForce = forces.getFullEntities(forceToEval);
            if (entitiesOnForce.isEmpty()) {
                forces.deleteForce(forceId);
            }
        }
    }

    private static int getTotalEntities(Force force, Forces forces) {
        return forces.getFullEntities(force).size();
    }

    private static void splitSubforce(Force force, Forces forces, IGame game) {
        // Get entities of the subforce
        List<ForceAssignable> entities = forces.getFullEntities(force);

        if (entities.size() <= 6 && !hasMixedUnitTypes(entities)) {
            // No need to split
            return;
        }

        // Separate entities by unit type
        List<ForceAssignable> groundEntities = new ArrayList<>();
        List<ForceAssignable> aerospaceEntities = new ArrayList<>();

        for (ForceAssignable entity : entities) {
            if (isAerospaceUnit(entity)) {
                aerospaceEntities.add(entity);
            } else {
                groundEntities.add(entity);
            }
        }

        // Remove all entities from the original subforce
        for (ForceAssignable entity : entities) {
            forces.removeEntityFromForces(entity.getId());
        }

        // Parent force
        Force parentForce = forces.getForce(force.getParentId());
        Player player = game.getPlayer(forces.getOwnerId(parentForce));

        // Reassign ground entities to subforces under the parent force
        assignEntitiesToSubforces(groundEntities, parentForce, forces, player, force.getName());

        // If there are aerospace entities, create a new top-level force (Wing)
        if (!aerospaceEntities.isEmpty()) {
            Force wingForce = createWingForce(parentForce.getName(), forces, player);
            assignEntitiesToSubforces(aerospaceEntities, wingForce, forces, player, "Wing");
        }
    }

    private static void splitTopLevelForceEntities(Force force, Forces forces, IGame game) {
        List<ForceAssignable> entities = forces.getFullEntities(force);

        if (entities.isEmpty()) {
            return;
        }

        // Separate entities by unit type
        List<ForceAssignable> groundEntities = new ArrayList<>();
        List<ForceAssignable> aerospaceEntities = new ArrayList<>();

        for (ForceAssignable entity : entities) {
            if (isAerospaceUnit(entity)) {
                aerospaceEntities.add(entity);
            } else {
                groundEntities.add(entity);
            }
        }

        // Remove all entities from the top-level force
        for (ForceAssignable entity : entities) {
            forces.removeEntityFromForces(entity.getId());
        }

        Player player = game.getPlayer(forces.getOwnerId(force));

        // Reassign ground entities to subforces under the original top-level force
        assignEntitiesToSubforces(groundEntities, force, forces, player, force.getName());

        // If there are aerospace entities, create a new top-level force (Wing)
        if (!aerospaceEntities.isEmpty()) {
            Force wingForce = createWingForce(force.getName(), forces, player);
            assignEntitiesToSubforces(aerospaceEntities, wingForce, forces, player, "Wing");
        }
    }


    private static void assignEntitiesToSubforces(List<ForceAssignable> entities, Force parentForce, Forces forces, Player player, String baseName) {
        int entitiesRemaining = entities.size();
        int entityIndex = 0;
        int subforceCount = 1;

        while (entitiesRemaining > 0) {
            // Create a new subforce
            Force newSubForce = new Force(
                baseName + " Subforce " + subforceCount++,
                -1,
                new Camouflage(),
                player
            );

            // Add newSubForce to the parent force
            int subForceId = forces.addSubForce(newSubForce, parentForce);

            // Add up to 6 entities to the new subforce
            int entitiesToAdd = Math.min(6, entitiesRemaining);
            for (int i = 0; i < entitiesToAdd; i++) {
                ForceAssignable entity = entities.get(entityIndex++);
                forces.addEntity(entity, subForceId);
            }

            entitiesRemaining -= entitiesToAdd;
        }
    }


    private static Force createWingForce(String baseName, Forces forces, Player player) {
        // Create a new top-level force for aerospace units
        Force wingForce = new Force(
            baseName + " Wing",
            -1,
            new Camouflage(),
            player
        );

        var wingForceId = forces.addTopLevelForce(wingForce, player);
        return forces.getForce(wingForceId);
    }


    private static boolean isAerospaceUnit(ForceAssignable entity) {
        if (entity instanceof Entity e) {
            return e.isAero() || e.isAerospace() || e.isAerospaceFighter() || e.isSmallCraft();
        }
        return false;
    }

    private static boolean hasMixedUnitTypes(List<ForceAssignable> entities) {
        boolean hasGroundUnits = false;
        boolean hasAerospaceUnits = false;

        for (ForceAssignable entity : entities) {
            if (isAerospaceUnit(entity)) {
                hasAerospaceUnits = true;
            } else {
                hasGroundUnits = true;
            }

            if (hasGroundUnits && hasAerospaceUnits) {
                return true;
            }
        }
        return false;
    }

    private static void redistributeSubforces(Force force, Forces forces, IGame game) {
        List<Force> subForces = new ArrayList<>(forces.getFullSubForces(force));
        Player player = game.getPlayer(forces.getOwnerId(force));

        if (subForces.size() <= 4) {
            // No need to redistribute
            return;
        }

        int totalSubForces = subForces.size();
        int index = 4; // Start redistributing from the 5th subforce

        while (index < totalSubForces) {
            Force subForce = subForces.get(index++);

            // If the subForce contains only aerospace units, attach it to a wing force
            List<ForceAssignable> entities = forces.getFullEntities(subForce);
            if (!entities.isEmpty() && isAerospaceUnit(entities.get(0))) {
                // Create or get the wing force
                Force wingForce = getOrCreateWingForce(force.getName(), forces, player);
                forces.attachForce(subForce, wingForce);
            } else {
                // Create a new top-level force
                Force newTopForce = new Force(
                    force.getName() + " Extra",
                    -1,
                    new Camouflage(),
                    player
                );
                forces.addTopLevelForce(newTopForce, player);
                forces.attachForce(subForce, newTopForce);
            }
        }
    }


    private static Force getOrCreateWingForce(String baseName, Forces forces, Player player) {
        // Check if a wing force already exists
        for (Force topLevelForce : forces.getTopLevelForces()) {
            if (topLevelForce.getName().equals(baseName + " Wing")) {
                return topLevelForce;
            }
        }
        // Create a new wing force
        return createWingForce(baseName, forces, player);
    }


    private static void redistributeEntities(Force force, Forces forces, IGame game) {
        int totalEntities = forces.getFullEntities(force).size();

        if (totalEntities <= 20) {
            // No need to redistribute
            return;
        }

        // Collect all entities from the force's subforces
        List<ForceAssignable> allEntities = new ArrayList<>();
        ArrayList<Force> subForces = forces.getFullSubForces(force);

        for (Force subForce : subForces) {
            allEntities.addAll(forces.getFullEntities(subForce));
            forces.deleteForce(subForce.getId());
        }

        Player player = game.getPlayer(forces.getOwnerId(force));

        // Separate entities by unit type
        List<ForceAssignable> groundEntities = new ArrayList<>();
        List<ForceAssignable> aerospaceEntities = new ArrayList<>();

        for (ForceAssignable entity : allEntities) {
            if (isAerospaceUnit(entity)) {
                aerospaceEntities.add(entity);
            } else {
                groundEntities.add(entity);
            }
        }

        // Reassign ground entities back to the original force
        int entitiesAssigned = 0;
        if (!groundEntities.isEmpty()) {
            entitiesAssigned = assignEntitiesToForce(force, forces, player, groundEntities, 0, Math.min(20, groundEntities.size()));
        }

        // If there are remaining ground entities, create new top-level forces
        int groundEntitiesRemaining = groundEntities.size() - entitiesAssigned;
        int groundEntityIndex = entitiesAssigned;

        while (groundEntitiesRemaining > 0) {
            // Create a new top-level force
            Force newTopForce = new Force(
                force.getName() + " Extra",
                -1,
                new Camouflage(),
                player
            );
            forces.addTopLevelForce(newTopForce, player);

            // Assign up to 20 entities to the new top-level force
            int entitiesToAssign = Math.min(20, groundEntitiesRemaining);
            assignEntitiesToForce(newTopForce, forces, player, groundEntities, groundEntityIndex, entitiesToAssign);
            groundEntityIndex += entitiesToAssign;
            groundEntitiesRemaining -= entitiesToAssign;
        }

        // Handle aerospace entities by creating wing forces
        if (!aerospaceEntities.isEmpty()) {
            Force wingForce = createWingForce(force.getName(), forces, player);
            assignEntitiesToForce(wingForce, forces, player, aerospaceEntities, 0, aerospaceEntities.size());
        }
    }


    private static int assignEntitiesToForce(Force force, Forces forces, Player player, List<ForceAssignable> entities, int startIndex, int numEntities) {
        int entitiesAssigned = 0;
        int entityIndex = startIndex;
        int entitiesRemaining = numEntities;

        while (entitiesRemaining > 0) {
            // Create a new subforce
            Force newSubForce = new Force(
                force.getName() + " Subforce " + (forces.getFullSubForces(force).size() + 1),
                -1,
                new Camouflage(),
                player
            );
            int subForceId = forces.addSubForce(newSubForce, force);

            // Add up to 6 entities to the new subforce
            int entitiesToAdd = Math.min(6, entitiesRemaining);
            for (int i = 0; i < entitiesToAdd; i++) {
                ForceAssignable entity = entities.get(entityIndex++);
                forces.addEntity(entity, subForceId);
                entitiesAssigned++;
            }

            entitiesRemaining -= entitiesToAdd;
        }

        return entitiesAssigned;
    }

}
