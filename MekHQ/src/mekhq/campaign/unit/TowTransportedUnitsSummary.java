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

import megamek.common.Tank;
import megamek.common.TankTrailerHitch;
import megamek.common.Transporter;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.enums.TransporterType;

import java.util.*;
import java.util.stream.Collectors;

import static mekhq.campaign.enums.CampaignTransportType.TOW_TRANSPORT;

public class TowTransportedUnitsSummary extends AbstractTransportedUnitsSummary{
    private static final MMLogger logger = MMLogger.create(TowTransportedUnitsSummary.class);

    public TowTransportedUnitsSummary(Unit transport) {
        super(transport);
    }

    @Override
    protected void init() {
        if (transport.getEntity() != null && transport.getEntity() instanceof Tank tractor && !transport.getEntity().isTrailer()) {
            recalculateTransportCapacity(tractor);
        }
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
            if (u.hasTransportShipAssignment()
                && Objects.equals(transport, u.getTransportAssignment(TOW_TRANSPORT).getTransport())) {
                u.setTransportAssignment(TOW_TRANSPORT, null);
            }
        });

        recalculateTransportCapacity(transport.getEntity().getTransports());
    }

    @Override
    public void recalculateTransportCapacity(Vector<Transporter> transporters) {
        Set<Transporter> trailerHitches = transporters.stream().filter(t -> t instanceof TankTrailerHitch).collect(Collectors.toSet());
        if (trailerHitches.isEmpty()) {
            return;
        }
        Unit tractor = getTractor();

        // Can't finish the calculations if the tractor isn't initialized yet, let's get out of here.
        if (tractor == null || tractor.getEntity() == null) {
            return;
        }

        Vector<Unit> otherTrailers = new Vector<>();
        if (tractor.hasTransportedUnits(TOW_TRANSPORT)) {
            Unit towingUnit = tractor.getTransportedUnits(TOW_TRANSPORT).stream().findAny().orElse(null);
            otherTrailers.add(towingUnit);
            while (towingUnit != null && towingUnit.hasTransportedUnits(TOW_TRANSPORT)) {
                towingUnit = towingUnit.getTransportedUnits(TOW_TRANSPORT).iterator().next();
                otherTrailers.add(towingUnit);
            }
        }

        // Finally calculate our towing capacity
        if (tractor.equals(transport)) {
            setCurrentTransportCapacity(TransporterType.TANK_TRAILER_HITCH, tractor.getEntity().getWeight()
                - otherTrailers.stream().filter(u -> u.getEntity() != null).mapToDouble(u -> u.getEntity().getWeight()).sum());
        } else {
            setCurrentTransportCapacity(TransporterType.TANK_TRAILER_HITCH, 0.0);
        }
    }

    @Override
    protected void unloadTransport(Unit transportedUnit) {
        super.unloadTransport(transportedUnit);

        if ((transportedUnit.hasTransportAssignment(TOW_TRANSPORT)
                && transportedUnit.getTransportAssignment(TOW_TRANSPORT).getTransport().equals(transport))) {
            transportedUnit.setTransportAssignment(TOW_TRANSPORT, null);
        }

        if (transportedUnit.getEntity() != null && transport.getEntity() != null) {
            Unit tractor = getTractor();
            if (tractor != null && tractor.hasTransportedUnits(TOW_TRANSPORT) &&  tractor.getEntity() != null && tractor.getEntity() instanceof Tank tank) {
                TowTransportedUnitsSummary tractorTransportedUnitsSummary = (TowTransportedUnitsSummary) tractor.getTransportedUnitsSummary(TOW_TRANSPORT);
                tractorTransportedUnitsSummary.recalculateTransportCapacity(tank);
            }
        }
    }

    /**
     * Find the tractor pulling this whole train
     *
     * @return the tractor pulling the entire train of trailers
     */
    private Unit getTractor() {
        Unit tractor;
        if (transport.getEntity() instanceof Tank tank) {
            if (tank.isTrailer()) {
                if (transport.hasTransportAssignment(TOW_TRANSPORT)) {
                    tractor = transport.getTransportAssignment(TOW_TRANSPORT).getTransport();
                    while (tractor.hasTransportAssignment(TOW_TRANSPORT)) {
                        tractor = tractor.getTransportAssignment(TOW_TRANSPORT).getTransport();
                    }
                } else {
                    // No tractor, no towing!
                    setCurrentTransportCapacity(TransporterType.TANK_TRAILER_HITCH, Double.MIN_VALUE);
                    return null;
                }
            } else {
                // If we aren't a trailer then we're the tractor
                tractor = transport;
            }
        } else {
            // Only tanks can tow, let's just assume the tractor is this transport
            tractor = transport;
        }
        return tractor;
    }

    /**
     * Fixes references after loading
     *
     * @param campaign
     * @param unit
     */
    @Override
    public void fixReferences(Campaign campaign, Unit unit) {

        Set<Unit> oldTransportedUnits = new HashSet<>(getTransportedUnits());
        clearTransportedUnits();
        for (Unit towTransportedUnit : oldTransportedUnits) {
            if (towTransportedUnit instanceof Unit.UnitRef) {
                Unit realUnit = campaign.getHangar().getUnit(towTransportedUnit.getId());
                if (realUnit != null) {
                    if (realUnit.hasTransportAssignment(TOW_TRANSPORT)) {
                        towTrailer(realUnit, null, realUnit.getTransportAssignment(TOW_TRANSPORT).getTransporterType());
                    } else {
                        logger.error(
                            String.format("Unit %s ('%s') references tow transported unit %s which has no transport assignment",
                                unit.getId(), unit.getName(), towTransportedUnit.getId()));
                    }
                } else {
                    logger.error(
                        String.format("Unit %s ('%s') references missing tow transported unit %s",
                            unit.getId(), unit.getName(), towTransportedUnit.getId()));
                }
            } else {
                towTrailer(towTransportedUnit, null, towTransportedUnit.getTransportAssignment(TOW_TRANSPORT).getTransporterType());
            }
        }
    }

    public Unit loadTransport(Unit transportedUnit, @Nullable Transporter transportedLocation, TransporterType transporterType) {
        return towTrailer(transportedUnit, transportedLocation, transporterType);
    }

    public Unit towTrailer(Unit towedUnit, @Nullable Transporter transportedLocation, TransporterType transporterType) {
        Unit oldTractor = null;
        if (towedUnit.getTransportAssignment(TOW_TRANSPORT) != null) {
            oldTractor = towedUnit.getTransportAssignment(TOW_TRANSPORT).getTransport();
            if (oldTractor != null && !oldTractor.equals(transport)) {
                oldTractor.unloadFromTransport(TOW_TRANSPORT);
            }
        }
        if (transportedLocation != null) {
            towedUnit.setTransportAssignment(TOW_TRANSPORT, new TransportAssignment(transport, transportedLocation));
        }
        else if (transporterType != null){
            towedUnit.setTransportAssignment(TOW_TRANSPORT,new TransportAssignment(transport, transporterType));
        } else {
            logger.error(String.format("Cannot load transport (%s) with unit (%s) without a transported location or transporter!", transport.getId(), towedUnit.getId()));
            return oldTractor;
        }
        addTransportedUnit(Objects.requireNonNull(towedUnit));

        // Update Transport Capacities
        if (!Objects.equals(oldTractor, transport)) {
            if (transport.getEntity() != null & !transport.getEntity().isTrailer() && transport.getEntity() instanceof Tank tank) {
                recalculateTransportCapacity(tank);
            }
            else {
                Unit tractor = getTractor();

                if (tractor != null && tractor.getTransportedUnitsSummary(TOW_TRANSPORT) != null && tractor.getEntity() instanceof Tank tractorTank) {
                    ((TowTransportedUnitsSummary) tractor.getTransportedUnitsSummary(TOW_TRANSPORT)).recalculateTransportCapacity(tractorTank);
                }
            }
        }

        return oldTractor;
    }

    private void recalculateTransportCapacity(Tank tractor) {
        recalculateTransportCapacity(new Vector<>(tractor.getTransports().stream().filter(t -> t instanceof TankTrailerHitch).collect(Collectors.toSet())));
    }
}
