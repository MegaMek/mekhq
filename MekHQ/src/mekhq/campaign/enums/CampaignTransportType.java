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

package mekhq.campaign.enums;

import megamek.common.*;
import mekhq.campaign.unit.*;
import mekhq.campaign.unit.enums.TransporterType;

/**
 * Enum for the different transport types used in MekHQ.
 * Campaign Transports allow a unit to be
 * assigned a transport (another unit).
 * The different transport types primarily differ
 * in the Transporters they allow.
 * @see Unit
 * @see TransporterType
 */
public enum CampaignTransportType
{
    //region Enum declarations
    /**
     * Units assigned a ship transport, if both units are in the battle
     * the unit will attempt to load onto the transport when deployed into battle.
     * Ship transports are intended to be used for long-term travel or space combat
     * and only allow units to be transported in long-term Transporters like Bays or
     * Docking Collars.
     * @see Bay
     * @see DockingCollar
     */
    SHIP_TRANSPORT(TransportShipAssignment.class, ShipTransportedUnitsSummary.class),
    /**
     * Units assigned a tactical transport, if both units are in the battle
     * the unit will attempt to load onto the transport when deployed into battle.
     * Tactical Transporters are meant to represent short-term transport - Infantry in
     * an Infantry compartment, or Battle Armor on Battle Armor Handles. It still allows
     * units to be loaded into bays though for tactical Dropship assaults.
     * @see InfantryCompartment
     * @see BattleArmorHandles
     */
    TACTICAL_TRANSPORT(TransportAssignment.class, TacticalTransportedUnitsSummary.class);
    // endregion Enum declarations


    // region Variable declarations
    private final Class<? extends ITransportAssignment> transportAssignmentType;
    private final Class<? extends AbstractTransportedUnitsSummary> transportedUnitsSummaryType;
    // endregion Variable declarations

    // region Constructors
    CampaignTransportType(Class<? extends ITransportAssignment> transportAssignmentType, Class<? extends AbstractTransportedUnitsSummary> transportedUnitsSummaryType) {
        this.transportAssignmentType = transportAssignmentType;
        this.transportedUnitsSummaryType = transportedUnitsSummaryType;
    }
    // endregion Constructors


    // region Boolean Comparison Methods

    /**
     * Is this a Ship Transport?
     * @return true if this is a SHIP_TRANSPORT
     */
    public boolean isShipTransport() { return this == SHIP_TRANSPORT; }

    /**
     * Is this a Tactical Transport?
     * @return true if this is a TACTICAL_TRANSPORT
     */
    public boolean isTacticalTransport() { return this == TACTICAL_TRANSPORT; }
    // endregion Boolean Comparison Methods

    // region Getters

    /**
     * Different Transport Types use different transport assignments.
     * @return Transport Assignment class used by this transport type
     */
    public Class<? extends ITransportAssignment> getTransportAssignmentType() { return transportAssignmentType; }

    /**
     * Different Transport Types use different transported units summaries.
     * @return Transported Unit Summary used by this transport type
     */
    public Class<? extends AbstractTransportedUnitsSummary> getTransportedUnitsSummaryType() { return transportedUnitsSummaryType; }
    // endregion Getters
}
