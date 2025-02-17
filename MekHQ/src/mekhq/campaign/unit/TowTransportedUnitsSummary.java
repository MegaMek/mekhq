package mekhq.campaign.unit;

import megamek.common.Entity;
import megamek.common.Tank;
import megamek.common.Transporter;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.enums.TransporterType;
import mekhq.campaign.utilities.CampaignTransportUtilities;

import java.util.*;

import static mekhq.campaign.enums.CampaignTransportType.TOW_TRANSPORT;

public class TowTransportedUnitsSummary extends AbstractTransportedUnitsSummary{
    private static final MMLogger logger = MMLogger.create(TowTransportedUnitsSummary.class);

    public TowTransportedUnitsSummary(Unit transport) {
        super(transport);
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
        if (transport.getEntity() instanceof Tank tank) {
            //tank.get
        }
        super.recalculateTransportCapacity(transporters);
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
