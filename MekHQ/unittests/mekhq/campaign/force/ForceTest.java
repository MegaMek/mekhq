/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 */
package mekhq.campaign.force;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForceTest {
    @Test
    void testGetAllUnits_ParentForceStandard_NoChildForces() {
        // Arrange
        Force force = new Force("Test Force");
        UUID unit1 = UUID.randomUUID();
        UUID unit2 = UUID.randomUUID();
        force.addUnit(unit1);
        force.addUnit(unit2);

        // Act
        Vector<UUID> allUnits = force.getAllUnits(true);

        // Assert
        assertEquals(2, allUnits.size());
    }

    @Test
    void testGetAllUnits_ParentForceStandard_ChildForcesAlsoStandard() {
        // Arrange
        Force force = new Force("Parent Force");
        force.setForceType(ForceType.STANDARD, true);
        UUID unit = UUID.randomUUID();
        force.addUnit(unit);

        Force childForce = new Force("Child Force");
        unit = UUID.randomUUID();
        childForce.addUnit(unit);
        force.addSubForce(childForce, true);

        Force childForce2 = new Force("Child Force (layer 2)");
        unit = UUID.randomUUID();
        childForce2.addUnit(unit);
        childForce.addSubForce(childForce2, true);

        // Act
        Vector<UUID> allUnits = force.getAllUnits(true);

        // Assert
        assertEquals(3, allUnits.size());
    }

    @Test
    void testGetAllUnits_ParentForceStandard_ChildForcesNotStandard() {
        // Arrange
        Force force = new Force("Parent Force");
        force.setForceType(ForceType.STANDARD, true);
        UUID unit = UUID.randomUUID();
        force.addUnit(unit);

        Force childForce = new Force("Child Force");
        unit = UUID.randomUUID();
        childForce.addUnit(unit);
        force.addSubForce(childForce, true);

        Force childForce2 = new Force("Child Force (layer 2)");
        unit = UUID.randomUUID();
        childForce2.addUnit(unit);
        childForce.addSubForce(childForce2, true);

        childForce.setForceType(ForceType.CONVOY, true);

        // Act
        Vector<UUID> allUnits = force.getAllUnits(true);

        // Assert
        assertEquals(1, allUnits.size());
    }

    @Test
    void testGetAllUnits_AllForcesNotStandard() {
        // Arrange
        Force force = new Force("Parent Force");
        UUID unit = UUID.randomUUID();
        force.addUnit(unit);

        Force childForce = new Force("Child Force");
        unit = UUID.randomUUID();
        childForce.addUnit(unit);
        force.addSubForce(childForce, true);

        Force childForce2 = new Force("Child Force (layer 2)");
        unit = UUID.randomUUID();
        childForce2.addUnit(unit);
        childForce.addSubForce(childForce2, true);

        force.setForceType(ForceType.SECURITY, true);

        // Act
        Vector<UUID> allUnits = force.getAllUnits(true);

        // Assert
        assertEquals(0, allUnits.size());
    }

    @Test
    void testGetAllUnits_AllForcesNotStandard_NoStandardFilter() {
        // Arrange
        Force force = new Force("Parent Force");
        UUID unit = UUID.randomUUID();
        force.addUnit(unit);

        Force childForce = new Force("Child Force");
        unit = UUID.randomUUID();
        childForce.addUnit(unit);
        force.addSubForce(childForce, true);

        Force childForce2 = new Force("Child Force (layer 2)");
        unit = UUID.randomUUID();
        childForce2.addUnit(unit);
        childForce.addSubForce(childForce2, true);

        force.setForceType(ForceType.SECURITY, true);

        // Act
        Vector<UUID> allUnits = force.getAllUnits(false);

        // Assert
        assertEquals(3, allUnits.size());
    }

    @Test
    void testGetAllUnits_AllForcesStandard_SecondLayerEmpty() {
        // Arrange
        Force force = new Force("Parent Force");
        force.setForceType(ForceType.STANDARD, true);
        UUID unit = UUID.randomUUID();
        force.addUnit(unit);

        Force childForce = new Force("Child Force");
        unit = UUID.randomUUID();
        childForce.addUnit(unit);
        force.addSubForce(childForce, true);

        Force childForce2 = new Force("Child Force (layer 2)");
        childForce.addSubForce(childForce2, true);

        Force childForce3 = new Force("Child Force (layer 3)");
        unit = UUID.randomUUID();
        childForce3.addUnit(unit);
        childForce.addSubForce(childForce3, true);

        // Act
        Vector<UUID> allUnits = force.getAllUnits(true);

        // Assert
        assertEquals(3, allUnits.size());
    }
}
