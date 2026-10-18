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
package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import megamek.common.equipment.IArmorState;
import megamek.common.units.Mek;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.unit.Maintenance.MaintenanceDamage;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * The maintenance report counts the parts a failed check damaged or destroyed. A location already destroyed has
 * nothing left to damage, so it is left alone and not counted; the report used to say a part was damaged when nothing
 * had changed (issue #5316).
 */
class MaintenanceDamageCountTest {
    private final PartsScenario scenario = PartsScenario.create();
    private final Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);

    private MekLocation rightTorso() {
        return PartsScenario.unitParts(wolverine, MekLocation.class).stream()
                     .filter(part -> part.getLoc() == Mek.LOC_RIGHT_TORSO)
                     .findFirst()
                     .orElseThrow();
    }

    private MekLocation destroyedRightTorso() {
        MekLocation rightTorso = rightTorso();
        wolverine.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, Mek.LOC_RIGHT_TORSO);
        return rightTorso;
    }

    @Test
    void aDestroyedLocationIsNotCountedAsDamaged() {
        MekLocation rightTorso = destroyedRightTorso();

        MaintenanceDamage damageDone = Maintenance.applyMaintenanceDamage(Map.of(rightTorso, 1));

        assertEquals(new MaintenanceDamage(0, 0), damageDone);
        assertTrue(wolverine.getEntity().getInternal(Mek.LOC_RIGHT_TORSO) < 1, "The right torso is still destroyed");
    }

    @Test
    void aDestroyedLocationIsNotCountedAsDestroyedAgain() {
        MekLocation rightTorso = destroyedRightTorso();

        MaintenanceDamage damageDone = Maintenance.applyMaintenanceDamage(Map.of(rightTorso, 4));

        assertEquals(new MaintenanceDamage(0, 0), damageDone);
        assertTrue(wolverine.getParts().contains(rightTorso), "The destroyed right torso is left as it is");
    }

    @Test
    void oneDamagedPartIsReportedInTheSingular() {
        String report = Maintenance.getDamageString(wolverine, 1, 0);

        assertTrue(report.contains("1 part was damaged."), report);
    }

    @Test
    void severalPartsAreReportedInThePlural() {
        String report = Maintenance.getDamageString(wolverine, 2, 3);

        assertTrue(report.contains("2 parts were damaged."), report);
        assertTrue(report.contains("3 parts were destroyed."), report);
    }

    @Test
    void nothingIsReportedWhenNothingWasDamaged() {
        assertEquals("", Maintenance.getDamageString(wolverine, 0, 0));
    }

    @Test
    void anIntactLocationIsCountedAsDamaged() {
        MaintenanceDamage damageDone = Maintenance.applyMaintenanceDamage(Map.of(rightTorso(), 1));

        assertEquals(new MaintenanceDamage(1, 0), damageDone);
    }
}
