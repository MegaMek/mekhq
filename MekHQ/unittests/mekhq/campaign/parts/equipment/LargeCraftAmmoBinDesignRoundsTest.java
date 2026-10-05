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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * A large craft's ammunition is rounded up to whole tons, so a bin built for 60 Gauss rounds has 8 tons of space. It
 * is reloaded to the 60 rounds its design carries, not to the 64 the space would hold (issue #3697).
 */
class LargeCraftAmmoBinDesignRoundsTest {
    private static final int EQUIPMENT_NUMBER = 7;
    private static final AmmoType GAUSS_AMMO = getAmmoType("IS Gauss Ammo");

    private static LargeCraftAmmoBin binOnAUnit(double capacity, int designRounds, int roundsLoaded) {
        AmmoMounted mounted = mock(AmmoMounted.class);
        when(mounted.getType()).thenReturn(GAUSS_AMMO);
        when(mounted.getSize()).thenReturn(capacity);
        when(mounted.getOriginalShots()).thenReturn(designRounds);
        when(mounted.getBaseShotsLeft()).thenReturn(roundsLoaded);
        Entity entity = mock(Entity.class);
        when(entity.getEquipment(EQUIPMENT_NUMBER)).thenReturn((Mounted) mounted);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);

        LargeCraftAmmoBin ammoBin = new LargeCraftAmmoBin(0, GAUSS_AMMO, EQUIPMENT_NUMBER, 0, capacity,
              mockCampaign());
        ammoBin.setUnit(unit);
        ammoBin.updateConditionFromEntity(false);
        return ammoBin;
    }

    @Test
    void aFullBinBuiltForPartOfATonNeedsNothing() {
        // the Union-X: 60 rounds at 8 a ton is 7.5 tons, rounded up to 8
        LargeCraftAmmoBin ammoBin = binOnAUnit(8.0, 60, 60);

        assertEquals(60, ammoBin.getFullShots());
        assertEquals(0, ammoBin.getShotsNeeded());
    }

    @Test
    void aBinIsReloadedToTheDesignRoundsOnly() {
        LargeCraftAmmoBin ammoBin = binOnAUnit(8.0, 60, 50);

        assertEquals(10, ammoBin.getShotsNeeded());
    }

    @Test
    void aBinWhoseSpaceChangedHoldsWhatItsSpaceFits() {
        // ammunition swaps move space between bins; 10 tons no longer matches a 60 round design
        LargeCraftAmmoBin ammoBin = binOnAUnit(10.0, 60, 60);

        assertEquals(80, ammoBin.getFullShots());
    }

    @Test
    void aBinWithoutADesignRoundCountHoldsWhatItsSpaceFits() {
        LargeCraftAmmoBin ammoBin = binOnAUnit(8.0, 0, 0);

        assertEquals(64, ammoBin.getFullShots());
    }
}
