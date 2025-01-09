/*
 * TransportShipAssignment.java
 *
 * Copyright (c) 2020 The Megamek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.unit;

import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;

import java.util.Objects;

/**
 * Represents an assignment to a specific bay on a transport ship.
 * Currently only used by SHIP_TRANSPORT
 * but this could be used by other transport types.
 * @see ShipTransportedUnitsSummary
 * @see CampaignTransportType#SHIP_TRANSPORT
 */
public class TransportShipAssignment extends TransportAssignment{
    private final int bayNumber;

    /**
     * Initializes a new instance of the TransportShipAssignment class.
     * @param transportShip The transport ship.
     * @param bayNumber The bay number on the transport ship.
     */
    public TransportShipAssignment(Unit transportShip, int bayNumber) {
        super(transportShip);
        this.bayNumber = bayNumber;

        if (getTransportShip().getEntity() != null) {
            setTransportedLocation(transportShip.getEntity().getBayById(bayNumber));
        }
    }

    /**
     * Gets the transport ship for this assignment.
     */
    public Unit getTransportShip() {
        return transport;
    }

    /**
     * Gets the bay number for the transport ship.
     */
    public int getBayNumber() {
        return bayNumber;
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
        if (getTransportShip() instanceof Unit.UnitRef){
            Unit transportShip = campaign.getHangar().getUnit(getTransportShip().getId());
            if (transportShip != null) {
                setTransport(transportShip);
            } else {
                logger.error(
                    String.format("Unit %s ('%s') references missing transport ship %s",
                        unit.getId(), unit.getName(), getTransportShip().getId()));

                unit.setTransportShipAssignment(null);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (getClass() != o.getClass()) {
            return false;
        } else {
            TransportShipAssignment other = (TransportShipAssignment) o;
            return Objects.equals(getTransportShip(), other.getTransportShip())
                    && (getBayNumber() == other.getBayNumber());
        }
    }


    @Override
    public int hashCode() {
        return Objects.hash(getTransportShip(), getBayNumber());
    }
}
