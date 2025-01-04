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

import megamek.common.Transporter;
import megamek.common.annotations.Nullable;

/**
 * Represents an assignment on a transport.
 */
public interface ITransportAssignment {

    /**
     * The transport that is assigned, or null if none
     * @return
     */
    public @Nullable Unit getTransport();

    public boolean hasTransport();

    /**
     * Change the transport assignment to have a new transport
     * @param transport new transport, or null if none
     * @return true if a unit was provided, false if it was null
     */
    public boolean setTransport(@Nullable Unit transport);

    public Class<? extends Transporter> getTransporterType();

    public boolean hasTransporterType();

    public boolean setTransporterType(Class<? extends Transporter> transporterType);



    /**
     * Where is this unit being transported?
     * @return The transporter this unit is in
     */
    public @Nullable Transporter getTransportedLocation();

    /**
     * Is this unit in a specific location?
     * @return true if it is
     */
    public boolean hasTransportedLocation();

    /**
     * Set where this unit should be transported
     * @return true if a location was provided, false if it was null
     */
    public boolean setTransportedLocation(@Nullable Transporter transportedLocation);

    /**
     * Convert location to hash to assist with saving/loading
     * @return hash int, or null if none
     */
    public @Nullable int hashTransportedLocation();

}
