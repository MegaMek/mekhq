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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.Transporter;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.enums.TransporterType;
import mekhq.campaign.utilities.CampaignTransportUtilities;

/**
 * Puts the units a generated command nests under a ship aboard that ship.
 *
 * <p>Generation nests a carrier's fighter complement under the carrier, and the command builder materializes
 * both the ship and the fighters as campaign units. Without this stage they arrive as neighbours in the TO&amp;E:
 * the fighters have no transport and are left behind when the ship jumps out. This walks the rolled tree once
 * every unit exists and assigns each nested unit to the nearest ship above it as ship transport, the same
 * assignment the TO&amp;E's "Assign to ship" action makes, so the fighters show aboard and the scenario launcher
 * loads them in game.</p>
 *
 * <p>What the tree nests under a ship goes aboard, and a DropShip with no ship of its own is docked to a JumpShip
 * or WarShip in the command with a collar free. The troopships generated to lift the command are sized to carry
 * it, but the tree never says which Mek rides in which hull; assigning those stays with the player.</p>
 */
public final class ShipTransportAssigner {

    private static final MMLogger LOGGER = MMLogger.create(ShipTransportAssigner.class);

    private ShipTransportAssigner() {
    }

    /**
     * Assigns every unit nested under a ship to that ship as ship transport.
     *
     * @param root              the rolled command; {@code null} is ignored
     * @param unitsByDescriptor the campaign unit built for each unit descriptor, keyed by descriptor identity; a
     *                          descriptor with no entry (skipped or excluded in the build) is left alone
     *
     * @return how many units were put aboard a ship
     */
    public static int assign(@Nullable ForceDescriptor root, Map<ForceDescriptor, Unit> unitsByDescriptor) {
        if (root == null) {
            return 0;
        }
        Set<Unit> shipsChanged = new LinkedHashSet<>();
        int assigned = assignBeneath(root, null, unitsByDescriptor, shipsChanged);
        assigned += dock(root, unitsByDescriptor, shipsChanged);
        for (Unit ship : shipsChanged) {
            MekHQ.triggerEvent(new UnitChangedEvent(ship));
        }
        if (assigned > 0) {
            LOGGER.info("[CompanyGen][ShipTransport] {} unit(s) assigned aboard {} ship(s)", assigned,
                  shipsChanged.size());
        } else {
            LOGGER.info("[CompanyGen][ShipTransport] nothing is nested under a built ship; no transport assigned");
        }
        return assigned;
    }

    /**
     * Walks the subtree, boarding each built unit on the nearest built ship above it.
     *
     * @param node              the node to walk
     * @param ship              the nearest ship above this node that was built, or {@code null} when there is none
     * @param unitsByDescriptor see {@link #assign(ForceDescriptor, Map)}
     * @param shipsChanged      receives every ship that took a unit
     *
     * @return how many units beneath (and including) this node boarded a ship
     */
    private static int assignBeneath(ForceDescriptor node, @Nullable Unit ship,
          Map<ForceDescriptor, Unit> unitsByDescriptor, Set<Unit> shipsChanged) {
        Unit shipForChildren = ship;
        int assigned = 0;

        Unit unit = unitsByDescriptor.get(node);
        if (unit != null) {
            if ((ship != null) && board(unit, ship)) {
                shipsChanged.add(ship);
                assigned++;
            }
            // A unit with transporters of its own takes what the tree nests under it, whether or not it is
            // itself being carried.
            if (hasTransporters(unit)) {
                shipForChildren = unit;
            }
        }

        for (ForceDescriptor child : node.getSubForces()) {
            assigned += assignBeneath(child, shipForChildren, unitsByDescriptor, shipsChanged);
        }
        for (ForceDescriptor child : node.getAttached()) {
            assigned += assignBeneath(child, shipForChildren, unitsByDescriptor, shipsChanged);
        }
        return assigned;
    }

    /**
     * Docks every DropShip still without a ship to a JumpShip or WarShip in the command with a collar to spare, in
     * tree order. The tree lists JumpShips and DropShips as separate categories rather than nesting one under the
     * other, so this is where the collars the transport stage counted are taken.
     *
     * @return how many DropShips were docked
     */
    private static int dock(ForceDescriptor root, Map<ForceDescriptor, Unit> unitsByDescriptor,
          Set<Unit> shipsChanged) {
        List<Unit> units = new ArrayList<>();
        collectUnits(root, unitsByDescriptor, units);
        List<Unit> collarShips = units.stream()
              .filter(unit -> (unit.getEntity() != null) && !unit.getEntity().getDockingCollars().isEmpty())
              .toList();
        if (collarShips.isEmpty()) {
            return 0;
        }
        int docked = 0;
        for (Unit unit : units) {
            boolean isUndockedDropship = (unit.getEntity() instanceof Dropship) && !unit.hasTransportShipAssignment();
            if (!isUndockedDropship) {
                continue;
            }
            for (Unit ship : collarShips) {
                if (ship.getCurrentShipTransportCapacity(TransporterType.DOCKING_COLLAR) >= 1) {
                    ship.loadShipTransport(TransporterType.DOCKING_COLLAR, Set.of(unit));
                    shipsChanged.add(ship);
                    docked++;
                    break;
                }
            }
        }
        return docked;
    }

    /** The built units under {@code node}, in tree order. */
    private static void collectUnits(ForceDescriptor node, Map<ForceDescriptor, Unit> unitsByDescriptor,
          List<Unit> into) {
        Unit unit = unitsByDescriptor.get(node);
        if (unit != null) {
            into.add(unit);
        }
        for (ForceDescriptor child : node.getSubForces()) {
            collectUnits(child, unitsByDescriptor, into);
        }
        for (ForceDescriptor child : node.getAttached()) {
            collectUnits(child, unitsByDescriptor, into);
        }
    }

    private static boolean hasTransporters(Unit unit) {
        Entity entity = unit.getEntity();
        return (entity != null) && !entity.getTransports().isEmpty();
    }

    /**
     * @return {@code true} when the unit was assigned to the ship
     */
    private static boolean board(Unit cargo, Unit ship) {
        Entity cargoEntity = cargo.getEntity();
        if (cargoEntity == null) {
            return false;
        }
        TransporterType transporterType = transporterWithRoomFor(cargoEntity, ship);
        if (transporterType == null) {
            LOGGER.debug("[CompanyGen][ShipTransport] '{}' has no bay with room for '{}'; it is not assigned",
                  ship.getName(), cargo.getName());
            return false;
        }
        ship.loadShipTransport(transporterType, Set.of(cargo));
        return true;
    }

    /**
     * Finds the kind of transporter aboard the ship that can take the unit and still has room for it, judged
     * by the campaign's running tally of the ship's capacity so units assigned earlier in the walk count.
     *
     * @return the transporter type to assign the unit to, or {@code null} when nothing aboard can take it
     */
    private static @Nullable TransporterType transporterWithRoomFor(Entity cargo, Unit ship) {
        Entity shipEntity = ship.getEntity();
        if (shipEntity == null) {
            return null;
        }
        for (Transporter transporter : shipEntity.getTransports()) {
            if (!transporter.canLoad(cargo)) {
                continue;
            }
            TransporterType transporterType = TransporterType.getTransporterType(transporter);
            if (transporterType == null) {
                continue;
            }
            double spaceNeeded = CampaignTransportUtilities.transportCapacityUsage(transporterType, cargo);
            if (ship.getCurrentShipTransportCapacity(transporterType) >= spaceNeeded) {
                return transporterType;
            }
        }
        return null;
    }
}
