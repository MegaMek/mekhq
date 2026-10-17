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

import static mekhq.campaign.parts.AmmoUtilities.getAmmoType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * Reload times under the Rearm Time Table (Campaign Operations p. 209): a one-shot weapon reloads in a quarter of the
 * standard time, and an omni pod is swapped in half. A one-shot SRM 4 on a Lightning Attack Hovercraft used to take
 * the full 15 minutes (MekHQ issue 3166).
 */
class AmmoBinReloadTimeTest {
    private static final int EQUIPMENT_NUMBER = 7;

    /**
     * Builds a bin on a unit whose mount holds the given ammunition, so the bin is reloading rather than swapping
     * unless the two differ.
     */
    private static AmmoBin bin(AmmoType binAmmo, AmmoType mountedAmmo, boolean isOneShot, boolean isOmniPodded,
          boolean isSalvage) {
        Campaign campaign = mockCampaign();
        AmmoBin ammoBin = new AmmoBin(0, binAmmo, EQUIPMENT_NUMBER, 0, isOneShot, isOmniPodded, campaign);
        Unit unit = mock(Unit.class);
        when(unit.isSalvage()).thenReturn(isSalvage);
        Entity entity = mock(Entity.class);
        when(unit.getEntity()).thenReturn(entity);
        Mounted<?> mounted = mock(Mounted.class);
        when(mounted.getType()).thenReturn(mountedAmmo);
        when(mounted.isOmniPodMounted()).thenReturn(isOmniPodded);
        when(entity.getEquipment(eq(EQUIPMENT_NUMBER))).thenReturn((Mounted) mounted);
        ammoBin.setUnit(unit);
        return ammoBin;
    }

    private static AmmoType srm4Ammo() {
        return getAmmoType("ISSRM4 Ammo");
    }

    @Test
    void standardBinReloadsInFifteenMinutes() {
        assertEquals(15, bin(srm4Ammo(), srm4Ammo(), false, false, false).getActualTime());
    }

    /** The reported case: a quarter of 15 minutes, rounded up. */
    @Test
    void oneShotLauncherReloadsInAQuarterOfTheTime() {
        assertEquals(4, bin(srm4Ammo(), srm4Ammo(), true, false, false).getActualTime());
    }

    @Test
    void podMountedOneShotLauncherTakesBothReductions() {
        assertEquals(2, bin(srm4Ammo(), srm4Ammo(), true, true, false).getActualTime());
    }

    /** Swapping to another munition takes twice a reload, and a one-shot launcher still takes a quarter of that. */
    @Test
    void oneShotMunitionSwapTakesAQuarterOfASwap() {
        AmmoType infernoAmmo = getAmmoType("ISSRM4 Inferno Ammo");

        assertEquals(8, bin(infernoAmmo, srm4Ammo(), true, false, false).getActualTime());
    }

    /** Salvaging the bin is not a reload, so it takes the full time. */
    @Test
    void salvagingAOneShotBinTakesTheFullTime() {
        assertEquals(120, bin(srm4Ammo(), srm4Ammo(), true, false, true).getActualTime());
    }
}
