/*
 * ITransportAssignment.java
 *
 * Copyright (c) 2020-2025 The Megamek Team. All rights reserved.
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
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.enums.TransporterType;

import java.util.Optional;

/**
 * Represents an assignment on a transport.
 * @see ITransportedUnitsSummary
 * @see mekhq.campaign.enums.CampaignTransportType
 */
public interface ITransportAssignment {

    /**
     * The transport that is assigned
     * @return
     */
    Unit getTransport();

    boolean hasTransport();

    TransporterType getTransporterType();

    boolean hasTransporterType();

    /**
     * Where is this unit being transported?
     * @return The transporter this unit is in
     */
    Transporter getTransportedLocation();

    /**
     * Is this unit in a specific location?
     * @return true if it is
     */
    boolean hasTransportedLocation();

    /**
     * Convert location to hash to assist with saving/loading
     * @return hash int, or null if none
     */
    Optional<Integer> hashTransportedLocation();

    /**
     * After loading UnitRefs need converted to Units
     * @see Unit#fixReferences(Campaign campaign)
     * @param campaign Campaign we need to fix references for
     * @param unit Unit we need to fix references for
     */
    void fixReferences(Campaign campaign, Unit unit);

}
