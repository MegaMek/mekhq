/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package mekhq.campaign.unit;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

import megamek.common.Entity;
import megamek.common.Transporter;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.enums.TransporterType;

/**
 * Tracks what units this transport is transporting, and its current capacity for its different transporter types.
 */
public abstract class AbstractTransportedUnitsSummary implements ITransportedUnitsSummary {
    private static final MMLogger logger = MMLogger.create(AbstractTransportedUnitsSummary.class);
    protected Unit transport;
    private Set<Unit> transportedUnits = new HashSet<>();
    private Map<TransporterType, Double> transportCapacity = new HashMap<>();
    private CampaignTransportType campaignTransportType;

    AbstractTransportedUnitsSummary(Unit transport, CampaignTransportType campaignTransportType) {
        this.transport = transport;
        this.campaignTransportType = campaignTransportType;
        init();
    }

    protected void init() {
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
     * Recalculates transport capacity - make sure you pass in all the transporters of a given type (class), or just all
     * the transporters an entity has.
     *
     * @param transporters What transporters are we recalculating?
     *
     * @see Entity#getTransports()
     */
    @Override
    public void recalculateTransportCapacity(@Nullable Vector<Transporter> transporters) {
        if (transporters == null || transporters.isEmpty()) {
            return;
        }

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
                transportCapacity.replace(transporterType,
                      transportCapacity.get(transporterType) + transporter.getUnused());
            } else {
                transportCapacity.put(transporterType, transporter.getUnused());
            }
        }

        // Finally clear the transport entity again
        clearTransportedEntities();
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
     *
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
     *
     * @return The current capacity of the transporter, or 0
     */
    @Override
    public double getCurrentTransportCapacity(@Nullable TransporterType transporterType) {
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
        transportCapacity.put(transporterType, capacity);
    }

    /**
     * Gets a value indicating whether or not this unit is transporting units.
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
     *
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

    /**
     * Completely clears the capacity map. Helpful if the transportCapacity has a TransporterType for a Transporter the
     * unit no longer has - such as after a refit.
     */
    public void clearTransportCapacityMap() {
        transportCapacity = new HashMap<>();
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
        Entity transportEntity = transport.getEntity();
        if (transportEntity != null) {
            for (Unit transportedUnit : getTransportedUnits()) {
                loadTransportedEntity(transportedUnit, transportEntity);
            }
        }
    }

    private void loadTransportedEntity(Unit transportedUnit, Entity transportEntity) {
        if (transportedUnit.getEntity() != null) {
            transportEntity.resetBays();
            if ((transportedUnit.hasTransportAssignment(campaignTransportType))
                      && (transportedUnit.getTransportAssignment(campaignTransportType).hasTransporterType())) {
                loadEntityInTransporter(transportedUnit, transportEntity.getTransports());
            } else {
                loadEntity(transportedUnit.getEntity());
            }
        }
    }

    private void loadEntityInTransporter(Unit transportedUnit, Vector<Transporter> transporters) {
        TransporterType transporterType =
              transportedUnit.getTransportAssignment(campaignTransportType).getTransporterType();
        for (Transporter transporter : transporters) {
            if (transporterType.getTransporterClass() == transporter.getClass() && transporter.canLoad(
                  transportedUnit.getEntity())) {
                transporter.load(transportedUnit.getEntity());
                return;
            }
        }
        loadEntity(transportedUnit.getEntity());
    }

    protected void loadEntity(Entity transportedEntity) {
        if (transport.getEntity() != null && transportedEntity != null) {
            if (transport.getEntity().canLoad(transportedEntity, false)) {
                transport.getEntity().load(transportedEntity, false);
            } else {
                logger.error(String.format("Could not load entity %s onto unit %s",
                      transportedEntity.getDisplayName(),
                      transport.getName()));
            }
        }
    }

    /**
     * When fixing references we need to replace the transported units
     *
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
