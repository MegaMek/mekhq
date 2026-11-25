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
package mekhq.campaign.market.procurement;

import static mekhq.campaign.market.procurement.Procurement.getNegotiatorModifier;
import static mekhq.campaign.market.procurement.Procurement.isConsumable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests the procurement checks used by Resupplies: which parts count as ammunition, and the negotiator modifiers.
 */
class ProcurementTest {
    @Test
    void testAmmoStorageIsConsumable() {
        // Ammunition reaches the Resupply pools as AmmoStorage, the part bought for an AmmoBin
        assertTrue(isConsumable(mock(AmmoStorage.class)));
    }

    @Test
    void testAmmoBinIsConsumable() {
        assertTrue(isConsumable(mock(AmmoBin.class)));
    }

    @Test
    void testOtherPartsAreNotConsumable() {
        assertFalse(isConsumable(mock(Armor.class)));
        assertFalse(isConsumable(mock(HeatSink.class)));
    }

    @ParameterizedTest
    @CsvSource({ "NONE, 4", "ULTRA_GREEN, 3", "GREEN, 0", "REGULAR, 0", "VETERAN, -2", "ELITE, -3", "HEROIC, -3",
                 "LEGENDARY, -3" })
    void testNegotiatorModifier(SkillLevel skillLevel, int expectedModifier) {
        assertEquals(expectedModifier, getNegotiatorModifier(skillLevel));
    }

    @ParameterizedTest
    @CsvSource({ "0, 3", "1, 0", "2, 0", "3, -2", "4, -3", "5, -3", "6, -3" })
    void testNegotiatorModifierFromExperienceLevel(int experienceLevel, int expectedModifier) {
        // A Resupply finds its negotiator's experience level, where Ultra-Green is 0, and converts it to a skill
        // level. Each experience level must receive its own modifier, not the one for the level below.
        SkillLevel skillLevel = SkillType.skillLevelFromExperienceLevel(experienceLevel);

        assertEquals(expectedModifier, getNegotiatorModifier(skillLevel));
    }
}
