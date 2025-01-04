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

import java.util.Objects;

/**
 * Represents an assignment to a specific bay on a transport ship.
 */
public class TransportShipAssignment{
    private final Unit transportShip;
    private final int bayNumber;

    /**
     * Initializes a new instance of the TransportShipAssignment class.
     * @param transportShip The transport ship.
     * @param bayNumber The bay number on the transport ship.
     */
    public TransportShipAssignment(Unit transportShip, int bayNumber) {
        this.transportShip = Objects.requireNonNull(transportShip);
        this.bayNumber = bayNumber;
    }

    /**
     * Gets the transport ship for this assignment.
     */
    public Unit getTransportShip() {
        return transportShip;
    }

    /**
     * Gets the bay number for the transport ship.
     */
    public int getBayNumber() {
        return bayNumber;
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
