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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.bays.ASFBay;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.DockingCollar;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.enums.TransporterType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers putting the units a generated command nests under a ship aboard it.
 *
 * <p>The tree used throughout is the shape the generator produces for a carried fighter complement: a DropShip
 * descriptor with a Flight attached beneath it, and the fighters as the Flight's children.</p>
 */
class ShipTransportAssignerTest {

    private Campaign campaign;
    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        game = new Game();
        game.setPhase(GamePhase.LOUNGE);
    }

    @Test
    void fightersNestedUnderAShipAreAssignedToIt() {
        Unit ship = ship(2);
        Unit firstFighter = fighter();
        Unit secondFighter = fighter();
        ForceDescriptor root = node("Naval Units");
        ForceDescriptor shipNode = node("Leopard");
        ForceDescriptor flight = node("Flight 1");
        ForceDescriptor firstNode = node("Fighter A");
        ForceDescriptor secondNode = node("Fighter B");
        flight.addSubForce(firstNode);
        flight.addAttached(secondNode);
        shipNode.addAttached(flight);
        root.addSubForce(shipNode);
        Map<ForceDescriptor, Unit> units = new IdentityHashMap<>();
        units.put(shipNode, ship);
        units.put(firstNode, firstFighter);
        units.put(secondNode, secondFighter);

        int assigned = ShipTransportAssigner.assign(root, units);

        assertEquals(2, assigned);
        assertSame(ship, firstFighter.getTransportShipAssignment().getTransportShip());
        assertSame(ship, secondFighter.getTransportShipAssignment().getTransportShip());
        assertEquals(TransporterType.ASF_BAY, firstFighter.getTransportShipAssignment().getTransporterType());
        assertEquals(2, ship.getShipTransportedUnits().size());
        assertEquals(0.0, ship.getCurrentShipTransportCapacity(TransporterType.ASF_BAY),
              "Both fighter bays are spoken for");
    }

    @Test
    void aFighterTheShipHasNoRoomForIsLeftUnassigned() {
        Unit ship = ship(1);
        Unit firstFighter = fighter();
        Unit secondFighter = fighter();
        ForceDescriptor shipNode = node("Leopard");
        ForceDescriptor firstNode = node("Fighter A");
        ForceDescriptor secondNode = node("Fighter B");
        shipNode.addAttached(firstNode);
        shipNode.addAttached(secondNode);
        Map<ForceDescriptor, Unit> units = new IdentityHashMap<>();
        units.put(shipNode, ship);
        units.put(firstNode, firstFighter);
        units.put(secondNode, secondFighter);

        int assigned = ShipTransportAssigner.assign(shipNode, units);

        assertEquals(1, assigned);
        assertTrue(firstFighter.hasTransportShipAssignment());
        assertFalse(secondFighter.hasTransportShipAssignment(), "The second fighter found the one bay taken");
    }

    @Test
    void aUnitBesideTheShipRatherThanUnderItIsLeftAlone() {
        Unit ship = ship(2);
        Unit fighter = fighter();
        ForceDescriptor root = node("Naval Units");
        ForceDescriptor shipNode = node("Leopard");
        ForceDescriptor fighterNode = node("Fighter A");
        root.addSubForce(shipNode);
        root.addSubForce(fighterNode);
        Map<ForceDescriptor, Unit> units = new IdentityHashMap<>();
        units.put(shipNode, ship);
        units.put(fighterNode, fighter);

        assertEquals(0, ShipTransportAssigner.assign(root, units));
        assertFalse(fighter.hasTransportShipAssignment());
    }

    @Test
    void aShipTheBuildSkippedCarriesNothing() {
        Unit fighter = fighter();
        ForceDescriptor shipNode = node("Leopard");
        ForceDescriptor fighterNode = node("Fighter A");
        shipNode.addAttached(fighterNode);
        // The ship never became a unit, so it has no entry.
        Map<ForceDescriptor, Unit> units = new IdentityHashMap<>();
        units.put(fighterNode, fighter);

        assertEquals(0, ShipTransportAssigner.assign(shipNode, units));
        assertFalse(fighter.hasTransportShipAssignment());
    }

    @Test
    void dropShipsDockToAJumpShipWithCollarsToSpare() {
        Unit jumpShip = jumpShip(1);
        Unit firstDropShip = ship(2);
        Unit secondDropShip = ship(2);
        ForceDescriptor root = node("Naval Units");
        ForceDescriptor jumpShipNode = node("Invader");
        ForceDescriptor firstNode = node("Leopard");
        ForceDescriptor secondNode = node("Union");
        root.addSubForce(jumpShipNode);
        root.addSubForce(firstNode);
        root.addSubForce(secondNode);
        Map<ForceDescriptor, Unit> units = new IdentityHashMap<>();
        units.put(jumpShipNode, jumpShip);
        units.put(firstNode, firstDropShip);
        units.put(secondNode, secondDropShip);

        int assigned = ShipTransportAssigner.assign(root, units);

        assertEquals(1, assigned, "one collar, one DropShip docked");
        assertSame(jumpShip, firstDropShip.getTransportShipAssignment().getTransportShip());
        assertEquals(TransporterType.DOCKING_COLLAR, firstDropShip.getTransportShipAssignment().getTransporterType());
        assertFalse(secondDropShip.hasTransportShipAssignment(), "no collar left for the second");
    }

    @Test
    void aMissingTreeIsIgnored() {
        assertEquals(0, ShipTransportAssigner.assign(null, new IdentityHashMap<>()));
    }

    private Unit ship(int fighterBays) {
        Dropship dropship = new Dropship();
        dropship.addTransporter(new ASFBay(fighterBays, 1, 1));
        return unitFor(dropship);
    }

    private Unit jumpShip(int collars) {
        Jumpship jumpship = new Jumpship();
        for (int collar = 1; collar <= collars; collar++) {
            jumpship.addTransporter(new DockingCollar(collar));
        }
        return unitFor(jumpship);
    }

    private Unit fighter() {
        return unitFor(new AeroSpaceFighter());
    }

    private Unit unitFor(Entity entity) {
        entity.setId(game.getNextEntityId());
        entity.setGame(game);
        game.addEntity(entity);
        Unit unit = new Unit(entity, campaign);
        unit.setId(UUID.randomUUID());
        unit.initializeShipTransportSpace();
        return unit;
    }

    private static ForceDescriptor node(String name) {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setName(name);
        return descriptor;
    }
}
