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

import java.util.Optional;

import megamek.common.annotations.Nullable;
import megamek.common.bays.Bay;
import megamek.common.equipment.Transporter;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.enums.TransporterType;

/**
 * Represents an assignment on transport. Currently only used by TACTICAL_TRANSPORT but this could be used by other
 * transport types.
 *
 * @see TacticalTransportedUnitsSummary
 * @see CampaignTransportType#TACTICAL_TRANSPORT
 */
public class TransportAssignment implements ITransportAssignment {
    protected final static MMLogger LOGGER = MMLogger.create(TransportAssignment.class);

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
        setTransporterType(hasTransportedLocation() ?
                                 TransporterType.getTransporterType(getTransportedLocation()) :
                                 null);
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
     */
    protected void setTransport(Unit newTransport) {
        this.transport = newTransport;
        hasTransport();
    }

    @Override
    public TransporterType getTransporterType() {
        return transporterType;
    }

    @Override
    public boolean hasTransporterType() {
        return transporterType != null;
    }

    protected void setTransporterType(TransporterType transporterType) {
        this.transporterType = transporterType;
        hasTransporterType();
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
     */
    protected void setTransportedLocation(@Nullable Transporter transportedLocation) {
        this.transportedLocation = transportedLocation;
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
     * @param unit     the unit that needs references fixed
     *
     * @see Unit#fixReferences(Campaign)
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
                } else {
                    setTransport(transport);
                    LOGGER.warn("Unit {} ('{}') is missing a transportedLocation " +
                                      "and transporterType for tactical transport {}",
                          unit.getId(),
                          unit.getName(),
                          getTransport().getId());
                }
            } else {
                LOGGER.error("Unit {} ('{}') references missing transport {}",
                      unit.getId(),
                      unit.getName(),
                      getTransport().getId());

                unit.setTacticalTransportAssignment(null);
            }
        }
    }

    /**
     * Bays have some extra functionality other transporters don't have, like having a tech crew, which will matter for
     * boarding actions against dropships and other Ship Transports. This method determines if this transport assignment
     * is for a Bay.
     *
     * @return true if the unit is transported in a Bay or a subclass
     *
     * @see Bay
     */
    @Override
    public boolean isTransportedInBay() {
        return (hasTransporterType() && Bay.class.isAssignableFrom(getTransporterType().getTransporterClass()));
    }
}
