/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.Objects;

import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;

/**
 * Represents an assignment to a specific bay on a transport ship. Currently only used by SHIP_TRANSPORT but this could
 * be used by other transport types.
 *
 * @see ShipTransportedUnitsSummary
 * @see CampaignTransportType#SHIP_TRANSPORT
 */
public class TransportShipAssignment extends TransportAssignment {
    private final int bayNumber;

    /**
     * Initializes a new instance of the TransportShipAssignment class.
     *
     * @param transportShip The transport ship.
     * @param bayNumber     The bay number on the transport ship.
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
     * @param unit     the unit that needs references fixed
     *
     * @see Unit#fixReferences(Campaign campaign)
     */
    @Override
    public void fixReferences(Campaign campaign, Unit unit) {
        if (getTransportShip() instanceof Unit.UnitRef) {
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
