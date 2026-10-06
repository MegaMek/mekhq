/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.utilities;

import java.util.UUID;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.Sensor;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.IBomber;
import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.unit.Unit;

/**
 * Utility class that provides helper methods for working with entities.
 */
public class EntityUtilities {
    /**
     * Retrieves an {@link Entity} associated with a specific unit ID.
     *
     * <p>This method attempts to find a {@link Unit} in the given {@link Campaign} using the provided
     * unit ID. If the unit exists, the method returns the associated {@link Entity}. If the unit does not exist or has
     * no associated entity, the method returns {@code null}.
     *
     * @param hangar The {@link LocalHangar} instance from which to retrieve the {@link Unit}.
     * @param unitID The {@link UUID} of the unit for which the associated {@link Entity} is requested.
     *
     * @return The {@link Entity} associated with the specified unit ID, or {@code null} if the unit is not found or has
     *       no associated entity.
     */
    public static @Nullable Entity getEntityFromUnitId(mekhq.campaign.LocalHangar hangar, UUID unitID) {
        Unit unit = hangar.getUnit(unitID);

        if (unit == null) {
            return null;
        }

        return unit.getEntity();
    }

    /**
     * Checks if the given entity is unsupported in MekHQ.
     *
     * <p>This method evaluates whether the specified entity is considered unsupported.
     * Currently, it checks if the unit type matches {@link UnitType#GUN_EMPLACEMENT} or if the entity uses drone OS
     * {@link Entity#hasDroneOs()}.
     *
     * @param entity The entity to be checked
     *
     * @return {@code true} if the entity is unsupported (e.g., a {@link UnitType#GUN_EMPLACEMENT}), otherwise
     *       {@code false}.
     */
    public static boolean isUnsupportedEntity(Entity entity) {
        return entity.getUnitType() == UnitType.GUN_EMPLACEMENT
                     || entity.hasDroneOs();
    }

    /**
     * Determines whether the given {@link Entity} is equipped with Improved Sensors.
     *
     * <p>Improved Sensors are represented by the presence of a BAP (Beagle Active Probe) flag and an internal name
     * that matches either {@link Sensor#IS_IMPROVED} or {@link Sensor#CL_IMPROVED}.</p>
     *
     * @param entity the {@link Entity} to check for improved sensors. They are also checked for
     * the Improved Sensors quirk.
     *
     * @return {@code true} if the entity has working Improved Sensors
     */
    public static boolean hasImprovedSensors(Entity entity) {
        for (Mounted<?> equip : entity.getMisc()) {
            EquipmentType type = equip.getType();
            if (type != null && type.hasFlag(MiscType.F_BAP) && equip.isOperable() && isImprovedSensors(type)) {
                return true;
            }
        }
        return entity.hasQuirk(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS);
    }

    /**
     * Determines whether the given {@link Entity} is equipped with an Active Probe (BAP) but does NOT have Improved
     * Sensors.
     *
     * <p>This checks for the BAP (Beagle Active Probe) flag, but explicitly excludes internal names that match
     * Improved Sensors (i.e., {@link Sensor#IS_IMPROVED} or {@link Sensor#CL_IMPROVED}).</p>
     *
     * @param entity the {@link Entity} to check for an active probe
     *
     * @return {@code true} if the entity has a working standard active probe (but not Improved Sensors)
     */
    public static boolean hasActiveProbe(Entity entity) {
        for (Mounted<?> equip : entity.getMisc()) {
            EquipmentType type = equip.getType();
            if (type != null && type.hasFlag(MiscType.F_BAP) && equip.isOperable() && !isImprovedSensors(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the given equipment type is Improved Sensors, rather than a standard active probe.
     *
     * @param type the equipment type to check
     *
     * @return {@code true} if the type is Inner Sphere or Clan Improved Sensors
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isImprovedSensors(EquipmentType type) {
        String internalName = type.getInternalName();
        return Sensor.IS_IMPROVED.equals(internalName) || Sensor.CL_IMPROVED.equals(internalName);
    }

    /**
     * Determines whether the given {@link Entity} carries a working Recon Camera (TO:AUE p.150), either mounted as
     * equipment or loaded as a Recon Camera Pod in its bomb bays.
     *
     * @param entity the {@link Entity} to check for a recon camera
     *
     * @return {@code true} if the entity has a Recon Camera or carries a Recon Camera Pod
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean hasReconCamera(Entity entity) {
        if (entity.hasWorkingMisc(MiscType.F_RECON_CAMERA)) {
            return true;
        }

        // A pod only becomes mounted equipment once a game starts, so between battles it is found in the bomb loadout
        if (entity instanceof IBomber bomber) {
            return bomber.getBombChoices().getCount(BombTypeEnum.RECON_CAMERA) > 0;
        }

        return false;
    }
}
