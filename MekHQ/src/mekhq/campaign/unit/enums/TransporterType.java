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

package mekhq.campaign.unit.enums;

import megamek.common.*;

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

    public static <T extends Transporter, Optional> TransporterType getTransporterType(T transporter) {
        for (TransporterType transporterType : TransporterType.values()) {
            if (transporterType.getTransporterClass() == transporter.getClass()) {
                return transporterType;
            }
        }
        return null;
    }

    public Class<? extends Transporter> getTransporterClass() { return transporterClass; }
}
