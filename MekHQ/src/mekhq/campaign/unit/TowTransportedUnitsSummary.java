package mekhq.campaign.unit;

import megamek.common.Tank;
import megamek.common.TankTrailerHitch;
import megamek.common.Transporter;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.enums.TransporterType;
import mekhq.campaign.utilities.CampaignTransportUtilities;

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
        if (transport.getEntity() != null && transport.getEntity() instanceof Tank tractor && transport.getEntity().isTractor()) {
            recalculateTransportCapacity(new Vector<>(tractor.getTransports().stream().filter(t -> t instanceof TankTrailerHitch).collect(Collectors.toSet())));
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
        if (transport.getEntity() instanceof Tank tank) {
            Unit tractor;

            if (tank.isTrailer()) {
                if (transport.hasTransportAssignment(TOW_TRANSPORT)) {
                    tractor = transport.getTransportAssignment(TOW_TRANSPORT).getTransport();
                    while (tractor.hasTransportAssignment(TOW_TRANSPORT)) {
                        tractor = transport.getTransportAssignment(TOW_TRANSPORT).getTransport();
                    }
                } else {
                    // No tractor, no towing!
                    setCurrentTransportCapacity(TransporterType.TANK_TRAILER_HITCH, Double.MIN_VALUE);
                    return;
                }
            } else {
                tractor = transport;
            }
            Vector<Unit> otherTrailers = new Vector<>();
            if (tractor.hasTransportedUnits(TOW_TRANSPORT)) {
                Unit towingUnit = tractor.getTransportedUnits(TOW_TRANSPORT).stream().findAny().orElse(null);
                otherTrailers.add(towingUnit);
                while (towingUnit != null && towingUnit.hasTransportedUnits(TOW_TRANSPORT)) {
                    towingUnit = towingUnit.getTransportedUnits(TOW_TRANSPORT).stream().findAny().orElse(null);
                    otherTrailers.add(towingUnit);
                }
            }

            // Finally calculate our towing capacity
            setCurrentTransportCapacity(TransporterType.TANK_TRAILER_HITCH, tractor.getEntity().getWeight()
                - otherTrailers.stream().mapToDouble(u -> u.getEntity().getWeight()).sum());
        }
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
                        towTrailer(realUnit, null, realUnit.getTacticalTransportAssignment().getTransporterType());
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
                towTrailer(towTransportedUnit, null, towTransportedUnit.getTacticalTransportAssignment().getTransporterType());
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
            if (oldTractor != null) {
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
            setCurrentTransportCapacity(transporterType,
                getCurrentTransportCapacity(transporterType) - CampaignTransportUtilities.transportCapacityUsage(transporterType, towedUnit.getEntity()));
        }

        return oldTractor;
    }
}
