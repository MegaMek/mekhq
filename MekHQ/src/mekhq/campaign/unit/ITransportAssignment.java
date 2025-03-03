/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 */

package mekhq.campaign.unit;

import megamek.common.Transporter;
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

    /**
     * Bays have some extra functionality other transporters don't have, like
     * having a tech crew, which will matter for boarding actions against
     * dropships and other Ship Transports. This method determines if this
     * transport assignment is for a Bay.
     * @return true if the unit is transported in a Bay or a subclass
     * @see megamek.common.Bay
     */
    boolean isTransportedInBay();

}
