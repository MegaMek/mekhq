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

import static mekhq.campaign.enums.CampaignTransportType.TOW_TRANSPORT;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.TankTrailerHitch;
import megamek.common.equipment.Transporter;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.enums.TransporterType;

/**
 * Tracks what units this tractor is towing, and its current capacity for towing more units if it is the lead tractor.
 * Tractors and towing are weirder than other CampaignTransportTypes so there is a lot more "hard-coding" to this being
 * used with TANK_TRAILER_HITCHes because of the unique rules around tractors and trailers.
 *
 * @see AbstractTransportedUnitsSummary
 * @see CampaignTransportType#TOW_TRANSPORT
 * @see TankTrailerHitch
 * @see TransporterType#TANK_TRAILER_HITCH
 *
 */
public class TowTransportedUnitsSummary extends AbstractTransportedUnitsSummary {
    private static final MMLogger LOGGER = MMLogger.create(TowTransportedUnitsSummary.class);

    public TowTransportedUnitsSummary(Unit transport) {
        super(transport, TOW_TRANSPORT);
    }

    /**
     * Bay unloading utility used when removing a bay-equipped Transport unit This removes all units assigned to the
     * transport from it
     *
     * @param campaign used to remove this unit as transport from any other units in the campaign
     */
    @Override
    public void clearTransportedUnits(Campaign campaign) {
        clearTransportedUnits();

        // And now reset the Transported values for all the units we just booted
        campaign.getHangar().forEachUnit(u -> {
            if (u.hasTransportAssignment(TOW_TRANSPORT)
                      && Objects.equals(transport, u.getTransportAssignment(TOW_TRANSPORT).getTransport())) {
                u.setTransportAssignment(TOW_TRANSPORT, null);
            }
        });

        recalculateTransportCapacity(transport.getEntity().getTransports());
    }

    /**
     * Recalculates transport capacity - Pass in all the transporters if you want, but it just matters if it has a
     * TANK_TRAILER_HITCH. This is special for tow transport and will only calculate it for the TANK_TRAILER_HITCH, and
     * it will only calculate the transport capacity if this unit is the tractor pulling all the trailers, otherwise it
     * sets the unit's capacity to 0.
     *
     * @param transporters What transporters are we recalculating?
     *
     * @see Entity#getTransports()
     * @see TankTrailerHitch
     */
    @Override
    public void recalculateTransportCapacity(Vector<Transporter> transporters) {
        // Make sure we have a trailer hitch for towing first
        boolean noTrailerHitch = transporters.stream().noneMatch(t -> t instanceof TankTrailerHitch);
        if (noTrailerHitch) {
            return;
        }
        Unit tractor = getTractor();

        // Not a tractor/doesn't have  tractor, no towing!
        if (tractor == null || !tractor.equals(transport)) {
            setCurrentTransportCapacity(TransporterType.TANK_TRAILER_HITCH, Double.MIN_VALUE);
            return;
        }

        // Can't finish the calculations if the tractor isn't initialized yet, let's get out of here.
        if (tractor.getEntity() == null) {
            return;
        }

        Vector<Unit> otherTrailers = new Vector<>();
        if (tractor.hasTransportedUnits(TOW_TRANSPORT)) {
            // The tractor will only be listed as towing the unit directly behind it, so we can "findAny"
            Unit towingUnit = tractor.getTransportedUnits(TOW_TRANSPORT).stream().findAny().orElse(null);
            otherTrailers.add(towingUnit);
            while (towingUnit != null && towingUnit.hasTransportedUnits(TOW_TRANSPORT)) {
                towingUnit = towingUnit.getTransportedUnits(TOW_TRANSPORT).iterator().next();
                otherTrailers.add(towingUnit);
            }
        }

        // Finally calculate our towing capacity - Get the tractor's weight then
        // get the entity for each trailer,  sum the trailer weights, and subtract
        // that from the tractor's weight to get the remaining towing capacity.
        setCurrentTransportCapacity(TransporterType.TANK_TRAILER_HITCH, tractor.getEntity().getWeight()
                                                                              -
                                                                              otherTrailers.stream()
                                                                                    .filter(u -> u.getEntity() != null)
                                                                                    .mapToDouble(u -> u.getEntity()
                                                                                                            .getWeight())
                                                                                    .sum());
    }

    /**
     * Fixes references after loading
     *
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
                        LOGGER.error(
                              "Unit {} ('{}') references tow transported unit {} which has no transport assignment",
                              unit.getId(),
                              unit.getName(),
                              towTransportedUnit.getId());
                    }
                } else {
                    LOGGER.error("Unit {} ('{}') references missing tow transported unit {}",
                          unit.getId(), unit.getName(), towTransportedUnit.getId());
                }
            } else {
                towTrailer(towTransportedUnit,
                      null,
                      towTransportedUnit.getTransportAssignment(TOW_TRANSPORT).getTransporterType());
            }
        }
    }

    /**
     * Designate a unit to be towed by the transport. Will update transport capacities. The transport assignments will
     * be between the specific units that are attached, essentially forming a linked list. We only want to attach to the
     * front-most unit though because that unit determines the towing capacity.
     *
     * @param towedUnit           unit assigned as being towed
     * @param transportedLocation specific hitch being used
     * @param transporterType     transporter type
     *
     * @return unit that was previously pulling this unit, or null
     */
    public @Nullable Unit towTrailer(Unit towedUnit, @Nullable Transporter transportedLocation,
          TransporterType transporterType) {
        Unit oldTractor = null;
        if (towedUnit.getTransportAssignment(TOW_TRANSPORT) != null) {
            oldTractor = towedUnit.getTransportAssignment(TOW_TRANSPORT).getTransport();
            if (oldTractor != null && !oldTractor.equals(transport)) {
                oldTractor.unloadFromTransport(TOW_TRANSPORT);
            }
        }
        if (transportedLocation != null) {
            towedUnit.setTransportAssignment(TOW_TRANSPORT, new TransportAssignment(transport, transportedLocation));
        } else if (transporterType != null) {
            towedUnit.setTransportAssignment(TOW_TRANSPORT, new TransportAssignment(transport, transporterType));
        } else {
            LOGGER.error("Cannot load transport ({}) with unit ({}) without a transported location or transporter!",
                  transport.getId(),
                  towedUnit.getId());
            return oldTractor;
        }
        addTransportedUnit(Objects.requireNonNull(towedUnit));

        // Update Transport Capacities
        if (!Objects.equals(oldTractor, transport)) {
            if (transport.getEntity() != null & !transport.getEntity().isTrailer() &&
                      transport.getEntity() instanceof Tank tank) {
                recalculateTransportCapacity(tank);
            } else {
                Unit tractor = getTractor();

                if (tractor != null &&
                          tractor.getTransportedUnitsSummary(TOW_TRANSPORT) != null &&
                          tractor.getEntity() instanceof Tank tractorTank) {
                    ((TowTransportedUnitsSummary) tractor.getTransportedUnitsSummary(TOW_TRANSPORT)).recalculateTransportCapacity(
                          tractorTank);
                }
            }
        }

        return oldTractor;
    }

    @Override
    protected void init() {
        if (transport.getEntity() != null &&
                  transport.getEntity() instanceof Tank tractor &&
                  !transport.getEntity().isTrailer()) {
            recalculateTransportCapacity(tractor);
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
            if (tractor != null &&
                      tractor.hasTransportedUnits(TOW_TRANSPORT) &&
                      tractor.getEntity() != null &&
                      tractor.getEntity() instanceof Tank tank) {
                TowTransportedUnitsSummary tractorTransportedUnitsSummary = (TowTransportedUnitsSummary) tractor.getTransportedUnitsSummary(
                      TOW_TRANSPORT);
                tractorTransportedUnitsSummary.recalculateTransportCapacity(tank);
            }
        }
    }

    /**
     * Find the tractor pulling this whole train
     *
     * @return the tractor pulling the entire train of trailers
     */
    private @Nullable Unit getTractor() {
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

    private void recalculateTransportCapacity(Tank tractor) {
        recalculateTransportCapacity(new Vector<>(tractor.getTransports()
                                                        .stream()
                                                        .filter(t -> t instanceof TankTrailerHitch)
                                                        .collect(Collectors.toSet())));
    }
}
