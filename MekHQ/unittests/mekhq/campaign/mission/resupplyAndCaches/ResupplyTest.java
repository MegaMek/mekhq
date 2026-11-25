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
package mekhq.campaign.mission.resupplyAndCaches;

import static mekhq.campaign.mission.resupplyAndCaches.Resupply.getPartMultiplier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.units.Mek;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.JumpJet;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.parts.meks.MekLocation;
import org.junit.jupiter.api.Test;

/**
 * Tests the part-type multipliers that weight how often each part is offered in a Resupply. Armor is not equipment,
 * and ammunition reaches the pools as {@link AmmoStorage}, so both must still receive the armor and ammunition
 * multiplier rather than falling into the critical component one.
 */
class ResupplyTest {
    private static final double ARMOR_AND_AMMO = 5.0;
    private static final double CRITICAL_COMPONENT = 1.5;

    @Test
    void testArmorUsesArmorAndAmmoMultiplier() {
        assertEquals(ARMOR_AND_AMMO, getPartMultiplier(mock(Armor.class)));
    }

    @Test
    void testAmmoStorageUsesArmorAndAmmoMultiplier() {
        assertEquals(ARMOR_AND_AMMO, getPartMultiplier(mock(AmmoStorage.class)));
    }

    @Test
    void testAmmoBinUsesArmorAndAmmoMultiplier() {
        assertEquals(ARMOR_AND_AMMO, getPartMultiplier(mock(AmmoBin.class)));
    }

    @Test
    void testHeatSinkMultiplier() {
        assertEquals(2.5, getPartMultiplier(mock(HeatSink.class)));
    }

    @Test
    void testMekHeadMultiplier() {
        MekLocation head = mock(MekLocation.class);
        when(head.getLoc()).thenReturn(Mek.LOC_HEAD);

        assertEquals(2.0, getPartMultiplier(head));
    }

    @Test
    void testOtherMekLocationsAreNotWeighted() {
        MekLocation arm = mock(MekLocation.class);
        when(arm.getLoc()).thenReturn(Mek.LOC_LEFT_ARM);

        assertEquals(1.0, getPartMultiplier(arm));
    }

    @Test
    void testJumpJetsAreNotWeighted() {
        assertEquals(1.0, getPartMultiplier(mock(JumpJet.class)));
    }

    @Test
    void testEngineUsesCriticalComponentMultiplier() {
        assertEquals(CRITICAL_COMPONENT, getPartMultiplier(mock(EnginePart.class)));
    }

    @Test
    void testActuatorUsesCriticalComponentMultiplier() {
        assertEquals(CRITICAL_COMPONENT, getPartMultiplier(mock(MekActuator.class)));
    }
}
