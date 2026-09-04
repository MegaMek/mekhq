/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.List;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link CommandGenerator#collectEntities(ForceDescriptor)} - the OpFor-side harvest of a
 * rolled command: included leaves' entities in tree order, excluded leaves skipped, entity-less
 * leaves ignored.
 */
class CommandGeneratorCollectEntitiesTest {

    /** {@code ForceDescriptor.entity} has no setter (set only during generation), so inject via reflection. */
    private static Entity injectEntity(ForceDescriptor descriptor) throws Exception {
        Field field = ForceDescriptor.class.getDeclaredField("entity");
        field.setAccessible(true);
        Entity entity = mock(Entity.class);
        field.set(descriptor, entity);
        return entity;
    }

    private static ForceDescriptor group(String name) {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setName(name);
        return descriptor;
    }

    @Test
    void collectEntities_returnsIncludedLeavesInTreeOrder() throws Exception {
        ForceDescriptor root = group("Battalion");
        ForceDescriptor companyOne = group("First Company");
        ForceDescriptor companyTwo = group("Second Company");
        root.addSubForce(companyOne);
        root.addSubForce(companyTwo);

        ForceDescriptor leafOne = group("Unit 1");
        Entity entityOne = injectEntity(leafOne);
        ForceDescriptor leafTwo = group("Unit 2");
        Entity entityTwo = injectEntity(leafTwo);
        companyOne.addSubForce(leafOne);
        companyOne.addSubForce(leafTwo);

        ForceDescriptor leafThree = group("Unit 3");
        Entity entityThree = injectEntity(leafThree);
        companyTwo.addSubForce(leafThree);

        // Attached forces are harvested after sub-forces at each node.
        ForceDescriptor attachedLeaf = group("Attached Unit");
        Entity attachedEntity = injectEntity(attachedLeaf);
        root.getAttached().add(attachedLeaf);

        List<Entity> collected = CommandGenerator.collectEntities(root);

        assertEquals(4, collected.size());
        assertSame(entityOne, collected.get(0));
        assertSame(entityTwo, collected.get(1));
        assertSame(entityThree, collected.get(2));
        assertSame(attachedEntity, collected.get(3));
    }

    @Test
    void collectEntities_skipsExcludedLeaves() throws Exception {
        ForceDescriptor root = group("Company");
        ForceDescriptor keptLeaf = group("Kept");
        Entity keptEntity = injectEntity(keptLeaf);
        ForceDescriptor excludedLeaf = group("Excluded");
        injectEntity(excludedLeaf);
        excludedLeaf.setIncluded(false);
        root.addSubForce(keptLeaf);
        root.addSubForce(excludedLeaf);

        List<Entity> collected = CommandGenerator.collectEntities(root);

        assertEquals(1, collected.size());
        assertSame(keptEntity, collected.get(0));
    }

    @Test
    void collectEntities_ignoresEntitylessLeavesAndEmptyTrees() throws Exception {
        // A leaf without an entity (engine failed to load one) contributes nothing.
        ForceDescriptor root = group("Company");
        root.addSubForce(group("No Entity Leaf"));

        assertTrue(CommandGenerator.collectEntities(root).isEmpty());

        // A bare leaf root with an entity harvests itself.
        ForceDescriptor loneLeaf = group("Lone Unit");
        Entity loneEntity = injectEntity(loneLeaf);
        List<Entity> collected = CommandGenerator.collectEntities(loneLeaf);
        assertEquals(1, collected.size());
        assertSame(loneEntity, collected.get(0));
    }

    @Test
    void collectEntities_includesAShipWithFightersNestedUnderIt() throws Exception {
        // A carrier is generated with its fighter complement beneath it; it is still a unit to harvest.
        ForceDescriptor root = group("Naval Units");
        ForceDescriptor ship = group("Leopard");
        Entity shipEntity = injectEntity(ship);
        ForceDescriptor flight = group("Flight 1");
        ForceDescriptor fighter = group("Fighter A");
        Entity fighterEntity = injectEntity(fighter);
        flight.addSubForce(fighter);
        ship.addAttached(flight);
        root.addSubForce(ship);

        assertEquals(List.of(shipEntity, fighterEntity), CommandGenerator.collectEntities(root));
    }
}
