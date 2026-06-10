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
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link InfantryDisposableWeaponPart} - the Disposable Weapon (TO:AR p.106) loadout part used to value and
 * buy/sell a conventional infantry platoon's disposable weapons.
 */
class InfantryDisposableWeaponPartTest {

    private static final String LAW = "Rocket Launcher (LAW)";
    private static final int TROOPERS = 28;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static InfantryDisposableWeaponPart newPart(int troopers) {
        InfantryWeapon law = (InfantryWeapon) EquipmentType.get(LAW);
        return new InfantryDisposableWeaponPart(0, law, -1, troopers, mock(Campaign.class));
    }

    @Test
    @DisplayName("name is the weapon name with a (Disposable) suffix")
    void nameHasDisposableSuffix() {
        assertEquals("Rocket Launcher (LAW) (Disposable)", newPart(TROOPERS).getName());
    }

    @Test
    @DisplayName("a disposable weapon part is not a primary weapon and starts unspent")
    void notPrimaryAndUnspent() {
        InfantryDisposableWeaponPart part = newPart(TROOPERS);
        assertFalse(part.isPrimary());
        assertFalse(part.isSpent());
        assertFalse(part.needsFixing());
    }

    @Test
    @DisplayName("the loadout is valued at the weapon cost times the number of troopers")
    void stickerPriceScalesWithTroopers() {
        Money onePerTrooper = newPart(1).getStickerPrice();
        Money fullPlatoon = newPart(TROOPERS).getStickerPrice();

        assertTrue(onePerTrooper.isPositive(), "A disposable weapon part should have a positive value");
        assertEquals(onePerTrooper.multipliedBy(TROOPERS), fullPlatoon);
    }

    @Test
    @DisplayName("clone produces an equal, distinct part of the same type")
    void cloneIsDistinctSameType() {
        InfantryDisposableWeaponPart part = newPart(TROOPERS);
        InfantryDisposableWeaponPart clone = part.clone();

        assertNotSame(part, clone);
        assertInstanceOf(InfantryDisposableWeaponPart.class, clone);
        assertEquals(part.getName(), clone.getName());
        assertEquals(part.getTroopers(), clone.getTroopers());
        assertTrue(part.isSamePartType(clone), "A clone should be the same part type for warehouse/acquisition");
    }

    @Test
    @DisplayName("the missing part produces a matching spare for reload-from-stock")
    void missingPartProducesMatchingSpare() {
        InfantryDisposableWeaponPart part = newPart(TROOPERS);

        MissingInfantryDisposableWeaponPart missing = part.getMissingPart();
        assertEquals(TROOPERS, missing.getTroopers());

        Part spare = missing.getNewPart();
        assertInstanceOf(InfantryDisposableWeaponPart.class, spare);
        assertEquals(TROOPERS, ((InfantryDisposableWeaponPart) spare).getTroopers());
        assertTrue(part.isSamePartType(spare),
              "A spare acquired via the missing part must match so a fired loadout can be reloaded from stock");
    }

    @Test
    @DisplayName("a spent loadout presents as reloadable work")
    void spentLoadoutIsReloadWork() throws Exception {
        InfantryDisposableWeaponPart part = newPart(TROOPERS);
        Field spentField = InfantryDisposableWeaponPart.class.getDeclaredField("spent");
        spentField.setAccessible(true);
        spentField.set(part, true);

        assertTrue(part.isSpent());
        assertTrue(part.needsFixing());
        assertEquals(15, part.getBaseTime());
        assertEquals(0, part.getDifficulty());
    }
}
