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

package mekhq.campaign.enums;

import megamek.common.battleArmor.BattleArmorHandles;
import megamek.common.bays.Bay;
import megamek.common.equipment.DockingCollar;
import megamek.common.units.InfantryCompartment;
import mekhq.campaign.unit.AbstractTransportedUnitsSummary;
import mekhq.campaign.unit.ITransportAssignment;
import mekhq.campaign.unit.ShipTransportedUnitsSummary;
import mekhq.campaign.unit.TacticalTransportedUnitsSummary;
import mekhq.campaign.unit.TowTransportedUnitsSummary;
import mekhq.campaign.unit.TransportAssignment;
import mekhq.campaign.unit.TransportShipAssignment;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.enums.TransporterType;

/**
 * Enum for the different transport types used in MekHQ. Campaign Transports allow a unit to be assigned a transport
 * (another unit). The different transport types primarily differ in the Transporters they allow.
 *
 * @see Unit
 * @see TransporterType
 */
public enum CampaignTransportType {
    // region Enum declarations
    // ORDER MATTERS! This enum's order is used for prioritizing
    // which transport assignment should be used when loading
    // units in the MegaMek lobby.
    /**
     * Units assigned a ship transport, if both units are in the battle the unit will attempt to load onto the transport
     * when deployed into battle. Ship transports are intended to be used for long-term travel or space combat and only
     * allow units to be transported in long-term Transporters like Bays or Docking Collars.
     *
     * @see Bay
     * @see DockingCollar
     */
    SHIP_TRANSPORT(TransportShipAssignment.class, ShipTransportedUnitsSummary.class),
    /**
     * Units assigned a tactical transport, if both units are in the battle the unit will attempt to load onto the
     * transport when deployed into battle. Tactical Transporters are meant to represent short-term transport - Infantry
     * in an Infantry compartment, or Battle Armor on Battle Armor Handles. It still allows units to be loaded into bays
     * though for tactical Dropship assaults.
     *
     * @see InfantryCompartment
     * @see BattleArmorHandles
     */
    TACTICAL_TRANSPORT(TransportAssignment.class, TacticalTransportedUnitsSummary.class),

    /**
     * Units assigned a tow transports will, if both deployed to battle, automatically set the unit as being towed.
     */
    TOW_TRANSPORT(TransportAssignment.class, TowTransportedUnitsSummary.class);
    // endregion Enum declarations

    // region Variable declarations
    private final Class<? extends ITransportAssignment> transportAssignmentType;
    private final Class<? extends AbstractTransportedUnitsSummary> transportedUnitsSummaryType;
    // endregion Variable declarations

    // region Constructors
    CampaignTransportType(Class<? extends ITransportAssignment> transportAssignmentType,
          Class<? extends AbstractTransportedUnitsSummary> transportedUnitsSummaryType) {
        this.transportAssignmentType = transportAssignmentType;
        this.transportedUnitsSummaryType = transportedUnitsSummaryType;
    }
    // endregion Constructors

    // region Boolean Comparison Methods

    /**
     * Is this a Ship Transport?
     *
     * @return true if this is a SHIP_TRANSPORT
     */
    public boolean isShipTransport() {
        return this == SHIP_TRANSPORT;
    }

    /**
     * Is this a Tactical Transport?
     *
     * @return true if this is a TACTICAL_TRANSPORT
     */
    public boolean isTacticalTransport() {
        return this == TACTICAL_TRANSPORT;
    }

    /**
     * Is this a Tow Transport?
     *
     * @return true if this is a TOW_TRANSPORT
     */
    public boolean isTowTransport() {
        return this == TOW_TRANSPORT;
    }
    // endregion Boolean Comparison Methods

    // region Getters

    /**
     * Different Transport Types use different transport assignments.
     *
     * @return Transport Assignment class used by this transport type
     */
    public Class<? extends ITransportAssignment> getTransportAssignmentType() {
        return transportAssignmentType;
    }

    /**
     * Different Transport Types use different transported units summaries.
     *
     * @return Transported Unit Summary used by this transport type
     */
    public Class<? extends AbstractTransportedUnitsSummary> getTransportedUnitsSummaryType() {
        return transportedUnitsSummaryType;
    }
    // endregion Getters
}
