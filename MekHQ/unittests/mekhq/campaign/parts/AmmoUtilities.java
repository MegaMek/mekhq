/*
 * Copyright (C) 2020 MegaMek team
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

package mekhq.campaign.parts;

import static org.junit.Assert.*;

import megamek.common.AmmoType;
import megamek.common.BombType;
import megamek.common.EquipmentType;
import megamek.common.weapons.infantry.InfantryWeapon;

public class AmmoUtilities {
    /**
     * Gets an AmmoType by name (performing any initialization required
     * on the MM side).
     * @param name The lookup name for the AmmoType.
     * @return The ammo type for the given name.
     */
    public synchronized static AmmoType getAmmoType(String name) {
        EquipmentType equipmentType = EquipmentType.get(name);
        assertNotNull(equipmentType);
        assertTrue(equipmentType instanceof AmmoType);

        return (AmmoType) equipmentType;
    }

    /**
     * Gets a BombType by name (performing any initialization required
     * on the MM side).
     * @param name The lookup name for the BombType.
     * @return The bomb type for the given name.
     */
    public synchronized static BombType getBombType(String name) {
        EquipmentType equipmentType = EquipmentType.get(name);
        assertNotNull(equipmentType);
        assertTrue(equipmentType instanceof BombType);

        return (BombType) equipmentType;
    }

    /**
     * Gets a InfantryWeapon by name (performing any initialization required
     * on the MM side).
     * @param name The lookup name for the InfantryWeapon.
     * @return The bomb type for the given name.
     */
    public synchronized static InfantryWeapon getInfantryWeapon(String name) {
        EquipmentType equipmentType = EquipmentType.get(name);
        assertNotNull(equipmentType);
        assertTrue(equipmentType instanceof InfantryWeapon);

        return (InfantryWeapon) equipmentType;
    }
}
