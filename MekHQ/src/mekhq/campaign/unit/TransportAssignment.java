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

import megamek.common.Bay;
import megamek.common.Transporter;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.enums.TransporterType;

import java.util.Optional;

/**
 * Represents an assignment on a transport. Currently only used by TACTICAL_TRANSPORT
 * but this could be used by other transport types.
 * @see TacticalTransportedUnitsSummary
 * @see CampaignTransportType#TACTICAL_TRANSPORT
 */
public class TransportAssignment implements ITransportAssignment {
    protected final MMLogger logger = MMLogger.create(this.getClass());

    Unit transport;
    Transporter transportedLocation;
    TransporterType transporterType;

    public TransportAssignment(Unit transport) {
        this(transport, (TransporterType) null);
    }

    public TransportAssignment(Unit transport, TransporterType transporterType) {
        setTransport(transport);
        setTransporterType(transporterType);
    }

    public TransportAssignment(Unit transport, @Nullable Transporter transportedLocation) {
        setTransport(transport);
        setTransportedLocation(transportedLocation);
        setTransporterType(hasTransportedLocation() ? TransporterType.getTransporterType(getTransportedLocation()) : null);
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
     * The transport that is assigned
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
    public TransporterType getTransporterType() {
        return transporterType;
    }

    @Override
    public boolean hasTransporterType() {
        return transporterType != null;
    }

    protected boolean setTransporterType(TransporterType transporterType) {
        this.transporterType = transporterType;
        return hasTransporterType();
    }

    @Override
    public Transporter getTransportedLocation() {
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
    public Optional<Integer> hashTransportedLocation() {
        if (hasTransportedLocation()) {
            return Optional.of(getTransportedLocation().hashCode());
        }
        return Optional.empty();
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

                } else if (hasTransporterType()) {
                    setTransport(transport);
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

    /**
     * Bays have some extra functionality other transporters don't have, like
     * having a tech crew, which will matter for boarding actions against
     * dropships and other Ship Transports. This method determines if this
     * transport assignment is for a Bay.
     *
     * @return true if the unit is transported in a Bay or a subclass
     * @see Bay
     */
    @Override
    public boolean isTransportedInBay() {
        return (hasTransporterType() && Bay.class.isAssignableFrom(getTransporterType().getTransporterClass()));
    }
}
