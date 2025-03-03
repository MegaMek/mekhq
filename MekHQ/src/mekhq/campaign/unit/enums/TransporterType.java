/**
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package mekhq.campaign.unit.enums;

import megamek.common.*;
import mekhq.campaign.enums.CampaignTransportType;

/**
 * Entities are equipped with different Transporters.
 * TransporterTypes are the different kinds of
 * transporters from MegaMek that are implemented
 * to be used with CampaignTransportTypes, like
 * Mek Bays, Docking Collars, Battle Armor Handles,
 * or Infantry Compartments.
 *
 * @see Transporter
 * @see CampaignTransportType
 */
public enum TransporterType {

    // region Enum declarations
    TANK_TRAILER_HITCH(TankTrailerHitch.class),
    HEAVY_VEHICLE_BAY(HeavyVehicleBay.class),
    NAVAL_REPAIR_FACILITY(NavalRepairFacility.class),
    REINFORCED_REPAIR_FACILITY(ReinforcedRepairFacility.class),
    DROPSHUTTLE_BAY(DropshuttleBay.class),
    LIGHT_VEHICLE_BAY(LightVehicleBay.class),
    SUPER_HEAVY_VEHICLE_BAY(SuperHeavyVehicleBay.class),
    MEK_BAY(MekBay.class),
    PROTO_MEK_BAY(ProtoMekBay.class),
    ASF_BAY(ASFBay.class),
    SMALL_CRAFT_BAY(SmallCraftBay.class),
    INFANTRY_BAY(InfantryBay.class),
    BATTLE_ARMOR_BAY(BattleArmorBay.class),
    INFANTRY_COMPARTMENT(InfantryCompartment.class),
    DOCKING_COLLAR(DockingCollar.class),
    BATTLE_ARMOR_HANDLES(BattleArmorHandles.class),
    BATTLE_ARMOR_HANDLES_TANK(BattleArmorHandlesTank.class),
    CLAMP_MOUNT_MEK(ClampMountMek.class),
    CLAMP_MOUNT_TANK(ClampMountTank.class),
    PROTO_MEK_CLAMP_MOUNT(ProtoMekClampMount.class);

    // endregion Enum declarations

    // region Variable declarations
    private final Class<? extends Transporter> transporterClass;
    // end region Variable declarations

    // region Constructor
    TransporterType(Class<? extends Transporter> transporterClass) {
        this.transporterClass = transporterClass;
    }
    // endregion Constructor

    /**
     * An Entity's Transporters need to be mapped to their
     * TransporterTypes. For the provided Transporter,
     * this returns its corresponding TransporterType,
     * or null if it's not found.
     *
     * @see Transporter
     * @param transporter specific transporter to return the type of
     * @return TransporterType (enum) of the provided transporter, or null
     * @param <T> extends Transporter
     */
    public static <T extends Transporter> TransporterType getTransporterType(T transporter) {
        for (TransporterType transporterType : TransporterType.values()) {
            if (transporterType.getTransporterClass() == transporter.getClass()) {
                return transporterType;
            }
        }
        return null;
    }

    /**
     * The specific Class of Transporter that corresponds to this TransporterType
     * @return Class that extends Transporter
     */
    public Class<? extends Transporter> getTransporterClass() { return transporterClass; }
}
