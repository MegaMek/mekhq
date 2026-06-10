/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;

import megamek.common.equipment.EquipmentType;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.campaign.Campaign;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link InfantryDisposableWeaponPart} - a single per-trooper Disposable Weapon (TO:AR p.106). A platoon gets one
 * of these per trooper, so it is valued/refit/bought-and-sold as an individual weapon.
 */
class InfantryDisposableWeaponPartTest {

    private static final String LAW = "Rocket Launcher (LAW)";

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static InfantryDisposableWeaponPart newPart() {
        InfantryWeapon law = (InfantryWeapon) EquipmentType.get(LAW);
        return new InfantryDisposableWeaponPart(0, law, -1, mock(Campaign.class));
    }

    @Test
    @DisplayName("name is the weapon name with a (Disposable) suffix")
    void nameHasDisposableSuffix() {
        assertEquals("Rocket Launcher (LAW) (Disposable)", newPart().getName());
    }

    @Test
    @DisplayName("a disposable weapon part is not a primary weapon and starts unspent")
    void notPrimaryAndUnspent() {
        InfantryDisposableWeaponPart part = newPart();
        assertFalse(part.isPrimary());
        assertFalse(part.isSpent());
        assertFalse(part.needsFixing());
    }

    @Test
    @DisplayName("each disposable weapon part is valued at the single weapon's cost")
    void stickerPriceIsWeaponCost() {
        assertTrue(newPart().getStickerPrice().isPositive(), "A disposable weapon part should have a positive value");
    }

    @Test
    @DisplayName("clone produces an equal, distinct part of the same type")
    void cloneIsDistinctSameType() {
        InfantryDisposableWeaponPart part = newPart();
        InfantryDisposableWeaponPart clone = part.clone();

        assertNotSame(part, clone);
        assertInstanceOf(InfantryDisposableWeaponPart.class, clone);
        assertEquals(part.getName(), clone.getName());
        assertTrue(part.isSamePartType(clone), "A clone should be the same part type for warehouse/acquisition");
    }

    @Test
    @DisplayName("the missing part accepts a plain EquipmentPart of the same weapon type from stock")
    void missingPartAcceptsEquipmentPartSpare() {
        InfantryDisposableWeaponPart part = newPart();
        InfantryWeapon law = (InfantryWeapon) EquipmentType.get(LAW);

        MissingInfantryDisposableWeaponPart missing = part.getMissingPart();
        assertInstanceOf(InfantryDisposableWeaponPart.class, missing.getNewPart());

        // A LAW stocked as a plain EquipmentPart (what the parts store creates) must satisfy the refit/replacement.
        EquipmentPart stockSpare = new EquipmentPart(0, law, -1, 1.0, false, mock(Campaign.class));
        assertTrue(missing.isAcceptableReplacement(stockSpare, true),
              "An existing EquipmentPart LAW in the warehouse should be an acceptable replacement");
    }

    @Test
    @DisplayName("a disposable order is a bulk acquisition (one roll fills the whole platoon)")
    void missingPartIsBulkAcquisition() {
        assertTrue(newPart().getMissingPart().isBulkAcquisition(),
              "Infantry disposable weapons should be ordered as a single bulk acquisition");
    }

    @Test
    @DisplayName("a spent disposable presents as reloadable work")
    void spentIsReloadWork() throws Exception {
        InfantryDisposableWeaponPart part = newPart();
        Field spentField = InfantryDisposableWeaponPart.class.getDeclaredField("spent");
        spentField.setAccessible(true);
        spentField.set(part, true);

        assertTrue(part.isSpent());
        assertTrue(part.needsFixing());
        assertEquals(15, part.getBaseTime());
        assertEquals(0, part.getDifficulty());
    }
}
