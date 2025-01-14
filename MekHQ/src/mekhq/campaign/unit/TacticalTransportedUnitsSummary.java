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
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.enums.TransporterType;
import mekhq.campaign.utilities.CampaignTransportUtilities;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Tracks what units this transport is transporting, and its current capacity for its different transporter types.
 * @see AbstractTransportedUnitsSummary
 * @see CampaignTransportType#TACTICAL_TRANSPORT
 *
 */
public class TacticalTransportedUnitsSummary extends AbstractTransportedUnitsSummary {

    public TacticalTransportedUnitsSummary(Unit transport) {
        super(transport);
    }

    /**
     * Main method to be used for unloading units from a transport
     *
     * @param transportedUnits Units we wish to unload
     */
    @Override
    public void unloadTransport(Set<Unit>  transportedUnits) {
        super.unloadTransport(transportedUnits);

    }

    @Override
    protected void unloadTransport(Unit transportedUnit) {
        super.unloadTransport(transportedUnit);

        // And if the unit is being transported by us,
        // then update its transport  assignment (provided the
        // assignment is actually to us!).
        if (transportedUnit.hasTacticalTransportAssignment()
            && transportedUnit.getTacticalTransportAssignment().getTransport().equals(transport)) {
            transportedUnit.setTacticalTransportAssignment(null);
        }
    }

    /**
     * Transporter loading utility used when assigning units to transport units
     * For each passed-in unit, this will assign the unit to the type of
     * Transporter if one isn't provided. Once in the MM lobby,
     * will be used to actually load the unit into a bay on the transport.
     *
     * @param transporterType type (Class) of bay or Transporter
     * @param units units being loaded
     * @return old transports; what were  the units' previous transport, if they had one
     */
    public Set<Unit> loadTransport(TransporterType transporterType, Set<Unit> units) {
        return loadTransport(units, null, transporterType);
    }


    /**
     * Transporter loading utility used when assigning units to transport units
     * For each passed-in unit, this will assign the unit to the specified bay,
     * or the type of Transporter if one isn't provided. Once in the MM lobby,
     * will be used to actually load the unit into a bay on the transport.
     *
     * @param units units being loaded
     * @param transportedLocation specific bay (Transporter), or null
     * @param transporterType type (Class) of bay or Transporter
     * @return old transports; what were  the units' previous transport, if they had one
     */
    public Set<Unit> loadTransport(Set<Unit> units, @Nullable Transporter transportedLocation, TransporterType transporterType) {
        Set<Unit> oldTransports = new HashSet<>();
        //Set<Entity> oldTransportedEntities = clearTransportedEntities();
        for (Unit transportedUnit : units) {
            Unit oldTransport = loadTransport(transportedLocation, transporterType, transportedUnit);
            if (oldTransport != null) {
                oldTransports.add(oldTransport);
            }
        }
        transport.initializeTacticalTransportSpace();
        return oldTransports;
    }


    /**
     * Transporter loading utility used when assigning units to transport units
     * For the passed in unit, this will assign the unit to the specified bay,
     * or the type of Transporter if one isn't provided. Once in the MM lobby,
     * will be used to actually load the unit into a bay on the transport.
     *
     * @param transportedLocation specific bay, or null
     * @param transporterType type (Class) of bay or Transporter
     * @param transportedUnit unit being loaded
     * @return old transport; what was the unit's previous transport, if it had one
     */
    public Unit loadTransport(@Nullable Transporter transportedLocation, TransporterType transporterType, Unit transportedUnit) {
        Unit oldTransport = null;
        TransporterType oldTransporterType = null;

        if (transportedUnit.hasTacticalTransportAssignment()) {
            oldTransport = transportedUnit.getTacticalTransportAssignment().getTransport();
            oldTransporterType = transportedUnit.getTacticalTransportAssignment().getTransporterType();
            if (oldTransport.getEntity() != null) {
                oldTransport.unloadTacticalTransport(transportedUnit);
            }
        }
        if (transportedLocation != null) {
            transportedUnit.setTacticalTransportAssignment(new TransportAssignment(transport, transportedLocation));
        }
        else if (transporterType != null){
            transportedUnit.setTacticalTransportAssignment(new TransportAssignment(transport, transporterType));
        } else {
            logger.error(String.format("Cannot load transport (%s) with unit (%s) without a transported location or transporter!", transport.getId(), transportedUnit.getId()));
            return oldTransport;
        }
        addTransportedUnit(Objects.requireNonNull(transportedUnit));

        // Update Transport Capacities
        if (!Objects.equals(oldTransport, transport)
            && (transportedUnit.getTacticalTransportAssignment().getTransporterType() != oldTransporterType)) {
            setCurrentTransportCapacity(transporterType,
                getCurrentTransportCapacity(transporterType) - CampaignTransportUtilities.transportCapacityUsage(transporterType,transportedUnit.getEntity()));
        }
        return oldTransport;
    }

    /**
     * Bay unloading utility used when removing units from bay-equipped transport
     * units
     * and/or moving them to a new transport
     *
     * @param transportedUnit The unit that we wish to unload from this transport
     */
    public void unloadFromTransport(Unit transportedUnit) {
        unloadTransport(transportedUnit);
    }

    /**
     * Bay unloading utility used when removing a bay-equipped Transport unit
     * This removes all units assigned to the transport from it
     *
     * @param campaign used to remove this unit as a transport from any other units in the campaign
     */
    @Override
    public void clearTransportedUnits(Campaign campaign) {
        clearTransportedUnits();

        // And now reset the Transported values for all the units we just booted
        campaign.getHangar().forEachUnit(u -> {
            if (u.hasTacticalTransportAssignment()
                && Objects.equals(transport, u.getTacticalTransportAssignment().getTransport())) {
                u.setTacticalTransportAssignment(null);
            }
        });

        recalculateTransportCapacity(transport.getEntity().getTransports());
    }

    /**
     * Fixes references after loading
     */
    @Override
    public void fixReferences(Campaign campaign, Unit unit) {
        Set<Unit> oldTransportedUnits = new HashSet<>(getTransportedUnits());
        clearTransportedUnits();
        for (Unit tacticalTransportedUnit : oldTransportedUnits) {
            if (tacticalTransportedUnit instanceof Unit.UnitRef) {
                Unit realUnit = campaign.getHangar().getUnit(tacticalTransportedUnit.getId());
                if (realUnit != null) {
                    if (realUnit.hasTacticalTransportAssignment()) {
                        loadTransport(realUnit.getTacticalTransportAssignment().getTransporterType(), new HashSet<>(Collections.singleton(realUnit)));
                    } else {
                        logger.error(
                            String.format("Unit %s ('%s') references tactical transported unit %s which has no transport assignment",
                                unit.getId(), unit.getName(), tacticalTransportedUnit.getId()));
                    }
                } else {
                    logger.error(
                        String.format("Unit %s ('%s') references missing tactical transported unit %s",
                            unit.getId(), unit.getName(), tacticalTransportedUnit.getId()));
                }
            } else {
                loadTransport(tacticalTransportedUnit.getTacticalTransportAssignment().getTransporterType(), new HashSet<>(Collections.singleton(tacticalTransportedUnit)));
            }
        }
    }
}
