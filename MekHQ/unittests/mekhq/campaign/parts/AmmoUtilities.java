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
 */
package mekhq.campaign.parts;

import megamek.common.AmmoType;
import megamek.common.BombType;
import megamek.common.EquipmentType;
import megamek.common.weapons.infantry.InfantryWeapon;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AmmoUtilities {
    /**
     * Gets an AmmoType by name (performing any initialization required
     * on the MM side).
     *
     * @param name The lookup name for the AmmoType.
     * @return The ammo type for the given name.
     */
    public synchronized static AmmoType getAmmoType(String name) {
        EquipmentType equipmentType = EquipmentType.get(name);
        assertNotNull(equipmentType);
        assertInstanceOf(AmmoType.class, equipmentType);
        return (AmmoType) equipmentType;
    }

    /**
     * Gets a BombType by name (performing any initialization required
     * on the MM side).
     *
     * @param name The lookup name for the BombType.
     * @return The bomb type for the given name.
     */
    public synchronized static BombType getBombType(String name) {
        EquipmentType equipmentType = EquipmentType.get(name);
        assertNotNull(equipmentType);
        assertInstanceOf(BombType.class, equipmentType);
        return (BombType) equipmentType;
    }

    /**
     * Gets a InfantryWeapon by name (performing any initialization required
     * on the MM side).
     *
     * @param name The lookup name for the InfantryWeapon.
     * @return The bomb type for the given name.
     */
    public synchronized static InfantryWeapon getInfantryWeapon(String name) {
        EquipmentType equipmentType = EquipmentType.get(name);
        assertNotNull(equipmentType);
        assertInstanceOf(InfantryWeapon.class, equipmentType);
        return (InfantryWeapon) equipmentType;
    }
}
