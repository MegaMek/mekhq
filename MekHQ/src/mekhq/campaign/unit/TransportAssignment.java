package mekhq.campaign.unit;

import megamek.common.Transporter;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;

import java.util.HashSet;
import java.util.Set;

public class TransportAssignment implements ITransportAssignment {
    protected final MMLogger logger = MMLogger.create(this.getClass());

    Unit transport;
    Transporter transportedLocation;
    Class<? extends Transporter> transporterType;

    public TransportAssignment(Unit transport) {
        this(transport, (Class<? extends Transporter>) null);
    }

    public TransportAssignment(Unit transport, Class<? extends Transporter> transporterType) {
        setTransport(transport);
        setTransporterType(transporterType);
    }

    public TransportAssignment(Unit transport, @Nullable Transporter transportedLocation) {
        setTransport(transport);
        setTransportedLocation(transportedLocation);
        setTransporterType(hasTransportedLocation() ? getTransportedLocation().getClass() : null);
    }

    public TransportAssignment(Unit transport, int hashedTransportedLocation) {
        setTransport(transport);
        for (Transporter location : transport.getEntity().getTransports()) {
            if (location.hashCode() == hashedTransportedLocation) {
                setTransportedLocation(transportedLocation);
                break;
            }
        }

    }


    /**
     * The transport that is assigned, or null if none
     *
     * @return
     */
    @Override
    public Unit getTransport() {
        return transport;
    }

    @Override
    public boolean hasTransport() {
        return transport != null;
    }

    /**
     * Change the transport assignment to have a new transport
     *
     * @param newTransport transport, or null if none
     * @return true if a unit was provided, false if it was null
     */
    protected boolean setTransport(Unit newTransport) {
        this.transport = newTransport;
        return hasTransport();
    }

    @Override
    public Class<? extends Transporter> getTransporterType() {
        return transporterType;
    }

    @Override
    public boolean hasTransporterType() {
        return transporterType != null;
    }

    protected boolean setTransporterType(Class<? extends Transporter> transporterType) {
        this.transporterType = transporterType;
        return hasTransporterType();
    }

    @Override
    public @Nullable Transporter getTransportedLocation() {
        return transportedLocation;
    }

    /**
     * Is this unit in a specific location?
     *
     * @return true if it is
     */
    @Override
    public boolean hasTransportedLocation() {
        return transportedLocation != null;
    }

    /**
     * Set where this unit should be transported
     *
     * @return true if a location was provided, false if it was null
     */
    protected boolean setTransportedLocation(@Nullable Transporter transportedLocation) {
        this.transportedLocation = transportedLocation;
        return hasTransportedLocation();
    }

    /**
     * Convert location to hash to assist with saving/loading
     *
     * @return hash int, or null if none
     */
    @Override
    public int hashTransportedLocation() {
        return getTransportedLocation().hashCode();
    }

    /**
     * After loading UnitRefs need converted to Units
     *
     * @param campaign Campaign we need to fix references for
     * @param unit the unit that needs references fixed
     * @see Unit#fixReferences(Campaign campaign)
     */
    @Override
    public void fixReferences(Campaign campaign, Unit unit) {
            if (getTransport() instanceof Unit.UnitRef) {
            Unit transport = campaign.getHangar().getUnit(getTransport().getId());
            if (transport != null) {
                if (hasTransportedLocation()) {
                    setTransport(transport);
                    //setTransportedLocation(getTransportedLocation());
                    //setTransporterType(hasTransportedLocation() ? getTransportedLocation().getClass() : null);
                } else if (hasTransporterType()) {
                    setTransport(transport);
                    //setTransporterType(getTransporterType());
                }
                else {
                    setTransport(transport);
                    logger.warn(
                        String.format("Unit %s ('%s') is missing a transportedLocation " +
                                "and transporterType for tactical transport %s",
                            unit.getId(), unit.getName(), getTransport().getId()));
                }
            } else {
                logger.error(
                    String.format("Unit %s ('%s') references missing transport %s",
                        unit.getId(), unit.getName(), getTransport().getId()));

                unit.setTacticalTransportAssignment(null);
            }
        }
    }
}
