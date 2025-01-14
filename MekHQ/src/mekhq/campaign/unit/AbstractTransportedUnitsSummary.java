/**
 * Copyright (c) 2025-2025 The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.unit;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.unit.enums.TransporterType;

import java.util.*;

/**
 * Tracks what units this transport is transporting, and its current capacity for its different transporter types.
 */
public abstract class AbstractTransportedUnitsSummary implements ITransportedUnitsSummary {
    protected final MMLogger logger = MMLogger.create(this.getClass());
    protected Unit transport;
    private Set<Unit> transportedUnits = new HashSet<>();
    private Map<TransporterType, Double> transportCapacity = new HashMap<>();

    AbstractTransportedUnitsSummary(Unit transport) {
        this.transport = transport;
        if (transport.getEntity() != null) {
            recalculateTransportCapacity(transport.getEntity().getTransports());
        }
    }

    /**
     * Main method to be used for unloading units from a transport
     *
     * @param transportedUnits Units we wish to unload
     */
    @Override
    public void unloadTransport(Set<Unit> transportedUnits) {
        for (Unit transportedUnit : transportedUnits) {
            unloadTransport(transportedUnit);
        }
    }

    protected void unloadTransport(Unit transportedUnit) {
        Objects.requireNonNull(transportedUnit);

        // Remove this unit from our collection of transported units.
        removeTransportedUnit(transportedUnit);
    }

    /**
     * Recalculates transport capacity - make sure you pass in all the transporters
     * of a given type (class), or just all the transporters an entity has.
     * @param transporters What transporters are we recalculating?
     * @see Entity#getTransports()
     */
    @Override
    public void recalculateTransportCapacity(@Nullable Vector<Transporter> transporters) {
        if (transporters != null && !transporters.isEmpty()) {
            // First let's clear the transport capacity for each transport type in transporters
            for (Transporter transporter : transporters) {
                TransporterType transporterType = TransporterType.getTransporterType(transporter);
                if (hasTransportCapacity(transporterType)) {
                    transportCapacity.remove(transporterType);
                }
            }
            // Then we make sure the transport entity is empty, and then let's load
            // our transport entity so we can use the Transporter's getUnused method
            clearTransportedEntities();
            loadTransportedEntities();

            // Now we can update our transport capacities using the unused space of each transporter

            for (Transporter transporter : transporters) {
                TransporterType transporterType = TransporterType.getTransporterType(transporter);
                if (transportCapacity.containsKey(transporterType)) {
                    transportCapacity.replace(transporterType, transportCapacity.get(transporterType) + transporter.getUnused());
                } else {
                    transportCapacity.put(transporterType, transporter.getUnused());
                }
            }
        // Finally clear the transport entity again
        clearTransportedEntities();
        }
    }

    /**
     * If this unit is capable of transporting another unit, return true
     *
     * @return true if the unit can transport another unit
     */
    @Override
    public boolean hasTransportCapacity() {
        return !transportCapacity.isEmpty();
    }

    /**
     * Gets the different kinds of transporters the transport has
     *
     * @return Set of Transporter classes
     */
    @Override
    public Set<TransporterType> getTransportCapabilities() {
        return transportCapacity.keySet();
    }

    /**
     * Returns true if the unit has capacity left for a transporter type
     *
     * @param transporterType Does the unit have free capacity in this type?
     * @return True if the unit has capacity, false if not
     */
    @Override
    public boolean hasTransportCapacity(TransporterType transporterType) {
        return transportCapacity.containsKey(transporterType);
    }

    /**
     * Returns the current capacity of a transporter type
     *
     * @param transporterType What kind of transporter types are we checking?
     * @return The current capacity of the transporter, or 0
     */
    @Override
    public double getCurrentTransportCapacity(TransporterType transporterType) {
        return transportCapacity.getOrDefault(transporterType, 0.0);
    }

    /**
     * Sets the current transport capacity for the provided transport type
     *
     * @param transporterType What kind of transporter are we changing the capacity of?
     * @param capacity        What is the new capacity?
     */
    @Override
    public void setCurrentTransportCapacity(TransporterType transporterType, double capacity) {
        transportCapacity.replace(transporterType, capacity);
    }

    /**
     * Gets a value indicating whether or not this unit is
     * transporting units.
     *
     * @return true if the unit has any transported units
     */
    @Override
    public boolean hasTransportedUnits() {
        return !transportedUnits.isEmpty();
    }

    /**
     * @return the set of units being transported by this unit.
     */
    @Override
    public Set<Unit> getTransportedUnits() {
        return Collections.unmodifiableSet(transportedUnits);
    }

    /**
     * Adds a unit to our set of transported units.
     *
     * @param unit The unit being transported by this instance.
     */
    @Override
    public void addTransportedUnit(Unit unit) {
        transportedUnits.add(Objects.requireNonNull(unit));
    }

    /**
     * Removes a unit from our set of transported units.
     *
     * @param unit The unit to remove from our set of transported units.
     * @return True if the unit was removed, otherwise false.
     */
    @Override
    public boolean removeTransportedUnit(Unit unit) {
        return transportedUnits.remove(unit);
    }

    /**
     * Clears the set of units being transported by this unit.
     */
    @Override
    public void clearTransportedUnits() {
        if (!transportedUnits.isEmpty()) {
            transportedUnits.clear();
        }

        clearTransportedEntities();
    }

    protected Set<Entity> clearTransportedEntities() {
        Set<Entity> transportedEntities = new HashSet<>();
        if (transport.getEntity() != null) {
            if (transport.getEntity().hasUnloadedUnitsFromBays()) {
                for (Entity transportedEntity : transport.getEntity().getUnitsUnloadableFromBays()) {
                    transport.getEntity().unload(transportedEntity);
                    transportedEntities.add(transportedEntity);
                }
            } // We can't just use Entity::getUnloadableUnits(); getUnloadableFromBays() throws NPE in that flow
            for (Entity transportedEntity : transport.getEntity().getUnitsUnloadableFromNonBays()) {
                transport.getEntity().unload(transportedEntity);
                transportedEntities.add(transportedEntity);
            }

                transport.getEntity().resetTransporter();
        }
        return transportedEntities;
    }

    protected void loadTransportedEntities() {
        if (transport.getEntity() != null) {
            for (Unit transportedUnit : getTransportedUnits()) {
                if (transportedUnit.getEntity() != null) {
                    transport.getEntity().resetBays();
                    loadEntity(transportedUnit.getEntity());
                }
            }
        }
    }

    protected void loadEntity(Entity transportedEntity) {
        if (transport.getEntity() != null && transportedEntity != null) {
            if (transport.getEntity().canLoad(transportedEntity, false)) {
                transport.getEntity().load(transportedEntity, false);
            }
            else {
                logger.error(String.format("Could not load entity %s onto unit %s", transportedEntity.getDisplayName(), transport.getName()));
            }
        }
    }

    /**
     * When fixing references we need to replace the transported units
     * @param newTransportedUnits The units that should be transported
     */
    @Override
    public void replaceTransportedUnits(Set<Unit> newTransportedUnits) {
        clearTransportedUnits();
        for (Unit newUnit : newTransportedUnits) {
            addTransportedUnit(newUnit);
        }
    }
}
